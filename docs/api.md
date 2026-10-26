# API 设计

## 1. 通用约定

- 基础路径：`/api/v1`
- 传输：JSON；文件上传为 `multipart/form-data`
- 认证：`Authorization: Bearer <JWT>`
  - `/api/v1/me/**` 要求 `role=USER`
  - `/api/v1/admin/**` 要求 `role=ADMIN`
  - 公开读接口无需令牌
- 时间为 `Asia/Shanghai` 的 ISO-8601 字符串，如 `2026-10-18T10:30:00+08:00`
- 金额为字符串或数字，单位元，保留两位小数；客户端展示值不参与服务端计算

### 1.1 统一响应

```json
{
  "code": "OK",
  "message": "OK",
  "data": {},
  "requestId": "0f7c2a3e9b6d4a1c"
}
```

### 1.2 分页响应

```json
{
  "code": "OK",
  "message": "OK",
  "data": {
    "items": [],
    "page": 1,
    "pageSize": 12,
    "total": 0
  },
  "requestId": "..."
}
```

### 1.3 错误码与 HTTP 状态

| code | HTTP | 含义 |
| --- | --- | --- |
| `VALIDATION_ERROR` | 400 | 参数校验失败 |
| `UNAUTHORIZED` | 401 | 未登录或令牌无效/过期 |
| `FORBIDDEN` | 403 | 无权限或资源不属于当前用户 |
| `NOT_FOUND` | 404 | 资源不存在 |
| `CONFLICT` | 409 | 重复提交、唯一约束冲突 |
| `STOCK_INSUFFICIENT` | 409 | 库存不足 |
| `ORDER_STATE_INVALID` | 409 | 非法订单状态转换 |
| `PRODUCT_UNAVAILABLE` | 409 | 商品下架/SKU 失效 |
| `ADDRESS_UNAVAILABLE` | 409 | 地址不存在或不属于当前用户 |
| `RATE_LIMITED` | 429 | 触发限流 |
| `PAYMENT_DISABLED` | 403 | 当前环境未启用该支付方式 |
| `INTERNAL_ERROR` | 500 | 服务端错误（不含堆栈） |

错误示例：

```json
{
  "code": "STOCK_INSUFFICIENT",
  "message": "商品库存不足：简约帆布鞋 / 42",
  "data": null,
  "requestId": "..."
}
```

### 1.4 分页参数

`page` 从 1 开始，默认 1；`pageSize` 默认 12，最大 100。

---

## 2. 公开接口

### 2.1 健康检查

`GET /api/v1/health`

```json
{ "status": "UP", "time": "..." }
```

### 2.2 首页聚合

`GET /api/v1/home`

返回启用的轮播图、一级分类、推荐/新品商品（首版取上架商品按排序与创建时间）。

```json
{
  "banners": [{ "id": 1, "title": "", "imageUrl": "", "linkType": "PRODUCT", "linkTarget": "1" }],
  "categories": [{ "id": 1, "name": "数码", "iconUrl": "" }],
  "recommendedProducts": [{ "id": 1, "name": "样例商品", "mainImage": "", "minPrice": "99.00" }]
}
```

### 2.3 轮播图

`GET /api/v1/banners` → 仅 `ENABLED`，按 `sort_order` 升序。

### 2.4 分类

- `GET /api/v1/categories`：返回启用分类树（首版两级）。
- `GET /api/v1/categories/{id}/products?page=&pageSize=`：分类下上架商品（含子分类）。

### 2.5 商品列表

`GET /api/v1/products`

| 参数 | 说明 |
| --- | --- |
| page / pageSize | 分页 |
| categoryId | 分类（含子分类） |
| keyword | 名称/副标题模糊搜索 |
| sort | `new`（默认）/ `price_asc` / `price_desc` |
| minPrice / maxPrice | 可选价格区间 |

仅返回 `ON_SALE` 且至少一个有效 SKU 的商品；返回 `minPrice`、`maxPrice`、`totalStock`。

### 2.6 商品详情

`GET /api/v1/products/{id}`

```json
{
  "id": 1,
  "name": "样例商品",
  "subtitle": "",
  "description": "",
  "mainImage": "",
  "images": [{ "id": 1, "url": "", "sortOrder": 0 }],
  "categoryId": 1,
  "status": "ON_SALE",
  "skus": [
    {
      "id": 1,
      "skuCode": "SKU-001",
      "spec": { "颜色": "红色", "尺码": "L" },
      "price": "99.00",
      "stock": 10,
      "status": "ENABLED"
    }
  ]
}
```

下架商品返回 404；SKU 的 `stock=0` 以状态体现，不隐藏（便于前端提示售罄）。

---

## 3. 认证

### 3.1 开发登录（仅 dev）

`POST /api/v1/auth/dev-login`

```json
{ "openId": "dev-user-1", "nickname": "开发用户" }
```

响应：

```json
{
  "token": "...",
  "devMode": true,
  "user": { "id": 1, "nickname": "开发用户", "avatarUrl": "" }
}
```

未开启开发登录时返回 `403 FORBIDDEN`，提示“开发登录未启用”。

### 3.2 微信登录

`POST /api/v1/auth/wechat-login`

```json
{ "code": "wx.login 返回的临时 code" }
```

服务端调用微信换取 openid。未配置 `WECHAT_APP_ID/SECRET` 时返回 `503`（`INTERNAL_ERROR` 或专用提示），**不伪造登录成功**。

### 3.3 当前用户

`GET /api/v1/me` → `{ id, nickname, avatarUrl, phone(masked), status }`

---

## 4. 购物车（需 USER）

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/v1/me/cart` | 列表 + 汇总（仅勾选且可购买项） |
| POST | `/api/v1/me/cart` | `{ skuId, quantity }`，重复则累加 |
| PUT | `/api/v1/me/cart/{id}` | `{ quantity?, selected? }` |
| PUT | `/api/v1/me/cart/selection` | `{ selected, ids? }`，`ids` 为空数组表示不操作，省略表示全部 |
| DELETE | `/api/v1/me/cart/{id}` | 删除 |
| DELETE | `/api/v1/me/cart` | 清空 |

写操作（新增/修改/勾选/删除/清空）成功后统一返回最新的购物车结构（`items` + `summary`），减少前端二次请求。

购物车项返回商品名、图片、规格、单价、库存、上下架状态、失效标记，前端据此提示。数量上限 99。

---

## 5. 收货地址（需 USER）

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/v1/me/addresses` | 列表，默认地址优先 |
| POST | `/api/v1/me/addresses` | 新增 |
| PUT | `/api/v1/me/addresses/{id}` | 编辑（校验归属） |
| DELETE | `/api/v1/me/addresses/{id}` | 删除 |
| PUT | `/api/v1/me/addresses/{id}/default` | 设为默认 |

请求体：

```json
{
  "receiverName": "张三",
  "phone": "13800138000",
  "province": "广东省",
  "city": "深圳市",
  "district": "南山区",
  "detail": "科技园路 1 号",
  "isDefault": true
}
```

---

## 6. 结算与订单（需 USER）

### 6.1 结算预览

`POST /api/v1/me/checkout/preview`

```json
{
  "addressId": 1,
  "source": "CART",
  "cartItemIds": [1, 2]
}
```

或立即购买：

```json
{ "addressId": 1, "source": "BUY_NOW", "skuId": 1, "quantity": 2 }
```

响应含商品明细、商品金额、运费、优惠（0）、应付金额、地址有效性警告、不可购买原因列表。**仅预览，不扣库存。**

### 6.2 创建订单

`POST /api/v1/me/orders`

```json
{
  "addressId": 1,
  "source": "CART",
  "cartItemIds": [1, 2],
  "clientRequestNo": "uuid-由客户端生成",
  "remark": ""
}
```

- 服务端重算价格与运费，条件扣减库存，写订单/明细/状态日志，删除对应购物车项。
- 全部在同一事务；任一 SKU 库存不足整单失败。
- 相同 `clientRequestNo` 重复提交：顺序重复直接返回已创建订单（HTTP 200）；并发竞态由唯一索引拦截并返回 409 `CONFLICT`，不会重复扣库存或创建订单。
- 响应：`{ orderNo, status, payableAmount, createdAt, ... }`（完整订单详情结构）。

### 6.3 订单列表 / 详情

- `GET /api/v1/me/orders?status=&page=&pageSize=`
- `GET /api/v1/me/orders/{orderNo}`

详情含明细快照、地址快照、金额、状态时间、物流信息、状态日志（可选返回）。

### 6.4 取消订单

`POST /api/v1/me/orders/{orderNo}/cancel`

```json
{ "reason": "不想要了" }
```

请求体可省略，默认原因为“用户取消”。仅 `PENDING_PAYMENT` 且未超过取消时限；事务内条件更新订单状态并幂等回补库存。重复取消已取消订单返回当前订单，不重复回补。

### 6.5 确认收货

`POST /api/v1/me/orders/{orderNo}/confirm-receipt`

仅 `PENDING_RECEIPT` 可操作。

### 6.6 模拟支付（仅 dev）

`GET /api/v1/payment/config`（公开）

```json
{ "mockEnabled": true, "channel": "MOCK" }
```

`POST /api/v1/me/orders/{orderNo}/pay`

- 仅当 `APP_PAYMENT_MOCK_ENABLED=true` 且非生产 profile。
- 仅 `PENDING_PAYMENT` / `UNPAID`；写 `payment_record(MOCK)`，条件更新订单为 `PENDING_SHIPMENT`、支付状态 `PAID`，写 `PAYMENT` 状态日志。
- 重复支付返回当前订单，不重复写支付记录。
- 响应：

```json
{
  "mock": true,
  "channel": "MOCK",
  "order": { "orderNo": "...", "status": "PENDING_SHIPMENT", "paymentStatus": "PAID" }
}
```

- 生产或未启用网关返回 `PAYMENT_DISABLED`；支付渠道失败返回 `PAYMENT_FAILED`。
- 真实微信支付与回调尚未实现，接入时必须验签、校验商户订单号/金额/交易状态并幂等处理。

---

## 7. 管理后台接口（需 ADMIN）

所有 `/api/v1/admin/**` 接口（除 `/admin/auth/login`）都需要 `role=ADMIN` 的 JWT。普通用户令牌返回 403。

### 7.1 管理员登录

`POST /api/v1/admin/auth/login`

```json
{ "username": "admin", "password": "..." }
```

响应：`{ token, admin: { id, username, displayName, role } }`。
登录失败统一返回“用户名或密码错误”，不区分账号是否存在。登录接口限流。

`GET /api/v1/admin/me`

### 7.2 仪表盘

`GET /api/v1/admin/dashboard/summary?days=30`

返回订单数、已支付销售额（口径：PAID 且未退款订单的 `payable_amount`）、待发货数、待收货数、今日订单数。明确 `range` 与 `generatedAt`；无数据返回 0，不伪造。

### 7.3 分类管理

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/v1/admin/categories?page=&pageSize=&status=&keyword=` | 分页 |
| GET | `/api/v1/admin/categories/options` | 全部分类选项（含停用） |
| POST | `/api/v1/admin/categories` | 新增 |
| PUT | `/api/v1/admin/categories/{id}` | 编辑 |
| PATCH | `/api/v1/admin/categories/{id}/status` | 启用/停用 |
| DELETE | `/api/v1/admin/categories/{id}` | 有商品/子分类时拒绝 |

### 7.4 商品与 SKU

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/v1/admin/products?page=&pageSize=&status=&categoryId=&keyword=` | 分页 |
| GET | `/api/v1/admin/products/{id}` | 详情（含 SKU/图片） |
| POST | `/api/v1/admin/products` | 新增含 SKU/图片 |
| PUT | `/api/v1/admin/products/{id}` | 编辑 |
| PATCH | `/api/v1/admin/products/{id}/status` | 上架/下架（上架校验有效 SKU） |
| DELETE | `/api/v1/admin/products/{id}` | 仅无订单引用时允许 |
| PATCH | `/api/v1/admin/skus/{skuId}/stock` | `{ stock }` 设置库存（≥0） |

新增商品请求示例：

```json
{
  "name": "样例商品",
  "subtitle": "",
  "description": "",
  "categoryId": 1,
  "mainImage": "https://...",
  "sortOrder": 0,
  "status": "OFF_SALE",
  "images": [{ "url": "https://...", "sortOrder": 0 }],
  "skus": [
    { "skuCode": "SKU-001", "spec": { "颜色": "红色" }, "price": "99.00", "stock": 10, "status": "ENABLED" }
  ]
}
```

### 7.5 订单管理

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/v1/admin/orders?orderNo=&userId=&status=&page=&pageSize=` | 分页 |
| GET | `/api/v1/admin/orders/{orderNo}` | 详情（含快照、日志） |
| POST | `/api/v1/admin/orders/{orderNo}/ship` | `{ logisticsCompany, logisticsNo }`，仅待发货 |
| POST | `/api/v1/admin/orders/{orderNo}/cancel` | 仅待付款，回补库存并记录原因 |

禁止提供“任意修改状态”的接口。

### 7.6 用户管理

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/v1/admin/users?keyword=&status=&page=&pageSize=` | 手机号脱敏 |
| PATCH | `/api/v1/admin/users/{id}/status` | `{ status }`，记录操作日志 |

### 7.7 轮播图管理

GET/POST/PUT/DELETE `/api/v1/admin/banners`，PATCH `/{id}/status`。
校验 `linkType` 与 `linkTarget` 匹配且目标存在。

### 7.8 图片上传

`POST /api/v1/admin/uploads`，字段 `file`。

- 限制 2MB，仅 `image/jpeg|png|webp`，校验扩展名与内容类型；
- 随机文件名，禁止路径穿越；
- 返回 `{ url }`（服务端绝对地址）。本地存储适配器写入 `APP_STORAGE_LOCAL_DIR`，由静态资源映射 `/uploads/**` 提供。

### 7.9 管理员操作日志

关键写操作（商品/分类/订单/用户/轮播图）记录 `admin_operation_log`，包含 requestId，不记录敏感内容。

---

## 8. 限流与审计

| 场景 | 策略 |
| --- | --- |
| 管理员登录 / 开发登录 | 单 IP 每分钟 10 次 |
| 创建订单 | 单用户每分钟 20 次 |
| 支付回调（未来） | 验签 + 幂等，不限流但记录 |

首版使用进程内固定窗口限流（单实例足够）；多实例部署时替换为 Redis 实现。

## 9. OpenAPI

- `/v3/api-docs`、`/swagger-ui.html` 在 dev/test 开放；生产通过配置关闭或加访问控制。
- 每个 Controller 标注 `@Tag`，DTO 标注校验注解与示例，文档随实现更新。

## 10. 兼容与版本

- 前缀 `/api/v1`；破坏性变更升版本。
- 新增可选字段视为兼容；枚举新增值前评估前端兼容。
- 未实现接口不发布空壳路由；真实微信支付回调在接入时补充。
