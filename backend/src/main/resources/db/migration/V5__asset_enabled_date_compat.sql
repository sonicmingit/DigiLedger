-- 兼容历史数据库遗留的 enabled_date 非空列；新库补列，旧库放宽为可空。
SET @enabled_date_exists := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'device_asset' AND COLUMN_NAME = 'enabled_date'
);
SET @enabled_date_sql := IF(@enabled_date_exists = 0,
    'ALTER TABLE device_asset ADD COLUMN enabled_date DATE NULL COMMENT ''启用日期'' AFTER purchase_date',
    'ALTER TABLE device_asset MODIFY COLUMN enabled_date DATE NULL COMMENT ''启用日期'''
);
PREPARE enabled_date_stmt FROM @enabled_date_sql;
EXECUTE enabled_date_stmt;
DEALLOCATE PREPARE enabled_date_stmt;
