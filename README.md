<p align="center">
  <img width="88px" src="./assets/logo.svg" />
</p>

<h3 align="center"><b>扫码自助洗车</b> <sup><em>(Washer)</em></sup></h3>

<p align="center">基于 TypeScript 的自助洗车微信小程序。</p>

<br/>

</div>

## 特性

- 基于成熟的产品设计、UI 设计，可以直接作为实际项目使用；

- 基于微信小程序原生开发技术，搭配使用 TypeScript、Scss 开发；

- 提供洗车流程、订单、钱包、优惠卷等 16 张全流程页面模板；

- 基于微信常用 Api 二次封装、网络请求、表单验证等工具函数；

## 快速开始

```sh
# 拉取最新项目代码

git clone https://github.com/kaivanwong/washer.git

# 进入项目代码目录

cd washer

# 开发

使用 微信开发者工具 IDE 进行微信小程序开发
```

## 本地联调

本地开发统一使用以下地址：

- 后端 API：`http://127.0.0.1:18080`
- PC 管理后台：`http://127.0.0.1:18073`

准备好 JDK 17、Maven 3.9+、Node.js 20+ 和本机 MySQL 后，在 PowerShell 中运行：

```powershell
.\scripts\start-local.ps1 -DbPassword '你的 MySQL 密码'
```

脚本会在缺少前端依赖时执行 `npm ci`，随后启动 Spring Boot 后端和 PC 管理后台。日志与进程 ID 写入 `.local/`。数据库连接也支持通过 `WASHER_DB_URL`、`WASHER_DB_USERNAME`、`WASHER_DB_PASSWORD` 环境变量覆盖。

小程序请求传输配置位于 `config/url.ts`。测试云开发环境默认使用 `wx.cloud.callContainer` 调用同环境的 `washer-api`，不需要把 CloudBase 默认域名填入小程序的服务器域名；本地调试时将 `API_TRANSPORT` 改为 `local`。

## 质量检查

```powershell
npm install
npm run check
cd .\backend
mvn test
cd ..\admin-web
npm run build
```

## 演示

<table>
	<tr>
		<td><img width="100%" src="https://github.com/kaivanwong/washer/blob/main/.github/assets/home.jpg?raw=true" /></td>
		<td><img width="100%" src="https://github.com/kaivanwong/washer/blob/main/.github/assets/submit.jpg?raw=true" /></td>
		<td><img width="100%" src="https://github.com/kaivanwong/washer/blob/main/.github/assets/pay.jpg?raw=true" /></td>
		<td><img width="100%" src="https://github.com/kaivanwong/washer/blob/main/.github/assets/order.jpg?raw=true" /></td>
	</tr>
	<tr>
		<td><img width="100%" src="https://github.com/kaivanwong/washer/blob/main/.github/assets/message.jpg?raw=true" /></td>
		<td><img width="100%" src="https://github.com/kaivanwong/washer/blob/main/.github/assets/discount.jpg?raw=true" /></td>
		<td><img width="100%" src="https://github.com/kaivanwong/washer/blob/main/.github/assets/service.jpg?raw=true" /></td>
		<td><img width="100%" src="https://github.com/kaivanwong/washer/blob/main/.github/assets/no-message.jpg?raw=true" /></td>
	</tr>
	<tr>
		<td><img width="100%" src="https://github.com/kaivanwong/washer/blob/main/.github/assets/wash.jpg?raw=true" /></td>
		<td><img width="100%" src="https://github.com/kaivanwong/washer/blob/main/.github/assets/discount-exchange.jpg?raw=true" /></td>
		<td><img width="100%" src="https://github.com/kaivanwong/washer/blob/main/.github/assets/no-order.jpg?raw=true" /></td>
		<td></td>
	</tr>
</table>

## 许可证

[MIT licensed](./LICENSE) © 2022-PRESENT [Kaivan Wong](https://github.com/kaivanwong)
