# Wash Ranking Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Turn the bottom-navigation ranking page into a trustworthy, privacy-conscious wash leaderboard with time and frequency rankings, selectable periods, and an accurate personal-rank state.

**Architecture:** Continue to derive ranking data from completed `wash_order` rows and user presentation data from `user_info`; no denormalized ranking table is introduced. The database performs per-user aggregation for the selected period, a small backend assembler applies deterministic shared-rank rules and public-name masking, and the mini-program renders the returned contract as a two-dimensional leaderboard (metric plus period).

**Tech Stack:** Spring Boot 3 / Java 17, MyBatis-Plus, MySQL, JUnit 5 / AssertJ, WeChat Mini Program, TypeScript, WXML, SCSS.

## Global Constraints

- Only `wash_order.order_status = 'completed'` rows with non-null `user_id`, `start_time`, and `end_time` contribute to every ranking metric.
- Periods remain `day` (the latest 24 hours), `month` (the latest 30 days), and `total` (all time), with filtering based on `end_time`.
- Metrics are `duration` (sum of positive wash seconds) and `count` (number of eligible completed wash orders); do not create a public spending or balance ranking.
- Keep the existing `GET /api/orders/duration-ranking` endpoint working as a duration-ranking compatibility endpoint while the mini-program moves to the new typed ranking endpoint.
- Clamp the public row limit to `1..50`; the mini-program requests `50` so a user can browse a useful leaderboard without unbounded payloads.
- Public rows expose a masked display name. Only the separately returned current-user row may contain that user's unmasked nickname.
- Preserve the project’s existing teal theme, custom tab bar, TypeScript strict compilation, and no-new-dependency policy.

---

### Task 1: Build A Typed, Indexed Ranking API

**Files:**
- Create: `sql/migrations/014_wash_ranking.sql`
- Create: `backend/src/main/java/com/washer/backend/dto/order/WashRankingAggregate.java`
- Create: `backend/src/main/java/com/washer/backend/dto/order/WashRankingItem.java`
- Create: `backend/src/main/java/com/washer/backend/dto/order/WashRankingResponse.java`
- Create: `backend/src/main/java/com/washer/backend/service/impl/WashRankingAssembler.java`
- Create: `backend/src/test/java/com/washer/backend/service/impl/WashRankingAssemblerTest.java`
- Modify: `sql/car_wash_dev_init.sql`
- Modify: `backend/src/main/java/com/washer/backend/config/DatabaseMigrationRunner.java`
- Modify: `backend/src/main/java/com/washer/backend/mapper/WashOrderMapper.java`
- Modify: `backend/src/main/java/com/washer/backend/service/WashOrderService.java`
- Modify: `backend/src/main/java/com/washer/backend/service/impl/WashOrderServiceImpl.java`
- Modify: `backend/src/main/java/com/washer/backend/controller/WashOrderController.java`

**Interfaces:**
- Consumes: `wash_order(user_id, order_status, start_time, end_time)` and `user_info(id, nickname, avatar_url, user_status)`.
- Produces: `GET /api/orders/ranking?scope=day|month|total&metric=duration|count&userId={id?}&limit=1..50` with `WashRankingResponse`.
- Produces: `WashOrderMapper.selectWashRankingAggregates(LocalDateTime fromTime, String metric): List<WashRankingAggregate>`; `metric` must be the server-normalized literal `duration` or `count`, never raw user input.
- Preserves: `Map<String, Object> getDurationRanking(String scope, Long userId, int limit)` and `GET /api/orders/duration-ranking` by delegating to the new duration implementation and serializing the old keys (`scope`, `generatedAt`, `rows`, `myRank`).

- [ ] **Step 1: Write the failing ranking-assembly tests**

Create `WashRankingAssemblerTest` with fixture aggregates and users. It must prove that duration mode uses total seconds, count mode uses completed-order count, equal scores receive the same displayed rank, disabled or missing users do not appear, the current user receives an unmasked `displayName` only in `myRank`, and public rows use a mask:

```java
@Test
void assemble_countRanking_usesSharedRanksAndMasksPublicNames() {
    List<WashRankingAggregate> aggregates = List.of(
        aggregate(7L, 1800L, 5, at("2026-08-27T10:00:00")),
        aggregate(8L, 7200L, 5, at("2026-08-27T09:00:00")),
        aggregate(9L, 9000L, 3, at("2026-08-27T08:00:00"))
    );
    Map<Long, UserInfo> users = Map.of(7L, user(7L, "王小明", 1), 8L, user(8L, "李四", 1), 9L, user(9L, "赵六", 1));

    WashRankingResponse response = assembler.assemble("day", "count", aggregates, users, 8L, 50, LocalDateTime.now());

    assertThat(response.getRows()).extracting(WashRankingItem::getRank).containsExactly(1, 1, 3);
    assertThat(response.getRows()).extracting(WashRankingItem::getDisplayName).containsExactly("王**", "李*", "赵*");
    assertThat(response.getMyRank().getDisplayName()).isEqualTo("李四");
    assertThat(response.getMyRank().getScore()).isEqualTo(5L);
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `mvn -f backend/pom.xml test -Dtest=WashRankingAssemblerTest`

Expected: FAIL because `WashRankingAssembler`, the ranking DTOs, and its response contract do not exist.

- [ ] **Step 3: Add the aggregate query and ranking index**

Add migration `014_wash_ranking.sql` and the matching `sql/car_wash_dev_init.sql` index definition:

```sql
ALTER TABLE `wash_order`
  ADD KEY `idx_wash_order_ranking` (`order_status`, `end_time`, `user_id`);
```

In `DatabaseMigrationRunner`, add `ensureWashOrderRankingIndex()` to `run()`. It must first check `tableExists("wash_order")` and `indexExists("wash_order", "idx_wash_order_ranking")`; only then execute the same `ALTER TABLE` statement. Implement `indexExists` against `information_schema.STATISTICS` scoped to `DATABASE()`, `TABLE_NAME`, and `INDEX_NAME` so startup is idempotent.

Extend `WashOrderMapper` with an aggregated query. It must filter eligibility in SQL, use `end_time >= #{fromTime}` only when `fromTime` is non-null, exclude invalid negative durations, aggregate each user once, and use only a `<choose>` on the normalized `metric` parameter for ordering:

```java
@Select("""
    <script>
    SELECT user_id AS userId,
           SUM(TIMESTAMPDIFF(SECOND, start_time, end_time)) AS durationSeconds,
           COUNT(*) AS orderCount,
           MAX(end_time) AS latestEndTime
      FROM wash_order
     WHERE order_status = 'completed'
       AND user_id IS NOT NULL
       AND start_time IS NOT NULL
       AND end_time IS NOT NULL
       AND end_time >= start_time
       <if test='fromTime != null'>AND end_time &gt;= #{fromTime}</if>
     GROUP BY user_id
     ORDER BY
       <choose>
         <when test='metric == "count"'>COUNT(*) DESC,</when>
         <otherwise>SUM(TIMESTAMPDIFF(SECOND, start_time, end_time)) DESC,</otherwise>
       </choose>
       MAX(end_time) DESC, user_id ASC
    </script>
    """)
List<WashRankingAggregate> selectWashRankingAggregates(
    @Param("fromTime") LocalDateTime fromTime,
    @Param("metric") String metric
);
```

- [ ] **Step 4: Implement the typed service contract and compatibility adapter**

Define the API model explicitly. `WashRankingItem` contains `rank`, `userId`, `displayName`, `avatarUrl`, `score`, `durationSeconds`, `durationText`, `orderCount`, and `isCurrentUser`; `WashRankingResponse` contains `scope`, `metric`, `generatedAt`, `participantCount`, `rows`, and nullable `myRank`.

Add this service method while retaining the existing method:

```java
WashRankingResponse getWashRanking(String scope, String metric, Long userId, int limit);
```

`WashOrderServiceImpl#getWashRanking` must normalize invalid scope to `day`, invalid metric to `duration`, clamp `limit`, calculate the existing rolling start time, query the mapper, load only the aggregate user IDs with `userInfoService.listByIds`, and pass both maps to `WashRankingAssembler`.

The assembler must:

```java
long score = "count".equals(metric) ? aggregate.getOrderCount() : aggregate.getDurationSeconds();
int rank = previousScore == null || score != previousScore ? index + 1 : previousRank;
```

It must omit users whose `UserInfo` is absent or whose `userStatus` is not `1`; format duration as `02时05分` or `08分`; order equal scores by `latestEndTime DESC, userId ASC`; return at most `limit` public rows; and still return the current user's full row in `myRank` when that user is eligible but lies below the public cut-off. The old `getDurationRanking` turns this typed duration response into the current map shape to avoid breaking an installed older client.

Expose the new route in `WashOrderController`:

```java
@GetMapping("/ranking")
public ApiResponse<WashRankingResponse> ranking(
    @RequestParam(defaultValue = "day") String scope,
    @RequestParam(defaultValue = "duration") String metric,
    @RequestParam(required = false) Long userId,
    @RequestParam(defaultValue = "50") int limit
) {
    return ApiResponse.success(washOrderService.getWashRanking(scope, metric, userId, limit));
}
```

- [ ] **Step 5: Run the backend tests**

Run: `mvn -f backend/pom.xml test -Dtest=WashRankingAssemblerTest,PointMallProductServiceTest`

Expected: PASS. The new ranking test verifies metric selection, tie ranking, anonymity, disabled-user filtering, and an out-of-top-N personal row; the existing points-mall test remains green.

- [ ] **Step 6: Commit the backend unit**

```bash
git add sql/migrations/014_wash_ranking.sql sql/car_wash_dev_init.sql backend/src/main/java/com/washer/backend/config/DatabaseMigrationRunner.java backend/src/main/java/com/washer/backend/mapper/WashOrderMapper.java backend/src/main/java/com/washer/backend/dto/order/WashRankingAggregate.java backend/src/main/java/com/washer/backend/dto/order/WashRankingItem.java backend/src/main/java/com/washer/backend/dto/order/WashRankingResponse.java backend/src/main/java/com/washer/backend/service/impl/WashRankingAssembler.java backend/src/main/java/com/washer/backend/service/WashOrderService.java backend/src/main/java/com/washer/backend/service/impl/WashOrderServiceImpl.java backend/src/main/java/com/washer/backend/controller/WashOrderController.java backend/src/test/java/com/washer/backend/service/impl/WashRankingAssemblerTest.java
git commit -m "feat: add typed wash leaderboard API"
```

### Task 2: Consume The Typed Ranking Contract In The Mini Program

**Files:**
- Modify: `apis/order.ts`
- Modify: `pages/ranking/index.ts`
- Test: TypeScript project compilation via `npx tsc --noEmit`

**Interfaces:**
- Consumes: `GET /api/orders/ranking` from Task 1.
- Produces: `getWashRanking(scope, metric, userId, limit): Promise<WashRankingResponse>` and page data using a stable `RankingRow` UI type.

- [ ] **Step 1: Add typed client response definitions and API method**

At the top of `apis/order.ts`, declare the endpoint contract and replace the page’s untyped duration call with a new method:

```ts
export type WashRankingMetric = 'duration' | 'count';
export type WashRankingScope = 'day' | 'month' | 'total';

export type WashRankingResponse = {
  scope: WashRankingScope;
  metric: WashRankingMetric;
  generatedAt: string;
  participantCount: number;
  rows: Array<Record<string, any>>;
  myRank: Record<string, any> | null;
};

export const getWashRanking = async (
  scope: WashRankingScope,
  metric: WashRankingMetric,
  userId?: number,
  limit = 50
): Promise<WashRankingResponse | null> => {
  const { code, data } = await GET<WashRankingResponse>('/api/orders/ranking', {
    scope,
    metric,
    userId,
    limit,
  });
  return code === 0 && data ? data : null;
};
```

Keep `getDurationRanking` exported temporarily for compatibility with any unsearched page or future fallback; no ranking page should call it after this task.

- [ ] **Step 2: Add race-safe metric and period state**

In `pages/ranking/index.ts`, import the typed API and define these two controls:

```ts
const RANK_METRICS: Array<{ key: WashRankingMetric; title: string; unit: string }> = [
  { key: 'duration', title: '时长榜', unit: '洗车时长' },
  { key: 'count', title: '次数榜', unit: '完成洗车次数' },
];
const RANK_TABS: Array<{ key: WashRankingScope; title: string }> = [
  { key: 'day', title: '今日' },
  { key: 'month', title: '近30日' },
  { key: 'total', title: '总榜' },
];
```

Map `displayName`, `score`, `durationText`, and `orderCount` from the response. For duration mode use the server `durationText`; for count mode format the score as `${orderCount} 次`. On every `switchMetric`, `switchScope`, `onShow`, and `onPullDownRefresh`, call `loadRanking()` with the current values.

Do not keep the current `loading` early-return guard: it blocks a fast user change and lets an old request overwrite new tabs. Instead increment `requestSequence` before each request, capture it locally, and apply `setData` only when it is still the current sequence:

```ts
const requestId = ++this.requestSequence;
const result = await getWashRanking(scope, metric, userId, 50);
if (requestId !== this.requestSequence) return;
this.setData({ rows: this.mapRows(result), loading: false, loadFailed: false });
```

In a `finally` block, clear `loading` only for the active request and call `wx.stopPullDownRefresh()`. Treat an absent cached user ID as a visitor: load public rows without `myRank`, and render the personal panel as a login prompt rather than fabricating a “自己” record.

- [ ] **Step 3: Compile the mini-program sources**

Run: `npx tsc --noEmit`

Expected: PASS with no `pages/ranking/index.ts` type error and no unused ranking API symbols.

- [ ] **Step 4: Commit the client data unit**

```bash
git add apis/order.ts pages/ranking/index.ts
git commit -m "feat: load time and frequency leaderboards"
```

### Task 3: Redesign The Ranking Screen For Discovery, Personal Context, And Recovery

**Files:**
- Modify: `pages/ranking/index.wxml`
- Modify: `pages/ranking/index.scss`
- Modify: `pages/ranking/index.ts`
- Modify: `docs/功能逻辑整理.md`
- Test: WeChat Developer Tools manual test matrix and `npx tsc --noEmit`

**Interfaces:**
- Consumes: `metrics`, `tabs`, `activeMetric`, `activeScope`, `topRows`, `rows`, `hasMyRank`, `myRank`, `loading`, and `loadFailed` from Task 2 page state.
- Produces: a fixed personal-rank strip that always sits above `custom-tab-bar`, podium treatment for ranks 1–3, and explicit loading/empty/error states.

- [ ] **Step 1: Implement the page information hierarchy**

Replace the single top row of period tabs with metric segmented controls followed by period tabs. Keep the title compact as `洗车达人榜`, show the active metric unit in the list header, and display the rule copy `仅统计已完成的洗车订单` next to the generated-period context. Split loaded public rows into `topRows` (rank <= 3) and `rows` (rank > 3) in TypeScript.

Render the top three as an ordered podium with a visually larger first-place member, then render remaining ranks as compact scan-friendly rows. Use `wx:if` guards so a one- or two-person leaderboard does not create empty podium columns. Retain the existing local user avatar fallback and use `mode="aspectFill"` for non-empty avatar URLs.

Use this state order inside the list area:

```xml
<view wx:if="{{loading && topRows.length === 0 && rows.length === 0}}" class="rank-state">榜单加载中</view>
<view wx:elif="{{loadFailed}}" class="rank-state rank-state--error" bindtap="retryLoad">加载失败，点击重试</view>
<view wx:elif="{{topRows.length === 0 && rows.length === 0}}" class="rank-state">暂无已完成洗车记录</view>
<view wx:else>...</view>
```

- [ ] **Step 2: Position the personal context safely above navigation**

For a signed-in, ranked user, show `第 N 名`, avatar, display name, current metric score, and a small `继续加油` status. For a signed-in user with no eligible completed order, show `完成一次洗车即可上榜`; for a visitor, show `登录后查看我的排名` and bind it to the existing Mine tab with `wx.switchTab({ url: '/pages/mine/index' })`.

Make the bar independent of the list height and account for the custom tab bar rather than fixing it to the physical bottom:

```scss
.my-rank {
  bottom: calc(122rpx + env(safe-area-inset-bottom));
  min-height: 92rpx;
}

.page {
  padding-bottom: calc(122rpx + 116rpx + #{$page-padding-y} + env(safe-area-inset-bottom));
}
```

Use the existing teal, white, neutral, gold, silver, and bronze colors; do not introduce a new image asset or a decorative gradient background. Ensure all name and metric fields use `min-width: 0`, `overflow: hidden`, `white-space: nowrap`, and `text-overflow: ellipsis` so the five-tab layout and long display names remain stable.

- [ ] **Step 3: Compile and manually verify the complete interaction matrix**

Run: `npx tsc --noEmit`

Expected: PASS.

Then, in WeChat Developer Tools against seeded completed orders, verify all of the following:

1. `时长榜` and `次数榜` change the score label and ordering while each keeps its selected period.
2. `今日`, `近30日`, and `总榜` return only orders completed inside the corresponding time range.
3. Three tied top values show shared rank `1`; the next score shows rank `4`.
4. A logged-in user below the first 50 rows sees their true personal rank in the fixed strip.
5. A logged-in user with no completed order and a visitor both see the correct non-ranked state.
6. Pull-to-refresh and a rapid metric/period change do not display stale results; the personal strip remains fully visible above the tab bar on a narrow device.
7. A failed request shows the tappable retry state without hiding a previously loaded result.

- [ ] **Step 4: Document the final business rules and commit**

Replace the leaderboard paragraph in `docs/功能逻辑整理.md` with the completed-order eligibility rule, the two metrics, three rolling periods, masked public identity, tie rule, and personal-rank behavior. Then commit:

```bash
git add pages/ranking/index.ts pages/ranking/index.wxml pages/ranking/index.scss docs/功能逻辑整理.md
git commit -m "feat: improve wash leaderboard experience"
```

## Self-Review

**Spec coverage:** The plan upgrades the existing bottom-navigation rank page, derives every score from current `wash_order` and `user_info` schema, adds mainstream lightweight competition mechanics (metric/period switching, podium, personal rank), makes scoring transparent, masks public identities, and handles loading, empty, visitor, error, refresh, and rapid-switch states. No unrelated business subsystem is included.

**Placeholder scan:** No task contains TBD, generic test instructions, or undefined implementation steps. Commands, method names, response fields, database index, and manual acceptance cases are specified.

**Type consistency:** `WashRankingMetric` uses `duration | count` in the API client, page controls, controller query parameter, service normalizer, mapper ordering branch, and response `metric`; `WashRankingScope` uses `day | month | total` everywhere. The public endpoint uses `WashRankingResponse`, while the previous duration endpoint deliberately remains map-shaped for compatibility.

## Execution Handoff

Plan complete and saved to `docs/superpowers/plans/2026-08-27-wash-ranking.md`. Two execution options:

1. Subagent-Driven (recommended) - I dispatch a fresh subagent per task, review between tasks, fast iteration.
2. Inline Execution - Execute tasks in this session using executing-plans, batch execution with checkpoints.

Which approach?
