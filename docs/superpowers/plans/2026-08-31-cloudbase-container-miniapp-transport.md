# CloudBase Container Miniapp Transport Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Let the test mini-program call `washer-api` through `wx.cloud.callContainer`, without registering the CloudBase default domain in the Mini Program server-domain allowlist.

**Architecture:** The mini-program initializes the associated CloudBase environment once at launch. A single request transport chooses CloudBase private container calls for the test build, while retaining an explicit localhost switch for desktop development. Existing direct `wx.request` API helpers are migrated onto this transport so login, store browsing, devices, franchise contact, and mini-admin requests use the same private route.

**Tech Stack:** WeChat Mini Program TypeScript, `wx.cloud.init`, `wx.cloud.callContainer`, Node test runner, TypeScript 5.8.

## Global Constraints

- Use `CLOUDBASE_ENV_ID = washer-test-d6gax6t26237e4265` and service name `washer-api` only in mini-program configuration; never expose the CloudBase API key or WeChat AppSecret.
- CloudBase default domains are not entered in request/uploadFile legal-domain fields.
- Preserve `wxAppId` request enrichment and `X-Washer-Openid` / `X-Washer-Admin-Token` headers.
- GET request data must be encoded into the request path query string because Spring controller parameters use `@RequestParam`.
- `wx.uploadFile` cannot use `callContainer`; CloudBase test mode must return a clear error for avatar uploads until storage-based upload is implemented.
- Local desktop mode must remain configurable without changing API modules.

---

### Task 1: Define and test the CloudBase transport

**Files:**
- Create: `utils/container-request.ts`
- Create: `test/container-request.test.mjs`
- Modify: `config/url.ts`
- Modify: `app.ts`
- Modify: `utils/request.ts`

**Interfaces:**
- Consumes: `API_TRANSPORT`, `CLOUDBASE_ENV_ID`, `CLOUDBASE_SERVICE_NAME`, `LOCAL_REQUEST_URL` from `config/url.ts`.
- Produces: `containerRequest<T>(method, path, data, headers): Promise<ResponseData<T>>` and `buildQueryPath(path, data): string`.

- [ ] **Step 1: Write a failing path test**

```js
assert.equal(
  buildQueryPath('/costomer/getOpenId', { code: 'abc 123', wxAppId: 'wx123' }),
  '/costomer/getOpenId?code=abc%20123&wxAppId=wx123'
);
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `node --test test/container-request.test.mjs`

Expected: FAIL because `utils/container-request.ts` does not exist.

- [ ] **Step 3: Add CloudBase transport configuration and initialization**

```ts
export const API_TRANSPORT = 'cloudbase' as const;
export const CLOUDBASE_ENV_ID = 'washer-test-d6gax6t26237e4265';
export const CLOUDBASE_SERVICE_NAME = 'washer-api';
export const LOCAL_REQUEST_URL = 'http://127.0.0.1:18080';
```

In `app.ts`, call `wx.cloud.init({ env: CLOUDBASE_ENV_ID })` before `ensureCurrentUser`. In `containerRequest`, use `wx.cloud.callContainer` with `service: CLOUDBASE_SERVICE_NAME`, `path`, `method`, JSON headers, and an 8-second timeout. For GET, use `buildQueryPath`; for POST, pass `data` as the request body.

- [ ] **Step 4: Route existing GET and POST helpers through the transport**

```ts
const response = await apiRequest<T>('GET', _url, { ..._data, wxAppId: BaseEnum.APP_ID }, buildJsonHeaders());
if (response.code === 0) return response;
throw response;
```

Keep `TENCENT_MAP_GET` as direct `wx.request`; it does not target the washer backend. In cloudbase mode, make `UPLOAD` reject with `new Error('Avatar upload is unavailable in the CloudBase test transport')` instead of silently attempting the disallowed default domain.

- [ ] **Step 5: Run static and unit verification**

Run: `npm run check:miniapp && node --test test/container-request.test.mjs`

Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add app.ts config/url.ts utils/container-request.ts utils/request.ts test/container-request.test.mjs
git commit -m "feat: route miniapp API calls through CloudBase container"
```

### Task 2: Migrate API modules that bypass the shared request utility

**Files:**
- Modify: `apis/costomer.ts`
- Modify: `apis/admin.ts`
- Modify: `apis/store.ts`
- Modify: `apis/device.ts`
- Modify: `apis/franchise.ts`
- Modify: `backend/README.md`

**Interfaces:**
- Consumes: `apiRequest<T>(method, path, data, headers)` from `utils/container-request.ts`.
- Produces: Existing exported API functions with unchanged return types and error behavior.

- [ ] **Step 1: Write failing source-level regression tests**

```js
for (const file of ['apis/costomer.ts', 'apis/admin.ts', 'apis/store.ts', 'apis/device.ts', 'apis/franchise.ts']) {
  assert.doesNotMatch(readFileSync(file, 'utf8'), /wx\.request\(/);
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `node --test test/container-request.test.mjs`

Expected: FAIL because the listed modules still call `wx.request` directly.

- [ ] **Step 3: Replace each direct backend call**

```ts
const response = await apiRequest<IObject>('POST', '/api/franchise-contacts', {
  ...payload,
  wxAppId: BaseEnum.APP_ID,
});
if (response.code !== 0) throw response;
return response.data || {};
```

Use the same helper for `requestSilently` in `apis/costomer.ts`, preserving its logging and silent fallback. Change `uploadAvatar` to reject in cloudbase transport with the same explicit test-mode error defined in Task 1.

- [ ] **Step 4: Document the test access flow**

Add a README section stating that CloudBase test builds use `wx.cloud.callContainer`, require the AppID to be associated with the CloudBase environment, and do not require the default `*.run.tcloudbase.com` domain in Mini Program server-domain settings. State that formal release still requires a custom HTTPS domain or a fully CloudBase-native upload flow.

- [ ] **Step 5: Run full mini-program verification**

Run: `npm run check`

Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add apis/costomer.ts apis/admin.ts apis/store.ts apis/device.ts apis/franchise.ts backend/README.md test/container-request.test.mjs
git commit -m "refactor: use CloudBase transport across miniapp APIs"
```

## Self-Review

1. **Spec coverage:** Task 1 initializes the associated environment and routes shared GET/POST calls privately; Task 2 removes all backend-facing bypasses so login and major page APIs do not fall back to the rejected default domain. The remaining avatar upload is explicitly failed rather than misleadingly sent to an unavailable domain.
2. **Placeholder scan:** No TBD/TODO placeholders or unspecified error handling remain. Each code task has a concrete API, expected behavior, and verification command.
3. **Type consistency:** `apiRequest<T>` returns `Promise<ResponseData<T>>` for both shared utilities and direct API modules. Configuration names are identical across initialization and the transport.
