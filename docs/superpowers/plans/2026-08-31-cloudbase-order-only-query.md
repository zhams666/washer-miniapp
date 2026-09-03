# CloudBase Order-Only Query Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Allow the CloudBase PostgreSQL HTTP mapper to execute MyBatis queries that contain only an `ORDER BY` clause, so the mini-program store list and similar endpoints do not fail before login.

**Architecture:** The mapper converts a MyBatis wrapper SQL segment into PostgREST query parameters. Its order parser must accept `ORDER BY` at the beginning of a segment as well as after filters, and preserve the existing `column.desc` parameter format.

**Tech Stack:** Java 17, Spring Boot 3.2, MyBatis-Plus, JUnit 5, Mockito.

## Global Constraints

- Keep the `cloudbase` profile on PostgreSQL HTTP REST/RPC; do not add JDBC access.
- Preserve rejection of unsupported `OR`, raw SQL, and row-lock expressions.
- Do not change deployment credentials or database schema for this parser fix.

---

### Task 1: Translate an order-only MyBatis wrapper

**Files:**
- Modify: `backend/src/test/java/com/washer/backend/cloudbase/mapper/CloudBaseMapperInvocationHandlerTest.java`
- Modify: `backend/src/main/java/com/washer/backend/cloudbase/mapper/CloudBaseWrapperTranslator.java`

**Interfaces:**
- Consumes: `CloudBaseMapperFactory.create(UserInfoMapper.class, client, objectMapper)`.
- Produces: `CloudBasePgClient.select("user_info", query)` with `order=id.desc` and `limit=2`.

- [x] **Step 1: Write the failing test**

```java
UserInfoMapper mapper = CloudBaseMapperFactory.create(UserInfoMapper.class, client, objectMapper);
mapper.selectList(new LambdaQueryWrapper<UserInfo>().orderByDesc(UserInfo::getId));
verify(client).select(eq("user_info"), query.capture());
assertEquals(List.of("id.desc"), query.getValue().get("order"));
```

- [x] **Step 2: Run the focused test to verify it fails**

Run: `mvn -Dtest=CloudBaseMapperInvocationHandlerTest test`

Expected: FAIL with `CloudBase HTTP profile does not support this MyBatis condition`.

- [x] **Step 3: Accept beginning-of-segment ORDER BY**

```java
private static final Pattern ORDER_BY = Pattern.compile("(?i)(?:^|\\s+)ORDER BY\\s+(.+)$");
```

Keep removing the matched order clause before condition parsing so an order-only segment becomes blank.

- [x] **Step 4: Run focused and full backend tests**

Run: `mvn -Dtest=CloudBaseMapperInvocationHandlerTest test` and `mvn test`.

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/washer/backend/cloudbase/mapper/CloudBaseWrapperTranslator.java backend/src/test/java/com/washer/backend/cloudbase/mapper/CloudBaseMapperInvocationHandlerTest.java docs/superpowers/plans/2026-08-31-cloudbase-order-only-query.md
git commit -m "fix: support ordered CloudBase HTTP queries"
```

## Self-Review

- Spec coverage: The plan directly covers the observed `orderByDesc` failure and leaves unsupported compound predicates rejected.
- Placeholder scan: No placeholder tasks or unspecified implementation details remain.
- Type consistency: The test uses the existing mapper factory and captured `Map<String, List<String>>` query interface.
