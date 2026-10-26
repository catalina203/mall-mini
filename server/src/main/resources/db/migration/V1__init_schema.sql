CREATE TABLE mall_user (
    id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    open_id       VARCHAR(64)     NOT NULL,
    union_id      VARCHAR(64)     NULL,
    nickname      VARCHAR(64)     NOT NULL DEFAULT '',
    avatar_url    VARCHAR(512)    NOT NULL DEFAULT '',
    phone         VARCHAR(20)     NULL,
    status        VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE',
    created_at    DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at    DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_mall_user_open_id (open_id),
    KEY idx_mall_user_status (status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '小程序用户';

CREATE TABLE admin_user (
    id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    username      VARCHAR(64)     NOT NULL,
    password_hash VARCHAR(100)    NOT NULL,
    display_name  VARCHAR(64)     NOT NULL DEFAULT '',
    role          VARCHAR(32)     NOT NULL DEFAULT 'ADMIN',
    status        VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE',
    last_login_at DATETIME(3)     NULL,
    created_at    DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at    DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_admin_user_username (username)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '管理员';

CREATE TABLE category (
    id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    parent_id     BIGINT UNSIGNED NOT NULL DEFAULT 0,
    name          VARCHAR(64)     NOT NULL,
    icon_url      VARCHAR(512)    NOT NULL DEFAULT '',
    sort_order    INT             NOT NULL DEFAULT 0,
    status        VARCHAR(20)     NOT NULL DEFAULT 'ENABLED',
    created_at    DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at    DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_category_parent_name (parent_id, name),
    KEY idx_category_parent_sort (parent_id, sort_order)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '商品分类';

CREATE TABLE product (
    id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    category_id   BIGINT UNSIGNED NOT NULL,
    name          VARCHAR(128)    NOT NULL,
    subtitle      VARCHAR(255)    NOT NULL DEFAULT '',
    description   TEXT            NULL,
    main_image    VARCHAR(512)    NOT NULL DEFAULT '',
    status        VARCHAR(20)     NOT NULL DEFAULT 'OFF_SALE',
    sort_order    INT             NOT NULL DEFAULT 0,
    created_at    DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at    DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_product_category_status (category_id, status),
    KEY idx_product_status_sort (status, sort_order),
    KEY idx_product_name (name)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '商品 SPU';

CREATE TABLE product_sku (
    id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    product_id    BIGINT UNSIGNED NOT NULL,
    sku_code      VARCHAR(64)     NOT NULL,
    spec_json     JSON            NOT NULL,
    price         DECIMAL(10, 2)  NOT NULL,
    stock         INT             NOT NULL DEFAULT 0,
    status        VARCHAR(20)     NOT NULL DEFAULT 'ENABLED',
    created_at    DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at    DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_product_sku_code (sku_code),
    KEY idx_product_sku_product (product_id, status),
    CONSTRAINT chk_product_sku_price CHECK (price >= 0),
    CONSTRAINT chk_product_sku_stock CHECK (stock >= 0)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '商品 SKU';

CREATE TABLE product_image (
    id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    product_id    BIGINT UNSIGNED NOT NULL,
    image_url     VARCHAR(512)    NOT NULL,
    sort_order    INT             NOT NULL DEFAULT 0,
    created_at    DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_product_image_product_sort (product_id, sort_order)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '商品图片';

CREATE TABLE cart_item (
    id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id       BIGINT UNSIGNED NOT NULL,
    sku_id        BIGINT UNSIGNED NOT NULL,
    quantity      INT             NOT NULL,
    selected      TINYINT(1)      NOT NULL DEFAULT 1,
    created_at    DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at    DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_cart_user_sku (user_id, sku_id),
    KEY idx_cart_user (user_id),
    CONSTRAINT chk_cart_item_quantity CHECK (quantity > 0)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '购物车项';

CREATE TABLE user_address (
    id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id       BIGINT UNSIGNED NOT NULL,
    receiver_name VARCHAR(64)     NOT NULL,
    phone         VARCHAR(20)     NOT NULL,
    province      VARCHAR(64)     NOT NULL,
    city          VARCHAR(64)     NOT NULL,
    district      VARCHAR(64)     NOT NULL,
    detail        VARCHAR(255)    NOT NULL,
    is_default    TINYINT(1)      NOT NULL DEFAULT 0,
    created_at    DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at    DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_address_user_default (user_id, is_default)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '收货地址';

CREATE TABLE mall_order (
    id                BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    order_no          VARCHAR(32)     NOT NULL,
    user_id           BIGINT UNSIGNED NOT NULL,
    status            VARCHAR(32)     NOT NULL,
    payment_status    VARCHAR(20)     NOT NULL DEFAULT 'UNPAID',
    goods_amount      DECIMAL(10, 2)  NOT NULL,
    discount_amount   DECIMAL(10, 2)  NOT NULL DEFAULT 0.00,
    shipping_fee      DECIMAL(10, 2)  NOT NULL DEFAULT 0.00,
    payable_amount    DECIMAL(10, 2)  NOT NULL,
    receiver_name     VARCHAR(64)     NOT NULL,
    receiver_phone    VARCHAR(20)     NOT NULL,
    receiver_province VARCHAR(64)     NOT NULL,
    receiver_city     VARCHAR(64)     NOT NULL,
    receiver_district VARCHAR(64)     NOT NULL,
    receiver_detail   VARCHAR(255)    NOT NULL,
    client_request_no VARCHAR(64)     NOT NULL,
    remark            VARCHAR(255)    NOT NULL DEFAULT '',
    logistics_company VARCHAR(64)     NULL,
    logistics_no      VARCHAR(64)     NULL,
    paid_at           DATETIME(3)     NULL,
    shipped_at        DATETIME(3)     NULL,
    completed_at      DATETIME(3)     NULL,
    cancelled_at      DATETIME(3)     NULL,
    cancel_reason     VARCHAR(255)    NULL,
    created_at        DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at        DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_order_no (order_no),
    UNIQUE KEY uk_order_user_request (user_id, client_request_no),
    KEY idx_order_user_status_created (user_id, status, created_at),
    KEY idx_order_status_created (status, created_at),
    CONSTRAINT chk_order_amounts CHECK (goods_amount >= 0 AND discount_amount >= 0 AND shipping_fee >= 0 AND payable_amount >= 0)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '订单主表';

CREATE TABLE mall_order_item (
    id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    order_id      BIGINT UNSIGNED NOT NULL,
    order_no      VARCHAR(32)     NOT NULL,
    product_id    BIGINT UNSIGNED NOT NULL,
    sku_id        BIGINT UNSIGNED NOT NULL,
    product_name  VARCHAR(128)    NOT NULL,
    sku_spec      VARCHAR(255)    NOT NULL,
    product_image VARCHAR(512)    NOT NULL DEFAULT '',
    unit_price    DECIMAL(10, 2)  NOT NULL,
    quantity      INT             NOT NULL,
    subtotal      DECIMAL(10, 2)  NOT NULL,
    created_at    DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_order_item_order (order_id),
    KEY idx_order_item_product (product_id),
    CONSTRAINT chk_order_item_quantity CHECK (quantity > 0),
    CONSTRAINT chk_order_item_amount CHECK (unit_price >= 0 AND subtotal >= 0)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '订单明细快照';

CREATE TABLE order_status_log (
    id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    order_id      BIGINT UNSIGNED NOT NULL,
    from_status   VARCHAR(32)     NULL,
    to_status     VARCHAR(32)     NOT NULL,
    operator_type VARCHAR(20)     NOT NULL,
    operator_id   BIGINT UNSIGNED NULL,
    remark        VARCHAR(255)    NOT NULL DEFAULT '',
    created_at    DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_status_log_order_created (order_id, created_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '订单状态日志';

CREATE TABLE payment_record (
    id             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    payment_no     VARCHAR(32)     NOT NULL,
    order_id       BIGINT UNSIGNED NOT NULL,
    order_no       VARCHAR(32)     NOT NULL,
    channel        VARCHAR(20)     NOT NULL,
    amount         DECIMAL(10, 2)  NOT NULL,
    status         VARCHAR(20)     NOT NULL,
    transaction_no VARCHAR(64)     NULL,
    paid_at        DATETIME(3)     NULL,
    created_at     DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at     DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_payment_no (payment_no),
    KEY idx_payment_order (order_no),
    CONSTRAINT chk_payment_amount CHECK (amount >= 0)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '支付记录';

CREATE TABLE banner (
    id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    title         VARCHAR(64)     NOT NULL DEFAULT '',
    image_url     VARCHAR(512)    NOT NULL,
    link_type     VARCHAR(20)     NOT NULL DEFAULT 'NONE',
    link_target   VARCHAR(64)     NOT NULL DEFAULT '',
    sort_order    INT             NOT NULL DEFAULT 0,
    status        VARCHAR(20)     NOT NULL DEFAULT 'ENABLED',
    created_at    DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at    DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_banner_status_sort (status, sort_order)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '轮播图';

CREATE TABLE admin_operation_log (
    id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    admin_id    BIGINT UNSIGNED NOT NULL,
    action      VARCHAR(64)     NOT NULL,
    target_type VARCHAR(32)     NOT NULL DEFAULT '',
    target_id   VARCHAR(64)     NOT NULL DEFAULT '',
    detail      VARCHAR(512)    NOT NULL DEFAULT '',
    request_id  VARCHAR(64)     NOT NULL DEFAULT '',
    created_at  DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_admin_log_admin_created (admin_id, created_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '管理员操作日志';
