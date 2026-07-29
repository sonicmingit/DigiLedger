-- 兼容在价格跟踪功能上线前已建成、且以 V1 作为 Flyway 基线的历史数据库。
ALTER TABLE wishlist
    ADD COLUMN current_price DECIMAL(12,2) NULL COMMENT '最近观测价格' AFTER expected_price,
    ADD COLUMN last_price_at DATETIME NULL COMMENT '最近价格采集时间' AFTER current_price;

CREATE TABLE IF NOT EXISTS wishlist_price_history (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    wishlist_id BIGINT NOT NULL COMMENT '心愿单ID',
    price DECIMAL(12,2) NOT NULL COMMENT '观测价格',
    captured_at DATETIME NOT NULL COMMENT '采集时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_wishlist_price_time (wishlist_id, captured_at),
    CONSTRAINT fk_wishlist_price_wishlist FOREIGN KEY (wishlist_id) REFERENCES wishlist(id) ON DELETE CASCADE
) COMMENT='心愿单价格观测历史';
