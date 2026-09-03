# CloudBase Backend Compatibility Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make every existing CloudBase-profile backend query path work without MyBatis expressions that the HTTP mapper cannot translate.

**Architecture:** Extend the CloudBase wrapper translator so grouped boolean conditions and trailing row-lock clauses can be expressed through the PostgreSQL REST API. Keep local MySQL behavior unchanged, then add focused tests that exercise order, card, point, admin dashboard, asset, and search query shapes through the CloudBase mapper.

**Tech Stack:** Java 17, Spring Boot 3, MyBatis-Plus 3.5, CloudBase PostgreSQL REST API, JUnit 5, Mockito.

## Global Constraints

- CloudBase profile must not create a JDBC data source.
- Local MySQL must retain existing `FOR UPDATE` behavior.
- CloudBase request filters must use only supported REST query syntax.
- No database credentials or CloudBase API keys may be written to source files or tests.

---

### Task 1: Translate grouped CloudBase filters and ignore cloud row locks

**Files:**
- Modify: `backend/src/main/java/com/washer/backend/cloudbase/mapper/CloudBaseWrapperTranslator.java`
- Modify: `backend/src/test/java/com/washer/backend/cloudbase/mapper/CloudBaseMapperInvocationHandlerTest.java`

**Interfaces:**
- Consumes: MyBatis `AbstractWrapper#getSqlSegment()` and parameter pairs.
- Produces: `Map<String, List<String>>` valid for `CloudBasePgClient.select`, `update`, and `delete`.

- [x] **Step 1: Add failing translator tests for a grouped OR condition and a trailing FOR UPDATE clause**

```java
mapper.selectList(new LambdaQueryWrapper<WashOrder>()
    .eq(WashOrder::getStoreId, 2L)
    .and(w -> w.eq(WashOrder::getDeviceId, 3L).or().eq(WashOrder::getBayId, 9L))
    .last("limit 1 for update"));
assertEquals(List.of("(device_id.eq.3,bay_id.eq.9)"), query.getValue().get("or"));
assertEquals(List.of("1"), query.getValue().get("limit"));
```

- [x] **Step 2: Implement recursive top-level AND/OR parsing and strip only a trailing FOR UPDATE token**

```java
segment = stripTrailingForUpdate(segment);
FilterExpression expression = parseExpression(stripOuterParentheses(segment));
appendExpression(expression, parameters, query);
```

- [x] **Step 3: Run the mapper test**

Run: `mvn -f .\\backend\\pom.xml -Dtest=CloudBaseMapperInvocationHandlerTest test`

Expected: PASS with grouped order, card, dashboard, and search filters translated.

### Task 2: Remove CloudBase full-table pagination count reads

**Files:**
- Modify: `backend/src/main/java/com/washer/backend/cloudbase/CloudBasePgClient.java`
- Modify: `backend/src/main/java/com/washer/backend/cloudbase/mapper/CloudBaseMapperInvocationHandler.java`
- Modify: `backend/src/test/java/com/washer/backend/cloudbase/CloudBasePgClientTest.java`

**Interfaces:**
- Produces: a paged REST result containing rows and a count parsed from the response header.
- Consumes: page `limit` and `offset` translated by `CloudBaseMapperInvocationHandler`.

- [x] **Step 1: Add a client test for `Content-Range: 0-9/42`**

```java
assertEquals(42L, response.total());
assertEquals(10, response.rows().size());
```

- [x] **Step 2: Request `Prefer: count=exact` for paginated REST reads and parse the total after `/`**

```java
CloudBasePgPage response = client.selectPage(table, query);
page.setTotal(response.total());
page.setRecords(response.rows());
```

- [x] **Step 3: Run the CloudBase client and mapper tests**

Run: `mvn -f .\\backend\\pom.xml -Dtest=CloudBasePgClientTest,CloudBaseMapperInvocationHandlerTest test`

Expected: PASS without a full unpaged select used to compute totals.

### Task 3: Verify all high-risk business query shapes

**Files:**
- Modify: `backend/src/test/java/com/washer/backend/cloudbase/mapper/CloudBaseMapperInvocationHandlerTest.java`

**Interfaces:**
- Consumes: the CloudBase mapper factory and representative `LambdaQueryWrapper` instances.
- Produces: regression coverage for active-card windows, dashboard fallback time windows, keyword searches, and row-lock suffixes.

- [x] **Step 1: Add representative query tests**

```java
new LambdaQueryWrapper<UserCard>()
    .eq(UserCard::getStatus, "active")
    .and(w -> w.isNull(UserCard::getExpireTime).or().gt(UserCard::getExpireTime, now));
```

- [x] **Step 2: Run full backend verification**

Run: `mvn -f .\\backend\\pom.xml test`

Expected: PASS with no CloudBase compatibility test failures.

- [ ] **Step 3: Commit after user review**

```bash
git add backend/src/main/java backend/src/test/java docs/superpowers/plans
git commit -m "fix: complete CloudBase backend query compatibility"
```
