INSERT INTO sys_setting (currency, storage_provider, storage_bucket) VALUES ('CNY', 'minio', 'digiledger');

INSERT INTO dict_category (id, name, parent_id, level, sort) VALUES
    (1, '数码设备', NULL, 1, 10),
    (2, '电脑', 1, 2, 10),
    (3, '笔记本', 2, 3, 10),
    (4, '摄影器材', 1, 2, 20),
    (5, '相机', 4, 3, 10);

INSERT INTO dict_brand (id, name, alias, initial, sort) VALUES
    (1, 'Apple', '苹果', 'A', 10),
    (2, 'Sony', NULL, 'S', 20),
    (3, 'Lenovo', '联想', 'L', 30);

INSERT INTO dict_platform (id, name, link, sort) VALUES
    (1, 'Apple Store', 'https://www.apple.com/store', 10),
    (2, '京东', 'https://www.jd.com', 20),
    (3, '闲鱼', 'https://2.taobao.com', 30);

INSERT INTO dict_tag (id, name, parent_id, color, icon, sort) VALUES
    (1, '创作', NULL, '#6266F1', 'ri-magic-fill', 10),
    (2, '主力机', NULL, '#2EC4B6', 'ri-flashlight-fill', 20),
    (3, '摄影', NULL, '#FF6B6B', 'ri-camera-lens-fill', 30);

INSERT INTO device_asset (name, category_id, category_path, brand_id, brand, model, serial_no, status, purchase_date, enabled_date, cover_image_url, notes)
VALUES ('MacBook Pro 14', 3, '/1/2/3', 1, 'Apple', 'M3 Pro', 'SN123456', '使用中', '2023-10-01', '2023-10-01', 'https://example.com/macbook.png', '主力剪辑设备');

INSERT INTO purchase (asset_id, type, name, platform_id, platform_name, seller, price, shipping_cost, purchase_date, invoice_no, attachments, notes)
VALUES (1, 'PRIMARY', NULL, 1, 'Apple Store', 'Apple', 16888.00, 0.00, '2023-10-01', 'APL-20231001', JSON_ARRAY('https://example.com/invoice-mbp.png'), '官网直营，三年 AppleCare+');

INSERT INTO asset_tag_map (asset_id, tag_id) VALUES (1, 1), (1, 2);

INSERT INTO device_asset (name, category_id, category_path, brand_id, brand, model, serial_no, status, purchase_date, enabled_date, retired_date, notes)
VALUES ('Sony A7M4', 5, '/1/4/5', 2, 'Sony', 'ILCE-7M4', 'SN987654', '已出售', '2022-05-12', '2022-05-15', '2024-02-01', '含 24-105G 套机');

INSERT INTO purchase (asset_id, type, name, platform_id, platform_name, seller, price, shipping_cost, purchase_date, notes)
VALUES (2, 'PRIMARY', NULL, 2, '京东', 'Sony 旗舰店', 16800.00, 200.00, '2022-05-12', '618 活动立减');

INSERT INTO sale (asset_id, platform_id, platform_name, buyer, sale_price, fee, shipping_cost, other_cost, net_income, sale_date, notes)
VALUES (2, 3, '闲鱼', '张三', 12000.00, 200.00, 50.00, 0.00, 11750.00, '2024-02-01', '赠送备用电池');

INSERT INTO asset_tag_map (asset_id, tag_id) VALUES (2, 3);

INSERT INTO wishlist (name, category_id, brand_id, image_url, status, link, notes, priority)
VALUES ('ThinkVision 4K 显示器', 2, 3, 'https://example.com/p27h30.png', '未购买', 'https://www.jd.com/p27h30', '计划暑期采购', 2);
