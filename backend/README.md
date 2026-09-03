# Washer Backend

这是第一阶段的最小可运行后端，目标是：

- 项目能启动
- 能连接 MySQL 8
- 能基于现有 SQL 建库
- 能完成用户、门店、设备、订单四类核心表的基础 CRUD

## 技术栈

- Spring Boot 3.2.x
- MyBatis-Plus
- MySQL 8
- Java 17

## 目录结构

```text
backend
├─ pom.xml
└─ src
   └─ main
      ├─ java/com/washer/backend
      │  ├─ WasherBackendApplication.java
      │  ├─ common
      │  ├─ config
      │  ├─ controller
      │  ├─ entity
      │  ├─ mapper
      │  └─ service
      └─ resources
         └─ application.yml
```

## 本地运行前提

1. 安装 JDK 17
2. 安装 Maven 3.9+
3. 安装 MySQL 8
4. 创建数据库 `washer`

## 数据库导入

按顺序执行根目录下的 SQL：

按文件名顺序执行 `sql/migrations/` 下的全部迁移，当前最新为 `014_point_redemption_order.sql`。

## 启动方式

```bash
cd backend
mvn clean package
mvn spring-boot:run
```

启动后访问：

```text
GET http://127.0.0.1:18080/ping
```

返回 `code=0` 且 `data.message=ok` 说明服务启动正常。

## 外部接口配置

默认设备为模拟模式，次卡/券码为 provider 模式，积分履约为模拟模式。生产环境必须通过环境变量明确设置：

```text
# 设备：simulated 或 provider
WASHER_DEVICE_MODE=provider
WASHER_DEVICE_VENDOR=<厂商型号或协议名称>
WASHER_DEVICE_BASE_URL=https://<设备厂商网关>
WASHER_DEVICE_START_PATH=/devices/{deviceCode}/start
WASHER_DEVICE_STOP_PATH=/devices/{deviceCode}/stop
WASHER_DEVICE_API_KEY_HEADER=X-Api-Key
WASHER_DEVICE_API_KEY=<厂商密钥>
WASHER_DEVICE_SUCCESS_FIELD=code
WASHER_DEVICE_SUCCESS_VALUE=0

# 卡支付与外部券码：simulation 只用于本地；provider 等待适配器实现
WASHER_COMMERCE_MODE=provider
WASHER_COMMERCE_PROVIDER=<支付或券平台名称>

# 积分商品履约：simulation 自动完成；provider 创建待履约订单
WASHER_POINT_MALL_FULFILLMENT_MODE=provider
```

需要对接时实现以下接口并替换默认组件：

- `integration.device.DeviceControlGateway`：发送启动/停止命令，厂商明确成功后才允许更新设备状态。
- `integration.commerce.CardPaymentGateway`：创建支付单并在支付回调确认后发卡。
- `integration.commerce.VoucherVerificationGateway`：向外部平台校验券码、门店、次数和外部订单号。
- `integration.points.PointFulfillmentGateway`：发放优惠券、洗车权益或创建实物履约任务。

## CloudBase 云托管测试部署

`backend/` 已包含用于 CloudBase Run 的 `Dockerfile`。该镜像会使用 Java 17 构建 Spring Boot 服务，默认启用 `cloudbase` profile，并监听 `8080`；如果云托管注入 `PORT`，会优先监听该端口。

当前 CloudBase 测试环境使用 PostgreSQL，但免费共享实例不提供给云托管的 JDBC 直连信息。后端在 `cloudbase` profile 下会通过 CloudBase PostgreSQL 的 HTTPS REST/RPC API 访问数据库，因此不需要开放公网 IPv4、不需要安全组，也不需要 `WASHER_PG_*` 变量。

请先在 CloudBase PostgreSQL 的 SQL 控制台依次执行：

1. `sql/postgresql/001_cloudbase_init.sql`：空测试环境的完整表结构、索引、演示数据和 `updated_at` 触发器。
2. `sql/postgresql/002_cloudbase_http_rpc.sql`：用户合并与积分兑换所需的原子 RPC。
3. `sql/postgresql/003_cloudbase_user_info_columns.sql`：为已按旧版本脚本创建的 `user_info` 表补齐登录与会员字段；新环境执行最新版 `001` 后也可安全执行。

在云托管服务的环境变量中配置以下值：

```text
CLOUDBASE_ENV_ID=<云开发环境 ID，不是显示名称>
CLOUDBASE_API_KEY=<云开发 API Key，仅服务端保存>
WECHAT_MINIAPP_APP_ID=wxb83ca5cce97b3680
WECHAT_MINIAPP_MOCK_LOGIN_ENABLED=false
WECHAT_PAY_ENABLED=false
```

`CLOUDBASE_API_KEY` 是云开发控制台生成的服务端密钥，不能写入小程序代码、Git 仓库或截图。`WECHAT_MINIAPP_SECRET` 可在管理员有空时再补；缺少它时，`/ping` 可用，但微信登录换取 `openid` 的功能不可用。已在截图中出现过的 AppSecret 应立即在微信公众平台重置，然后只填入云托管环境变量。

云开发 HTTP profile 会默认关闭依赖数据库行锁的“订单超时自动完成”定时任务，避免在免费环境中执行非原子资金操作。测试期间不要打开微信支付；付款、退款、卡支付和设备正式控制仍应在对应 RPC 完成后再启用。

在微信开发者工具的“云开发 -> 云托管”中创建测试服务后，选择 Git 平台部署，目标目录填写 `backend`，Dockerfile 路径填写 `Dockerfile`，服务端口填写 `8080`。首次联调建议关闭自动部署，待 `/ping`、登录和积分兑换验证完成后再开启。

测试小程序调用此服务使用 `wx.cloud.callContainer`，要求小程序 AppID 与该云开发环境关联，服务名为 `washer-api`。不要把 `*.run.tcloudbase.com` 默认域名填入微信公众平台的 `request` 或 `uploadFile` 合法域名字段，该默认域名只用于开发测试且会被公众平台拒绝。头像上传目前仍使用 HTTP 文件上传，因此在 CloudBase 私有测试调用模式下会明确提示不可用；正式上线应改用云存储上传或绑定已备案的自定义 HTTPS 域名。

云开发免费环境只用于测试。不要在其中保存唯一的生产数据库、生产支付密钥或正式用户资金；到期前必须导出测试数据库和部署配置。
