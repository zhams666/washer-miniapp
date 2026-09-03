# CloudBase 设备状态更新修复计划

> **给开发者：** 执行本计划时按顺序完成每项任务，并保留 CloudBase 与本地 MySQL 的既有业务结果一致。

**目标：** 修复后台将设备切换为空闲、离线等非运行状态时，CloudBase HTTP 数据访问层因 MyBatis 行锁和嵌套 OR 条件导致的保存失败。

**根因：** `cancelRunningOrdersForDevice` 使用了 `limit 1 for update`，随后构造了设备 ID 与工位 ID 的嵌套 OR 条件；CloudBase HTTP Mapper 不支持这两类 MyBatis SQL 片段。

## 任务 1：将设备停用前的订单查询改为 CloudBase 兼容流程

**文件：**
- 修改：`backend/src/main/java/com/washer/backend/service/impl/WashOrderServiceImpl.java`

**步骤：**
1. 用主键读取设备，移除 `for update`。
2. 仅用门店和 `running` 状态构造数据库查询。
3. 在 Java 内根据 `deviceId` 或解析出的 `bayId` 筛选订单，再沿用现有取消订单流程。

## 任务 2：取消订单时避免 CloudBase 不支持的行锁

**文件：**
- 修改：`backend/src/main/java/com/washer/backend/service/impl/WashOrderServiceImpl.java`

**步骤：**
1. 让订单及其关联设备在 CloudBase 环境下使用主键或简单门店查询，避免产生 `FOR UPDATE` SQL 片段。
2. 保留现有状态校验、卡次释放、设备回空闲和订单状态日志行为。

## 任务 3：补充回归测试并验证构建

**文件：**
- 修改：`backend/src/test/java/com/washer/backend/cloudbase/mapper/CloudBaseMapperInvocationHandlerTest.java`

**步骤：**
1. 覆盖“门店 + 运行中”订单查询到 CloudBase 参数的翻译。
2. 运行 `mvn -f .\\backend\\pom.xml test` 与 `mvn -f .\\backend\\pom.xml -DskipTests compile`。
