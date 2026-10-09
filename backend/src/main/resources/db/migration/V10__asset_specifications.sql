ALTER TABLE device_asset
  ADD COLUMN specifications TEXT NULL COMMENT '主商品配置规格' AFTER serial_no;
