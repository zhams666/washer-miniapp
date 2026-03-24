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

1. `sql/migrations/001.sql`
2. `sql/migrations/002_schema_upgrade.sql`
3. `sql/migrations/003_schema_enhance.sql`

## 启动方式

```bash
cd backend
mvn clean package
mvn spring-boot:run
```

启动后访问：

```text
GET http://localhost:8080/ping
```

返回 `code=0` 且 `data.message=ok` 说明服务启动正常。
