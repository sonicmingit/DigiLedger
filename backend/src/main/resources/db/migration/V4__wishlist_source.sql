-- 心愿来源用于复盘“从哪里种草”；购买时间、金额、平台保存到转化后物品的主购买记录。
ALTER TABLE wishlist ADD COLUMN source VARCHAR(200) NULL COMMENT '心愿来源/种草途径' AFTER link;
ALTER TABLE wishlist ADD COLUMN purchased_at DATE NULL COMMENT '确认购买日期' AFTER converted_asset_id,
    ADD COLUMN purchased_price DECIMAL(12,2) NULL COMMENT '确认购买价格' AFTER purchased_at,
    ADD COLUMN purchase_price_diff DECIMAL(12,2) NULL COMMENT '购买价减目标价' AFTER purchased_price;
