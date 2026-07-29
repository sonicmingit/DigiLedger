ALTER TABLE device_asset
    ADD COLUMN related_links TEXT NULL COMMENT '相关链接 JSON' AFTER cover_image_url,
    ADD COLUMN manual_use_months INT NULL COMMENT '手动填写的使用时长（月）' AFTER related_links;
