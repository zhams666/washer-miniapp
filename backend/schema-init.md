# Schema Init Guide

这份文档用于把当前项目的数据库和最小后端在本地跑起来。

目标是完成这 4 件事：

1. 创建数据库 `washer`
2. 导入现有 SQL 表结构
3. 配置后端连接数据库
4. 启动后端并验证接口可用

适用环境：

- Windows
- MySQL 8.x
- JDK 17
- Maven 3.9+

---

## 1. 你现在手上有哪些文件

数据库相关文件在项目根目录：

- [001.sql](C:/Users/10415/Desktop/项目资料/自助洗车/washer-main/sql/migrations/001.sql)
- [002_schema_upgrade.sql](C:/Users/10415/Desktop/项目资料/自助洗车/washer-main/sql/migrations/002_schema_upgrade.sql)
- [003_schema_enhance.sql](C:/Users/10415/Desktop/项目资料/自助洗车/washer-main/sql/migrations/003_schema_enhance.sql)

后端项目目录在：

- [backend](C:/Users/10415/Desktop/项目资料/自助洗车/washer-main/backend)

后端数据库配置文件在：

- [application.yml](C:/Users/10415/Desktop/项目资料/自助洗车/washer-main/backend/src/main/resources/application.yml)

---

## 2. 前提准备

开始前请先确认你本地已经装好：

1. MySQL 8
2. JDK 17
3. Maven

你可以先在 PowerShell 里执行下面几条命令检查：

```powershell
mysql --version
java -version
mvn -v
```

如果任意一条报“找不到命令”，说明对应软件还没装好，或者环境变量没配好。

---

## 3. 创建数据库 washer

注意：后端配置的是连接 `washer` 这个数据库，但**不会自动创建数据库**。  
所以这一步你需要手动执行一次。

### 方式 A：用 MySQL 命令行

先打开 PowerShell，执行：

```powershell
mysql -uroot -p
```

输入你的 MySQL root 密码后，进入 MySQL 控制台。

然后执行：

```sql
CREATE DATABASE washer DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

再执行：

```sql
SHOW DATABASES;
```

如果你能看到 `washer`，说明数据库已经创建成功。

### 方式 B：用 Navicat / DataGrip / MySQL Workbench

如果你平时不用命令行，也可以：

1. 打开数据库工具
2. 连接本地 MySQL
3. 新建数据库
4. 数据库名填写 `washer`
5. 字符集选择 `utf8mb4`
6. 排序规则选择 `utf8mb4_unicode_ci`

---

## 4. 导入 SQL 表结构

数据库创建完之后，要按顺序导入这 3 个 SQL 文件：

1. [001.sql](C:/Users/10415/Desktop/项目资料/自助洗车/washer-main/sql/migrations/001.sql)
2. [002_schema_upgrade.sql](C:/Users/10415/Desktop/项目资料/自助洗车/washer-main/sql/migrations/002_schema_upgrade.sql)
3. [003_schema_enhance.sql](C:/Users/10415/Desktop/项目资料/自助洗车/washer-main/sql/migrations/003_schema_enhance.sql)

顺序不要乱。

### 方式 A：PowerShell 命令行导入

先切到项目根目录：

```powershell
cd C:\Users\10415\Desktop\项目资料\自助洗车\washer-main
```

然后依次执行：

```powershell
mysql -uroot -p washer < sql/migrations/001.sql
mysql -uroot -p washer < sql/migrations/002_schema_upgrade.sql
mysql -uroot -p washer < sql/migrations/003_schema_enhance.sql
```

每执行一条都会要求你输入密码。

如果你不想每次输密码，也可以使用：

```powershell
mysql -uroot -p你的密码 washer < sql/migrations/001.sql
```

但不建议长期这样做，因为密码会直接暴露在命令里。

### 方式 B：在数据库工具里执行

如果你用 Navicat、Workbench、DataGrip 等工具：

1. 打开 `washer` 数据库
2. 新建查询窗口
3. 把 [001.sql](C:/Users/10415/Desktop/项目资料/自助洗车/washer-main/sql/migrations/001.sql) 内容复制进去执行
4. 再执行 [002_schema_upgrade.sql](C:/Users/10415/Desktop/项目资料/自助洗车/washer-main/sql/migrations/002_schema_upgrade.sql)
5. 最后执行 [003_schema_enhance.sql](C:/Users/10415/Desktop/项目资料/自助洗车/washer-main/sql/migrations/003_schema_enhance.sql)

---

## 5. 检查表是否导入成功

导入完成后，进入 MySQL：

```powershell
mysql -uroot -p washer
```

执行：

```sql
SHOW TABLES;
```

你应该能看到类似这些核心表：

- `user_info`
- `store`
- `device`
- `wash_order`
- `wallet_transaction`
- `refund_order`
- `payment_transaction`

你也可以单独检查：

```sql
DESC user_info;
DESC store;
DESC device;
DESC wash_order;
```

如果这些表都存在，说明导库成功。

---

## 6. 配置后端数据库连接

后端配置文件在：

- [application.yml](C:/Users/10415/Desktop/项目资料/自助洗车/washer-main/backend/src/main/resources/application.yml)

你至少要检查下面 3 项：

```yaml
spring:
  datasource:
    url: jdbc:mysql://127.0.0.1:3306/washer?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&useSSL=false
    username: root
    password: 123456
```

请根据你本地实际情况修改：

- 如果数据库端口不是 `3306`，改 `url`
- 如果用户名不是 `root`，改 `username`
- 如果密码不是 `123456`，改 `password`

### 最常见的本地配置示例

```yaml
spring:
  datasource:
    url: jdbc:mysql://127.0.0.1:3306/washer?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&useSSL=false
    username: root
    password: 你的MySQL密码
```

---

## 7. 启动后端

后端目录在：

- [backend](C:/Users/10415/Desktop/项目资料/自助洗车/washer-main/backend)

### 用 PowerShell 启动

先进入后端目录：

```powershell
cd C:\Users\10415\Desktop\项目资料\自助洗车\washer-main\backend
```

先编译一次：

```powershell
mvn clean package
```

如果编译成功，再启动：

```powershell
mvn spring-boot:run
```

如果启动成功，你会看到类似日志：

```text
Started WasherBackendApplication in xx.xxx seconds
```

默认端口是 `8080`。

---

## 8. 验证服务是否启动成功

### 先测 /ping

打开浏览器或 PowerShell，访问：

```text
http://localhost:8080/ping
```

或者用命令：

```powershell
curl http://localhost:8080/ping
```

预期返回：

```json
{
  "code": 0,
  "message": "ok",
  "data": {
    "message": "ok"
  }
}
```

如果这里正常，说明：

- 后端启动成功
- Web 层正常
- Spring Boot 正常工作

---

## 9. 验证数据库是否真的连通

`/ping` 只是证明服务启动，不一定证明数据库连通。  
数据库连通要进一步测 CRUD 接口。

建议按下面顺序验证。

### 9.1 新增一个用户

```powershell
curl -X POST http://localhost:8080/api/users `
  -H "Content-Type: application/json" `
  -d "{\"nickname\":\"测试用户\",\"mobile\":\"13800138000\"}"
```

如果返回成功，说明：

- `user_info` 表存在
- MyBatis-Plus 映射正常
- 数据库连接可用

### 9.2 查询用户列表

```powershell
curl "http://localhost:8080/api/users?page=1&size=10"
```

### 9.3 新增门店

```powershell
curl -X POST http://localhost:8080/api/stores `
  -H "Content-Type: application/json" `
  -d "{\"storeName\":\"南山测试店\",\"address\":\"深圳市南山区测试路100号\"}"
```

### 9.4 新增设备

这里假设上一步创建出的门店 ID 是 `1`：

```powershell
curl -X POST http://localhost:8080/api/devices `
  -H "Content-Type: application/json" `
  -d "{\"storeId\":1,\"deviceName\":\"1号洗车机\"}"
```

### 9.5 新增订单

这里假设：

- 用户 ID 是 `1`
- 门店 ID 是 `1`
- 设备 ID 是 `1`

```powershell
curl -X POST http://localhost:8080/api/orders `
  -H "Content-Type: application/json" `
  -d "{\"userId\":1,\"storeId\":1,\"deviceId\":1,\"remark\":\"最小闭环测试订单\"}"
```

### 9.6 查询订单列表

```powershell
curl "http://localhost:8080/api/orders?page=1&size=10"
```

如果以上都成功，说明第一阶段目标已经基本达成：

1. 后端能启动
2. 数据库能连通
3. 核心表能 CRUD
4. 能开始验证当前表设计和基础逻辑

---

## 10. 当前后端已提供的接口

### 健康检查

- `GET /ping`

### 用户

- `GET /api/users`
- `GET /api/users/{id}`
- `POST /api/users`
- `PUT /api/users/{id}`
- `DELETE /api/users/{id}`

### 门店

- `GET /api/stores`
- `GET /api/stores/{id}`
- `POST /api/stores`
- `PUT /api/stores/{id}`
- `DELETE /api/stores/{id}`

### 设备

- `GET /api/devices`
- `GET /api/devices/{id}`
- `POST /api/devices`
- `PUT /api/devices/{id}`
- `DELETE /api/devices/{id}`

### 订单

- `GET /api/orders`
- `GET /api/orders/{id}`
- `POST /api/orders`
- `PUT /api/orders/{id}`
- `DELETE /api/orders/{id}`

---

## 11. 常见报错与排查

### 11.1 `mysql : 无法将“mysql”项识别为命令`

原因：

- MySQL 客户端没装
- 或 MySQL `bin` 目录没加入环境变量

排查：

1. 找到 MySQL 安装目录，比如：
   `C:\Program Files\MySQL\MySQL Server 8.0\bin`
2. 把这个目录加到 Windows 的 `Path`
3. 重开 PowerShell 再试 `mysql --version`

### 11.2 `java : 无法将“java”项识别为命令`

原因：

- 没装 JDK
- 或环境变量没配

排查：

1. 安装 JDK 17
2. 配置 `JAVA_HOME`
3. 把 `%JAVA_HOME%\bin` 加到 `Path`
4. 重新打开终端执行 `java -version`

### 11.3 `mvn : 无法将“mvn”项识别为命令`

原因：

- Maven 没装
- 或环境变量没配

排查：

1. 安装 Maven
2. 配置 `MAVEN_HOME`
3. 把 `%MAVEN_HOME%\bin` 加到 `Path`
4. 执行 `mvn -v`

### 11.4 `Unknown database 'washer'`

原因：

- 你还没创建数据库

解决：

先执行：

```sql
CREATE DATABASE washer DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### 11.5 `Table 'washer.xxx' doesn't exist`

原因：

- 001 / 002 / 003 没有完整导入
- 或导入顺序错了

解决：

重新按顺序导入：

1. `001.sql`
2. `002_schema_upgrade.sql`
3. `003_schema_enhance.sql`

### 11.6 `Access denied for user`

原因：

- 用户名或密码错误
- MySQL 账号没有权限

解决：

检查 [application.yml](C:/Users/10415/Desktop/项目资料/自助洗车/washer-main/backend/src/main/resources/application.yml)：

```yaml
spring:
  datasource:
    username: root
    password: 你的密码
```

### 11.7 `Communications link failure`

原因：

- MySQL 服务没启动
- 端口不对
- 地址不对

解决：

1. 确认 MySQL 服务启动
2. 确认端口是 `3306`
3. 确认 `application.yml` 的 `url` 没写错

### 11.8 `Duplicate entry`

原因：

- 命中了唯一索引冲突
- 比如手机号、门店编码、设备编码、订单号重复

解决：

换一个值重新测试，或者检查表里是否已有同样数据。

### 11.9 启动成功但接口报 500

最常见原因：

- 表结构和实体字段不匹配
- 数据库没导完整
- 某些默认值没有传

建议检查：

1. 后端控制台日志
2. 对应表是否真的存在
3. 请求 JSON 是否包含必要字段

---

## 12. 建议的最小验证顺序

第一次启动时，建议严格按下面顺序走：

1. 先确认 `mysql --version`
2. 再确认 `java -version`
3. 再确认 `mvn -v`
4. 创建数据库 `washer`
5. 导入 `001 / 002 / 003`
6. 修改 `application.yml`
7. 启动后端
8. 访问 `/ping`
9. 新增用户
10. 新增门店
11. 新增设备
12. 新增订单

这样排错最省时间。

---

## 13. 当前阶段的边界

这份后端现在是“最小可运行后端”，不是完整业务系统。

当前已完成：

- Spring Boot 骨架
- MySQL 连接
- MyBatis-Plus 接入
- 用户 / 门店 / 设备 / 订单 基础 CRUD

当前还没做：

- 钱包
- 余额流水
- 会员
- 优惠
- 计费规则落单
- 退款流程
- 支付回调
- Redis / MQ / 定时任务

所以这套后端现在的作用是：

- 验证数据库结构能不能正常用
- 验证基础增删改查是否顺畅
- 为下一阶段复杂业务做地基

---

## 14. 下一步建议

第一阶段跑通后，建议按下面顺序继续：

1. 把前端已有 `/order/*`、`/device/*` 接口和当前后端接口对齐
2. 补 DTO 和参数校验
3. 做最小订单流程：创建订单 -> 更新状态 -> 查询详情
4. 再接钱包和余额流水

如果你需要，我下一步可以继续直接帮你做第二阶段，而且还是按“小步、可运行、少出错”的方式往下推进。
