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
| role | VARCHAR(32) | NOT NULL DEFAULT 'ADMIN' | 预留角色 |
| status | VARCHAR(20) | NOT NULL DEFAULT 'ACTIVE' | ACTIVE / DISABLED |
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
| payment_status | VARCHAR(20) | NOT NULL DEFAULT 'UNPAID' | UNPAID / PAID / REFUNDED |
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
| logistics_company | VARCHAR(64) | NULL | |
| logistics_no | VARCHAR(64) | NULL | |
| paid_at / shipped_at / completed_at / cancelled_at | DATETIME(3) | NULL | |
| cancel_reason | VARCHAR(255) | NULL | |
| created_at / updated_at | DATETIME(3) | NOT NULL | |

索引：`uk_order_no (order_no)`，`uk_order_user_request (user_id, client_request_no)`，
`idx_order_user_status_created (user_id, status, created_at)`，`idx_order_status_created (status, created_at)`。

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

## 3. 状态枚举

- 用户：`ACTIVE / DISABLED`
- 分类/轮播图/商品：`ENABLED / DISABLED`、`ON_SALE / OFF_SALE`
- SKU：`ENABLED / DISABLED`
- 订单：`PENDING_PAYMENT / PENDING_SHIPMENT / PENDING_RECEIPT / COMPLETED / CANCELLED`
- 支付：`UNPAID / PAID / REFUNDED`；支付记录：`PENDING / SUCCESS / FAILED / REFUNDED`
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
- 生产关闭 `spring.flyway.clean-disabled=false` 的风险配置，保持 `clean-disabled=true`。
- 索引新增使用 `CREATE INDEX` 的独立版本脚本，避免大表锁定时段不可控。

## 6. 待评估扩展表

优惠券、收藏、评价、售后、退款、库存流水、消息通知。首版不建表，避免空壳字段。
