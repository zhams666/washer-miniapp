# CloudBase PostgREST Refactor Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox syntax for tracking.

**Goal:** Preserve the existing mini-program HTTP endpoints while replacing JDBC/MyBatis in the cloudbase deployment profile with CloudBase PostgreSQL HTTP API access.

**Architecture:** CloudBase services call PostgREST through a typed server-side client using an environment ID and service API Key. Local development keeps MyBatis/JDBC. A cloudbase-only Mapper adapter translates the existing safe MyBatis-Plus CRUD and wrapper operations into PostgREST requests, avoiding a risky one-time rewrite of all 33 mapper consumers. Multi-table writes are then incrementally replaced with PostgreSQL RPC functions so financial state remains atomic.

**Tech Stack:** Java 17, Spring Boot 3.2 RestClient, Jackson, JUnit 5, WireMock, CloudBase PostgreSQL/PostgREST.

## Global Constraints

- Keep AppID wxb83ca5cce97b3680.
- API Keys, AppSecrets, database passwords, and connection strings must never be committed or returned to clients.
- The cloudbase profile must create no DataSource, SqlSessionFactory, or mapper bean and must not require WASHER_PG_*.
- Existing /api/** routes and ApiResponse envelopes remain unchanged.
- Local MySQL plus its migration runner remain unchanged.
- Every multi-table write uses one PostgreSQL RPC instead of multiple REST writes.

---

### Task 1: Add CloudBase HTTP transport

**Files:**
- Create: backend/src/main/java/com/washer/backend/cloudbase/CloudBasePgProperties.java
- Create: backend/src/main/java/com/washer/backend/cloudbase/CloudBasePgClient.java
- Create: backend/src/main/java/com/washer/backend/cloudbase/CloudBasePgException.java
- Create: backend/src/test/java/com/washer/backend/cloudbase/CloudBasePgClientTest.java
- Modify: backend/pom.xml
- Modify: backend/src/main/resources/application-cloudbase.yml

**Interfaces:**
- Consumes: CLOUDBASE_ENV_ID and CLOUDBASE_API_KEY.
- Produces: select(table, query), insert(table, body), update(table, filter, body), delete(table, filter), and rpc(function, body).

- [ ] **Step 1: Write the failing WireMock tests**

    @Test
    void selectUsesCloudBasePostgrestUrlAndServerAuthorization() {
        client.select("store", new LinkedMultiValueMap<>(Map.of("id", List.of("eq.1"))));
        verify(getRequestedFor(urlEqualTo("/v1/rdb/rest/store?id=eq.1"))
            .withHeader("Authorization", equalTo("Bearer server-key")));
    }

    @Test
    void unsuccessfulPostgrestResponseBecomesCloudBasePgException() {
        stubFor(get("/v1/rdb/rest/store")
            .willReturn(status(403).withBody("{\"message\":\"permission denied\"}")));
        assertThatThrownBy(() -> client.select("store", new LinkedMultiValueMap<>()))
            .isInstanceOf(CloudBasePgException.class)
            .hasMessageContaining("permission denied");
    }

- [ ] **Step 2: Verify the test fails**

Run: mvn -B -ntp -f backend/pom.xml -Dtest=CloudBasePgClientTest test

Expected: FAIL because CloudBasePgClient does not exist.

- [ ] **Step 3: Implement the client**

Build every URI from https://<envId>.api.tcloudbasegateway.com/v1/rdb/rest/. Attach Authorization: Bearer <apiKey>, Accept: application/json, and Content-Type: application/json on writes. Map HTTP 400, 401, 403, 404, 409, and 5xx to CloudBasePgException with a sanitized CloudBase response message. Support TCB_ENV_ID, CLOUDBASE_APIKEY, and TCB_API_KEY only as fallback aliases. Add WireMock as a test dependency.

- [ ] **Step 4: Verify the test passes**

Run: mvn -B -ntp -f backend/pom.xml -Dtest=CloudBasePgClientTest test

Expected: PASS.

- [ ] **Step 5: Commit**

    git add backend/pom.xml backend/src/main/resources/application-cloudbase.yml backend/src/main/java/com/washer/backend/cloudbase backend/src/test/java/com/washer/backend/cloudbase
    git commit -m "feat: add CloudBase PostgreSQL HTTP client"

### Task 2: Isolate the cloudbase profile from JDBC and MyBatis

**Files:**
- Create: backend/src/main/java/com/washer/backend/config/CloudBaseHttpConfiguration.java
- Create: backend/src/test/java/com/washer/backend/config/CloudBaseProfileContextTest.java
- Modify: backend/src/main/java/com/washer/backend/WasherBackendApplication.java
- Modify: backend/src/main/java/com/washer/backend/config/MybatisPlusConfig.java
- Modify: backend/src/main/resources/application-cloudbase.yml

**Interfaces:**
- Consumes: CloudBasePgProperties and CloudBasePgClient.
- Produces: a cloudbase application context without DataSource, SqlSessionFactory, or mapper beans.

- [ ] **Step 1: Write the failing profile test**

    @SpringBootTest(properties = {
        "spring.profiles.active=cloudbase",
        "cloudbase.pg.env-id=washer-test-example",
        "cloudbase.pg.api-key=server-key"
    })
    class CloudBaseProfileContextTest {
        @Autowired ApplicationContext context;

        @Test
        void doesNotCreateJdbcOrMybatisInfrastructure() {
            assertThat(context.getBeansOfType(DataSource.class)).isEmpty();
            assertThat(context.getBeansOfType(SqlSessionFactory.class)).isEmpty();
        }
    }

- [ ] **Step 2: Verify it fails**

Run: mvn -B -ntp -f backend/pom.xml -Dtest=CloudBaseProfileContextTest test

Expected: FAIL because application-cloudbase.yml currently requires JDBC properties.

- [ ] **Step 3: Implement profile boundaries**

Apply @Profile("!cloudbase") to JDBC/MyBatis configuration and mapper scanning. Remove spring.datasource, mybatis-plus, and washer.database-type from application-cloudbase.yml. Register RestClient only under cloudbase, with a 10-second connection timeout and 20-second read timeout.

- [ ] **Step 4: Verify it passes**

Run: mvn -B -ntp -f backend/pom.xml -Dtest=CloudBaseProfileContextTest test

Expected: PASS.

- [ ] **Step 5: Commit**

    git add backend/src/main/java/com/washer/backend backend/src/main/resources/application-cloudbase.yml backend/src/test/java/com/washer/backend/config
    git commit -m "refactor: isolate cloudbase HTTP profile from JDBC"

### Task 3: Add a cloudbase-only Mapper adapter

**Files:**
- Create: backend/src/main/java/com/washer/backend/cloudbase/mapper/CloudBaseMapperFactory.java
- Create: backend/src/main/java/com/washer/backend/cloudbase/mapper/CloudBaseMapperInvocationHandler.java
- Create: backend/src/main/java/com/washer/backend/cloudbase/mapper/CloudBaseWrapperTranslator.java
- Create: backend/src/test/java/com/washer/backend/cloudbase/mapper/CloudBaseMapperInvocationHandlerTest.java
- Modify: backend/src/main/java/com/washer/backend/config/CloudBaseHttpConfiguration.java
- Modify: backend/src/main/java/com/washer/backend/WasherBackendApplication.java

**Interfaces:**
- Consumes: CloudBasePgClient and existing BaseMapper entity metadata.
- Produces: cloudbase-profile mapper beans for all 33 existing mapper interfaces. Supported operations are selectById, selectOne, selectList, selectCount, insert, updateById, update, deleteById, and delete; unsupported SQL fragments fail closed.

- [ ] **Step 1: Write the failing repository tests**

    @Test
void activeStoresUsesEnabledFilterAndStableSort() {
        mapper.selectList(new LambdaQueryWrapper<Store>().eq(Store::getIsEnabled, true).orderByAsc(Store::getId));
        verify(client).select(eq("store"), query("is_enabled", "eq.true", "order", "id.asc"));
    }

    @Test
void selectOneReturnsNullWhenPostgrestReturnsNoRows() {
        stubRows("user_info", "[]");
        assertThat(mapper.selectOne(new LambdaQueryWrapper<UserInfo>().eq(UserInfo::getOpenid, "openid-1"))).isNull();
    }

- [ ] **Step 2: Verify the tests fail**

Run: mvn -B -ntp -f backend/pom.xml -Dtest=CloudBaseTableRepositoryTest test

Expected: FAIL because the cloudbase mapper adapter does not exist.

- [ ] **Step 3: Implement explicit reads**

Use table names and fields exactly as created by sql/postgresql/001_cloudbase_init.sql. Translate only generated LambdaQueryWrapper conditions eq, ne, gt, ge, lt, le, in, isNull, isNotNull, orderByAsc, orderByDesc, and last limit/offset. Reject apply, exists, nested OR groups, and unknown raw fragments with CloudBasePgException. Preserve entity ID mapping, generated IDs, null behavior, page size, and ordering.

- [ ] **Step 4: Verify core tests**

Run: mvn -B -ntp -f backend/pom.xml -Dtest=CloudBaseMapperInvocationHandlerTest,DeviceControllerTest test

Expected: PASS.

- [ ] **Step 5: Commit**

git add backend/src/main/java/com/washer/backend/cloudbase/mapper backend/src/main/java/com/washer/backend/config/CloudBaseHttpConfiguration.java backend/src/main/java/com/washer/backend/WasherBackendApplication.java backend/src/test/java/com/washer/backend/cloudbase/mapper
git commit -m "feat: adapt MyBatis CRUD operations to CloudBase PostgREST"

### Task 4: Make customer, order, wallet, and card writes atomic

**Files:**
- Create: sql/postgresql/002_cloudbase_business_rpc.sql
- Create: backend/src/main/java/com/washer/backend/cloudbase/repository/CloudBaseWalletRepository.java
- Create: backend/src/main/java/com/washer/backend/cloudbase/repository/CloudBaseWashRepository.java
- Create: backend/src/test/java/com/washer/backend/cloudbase/repository/CloudBaseWashRepositoryTest.java
- Modify: backend/src/main/java/com/washer/backend/service/impl/WashOrderServiceImpl.java
- Modify: backend/src/main/java/com/washer/backend/controller/MiniWalletController.java
- Modify: backend/src/main/java/com/washer/backend/controller/MiniCardController.java
- Modify: backend/src/main/java/com/washer/backend/controller/CostomerController.java

**Interfaces:**
- Consumes: CloudBasePgClient.rpc(function, body).
- Produces: create_wash_order, pay_wash_order_with_wallet, pay_wash_order_with_card, cancel_wash_order, create_wallet_recharge_order, and apply_wallet_recharge RPCs.

- [ ] **Step 1: Write the failing RPC contract test**

    @Test
    void walletPaymentCallsSingleAtomicRpc() {
        washRepository.payWithWallet(12L, 7L, new BigDecimal("10.00"));
        verify(client).rpc("pay_wash_order_with_wallet", Map.of(
            "p_order_id", 12L, "p_user_id", 7L, "p_amount", new BigDecimal("10.00")));
    }

- [ ] **Step 2: Verify it fails**

Run: mvn -B -ntp -f backend/pom.xml -Dtest=CloudBaseWashRepositoryTest test

Expected: FAIL because the repository and RPC functions do not exist.

- [ ] **Step 3: Implement RPC functions and repositories**

Each RPC uses SECURITY DEFINER, sets search_path to public, checks request role service_role, locks affected rows with FOR UPDATE, and raises errors on insufficient balance, unavailable card count, or invalid order state. Apply the SQL through the CloudBase SQL migration mechanism before enabling the corresponding endpoints.

- [ ] **Step 4: Verify rollback behavior**

Run: mvn -B -ntp -f backend/pom.xml -Dtest=CloudBaseWashRepositoryTest,PointRedemptionServiceTest test

Expected: PASS, including no partial wallet transaction after insufficient-balance rejection.

- [ ] **Step 5: Commit**

    git add sql/postgresql/002_cloudbase_business_rpc.sql backend/src/main/java/com/washer/backend/cloudbase backend/src/main/java/com/washer/backend/service backend/src/main/java/com/washer/backend/controller backend/src/test/java/com/washer/backend/cloudbase
    git commit -m "feat: use CloudBase RPC for wash and wallet writes"

### Task 5: Migrate memberships, points, assets, and administrative writes

**Files:**
- Modify: sql/postgresql/002_cloudbase_business_rpc.sql
- Create: backend/src/main/java/com/washer/backend/cloudbase/repository/CloudBaseMembershipRepository.java
- Create: backend/src/main/java/com/washer/backend/cloudbase/repository/CloudBasePointMallRepository.java
- Create: backend/src/main/java/com/washer/backend/cloudbase/repository/CloudBaseMiniAdminRepository.java
- Create: backend/src/test/java/com/washer/backend/cloudbase/repository/CloudBaseMembershipRepositoryTest.java
- Create: backend/src/test/java/com/washer/backend/cloudbase/repository/CloudBasePointMallRepositoryTest.java
- Modify: backend/src/main/java/com/washer/backend/service/MembershipService.java
- Modify: backend/src/main/java/com/washer/backend/service/impl/PointRedemptionServiceImpl.java
- Modify: backend/src/main/java/com/washer/backend/service/impl/MiniAdminAssetServiceImpl.java
- Modify: backend/src/main/java/com/washer/backend/service/impl/AdminWalletRechargeServiceImpl.java
- Modify: backend/src/main/java/com/washer/backend/service/impl/AdminWalletRefundServiceImpl.java
- Modify: backend/src/main/java/com/washer/backend/service/impl/AdminWalletFineServiceImpl.java

**Interfaces:**
- Produces: redeem_points_product, purchase_membership, adjust_user_card, admin_wallet_recharge, admin_wallet_refund, and admin_wallet_fine RPCs.

- [ ] **Step 1: Write the failing idempotency test**

    @Test
    void pointRedemptionNeverCreatesDuplicateOrderForSameRequestNumber() {
        service.redeem(new PointRedemptionRequest("REQ-100", 5L, 9L));
        service.redeem(new PointRedemptionRequest("REQ-100", 5L, 9L));
        verify(repository, times(1)).redeem("REQ-100", 5L, 9L);
    }

- [ ] **Step 2: Verify it fails**

Run: mvn -B -ntp -f backend/pom.xml -Dtest=CloudBaseMembershipRepositoryTest,CloudBasePointMallRepositoryTest,PointRedemptionServiceTest test

Expected: FAIL because CloudBase write repositories do not exist.

- [ ] **Step 3: Implement write RPCs**

Each RPC accepts an idempotency request number, checks the existing unique record before balances change, returns mutated records, and raises errors instead of returning partial work. Replace corresponding cloudbase-profile mapper calls only.

- [ ] **Step 4: Verify it passes**

Run: mvn -B -ntp -f backend/pom.xml -Dtest=CloudBaseMembershipRepositoryTest,CloudBasePointMallRepositoryTest,PointRedemptionServiceTest,PointMallProductServiceTest test

Expected: PASS.

- [ ] **Step 5: Commit**

    git add sql/postgresql/002_cloudbase_business_rpc.sql backend/src/main/java/com/washer/backend/cloudbase backend/src/main/java/com/washer/backend/service backend/src/test/java/com/washer/backend/cloudbase
    git commit -m "feat: migrate asset operations to CloudBase RPC"

### Task 6: Migrate reporting and remove cloudbase mapper use

**Files:**
- Create: backend/src/main/java/com/washer/backend/cloudbase/repository/CloudBaseReportingRepository.java
- Create: sql/postgresql/003_cloudbase_reporting_rpc.sql
- Create: backend/src/test/java/com/washer/backend/cloudbase/repository/CloudBaseReportingRepositoryTest.java
- Modify: backend/src/main/java/com/washer/backend/service/impl/AdminDashboardServiceImpl.java
- Modify: backend/src/main/java/com/washer/backend/service/impl/AdminPaymentCenterServiceImpl.java
- Modify: backend/src/main/java/com/washer/backend/service/impl/MiniAdminPortalServiceImpl.java
- Modify: backend/src/main/java/com/washer/backend/controller/AdminMiniAdminPermissionController.java

**Interfaces:**
- Produces: admin_dashboard_overview, admin_payment_center, and mini_admin_portal_overview JSON reporting RPCs.

- [ ] **Step 1: Write the failing reporting contract test**

    @Test
    void dashboardOverviewUsesOneReportingRpcInsteadOfLoadingEveryOrder() {
        repository.dashboardOverview(1L, LocalDate.of(2026, 8, 30));
        verify(client).rpc(eq("admin_dashboard_overview"), anyMap());
    }

- [ ] **Step 2: Verify it fails**

Run: mvn -B -ntp -f backend/pom.xml -Dtest=CloudBaseReportingRepositoryTest test

Expected: FAIL because the reporting repository does not exist.

- [ ] **Step 3: Implement reporting RPCs and service ports**

Return JSON payloads that map directly to existing dashboard DTOs. Create cloudbase service implementations where a current service directly injects a mapper. The cloudbase package must contain no JdbcTemplate, DataSource, BaseMapper, LambdaQueryWrapper, or LambdaUpdateWrapper imports.

- [ ] **Step 4: Verify transport purity and tests**

Run: rg -n "JdbcTemplate|DataSource|BaseMapper|LambdaQueryWrapper|LambdaUpdateWrapper" backend/src/main/java/com/washer/backend/cloudbase

Expected: no output.

Run: mvn -B -ntp -f backend/pom.xml clean test

Expected: PASS.

- [ ] **Step 5: Commit**

    git add backend/src/main/java/com/washer/backend/cloudbase backend/src/main/java/com/washer/backend/service backend/src/test/java/com/washer/backend sql/postgresql/003_cloudbase_reporting_rpc.sql
    git commit -m "refactor: remove CloudBase profile dependency on JDBC"

### Task 7: Configure the service and mini-program

**Files:**
- Create: docs/cloudbase-cloudrun-deployment.md
- Modify: backend/README.md
- Modify: README.md
- Modify: config/url.ts
- Modify: utils/request.ts
- Modify: backend/src/test/java/com/washer/backend/config/CloudBaseProfileContextTest.java

**Interfaces:**
- Consumes: CloudBase Run default HTTPS service domain supplied after deployment.
- Produces: a deployment checklist with no PostgreSQL host, password, VPC, or external IP.

- [ ] **Step 1: Write the failing missing-key test**

    @Test
    void cloudbaseProfileFailsFastWhenApiKeyIsMissing() {
        assertThatThrownBy(() -> contextRunner.withPropertyValues(
            "spring.profiles.active=cloudbase", "cloudbase.pg.env-id=washer-test")
            .run(context -> context.getBean(CloudBasePgClient.class)))
            .hasMessageContaining("CLOUDBASE_API_KEY");
    }

- [ ] **Step 2: Verify it fails**

Run: mvn -B -ntp -f backend/pom.xml -Dtest=CloudBaseProfileContextTest test

Expected: FAIL until configuration validation exists.

- [ ] **Step 3: Write deployment documentation**

Document these cloud-run variables without values: SPRING_PROFILES_ACTIVE=cloudbase, CLOUDBASE_ENV_ID, CLOUDBASE_API_KEY, WECHAT_MINIAPP_APP_ID, WECHAT_MINIAPP_SECRET, WECHAT_MINIAPP_MOCK_LOGIN_ENABLED=false, WECHAT_PAY_ENABLED=false, and SERVER_PORT=8080. State that WASHER_PG_*, database password reset, external IPv4, security groups, and VPC are not used by this profile.

- [ ] **Step 4: Update mini-program configuration**

Set REQUEST_URL in config/url.ts to the published CloudBase Run HTTPS origin. Preserve wxAppId enrichment and X-Washer-Openid forwarding.

- [ ] **Step 5: Run final verification**

Run: npm test

Expected: PASS.

Run: mvn -B -ntp -f backend/pom.xml clean test

Expected: PASS.

Run: git diff --check

Expected: no output.

- [ ] **Step 6: Commit**

    git add README.md backend/README.md config/url.ts utils/request.ts docs/cloudbase-cloudrun-deployment.md backend/src/test/java/com/washer/backend/config/CloudBaseProfileContextTest.java
    git commit -m "docs: document CloudBase HTTP API deployment"

## Self-Review

- Spec coverage: Tasks 1-2 eliminate the impossible JDBC/VPC dependency. Tasks 3-6 retain endpoint behavior through explicit PostgREST reads and transactional RPC writes. Task 7 verifies deployment and mini-program routing.
- Placeholder scan: configuration names, table names, RPC names, file paths, tests, and commands are explicit. Secret values are deliberately excluded.
- Type consistency: repositories use CloudBasePgClient, atomic writes use rpc(function, body), and deployment uses CLOUDBASE_ENV_ID plus CLOUDBASE_API_KEY.

## Execution Handoff

Plan complete and saved to docs/superpowers/plans/2026-08-30-cloudbase-postgrest-refactor.md. Two execution options:

1. Subagent-Driven (recommended) - I dispatch a fresh subagent per task and review between tasks.

2. Inline Execution - Execute tasks in this session using executing-plans, with verification after each task.

Which approach?
