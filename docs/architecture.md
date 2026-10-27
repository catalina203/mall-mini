# 架构设计

## 1. 目标与范围

MallMini 是面向国内电商的可复用脚手架，优先打通：

> 商品浏览 → 购物车 → 收货地址 → 创建订单 → 支付（开发模拟）→ 发货 → 确认收货

首版包含四个客户端/服务：

| 端 | 技术 | 默认端口 | 说明 |
| --- | --- | --- | --- |
| 小程序用户端 | 微信原生小程序 + TypeScript + TDesign Miniprogram | 微信开发者工具 | 商品、购物车、地址、订单、个人中心 |
| Android 客户端 | Kotlin + Jetpack Compose + Retrofit + Coil | Android 8.0+ | 首页、分类、搜索、详情、购物车、地址、订单、模拟支付 |
| 管理后台 | Vue 3 + TypeScript + Vite + Element Plus | 5173 | 商品/分类/SKU/订单/用户/轮播图 |
| 服务端 | Java 17 + Spring Boot 3 + MyBatis-Plus + MySQL | 8080 | 业务 API、鉴权、库存与订单事务 |

首版**不包含**：微服务、消息队列、分布式事务、多商户、优惠券、真实微信支付、真实物流查询、Redis 强依赖。

## 2. 技术版本（锁定）

| 组件 | 版本 | 备注 |
| --- | --- | --- |
| Java | 17（本机已确认；代码同时兼容 21） | 因开发机无 JDK 21，经确认以 17 构建，生产建议 21 |
| Spring Boot | 3.3.5 | Java 17+ 兼容 |
| MyBatis-Plus | 3.5.x（spring-boot3-starter） | |
| MySQL | 8.0+（本地 9.3 亦兼容） | utf8mb4 |
| Flyway | 随 Spring Boot 管理 | 迁移脚本版本化 |
| Springdoc OpenAPI | 2.6.x | `/swagger-ui.html` |
| JWT | jjwt 0.12.x | 密钥来自环境变量 |
| Node.js | ≥ 20（开发机 22） | |
| Vue / Vite | Vue 3.5 / Vite 6 | |
| Element Plus | 2.9.x | |
| TDesign Miniprogram | 1.9.x | 依赖已安装，当前小程序使用原生组件 |
| Kotlin / Compose | Kotlin 2.0.21 / Compose BOM 2024.12 | Android 客户端 |
| Android SDK | compile/target 35，minSdk 26 | Gradle 8.10.2 |

> 实际初始化时以 `pom.xml`、`package.json` 锁定的精确版本为准，不使用 SNAPSHOT。

## 3. 总体结构

```text
mall-mini/
├── miniapp/     微信原生小程序（TS）
├── android/     Android 客户端（Kotlin + Compose）
├── admin/       Vue 3 管理后台
├── server/      Spring Boot 模块化单体
├── docs/        设计与运维文档
├── docker-compose.yml   仅 MySQL（可选，用于本地基础设施）
└── .env.example
```

各客户端仅通过 HTTP API 通信，API 地址由各自环境配置提供，不硬编码。

## 4. 服务端模块边界

按业务分包，每包内按需包含 `controller / service / mapper / entity / dto / vo / enums`：

| 包 | 职责 |
| --- | --- |
| `common` | 统一响应、异常、错误码、分页、工具、请求 ID |
| `config` | MyBatis-Plus、OpenAPI、CORS、Jackson、存储配置 |
| `security` | 用户/管理员 JWT 解析、角色与权限 authorities、登录态上下文 |
| `auth` | 开发登录、微信登录适配、令牌签发 |
| `user` | 用户资料、用户状态 |
| `admin` | 管理员账号、认证、RBAC、操作日志、CSV 导出 |
| `category` | 分类查询与管理 |
| `product` | 商品、SKU、图片、上下架、库存查询 |
| `inventory` | 库存行锁扣减/回补、库存流水、低库存预警 |
| `coupon` | 优惠券模板、领取、算价、锁定与释放 |
| `shipping` | 运费模板、区域规则与运费计算 |
| `review` | 商品评价、审核、回复、评分聚合 |
| `favorite` | 收藏与浏览足迹 |
| `search` | 搜索热词、中文全文搜索辅助 |
| `member` | 积分账户、积分流水、会员等级 |
| `support` | 客服工单与回复 |
| `cart` | 购物车（仅登录用户，持久化） |
| `address` | 收货地址与默认地址一致性 |
| `order` | 结算预览、创建订单、状态机、发货、确认收货 |
| `aftersale` | 售后申请/审核/退货、退款记录、模拟/手工退款网关 |
| `payment` | 支付抽象、模拟支付适配器、支付记录 |
| `notification` | 站内通知、通知记录、后续订阅消息/短信扩展点 |
| `banner` | 轮播图查询与管理 |
| `storage` | 图片存储接口 + 本地存储适配器 |
| `dashboard` | 经营指标、退款/净收入/客单价/趋势聚合 |

分层约束：

- Controller 只做参数接收、鉴权上下文获取、调用 Service、返回 VO。
- Service 承担业务校验、事务边界、状态机转换。
- Mapper 只做数据访问，复杂条件更新用显式 SQL（库存扣减）。
- Entity 不直接作为 API 契约，统一 DTO/VO。

## 5. 认证与授权

- 采用 **JWT**（无服务端会话）。密钥、有效期来自环境变量。
- 两类主体：`USER`（小程序用户）与 `ADMIN`（管理后台）。
- JWT claims：`sub`（主体 ID）、`role`（USER/ADMIN）、`iat`、`exp`；管理员额外包含 `ver`（token version）。
- 管理员每次请求从数据库加载当前角色与权限，角色调整后立即生效；`ver` 不匹配则令牌失效。
- Spring Security 两条过滤链：
  - `/api/v1/admin/**`：要求 `ROLE_ADMIN`。
  - `/api/v1/me/**`：要求 `ROLE_USER`。
  - 商品/分类/轮播图/首页读接口公开；支付回调公开但验签。
- 所有资源访问在 Service 层按 `userId` 过滤，防止 ID 越权；前端路由守卫只做体验控制。
- 密码使用 BCrypt；管理员通过初始化命令或 SQL 脚本按需创建，仓库不内置生产弱口令。
- RBAC 角色：`SUPER_ADMIN / OPERATOR / CUSTOMER_SERVICE / WAREHOUSE`；权限矩阵在 `AdminPermissionService` 维护，接口用 `@PreAuthorize` 强制校验。

### 登录方式

1. **开发登录**（仅 `dev` 配置且显式开启）：`POST /api/v1/auth/dev-login`，按传入 `openId` 查找/创建用户并签发 JWT，响应标记 `devMode: true`。
2. **微信登录**（生产）：小程序 `wx.login` 获取 code，服务端调用微信 `jscode2session`。未配置 `WECHAT_APP_ID/SECRET` 时返回明确错误，不伪造成功。

## 6. 订单与库存

### 6.1 订单状态机

```text
PENDING_PAYMENT --支付成功--> PENDING_SHIPMENT --管理员发货--> PENDING_RECEIPT --用户确认/超时自动确认--> COMPLETED
       |
       +--用户取消/超时取消--> CANCELLED
```

- 支付状态独立字段：`UNPAID / PAID / PARTIALLY_REFUNDED / REFUNDED`，不与订单状态混用；已退款订单禁止发货。
- 状态变更只能通过 Service 中受控方法，写 `order_status_log`。
- 客户端不能提交目标状态；接口语义化（cancel / confirm-receipt / ship）。
- 自动确认收货 Job 扫描超期未完成订单，带 SYSTEM 日志与用户通知；售后中或已退款订单跳过。

### 6.2 库存

- 下单事务内按 SKU ID 升序执行条件更新：
  `UPDATE product_sku SET stock = stock - ? WHERE id = ? AND stock >= ?`
- 影响行数 ≠ 1 即库存不足，抛业务异常，整个事务回滚（无半成品订单）。
- 取消未支付订单在同一事务内回补库存，并把订单置为 `CANCELLED`；状态条件更新保证只回补一次。
- 库存列带 `CHECK (stock >= 0)`，双重保障。
- 所有库存变化写 `stock_log`（前后值、业务单号、操作人）；回补按业务键幂等。
- 售后退货入库在管理员确认收到退货且退款成功后执行，只对 `RETURN_REFUND` 生效。

### 6.5 售后与退款

```text
APPLIED -> APPROVED -> REFUNDING -> REFUNDED / REFUND_FAILED
APPLIED -> WAIT_RETURN -> RETURNED -> RETURN_RECEIVED -> REFUNDING -> REFUNDED
APPLIED -> REJECTED
APPLIED/APPROVED/WAIT_RETURN -> CANCELLED
```

- 退款金额由服务端按订单明细与实付金额计算，客户端不可提交。
- 开发环境使用 `MOCK` 退款；生产未接微信退款时使用 `MANUAL` 并记录线下凭证，不静默成功。
- 退款成功更新 `payment_status`，退货退款才回补库存；退款记录与售后状态均幂等。
- 外部退款渠道通过 `RefundGateway` 隔离，后续接入微信退款不影响订单/售后核心逻辑。

### 6.3 幂等

- 创建订单要求 `clientRequestNo`，`(user_id, client_request_no)` 唯一索引；冲突时返回已存在订单，避免重复扣库存。
- 模拟支付按订单状态条件更新，重复调用返回当前结果，不重复发货前置流转。

### 6.4 金额

- 数据库 `DECIMAL(10,2)`，Java `BigDecimal`，统一四舍五入 `HALF_UP`。
- 商品单价、数量、运费全部服务端重算；客户端金额字段仅用于展示，不参与计算。
- 订单明细保存商品名、SKU 规格、图片、成交单价快照。

## 7. 配置管理

`application.yml` 通过环境变量覆盖，关键项：

| 变量 | 说明 | 默认 |
| --- | --- | --- |
| `DB_URL / DB_USERNAME / DB_PASSWORD` | MySQL 连接 | 本地 mall_mini |
| `JWT_SECRET` | 签名密钥（≥ 32 字节） | 无，必须提供 |
| `JWT_EXPIRE_MINUTES` | 令牌有效期 | 1440 |
| `APP_DEV_LOGIN_ENABLED` | 开发登录 | dev=true, prod=false |
| `APP_PAYMENT_MOCK_ENABLED` | 模拟支付 | dev=true, prod=false |
| `APP_FREE_SHIPPING_THRESHOLD` | 包邮门槛（元） | 99.00 |
| `APP_SHIPPING_FEE` | 固定运费（元） | 10.00 |
| `APP_ORDER_CANCEL_MINUTES` | 未支付取消时限 | 30 |
| `APP_ORDER_AUTO_CONFIRM_DAYS` | 自动确认收货天数 | 7 |
| `APP_STOCK_ALERT_THRESHOLD` | 库存预警阈值 | 10 |
| `APP_STORAGE_LOCAL_DIR` | 本地图片目录 | ./uploads |
| `WECHAT_APP_ID / WECHAT_APP_SECRET` | 微信登录 | 空 |
| `CORS_ALLOWED_ORIGINS` | 后台来源白名单 | http://localhost:5173,http://127.0.0.1:5173 |

生产 profile 强制关闭开发登录与模拟支付（启动校验）。

## 8. 外部服务边界

- `PaymentGateway` 接口：`MockPaymentGateway`（dev）与未来 `WechatPayGateway`（prod）。
- `RefundGateway` 接口：`MockRefundGateway`（dev）；生产未接微信退款时使用 `MANUAL` 线下凭证登记。
- `StorageService` 接口：`LocalStorageService`（dev），生产可替换对象存储。
- 物流仅存公司/单号，不接第三方查询。

## 9. 环境与服务

**必需**：JDK 17+（本机使用 17）、Maven 3.9+、Node.js ≥ 20、pnpm/npm、MySQL 8.0+。

**可选**：Docker/Colima（跑 MySQL 容器）、微信开发者工具（编译小程序）、Nginx（部署）。

Redis 首版不接入。

## 10. 阶段计划

| 阶段 | 内容 | 验收 |
| --- | --- | --- |
| 0 | 环境检查与设计文档 | 本文档组完成 |
| 1 | 三端骨架、配置、迁移、健康检查 | 三端可构建/启动，空库迁移成功 |
| 2 | 公共层 + 分类/商品/SKU | 查询 API + 测试 + 演示数据 |
| 3 | 小程序浏览流程 | 首页/分类/搜索/详情可用 |
| 4 | 登录、地址、购物车 | 用户数据隔离测试 |
| 5 | 订单与库存闭环 | 下单/取消/回补/并发测试 |
| 6 | 管理后台 | 登录、CRUD、发货可用 |
| 7 | 模拟支付与扩展边界 | dev 模拟支付，prod 禁用 |
| 8 | 回归与交付文档 | 全部测试与构建通过 |
| R1 | 售后/退款、库存流水、站内通知 | 退款幂等、库存回补、通知记录 |
| R2 | RBAC、管理员管理、审计、导出、自动确认、看板 | 角色隔离、旧令牌失效、CSV 与指标 |
| R3 | 优惠券、运费模板、评价、收藏/足迹 | 算价可信、领取不超发、评价审核 |
| R4 | 搜索优化、客服工单、会员积分、首页运营位 | 中文搜索、积分闭环、工单可处理 |

## 11. 关键风险与决策

| 风险 | 决策 |
| --- | --- |
| 本机无 JDK 21 | 经确认改用 JDK 17 构建（Spring Boot 3 兼容），文档与 pom 保持一致；生产可升级 21 |
| 本机 MySQL 9.3 而非 8.0 | 开发兼容；docker-compose 提供 8.0 以贴近生产 |
| 无微信 AppID/支付商户 | 默认 dev 登录 + 模拟支付，界面明确标注；真实接入单独验收 |
| 图片托管 | 首版本地 `uploads/`，接口隔离便于替换 |

## 12. 非目标（后续扩展）

多商户、虚拟商品、Redis 缓存、真实微信支付/退款/物流查询、电子发票、对象存储、大数据量异步导出、复杂促销规则引擎、积分抵现。
