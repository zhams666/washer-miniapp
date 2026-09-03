# CloudBase Recharge Fixes Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Restore recharge price options and membership mock recharge in the CloudBase profile by serializing update timestamps correctly and removing unsupported active-date OR expressions.

**Architecture:** Use the existing CloudBase entity value conversion for update-wrapper assignment values so every `LocalDateTime` reaches PostgreSQL as ISO-8601 text. Keep the CloudBase mapper intentionally restricted to simple REST predicates; fetch enabled recharge products with equality predicates and decide time-window eligibility in Java.

**Tech Stack:** Spring Boot, MyBatis-Plus, CloudBase PostgreSQL HTTP API, Java 17, JUnit 5, Mockito.

## Global Constraints

- Preserve current payment modes and the mock-payment behavior when WeChat Pay is disabled.
- Do not broaden the CloudBase mapper to translate arbitrary MyBatis `OR` expressions.
- Do not log credentials, authorization headers, openids, phone numbers, or payment signatures.
- Retain the existing device/recharge/member diagnostic log events.

---

### Task 1: Serialize update-wrapper timestamps for CloudBase PATCH bodies

**Files:**
- Modify: `backend/src/main/java/com/washer/backend/cloudbase/mapper/CloudBaseEntityMetadata.java`
- Modify: `backend/src/main/java/com/washer/backend/cloudbase/mapper/CloudBaseWrapperTranslator.java`
- Modify: `backend/src/test/java/com/washer/backend/cloudbase/mapper/CloudBaseMapperInvocationHandlerTest.java`

**Interfaces:**
- Consumes: `LambdaUpdateWrapper<T>.getParamNameValuePairs()` values such as `LocalDateTime`.
- Produces: PATCH request bodies where `LocalDateTime` values are ISO local timestamp strings, e.g. `2026-09-03T09:43:23.858741358`.

- [ ] **Step 1: Write a failing CloudBase update serialization test**

```java
@Test
void updateSerializesTimestampAssignmentsAsIsoStrings() throws Exception {
    LocalDateTime timestamp = LocalDateTime.of(2026, 9, 3, 9, 43, 23, 858_741_358);
    mapper.update(null, new LambdaUpdateWrapper<UserInfo>()
        .eq(UserInfo::getId, 1L)
        .set(UserInfo::getLastLoginTime, timestamp));
    assertEquals("2026-09-03T09:43:23.858741358", capturedBody.getValue().get("last_login_time"));
}
```

- [ ] **Step 2: Run the focused test to confirm the historical failure**

Run: `mvn -f .\\backend\\pom.xml -Dtest=CloudBaseMapperInvocationHandlerTest test`

Expected: FAIL because update-wrapper values are currently passed through as raw `LocalDateTime` objects.

- [ ] **Step 3: Reuse entity conversion in wrapper updates**

```java
body.put(columnName(matcher.group(1)), metadata.postgrestValue(
    updateWrapper.getParamNameValuePairs().get(matcher.group(2))
));
```

Make `CloudBaseEntityMetadata.postgrestValue(Object)` package-visible, retaining its existing conversion for `LocalDateTime`, `LocalDate`, and `LocalTime`.

- [ ] **Step 4: Run the focused test to verify the fix**

Run: `mvn -f .\\backend\\pom.xml -Dtest=CloudBaseMapperInvocationHandlerTest test`

Expected: PASS; generated PATCH body contains a string, not a timestamp array.

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/washer/backend/cloudbase/mapper/CloudBaseEntityMetadata.java backend/src/main/java/com/washer/backend/cloudbase/mapper/CloudBaseWrapperTranslator.java backend/src/test/java/com/washer/backend/cloudbase/mapper/CloudBaseMapperInvocationHandlerTest.java
git commit -m "fix: serialize CloudBase update timestamps"
```

### Task 2: Make recharge product eligibility queries CloudBase-safe

**Files:**
- Modify: `backend/src/main/java/com/washer/backend/controller/MiniWalletController.java`
- Modify: `backend/src/main/java/com/washer/backend/service/impl/AdminWalletRechargeServiceImpl.java`
- Test: `backend/src/test/java/com/washer/backend/cloudbase/mapper/CloudBaseMapperInvocationHandlerTest.java`

**Interfaces:**
- Consumes: enabled `WalletRechargeProduct` rows selected by store ID, product ID, and status.
- Produces: available recharge products only when `effectiveTime` is absent/past and `expireTime` is absent/future, evaluated in Java without MyBatis `OR`.

- [ ] **Step 1: Add a failing assertion that an OR wrapper is rejected by CloudBase**

```java
assertThatThrownBy(() -> mapper.selectList(new LambdaQueryWrapper<WalletRechargeProduct>()
    .eq(WalletRechargeProduct::getStatus, 1)
    .and(w -> w.isNull(WalletRechargeProduct::getEffectiveTime)
        .or().le(WalletRechargeProduct::getEffectiveTime, now))))
    .isInstanceOf(CloudBasePgException.class);
```

- [ ] **Step 2: Run the mapper test before changing the call sites**

Run: `mvn -f .\\backend\\pom.xml -Dtest=CloudBaseMapperInvocationHandlerTest test`

Expected: PASS, documenting the deliberate CloudBase constraint that the production call sites must avoid.

- [ ] **Step 3: Query simple predicates and filter the active time window in Java**

```java
return walletRechargeProductMapper.selectList(new LambdaQueryWrapper<WalletRechargeProduct>()
    .eq(WalletRechargeProduct::getStoreId, storeId)
    .eq(WalletRechargeProduct::getStatus, 1)
    .orderByAsc(WalletRechargeProduct::getPayAmount)
    .orderByAsc(WalletRechargeProduct::getId)
).stream().filter(product -> isRechargeProductActive(product, now)).toList();
```

For an individual purchase, select using only product ID, store ID, and status, then call the same time-window predicate before creating an order.

- [ ] **Step 4: Run targeted backend tests**

Run: `mvn -f .\\backend\\pom.xml -Dtest=CloudBaseMapperInvocationHandlerTest,DeviceControllerTest test`

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/washer/backend/controller/MiniWalletController.java backend/src/main/java/com/washer/backend/service/impl/AdminWalletRechargeServiceImpl.java backend/src/test/java/com/washer/backend/cloudbase/mapper/CloudBaseMapperInvocationHandlerTest.java
git commit -m "fix: load CloudBase recharge products without OR"
```

### Task 3: Verify the release path

**Files:**
- Modify: `docs/小程序当前进度.md`

**Interfaces:**
- Consumes: CloudBase deployment logs and mini-program recharge/member pages.
- Produces: a regression checklist for fetching recharge amounts and completing a mock membership recharge.

- [ ] **Step 1: Add cloud regression checks**

Document: open the recharge page and see at least one amount; submit a mock membership order; search `membership_order_completed`; verify no `DATABASE_22007`, no `CloudBase HTTP profile does not support this MyBatis query expression`, and no `recharge_create_failed` in the same time window.

- [ ] **Step 2: Run full local checks**

Run: `mvn -f .\\backend\\pom.xml test`

Run: `npm run check`

Expected: PASS.

- [ ] **Step 3: Commit**

```bash
git add docs/小程序当前进度.md
git commit -m "docs: add CloudBase recharge regression checks"
```

## Self-Review

1. Spec coverage: Task 1 resolves the observed `DATABASE_22007` timestamp-array failure; Task 2 resolves the observed unsupported MyBatis OR expression that empties recharge price options; Task 3 records how to verify both fixes.
2. Placeholder scan: Every task declares exact files, tests, commands, and expected results.
3. Type consistency: `CloudBaseEntityMetadata.postgrestValue(Object)` is consumed only inside the existing mapper package, and product filtering uses the existing `WalletRechargeProduct` entity fields.

## Execution Handoff

Plan complete and saved to `docs/superpowers/plans/2026-09-03-cloudbase-recharge-fixes.md`.

Two execution options:

1. Subagent-Driven (recommended): dispatch a fresh subagent per task and review between tasks.
2. Inline Execution: execute this plan in the current session using focused tests and checkpoints.

The user explicitly requested a backend fix, so use Inline Execution.
