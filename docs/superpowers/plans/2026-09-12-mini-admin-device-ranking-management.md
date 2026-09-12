# Mini Admin Device And Ranking Management Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Let store managers add devices and manage the existing public ranking display duration from the mobile mini-program while every read and write remains consistent with the PC backend data.

**Architecture:** Mobile pages call new `/api/mini-admin` endpoints authenticated by `X-Washer-Admin-Token`. Those endpoints delegate to the same `DeviceService`, `WashOrderService`, and `RankingDisplayAdjustmentService` used by the PC management system. The portal service resolves the logged-in manager's store scope before creating a device or allowing a ranking adjustment, so a store manager cannot write data for an unrelated store or customer.

**Tech Stack:** WeChat Mini Program TypeScript/WXML/SCSS, Spring Boot 3, MyBatis-Plus, JUnit 5, Mockito.

## Global Constraints

- PC and mobile must write the same `device` records and `ranking_display_adjustment` records; no mobile-only cache or duplicate table is allowed.
- The mobile entries are visible only for the `store` tier. Backend scope checks remain authoritative for every request.
- Device creation requires a store within the logged-in manager's scope and the existing device-code uniqueness constraint remains the final duplicate guard.
- Ranking rows use the existing public ranking calculation. A store manager may set or remove only a ranking adjustment belonging to a user with a completed order in the manager's accessible store(s) during the selected ranking period.
- A ranking display adjustment must never modify wash orders, device runtime, wallet balances, settlement, or real wash duration.
- Page-size and ranking limits are bounded server-side.

---

### Task 1: Centralize device creation in the existing service layer

**Files:**
- Create: `backend/src/main/java/com/washer/backend/dto/miniadmin/MiniAdminDeviceCreateRequest.java`
- Modify: `backend/src/main/java/com/washer/backend/service/DeviceService.java`
- Modify: `backend/src/main/java/com/washer/backend/service/impl/DeviceServiceImpl.java`
- Modify: `backend/src/main/java/com/washer/backend/controller/DeviceController.java`

**Interfaces:**
- `DeviceService.createManagedDevice(Device device): DeviceSimpleItem` becomes the only creation path used by both the PC controller and mobile portal.
- `MiniAdminDeviceCreateRequest` supplies `storeId`, device identifiers, status, protocol fields, and remark to the mobile portal.

- [x] **Step 1: Add a focused DTO with the PC device-form fields**

```java
@Data
public class MiniAdminDeviceCreateRequest {
    private Long storeId;
    private String deviceCode;
    private String deviceName;
    private String deviceType;
    private String deviceRole;
    private String deviceStatus;
    private String protocolType;
    private String firmwareVersion;
    private String remark;
}
```

- [x] **Step 2: Move defaulting, normalization, store validation, and persistence into `DeviceService.createManagedDevice`**

```java
DeviceSimpleItem createManagedDevice(Device device);
```

It must require a real `storeId`, generate `D` plus 16 UUID characters when `deviceCode` is blank, default `deviceType` to `washer`, `deviceRole` to `main`, and `deviceStatus` to `offline`, then return `getSimpleDeviceById(createdId)`.

- [x] **Step 3: Make `POST /api/devices` delegate to the shared service**

```java
return ApiResponse.success("created", deviceService.createManagedDevice(device));
```

- [x] **Step 4: Verify the backend compiles before adding mobile routes**

Run: `mvn -q -f .\backend\pom.xml -DskipTests package`

### Task 2: Expose scoped mobile-admin endpoints

**Files:**
- Modify: `backend/src/main/java/com/washer/backend/service/MiniAdminPortalService.java`
- Modify: `backend/src/main/java/com/washer/backend/service/impl/MiniAdminPortalServiceImpl.java`
- Modify: `backend/src/main/java/com/washer/backend/controller/MiniAdminPortalController.java`
- Test: `backend/src/test/java/com/washer/backend/controller/MiniAdminPortalControllerTest.java`

**Interfaces:**
- `POST /api/mini-admin/devices` accepts `MiniAdminDeviceCreateRequest` and returns `DeviceSimpleItem`.
- `GET /api/mini-admin/rankings?scope=day|month|total&limit=1..100` returns the existing `AdminRankingDurationItem` rows filtered to users who have completed a wash in the caller's stores during that ranking period.
- `GET /api/mini-admin/ranking-adjustments?page=1&size=10&scope=...&keyword=...` returns only scoped users' existing adjustment records.
- `POST /api/mini-admin/ranking-adjustments/set` accepts `AdminRankingDisplayAdjustmentCreateRequest` and writes through `RankingDisplayAdjustmentService.setAdjustment`.
- `DELETE /api/mini-admin/ranking-adjustments/{id}` removes a scoped adjustment through `RankingDisplayAdjustmentService.deleteAdjustment`.

- [x] **Step 1: Add a controller test for device creation delegation**

```java
when(miniAdminPortalService.createDevice(context, request)).thenReturn(expected);
ApiResponse<DeviceSimpleItem> result = controller.createDevice("admin-token", request);
assertThat(result.getData()).isSameAs(expected);
verify(miniAdminPortalService).createDevice(context, request);
```

- [x] **Step 2: Add portal-service methods and authenticated controller routes**

The controller must call `miniAdminAuthService.requireContext(token)` first. Route parameters must be clamped to page `>= 1`, size `1..100`, and ranking limit `1..100` in the service.

- [x] **Step 3: Enforce store and ranking-user scope in the portal service**

```java
Store store = getAccessibleStore(context, request.getStoreId());
device.setStoreId(store.getId());

Set<Long> userIds = resolveAccessibleRankingUserIds(context, scope);
if (!userIds.contains(request.getUserId())) {
    throw new IllegalArgumentException("无权调整该用户的排行榜时长");
}
```

For adjustment lists, add a `pageAdjustmentsByUserIds(...)` service method so filtering happens in the database query before pagination.

- [x] **Step 4: Run controller and ranking service tests**

Run: `mvn -q -f .\backend\pom.xml -Dtest=MiniAdminPortalControllerTest,RankingDisplayAdjustmentServiceTest test`

### Task 3: Add mobile API methods and pages

**Files:**
- Modify: `apis/admin.ts`
- Modify: `app.json`
- Create: `pages-admin/ranking-management/index.ts`
- Create: `pages-admin/ranking-management/index.wxml`
- Create: `pages-admin/ranking-management/index.scss`
- Create: `pages-admin/ranking-management/index.json`
- Modify: `pages-admin/devices/index.ts`
- Modify: `pages-admin/devices/index.wxml`
- Modify: `pages-admin/devices/index.scss`
- Modify: `pages-admin/home/index.ts`

**Interfaces:**
- The mobile API wrapper sends admin-token-authenticated calls to the scoped routes above.
- The devices page presents an `新增设备` command and creates a device only for the chosen accessible store.
- The ranking page provides day/month/total tabs, ranking rows, a final display-minutes form, scoped adjustment history, and deletion confirmation.

- [x] **Step 1: Add typed mobile API wrappers**

```ts
export const createMiniAdminDevice = (_data: IObject) =>
  request<IObject>('POST', '/api/mini-admin/devices', _data);

export const getMiniAdminRankings = (_params?: IObject) =>
  request<IObject[]>('GET', '/api/mini-admin/rankings', _params);
```

Add matching wrappers for adjustment paging, set, and deletion.

- [x] **Step 2: Add a device-creation drawer with PC-equivalent fields**

The drawer must require a selected store, device name, and optional device number. It exposes type, role, initial status, protocol, firmware, and remark, then calls `createMiniAdminDevice` and reloads from `getMiniAdminDevices`.

- [x] **Step 3: Implement the ranking management page**

The page must initialize the session, load ranking rows and the current adjustment page for the selected scope, preserve one source of truth from API responses, and reload both datasets after a successful save or delete. The minutes form represents the final displayed duration, matching the PC `/set` behavior.

- [x] **Step 4: Add store-tier dashboard navigation**

```ts
{ key: 'rankingManagement', title: '排行榜管理', desc: '设置本店用户展示时长', icon: '/assets/icons/tab-ranking-active.png' }
```

Map it to `/pages-admin/ranking-management/index`. Do not expose it in platform or franchisee mobile quick actions.

### Task 4: Verify quality and behavior

**Files:**
- Modify: `docs/superpowers/plans/2026-09-12-mini-admin-device-ranking-management.md`

- [x] **Step 1: Run mini-program TypeScript checks and tests**

Run: `npm run check`

- [x] **Step 2: Run the complete backend test suite and package build**

Run: `mvn -q -f .\backend\pom.xml test`

Run: `mvn -q -f .\backend\pom.xml -DskipTests package`

- [x] **Step 3: Perform static whitespace validation**

Run: `git diff --check`

- [x] **Step 4: Mark completed checklist items and summarize the required CloudBase deployment**

The backend must be rebuilt and deployed to `washer-api`; the mini-program must be recompiled before the new mobile routes are tested.

## Self-Review

Spec coverage: Task 1 makes PC and mobile device creation share one service and one table. Task 2 supplies mobile-authenticated, store-scoped backend routes and reuses the PC ranking services. Task 3 exposes both features only in the store-manager mobile workspace. Task 4 covers compilation, tests, and deployment requirements.

Placeholder scan: no implementation step relies on an unspecified endpoint, field, or permission decision.

Type consistency: all mobile create and ranking requests use the DTOs and endpoint names defined in Tasks 1 and 2.
