# MallMini · 优选商城

国内电商全栈脚手架：微信小程序、原生 Android、Vue 3 管理后台、Spring Boot 服务端与 MySQL。

> 当前进度：**阶段 0–8 + R1–R4 已完成**。交易、售后、库存、通知、RBAC、审计/导出、自动确认、看板、优惠券、运费模板、评价、收藏/足迹、搜索优化、会员积分、客服工单与首页运营位已落地；真实微信支付/物流与部分扩展能力见「已知限制」。

## 功能规划

| 模块 | 内容 | 状态 |
| --- | --- | --- |
| 小程序 | 首页（新品/热销/快捷入口）、分类、搜索（热词）、详情、登录、地址、购物车、结算、优惠券、订单、模拟支付、售后、评价、收藏、足迹、会员积分、客服工单 | 已完成 |
| 管理后台 | 管理员登录、RBAC/管理员管理、经营看板、商品/SKU/分类、订单、售后、库存、优惠券、运费模板、评价、客服工单、用户积分、通知、操作日志、CSV 导出、轮播图、上传 | 已完成 |
| 服务端 | 商品、JWT、RBAC、地址、购物车、订单库存、优惠券、运费、评价、收藏/足迹、售后、库存、通知、自动确认、发票、搜索热词/全文索引、会员积分、客服工单、CSV、看板、支付抽象 | 已完成 |
| Android | 首页（新品/热销/快捷入口）、分类、搜索（热词）、详情、购物车、地址、结算（优惠券/发票）、订单、模拟支付、售后、评价、收藏/足迹、会员积分、客服工单、通知 | 已完成 |
| 数据库 | 30 张业务表、Flyway V1–V5 迁移、开发演示数据 | 已完成 |

## 技术栈

- 小程序：微信原生小程序 + TypeScript + TDesign Miniprogram（依赖已安装，当前使用原生组件）
- Android：Kotlin + Jetpack Compose + Material 3 + Retrofit + Coil（minSdk 26 / target 35）
- 管理后台：Vue 3 + TypeScript + Vite + Element Plus + Pinia + Axios
- 服务端：Java 17 + Spring Boot 3.3 + MyBatis-Plus + MySQL + Flyway + Spring Security + JWT + Springdoc
- 数据库：MySQL 8.0+（本地开发可用 Docker Compose，见下）

> 说明：AGENTS.md 原定 Java 21；因当前开发机无 JDK 21，经确认改用 JDK 17 构建，代码兼容 Java 21。生产建议使用 Java 21 LTS。

## 目录

```text
mall-mini/
├── admin/        Vue 3 管理后台
├── miniapp/      微信原生小程序
├── android/      原生 Android 客户端
├── server/       Spring Boot 服务端
├── docs/         架构、数据库、API、业务规则
├── docker-compose.yml
├── .env.example
└── AGENTS.md
```

## 环境要求

| 工具 | 版本 |
| --- | --- |
| JDK | 17+（本机 17.0.15） |
| Maven | 3.9+ |
| Node.js | ≥ 20 |
| pnpm / npm | pnpm 10 或 npm 10 |
| MySQL | 8.0+（本机 9.3） |
| 微信开发者工具 | 可选，运行小程序时需要 |
| Android SDK | 可选，构建 Android APK 时需要（platform 35 / build-tools 35.0.0） |

## 1. 初始化数据库

推荐使用 Docker（MySQL 8.0，映射到本机 **3307**，避免与本机 3306 冲突）：

```bash
cd /Users/catalina/Workspace/module/mall-mini
colima status || colima start
docker compose up -d mysql
docker compose ps
```

> Docker 需先启动 Docker Desktop 或 `colima start`。Homebrew 的 Compose 插件如提示 `docker: 'compose' is not a docker command`，按 [开发说明](./docs/development.md) 配置 `cliPluginsExtraDirs`。

也可使用本机 MySQL（3306），需自行创建库与账号：

```sql
CREATE DATABASE IF NOT EXISTS mall_mini
  DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
CREATE USER IF NOT EXISTS 'mallmini'@'localhost' IDENTIFIED BY 'mallmini_dev';
GRANT ALL PRIVILEGES ON mall_mini.* TO 'mallmini'@'localhost';
FLUSH PRIVILEGES;
```

> 不要混用 3306/3307。使用 Docker 时必须设置下方 `DB_URL`。

## 2. 启动服务端

推荐方式：环境变量统一放在 `server/.env`，启动脚本会自动加载。

```bash
cd /Users/catalina/Workspace/module/mall-mini/server

# 首次使用：生成本地 .env（已被 .gitignore 忽略）
cp .env.example .env

# 启动
./run-dev.sh
```

`run-dev.sh` 会：
- 自动加载 `server/.env`
- 未设置 `JAVA_HOME` 时自动选择 JDK 17
- 执行 `mvn spring-boot:run`

停止：

```bash
./stop-dev.sh
```

手动方式（仍可用）：

```bash
export JAVA_HOME="$(/usr/libexec/java_home -v 17)"
export SPRING_PROFILES_ACTIVE=dev
export DB_URL='jdbc:mysql://localhost:3307/mall_mini?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true'
export DB_USERNAME=mallmini
export DB_PASSWORD=mallmini_dev
mvn spring-boot:run
```

使用本机 MySQL（3306）时，把 `DB_URL` 改为：

```bash
export DB_URL='jdbc:mysql://localhost:3306/mall_mini?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true'
```

> `dev` 使用开发默认 JWT 密钥；生产必须设置独立 `JWT_SECRET`，并关闭开发登录与模拟支付。

验证：

```bash
curl http://localhost:8080/api/v1/health
# {"code":"OK","message":"OK","data":{"status":"UP","database":"UP",...},"requestId":"..."}
```

- Swagger UI：http://localhost:8080/swagger-ui.html
- Flyway 启动时自动执行 `server/src/main/resources/db/migration/V1__init_schema.sql`
- 开发环境（`dev`）启动时，若分类表为空，自动执行 `server/src/main/resources/db/demo/demo-data.sql` 写入样例分类、商品、SKU 与轮播图；重复启动不会重复写入

商品接口示例：

```bash
curl "http://localhost:8080/api/v1/home"
curl "http://localhost:8080/api/v1/products?page=1&pageSize=5&sort=price_asc"
curl "http://localhost:8080/api/v1/products?categoryId=1&keyword=耳机"
curl "http://localhost:8080/api/v1/products/1"
curl "http://localhost:8080/api/v1/categories"
```

> 演示图片由服务端静态资源 `/demo/*.svg` 提供，地址为 `http://localhost:8080/demo/...`，仅用于本地开发。

## 3. 启动管理后台

```bash
cd admin
pnpm install
pnpm dev
```

访问 http://localhost:5173 。开发环境 `/api` 由 Vite 代理到 `http://localhost:8080`。

当前本机开发库的管理员账号：

| 字段 | 值 |
| --- | --- |
| 用户名 | `admin` |
| 密码 | `AdminR1test!` |
| 角色 | 超级管理员 |
| 适用环境 | **仅本地开发库** |

> 该密码是当前本机开发数据库中已存在的本地测试账号，不是线上生产密码。生产环境必须通过 `APP_ADMIN_INIT_USERNAME` / `APP_ADMIN_INIT_PASSWORD` 初始化独立管理员，并立即修改为强密码。

生产构建与类型检查：

```bash
cd admin
pnpm build
```

> 管理员账号通过 `APP_ADMIN_INIT_USERNAME` / `APP_ADMIN_INIT_PASSWORD` 在首次启动且无管理员时创建（BCrypt 存储）。当前本机开发库已存在 `admin / AdminR1test!`，仅用于本地开发；仓库不会在生产环境自动创建固定密码。登录后可使用仪表盘、商品/分类/SKU 管理、订单、售后、库存、优惠券、评价、客服工单和用户积分等功能。
>
> 上传图片保存在服务端 `APP_STORAGE_LOCAL_DIR`（默认 `server/uploads`），通过 `/uploads/**` 访问；生产请替换为对象存储。

## 4. 运行小程序

```bash
cd miniapp
cp project.config.example.json project.config.json
pnpm install
pnpm typecheck
```

用微信开发者工具导入 `miniapp` 目录：

1. 如无本地配置：`cp project.config.example.json project.config.json`；`appid` 可使用测试号或替换为自己 AppID。配置已包含 `useCompilerPlugins: ["typescript"]`，请勿移除。
2. 本地调试勾选「不校验合法域名」，或将 `http://localhost:8080` 加入 request 合法域名。
3. 服务端 API 地址集中在 `miniapp/utils/config.ts`，生产环境替换为 HTTPS 域名。
4. 登录：打开「我的」→「开发登录（测试用）」。开发登录仅在 `dev` profile 且 `APP_DEV_LOGIN_ENABLED=true` 时可用，不是真实微信登录。
5. 真实微信登录需配置 `WECHAT_APP_ID` / `WECHAT_APP_SECRET`；未配置时接口返回明确错误，不会伪造成功。
6. TDesign 依赖已安装但当前页面使用原生组件，不需要执行「构建 npm」。

登录后可在商品详情加入购物车或立即购买，在「购物车」勾选后结算；订单创建后可在「我的 → 我的订单」查看、取消（未支付）、模拟支付或确认收货。

支付说明：

- `dev` profile 默认启用明确标记的**模拟支付**，仅用于本地联调，不会产生真实扣款
- 小程序订单详情在待付款时展示「模拟支付（开发环境）」按钮；支付成功后订单进入待发货
- `prod` profile 强制关闭模拟支付；未配置真实支付网关时返回 `PAYMENT_DISABLED`
- 支付配置查询：`GET /api/v1/payment/config`（公开，返回 `mockEnabled`）
- 真实微信支付、证书、回调验签与对账尚未接入，不能视为已完成

订单接口示例：

```bash
# 登录后获取 token
TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/auth/dev-login \
  -H 'Content-Type: application/json' \
  -d '{"openId":"dev-user-1","nickname":"开发用户"}' | jq -r .data.token)

# 结算预览 / 创建订单 / 查询
curl -X POST http://localhost:8080/api/v1/me/checkout/preview \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"addressId":1,"source":"BUY_NOW","skuId":1,"quantity":1}'
curl -X POST http://localhost:8080/api/v1/me/orders \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"addressId":1,"source":"BUY_NOW","skuId":1,"quantity":1,"clientRequestNo":"demo-001"}'
curl "http://localhost:8080/api/v1/me/orders" -H "Authorization: Bearer $TOKEN"
```

## 5. Android 客户端

当前提供原生 Kotlin + Jetpack Compose 客户端，复用同一套 `/api/v1` 接口。

### 构建 APK

```bash
cd /Users/catalina/Workspace/module/mall-mini/android
export JAVA_HOME="$(/usr/libexec/java_home -v 17)"
export ANDROID_HOME=/opt/homebrew/share/android-commandlinetools
export ANDROID_SDK_ROOT=$ANDROID_HOME
./gradlew :app:assembleDebug
```

产物：

```text
android/app/build/outputs/apk/debug/app-debug.apk
```

### 安装到 Android 手机

已确认可通过邮件、网盘或 AirDrop 把 APK 传到手机，允许“安装未知来源应用”后直接安装。

USB 调试方式：

```bash
export PATH="/opt/homebrew/share/android-commandlinetools/platform-tools:$PATH"
adb devices
adb install -r /Users/catalina/Workspace/module/mall-mini/android/app/build/outputs/apk/debug/app-debug.apk
```

当前 APK 为 debug 签名，只用于开发测试，不能直接上架应用商店。

### 首次连接服务端

1. 手机与 Mac 连接同一个 Wi-Fi，关闭手机 VPN/代理。
2. Mac 启动 MySQL 与服务端（见本文第 1、2 节）。
3. 先用手机浏览器访问：

```text
http://<Mac 局域网 IP>:8080/api/v1/health
```

看到 `data.status=UP` 说明网络可通。

4. 打开 App →「我的」→「服务器设置」，填写：

```text
http://<Mac 局域网 IP>:8080/api/v1
```

例如当前开发机为 `http://192.168.2.7:8080/api/v1`。修改服务器地址会清除本地登录态，需要重新登录。

5. 如果 Mac 的局域网 IP 变化，在 App 内重新修改即可，无需重新打包。
6. Mac 防火墙需要允许 Java/服务端接受局域网入站连接。

### 开发登录

Android 端当前使用明确标记的开发登录，**不是微信登录**：

| 字段 | 默认值 |
| --- | --- |
| 用户标识（openId） | `android-dev-user` |
| 昵称 | `Android 用户` |
| 密码 | 无 |

- 首次登录会自动创建用户。
- 修改 openId 可创建另一个独立测试账号，其购物车、地址、订单相互隔离。
- 开发登录仅在服务端 `dev` profile 且 `APP_DEV_LOGIN_ENABLED=true` 时可用。

### 完整测试流程

1. 首页应加载轮播图、分类和推荐商品。
2. 「我的」→ 开发登录。
3. 商品详情 → 选择 SKU → 加入购物车或立即购买。
4. 购物车 → 去结算 → 新增收货地址 → 提交订单。
5. 订单详情 →「模拟支付（开发环境）」→ 二次确认；不会真实扣款。
6. 订单变为待发货后，在 Mac 浏览器打开管理后台：

```text
http://localhost:5173
```

本机开发库管理员：`admin / AdminR1test!`（仅本地开发使用）。
进入「订单管理」→ 发货，填写物流公司与单号。

7. 回到手机 App →「我的订单」→ 重新进入订单 →「确认收货」。
8. 后台仪表盘可看到订单数与已支付销售额变化。

### Android 常见问题

| 现象 | 处理 |
| --- | --- |
| 首页加载失败 | 确认服务端已启动；手机浏览器访问健康检查地址 |
| 服务器连接失败 | 手机与 Mac 是否同一 Wi-Fi；Mac 防火墙是否放行；IP 是否变化 |
| 图片不显示 | 确认服务端可访问；示例图片域名会被 App 重写到当前服务器 |
| 登录失败 | 确认服务端为 dev profile 且开发登录开启；检查 openId |
| 支付按钮不显示 | 服务端未开启模拟支付，或订单不是待付款状态 |
| 安装被拦截 | 在手机设置中允许当前文件管理器/邮件应用安装未知来源应用 |

### 停止 Android 测试环境

```bash
# 停止模拟器（如果使用）
/opt/homebrew/share/android-commandlinetools/platform-tools/adb emu kill

# 停止服务端与管理后台
lsof -ti:8080 | xargs kill
lsof -ti:5173 | xargs kill

# 停止数据库容器（数据卷保留）
cd /Users/catalina/Workspace/module/mall-mini
docker compose stop mysql
```

### 当前验证状态

- Android 35 模拟器：安装启动、新版首页、开发登录、商品详情/收藏/评价、加购、结算（优惠券/发票）、下单、模拟支付、订单详情、售后申请、会员积分、客服工单、收藏/足迹、消息通知均已跑通。
- Android 真机：APK 已通过邮件安装；如遇网络或界面问题，按上方常见问题排查。
- 真实微信登录、真实微信支付、推送、离线缓存未接入。

详细构建、目录与限制见 [android/README.md](./android/README.md)。

## 6. 测试与构建

```bash
# 服务端
cd server
export JAVA_HOME=$(/usr/libexec/java_home -v 17)
mvn test

# 管理后台
cd admin
pnpm typecheck
pnpm build

# 小程序
cd miniapp
pnpm typecheck
```

## 配置与安全

- 所有配置项见 [.env.example](./.env.example)，不提交真实密钥。
- `JWT_SECRET` 至少 32 字符；生产必须使用独立随机值，启动校验会拒绝开发默认密钥。
- 生产 profile 强制关闭开发登录与模拟支付。
- 生产环境管理员通过初始化环境变量创建，仓库不内置生产固定密码；当前本机开发库为 `admin / AdminR1test!`（仅本地）。
- 微信 AppSecret、支付证书等只放服务端环境变量。
- 上线前逐项核对 [发布检查清单](./docs/release-checklist.md)。

## 验证结果（当前基线）

| 检查 | 结果 |
| --- | --- |
| 服务端单元测试 | 101 个全部通过 |
| 空库 / 增量 Flyway 迁移 | 成功（V1–V5，30 张业务表，含 ngram 全文索引） |
| 开发演示数据 | 11 分类 / 13 商品 / 18 SKU 自动导入 |
| 完整交易回归 | 加购 → 结算 → 下单 → 模拟支付 → 管理员发货 → 确认收货 |
| 售后回归 | 仅退款、退货退款、审核、退货物流、确认收货、模拟退款 |
| 库存与取消回补 | 并发不超卖、重复取消不重复回补；售后回补写流水 |
| 通知 | 支付/发货/售后/退款事件写入站内通知 |
| RBAC | 超级管理员/运营/客服/仓储权限矩阵，接口 403、旧令牌失效实测通过 |
| 管理后台 | 登录、RBAC、管理员管理、操作日志、CRUD、发货、售后审核/退款、库存流水、通知、CSV 导出、用户状态、轮播图、上传、看板实测通过 |
| 订单增强 | 发票信息、后台备注、待发货改地址、自动确认收货实测通过 |
| 营销与内容 | 优惠券领取/算价/锁定/释放、运费模板计算、评价审核回复、收藏/足迹实测通过 |
| 搜索与会员 | 热词记录、中文全文搜索、销量排序、积分奖励/退款扣回/等级、工单回复实测通过 |
| 管理后台构建 | `pnpm typecheck` / `pnpm build` 通过 |
| 小程序 | TypeScript 校验通过；售后页面需在微信开发者工具中编译验证 |

## 已知限制

- Android 端尚未接入微信 SDK 登录与真实微信支付，使用开发登录 + 模拟支付
- Android debug APK 允许 HTTP 明文，生产需切换 HTTPS 并重新签名
- 真实微信登录需配置 AppID/Secret；当前开发登录仅用于本地。
- 真实微信支付、回调验签与对账未实现，仅提供支付抽象与开发模拟支付；退款已实现模拟/线下手工登记，真实微信退款待接入。
- 管理员已支持自行改密与超级管理员重置密码；生产仍建议接入统一身份/密钥管理。
- 限流为单实例内存实现，多实例部署需替换为 Redis 等共享存储。
- Redis、对象存储未接入；优惠券、收藏、评价、会员积分、客服工单已落地，后续规划见 [核心功能评估与迭代计划](./docs/roadmap.md)。
- 微信订阅消息/短信未接入，当前通知为站内通知。
- Android 端已补齐售后、优惠券、评价、收藏/足迹、会员积分、客服工单与通知；仍使用开发登录与模拟支付。
- 优惠券首版每人每模板限领 1 张，未做复杂叠加/阶梯促销；评价未接图片上传。
- 搜索使用 MySQL ngram，未接 Elasticsearch/OpenSearch；积分不抵现、不设过期；工单无附件与推送。
- 发票仅记录抬头/税号/邮箱，未接入电子发票开具。
- CSV 导出单次最多 10000 行，未做异步大文件导出。
- 小程序当前使用原生组件，TDesign 依赖已安装但未使用。
- 演示图片地址指向 `localhost:8080`，真机需替换为可访问域名。
- 真实微信支付、退款、对账与物流轨迹的接入步骤见 [微信接入手册](./docs/wechat-integration.md) 与 [物流接入手册](./docs/logistics-integration.md)，当前外部渠道仍未接代码。

## 文档

- [架构设计](./docs/architecture.md)
- [数据库设计](./docs/database.md)
- [API 设计](./docs/api.md)
- [业务规则](./docs/business-rules.md)
- [开发说明](./docs/development.md)
- [二次开发指南](./docs/customization-guide.md)
- [发布检查清单](./docs/release-checklist.md)
- [微信登录与支付接入手册](./docs/wechat-integration.md)
- [物流接入手册](./docs/logistics-integration.md)
- [核心功能评估与迭代计划](./docs/roadmap.md)
- [小程序配置说明](./miniapp/README.md)
- [Android 客户端说明](./android/README.md)

## 阶段路线

1. ✅ 阶段 0：环境检查与设计文档
2. ✅ 阶段 1：三端骨架、统一响应、健康检查、数据库初始迁移
3. ✅ 阶段 2：分类/商品/SKU 查询、首页聚合、演示数据、小程序与后台只读接通
4. ✅ 阶段 3：小程序首页/分类/搜索/详情浏览与 SKU、数量选择
5. ✅ 阶段 4：开发登录、JWT 用户鉴权、收货地址、购物车
6. ✅ 阶段 5：结算、订单创建、订单查询/取消/确认收货、库存扣减与回补、自动取消
7. ✅ 阶段 6：管理员认证、后台 CRUD、订单发货、用户、轮播图、上传、仪表盘
8. ✅ 阶段 7：支付抽象、开发模拟支付、支付记录与订单支付状态流转
9. ✅ 阶段 8：回归测试、空库迁移验证、安全配置检查、二次开发指南与发布清单
10. ✅ R0：清理小程序假入口，补齐用户协议/隐私说明示例页
11. ✅ R1：售后/退款、库存流水、站内通知、管理后台售后与库存页面
12. ✅ R2：RBAC、管理员管理、操作日志、CSV 导出、自动确认收货、发票/备注/改地址、经营看板
13. ✅ R3：优惠券、运费模板、商品评价、收藏/浏览足迹
14. ✅ R4：搜索优化、客服工单、会员积分、首页运营位
15. ⏳ R5：真实支付/退款/物流、基础设施与上线（见 [路线图](./docs/roadmap.md)）
