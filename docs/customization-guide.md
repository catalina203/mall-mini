# 二次开发指南

> 目标：在不破坏核心交易流程的前提下，快速把 MallMini 改造成不同电商业务。

## 1. 修改商城名称、Logo、主题色

### 商城名称

当前品牌名没有集中在单一服务端配置中，修改时全局搜索并替换以下位置：

| 位置 | 说明 |
| --- | --- |
| `miniapp/utils/config.ts` | `brandName`、API 地址、开发登录 openId |
| `miniapp/app.json` | `navigationBarTitleText`、tabBar 文案 |
| `miniapp/pages/home/index.ts` / `.wxml` | 首页品牌文案 |
| `admin/index.html` | 浏览器标题 |
| `admin/src/layouts/AdminLayout.vue` | 后台品牌 |
| `admin/src/views/login/LoginView.vue` | 登录页品牌 |
| `README.md`、`docs/*` | 文档展示名 |

建议后续把品牌名、Logo、客服信息收敛到服务端配置接口，首版不为此增加复杂度。

### Logo 与图片

- 小程序：把 Logo 放入 `miniapp/assets/`（需新建），用 `<image>` 替换当前文字/头像占位；服务端静态示例图在 `server/src/main/resources/static/demo/`。
- 管理后台：把 Logo 放入 `admin/src/assets/`，在 `AdminLayout.vue` 与登录页引用。
- 生产图片建议使用对象存储，见第 7 节。

### 主题色

- 小程序：`miniapp/app.wxss` 及各页面 WXSS 中的 `#d9482b`、`#fdf2ee` 等颜色；组件 `components/*/index.wxss`。
- 管理后台：`admin/src/assets/main.css` 的 `--mall-primary`，以及 `AdminLayout.vue`、登录页、订单状态色。
- 修改后检查价格、按钮、状态标签、禁用态的对比度。

## 2. 增加商品字段

示例：为商品增加“品牌”字段。

1. 新增迁移脚本（禁止修改已执行的 `V1`）：

```sql
-- server/src/main/resources/db/migration/V2__add_product_brand.sql
ALTER TABLE product ADD COLUMN brand VARCHAR(64) NOT NULL DEFAULT '' AFTER subtitle;
```

2. 修改实体：`product/entity/Product.java` 增加 `brand`。
3. 修改接口契约：
   - 用户端 `ProductSummaryVO` / `ProductDetailVO`
   - 管理端 `AdminProductSummaryVO` / `AdminProductDetailVO`
   - `ProductSaveRequest`
4. 修改服务：`ProductService`、`AdminProductService` 的查询与保存。
5. 修改管理后台 `ProductListView.vue` 表单与列表。
6. 修改小程序商品详情/商品卡片（如需要展示）。
7. 更新 `docs/database.md`、`docs/api.md`。
8. 运行 `mvn test`、`pnpm typecheck`。

注意：

- Flyway 只增不改；已有环境升级与空库初始化都要验证。
- 金额仍使用 `DECIMAL(10,2)` / `BigDecimal`。
- 新增可筛选字段时同步增加索引与查询参数。

## 3. 增加 API

以“商品收藏”为例：

1. 新建包 `com.mallmini.favorite`，按需包含 `controller/service/mapper/entity/dto/vo`。
2. Mapper 继承 `BaseMapper<T>` 并加 `@Mapper`。
3. Controller 只负责参数、鉴权主体、调用 Service、返回 `ApiResponse`。
4. 私有接口放在 `/api/v1/me/**`，管理员接口放在 `/api/v1/admin/**`；Spring Security 已按路径控制角色。
5. 输入使用 Jakarta Validation；资源归属在 Service 内按 `userId` 校验。
6. 业务错误抛 `BusinessException(ErrorCode, message)`，由全局异常处理统一返回。
7. 更新 `docs/api.md` 与测试。
8. 管理员接口必须单独校验权限，不能依赖前端隐藏按钮。

## 4. 增加管理后台菜单和页面

1. 新建 `admin/src/api/xxx.ts`，统一通过 `api/request.ts` 发请求。
2. 新建 `admin/src/types/xxx.ts` 类型。
3. 新建 `admin/src/views/xxx/XxxView.vue`，使用 Element Plus 表格/表单/分页。
4. 在 `admin/src/router/index.ts` 添加子路由、`meta.title` 与 `meta.permission`。
5. 在 `admin/src/layouts/AdminLayout.vue` 添加菜单项并设置 `permission`；权限码在 `AdminPermissionService` 与 `AdminPermission` 中维护。
6. 写操作加确认、提交 loading、错误提示；列表处理加载/空/失败态。
7. 后端实现对应接口，用 `@PreAuthorize("hasAuthority('...')")` 校验权限，并写操作日志。
8. 运行 `pnpm typecheck && pnpm build`。

## 5. 增加小程序页面

1. 新建 `miniapp/pages/xxx/index.{ts,json,wxml,wxss}`。
2. 在 `miniapp/app.json` 的 `pages` 中注册路径。
3. 请求统一放 `miniapp/services/`，类型放 `miniapp/types/`。
4. 页面覆盖加载、空数据、失败重试、重复点击保护。
5. 图片使用 `components/fallback-image`，金额/日期使用 `utils/format.ts`。
6. 如需 tab 页：更新 `app.json` 的 `tabBar.list` 与 `custom-tab-bar/index.ts`，并在页面 `onShow` 调用 `syncTabBar`。
7. 页面必须连接真实 API；未实现功能应隐藏或明确标注。
8. **不要**把 AppSecret、支付密钥放入小程序。

## 6. 增加订单状态或业务规则

订单状态定义在 `server/.../order/enums/OrderStatus.java`，数据库 `mall_order.status` 为 `VARCHAR`，新增枚举通常不需要迁移。

新增状态时必须同步：

1. 枚举与合法转换表。
2. `OrderService` 中的受控业务方法（例如发货、取消、退款），使用条件更新检查旧状态。
3. 时间字段（如 `refunded_at`）与 `order_status_log`。
4. 用户端/管理端状态文案、标签颜色、筛选 Tab、可用操作（`utils/order.ts`）。
5. 管理员操作接口与权限校验。
6. 单元测试：合法转换、非法转换、并发重复操作、库存/金额影响。
7. `docs/business-rules.md`、`docs/api.md`。

禁止提供“任意修改订单状态”的接口。客户端只能调用语义化动作。

## 7. 替换图片存储服务

当前实现：

- 接口：`server/.../storage/service/StorageService.java`
- 本地实现：`LocalStorageService`
- 配置：`APP_STORAGE_LOCAL_DIR`
- 访问：`/uploads/**`（`WebMvcConfig`）

替换为对象存储：

1. 实现 `StorageService`，例如 `OssStorageService`。
2. 通过 `@ConditionalOnProperty` 或 `@Primary` 选择实现，避免同时启用两个 Bean。
3. 配置 Bucket、Endpoint、AccessKey/Secret（仅环境变量）。
4. 返回可公网访问的 HTTPS URL，或签名 URL。
5. 保留上传大小、类型、魔数校验；对象名使用随机 UUID，禁止使用用户原始文件名。
6. 更新 `docs/development.md`、`.env.example`（占位符）。
7. 生产建议加 CDN 与图片压缩；不要提交密钥。

## 8. 配置真实微信登录和支付

> 完整操作步骤、参数清单、回调验签、关单、退款与验收见 [`wechat-integration.md`](./wechat-integration.md)。本节只保留速查。

### 微信登录

1. 服务端配置 `WECHAT_APP_ID`、`WECHAT_APP_SECRET`（Secret 不得进入小程序或仓库）。
2. 小程序端 `wx.login` 获取 code，调用 `/api/v1/auth/wechat-login`。
3. 服务端调用微信 `jscode2session` 换取 openid，创建/复用用户并签发 JWT。
4. 生产必须使用 HTTPS 域名，配置小程序 request 合法域名。
5. 验收：新用户、老用户、无效 code、微信接口异常、账号禁用。

### 真实微信支付

当前 `PaymentGateway` 只有模拟实现，真实支付尚未接入。接入步骤：

1. 实现 `WechatPayGateway`，使用官方微信支付 API v3，配置商户号、私钥、证书序列号、API v3 Key。
2. 下单时创建本地 `payment_record`（PENDING），调用统一下单获取 prepay_id，返回小程序调起支付所需参数。
3. 新增支付回调接口（公开路径），必须：
   - 验证微信签名与证书
   - 校验 appid、mchid、商户订单号、金额、交易状态
   - 幂等处理重复通知
   - 更新订单支付状态与支付记录
4. 小程序调用 `wx.requestPayment`，支付成功后刷新订单；不要仅凭前端结果改订单。
5. 生产关闭模拟支付（`APP_PAYMENT_MOCK_ENABLED=false`），启动校验会强制阻止误开启。
6. 补充退款、对账、异常补偿与监控；单独验收后再上线。
7. 证书与密钥只放服务端安全存储或环境变量。

支付回调涉及外部网络，不应与长事务耦合；建议先落支付记录，再异步/幂等更新订单。

物流轨迹与自动确认收货见 [`logistics-integration.md`](./logistics-integration.md)。

## 9. 构建并部署

### 服务端

```bash
cd server
export JAVA_HOME=$(/usr/libexec/java_home -v 17)
mvn package
java -jar target/mall-mini-server-0.1.0-SNAPSHOT.jar
```

生产必须设置：

- `SPRING_PROFILES_ACTIVE=prod`
- 独立 `JWT_SECRET`（≥32 字符，不能使用开发默认值）
- `DB_URL` / `DB_USERNAME` / `DB_PASSWORD`
- `CORS_ALLOWED_ORIGINS` 精确白名单
- `APP_DEV_LOGIN_ENABLED=false`
- `APP_PAYMENT_MOCK_ENABLED=false`
- 管理员初始化密码（仅首次）
- 对象存储/微信/支付配置（如启用）

Flyway 启动自动迁移；生产保持 `clean-disabled=true`，迁移前备份。

### 管理后台

```bash
cd admin
pnpm install --frozen-lockfile
pnpm build
```

将 `dist/` 部署到 Nginx，反向代理 `/api` 与 `/uploads` 到服务端；配置 HTTPS、缓存与 SPA fallback。

### 小程序

1. 修改 `miniapp/utils/config.ts` 的 API 地址为 HTTPS 域名。
2. 用真实 AppID 打开/上传，配置服务器域名、隐私保护指引、业务类目。
3. 不要提交 `project.config.json`、AppSecret、证书。

### 数据库

- MySQL 8.0，utf8mb4。
- 备份后再执行迁移。
- 生产禁止加载演示数据（`DemoDataInitializer` 仅在 dev profile）。
- 管理员与演示数据均不内置弱口令。

## 10. 修改后的最低验证

- `mvn test` 全部通过。
- 空库 Flyway 迁移成功。
- 管理后台 `pnpm typecheck && pnpm build`。
- 小程序 `pnpm typecheck`，微信开发者工具编译。
- 下单 → 支付 → 发货 → 确认收货主流程。
- 库存不足、重复提交、越权、取消回补。
- 管理员权限与操作日志。
- 更新受影响文档。

## 11. Android 客户端定制

项目路径：`android/`，使用 Kotlin + Jetpack Compose，与小程序共用服务端 API。

### 品牌与主题

- App 名称：`android/app/src/main/res/values/strings.xml`
- 主题色：`android/app/src/main/java/com/mallmini/android/ui/theme/Color.kt`
- 应用图标：`res/mipmap-anydpi-v26/ic_launcher.xml` 与 `res/drawable/ic_launcher_foreground.xml`
- 首页品牌文案：`ui/screens/HomeScreen.kt`
- 默认服务器：`android/app/build.gradle.kts` 的 `DEFAULT_API_BASE_URL`

### 增加页面

1. 在 `ui/screens/` 新建 Composable 页面。
2. 在 `ui/MallApp.kt` 的 `Routes` 和 `NavHost` 注册路由。
3. 数据请求统一走 `data/MallRepository.kt` 与 `data/ApiService.kt`。
4. 需要登录的页面通过 `MallViewModel.loggedIn` 判断；不要把权限只放在 UI 层。
5. 加载/失败/空态使用 `ui/components` 中的通用组件。

### 修改 API 地址

- App 内「我的 → 服务器设置」运行时可改。
- 发布前通过 `BuildConfig.DEFAULT_API_BASE_URL` 设置生产 HTTPS 地址。
- `ApiConfig.resolveImage()` 会把演示数据的 `localhost` 图片重写到当前服务器，生产可移除该兼容逻辑。

### 发布注意

- 当前 Debug APK 允许 HTTP 明文；生产必须移除 `usesCleartextTraffic` 并配置 HTTPS。
- 使用正式签名替换 debug 签名，提升 `versionCode`/`versionName`。
- 接入微信登录/支付需要微信开放平台/商户资料与 Android SDK，当前未实现。
- 真机需与开发服务端同网，生产必须使用公网 HTTPS 域名。
- 完成真机主流程验收后再上架。
