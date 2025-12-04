-- V0.3.8 新增字段：购买记录商品链接
ALTER TABLE purchase
    ADD COLUMN product_link VARCHAR(1000) NULL AFTER warranty_expire_date;
