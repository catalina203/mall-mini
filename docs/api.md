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

返回启用的轮播图、一级分类、推荐商品、新品上架与热销榜单。

```json
{
  "banners": [{ "id": 1, "title": "", "imageUrl": "", "linkType": "PRODUCT", "linkTarget": "1" }],
  "categories": [{ "id": 1, "name": "数码", "iconUrl": "" }],
  "recommendedProducts": [{ "id": 1, "name": "样例商品", "mainImage": "", "minPrice": "99.00" }],
  "newProducts": [],
  "hotProducts": []
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
| sort | `new`（默认）/ `sales` / `price_asc` / `price_desc` |
| minPrice / maxPrice | 可选价格区间 |

仅返回 `ON_SALE` 且至少一个有效 SKU 的商品；返回 `minPrice`、`maxPrice`、`totalStock`、`salesCount`。关键词使用 ngram 全文索引 + LIKE 兜底，第一页搜索会记录热词。

### 2.5.1 搜索热词

- `GET /api/v1/search/hot?limit=10`
- `POST /api/v1/search/keywords` `{ "keyword": "耳机" }`

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
{ "addressId": 1, "source": "BUY_NOW", "skuId": 1, "quantity": 2, "couponId": null }
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
  "remark": "",
  "invoiceRequired": false,
  "invoiceTitle": "",
  "invoiceTaxNo": "",
  "invoiceEmail": "",
  "couponId": null
}
```

- 服务端重算价格、运费与优惠券，条件扣减库存，写订单/明细/状态日志，删除对应购物车项。
- 使用优惠券时服务端校验归属、有效期、门槛、范围，并在订单创建成功后条件锁定；取消/超时取消释放。
- 全部在同一事务；任一 SKU 库存不足整单失败。
- 相同 `clientRequestNo` 重复提交：顺序重复直接返回已创建订单（HTTP 200）；并发竞态由唯一索引拦截并返回 409 `CONFLICT`，不会重复扣库存或创建订单。
- 响应：`{ orderNo, status, payableAmount, createdAt, ... }`（完整订单详情结构）。

### 6.3 订单列表 / 详情

- `GET /api/v1/me/orders?status=&page=&pageSize=`
- `GET /api/v1/me/orders/{orderNo}`

详情含明细快照、地址快照、金额、状态时间、物流信息、发票信息、状态日志与售后状态。用户响应中的 `adminRemark` 始终为空。

### 6.4 取消订单

`POST /api/v1/me/orders/{orderNo}/cancel`

```json
{ "reason": "不想要了" }
```

请求体可省略，默认原因为“用户取消”。仅 `PENDING_PAYMENT` 且未超过取消时限；事务内条件更新订单状态并幂等回补库存。重复取消已取消订单返回当前订单，不重复回补。

### 6.5 确认收货

`POST /api/v1/me/orders/{orderNo}/confirm-receipt`

仅 `PENDING_RECEIPT` 可操作。

### 6.6 修改收货地址

`POST /api/v1/me/orders/{orderNo}/address`

```json
{ "addressId": 2 }
```

仅 `PENDING_SHIPMENT`、已支付且无进行中售后的订单可修改；更新订单地址快照并写状态日志。已发货订单不能直接改地址。

### 6.7 模拟支付（仅 dev）

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

### 6.8 售后（需 USER）

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/api/v1/me/orders/{orderNo}/after-sales` | 发起售后 |
| GET | `/api/v1/me/after-sales?status=&page=&pageSize=` | 我的售后列表 |
| GET | `/api/v1/me/after-sales/{afterSaleNo}` | 售后详情 |
| POST | `/api/v1/me/after-sales/{afterSaleNo}/cancel` | 取消售后（仅待审核/待退款/待退货） |
| POST | `/api/v1/me/after-sales/{afterSaleNo}/return-shipment` | 填写退货物流（仅待退货） |

发起售后请求：

```json
{
  "type": "REFUND_ONLY",
  "reason": "不想要了",
  "description": "可选",
  "items": [{ "orderItemId": 18, "quantity": 1 }]
}
```

规则：

- 订单必须已支付，且状态为待发货/待收货/已完成；
- 未发货订单只能 `REFUND_ONLY`；
- 同一订单只允许一个进行中的售后；
- 退款金额由服务端按订单明细计算；整单退款按实付金额（含运费），部分退款按明细成交金额，运费不退；
- 客户端不能提交退款金额或目标状态。

### 6.9 优惠券（需 USER）

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/v1/me/coupons?status=` | 我的优惠券 |
| GET | `/api/v1/me/coupons/claimable` | 可领取模板 |
| POST | `/api/v1/me/coupons/{templateId}/claim` | 领取 |

首版每人每模板限领 1 张；领取使用模板计数条件更新，避免超发。结算预览/创建订单传 `couponId`，折扣由服务端计算。

### 6.10 商品评价（公开 + USER）

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/api/v1/me/orders/{orderNo}/reviews` | 评价订单明细 |
| GET | `/api/v1/products/{productId}/reviews?page=&pageSize=` | 公开评价（仅审核通过） |
| GET | `/api/v1/products/{productId}/reviews/summary` | 平均分与数量 |

评价请求：`{ orderItemId, rating(1-5), content, anonymous }`。仅已完成订单、且每个订单明细只能评价一次。

### 6.11 收藏与浏览足迹（需 USER）

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST/DELETE | `/api/v1/me/favorites/{productId}` | 收藏/取消收藏 |
| GET | `/api/v1/me/favorites/{productId}/status` | 是否已收藏 |
| GET | `/api/v1/me/favorites?page=&pageSize=` | 收藏列表 |
| POST | `/api/v1/me/browse-history/{productId}` | 记录浏览 |
| GET | `/api/v1/me/browse-history?page=&pageSize=` | 浏览足迹 |
| DELETE | `/api/v1/me/browse-history` | 清空足迹 |

### 6.12 会员积分（需 USER）

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/v1/me/member` | 积分余额、累计积分、等级、下一等级 |
| GET | `/api/v1/me/points/logs?page=&pageSize=` | 积分明细 |

订单完成后按实付金额 1 元 = 1 积分；整单退款扣回对应积分。会员等级：银卡 100、金卡 500、钻石 2000。

### 6.13 客服工单（需 USER）

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/api/v1/me/support-tickets` | 提交工单 |
| GET | `/api/v1/me/support-tickets` | 我的工单 |
| GET | `/api/v1/me/support-tickets/{ticketNo}` | 详情 |
| POST | `/api/v1/me/support-tickets/{ticketNo}/close` | 关闭 |

提交字段：`orderNo? / type(ORDER|PRODUCT|REFUND|OTHER) / subject / content`。

### 6.14 站内通知（需 USER）

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/v1/me/notifications?page=&pageSize=` | 我的通知 |
| GET | `/api/v1/me/notifications/unread-count` | 未读数 |
| POST | `/api/v1/me/notifications/{id}/read` | 标记已读 |

---

## 7. 管理后台接口（需 ADMIN）

所有 `/api/v1/admin/**` 接口（除 `/admin/auth/login`）都需要 `role=ADMIN` 的 JWT。普通用户令牌返回 403。

### 7.0 角色与权限

| 角色 | 能力 |
| --- | --- |
| `SUPER_ADMIN` | 全部权限，包括管理员管理、操作日志、CSV 导出 |
| `OPERATOR` | 商品/分类/轮播图、订单、售后、库存、用户状态、导出；无管理员管理与审计 |
| `CUSTOMER_SERVICE` | 仪表盘、订单查看/备注、售后审核、用户查看、通知；无退款、库存调整、导出 |
| `WAREHOUSE` | 仪表盘、订单查看/发货、售后查看、库存查看/调整、通知 |

- 权限在后端通过 `@PreAuthorize` 校验，前端菜单隐藏只是体验层。
- 管理员改角色、禁用或重置密码会递增 `token_version`，旧 JWT 立即失效。
- 权限码：`DASHBOARD_READ / PRODUCT_READ / PRODUCT_WRITE / CATEGORY_READ / CATEGORY_WRITE / BANNER_READ / BANNER_WRITE / COUPON_READ / COUPON_WRITE / REVIEW_READ / REVIEW_WRITE / ORDER_READ / ORDER_WRITE / ORDER_SHIP / ORDER_CANCEL / AFTER_SALE_READ / AFTER_SALE_REVIEW / AFTER_SALE_REFUND / STOCK_READ / STOCK_ADJUST / USER_READ / USER_STATUS / NOTIFICATION_READ / SUPPORT_READ / SUPPORT_WRITE / ADMIN_MANAGE / AUDIT_READ / EXPORT_READ`。

### 7.1 管理员登录

`POST /api/v1/admin/auth/login`

```json
{ "username": "admin", "password": "..." }
```

响应：`{ token, admin: { id, username, displayName, role, permissions } }`。

`POST /api/v1/admin/auth/password`：修改自己的密码，成功后旧 JWT 失效。
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

### 7.10 售后管理

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/v1/admin/after-sales?status=&afterSaleNo=&orderNo=&page=&pageSize=` | 分页 |
| GET | `/api/v1/admin/after-sales/{afterSaleNo}` | 详情（商品、日志、退款记录） |
| POST | `/api/v1/admin/after-sales/{afterSaleNo}/approve` | 审核通过 |
| POST | `/api/v1/admin/after-sales/{afterSaleNo}/reject` | `{ reason }` |
| POST | `/api/v1/admin/after-sales/{afterSaleNo}/confirm-return` | 确认收到退货 |
| POST | `/api/v1/admin/after-sales/{afterSaleNo}/refund` | `{ manualRefundNo?, remark? }` |

退款规则：

- 仅退款需先审核通过；退货退款需先确认收到退货；
- 退款失败状态可重试；
- 开发环境使用 `MOCK` 退款；生产未接微信退款时，必须填写线下退款凭证（`MANUAL`）；
- 退款成功后按整单/部分更新订单 `payment_status`，退货退款才回补库存；
- 退款失败不会假装成功，详情中保留失败原因。

### 7.11 库存中心

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/v1/admin/stock/logs?skuId=&bizNo=&changeType=&page=&pageSize=` | 库存流水 |
| GET | `/api/v1/admin/stock/alerts?threshold=` | 低库存 SKU |
| POST | `/api/v1/admin/stock/skus/{skuId}/adjust` | `{ stock, remark? }` |

订单扣减、取消回补、售后退货回补、商品初始库存与手动调整都会写 `stock_log`；回补按 `(change_type, biz_no, sku_id)` 幂等。

### 7.12 通知记录

`GET /api/v1/admin/notification-logs?status=&templateCode=&page=&pageSize=`

当前只投递站内通知（`IN_APP`）；微信订阅消息与短信渠道未接入，生产启用前需补充模板配置与发送适配器。

### 7.13 管理员管理（仅 SUPER_ADMIN）

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/v1/admin/admins?keyword=&status=&page=&pageSize=` | 管理员列表 |
| GET | `/api/v1/admin/admins/roles` | 角色码列表 |
| POST | `/api/v1/admin/admins` | 新增管理员 |
| PUT | `/api/v1/admin/admins/{id}` | 修改显示名称/角色 |
| PATCH | `/api/v1/admin/admins/{id}/status` | 启用/禁用 |
| POST | `/api/v1/admin/admins/{id}/reset-password` | 重置密码 |

规则：不能禁用自己或修改自己的角色；必须至少保留一个启用的超级管理员。

### 7.14 操作日志与导出

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/v1/admin/operation-logs?adminId=&action=&targetType=&page=&pageSize=` | 操作日志 |
| GET | `/api/v1/admin/exports/orders?status=&orderNo=&userId=` | 订单 CSV |
| GET | `/api/v1/admin/exports/users?keyword=&status=` | 用户 CSV（手机号脱敏） |
| GET | `/api/v1/admin/exports/stock-logs?skuId=&bizNo=&changeType=` | 库存流水 CSV |

导出需要 `EXPORT_READ`，单次最多 10000 行，写管理员操作日志。

### 7.15 优惠券、运费模板与评价

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET/POST/PUT/DELETE | `/api/v1/admin/coupons` | 优惠券模板管理 |
| PATCH | `/api/v1/admin/coupons/{id}/status` | 启停 |
| GET/POST/PUT/DELETE | `/api/v1/admin/shipping-templates` | 运费模板与区域规则 |
| GET | `/api/v1/admin/shipping-templates/options` | 商品绑定时下拉 |
| GET | `/api/v1/admin/reviews?status=&productId=` | 评价列表 |
| POST | `/api/v1/admin/reviews/{id}/approve|reject|reply` | 审核/回复 |

商品保存请求新增可选 `shippingTemplateId`。

### 7.16 客服工单与用户积分

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/v1/admin/support-tickets?status=&type=&keyword=` | 工单列表 |
| POST | `/api/v1/admin/support-tickets/{ticketNo}/reply` | 回复 |
| POST | `/api/v1/admin/support-tickets/{ticketNo}/close` | 关闭 |
| POST | `/api/v1/admin/users/{id}/points` | `{ delta, remark }` 调整积分 |

权限：`SUPPORT_READ / SUPPORT_WRITE`；积分调整使用 `USER_STATUS`。

### 7.17 订单备注与仪表盘

- `PATCH /api/v1/admin/orders/{orderNo}/remark`：`{ remark }`，仅后台可见，需 `ORDER_WRITE`。
- `GET /api/v1/admin/dashboard/summary?days=`：返回支付金额、退款金额、净收入、客单价、退款率、待发货/待收货/售后待处理/低库存、支付趋势。

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
