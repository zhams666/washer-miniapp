# Device And Recharge Diagnostics Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Restore actionable diagnostics for device status changes, wallet recharge, and membership recharge without changing their business behavior.

**Architecture:** Add structured SLF4J logs at controller boundaries where user/store/device/plan IDs are resolved and at service boundaries where an order is created. Add browser or mini-program console context that mirrors safe IDs and the returned message. Exceptions continue through the existing global error handler unchanged.

**Tech Stack:** Spring Boot, SLF4J, Vue 3, Element Plus, WeChat Mini Program TypeScript, Maven, Vite.

## Global Constraints

- Do not change device state transitions, recharge calculations, payment mode, CloudBase query expressions, or database update behavior.
- Do not log API keys, authorization headers, openids, phone numbers, complete request bodies, or payment signatures.
- Keep the existing Chinese client error messages and the global exception response format.

---

### Task 1: Restore server-side diagnostics

**Files:**
- Modify: `backend/src/main/java/com/washer/backend/controller/DeviceController.java`
- Modify: `backend/src/main/java/com/washer/backend/controller/MiniWalletController.java`
- Modify: `backend/src/main/java/com/washer/backend/controller/MembershipController.java`
- Modify: `backend/src/main/java/com/washer/backend/service/MembershipService.java`
- Test: `backend/src/test/java/com/washer/backend/controller/DeviceControllerTest.java`

**Interfaces:**
- Consumes: `PUT /api/devices/{id}`, `POST /pay/recharge`, and `POST /membership/orders`.
- Produces: `device_update_*`, `recharge_create_*`, and `membership_order_*` log events containing safe IDs, resulting order numbers/statuses, and exception messages.

- [ ] **Step 1: Write a failing controller-path test**

```java
@Test
void updateIdleDeviceStillCancelsRunningOrdersBeforeUpdating() {
    Device device = new Device();
    device.setDeviceStatus("idle");
    when(deviceService.updateById(device)).thenReturn(true);
    controller.update(1L, device);
    verify(washOrderService).cancelRunningOrdersForDevice(1L, "管理员更新设备状态");
}
```

- [ ] **Step 2: Run the focused test before code changes**

Run: `mvn -f .\\backend\\pom.xml -Dtest=DeviceControllerTest test`

Expected: PASS, establishing that the diagnostic changes must not alter device behavior.

- [ ] **Step 3: Add start/success/failure logs around each operation**

```java
LOGGER.info("device_update_started deviceId={}, fromStatus={}, targetStatus={}", id, fromStatus, targetStatus);
LOGGER.error("device_update_failed deviceId={}, targetStatus={}, reason={}", id, targetStatus, ex.getMessage(), ex);
```

Use the same pattern for recharge with `userId`, `storeId`, `rechargeProductId`, `rechargeOrderNo`, and `payStatus`; use it for membership with `userId`, `planId`, `orderNo`, and `payStatus`.

- [ ] **Step 4: Run the focused test after code changes**

Run: `mvn -f .\\backend\\pom.xml -Dtest=DeviceControllerTest test`

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/washer/backend/controller/DeviceController.java backend/src/main/java/com/washer/backend/controller/MiniWalletController.java backend/src/main/java/com/washer/backend/controller/MembershipController.java backend/src/main/java/com/washer/backend/service/MembershipService.java backend/src/test/java/com/washer/backend/controller/DeviceControllerTest.java
git commit -m "chore: restore device and recharge diagnostics"
```

### Task 2: Restore safe client-side failure context

**Files:**
- Modify: `admin-web/src/views/devices/DeviceListPage.vue`
- Modify: `pages/pay/index.ts`
- Modify: `pages/member/index.ts`

**Interfaces:**
- Consumes: Axios and mini-program error objects plus the selected device, recharge product, or membership plan.
- Produces: browser/WeChat developer-console entries that identify the failed operation without exposing credentials; PC device toasts retain a meaningful server response when available.

- [ ] **Step 1: Write the expected error extraction helper**

```ts
const resolveRequestErrorMessage = (error: unknown, fallback: string) =>
  error instanceof Error && error.message.trim() ? error.message.trim() : fallback;
```

- [ ] **Step 2: Run the type checker before the view change**

Run: `npm run check`

Expected: PASS.

- [ ] **Step 3: Log safe identifiers and extracted messages at each catch block**

```ts
console.error('device save failed', {
  deviceId: editingId.value,
  targetStatus: deviceForm.deviceStatus,
  message: resolveRequestErrorMessage(error, fallback),
  error,
});
```

For recharge log `storeId`, `rechargeProductId`, `rechargeOrderNo` when returned, and error message. For membership log `planId`, `orderNo` when returned, and error message.

- [ ] **Step 4: Run type/build checks after the view change**

Run: `npm run check`

Run: `npm run build` from `admin-web`

Expected: PASS; Sass deprecation and bundle-size warnings are allowed.

- [ ] **Step 5: Commit**

```bash
git add admin-web/src/views/devices/DeviceListPage.vue pages/pay/index.ts pages/member/index.ts
git commit -m "chore: restore client diagnostic context"
```

## Self-Review

1. Spec coverage: Task 1 adds server logs for both reported issue areas; Task 2 makes the failure context visible in each client development console and preserves a useful PC toast.
2. Placeholder scan: Each task names files, log fields, exact commands, and expected outcomes.
3. Type consistency: `Device`, recharge requests/results, membership results, and existing controller/service endpoints remain unchanged.

## Execution Handoff

Plan complete and saved to `docs/superpowers/plans/2026-09-03-device-recharge-diagnostics.md`.

Two execution options:

1. Subagent-Driven (recommended): dispatch a fresh subagent per task and review between tasks.
2. Inline Execution: execute this plan in the current session using focused tests and checkpoints.

The user requested the logs restored now, so use Inline Execution.
