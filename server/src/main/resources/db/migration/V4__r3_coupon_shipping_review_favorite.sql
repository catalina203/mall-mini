-- R3: 优惠券、运费模板、商品评价、收藏与浏览足迹

CREATE TABLE shipping_template (
    id             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    name           VARCHAR(64)     NOT NULL,
    default_fee    DECIMAL(10, 2)  NOT NULL DEFAULT 0.00,
    free_threshold DECIMAL(10, 2)  NOT NULL DEFAULT 0.00,
    status         VARCHAR(20)     NOT NULL DEFAULT 'ENABLED',
    created_at     DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at     DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_shipping_template_status (status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '运费模板';

CREATE TABLE shipping_template_rule (
    id               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    template_id      BIGINT UNSIGNED NOT NULL,
    region_code      VARCHAR(64)     NOT NULL DEFAULT '' COMMENT '省份名称；空表示全国',
    first_unit       INT             NOT NULL DEFAULT 1,
    first_fee        DECIMAL(10, 2)  NOT NULL DEFAULT 0.00,
    additional_unit  INT             NOT NULL DEFAULT 1,
    additional_fee   DECIMAL(10, 2)  NOT NULL DEFAULT 0.00,
    free_threshold   DECIMAL(10, 2)  NOT NULL DEFAULT 0.00,
    status           VARCHAR(20)     NOT NULL DEFAULT 'ENABLED',
    sort_order       INT             NOT NULL DEFAULT 0,
    created_at       DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at       DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_shipping_rule_template (template_id, status, sort_order)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '运费模板规则';

ALTER TABLE product
    ADD COLUMN shipping_template_id BIGINT UNSIGNED NULL COMMENT '运费模板，空使用全局配置';

CREATE TABLE coupon_template (
    id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    name          VARCHAR(64)     NOT NULL,
    type          VARCHAR(20)     NOT NULL COMMENT 'FIXED / PERCENT',
    value         DECIMAL(10, 2)  NOT NULL COMMENT 'FIXED 为减免金额；PERCENT 为减免百分比',
    min_amount    DECIMAL(10, 2)  NOT NULL DEFAULT 0.00,
    scope         VARCHAR(20)     NOT NULL DEFAULT 'ALL' COMMENT 'ALL / CATEGORY / PRODUCT',
    scope_id      BIGINT UNSIGNED NULL,
    total_count   INT             NOT NULL DEFAULT 0,
    claimed_count INT             NOT NULL DEFAULT 0,
    per_user_limit INT            NOT NULL DEFAULT 1,
    valid_from    DATETIME(3)     NOT NULL,
    valid_to      DATETIME(3)     NOT NULL,
    status        VARCHAR(20)     NOT NULL DEFAULT 'ENABLED',
    created_at    DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at    DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_coupon_template_status (status, valid_to)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '优惠券模板';

CREATE TABLE coupon (
    id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    coupon_no   VARCHAR(32)     NOT NULL,
    template_id BIGINT UNSIGNED NOT NULL,
    user_id     BIGINT UNSIGNED NOT NULL,
    status      VARCHAR(20)     NOT NULL DEFAULT 'UNUSED',
    order_no    VARCHAR(32)     NULL,
    used_at     DATETIME(3)     NULL,
    valid_from  DATETIME(3)     NOT NULL,
    valid_to    DATETIME(3)     NOT NULL,
    created_at  DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at  DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_coupon_no (coupon_no),
    UNIQUE KEY uk_coupon_template_user (template_id, user_id),
    KEY idx_coupon_user_status (user_id, status, valid_to),
    KEY idx_coupon_order (order_no)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户优惠券';

ALTER TABLE mall_order
    ADD COLUMN coupon_id BIGINT UNSIGNED NULL,
    ADD COLUMN coupon_name VARCHAR(64) NOT NULL DEFAULT '';

CREATE TABLE product_review (
    id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    order_no      VARCHAR(32)     NOT NULL,
    order_item_id BIGINT UNSIGNED NOT NULL,
    product_id    BIGINT UNSIGNED NOT NULL,
    sku_id        BIGINT UNSIGNED NOT NULL,
    user_id       BIGINT UNSIGNED NOT NULL,
    rating        TINYINT         NOT NULL,
    content       VARCHAR(1000)   NOT NULL DEFAULT '',
    anonymous     TINYINT(1)      NOT NULL DEFAULT 0,
    status        VARCHAR(20)     NOT NULL DEFAULT 'PENDING',
    reply         VARCHAR(500)    NOT NULL DEFAULT '',
    created_at    DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at    DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_review_order_item (order_item_id),
    KEY idx_review_product_status (product_id, status, created_at),
    KEY idx_review_user (user_id),
    CONSTRAINT chk_review_rating CHECK (rating BETWEEN 1 AND 5)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '商品评价';

CREATE TABLE favorite (
    id         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id    BIGINT UNSIGNED NOT NULL,
    product_id BIGINT UNSIGNED NOT NULL,
    created_at DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_favorite_user_product (user_id, product_id),
    KEY idx_favorite_user_created (user_id, created_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '商品收藏';

CREATE TABLE browse_history (
    id         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id    BIGINT UNSIGNED NOT NULL,
    product_id BIGINT UNSIGNED NOT NULL,
    viewed_at  DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_browse_user_product (user_id, product_id),
    KEY idx_browse_user_viewed (user_id, viewed_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '浏览足迹';

INSERT INTO shipping_template (id, name, default_fee, free_threshold, status)
VALUES (1, '默认运费模板', 10.00, 99.00, 'ENABLED');

INSERT INTO shipping_template_rule
    (template_id, region_code, first_unit, first_fee, additional_unit, additional_fee, free_threshold, status, sort_order)
VALUES (1, '', 1, 10.00, 1, 0.00, 99.00, 'ENABLED', 0);

UPDATE product SET shipping_template_id = 1 WHERE shipping_template_id IS NULL;
