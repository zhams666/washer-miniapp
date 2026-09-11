# Ranking Display Adjustments Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Allow administrators to add display-only wash duration records that are combined with actual completed-order durations on the existing mini-program ranking.

**Architecture:** Store each administrator-added duration in an independent `ranking_display_adjustment` table with its own timestamp. The ranking service aggregates completed-order duration and these display adjustments over the selected time range, without changing orders, devices, balances, payments, or settlement data. The Vue admin page manages adjustment records; the mini-program continues to call its existing ranking API.

**Tech Stack:** Spring Boot 3, MyBatis-Plus, CloudBase PostgreSQL REST mapper, MySQL local development, Vue 3, Element Plus, TypeScript, WeChat Mini Program.

## Global Constraints

- The adjustment table must never be used by billing, device control, wallet, settlement, or order lifecycle code.
- Each created adjustment uses the server current time; it contributes to 24H, 30-day, and total rankings according to that timestamp.
- Ranking output remains one list and ranks by actual completed-order duration plus display adjustment duration.
- The feature must work in both local MySQL and CloudBase PostgreSQL profiles.

---

### Task 1: Add persistence and deployment schema

**Files:**
- Create: `backend/src/main/java/com/washer/backend/entity/RankingDisplayAdjustment.java`
- Create: `backend/src/main/java/com/washer/backend/mapper/RankingDisplayAdjustmentMapper.java`
- Modify: `backend/src/main/java/com/washer/backend/config/CloudBaseMapperConfiguration.java`
- Modify: `backend/src/main/java/com/washer/backend/config/DatabaseMigrationRunner.java`
- Create: `sql/migrations/015_ranking_display_adjustment.sql`
- Create: `sql/postgresql/004_ranking_display_adjustment.sql`

**Interfaces:**
- Produces `RankingDisplayAdjustment` fields: `id`, `userId`, `displayDurationSeconds`, `remark`, `occurredAt`, `createdAt`, `updatedAt`.
- Produces a `RankingDisplayAdjustmentMapper` available under both database profiles.

- [x] **Step 1: Define the independent adjustment entity and mapper**
- [x] **Step 2: Register the CloudBase mapper bean and local/MySQL schema creation**
- [x] **Step 3: Add MySQL and PostgreSQL deployment migration scripts**
- [x] **Step 4: Verify the backend compiles with the new mapper**

### Task 2: Add administration and ranking aggregation services

**Files:**
- Create: `backend/src/main/java/com/washer/backend/dto/admin/AdminRankingDisplayAdjustmentCreateRequest.java`
- Create: `backend/src/main/java/com/washer/backend/dto/admin/AdminRankingDisplayAdjustmentItem.java`
- Create: `backend/src/main/java/com/washer/backend/service/RankingDisplayAdjustmentService.java`
- Create: `backend/src/main/java/com/washer/backend/service/impl/RankingDisplayAdjustmentServiceImpl.java`
- Modify: `backend/src/main/java/com/washer/backend/service/impl/WashOrderServiceImpl.java`
- Test: `backend/src/test/java/com/washer/backend/service/RankingDisplayAdjustmentServiceTest.java`

**Interfaces:**
- `createAdjustment(AdminRankingDisplayAdjustmentCreateRequest request): AdminRankingDisplayAdjustmentItem` accepts `userId`, `durationMinutes`, and optional `remark`.
- `pageAdjustments(long page, long size, String keyword): Page<AdminRankingDisplayAdjustmentItem>` lists only display-operation records.
- `deleteAdjustment(Long id): void` removes only the selected display-operation record.
- `getDurationSecondsByUser(LocalDateTime fromTime, LocalDateTime toTime): Map<Long, Long>` supplies the display-only additions for ranking aggregation.

- [x] **Step 1: Write service tests for duration validation, current timestamp assignment, and deletion isolation**
- [x] **Step 2: Implement the adjustment service using the independent table**
- [x] **Step 3: Merge per-user display duration into `getDurationRanking` after actual order aggregation**
- [x] **Step 4: Run the focused service tests and the full backend test suite**

### Task 3: Expose admin endpoints

**Files:**
- Create: `backend/src/main/java/com/washer/backend/controller/AdminRankingDisplayAdjustmentController.java`

**Interfaces:**
- `GET /api/admin/ranking-display-adjustments?page=1&size=10&keyword=` returns a paged adjustment list.
- `POST /api/admin/ranking-display-adjustments` creates a display duration addition at server current time.
- `DELETE /api/admin/ranking-display-adjustments/{id}` removes one display duration addition.

- [x] **Step 1: Bind each endpoint to the adjustment service with existing `ApiResponse` envelopes**
- [x] **Step 2: Verify invalid duration and unknown user requests return clear validation messages**

### Task 4: Build the admin ranking management page

**Files:**
- Create: `admin-web/src/api/ranking-display-adjustment.ts`
- Create: `admin-web/src/types/ranking-display-adjustment.ts`
- Create: `admin-web/src/views/ranking/RankingDisplayAdjustmentPage.vue`
- Modify: `admin-web/src/router/index.ts`
- Modify: `admin-web/src/layout/AdminLayout.vue`

**Interfaces:**
- The page loads `GET /api/admin/ranking-display-adjustments`.
- The form sends `{ userId, durationMinutes, remark }` to `POST /api/admin/ranking-display-adjustments`.
- Each table row can be deleted with `DELETE /api/admin/ranking-display-adjustments/{id}`.

- [x] **Step 1: Add API client types and methods**
- [x] **Step 2: Implement the searchable table and add-duration dialog**
- [x] **Step 3: Add the `排行榜管理` route and sidebar entry**
- [x] **Step 4: Run the admin production build**

### Task 5: Document and verify deployment

**Files:**
- Modify: `docs/superpowers/plans/2026-09-04-ranking-display-adjustments.md`

- [x] **Step 1: Record the CloudBase PostgreSQL migration requirement**
- [x] **Step 2: Run `mvn -f .\\backend\\pom.xml test` and `mvn -f .\\backend\\pom.xml -DskipTests package`**
- [x] **Step 3: Run `npm run build` in `admin-web`**
