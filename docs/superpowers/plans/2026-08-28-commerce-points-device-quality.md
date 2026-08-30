# Commerce, Points, Device, and Quality Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace public card/voucher mocks with order and verification flows, complete point redemption, add simulated and provider-backed device control modes, and make local checks repeatable.

**Architecture:** Keep the existing Spring Boot and MyBatis-Plus data model. External payment, voucher verification, fulfillment, and device commands are expressed as explicit gateway interfaces; local simulation is selected only through configuration. Point redemption is a transactional stock-and-points debit that records an auditable fulfillment order.

**Tech Stack:** Spring Boot 3, Java 17, MyBatis-Plus, MySQL, WeChat Mini Program TypeScript, Node.js test runner, TypeScript.

## Global Constraints

- Local development remains on `127.0.0.1:18080`; physical-device deployment requires an explicitly configured HTTPS endpoint.
- External providers are never treated as successful unless their adapter returns a confirmed result.
- Simulation is configuration-controlled and must not be the default production behavior.
- All `page`, `size`, and `limit` query values are bounded globally to prevent unbounded queries.
- New behavior has focused backend unit tests and participates in repeatable project checks.

---

### Task 1: Provider contracts and card/voucher lifecycle

**Files:**
- Create: `backend/src/main/java/com/washer/backend/integration/commerce/*`
- Modify: `backend/src/main/java/com/washer/backend/controller/MiniCardController.java`
- Modify: `backend/src/main/resources/application.yml`
- Test: `backend/src/test/java/com/washer/backend/service/CardCommerceServiceTest.java`

**Interfaces:**
- Produces `CardPaymentGateway#createPayment(CardPaymentRequest)` and `VoucherVerificationGateway#verify(VoucherVerificationRequest)`.
- Produces pending card-purchase orders; only confirmed payment or verified voucher creates user cards.

- [ ] Write tests proving that pending payment creates no cards and confirmed callbacks are idempotent.
- [ ] Create simulation and provider-placeholder adapters behind `washer.commerce.mode`.
- [ ] Replace `mock-purchase` and `mock-redeem` with create/query/confirm purchase APIs and verify-and-redeem voucher APIs.
- [ ] Run focused tests and `mvn test`.

### Task 2: Point redemption transaction and mini-program interaction

**Files:**
- Create: `backend/src/main/java/com/washer/backend/entity/PointRedemptionOrder.java`
- Create: `backend/src/main/java/com/washer/backend/mapper/PointRedemptionOrderMapper.java`
- Create: `backend/src/main/java/com/washer/backend/service/PointRedemptionService.java`
- Create: `backend/src/main/java/com/washer/backend/service/impl/PointRedemptionServiceImpl.java`
- Create: `sql/migrations/014_point_redemption_order.sql`
- Modify: `backend/src/main/java/com/washer/backend/controller/MiniPointMallController.java`
- Modify: `apis/points-mall.ts`, `pages/points-mall/index.ts`, `pages/points-mall/index.wxml`
- Test: `backend/src/test/java/com/washer/backend/service/PointRedemptionServiceTest.java`

**Interfaces:**
- Consumes `PointRedemptionFulfillmentGateway#fulfill(PointRedemptionOrder)`.
- Produces `POST /point-mall/redemptions` and `GET /point-mall/redemptions?userId=`.

- [ ] Create a migration with order, user/product indexes, and unique order number.
- [ ] Lock product and user rows, verify sale window, stock, per-user limit, and points before changing balances.
- [ ] Decrease stock and points atomically, create an order, then apply simulation or provider fulfillment status.
- [ ] Add user confirmation and redemption feedback to the mini-program.
- [ ] Run focused tests and TypeScript checks.

### Task 3: Device gateway modes

**Files:**
- Create: `backend/src/main/java/com/washer/backend/integration/device/*`
- Modify: `backend/src/main/java/com/washer/backend/service/DeviceService.java`
- Modify: `backend/src/main/java/com/washer/backend/service/impl/DeviceServiceImpl.java`
- Modify: `backend/src/main/resources/application.yml`
- Test: `backend/src/test/java/com/washer/backend/service/DeviceGatewayServiceTest.java`

**Interfaces:**
- Produces `DeviceControlGateway#start(DeviceCommand)` and `#stop(DeviceCommand)`.
- Simulated mode records success without an HTTP request; provider mode posts the configured command payload and accepts only explicit success codes.

- [ ] Define command/result records and vendor properties (`vendor`, `base-url`, `start-path`, `stop-path`, `api-key-header`, `api-key`).
- [ ] Use a gateway router selected by `washer.device.mode=simulated|provider`.
- [ ] Change device start/stop paths to call the selected gateway before persisting state.
- [ ] Add tests for simulation, provider failure, and provider success.

### Task 4: Global query limits and reproducible checks

**Files:**
- Create: `backend/src/main/java/com/washer/backend/config/QueryParameterValidationInterceptor.java`
- Modify: `backend/src/main/java/com/washer/backend/config/WebCorsConfig.java`
- Modify: `package.json`, `package-lock.json`
- Create: `scripts/check-miniapp.mjs`, `test/miniapp-project.test.mjs`
- Test: `backend/src/test/java/com/washer/backend/config/QueryParameterValidationInterceptorTest.java`

**Interfaces:**
- Enforces `page` in `1..100000`, and `size`/`limit` in `1..100` for every HTTP request containing those keys.
- Produces root commands `npm run check:miniapp`, `npm run test:miniapp`, and `npm run check`.

- [ ] Write interceptor tests for valid, zero, non-numeric, and excessive values.
- [ ] Register the interceptor without changing CORS behavior.
- [ ] Add root TypeScript dependency and lockfile, compile the mini-program in a deterministic command, and validate declared page paths with Node tests.
- [ ] Run backend tests, mini-program checks, PC checks, and production build.

## Self-Review

- Card purchase, voucher verification, point redemption, device mode selection, query limits, and quality checks each have an implementation task.
- Provider-facing behavior is defined by named Java interfaces and configuration keys; no external vendor response is fabricated in provider mode.
- All cross-task names are declared in the relevant interface blocks.

## Execution Handoff

This plan is being executed inline in the current session because the requested scope is an implementation request.
