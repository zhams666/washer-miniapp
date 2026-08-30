# Device And Order State Consistency Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Keep a device reported as idle from retaining a running wash order that makes the mini-program mark its bay as occupied.

**Architecture:** Device control remains the boundary for forced state changes. Before an administrator stops a device or saves it in a non-running state, the backend cancels any running orders targeting that device or its bay, releasing order-held resources through the existing cancellation flow before the device state is persisted.

**Tech Stack:** Spring Boot 3, Java 17, MyBatis-Plus, JUnit 5, Mockito.

## Global Constraints

- Reuse `WashOrderService.cancelOrder` so card locks, order status logs, and device reset logic follow the existing cancellation rules.
- Do not change the mini-program API contract for `bayStatusList`.
- A running device update must not cancel an order; only non-running target states perform reconciliation.

---

### Task 1: Reconcile Device-Controlled Orders

**Files:**
- Modify: `backend/src/main/java/com/washer/backend/service/WashOrderService.java`
- Modify: `backend/src/main/java/com/washer/backend/service/impl/WashOrderServiceImpl.java`
- Modify: `backend/src/main/java/com/washer/backend/controller/DeviceController.java`
- Test: `backend/src/test/java/com/washer/backend/controller/DeviceControllerTest.java`

**Interfaces:**
- Produces: `int WashOrderService.cancelRunningOrdersForDevice(Long deviceId, String remark)`.
- Consumes: `POST /api/devices/{id}/stop` and `PUT /api/devices/{id}`.

- [x] **Step 1: Write controller tests for stop and idle update reconciliation.**

```java
verify(washOrderService).cancelRunningOrdersForDevice(1L, "管理员模拟停止设备");
verify(deviceService).mockStopDevice(1L);
```

- [x] **Step 2: Implement the service method using the existing cancellation flow.**

```java
for (WashOrder order : runningOrders) {
    cancelOrder(order.getId());
}
```

- [x] **Step 3: Invoke reconciliation before device stop and non-running status updates.**

```java
if (shouldStopRunningOrders(device.getDeviceStatus())) {
    washOrderService.cancelRunningOrdersForDevice(id, "管理员更新设备状态");
}
```

- [x] **Step 4: Run `mvn -f backend/pom.xml test -Dtest=DeviceControllerTest` and then the full backend test suite.**
