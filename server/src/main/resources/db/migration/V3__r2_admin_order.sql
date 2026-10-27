-- R2: 管理员角色/token 版本、订单发票与后台备注、导出与自动确认索引

ALTER TABLE admin_user
    ADD COLUMN token_version INT NOT NULL DEFAULT 0 COMMENT '令牌版本，改密/禁用/改角色时递增';

UPDATE admin_user SET role = 'SUPER_ADMIN' WHERE role = 'ADMIN';

ALTER TABLE mall_order
    ADD COLUMN admin_remark VARCHAR(255) NOT NULL DEFAULT '' COMMENT '管理员内部备注',
    ADD COLUMN invoice_required TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否需要发票',
    ADD COLUMN invoice_title VARCHAR(128) NOT NULL DEFAULT '' COMMENT '发票抬头',
    ADD COLUMN invoice_tax_no VARCHAR(64) NOT NULL DEFAULT '' COMMENT '税号',
    ADD COLUMN invoice_email VARCHAR(128) NOT NULL DEFAULT '' COMMENT '电子发票接收邮箱';

CREATE INDEX idx_order_status_shipped ON mall_order (status, shipped_at);
CREATE INDEX idx_order_paid_at ON mall_order (paid_at);
CREATE INDEX idx_refund_success_at ON refund_record (status, success_at);
CREATE INDEX idx_admin_log_action_created ON admin_operation_log (action, created_at);
CREATE INDEX idx_admin_log_created ON admin_operation_log (created_at);
