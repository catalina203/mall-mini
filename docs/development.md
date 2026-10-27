# 本地开发说明

## 1. 环境

| 工具 | 要求 | 本项目验证版本 |
| --- | --- | --- |
| JDK | 17+ | 17.0.15（Homebrew） |
| Maven | 3.9+ | 3.9.9 |
| Node.js | ≥ 20 | 22.23.1 |
| pnpm / npm | pnpm 9+ / npm 10+ | pnpm 10.33.3 |
| MySQL | 8.0+ | Docker MySQL 8.0 |
| Android SDK | platform 35、build-tools 35.0.0 | command-line tools |
| Android 模拟器/真机 | Android 8.0+ | 模拟器 Android 35 |

JDK 21 亦可直接使用；本项目 `pom.xml` 以 17 为编译目标。

## 2. 数据库

### 2.1 本机 MySQL

```sql
CREATE DATABASE IF NOT EXISTS mall_mini
  DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
CREATE USER IF NOT EXISTS 'mallmini'@'localhost' IDENTIFIED BY 'mallmini_dev';
GRANT ALL PRIVILEGES ON mall_mini.* TO 'mallmini'@'localhost';
FLUSH PRIVILEGES;
```

服务端默认连接 `localhost:3306`。

### 2.2 Docker MySQL（推荐统一环境）

```bash
# 仓库根目录
colima start        # 或启动 Docker Desktop
docker compose up -d mysql
```

容器映射到本机 **3307**，库名 `mall_mini`，账号 `mallmini / mallmini_dev`。启动服务端时覆盖连接地址：

```bash
export DB_URL='jdbc:mysql://localhost:3307/mall_mini?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true'
```

Homebrew 安装的 Compose 插件如未被 Docker 识别，在 `~/.docker/config.json` 增加：

```json
{
  "cliPluginsExtraDirs": ["/opt/homebrew/lib/docker/cli-plugins"]
}
```

## 3. 服务端

```bash
cd server
export JAVA_HOME=$(/usr/libexec/java_home -v 17)
mvn spring-boot:run
```

- 默认 profile：`dev`
- 健康检查：`GET http://localhost:8080/api/v1/health`
- Swagger：http://localhost:8080/swagger-ui.html
- 配置项见 `.env.example`；关键变量可用环境变量覆盖

### 演示数据

`dev` profile 启动时，`DemoDataInitializer` 检查 `category` 表：

- 为空：执行 `server/src/main/resources/db/demo/demo-data.sql`
- 非空：跳过，保证重复启动不重复插入

演示数据包含 6 个一级分类、5 个二级分类、12 个在售样例商品、1 个下架样例商品、18 个 SKU、3 条轮播图。全部标注为“样例”，不含真实销量/评价。

**重置开发数据（会删除数据，仅限本地）**：

```bash
# 仓库根目录；确认没有需要保留的数据
docker compose down -v
docker compose up -d mysql
# 重新启动服务端后自动写入演示数据
```

### 测试

```bash
cd server
export JAVA_HOME=$(/usr/libexec/java_home -v 17)
mvn test
```

当前单元测试覆盖：统一响应、生产配置校验、分类树/后代分类、商品聚合、下架过滤、SKU 规格解析、价格区间校验、登录鉴权、地址默认一致性、购物车汇总/越权、订单结算/幂等/取消回补、管理员登录、分类删除约束、商品上架校验、发货与管理员取消、模拟支付/重复支付/支付禁用。

## 4. 管理后台

```bash
cd admin
pnpm install
pnpm dev
```

- http://localhost:5173
- `/api` 与 `/uploads` 由 Vite 代理到 `http://localhost:8080`
- 管理员账号初始化：首次启动服务端前设置 `APP_ADMIN_INIT_USERNAME` / `APP_ADMIN_INIT_PASSWORD`（至少 8 位），库中无管理员时自动创建，BCrypt 存储
- 登录后可管理分类、商品/SKU/库存、订单发货与取消、用户状态、轮播图与图片上传
- 图片上传保存在 `APP_STORAGE_LOCAL_DIR`（默认 `./uploads`），通过 `/uploads/**` 访问；限制 2MB，仅 jpg/png/webp，校验文件内容
- 管理员密码不会以明文存储；仓库不包含固定管理员密码

```bash
pnpm typecheck
pnpm build
```

## 5. 小程序

```bash
cd miniapp
cp project.config.example.json project.config.json
pnpm install
pnpm typecheck
```

1. 用微信开发者工具导入 `miniapp` 目录
2. 将 `project.config.json` 的 `appid` 改为测试号或自有 AppID；示例配置已包含 `useCompilerPlugins: ["typescript"]`，请勿移除
3. 勾选「不校验合法域名」，或将 `http://localhost:8080` 加入 request 合法域名
4. TDesign 依赖已安装；当前页面使用原生组件。需要 TDesign 时在开发者工具执行「工具 → 构建 npm」
5. API 地址集中在 `miniapp/utils/config.ts`

首页与商品详情已接通 `/api/v1/home`、`/api/v1/products/{id}`，分类与搜索页使用 `/api/v1/categories`、`/api/v1/products`，包含加载、失败重试、空数据与图片加载占位。

阶段 3 已实现的浏览能力：

- 首页：轮播图、分类入口（跳转分类页并定位）、推荐商品、搜索入口
- 分类：一级分类切换、二级分类筛选、下拉刷新、触底分页
- 搜索：关键词搜索、最近 10 条本地记录、清空记录、触底分页、无结果状态
- 详情：SKU 规格选择、数量增减、库存/售罄提示、小计计算

阶段 4 已实现：

- 开发登录：`POST /api/v1/auth/dev-login`，按 `openId` 自动创建/复用用户并签发 JWT
- 微信登录：服务端配置 `WECHAT_APP_ID/SECRET` 后调用微信 `jscode2session`；未配置时明确报错
- 个人中心：登录状态、开发登录、微信登录、退出、地址入口
- 收货地址：新增/编辑/删除/设默认，首次新增自动默认，默认地址唯一
- 购物车：加购、勾选、数量调整、删除、清空、合计；下架/停用/库存不足会标记不可选
- 商品详情：选中规格后可「加入购物车」；未登录会提示先登录

开发登录账号由 `miniapp/utils/config.ts` 中的 `devLoginOpenId` 决定，首次登录自动创建用户，无需预置密码。

### 登录与权限

- `/api/v1/me/**` 需要 `USER` 角色 JWT；未登录返回 401，越权访问他人资源返回 404。
- JWT 密钥来自 `JWT_SECRET`，生产必须替换为至少 32 字符的随机值。
- `dev` profile 默认开启开发登录与模拟支付；`prod` profile 启动时强制校验关闭。
- 管理员认证已接入：`/api/v1/admin/**` 需要 `ADMIN` 角色 JWT，普通用户返回 403。

### 订单与库存（阶段 5）

- 结算预览：服务端按数据库价格重新计算商品金额、运费、应付金额；不扣库存
- 创建订单：订单头先写入（`clientRequestNo` 唯一），事务内条件扣减库存，再写明细/状态日志；任一 SKU 失败整单回滚
- 幂等：同一用户相同 `clientRequestNo` 重复提交返回原订单；并发竞态返回 409，库存只扣一次
- 取消：仅待付款可取消；条件更新 `PENDING_PAYMENT → CANCELLED` 成功后才回补库存，重复取消不会重复回补
- 确认收货：仅待收货可操作
- 自动取消：每 5 分钟扫描超时未支付订单（默认 30 分钟），库存回补并写系统状态日志
- 订单列表/详情只返回当前用户数据，越权返回 404
- 支付尚未接入真实渠道：`dev` profile 默认启用明确标记的模拟支付；`prod` 强制关闭，未配置真实网关时返回 `PAYMENT_DISABLED`

### 支付（阶段 7）

- 支付抽象：`PaymentGateway` 接口 + `MockPaymentGateway` 实现，模拟网关仅在 `app.payment.mock-enabled=true` 时创建
- 支付流程：待付款订单调用 `POST /api/v1/me/orders/{orderNo}/pay` → 写 `payment_record` → 条件更新订单为 `PENDING_SHIPMENT` / `PAID` → 写 `PAYMENT` 状态日志
- 重复支付：已支付订单直接返回当前订单，不重复写支付记录
- 金额：使用订单 `payable_amount`，不接受客户端提交金额
- 生产：`APP_PAYMENT_MOCK_ENABLED=false`，启动校验禁止生产开启模拟支付；真实微信支付需实现网关、回调验签、金额校验、幂等与对账
- 支付配置：`GET /api/v1/payment/config` 返回 `mockEnabled` / `channel`，小程序据此显示模拟支付入口

模拟支付仅用于本地开发，页面与日志均明确标记“模拟支付”，不得误认为真实支付。真实微信支付/回调/退款/对账的完整步骤见 [`wechat-integration.md`](./wechat-integration.md)；物流轨迹接入见 [`logistics-integration.md`](./logistics-integration.md)。

订单创建/取消/确认收货/库存回补/支付均有单元测试；并发超卖、重复提交、支付重复调用、管理员发货与越权隔离已通过真实 API 实测。

### 售后、库存流水与通知（R1）

- 数据库：`V2__after_sale_stock_notification.sql`，启动时自动迁移。
- 用户端：订单详情「申请售后」→ 选择商品/类型/原因；「售后服务」列表与详情；待退货状态可填写退货物流。
- 管理后台：交易与用户 → 售后管理（审核、拒绝、确认收货、退款）；商品运营 → 库存中心（流水、预警、调整）；交易与用户 → 通知记录。
- 退款：开发环境使用 `MOCK` 模拟退款；生产未接微信退款时，管理员必须填写线下退款凭证（`MANUAL`），不会静默成功。
- 库存：下单/取消/售后退货/手动调整均写 `stock_log`；低库存阈值 `APP_STOCK_ALERT_THRESHOLD` 默认 10。
- 通知：当前为站内通知，用户接口 `/api/v1/me/notifications`；微信订阅消息尚未接入。
- 测试：`mvn test` 覆盖售后金额、状态流转、退款幂等/手工退款、库存流水幂等与退款后回补。

### 管理员权限、导出、自动确认（R2）

- 数据库：`V3__r2_admin_order.sql`，启动时自动迁移；已有 `ADMIN` 角色会迁移为 `SUPER_ADMIN`。
- 升级后旧管理员 JWT 因缺少 token version 会失效，需要重新登录。
- 管理员角色：超级管理员、运营、客服、仓储；菜单按权限隐藏，后端接口用 `@PreAuthorize` 校验。
- 管理员管理：系统 → 管理员管理；支持新增、改角色/显示名、禁用、重置密码。改角色/禁用/重置密码后旧令牌失效。
- 修改自己的密码：左下角用户卡片 → 锁图标；成功后需要重新登录。
- 操作日志：系统 → 操作日志（仅超级管理员）。
- CSV 导出：订单、用户、库存流水页面右上角；需要 `EXPORT_READ`，单次最多 10000 行。
- 自动确认收货：`APP_ORDER_AUTO_CONFIRM_ENABLED` / `APP_ORDER_AUTO_CONFIRM_DAYS`，Job 初始延迟 60 秒，之后默认每 30 分钟扫描。
- 订单增强：用户待发货可改地址；管理后台可写内部备注；下单可填发票信息（仅记录，不自动开票）。
- 仪表盘：支付金额、退款金额、净收入、客单价、退款率、售后待处理、低库存与支付趋势。

### 优惠券、运费模板、评价、收藏（R3）

- 数据库：`V4__r3_coupon_shipping_review_favorite.sql`，启动时自动迁移；默认运费模板 id=1，历史商品绑定该模板。
- 管理后台：营销与内容 → 优惠券/评价管理；商品运营 → 运费模板；商品编辑可绑定运费模板。
- 小程序：「我的」→ 我的优惠券/我的收藏/浏览足迹；商品详情可收藏、查看评价；已完成订单可评价。
- 优惠券：结算页选择未使用券，服务端重新计算优惠；取消/超时取消释放。
- 运费：结算按模板+省份计算首件/续件与包邮门槛；未绑定回退全局配置。
- 评价：提交后待审核，管理员通过后展示；支持商家回复。
- 收藏/足迹：商品详情自动记录足迹；收藏/足迹页面可查看与清空。

### 搜索、客服、会员积分、首页运营位（R4）

- 数据库：`V5__r4_search_support_member_home.sql`，增加积分、热词、工单、商品销量与 ngram 全文索引。
- 搜索：商品列表支持 `sort=sales`；第一页搜索记录热词；小程序搜索页展示热门搜索。
- 首页：新增新品上架、热销榜单与领券/客服/会员快捷入口。
- 会员：订单完成后按实付 1 元 = 1 积分奖励；整单退款扣回；小程序「我的」显示会员卡，会员页可看积分明细。
- 客服：小程序首页/我的 → 联系客服，提交工单；管理后台交易与用户 → 客服工单，回复/关闭。
- 用户管理：新增会员等级/积分列，可手动调整积分（`USER_STATUS` 权限）。

## 6. Android 客户端

项目路径：`android/`，原生 Kotlin + Jetpack Compose，复用 `/api/v1` 接口。

### 构建

```bash
cd android
export JAVA_HOME="$(/usr/libexec/java_home -v 17)"
export ANDROID_HOME=/opt/homebrew/share/android-commandlinetools
export ANDROID_SDK_ROOT=$ANDROID_HOME
./gradlew :app:assembleDebug
```

APK：`android/app/build/outputs/apk/debug/app-debug.apk`

`android/local.properties` 的 `sdk.dir` 指向本机 SDK，不提交仓库。当前为 debug 签名，仅用于开发测试。

### 安装

```bash
export PATH="/opt/homebrew/share/android-commandlinetools/platform-tools:$PATH"
adb devices
adb install -r android/app/build/outputs/apk/debug/app-debug.apk
```

也可传输 APK 到手机后允许“安装未知来源应用”安装。

### 连接服务端

- 手机与 Mac 同 Wi-Fi。
- App「我的 → 服务器设置」填写 `http://<Mac 局域网 IP>:8080/api/v1`。
- 默认值在 `android/app/build.gradle.kts` 的 `DEFAULT_API_BASE_URL`。
- Debug 包允许 HTTP 明文；生产必须 HTTPS。
- 开发登录默认 openId `android-dev-user`，不是微信登录；支付为模拟支付。

### 验证

已在 Android 35 模拟器完成：安装启动、首页加载、开发登录、SKU 选择、加购、结算、下单、模拟支付、管理员发货、确认收货。真实手机需你本地安装验证。

## 7. 常见问题

| 现象 | 排查 |
| --- | --- |
| 8080 端口占用 | `lsof -ti:8080 \| xargs kill` 后重启 |
| 数据库连接拒绝 | 确认 MySQL 已启动、端口（3306/3307）与 `DB_URL` 一致 |
| Flyway 校验失败 | 不要修改已执行的迁移脚本；开发库可按下文重置 |
| 管理后台接口 401/403 | 确认使用管理员账号登录；普通用户令牌不能访问 `/admin/**` |
| 小程序图片不显示 | 确认服务端已启动且开发者工具未校验域名 |
| 演示数据未出现 | 检查是否 `dev` profile、`category` 表是否已有数据 |

## 8. 配置优先级

环境变量 > `application-{profile}.yml` > `application.yml`。生产配置校验在启动时执行：`JWT_SECRET` 至少 32 字符，生产 profile 禁止开发登录与模拟支付。
