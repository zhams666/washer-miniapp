# Mini Admin Diagnostic Logging Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make mobile admin home loading failures diagnosable from the mini-program console and CloudBase Run logs without exposing administrator tokens or credentials.

**Architecture:** Each mini-program API request receives a generated trace ID in `X-Washer-Trace-Id`. The mobile admin home records the failed stage, trace ID, endpoint-safe error summary, and shows those values in a diagnostic modal. The backend logs request failures globally and logs the mini-admin overview lifecycle under the same trace ID.

**Tech Stack:** WeChat Mini Program TypeScript, Spring Boot 3, SLF4J, CloudBase Run logs, JUnit 5, Mockito.

## Global Constraints

- Never log `X-Washer-Admin-Token`, CloudBase API keys, WeChat login codes, passwords, or request bodies.
- Keep the existing API response envelope and authentication behavior unchanged.
- The diagnostic keywords must be stable: `mini_admin_home_load_failed`, `mini_admin_operation_overview_started`, `mini_admin_operation_overview_completed`, `mini_admin_operation_overview_failed`, and `api_request_failed`.
- The visible diagnostic text must be limited to phase, trace ID, and a sanitized error summary.

---

### Task 1: Propagate a safe request trace ID from the mini-program

**Files:**
- Modify: `typings/interface.d.ts`
- Modify: `utils/container-request.ts`

**Interfaces:**
- `ResponseData<T>` gains optional `traceId?: string`.
- `apiRequest` sends `X-Washer-Trace-Id` and includes that same ID in both successful envelopes and rejected errors.

- [x] **Step 1: Add an optional response trace field**

```ts
export interface ResponseData<T = any> {
  code: number;
  data: T;
  traceId?: string;
}
```

- [x] **Step 2: Generate an ID with the `WASHER_` prefix for every API request**

```ts
const traceId = `WASHER_${Date.now()}_${Math.random().toString(36).slice(2, 8)}`;
```

- [x] **Step 3: Add the trace header and log cloud-container transport failures without request bodies**

```ts
console.error('miniapp_api_transport_failed', { traceId, method, path, message });
```

- [x] **Step 4: Verify TypeScript compilation in the mini-program quality check**

### Task 2: Add mobile admin home diagnostics

**Files:**
- Modify: `pages-admin/home/index.ts`

**Interfaces:**
- Console event `mini_admin_home_load_failed` includes `phase`, `traceId`, `selectedStoreId`, and sanitized `message`.
- A non-blocking user-visible modal provides the phase and trace ID for log lookup.

- [x] **Step 1: Track whether profile validation or overview loading failed**
- [x] **Step 2: Extract a trace ID from backend error envelopes or transport errors**
- [x] **Step 3: Show a diagnostic modal with the safe summary and write the structured console event**
- [x] **Step 4: Verify the modified mini-program files pass `npm run check`**

### Task 3: Add CloudBase Run backend diagnostics

**Files:**
- Modify: `backend/src/main/java/com/washer/backend/common/GlobalExceptionHandler.java`
- Modify: `backend/src/main/java/com/washer/backend/controller/MiniAdminPortalController.java`
- Test: `backend/src/test/java/com/washer/backend/controller/MiniAdminPortalControllerTest.java`

**Interfaces:**
- `GET /api/mini-admin/operation/overview` writes start, completion, and failure events that include only trace ID, staff ID, store ID, and date.
- Global exception handling writes `api_request_failed` including request method, path, trace ID, exception type, and message.

- [x] **Step 1: Write a controller unit test that verifies a successful overview request returns the service result**
- [x] **Step 2: Log the overview request lifecycle while preserving its response body and status behavior**
- [x] **Step 3: Log globally handled failures with a trace header and no sensitive headers**
- [x] **Step 4: Run the focused controller test and full backend test suite**

### Task 4: Verify and document the log lookup procedure

**Files:**
- Modify: `docs/superpowers/plans/2026-09-12-mini-admin-diagnostic-logging.md`

- [x] **Step 1: Run `npm run check` at the repository root**
- [x] **Step 2: Run `mvn -f .\\backend\\pom.xml test` and `mvn -f .\\backend\\pom.xml -DskipTests package`**
- [x] **Step 3: Document the exact console and CloudBase Run log keywords for the user**

## Diagnostic Lookup Procedure

1. Rebuild the mini-program, open the mobile admin home, and reproduce the failure. The failure modal displays the stage, a `WASHER_...` diagnostic ID, and a sanitized reason.
2. In the WeChat DevTools console, search `mini_admin_home_load_failed`. If the request cannot reach CloudBase Run, also search `miniapp_api_transport_failed`.
3. In CloudBase Run service `washer-api` logs, search the exact `WASHER_...` diagnostic ID. Do not search or copy the administrator token.
4. Interpret backend matches as follows:
   - `api_request_failed`: the request was rejected or an unhandled backend exception occurred; inspect its `exception` and `message` fields.
   - `mini_admin_operation_overview_started` without `mini_admin_operation_overview_completed`: overview data loading entered the backend and failed before completion.
   - `mini_admin_operation_overview_failed`: inspect its exception message to identify the database table, column, or query that requires correction.
5. If there is no CloudBase Run log for the displayed diagnostic ID, the failure occurred before the request reached the backend. Check the DevTools console event and confirm that the current mini-program build points to the deployed `washer-api` service.
