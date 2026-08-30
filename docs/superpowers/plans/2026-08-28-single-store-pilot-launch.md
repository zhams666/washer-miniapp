# Single-store Pilot Launch Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Turn the current self-service car-wash mini program into a controlled, one-store production pilot with real payment and verified device control.

**Architecture:** The mini program, Spring Boot API, and MySQL run in a production environment. The API, not the mini program, owns payment confirmation, order state, and device commands; the physical washer reports its state back through a vendor gateway.

**Tech Stack:** WeChat Mini Program TypeScript, Spring Boot 3.2, MyBatis-Plus, MySQL 8, Vue/Vite, WeChat Pay API v3, HTTPS reverse proxy, vendor device gateway.

## Global Constraints

- Pilot one store with one to three physically tested bays for 7 to 14 days before expanding.
- Use one authenticated individual-business mini program AppID throughout the client, API, and payment configuration.
- Production API requests use HTTPS only. `127.0.0.1`, LAN addresses, and disabled domain checks are development-only.
- Payment callbacks and device callbacks are verified server-side; the client cannot decide a payment, wallet, or device state.
- Real device control requires a vendor protocol and a manual emergency-stop process.
- Test users, test money, and test devices stay separate from production data.

---

## Current-Code Assessment

| Area | Current capability | Pilot decision |
| --- | --- | --- |
| User journey | Store selection, QR-to-bay parsing, queueing, wallet, cards, orders, washing state, and order details exist | Use as the pilot core journey |
| Operations | Mini-program merchant portal and PC admin support stores, devices, orders, users, finance, and asset changes | Restrict to headquarters and pilot-store staff |
| Login | `code -> openId` and `getPhoneNumber` exchange code exists | Enable only after entity verification and AppID alignment |
| Payments | WeChat Pay v3 prepay, query, and notification implementations exist; payment defaults to disabled | Validate a real small recharge end-to-end |
| Devices | Device routes and client calls are named `mockStart` / `mockStop` | **Launch blocker: replace with vendor gateway** |
| Deployment | `config/url.ts` and backend defaults use `127.0.0.1` | **Launch blocker: deploy HTTPS API** |
| AppID | `project.config.json` has `wxb83ca5cce97b3680`; `config/enums.ts` and backend default use `wxe60fa81e79d6636c` | **Launch blocker: unify the real AppID** |
| Demo routes | Card purchase/redemption and merchant logins include mock routes | Hide or replace before production |

### Task 1: Establish the production identity and network

**Files:**
- Modify: `project.config.json`
- Modify: `config/enums.ts`
- Modify: `config/url.ts`
- Modify: `backend/src/main/resources/application.yml`
- Create: `backend/src/main/resources/application-prod.yml`
- Create: `docs/operations/production-environment.md`

**Interfaces:**
- Consumes: authenticated individual-business mini program AppID, API domain, TLS certificate, and production MySQL.
- Produces: one AppID and `https://api.<business-domain>` used consistently by all production components.

- [ ] **Step 1: Verify the owner and AppID**

Confirm in the WeChat public platform that the production mini program is authenticated under the individual business. Record its AppID and set that exact value in the project configuration, `BaseEnum.APP_ID`, `WECHAT_MINIAPP_APP_ID`, and WeChat Pay AppID. Do not reuse the two current example/conflicting IDs.

- [ ] **Step 2: Prepare isolated production infrastructure**

Provision a production server, MySQL 8 database, HTTPS API domain, and valid TLS certificate. Keep MySQL private, store daily backups for seven days, and perform one restoration test before accepting real money.

- [ ] **Step 3: Set production secrets and host configuration**

Store `WASHER_DB_URL`, database credentials, `WECHAT_MINIAPP_SECRET`, `WECHAT_PAY_*`, certificate paths, and callback URL in server environment variables or a protected secret store. Bind Spring Boot to an internal address and expose only HTTPS through a reverse proxy. Never commit merchant private keys, API v3 keys, or mini-program secrets.

- [ ] **Step 4: Register the domain in WeChat**

Add the HTTPS API domain as a request legal domain in the mini program console and as an upload domain if avatar uploads remain enabled. Add operations staff as experience members; do not use disabled URL checks to work around production configuration.

- [ ] **Step 5: Verify from a physical phone**

Use the experience version to open home and store pages and call `GET /ping`. Accept this task only when there is no domain error, production logs expose no secrets, and all client/API/payment AppIDs match.

### Task 2: Enable compliant login and real payment

**Files:**
- Modify: `pages/mine/index.ts`
- Modify: `pages/mine/index.wxml`
- Modify: `backend/src/main/java/com/washer/backend/controller/CostomerController.java`
- Modify: `backend/src/main/java/com/washer/backend/controller/MiniWalletController.java`
- Modify: `backend/src/main/java/com/washer/backend/service/impl/WechatPayServiceImpl.java`
- Test: `backend/src/test/java/com/washer/backend/controller/CostomerControllerTest.java`
- Test: `backend/src/test/java/com/washer/backend/service/WechatPayServiceTest.java`

**Interfaces:**
- Consumes: verified mini program, associated WeChat Pay merchant account, payment certificates, API v3 key, and HTTPS notification URL.
- Produces: real phone authorization, signed prepay parameters, verified callbacks, query recovery, and traceable wallet records.

- [ ] **Step 1: Complete platform materials**

Finish WeChat authentication, relevant service categories, privacy protection instructions, customer-service details, and user agreement. Associate the individual-business merchant account with the same mini program AppID. If the phone-verification component remains unavailable, register and authenticate the mini program itself under the individual-business entity rather than only binding the entity to the operator's public-platform account.

- [ ] **Step 2: Declare minimal personal-data use**

State that location finds nearby stores and enforces the 100-metre queue rule; phone number supports login, orders, and customer contact; avatar upload occurs only after a user actively changes their profile. Do not require phone authorization before a user starts a flow that needs it.

- [ ] **Step 3: Remove production identity shortcuts**

Set `WECHAT_MINIAPP_MOCK_LOGIN_ENABLED=false`. Remove or hide history-phone direct login and all `mock-*` merchant identities in production. The server accepts a WeChat-issued code and exchanges it itself; it must not trust a client-declared openId or phone number.

- [ ] **Step 4: Configure and exercise WeChat Pay**

Enable WeChat Pay with the shared AppID, merchant ID, merchant serial number, private key, API v3 key, platform certificate, and public notification URL. Perform a lowest-allowed real recharge and check prepay, `wx.requestPayment`, callback signature validation, wallet credit, and payment query.

- [ ] **Step 5: Verify recovery cases**

Test cancellation, delayed callback, and duplicate callback. A cancelled payment remains unpaid; a delayed callback can be recovered by query; duplicate notification adds wallet value once only. Reconcile recharge order, payment transaction, and wallet transaction by amount, user, and store.

### Task 3: Integrate the physical washer safely

**Files:**
- Create: `backend/src/main/java/com/washer/backend/service/DeviceGateway.java`
- Create: `backend/src/main/java/com/washer/backend/service/impl/<Vendor>DeviceGateway.java`
- Create: `backend/src/main/java/com/washer/backend/dto/device/DeviceCommandResult.java`
- Create: `backend/src/main/java/com/washer/backend/controller/DeviceCallbackController.java`
- Modify: `backend/src/main/java/com/washer/backend/controller/DeviceController.java`
- Modify: `backend/src/main/java/com/washer/backend/service/impl/WashOrderServiceImpl.java`
- Modify: `sql/migrations/014_device_command_log.sql`
- Test: `backend/src/test/java/com/washer/backend/service/<Vendor>DeviceGatewayTest.java`

**Interfaces:**
- Consumes: written vendor API/protocol, device identifiers, device status callbacks, test machine, and emergency-stop procedure.
- Produces: `start(deviceId, orderNo)`, `stop(deviceId, orderNo, reason)`, signed callbacks, and immutable device-command logs.

- [ ] **Step 1: Obtain the vendor contract**

Get the vendor's written command and callback contract: unique device identifier, start, stop, heartbeat, status, fault code, timeout, authentication, retry, callback signing, and emergency-stop procedure. Do not connect a real washer to the current mock endpoints without this information.

- [ ] **Step 2: Define idempotent command logging**

Each start and stop command creates a `commandId` and records device, order, reason, request time, sanitized request summary, vendor response, final status, and failure reason. The same order and command request returns the existing outcome and cannot start a washer twice.

- [ ] **Step 3: Gate device start by server state**

Start only when the order exists, the user payment or wallet debit has succeeded, the device is idle, and the bay has no running order. Change the order to running only after vendor confirmation. On start failure, reverse/restore the pre-deduction according to the payment policy and lock the bay for staff review.

- [ ] **Step 4: Make every stop recoverable**

User finish, time expiry, staff stop, gateway timeout, and hardware fault use the same stop path. Complete billing only after stop confirmation. If stop cannot be confirmed, make the bay unavailable and alert staff; never issue a blind restart.

- [ ] **Step 5: Run the physical test matrix**

Record results for normal start, duplicate start, normal stop, duplicate stop, offline device, duplicate callback, paid-but-start-failed, network recovery, and manual emergency stop. For each, reconcile command log, order state, wallet amount, and bay state. Any paid user without an actionable recovery route blocks the pilot.

### Task 4: Prepare safe operations and a controlled pilot

**Files:**
- Modify: `pages/store-detail/index.ts`
- Modify: `pages/washing/index.ts`
- Modify: `pages/pay/index.ts`
- Modify: `pages-admin/login/index.ts`
- Modify: `pages-admin/devices/index.ts`
- Modify: `admin-web/src/views/devices/DeviceListPage.vue`
- Create: `docs/operations/store-staff-runbook.md`
- Create: `docs/operations/pilot-daily-checklist.md`
- Create: `docs/operations/go-live-acceptance.md`

**Interfaces:**
- Consumes: validated payment/device states, pilot staff list, customer-service contact, and real store/device QR codes.
- Produces: staff-operable failure handling, daily reconciliation, and an explicit expand-or-hold decision.

- [ ] **Step 1: Narrow the pilot journey**

Prioritize scan wash, store selection, recharge/balance, orders, and customer service. Keep points mall, rankings, invitations, franchise leads, demo card purchase, and nonessential campaign entry points out of the pilot's primary navigation until the wash loop is reliable.

- [ ] **Step 2: Add recovery actions for every customer failure**

For paid-but-not-started, interrupted wash, unable-to-finish, offline bay, insufficient balance, invalid QR, and queue-out-of-range states, show order/device reference, refresh, customer-service contact, and the next available staff action.

- [ ] **Step 3: Limit merchant permissions**

Give clerks read-only access to their store's devices and orders; give the store manager validated stop and exception actions; reserve wallet/card changes and cross-store data for headquarters. No production account may use `mock-platform`, `mock-store`, or equivalent credentials.

- [ ] **Step 4: Operate one store first**

Create the real store record, address, coordinates, pricing, business hours, service contact, and QR codes for only one to three tested bays. For the first three days, invite staff and known users only, then test peak-time concurrency before public promotion.

- [ ] **Step 5: Reconcile and apply the expansion gate**

At daily close, reconcile paid transactions, recharge credits, wallet ledger, wash orders, device starts/stops, and manual adjustments. Expand only after seven consecutive days with at least 99% paid-to-successful-start rate, zero unresolved money discrepancies, closed exception records, completed staff drills, and completed daily reconciliation. Otherwise hold at one store.

## Recommended Business Sequence

1. This week: verify the individual-business mini program, unify the AppID, and provision HTTPS production infrastructure.
2. Next: obtain the washer vendor protocol and test machine. This is the decisive current blocker for a paid wash.
3. In parallel: associate WeChat Pay, configure callback verification, and validate phone authorization.
4. Test only the scan-to-wash loop in one store before investing in points, franchise, rankings, or multi-store campaigns.
5. Expand bays and stores only after the controlled pilot meets its seven-day gate.

## Self-Review

- Spec coverage: deployment, AppID, authentication, phone authorization, payment, physical device control, permissions, customer recovery, pilot operations, and expansion criteria are covered.
- Placeholder scan: no deferred implementation placeholders exist. The vendor protocol is a required external dependency and its format is intentionally not invented.
- Interface consistency: device start/stop are keyed by `deviceId` and `orderNo`; payment confirmation comes from verified server callback or query; the same states drive customer and staff recovery.
