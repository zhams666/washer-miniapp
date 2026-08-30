# Points Mall Admin Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Let operators configure and publish point-exchange products in the admin console, then expose only eligible products to the mini-program points mall.

**Architecture:** Add a standalone `point_mall_product` aggregate instead of coupling point products to recharge or membership products. The backend owns publishing eligibility from status, stock, and scheduled availability; the Vue console administers those fields, and the mini-program consumes the public catalog endpoint.

**Tech Stack:** Spring Boot 3, MyBatis-Plus, MySQL, Vue 3, Element Plus, TypeScript, WeChat Mini Program.

## Global Constraints

- Keep existing user point balances in `user_info.points` unchanged.
- This release covers product publishing and catalog display only; it does not deduct points, create redemption orders, arrange delivery, or perform refunds.
- Products use pure-points pricing; no cash surcharge is introduced.
- Only published, in-stock products within their configured availability window may reach the mini-program.
- Product images are configured as existing HTTPS/image URLs; no new media storage subsystem is introduced.

---

### Task 1: Persist And Serve Point-Mall Products

**Files:**
- Create: `sql/migrations/013_point_mall_product.sql`
- Create: `backend/src/main/java/com/washer/backend/entity/PointMallProduct.java`
- Create: `backend/src/main/java/com/washer/backend/mapper/PointMallProductMapper.java`
- Create: `backend/src/main/java/com/washer/backend/service/PointMallProductService.java`
- Create: `backend/src/main/java/com/washer/backend/controller/AdminPointMallProductController.java`
- Create: `backend/src/main/java/com/washer/backend/controller/MiniPointMallController.java`
- Modify: `backend/src/main/java/com/washer/backend/config/DatabaseMigrationRunner.java`
- Test: `backend/src/test/java/com/washer/backend/service/PointMallProductServiceTest.java`

**Interfaces:**
- Consumes: `POST /api/admin/point-mall/products`, `PUT /api/admin/point-mall/products/{id}`, `PATCH /api/admin/point-mall/products/{id}/status`, and `GET /api/admin/point-mall/products`.
- Produces: `GET /point-mall/products`, returning only published products whose stock is positive and whose availability window contains the request time.

- [ ] **Step 1: Write the failing service test**

```java
@Test
void listPublishedProducts_excludesUnpublishedExpiredAndSoldOutProducts() {
    PointMallProduct available = product("可兑换洗车券", 1, 3, now.minusHours(1), now.plusHours(1));
    when(mapper.selectList(any())).thenReturn(List.of(available));

    assertThat(service.listPublishedProducts()).extracting(PointMallProduct::getTitle)
        .containsExactly("可兑换洗车券");
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -f backend/pom.xml test -Dtest=PointMallProductServiceTest`

Expected: FAIL because the point-mall product service does not exist.

- [ ] **Step 3: Implement schema and API**

```sql
CREATE TABLE IF NOT EXISTS `point_mall_product` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `title` VARCHAR(100) NOT NULL,
  `description` VARCHAR(500) DEFAULT NULL,
  `cover_image` VARCHAR(500) DEFAULT NULL,
  `product_type` VARCHAR(20) NOT NULL DEFAULT 'wash_service',
  `points_price` INT NOT NULL,
  `stock_total` INT NOT NULL DEFAULT 0,
  `limit_per_user` INT NOT NULL DEFAULT 0,
  `effective_time` DATETIME DEFAULT NULL,
  `expire_time` DATETIME DEFAULT NULL,
  `status` TINYINT NOT NULL DEFAULT 0,
  `sort_order` INT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`)
);
```

The public query applies `status = 1`, `stock_total > 0`, availability-window filters, then sorts by `sort_order ASC, id DESC`.

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -f backend/pom.xml test -Dtest=PointMallProductServiceTest`

Expected: PASS and only publishable products are returned.

### Task 2: Build The Admin Product Publishing Console

**Files:**
- Create: `admin-web/src/types/point-mall.ts`
- Create: `admin-web/src/api/point-mall.ts`
- Create: `admin-web/src/views/points-mall/PointMallProductPage.vue`
- Modify: `admin-web/src/router/index.ts`
- Modify: `admin-web/src/layout/AdminLayout.vue`
- Test: `admin-web` production build via `npm run build`

**Interfaces:**
- Consumes: the administrative endpoints from Task 1 using `PointMallProduct`, `PointMallProductPage`, and `PointMallProductQuery` types.
- Produces: Route `/point-mall` and sidebar entry “积分商城”.

- [ ] **Step 1: Write the client API**

```ts
export const fetchPointMallProducts = (params: PointMallProductQuery) =>
  http.get<PointMallProductPage>('/api/admin/point-mall/products', { params });

export const savePointMallProduct = (product: PointMallProduct) =>
  product.id
    ? http.put<PointMallProduct>(`/api/admin/point-mall/products/${product.id}`, product)
    : http.post<PointMallProduct>('/api/admin/point-mall/products', product);
```

- [ ] **Step 2: Build table, filters, form, and status actions**

```vue
<el-table-column prop="title" label="商品名称" min-width="180" />
<el-table-column prop="pointsPrice" label="兑换积分" min-width="110" />
<el-table-column prop="stockTotal" label="库存" min-width="90" />
<el-table-column label="状态" min-width="100">
  <template #default="{ row }">
    <el-tag :type="row.status === 1 ? 'success' : 'info'">
      {{ row.status === 1 ? '已上架' : '已下架' }}
    </el-tag>
  </template>
</el-table-column>
```

Validate title, positive integer point price and stock, product type, non-negative per-user limit, and an end time later than its start time. The publish action calls the status API instead of deleting historical product records.

- [ ] **Step 3: Build the admin app**

Run: `npm run build`

Expected: exit code `0` and Vite emits the production bundle.

### Task 3: Render The Published Catalog In The Mini Program

**Files:**
- Create: `apis/points-mall.ts`
- Modify: `pages/points-mall/index.ts`
- Modify: `pages/points-mall/index.wxml`
- Modify: `pages/points-mall/index.scss`
- Test: mini-program TypeScript compilation via `npx tsc --noEmit`

**Interfaces:**
- Consumes: `GET /point-mall/products` from Task 1.
- Produces: product cards containing the configured title, description, cover image, type tag, and points price.

- [ ] **Step 1: Add the typed catalog API**

```ts
export const getPointMallProducts = async (): Promise<PointMallProduct[]> => {
  const { code, data } = await GET<PointMallProduct[]>('/point-mall/products');
  return code === 0 && Array.isArray(data) ? data : [];
};
```

- [ ] **Step 2: Load the catalog whenever the page shows**

```ts
onShow() {
  this.loadPoints();
  this.loadProducts();
},
```

- [ ] **Step 3: Compile mini-program sources**

Run: `npx tsc --noEmit`

Expected: successful compilation with no points-mall errors.
