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

部署前，在云托管服务的环境变量中配置以下值：

```text
WASHER_DB_URL=jdbc:mysql://<host>:3306/washer?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=true
WASHER_DB_USERNAME=<test database user>
WASHER_DB_PASSWORD=<test database password>
WECHAT_MINIAPP_APP_ID=wxb83ca5cce97b3680
WECHAT_MINIAPP_SECRET=<mini program secret>
WECHAT_MINIAPP_MOCK_LOGIN_ENABLED=false
WECHAT_PAY_ENABLED=false
```

在微信开发者工具的“云开发 -> 云托管”中创建测试服务后，选择“本地代码部署”，上传 `backend/` 目录，Dockerfile 路径填写 `Dockerfile`，服务端口填写 `8080`。部署完成后，通过服务 HTTPS 地址访问 `/ping`；返回 `code=0` 后，再将该 HTTPS 地址加入小程序 request 合法域名并配置到 `config/url.ts` 的测试环境。

云开发免费环境只用于测试。不要在其中保存唯一的生产数据库、生产支付密钥或正式用户资金；到期前必须导出测试数据库和部署配置。
