INSERT INTO sys_setting (currency, storage_provider, storage_bucket) VALUES ('CNY', 'minio', 'digiledger');

INSERT INTO device_asset (name, category, brand, model, serial_no, status, purchase_date, enabled_date, tags, cover_image_url, notes)
VALUES ('MacBook Pro 14', '笔记本', 'Apple', 'M3 Pro', 'SN123456', '使用中', '2023-10-01', '2023-10-01', JSON_ARRAY('创作', '主力机'), 'https://example.com/macbook.png', '主力剪辑设备');

INSERT INTO purchase (asset_id, type, platform, seller, price, shipping_cost, purchase_date, invoice_no, attachments, notes)
VALUES (1, 'PRIMARY', 'Apple Store', 'Apple', 16888.00, 0.00, '2023-10-01', 'APL-20231001', JSON_ARRAY('https://example.com/invoice-mbp.png'), '官网直营，三年 AppleCare+');

INSERT INTO device_asset (name, category, brand, model, serial_no, status, purchase_date, enabled_date, retired_date, tags, notes)
VALUES ('Sony A7M4', '相机', 'Sony', 'ILCE-7M4', 'SN987654', '已出售', '2022-05-12', '2022-05-15', '2024-02-01', JSON_ARRAY('摄影'), '含 24-105G 套机');

INSERT INTO purchase (asset_id, type, platform, seller, price, shipping_cost, purchase_date, notes)
VALUES (2, 'PRIMARY', '京东', 'Sony 旗舰店', 16800.00, 200.00, '2022-05-12', '618 活动立减');

INSERT INTO sale (asset_id, platform, buyer, sale_price, fee, shipping_cost, other_cost, net_income, sale_date, notes)
VALUES (2, '闲鱼', '张三', 12000.00, 200.00, 50.00, 0.00, 11750.00, '2024-02-01', '赠送备用电池');

INSERT INTO wishlist (name, category, brand, model, expected_price, planned_platform, link, notes, priority)
VALUES ('ThinkVision 4K 显示器', '显示器', 'Lenovo', 'P27h-30', 2999.00, '京东', 'https://www.jd.com/p27h30', '计划暑期采购', 2);
