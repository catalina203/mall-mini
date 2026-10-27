# 数据库设计

## 1. 通用约定

- 数据库：`mall_mini`，MySQL 8.0+，字符集 `utf8mb4`，排序规则 `utf8mb4_0900_ai_ci`，引擎 InnoDB。
- 主键：`BIGINT UNSIGNED AUTO_INCREMENT`。
- 金额：`DECIMAL(10,2)`，Java `BigDecimal`，禁止浮点。
- 时间：`DATETIME(3)`，应用与 JVM 时区统一 `Asia/Shanghai`，由应用写入或数据库默认值；文档中的“时间”均指该时区。
- 状态：使用 `VARCHAR(20/32)` 存枚举名，Java 侧枚举映射，不用难以阅读的魔法数字。
- 逻辑删除：仅订单等审计数据不物理删除；商品被订单引用后禁止物理删除（由 Service 校验）。
- **外键策略**：不建立数据库物理外键，依赖 Service 校验 + 唯一索引 + 普通索引保证一致性。原因：便于分库/数据迁移与演示数据初始化；所有跨表校验在业务层显式实现。
- 迁移：Flyway 版本化脚本，禁止手工改生产库结构。
  - `src/main/resources/db/migration/V1__init_schema.sql` 建表。
  - `src/main/resources/db/demo/demo-data.sql` 演示数据，仅 `dev` profile 下由 `DemoDataInitializer` 在 `category` 表为空时执行，不写入 Flyway 历史，避免污染生产迁移版本。
  - 演示数据仅用于开发测试，生产 profile 不加载。

## 2. 表清单

| # | 表 | 用途 |
| --- | --- | --- |
| 1 | `mall_user` | 小程序用户 |
| 2 | `admin_user` | 管理员 |
| 3 | `category` | 商品分类 |
| 4 | `product` | 商品 SPU |
| 5 | `product_sku` | 商品 SKU（价格/库存） |
| 6 | `product_image` | 商品图片 |
| 7 | `cart_item` | 购物车项 |
| 8 | `user_address` | 收货地址 |
| 9 | `mall_order` | 订单主表 |
| 10 | `mall_order_item` | 订单明细（快照） |
| 11 | `order_status_log` | 订单状态日志 |
| 12 | `payment_record` | 支付记录 |
| 13 | `banner` | 轮播图 |
| 14 | `admin_operation_log` | 管理员操作审计 |
| 15 | `after_sale_order` | 售后单 |
| 16 | `after_sale_item` | 售后明细 |
| 17 | `after_sale_log` | 售后状态日志 |
| 18 | `refund_record` | 退款记录 |
| 19 | `stock_log` | 库存流水 |
| 20 | `notification_log` | 站内通知记录 |
| 21 | `shipping_template` | 运费模板 |
| 22 | `shipping_template_rule` | 运费区域规则 |
| 23 | `coupon_template` | 优惠券模板 |
| 24 | `coupon` | 用户优惠券 |
| 25 | `product_review` | 商品评价 |
| 26 | `favorite` | 商品收藏 |
| 27 | `browse_history` | 浏览足迹 |
| 28 | `points_log` | 积分流水 |
| 29 | `search_keyword` | 搜索热词 |
| 30 | `support_ticket` | 客服工单 |

---

### 2.1 `mall_user`

| 字段 | 类型 | 约束 | 说明 |
| --- | --- | --- | --- |
| id | BIGINT UNSIGNED | PK, AUTO_INCREMENT | |
| open_id | VARCHAR(64) | NOT NULL, UNIQUE | 微信 openid；开发登录亦使用 |
| union_id | VARCHAR(64) | NULL | 可空 |
| nickname | VARCHAR(64) | NOT NULL DEFAULT '' | |
| avatar_url | VARCHAR(512) | NOT NULL DEFAULT '' | |
| phone | VARCHAR(20) | NULL | 敏感，管理端脱敏展示 |
| status | VARCHAR(20) | NOT NULL DEFAULT 'ACTIVE' | ACTIVE / DISABLED |
| points | INT | NOT NULL DEFAULT 0 | 当前积分余额 |
| total_points | INT | NOT NULL DEFAULT 0 | 累计获得积分，用于会员等级 |
| member_level | VARCHAR(20) | NOT NULL DEFAULT 'NORMAL' | NORMAL / SILVER / GOLD / PLATINUM |
| created_at | DATETIME(3) | NOT NULL | |
| updated_at | DATETIME(3) | NOT NULL | |

索引：`uk_mall_user_open_id (open_id)`，`idx_mall_user_status (status)`。

### 2.2 `admin_user`

| 字段 | 类型 | 约束 | 说明 |
| --- | --- | --- | --- |
| id | BIGINT UNSIGNED | PK | |
| username | VARCHAR(64) | NOT NULL, UNIQUE | |
| password_hash | VARCHAR(100) | NOT NULL | BCrypt |
| display_name | VARCHAR(64) | NOT NULL DEFAULT '' | |
| role | VARCHAR(32) | NOT NULL DEFAULT 'SUPER_ADMIN' | `SUPER_ADMIN / OPERATOR / CUSTOMER_SERVICE / WAREHOUSE` |
| status | VARCHAR(20) | NOT NULL DEFAULT 'ACTIVE' | ACTIVE / DISABLED |
| token_version | INT | NOT NULL DEFAULT 0 | 改密/禁用/改角色时递增，使旧 JWT 失效 |
| last_login_at | DATETIME(3) | NULL | |
| created_at / updated_at | DATETIME(3) | NOT NULL | |

索引：`uk_admin_user_username (username)`。

> 管理员不通过注册接口创建。启动时若库中无管理员且提供了 `APP_ADMIN_INIT_USERNAME` / `APP_ADMIN_INIT_PASSWORD`，则用 BCrypt 创建并打印提示；否则不创建。仓库不保存任何固定密码。

### 2.3 `category`

| 字段 | 类型 | 约束 | 说明 |
| --- | --- | --- | --- |
| id | BIGINT UNSIGNED | PK | |
| parent_id | BIGINT UNSIGNED | NOT NULL DEFAULT 0 | 0 表示一级分类 |
| name | VARCHAR(64) | NOT NULL | |
| icon_url | VARCHAR(512) | NOT NULL DEFAULT '' | 首页分类入口 |
| sort_order | INT | NOT NULL DEFAULT 0 | 升序 |
| status | VARCHAR(20) | NOT NULL DEFAULT 'ENABLED' | ENABLED / DISABLED |
| created_at / updated_at | DATETIME(3) | NOT NULL | |

索引：`idx_category_parent_sort (parent_id, sort_order)`，`uk_category_parent_name (parent_id, name)`。

规则：父级必须存在且为一级（首版最多两级）；禁止把自身/后代设为父级；有子分类或关联商品时禁止删除。

### 2.4 `product`

| 字段 | 类型 | 约束 | 说明 |
| --- | --- | --- | --- |
| id | BIGINT UNSIGNED | PK | |
| category_id | BIGINT UNSIGNED | NOT NULL | |
| name | VARCHAR(128) | NOT NULL | |
| subtitle | VARCHAR(255) | NOT NULL DEFAULT '' | |
| description | TEXT | NULL | 富文本/纯文本 |
| main_image | VARCHAR(512) | NOT NULL DEFAULT '' | |
| status | VARCHAR(20) | NOT NULL DEFAULT 'OFF_SALE' | ON_SALE / OFF_SALE |
| sort_order | INT | NOT NULL DEFAULT 0 | |
| shipping_template_id | BIGINT UNSIGNED | NULL | 运费模板，空使用全局配置 |
| sales_count | INT | NOT NULL DEFAULT 0 | 累计销量（按已支付件数） |
| created_at / updated_at | DATETIME(3) | NOT NULL | |

索引：`idx_product_category_status (category_id, status)`，`idx_product_status_sort (status, sort_order)`，`idx_product_name (name)`（关键词前缀/全文检索首版用 LIKE，后续可替换）。

规则：至少一个有效 SKU 才能上架；被订单明细引用后禁止物理删除。

### 2.5 `product_sku`

| 字段 | 类型 | 约束 | 说明 |
| --- | --- | --- | --- |
| id | BIGINT UNSIGNED | PK | |
| product_id | BIGINT UNSIGNED | NOT NULL | |
| sku_code | VARCHAR(64) | NOT NULL, UNIQUE | 业务编码 |
| spec_json | JSON | NOT NULL | 例 `{"颜色":"红色","尺码":"L"}` |
| price | DECIMAL(10,2) | NOT NULL | ≥ 0 |
| stock | INT | NOT NULL DEFAULT 0 | ≥ 0（CHECK） |
| status | VARCHAR(20) | NOT NULL DEFAULT 'ENABLED' | ENABLED / DISABLED |
| created_at / updated_at | DATETIME(3) | NOT NULL | |

索引：`uk_product_sku_code (sku_code)`，`idx_product_sku_product (product_id, status)`。
约束：`CHECK (stock >= 0)`，`CHECK (price >= 0)`。

### 2.6 `product_image`

| 字段 | 类型 | 约束 | 说明 |
| --- | --- | --- | --- |
| id | BIGINT UNSIGNED | PK | |
| product_id | BIGINT UNSIGNED | NOT NULL | |
| image_url | VARCHAR(512) | NOT NULL | |
| sort_order | INT | NOT NULL DEFAULT 0 | |
| created_at | DATETIME(3) | NOT NULL | |

索引：`idx_product_image_product_sort (product_id, sort_order)`。

### 2.7 `cart_item`

| 字段 | 类型 | 约束 | 说明 |
| --- | --- | --- | --- |
| id | BIGINT UNSIGNED | PK | |
| user_id | BIGINT UNSIGNED | NOT NULL | |
| sku_id | BIGINT UNSIGNED | NOT NULL | |
| quantity | INT | NOT NULL | > 0 |
| selected | TINYINT(1) | NOT NULL DEFAULT 1 | 结算勾选 |
| created_at / updated_at | DATETIME(3) | NOT NULL | |

索引：`uk_cart_user_sku (user_id, sku_id)`，`idx_cart_user (user_id)`。
约束：`CHECK (quantity > 0)`。重复加购走 `quantity = quantity + n`。

### 2.8 `user_address`

| 字段 | 类型 | 约束 | 说明 |
| --- | --- | --- | --- |
| id | BIGINT UNSIGNED | PK | |
| user_id | BIGINT UNSIGNED | NOT NULL | |
| receiver_name | VARCHAR(64) | NOT NULL | |
| phone | VARCHAR(20) | NOT NULL | 日志脱敏 |
| province | VARCHAR(64) | NOT NULL | |
| city | VARCHAR(64) | NOT NULL | |
| district | VARCHAR(64) | NOT NULL | |
| detail | VARCHAR(255) | NOT NULL | |
| is_default | TINYINT(1) | NOT NULL DEFAULT 0 | |
| created_at / updated_at | DATETIME(3) | NOT NULL | |

索引：`idx_address_user_default (user_id, is_default)`。

规则：同一用户至多一个默认地址；新增/设默认时事务内先清除旧默认；删除默认地址后不自动指定新默认（保持无默认，前端提示）。

### 2.9 `mall_order`

| 字段 | 类型 | 约束 | 说明 |
| --- | --- | --- | --- |
| id | BIGINT UNSIGNED | PK | |
| order_no | VARCHAR(32) | NOT NULL, UNIQUE | 服务端生成 |
| user_id | BIGINT UNSIGNED | NOT NULL | |
| status | VARCHAR(32) | NOT NULL | 订单状态 |
| payment_status | VARCHAR(20) | NOT NULL DEFAULT 'UNPAID' | UNPAID / PAID / PARTIALLY_REFUNDED / REFUNDED |
| goods_amount | DECIMAL(10,2) | NOT NULL | 商品金额 |
| discount_amount | DECIMAL(10,2) | NOT NULL DEFAULT 0.00 | 首版 0 |
| shipping_fee | DECIMAL(10,2) | NOT NULL DEFAULT 0.00 | 服务端计算 |
| payable_amount | DECIMAL(10,2) | NOT NULL | 应付 |
| receiver_name | VARCHAR(64) | NOT NULL | 地址快照 |
| receiver_phone | VARCHAR(20) | NOT NULL | 地址快照 |
| receiver_province | VARCHAR(64) | NOT NULL | |
| receiver_city | VARCHAR(64) | NOT NULL | |
| receiver_district | VARCHAR(64) | NOT NULL | |
| receiver_detail | VARCHAR(255) | NOT NULL | |
| client_request_no | VARCHAR(64) | NOT NULL | 幂等键 |
| remark | VARCHAR(255) | NOT NULL DEFAULT '' | 用户备注 |
| coupon_id | BIGINT UNSIGNED | NULL | 使用的优惠券 |
| coupon_name | VARCHAR(64) | NOT NULL DEFAULT '' | 优惠券名称快照 |
| admin_remark | VARCHAR(255) | NOT NULL DEFAULT '' | 仅后台可见的内部备注 |
| invoice_required | TINYINT(1) | NOT NULL DEFAULT 0 | 是否需要发票 |
| invoice_title | VARCHAR(128) | NOT NULL DEFAULT '' | 发票抬头 |
| invoice_tax_no | VARCHAR(64) | NOT NULL DEFAULT '' | 税号 |
| invoice_email | VARCHAR(128) | NOT NULL DEFAULT '' | 电子发票邮箱 |
| logistics_company | VARCHAR(64) | NULL | |
| logistics_no | VARCHAR(64) | NULL | |
| paid_at / shipped_at / completed_at / cancelled_at | DATETIME(3) | NULL | |
| cancel_reason | VARCHAR(255) | NULL | |
| created_at / updated_at | DATETIME(3) | NOT NULL | |

索引：`uk_order_no (order_no)`，`uk_order_user_request (user_id, client_request_no)`，
`idx_order_user_status_created (user_id, status, created_at)`，`idx_order_status_created (status, created_at)`，
`idx_order_status_shipped (status, shipped_at)`，`idx_order_paid_at (paid_at)`。

### 2.10 `mall_order_item`

| 字段 | 类型 | 约束 | 说明 |
| --- | --- | --- | --- |
| id | BIGINT UNSIGNED | PK | |
| order_id | BIGINT UNSIGNED | NOT NULL | |
| order_no | VARCHAR(32) | NOT NULL | 冗余便于查询 |
| product_id | BIGINT UNSIGNED | NOT NULL | |
| sku_id | BIGINT UNSIGNED | NOT NULL | |
| product_name | VARCHAR(128) | NOT NULL | 快照 |
| sku_spec | VARCHAR(255) | NOT NULL | 快照（JSON 字符串或可读文本） |
| product_image | VARCHAR(512) | NOT NULL DEFAULT '' | 快照 |
| unit_price | DECIMAL(10,2) | NOT NULL | 成交单价快照 |
| quantity | INT | NOT NULL | > 0 |
| subtotal | DECIMAL(10,2) | NOT NULL | unit_price × quantity |
| created_at | DATETIME(3) | NOT NULL | |

索引：`idx_order_item_order (order_id)`，`idx_order_item_product (product_id)`。

### 2.11 `order_status_log`

| 字段 | 类型 | 约束 | 说明 |
| --- | --- | --- | --- |
| id | BIGINT UNSIGNED | PK | |
| order_id | BIGINT UNSIGNED | NOT NULL | |
| from_status | VARCHAR(32) | NULL | 创建时为空 |
| to_status | VARCHAR(32) | NOT NULL | |
| operator_type | VARCHAR(20) | NOT NULL | USER / ADMIN / SYSTEM |
| operator_id | BIGINT UNSIGNED | NULL | |
| remark | VARCHAR(255) | NOT NULL DEFAULT '' | |
| created_at | DATETIME(3) | NOT NULL | |

索引：`idx_status_log_order_created (order_id, created_at)`。

### 2.12 `payment_record`

| 字段 | 类型 | 约束 | 说明 |
| --- | --- | --- | --- |
| id | BIGINT UNSIGNED | PK | |
| payment_no | VARCHAR(32) | NOT NULL, UNIQUE | 支付单号 |
| order_id | BIGINT UNSIGNED | NOT NULL | |
| order_no | VARCHAR(32) | NOT NULL | |
| channel | VARCHAR(20) | NOT NULL | MOCK / WECHAT |
| amount | DECIMAL(10,2) | NOT NULL | |
| status | VARCHAR(20) | NOT NULL | PENDING / SUCCESS / FAILED / REFUNDED |
| transaction_no | VARCHAR(64) | NULL | 渠道交易号 |
| paid_at | DATETIME(3) | NULL | |
| created_at / updated_at | DATETIME(3) | NOT NULL | |

索引：`uk_payment_no (payment_no)`，`idx_payment_order (order_no)`。

### 2.13 `banner`

| 字段 | 类型 | 约束 | 说明 |
| --- | --- | --- | --- |
| id | BIGINT UNSIGNED | PK | |
| title | VARCHAR(64) | NOT NULL DEFAULT '' | |
| image_url | VARCHAR(512) | NOT NULL | |
| link_type | VARCHAR(20) | NOT NULL DEFAULT 'NONE' | NONE / PRODUCT / CATEGORY |
| link_target | VARCHAR(64) | NOT NULL DEFAULT '' | 商品 ID 或分类 ID |
| sort_order | INT | NOT NULL DEFAULT 0 | |
| status | VARCHAR(20) | NOT NULL DEFAULT 'ENABLED' | ENABLED / DISABLED |
| created_at / updated_at | DATETIME(3) | NOT NULL | |

索引：`idx_banner_status_sort (status, sort_order)`。

规则：`link_type != NONE` 时 `link_target` 必填且目标存在；用户端只返回 ENABLED。

### 2.14 `admin_operation_log`

| 字段 | 类型 | 约束 | 说明 |
| --- | --- | --- | --- |
| id | BIGINT UNSIGNED | PK | |
| admin_id | BIGINT UNSIGNED | NOT NULL | |
| action | VARCHAR(64) | NOT NULL | 如 `PRODUCT_UPDATE` |
| target_type | VARCHAR(32) | NOT NULL DEFAULT '' | |
| target_id | VARCHAR(64) | NOT NULL DEFAULT '' | |
| detail | VARCHAR(512) | NOT NULL DEFAULT '' | 不含敏感信息 |
| request_id | VARCHAR(64) | NOT NULL DEFAULT '' | |
| created_at | DATETIME(3) | NOT NULL | |

索引：`idx_admin_log_admin_created (admin_id, created_at)`。


### 2.15 `after_sale_order`

| 字段 | 类型 | 约束 | 说明 |
| --- | --- | --- | --- |
| id | BIGINT UNSIGNED | PK | |
| after_sale_no | VARCHAR(32) | NOT NULL, UNIQUE | 售后单号，`AS` + 雪花 ID |
| order_id / order_no | BIGINT UNSIGNED / VARCHAR(32) | NOT NULL | 关联订单 |
| user_id | BIGINT UNSIGNED | NOT NULL | 申请人 |
| type | VARCHAR(20) | NOT NULL | `REFUND_ONLY / RETURN_REFUND` |
| status | VARCHAR(32) | NOT NULL | 见下方状态枚举 |
| reason / description | VARCHAR(255) / VARCHAR(1000) | NOT NULL | 申请原因与补充说明 |
| refund_amount | DECIMAL(10,2) | NOT NULL, ≥0 | 服务端计算，不信任客户端 |
| return_company / return_tracking_no | VARCHAR(64) | NULL | 退货物流 |
| review_remark / reviewed_by / reviewed_at | | NULL | 审核信息 |
| returned_at / refunded_at / closed_at | DATETIME(3) | NULL | 关键时间 |
| created_at / updated_at | DATETIME(3) | NOT NULL | |

索引：`uk_after_sale_no`、`idx_after_sale_user_created`、`idx_after_sale_order`、`idx_after_sale_status_created`。

### 2.16 `after_sale_item`

售后商品快照与退款金额。字段包含 `after_sale_id`、`order_item_id`、`product_id`、`sku_id`、商品名/规格/图片、`unit_price`、`quantity`、`refund_amount`。
唯一索引 `uk_after_sale_item (after_sale_id, order_item_id)`，防止同一明细重复。

### 2.17 `after_sale_log`

售后状态变更日志：`after_sale_id`、`from_status`、`to_status`、`operator_type`、`operator_id`、`remark`、`created_at`。

### 2.18 `refund_record`

| 字段 | 类型 | 约束 | 说明 |
| --- | --- | --- | --- |
| id | BIGINT UNSIGNED | PK | |
| refund_no | VARCHAR(32) | NOT NULL, UNIQUE | 退款单号，`RF` + 雪花 ID |
| after_sale_id | BIGINT UNSIGNED | NOT NULL, UNIQUE | 一个售后单对应一条退款记录 |
| order_id / order_no | | NOT NULL | 关联订单 |
| channel | VARCHAR(20) | NOT NULL | `MOCK / WECHAT / MANUAL` |
| amount | DECIMAL(10,2) | NOT NULL, ≥0 | 退款金额 |
| status | VARCHAR(20) | NOT NULL | `PENDING / SUCCESS / FAILED` |
| transaction_no | VARCHAR(64) | NULL | 渠道退款单号或线下凭证号 |
| requested_at / success_at | DATETIME(3) | | 发起/成功时间 |
| failed_reason | VARCHAR(255) | NULL | 失败原因 |

### 2.19 `stock_log`

| 字段 | 类型 | 约束 | 说明 |
| --- | --- | --- | --- |
| id | BIGINT UNSIGNED | PK | |
| sku_id / product_id | BIGINT UNSIGNED | NOT NULL | 关联 SKU/商品 |
| change_type | VARCHAR(32) | NOT NULL | 见库存变动类型 |
| quantity_delta | INT | NOT NULL | 正数入库，负数出库 |
| stock_before / stock_after | INT | NOT NULL, ≥0 | 变动前后库存 |
| biz_type / biz_no | VARCHAR(32) / VARCHAR(64) | NOT NULL | 业务类型与单号 |
| operator_type / operator_id | | | USER / ADMIN / SYSTEM |
| remark | VARCHAR(255) | NOT NULL DEFAULT '' | |
| created_at | DATETIME(3) | NOT NULL | |

唯一索引 `uk_stock_log_biz (change_type, biz_no, sku_id)`，用于回补幂等兜底；普通索引覆盖 SKU 时间线与业务单号查询。

### 2.20 `notification_log`

站内通知记录：`channel`（`IN_APP / MINIPROGRAM / SMS`）、`recipient_type`（`USER / ADMIN`）、`recipient_id`、`template_code`、`title`、`content`、`biz_type`、`biz_no`、`payload_json`、`status`（`PENDING / SENT / FAILED / SKIPPED`）、`retry_count`、`read_at`、`sent_at`、`error_message`、时间字段。

> 当前实现只写入 `IN_APP` 并标记 `SENT`，用户可通过 `/api/v1/me/notifications` 读取；微信订阅消息与短信渠道尚未接入。


### 2.21 `shipping_template` / `shipping_template_rule`

- `shipping_template`：`name`、`default_fee`、`free_threshold`、`status`。
- `shipping_template_rule`：`template_id`、`region_code`（空=全国）、`first_unit/first_fee`、`additional_unit/additional_fee`、`free_threshold`、`status`、`sort_order`。
- 商品通过 `product.shipping_template_id` 绑定；未绑定使用全局 `APP_SHIPPING_FEE` / `APP_SHIPPING_FREE_THRESHOLD`。
- V4 初始化默认模板（id=1）并把历史商品绑定到该模板。

### 2.22 `coupon_template` / `coupon`

- `coupon_template`：`name`、`type(FIXED/PERCENT)`、`value`、`min_amount`、`scope(ALL/CATEGORY/PRODUCT)`、`scope_id`、`total_count`、`claimed_count`、`per_user_limit`、有效期、状态。
- `coupon`：`coupon_no`、`template_id`、`user_id`、`status(UNUSED/USED/EXPIRED)`、`order_no`、`used_at`、有效期。
- 首版每人每模板限领 1 张（唯一索引 `uk_coupon_template_user`）；`FIXED` 为减免金额，`PERCENT` 为减免百分比。

### 2.23 `product_review`

`order_no`、`order_item_id`（唯一，一单一明细一条评价）、`product_id`、`sku_id`、`user_id`、`rating(1-5)`、`content`、`anonymous`、`status(PENDING/APPROVED/REJECTED)`、`reply`、时间字段。公开列表只返回 `APPROVED`。

### 2.24 `favorite` / `browse_history`

- `favorite`：`user_id + product_id` 唯一，记录收藏时间。
- `browse_history`：`user_id + product_id` 唯一，重复浏览更新 `viewed_at`；可清空。


### 2.28 `points_log`

积分流水：`user_id`、`change_type(ORDER_REWARD/ORDER_REFUND_REVOKE/ADMIN_ADJUST)`、`points_delta`、`balance_after`、`biz_type/biz_no`、`remark`、`created_at`。
唯一索引 `uk_points_biz (change_type, biz_no, user_id)` 保证订单奖励/退款扣回幂等。

### 2.29 `search_keyword`

搜索热词：`keyword` 唯一、`search_count`、`last_searched_at`。商品搜索第一页会累加计数，用于热门搜索与运营观察。

### 2.30 `support_ticket`

客服工单：`ticket_no` 唯一、`user_id`、`order_no`、`type(ORDER/PRODUCT/REFUND/OTHER)`、`subject`、`content`、`status(OPEN/PROCESSING/RESOLVED/CLOSED)`、`reply`、`replied_by/replied_at`、`closed_at`、时间字段。

## 3. 状态枚举

- 用户：`ACTIVE / DISABLED`
- 管理员角色：`SUPER_ADMIN / OPERATOR / CUSTOMER_SERVICE / WAREHOUSE`；状态：`ACTIVE / DISABLED`
- 分类/轮播图/商品：`ENABLED / DISABLED`、`ON_SALE / OFF_SALE`
- SKU：`ENABLED / DISABLED`
- 订单：`PENDING_PAYMENT / PENDING_SHIPMENT / PENDING_RECEIPT / COMPLETED / CANCELLED`
- 支付：`UNPAID / PAID / PARTIALLY_REFUNDED / REFUNDED`；支付记录：`PENDING / SUCCESS / FAILED / REFUNDED`
- 售后类型：`REFUND_ONLY / RETURN_REFUND`
- 售后状态：`APPLIED / APPROVED / WAIT_RETURN / RETURNED / RETURN_RECEIVED / REFUNDING / REFUNDED / REFUND_FAILED / REJECTED / CANCELLED`
- 退款记录：`PENDING / SUCCESS / FAILED`；退款渠道：`MOCK / WECHAT / MANUAL`
- 库存变动：`ORDER_DEDUCT / ORDER_CANCEL_RESTORE / AFTER_SALE_RESTORE / MANUAL_ADJUST / PRODUCT_INIT / STOCKTAKE`
- 通知：`IN_APP / MINIPROGRAM / SMS`，状态 `PENDING / SENT / FAILED / SKIPPED`
- 优惠券：类型 `FIXED / PERCENT`，范围 `ALL / CATEGORY / PRODUCT`，状态 `UNUSED / USED / EXPIRED`；模板状态 `ENABLED / DISABLED`
- 评价：`PENDING / APPROVED / REJECTED`
- 会员等级：`NORMAL / SILVER / GOLD / PLATINUM`；积分变动：`ORDER_REWARD / ORDER_REFUND_REVOKE / ADMIN_ADJUST`
- 客服工单：`OPEN / PROCESSING / RESOLVED / CLOSED`
- 轮播跳转：`NONE / PRODUCT / CATEGORY`

## 4. 演示数据（dev）

执行方式：`dev` profile 启动时由 `DemoDataInitializer` 检查 `category` 表，为空则执行 `db/demo/demo-data.sql`，非空则跳过（幂等）。

- 6 个一级分类（数码、服饰、家居、美妆、食品、运动），5 个二级分类。
- 12 个在售样例商品 + 1 个下架样例商品，每个商品 1–3 个 SKU；价格与库存合理。
- 3 条轮播图，图片使用服务端本地占位 SVG（`/demo/banner.svg`）。
- 商品名称/描述中标注“样例”，不伪造销量、评价、折扣。
- 不自动创建订单、不自动创建用户、不内置管理员固定密码。

## 5. 迁移与升级策略

- `V{n}__description.sql` 只增不改；已发布脚本禁止修改。
- 结构变更必须同时提供回滚说明（人工）与向后兼容评估。
- 空库执行：`mvn -pl server flyway:migrate` 或应用启动自动迁移（dev）。
- `V2__after_sale_stock_notification.sql` 新增售后、退款、库存流水、通知表，可在已有 V1 库上增量执行。
- `V3__r2_admin_order.sql` 增加管理员 token 版本、角色迁移、订单发票/后台备注与 R2 查询索引，可在已有 V2 库上增量执行。
- `V4__r3_coupon_shipping_review_favorite.sql` 新增优惠券、运费模板、评价、收藏/足迹，并初始化默认运费模板，可在已有 V3 库上增量执行。
- `V5__r4_search_support_member_home.sql` 增加积分、搜索热词、客服工单、商品销量与全文索引（ngram），可在已有 V4 库上增量执行。
- 生产关闭 `spring.flyway.clean-disabled=false` 的风险配置，保持 `clean-disabled=true`。
- 索引新增使用 `CREATE INDEX` 的独立版本脚本，避免大表锁定时段不可控。

## 6. 待评估扩展表

优惠券、收藏、评价、会员积分、消息模板配置。售后、退款、库存流水、站内通知已在 V2 落地；微信订阅消息与短信发送渠道仍待接入。
