-- R4: 搜索优化、客服工单、会员积分、首页运营位

ALTER TABLE mall_user
    ADD COLUMN points INT NOT NULL DEFAULT 0 COMMENT '当前积分余额',
    ADD COLUMN total_points INT NOT NULL DEFAULT 0 COMMENT '累计获得积分',
    ADD COLUMN member_level VARCHAR(20) NOT NULL DEFAULT 'NORMAL' COMMENT '会员等级';

CREATE TABLE points_log (
    id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id       BIGINT UNSIGNED NOT NULL,
    change_type   VARCHAR(32)     NOT NULL,
    points_delta  INT             NOT NULL,
    balance_after INT             NOT NULL,
    biz_type      VARCHAR(32)     NOT NULL DEFAULT '',
    biz_no        VARCHAR(64)     NOT NULL DEFAULT '',
    remark        VARCHAR(255)    NOT NULL DEFAULT '',
    created_at    DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_points_biz (change_type, biz_no, user_id),
    KEY idx_points_user_created (user_id, created_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '积分流水';

ALTER TABLE product
    ADD COLUMN sales_count INT NOT NULL DEFAULT 0 COMMENT '累计销量（按已支付件数）';

CREATE FULLTEXT INDEX ft_product_search ON product (name, subtitle, description) WITH PARSER ngram;

CREATE TABLE search_keyword (
    id               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    keyword          VARCHAR(64)     NOT NULL,
    search_count     INT             NOT NULL DEFAULT 0,
    last_searched_at DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    created_at       DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_search_keyword (keyword),
    KEY idx_search_keyword_count (search_count, last_searched_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '搜索热词';

CREATE TABLE support_ticket (
    id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    ticket_no   VARCHAR(32)     NOT NULL,
    user_id     BIGINT UNSIGNED NOT NULL,
    order_no    VARCHAR(32)     NULL,
    type        VARCHAR(32)     NOT NULL DEFAULT 'OTHER',
    subject     VARCHAR(128)    NOT NULL,
    content     VARCHAR(2000)   NOT NULL,
    status      VARCHAR(20)     NOT NULL DEFAULT 'OPEN',
    reply       VARCHAR(2000)   NOT NULL DEFAULT '',
    replied_by  BIGINT UNSIGNED NULL,
    replied_at  DATETIME(3)     NULL,
    closed_at   DATETIME(3)     NULL,
    created_at  DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at  DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_support_ticket_no (ticket_no),
    KEY idx_support_user_created (user_id, created_at),
    KEY idx_support_status_created (status, created_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '客服工单';
