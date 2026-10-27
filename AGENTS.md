# AGENTS.md --- MallMini 国内电商小程序脚手架

> 项目定位：可复用、可演示、可二次开发的国内电商基础脚手架。目标不是复制淘宝/京东，而是把常见电商业务的工程结构、核心数据模型、基础页面和关键交易流程提前搭好，方便后续按不同业务快速定制。

## 1. 项目目标与原则

构建一个完整可运行的国内电商项目模板，包含： -
微信小程序用户端：商品浏览、搜索、购物车、地址、下单、订单查询、个人中心。 -
Web
运营管理后台：管理员登录、商品/分类/SKU/库存/订单/用户/轮播图管理。 -
Java 服务端：认证授权、业务 API、订单与库存逻辑、持久化、接口文档。 -
本地开发环境：数据库初始化、演示数据、启动说明、测试和可选 Docker
Compose。 -
二次开发文档：模块说明、数据表、API、扩展点、部署步骤和常见修改指南。

核心原则： 1. **基本功能必须可用**：不是只有 UI 的展示项目；核心页面连接
API，关键业务有持久化和校验。 2.
**方便二次开发**：模块职责清楚、命名稳定、配置可替换，避免把业务规则写死在页面。
3.
**避免过度设计**：第一版采用模块化单体，不引入微服务、Kubernetes、复杂消息队列或分布式事务。
4. **优先打通闭环**：商品 → 购物车 → 地址 → 创建订单 →
订单管理，优先于复杂营销功能。 5.
**服务端可信**：价格、库存、权限和订单状态由服务端校验，不信任客户端提交的关键字段。
6. **本地可运行**：缺少微信
AppID、支付商户号或云服务凭证时，项目仍能通过开发模式运行和测试。 7.
**模拟与真实严格区分**：模拟登录/支付必须明确标记为开发模式，不得伪装成真实微信登录或真实支付。
8. **文档与代码同步**：API、表结构或启动方式变化时更新文档。 9.
**分阶段验证**：每一阶段都要可构建、可测试；禁止一次性生成所有代码却不运行验证。

## 2. 项目默认约定

-   项目名称：`MallMini`
-   中文展示名：`优选商城`，应支持配置修改。
-   Maven group / Java 基础包：`com.mallmini`
-   API 前缀：`/api/v1`
-   默认数据库：`mall_mini`
-   货币：人民币 CNY。
-   默认语言：简体中文。
-   首期商品类型：普通实物商品。
-   首期库存策略：下单时在数据库事务内条件扣减；取消符合条件的未支付订单时幂等回补。若采用其他策略，必须更新架构文档并测试。
-   首期支付：开发环境可使用明确标记的模拟支付适配器；生产环境禁止启用模拟支付。
-   管理员通过安全初始化流程创建；不得在生产环境内置固定弱口令。
-   品牌名、域名、API 地址、存储和外部服务优先使用配置，不要散落硬编码。

## 3. 技术栈

开发前核实官方文档和依赖兼容性，使用稳定且明确锁定的版本，禁止使用
SNAPSHOT 或未经验证的依赖。

### 3.1 微信小程序

-   微信原生小程序 + TypeScript。
-   WXML、WXSS、JSON 页面配置。
-   TDesign Miniprogram 作为首选 UI 组件库，按所选版本官方文档配置。
-   微信开发者工具。
-   统一 API Client、类型定义、登录态管理和错误处理。
-   不引入职责重复的多个 UI 组件库。

### 3.2 Web 管理后台

-   Vue 3、TypeScript、Vite。
-   Vue Router、Pinia、Element Plus、Axios。
-   ECharts 仅在确实实现统计图表时添加。

### 3.3 Java 服务端

-   Java 21。
-   Spring Boot 3.x，选择兼容 Java 21 的稳定版本并锁定。
-   Maven、Spring Web、Spring Validation、Spring Security。
-   MyBatis-Plus、MySQL Connector/J。
-   Springdoc OpenAPI。
-   JUnit 5、Mockito、Spring Boot Test。
-   建议 Flyway 管理数据库迁移。
-   Redis 为可选组件；第一版没有实际场景时可暂不接入。
-   JWT 或服务端会话方案择一并保持一致；若使用
    JWT，密钥必须来自环境变量。

### 3.4 本地运行与部署

-   MySQL 8.0。
-   Redis（可选）。
-   Docker / Docker Compose（简化本地基础设施启动）。
-   Nginx（部署后台静态资源或反向代理时使用）。
-   `.env.example` 只放变量名和占位示例，不包含真实凭证。
-   图片存储通过接口隔离：本地开发适配器与生产对象存储适配器分离。

不要为了"全栈完整"强制依赖所有基础设施。README
必须说明必需服务与可选服务。

## 4. 推荐仓库结构

``` text
mall-mini/
├── AGENTS.md
├── README.md
├── .gitignore
├── .env.example
├── docker-compose.yml
├── docs/
│   ├── architecture.md
│   ├── database.md
│   ├── api.md
│   ├── development.md
│   ├── customization-guide.md
│   ├── business-rules.md
│   └── release-checklist.md
├── miniapp/
│   ├── app.ts
│   ├── app.json
│   ├── app.wxss
│   ├── project.config.example.json
│   ├── pages/
│   │   ├── home/
│   │   ├── category/
│   │   ├── search/
│   │   ├── product-detail/
│   │   ├── cart/
│   │   ├── checkout/
│   │   ├── orders/
│   │   ├── order-detail/
│   │   ├── address-list/
│   │   ├── address-edit/
│   │   └── profile/
│   ├── components/
│   ├── services/
│   ├── store/
│   ├── types/
│   ├── utils/
│   └── custom-tab-bar/
├── android/                Android 客户端（Kotlin + Compose）
├── admin/
│   ├── package.json
│   ├── vite.config.ts
│   └── src/
│       ├── api/
│       ├── assets/
│       ├── components/
│       ├── layouts/
│       ├── router/
│       ├── stores/
│       ├── types/
│       └── views/
│           ├── login/
│           ├── dashboard/
│           ├── products/
│           ├── categories/
│           ├── orders/
│           ├── users/
│           └── banners/
└── server/
    ├── pom.xml
    └── src/
        ├── main/java/com/mallmini/
        │   ├── MallMiniApplication.java
        │   ├── common/
        │   │   ├── api/
        │   │   ├── exception/
        │   │   ├── pagination/
        │   │   └── util/
        │   ├── config/
        │   ├── security/
        │   ├── auth/
        │   ├── user/
        │   ├── admin/
        │   ├── category/
        │   ├── product/
        │   ├── inventory/
        │   ├── cart/
        │   ├── address/
        │   ├── order/
        │   ├── payment/
        │   ├── banner/
        │   └── storage/
        └── main/resources/
            ├── application.yml
            └── db/migration/
```

目录可根据实际工程微调，但 Controller、业务服务、数据访问、DTO
和实体职责必须清楚。

## 5. 用户端功能需求

### 5.1 首页

-   轮播图，支持排序、启用/停用、跳转到商品或分类。
-   商品分类入口、推荐商品、新品/热销区块。
-   首页内容由服务端返回；没有运营配置时展示合理默认内容。
-   图片加载失败有占位图，不得导致页面崩溃。

### 5.2 分类与搜索

-   分类列表、分类商品列表、分页或触底加载。
-   关键词搜索、清空关键词、分页、无结果状态。
-   最近搜索记录可以保存在本地，不要求上传服务端。
-   首版不引入复杂搜索引擎，先使用数据库索引和合理查询。
-   仅展示有效分类和上架商品。

### 5.3 商品详情

-   商品名称、描述、图片、价格、库存状态、SKU 规格。
-   选择规格和数量，支持加入购物车、立即购买。
-   对下架商品、无效 SKU、库存不足提供清楚反馈。
-   页面价格仅供展示；下单时由服务端重新计算。

### 5.4 购物车

-   查看商品、勾选/取消勾选、修改数量、删除商品。
-   显示小计与总金额。
-   检查商品下架、SKU 失效、库存变化。
-   登录用户购物车保存到数据库；如实现游客购物车，必须定义合并策略。
-   客户端总金额不能作为最终订单金额。

### 5.5 收货地址

-   地址列表、新增、编辑、删除、设置默认地址。
-   字段包含收货人、联系电话、省市区、详细地址、默认标记。
-   用户只能管理自己的地址。
-   删除/更换默认地址时保证状态一致。
-   日志不记录完整手机号或详细地址。

### 5.6 结算与创建订单

-   展示商品、地址、商品金额、运费、优惠金额和应付金额。
-   首期运费规则可采用配置化固定运费或满额包邮，并写入业务文档。
-   服务端重新校验商品、SKU、价格、库存、地址归属与优惠规则。
-   服务端生成订单号，订单明细保存商品快照。
-   事务内完成订单写入与库存处理。
-   防止重复点击导致重复下单，提供幂等策略或请求标识。
-   失败时不留下半成品数据，并返回可理解的错误信息。

### 5.7 订单列表与详情

-   状态包含待付款、待发货、待收货、已完成、已取消，前后端定义一致。
-   展示商品、地址快照、金额明细、时间和物流信息。
-   用户可取消符合规则的未支付订单。
-   用户可在合法状态下确认收货。
-   用户只能查看和操作自己的订单。
-   不允许客户端任意指定目标状态。

### 5.8 个人中心

-   登录状态和基础资料。
-   我的订单、地址管理入口。
-   用户协议、隐私说明入口。
-   退出登录。
-   不收集业务不需要的个人信息。

## 6. Web 管理后台功能需求

### 6.1 通用能力

-   管理员登录、退出和权限控制。
-   统一布局、菜单、面包屑、分页、加载态、空态和错误反馈。
-   表单校验、提交状态、重复提交保护。
-   删除、下架、发货等危险操作需二次确认。
-   前端路由守卫不能代替后端权限验证。

### 6.2 仪表盘

-   展示订单数、销售额、待处理订单等基础指标。
-   明确统计时间范围和口径。
-   没有真实数据时展示空态或明确标注的演示数据，不伪造经营成果。

### 6.3 分类管理

-   分类增删改查、排序、启用/停用。
-   层级分类需校验父子关系并防止循环引用。
-   有商品关联的分类删除时，按明确规则阻止删除或要求先迁移商品。

### 6.4 商品与 SKU 管理

-   商品新增、编辑、详情、上下架。
-   商品标题、描述、图片、分类、SKU、价格、库存等字段。
-   校验价格、库存、图片地址和分类。
-   已关联历史订单的商品不能被破坏性物理删除。

### 6.5 订单管理

-   分页查询，按订单号、用户、状态筛选。
-   查看订单明细与地址快照。
-   对合法状态的订单执行发货并记录物流公司、物流单号。
-   状态变更写入订单状态日志。
-   禁止通过无约束下拉框任意修改订单状态。

### 6.6 用户管理

-   查看必要的用户资料、注册时间和状态。
-   默认不展示完整手机号、地址等敏感数据。
-   用户状态变更记录操作人和时间。
-   管理员不能读取用户密码或令牌。

### 6.7 轮播图管理

-   图片 URL、标题、跳转类型/目标、排序、启用状态。
-   校验跳转类型与目标匹配。
-   用户端只展示启用的轮播图。

## 7. 服务端架构与 API 规范

### 7.1 分层职责

业务模块按规模采用以下结构： - `controller`：HTTP
接口和参数接收，不放复杂业务逻辑。 -
`service`：业务规则、事务边界、状态校验。 - `mapper`：数据库访问。 -
`entity`：持久化实体。 - `dto`：请求对象。 - `vo`：响应对象。 -
`enums`：状态与类型枚举。 -
`converter`：只有转换复杂时才建立，不为形式而增加空层。

简单模块可以合并目录，但不得把所有业务塞进一个超大 Controller 或
Service。

### 7.2 API 约定

-   REST API 前缀统一 `/api/v1`。
-   用户公开、用户私有、管理员接口按路径和权限区分，例如：
    -   `GET /api/v1/products`
    -   `GET /api/v1/products/{id}`
    -   `GET /api/v1/categories`
    -   `GET /api/v1/me/cart`
    -   `POST /api/v1/me/orders`
    -   `GET /api/v1/me/orders`
    -   `GET /api/v1/admin/products`
    -   `POST /api/v1/admin/products`
    -   `POST /api/v1/admin/orders/{id}/ship`
-   统一响应结构，例如 `code`、`message`、`data`、`requestId`。
-   分页结构统一包含 `items`、`page`、`pageSize`、`total`。
-   合理使用 HTTP
    状态码区分参数错误、未认证、无权限、资源不存在、冲突和服务端错误；不要所有请求都返回
    HTTP 200。
-   使用 DTO 和 VO，不直接暴露数据库实体作为 API 契约。
-   使用 Validation 校验输入，错误信息不泄露堆栈。
-   OpenAPI 文档与实际实现同步。
-   小程序和后台通过环境配置读取 API 地址，不在多个页面硬编码本机地址。

### 7.3 异常与日志

-   全局异常处理统一响应格式。
-   业务错误返回明确错误码与用户可理解的消息。
-   日志使用 requestId 等标识帮助排查。
-   不记录密码、Authorization Token、支付密钥或不必要的个人信息。
-   生产环境关闭调试错误页和详细堆栈。

## 8. 数据库设计与迁移

至少评估以下数据表，并根据实现落地： 1.
`user`：用户基础信息、微信标识、状态、创建/更新时间。 2.
`admin_user`：管理员账号、密码哈希、状态和角色。 3.
`category`：分类名称、父级 ID、排序、状态。 4.
`product`：商品名称、描述、主图、分类、上架状态、排序。 5.
`product_sku`：SKU 编码、规格信息、价格、库存、状态。 6.
`product_image`：商品图片和排序（需要独立管理时）。 7.
`cart_item`：用户、SKU、数量和时间字段。 8.
`user_address`：用户、收货人、电话、地区、详细地址、默认标记。 9.
`mall_order`：订单号、用户、状态、商品金额、优惠金额、运费、应付金额、支付状态、地址快照和时间。
10. `mall_order_item`：订单、商品/SKU 快照、数量、成交单价、小计。 11.
`order_status_log`：状态变更前后值、操作人、原因、时间。 12.
`payment_record`：支付渠道、商户订单号、交易号、金额、支付状态、回调处理状态。
13. `banner`：图片、标题、跳转类型/目标、排序、启用状态。 14. 按需增加
`admin_operation_log`、优惠券、收藏、评价、售后等表。

数据库规则： - 使用 Flyway
等版本化迁移工具管理结构变化，不依赖未记录的手工 SQL。 - 金额使用数据库
`DECIMAL` 和 Java `BigDecimal`，禁止用 `float`/`double` 计算金额。 -
统一时间字段策略并在文档中说明。 - 订单号、SKU
编码等业务标识建立唯一索引。 - 为常见过滤、关联和排序字段设计索引。 -
外键策略必须明确说明，保证数据一致性。 -
订单明细保存商品、SKU、规格、单价等快照。 - 库存不得为负。 -
用户私有数据必须进行用户 ID 权限隔离。 -
演示数据只用于开发测试，生产环境不得自动创建演示账号或演示订单。 -
迁移脚本必须同时考虑空库初始化和已有环境升级，不能随意删除生产字段。

## 9. 核心业务规则

### 9.1 金额

-   订单金额由服务端根据数据库中有效的商品价格重新计算。
-   客户端传来的总金额、优惠金额、运费不可信。
-   金额精度与舍入规则统一。
-   创建订单时保存价格快照。
-   首期未实现优惠券时，优惠金额明确为 0。

### 9.2 库存与并发

-   下单时重新校验库存。
-   使用条件更新等并发安全方式扣减库存，并检查影响行数。
-   多 SKU 订单需定义扣减顺序和失败回滚行为。
-   库存不足时整个订单创建失败，不能留下部分订单明细。
-   取消订单的库存回补必须幂等。
-   测试并发下单不会出现负库存。

### 9.3 订单状态机

建议订单状态： - `PENDING_PAYMENT`：待付款 -
`PENDING_SHIPMENT`：待发货 - `PENDING_RECEIPT`：待收货 -
`COMPLETED`：已完成 - `CANCELLED`：已取消

支付状态应单独建模，避免把订单状态与支付状态混为一谈。所有状态变更通过业务方法校验合法转换，并记录操作人、时间和必要原因。客户端不能直接传状态值完成订单。

### 9.4 幂等性与事务

-   创建订单防止重复提交。
-   支付/退款回调（若实现）和库存回补必须幂等。
-   订单主表、明细和库存操作有明确事务边界。
-   事务中不执行耗时外部网络请求。
-   外部支付成功但本地更新失败时，提供可排查、可重试方案；未配置商户资料时只实现接口与状态模型。

### 9.5 用户与权限

-   用户只能读取或修改自己的购物车、地址、订单。
-   管理员接口必须单独鉴权。
-   不得仅靠前端隐藏按钮控制权限。
-   更新对象时校验资源归属，防止修改 ID 越权访问。

## 10. 登录、支付和外部服务

### 10.1 登录

-   本地开发模式提供明确标识的开发登录方式。
-   真实微信登录由小程序端获取临时
    `code`，服务端调用微信官方接口换取身份信息。
-   AppSecret 只能放服务端环境变量，不得打包进小程序。
-   真实登录与开发登录配置隔离，生产环境禁止开发登录。
-   管理员与普通用户的身份和权限分开。

### 10.2 支付

-   首期实现支付抽象接口、订单支付状态和开发测试流程。
-   模拟支付只在明确开启的本地开发配置中可用，界面和日志标记"测试支付"。
-   生产环境配置不完整时必须禁用支付或明确报错，不可静默模拟成功。
-   真实微信支付按官方当前文档接入，配置商户号、证书/密钥、API v3 Key
    和回调 URL。
-   支付回调必须验证签名、商户订单号、金额和交易状态，并幂等处理。
-   不允许客户端调用普通接口直接将订单标记为已支付。
-   支付证书和密钥不能进入仓库。

### 10.3 外部服务

-   物流、短信、对象存储通过接口/适配器隔离。
-   没有凭证时可在开发环境使用明确标记的 Stub 或本地适配器。
-   Stub 不得在生产环境误启用。
-   首版不无故接入短信、复杂物流查询或第三方搜索服务。

## 11. 代码规范

### 11.1 小程序

-   页面负责展示和交互；请求统一放在 `services/`。
-   API、错误处理、Token 处理集中封装。
-   公共结构放在 `types/`。
-   处理登录过期、网络失败、服务器错误、空列表和重复点击。
-   金额、日期、图片占位等建立可复用工具。
-   服务端密钥不能放进小程序。
-   `project.config.json` 中的个人 AppID
    或本机路径应使用示例配置，不提交个人凭证。

### 11.2 管理后台

-   页面、API、类型、状态管理分离。
-   Axios 统一实例、超时、错误处理和鉴权注入。
-   路由守卫只负责体验层控制，权限最终由后端验证。
-   表格统一分页参数和响应格式。
-   表单提交期间禁用重复操作。
-   避免 `any` 泛滥，优先定义明确类型。
-   不在浏览器端保存不必要的长期敏感凭证。

### 11.3 Java

-   遵循 Java 命名规范。
-   Controller 不堆叠业务逻辑，Service 负责业务校验和事务，Mapper
    负责数据访问。
-   DTO、Entity、VO 职责分离。
-   对写操作、金额、状态转换和库存逻辑编写测试。
-   避免无意义的接口/实现类层层转发；抽象应服务于可替换性或清晰边界。
-   按开发、测试、生产环境分离配置。
-   不吞异常、不写空 `catch`，不以打印堆栈替代正确错误处理。

## 12. UI/UX 要求

### 用户端

-   移动端优先，兼容常见微信屏幕与安全区域。
-   首页、分类、购物车、个人中心形成清晰主导航。
-   风格简洁可信，接近主流国内商城，但不复制其他品牌的
    Logo、文案或专有视觉资产。
-   商品价格、规格、库存和购买按钮层级清楚。
-   所有异步页面覆盖加载、空数据、失败重试、成功和失败反馈。
-   按钮不能只是装饰；未实现的功能应隐藏或明确标记未开放。
-   演示商品应标记为样例，不伪造真实评价、销量或折扣。

### 管理端

-   统一后台布局、菜单、面包屑和权限提示。
-   列表支持筛选、分页、详情查看。
-   表单标签、校验和错误信息清晰。
-   危险操作二次确认。
-   订单状态与业务操作相匹配，不能任意下拉修改状态。

## 13. 测试与验收标准

### 后端必须覆盖

-   商品列表只返回上架商品。
-   搜索、分类筛选、分页和无结果场景。
-   未登录访问受保护接口会被拒绝。
-   用户不能查看其他用户的订单和地址。
-   篡改价格/总金额不影响服务端计算结果。
-   库存不足时订单创建失败且无脏数据。
-   并发购买不会产生负库存。
-   重复提交不会重复扣库存或创建重复订单。
-   非法订单状态转换被拒绝。
-   取消订单的库存回补只生效一次。
-   管理员接口拒绝未授权用户。
-   支付回调（若实现）校验签名/金额并能处理重复通知。
-   数据库迁移能在空数据库中执行。

### 前端必须验证

-   小程序可在微信开发者工具中编译并打开首页。
-   首页、分类、搜索、详情、购物车、地址、下单、订单、个人中心形成基本流程。
-   管理后台能完成登录、商品 CRUD、分类 CRUD、订单查询和发货操作。
-   网络失败、无数据、登录过期有可理解反馈。
-   管理后台生产构建通过，无明显类型错误或运行时错误。

### 交付验收

-   本地启动步骤可复现。
-   有数据库迁移和演示数据。
-   有 API 文档与业务规则说明。
-   无硬编码生产密钥。
-   无仅为展示而伪装可用的核心按钮。
-   未实现功能列入后续扩展清单。
-   测试失败必须修复或明确记录原因，不能声称通过。

## 14. 二次开发友好性

这是可复用脚手架，减少重复开发是首要价值。

### 14.1 品牌与基础配置

-   商城名称、Logo、主题色、客服信息、运费规则、默认分页大小和存储策略通过配置管理。
-   不把品牌文案散落在所有页面。
-   文档说明如何替换品牌与环境配置。

### 14.2 商品模型

-   首版支持普通实物商品和 SKU。
-   规格结构应可扩展，但不提前设计复杂的任意属性规则引擎。
-   虚拟商品、预约服务、多商户等需求出现后再设计领域扩展，不使用大量
    nullable 字段强行兼容所有场景。

### 14.3 业务规则

-   运费、订单取消时限、自动确认收货时限、是否允许游客购物等优先配置化。
-   规则集中在服务端，不在小程序与后台复制两套不同逻辑。
-   影响资金或库存的规则必须有测试。

### 14.4 外部集成

-   支付、存储、物流、短信通过接口隔离。
-   替换供应商时尽量不重写商品和订单核心业务。
-   每个适配器说明配置项、错误处理、测试方式和生产启用条件。

### 14.5 定制指南

`docs/customization-guide.md` 至少说明： 1. 修改商城名称、Logo
和主题色。 2. 增加商品字段。 3. 增加 API。 4. 增加管理后台菜单和页面。
5. 增加小程序页面。 6. 增加订单状态或业务规则。 7. 替换图片存储服务。 8.
配置真实微信登录和支付。 9. 构建并部署到测试/生产环境。

## 15. 分阶段实施流程

必须按阶段实现，每阶段都要可构建、可测试。

### 阶段 0：检查与设计

-   检查仓库、Git 状态、Node.js、包管理器、Java、Maven、Docker 等环境。
-   识别已有内容，不覆盖用户成果。
-   输出技术决策、目录结构、数据模型、API 规划、订单状态机和验收标准。
-   创建/更新
    `docs/architecture.md`、`docs/database.md`、`docs/api.md`、`docs/business-rules.md`。
-   明确必需服务与可选服务。

### 阶段 1：工程骨架

-   初始化小程序、Vue 3 管理后台和 Spring Boot 后端。
-   建立统一配置、代码结构、错误处理基础和健康检查。
-   提供 `.env.example`、`.gitignore`、README。
-   配置数据库迁移初始结构。
-   验证三端分别能构建/启动。

### 阶段 2：后端公共层与商品基础

-   完成统一响应、异常处理、参数校验、分页和 OpenAPI。
-   完成分类、商品、SKU 模型与查询接口。
-   添加演示分类和商品数据。
-   编写测试。
-   小程序与后台接通商品查询 API。

### 阶段 3：用户端浏览流程

-   完成首页、分类、搜索、商品详情和规格选择。
-   完成加载、空态、错误态。
-   验证上下架、分页、库存状态和 SKU 选择。

### 阶段 4：认证、地址与购物车

-   完成开发登录与生产登录配置隔离。
-   完成用户权限基础。
-   完成地址和购物车 API/页面。
-   测试用户数据隔离。

### 阶段 5：订单与库存闭环

-   完成结算、订单创建、订单列表和详情。
-   完成取消订单、库存回补、状态日志。
-   完成价格重算、库存并发、重复提交和非法状态转换测试。
-   本阶段结束时必须能完成浏览商品、加购、下单和查看订单的完整流程。

### 阶段 6：管理后台

-   完成管理员认证和权限。
-   完成分类、商品、SKU、订单、用户、轮播图管理。
-   完成发货和物流单号维护。
-   完成基础仪表盘并说明统计口径。
-   验证管理员 API 权限。

### 阶段 7：集成边界与可选扩展

-   完成支付适配接口和明确标记的本地模拟支付（仅开发环境）。
-   按需添加 Redis、对象存储适配器、优惠券等。
-   没有外部凭证时，不得假装真实服务已接通。
-   真实微信登录与支付按官方流程配置并单独验收。

### 阶段 8：回归测试与交付

-   运行服务端测试。
-   运行后台类型检查与生产构建。
-   检查小程序编译。
-   检查安全配置、环境变量、迁移脚本和演示数据。
-   完善 README、API 文档、二次开发指南和发布清单。
-   汇报已完成、未完成、测试结果和部署前置条件。

## 16. 必须交付的文件与文档

-   `README.md`：项目简介、功能、技术栈、环境要求、启动步骤。
-   `.env.example`：配置项示例，无真实凭证。
-   `docker-compose.yml`：若使用 Docker，提供可复现的本地基础设施配置。
-   数据库迁移脚本与开发演示数据。
-   `docs/architecture.md`：架构和模块边界。
-   `docs/database.md`：表结构、字段、索引与迁移策略。
-   `docs/api.md`：主要接口、认证要求、请求/响应示例。
-   `docs/business-rules.md`：价格、库存、订单状态和取消规则。
-   `docs/customization-guide.md`：二次开发指南。
-   `docs/development.md`：本地运行、调试和测试说明。
-   `docs/release-checklist.md`：上线前检查项。
-   安全的本地开发账号创建说明，不提供固定生产密码。

README
必须提供可复制执行的命令，并说明在哪个目录执行，不能只写"安装依赖并启动"。

## 17. 安全与上线要求

-   密码使用安全哈希保存。
-   生产密钥、数据库密码、微信 AppSecret、支付证书/密钥不进入仓库。
-   生产环境禁用开发登录和模拟支付。
-   CORS 使用明确白名单。
-   对认证、订单创建、支付回调和高风险操作实施合理限流与审计。
-   文件上传校验大小、类型和内容，防止任意路径写入。
-   防止 SQL 注入、越权、重复提交、库存超卖和不安全状态变更。
-   日志不输出 Token、密码和支付密钥。
-   上线前核对小程序主体、备案、隐私保护指引、域名
    HTTPS、服务器、微信平台配置和支付商户要求。
-   不承诺仅凭代码即可绕过平台审核或资质要求。

## 18. Agent 工作规范

每次执行任务： 1. 阅读本文件和相关模块文档。 2. 检查 Git
diff、工作区状态和已有实现。 3. 明确本次任务范围及影响模块。 4.
先给出简短计划，再执行修改。 5. 做小而完整的改动，避免无关重构。 6.
修改后执行相关测试、类型检查或构建。 7.
测试失败时先定位并修复；若环境限制无法执行，明确说明，不能虚报通过。 8.
更新受影响文档。 9.
最终用中文汇报：完成内容、修改文件、检查结果、未完成事项、下一步建议。
10.
缺少外部凭证时，不要停止整个项目；先完成可测试的抽象和业务流程，并说明真实接入条件。
11.
未经用户确认，不执行不可逆操作、删除数据、覆盖已有文件或部署生产环境。
12.
不要把"生成了代码"当作"功能完成"；只有实际检查、测试或可复现手动验证后才能报告完成。

## 19. 立即执行的第一条任务

从**阶段
0：检查与设计**开始。先检查当前仓库和开发环境，确认是否已有代码；然后给出简明实施计划和架构决策，创建必要的设计文档。之后按阶段逐步实现。

最终目标是交付一个**基础功能可用、交易流程闭环、结构清晰、便于快速改造成不同国内电商业务的脚手架**。不要做成只有页面的
Demo，也不要过度建设成复杂的企业级微服务平台。

## 20. 本地启动说明（当前实现）

> 本节记录当前仓库可直接执行的本地启动方式。生产部署另见
> `docs/release-checklist.md` 与 `docs/customization-guide.md`。

### 20.1 环境约定

| 组件 | 当前要求 |
| --- | --- |
| JDK | 17+（本机 17.0.15，代码兼容 21） |
| Maven | 3.9+ |
| Node.js | ≥ 20 |
| pnpm / npm | pnpm 10 或 npm 10 |
| MySQL | 8.0+，推荐 Docker Compose（本机 3307） |
| 微信开发者工具 | 运行小程序时需要，可选 |

先确认：

```bash
export JAVA_HOME="$(/usr/libexec/java_home -v 17)"
echo "$JAVA_HOME"
"$JAVA_HOME/bin/java" -version
mvn -v
```

如果 `mvn` 报 `JAVA_HOME ... not defined correctly`：

1. 确认 `export` 与 `mvn` 在同一个终端会话执行。
2. 使用固定路径：
   `export JAVA_HOME="/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home"`
3. 重新执行 `mvn -v`，必须看到 `Java version: 17.x`。

### 20.2 启动数据库（Docker，推荐）

```bash
cd /Users/catalina/Workspace/module/mall-mini
colima status || colima start
docker compose up -d mysql
docker compose ps
```

预期：

- 容器名：`mall-mini-mysql`
- 状态：`healthy`
- 本机端口：`3307`
- 数据库：`mall_mini`
- 开发账号：`mallmini` / `mallmini_dev`
- root 密码仅用于本地容器，生产禁止使用

Homebrew 的 Compose 插件若提示 `docker: 'compose' is not a docker command`，
在 `~/.docker/config.json` 增加：

```json
{
  "cliPluginsExtraDirs": ["/opt/homebrew/lib/docker/cli-plugins"]
}
```

本机 MySQL（3306）也可使用，但需自行创建库与账号：

```sql
CREATE DATABASE IF NOT EXISTS mall_mini
  DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
CREATE USER IF NOT EXISTS 'mallmini'@'localhost' IDENTIFIED BY 'mallmini_dev';
GRANT ALL PRIVILEGES ON mall_mini.* TO 'mallmini'@'localhost';
FLUSH PRIVILEGES;
```

> 不要混用 3306/3307。使用 Docker 时，服务端必须设置下方 `DB_URL`。

### 20.3 启动服务端

推荐使用 `server/.env` + 启动脚本：

```bash
cd /Users/catalina/Workspace/module/mall-mini/server
cp .env.example .env   # 首次
./run-dev.sh
```

停止：`./stop-dev.sh`。脚本自动加载 `.env`、选择 JDK 17 并执行 `mvn spring-boot:run`。

手动方式：

```bash
export JAVA_HOME="$(/usr/libexec/java_home -v 17)"
export SPRING_PROFILES_ACTIVE=dev
export DB_URL='jdbc:mysql://localhost:3307/mall_mini?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true'
export DB_USERNAME=mallmini
export DB_PASSWORD=mallmini_dev
mvn spring-boot:run
```

启动后验证：

```bash
curl http://localhost:8080/api/v1/health
# 期望：data.status=UP，data.database=UP
```

- Swagger：http://localhost:8080/swagger-ui.html（仅 dev/test）
- Flyway 启动时自动迁移
- dev profile 下分类表为空时自动导入演示数据
- 服务端默认端口：8080

后台运行与停止：

```bash
nohup mvn spring-boot:run > /tmp/mallmini-server.log 2>&1 &
# 停止
lsof -ti:8080 | xargs kill
```

生产 profile 必须：

```bash
export SPRING_PROFILES_ACTIVE=prod
export JWT_SECRET="$(openssl rand -base64 48)"
export APP_DEV_LOGIN_ENABLED=false
export APP_PAYMENT_MOCK_ENABLED=false
```

生产使用开发默认 `JWT_SECRET`、开发登录或模拟支付时，启动校验会直接失败。

### 20.4 启动管理后台

```bash
cd /Users/catalina/Workspace/module/mall-mini/admin
pnpm install
pnpm dev
```

- 地址：http://localhost:5173
- `/api` 与 `/uploads` 由 Vite 代理到 `http://localhost:8080`
- 管理员账号通过服务端 `APP_ADMIN_INIT_*` 初始化，仓库不内置固定密码
- 生产构建：`pnpm build`，产物在 `admin/dist/`

### 20.5 运行小程序

```bash
cd /Users/catalina/Workspace/module/mall-mini/miniapp
cp project.config.example.json project.config.json   # 已有本地配置时跳过
pnpm install
pnpm typecheck
```

微信开发者工具 → 导入项目：

- 目录：`/Users/catalina/Workspace/module/mall-mini/miniapp`
- AppID：测试号或自有 AppID
- 后端服务：不使用云服务
- 本地设置勾选「不校验合法域名」
- 配置已包含 `useCompilerPlugins: ["typescript"]`，不要移除

本地登录：

- 「我的」→「开发登录（测试用）」
- 开发登录仅在 `dev` profile 且 `APP_DEV_LOGIN_ENABLED=true` 时可用
- 不是真实微信登录，生产环境禁止开启

真机预览需将 `miniapp/utils/config.ts` 的 `apiBaseUrl` 改为
`http://<Mac 局域网 IP>:8080/api/v1`，并确保手机与 Mac 同网。

### 20.6 停止服务

```bash
lsof -ti:8080 | xargs kill   # 服务端
lsof -ti:5173 | xargs kill   # 管理后台
cd /Users/catalina/Workspace/module/mall-mini
docker compose stop mysql
```

### 20.7 常见启动问题

| 现象 | 处理 |
| --- | --- |
| `JAVA_HOME ... not defined correctly` | 在同一终端 export；或使用固定 JDK 路径；`mvn -v` 验证 |
| 8080/5173 被占用 | `lsof -ti:<端口> \| xargs kill` |
| 数据库连接失败 | 确认容器 healthy；Docker 用 3307 并设置 `DB_URL` |
| 管理员无法登录 | 确认库中已有管理员；空库首次启动时设置 `APP_ADMIN_INIT_*` |
| 小程序 `.ts` 不编译 | 确认 `project.config.json` 含 `useCompilerPlugins: ["typescript"]` |
| 小程序请求失败 | 服务端已启动；勾选不校验域名；真机改用局域网 IP |
| 生产启动失败 | 检查独立 `JWT_SECRET`，关闭开发登录与模拟支付 |

### 20.8 当前开发库说明

- 本地 Docker MySQL 数据卷会保留已有用户、订单和管理员。
- 需要重置开发数据时，确认无保留数据后执行 `docker compose down -v`，再重新启动。
- 演示数据只在 `dev` profile 且分类表为空时导入。
- 任何真实密钥、AppSecret、支付证书、管理员密码都不得写入仓库或本文件。

## 21. Android 客户端（当前实现）

Android 端为原生 Kotlin + Jetpack Compose 工程，复用现有 `/api/v1` 接口，不复制服务端业务规则。

### 21.1 环境

| 组件 | 要求 |
| --- | --- |
| JDK | 17 |
| Android SDK | platform 35、build-tools 35.0.0、platform-tools |
| Gradle | 项目自带 wrapper 8.10.2 |
| 最低系统 | Android 8.0（minSdk 26） |
| 目标系统 | Android 15（targetSdk 35） |

当前机器 SDK：

```text
/opt/homebrew/share/android-commandlinetools
```

`android/local.properties` 中的 `sdk.dir` 用于本地构建，不提交仓库。

### 21.2 功能范围（MVP）

- 首页、分类、搜索、商品详情与 SKU 选择
- 购物车、收货地址、结算、创建订单
- 订单列表/详情、取消、模拟支付、确认收货
- 开发登录、服务器地址切换、退出登录
- 加载、错误、空数据状态

未实现：微信 SDK 登录、真实微信支付、推送、离线缓存、三级地区联动。

### 21.3 构建与安装

```bash
cd /Users/catalina/Workspace/module/mall-mini/android
export JAVA_HOME="$(/usr/libexec/java_home -v 17)"
export ANDROID_HOME=/opt/homebrew/share/android-commandlinetools
export ANDROID_SDK_ROOT=$ANDROID_HOME
./gradlew :app:assembleDebug
```

APK：

```text
android/app/build/outputs/apk/debug/app-debug.apk
```

安装到已开启 USB 调试的 Android 手机：

```bash
export PATH="$ANDROID_HOME/platform-tools:$PATH"
adb devices
adb install -r android/app/build/outputs/apk/debug/app-debug.apk
```

也可把 APK 传到手机，允许“安装未知来源应用”后直接安装。当前为 debug 签名，不能直接上架。

### 21.4 连接服务端

- 手机与 Mac 在同一局域网。
- App 内「我的 → 服务器设置」填写 `http://<Mac 局域网 IP>:8080/api/v1`。
- 默认值在 `android/app/build.gradle.kts` 的 `DEFAULT_API_BASE_URL`。
- 服务端默认监听 `0.0.0.0:8080`；注意 Mac 防火墙与局域网隔离。
- Debug 包允许 HTTP 明文，生产必须切换 HTTPS 并移除 `usesCleartextTraffic`。

当前 Android 端使用开发登录（默认 openId：`android-dev-user`），不是微信登录；模拟支付仅开发环境可用。

### 21.5 验证记录

已在 Android 35 模拟器完成安装、首页数据加载、开发登录、SKU 选择、加购、结算、下单、模拟支付、管理员发货与确认收货。真实 Android 手机尚未连接验证。

更详细的构建、安装、目录与限制说明见 `android/README.md`。
