# CloudBase 30-Day Completion Sprint Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Use the remaining CloudBase trial as a production-like test environment to finish and verify the one-store wash loop without putting real customer money or sole business data at risk.

**Architecture:** Keep the existing native mini program, Spring Boot API, and relational data model. Deploy the Spring Boot API to CloudBase Run and attach a test MySQL instance; do not rewrite the current system into cloud functions or cloud document database during the 30-day window. The mini program calls the CloudBase HTTPS endpoint, while payment and device credentials remain server-side.

**Tech Stack:** WeChat Mini Program, CloudBase Run, Docker, Spring Boot 3.2, MySQL 8, WeChat Pay API v3, vendor device gateway.

## Global Constraints

- The CloudBase trial environment is named `washer-test` and contains no production customer funds or exclusive production data.
- All mini-program, backend, and payment configuration use AppID `wxb83ca5cce97b3680`.
- `WECHAT_MINIAPP_MOCK_LOGIN_ENABLED=false` and `WECHAT_PAY_ENABLED=false` remain the default for shared test builds until real payment acceptance begins.
- Do not migrate business tables to the CloudBase document database during this sprint; the existing SQL and MyBatis model remain the source of truth.
- Export the test database and download required logs before the free environment expires.

---

### Task 1: Create a CloudBase test deployment

**Files:**
- Create: `backend/Dockerfile`
- Create: `backend/.dockerignore`
- Create: `backend/src/main/resources/application-cloudbase.yml`
- Modify: `config/url.ts`
- Create: `docs/operations/cloudbase-test-deployment.md`

**Interfaces:**
- Consumes: CloudBase environment ID, test MySQL connection information, and CloudBase Run HTTPS service URL.
- Produces: a Spring Boot service responding to `GET /ping` through CloudBase Run.

- [ ] **Step 1: Create the test environment**

In WeChat Developer Tools, create or select one cloud environment named `washer-test`, bind it to AppID `wxb83ca5cce97b3680`, and record its environment ID. Do not place production credentials in this environment.

- [ ] **Step 2: Package the existing backend**

Add a Dockerfile under `backend/` that builds the Maven project, exposes the service port configured by `SERVER_PORT`, and starts `washer-backend` with the `cloudbase` Spring profile. Add `.dockerignore` entries for `target/`, `.git/`, local logs, and database dump files.

- [ ] **Step 3: Provision test data storage**

Use a CloudBase-supported MySQL resource only if the selected trial includes it; otherwise use a separate test MySQL instance. Import the existing SQL migrations in numeric order from `sql/migrations/001.sql` through `013_point_mall_product.sql`. Store the resulting connection string only in CloudBase service environment variables.

- [ ] **Step 4: Deploy CloudBase Run**

Deploy the `backend/` folder as a CloudBase Run service using the Dockerfile. Configure `SERVER_PORT`, database variables, `WECHAT_MINIAPP_APP_ID=wxb83ca5cce97b3680`, and the test profile. Verify the generated HTTPS URL returns a successful `/ping` response.

- [ ] **Step 5: Point the development build at the test service**

Set `REQUEST_URL` in `config/url.ts` to the CloudBase Run HTTPS endpoint for the cloud-test build. Register the same HTTPS endpoint as the request legal domain in the mini-program console. Verify home, store list, login, and order list on a physical phone.

### Task 2: Spend days 1-10 on the core test loop

**Files:**
- Modify: `pages/home/index.ts`
- Modify: `pages/store-detail/index.ts`
- Modify: `pages/washing/index.ts`
- Modify: `pages/order/index.ts`
- Modify: `backend/src/test/java/com/washer/backend/controller/DeviceControllerTest.java`
- Create: `docs/operations/core-loop-test-cases.md`

**Interfaces:**
- Consumes: deployed test API, seeded test store, test bays, and experience-member accounts.
- Produces: a documented pass/fail result for scan, login, recharge simulation, start, finish, and order reconciliation.

- [ ] **Step 1: Seed one non-production store and devices**

Create one clearly named test store and one to three test devices with non-production QR codes. Do not reuse QR codes that are attached to a real washer until the vendor integration is validated.

- [ ] **Step 2: Verify the happy path on devices**

Use an experience member to scan a test QR code, select a bay, log in, create an order, enter the washing page, end the order, and view order details. Capture the generated user ID, order number, device ID, and status log for each test.

- [ ] **Step 3: Verify failure states**

Test invalid QR, offline device, occupied bay, insufficient balance, duplicate order creation, repeated finish request, lost network, and queue range failure. Each case must retain a visible order state and a recoverable staff action.

- [ ] **Step 4: Repair only core-loop defects**

Prioritize defects that prevent a user from reaching a terminal result: successful wash, clear refusal before charging, or a recoverable exception. Defer ranking, points mall, invitation, franchise, and new marketing work until all core-loop cases pass.

### Task 3: Spend days 11-20 on real service dependencies

**Files:**
- Modify: `pages/mine/index.ts`
- Modify: `backend/src/main/java/com/washer/backend/controller/CostomerController.java`
- Modify: `backend/src/main/java/com/washer/backend/controller/MiniWalletController.java`
- Modify: `backend/src/main/java/com/washer/backend/controller/DeviceController.java`
- Create: `docs/operations/vendor-device-contract.md`
- Create: `docs/operations/payment-test-record.md`

**Interfaces:**
- Consumes: verified individual-business mini program, WeChat Pay merchant account, device-vendor protocol, and a physical test washer.
- Produces: verified login, a payment test record, and a device-integration design with an explicit stop/exception procedure.

- [ ] **Step 1: Complete the account prerequisites**

Confirm the mini program is authenticated under the individual business, configure privacy declarations for location, phone number, and avatar, and associate the WeChat Pay merchant account with AppID `wxb83ca5cce97b3680`.

- [ ] **Step 2: Verify phone authorization**

Use the production-style experience build to obtain a user-authorized phone code and exchange it server-side. Do not activate history-phone or manually supplied phone login as a production alternative.

- [ ] **Step 3: Validate payment without relying on the trial environment**

Use a small controlled payment only after the callback URL is stable and server-side signature verification is enabled. Reconcile prepay result, callback, payment transaction, recharge order, and wallet entry. Treat CloudBase as the test host; duplicate this configuration on a retained paid environment before public sales.

- [ ] **Step 4: Obtain and test the physical-device contract**

Collect the vendor API for start, stop, heartbeat, status, fault, retry, authentication, callback signature, and emergency stop. Implement no real command until one test washer has passed start, stop, double-submit, offline, network-recovery, and emergency-stop tests.

### Task 4: Spend days 21-30 on release decision and exit preparation

**Files:**
- Create: `docs/operations/cloudbase-exit-checklist.md`
- Create: `docs/operations/pilot-acceptance-report.md`
- Create: `docs/operations/production-cutover.md`

**Interfaces:**
- Consumes: test database export, CloudBase service configuration, payment/device results, and pilot staff review.
- Produces: a backed-up test environment and a concrete decision to retain paid CloudBase, move to another production host, or continue testing.

- [ ] **Step 1: Run a staff acceptance drill**

Have a non-developer staff member complete the customer path and then resolve paid-not-started, device offline, and user cannot finish cases. Accept only when the staff member can locate the order, identify the device, execute or escalate a safe stop, and record the outcome.

- [ ] **Step 2: Back up the trial environment**

Export the test MySQL database, save the deployed image or Dockerfile revision, capture CloudBase environment-variable names without secret values, and download service logs required for unresolved defects. Verify the database export restores into a separate empty test database.

- [ ] **Step 3: Choose the retained host before expiry**

Before day 25, choose either a paid CloudBase Run/MySQL environment or a separately managed production server and MySQL database. Recreate the HTTPS API, database, secrets, monitoring, and backup process there; do not wait for the trial to expire before deciding.

- [ ] **Step 4: Apply the public-launch gate**

Public release requires: matching AppID everywhere, production HTTPS domain, verified payment callback, device gateway test pass, staff runbook, daily reconciliation, and seven-day controlled pilot metrics. If any item is missing, publish only an experience build to invited testers.

## Self-Review

- Spec coverage: AppID standardization, actual CloudBase integration status, existing Spring Boot architecture, 30-day prioritization, payment/device risks, and trial-expiry exit are covered.
- Placeholder scan: no temporary implementation placeholders are used; the vendor protocol and chosen retained host are explicit external decisions required before physical operation.
- Type consistency: the plan keeps the existing Spring Boot and MySQL contracts, so no new client data model or cloud-function API is invented during the time-limited sprint.
