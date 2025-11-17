-- 清库脚本：清空业务表与字典表数据，不删除表结构
-- 注意：执行前请确认当前数据库为测试/开发环境，避免误删生产数据。

SET FOREIGN_KEY_CHECKS = 0;

TRUNCATE TABLE asset_tag_map;
TRUNCATE TABLE wishlist_tag_map;
TRUNCATE TABLE sale;
TRUNCATE TABLE purchase;
TRUNCATE TABLE wishlist;
TRUNCATE TABLE device_asset;
TRUNCATE TABLE file_attachment;

TRUNCATE TABLE dict_category;
TRUNCATE TABLE dict_platform;
TRUNCATE TABLE dict_tag;
TRUNCATE TABLE dict_brand;

TRUNCATE TABLE sys_setting;

SET FOREIGN_KEY_CHECKS = 1;

