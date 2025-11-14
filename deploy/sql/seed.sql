INSERT INTO sys_setting (currency, storage_provider, storage_bucket)
VALUES ('CNY', 'minio', 'digiledger');

INSERT INTO dict_category (id, name, parent_id, level, sort) VALUES
    (1, '办公设备', NULL, 1, 10),
    (2, '笔记本电脑', 1, 2, 10),
    (3, '摄影器材', NULL, 1, 20),
    (4, '摄影附件', 3, 2, 10),
    (5, '无人机', 3, 2, 20);

INSERT INTO dict_brand (id, name, alias, initial, sort) VALUES
    (1, 'Apple', '苹果', 'A', 10),
    (2, 'Huawei', '华为', 'H', 20),
    (3, 'DJI', '大疆', 'D', 30),
    (4, 'Sony', '索尼', 'S', 40);

INSERT INTO dict_platform (id, name, link, sort) VALUES
    (1, 'Apple Store', 'https://www.apple.com/store', 10),
    (2, '京东自营', 'https://www.jd.com', 20),
    (3, '淘宝海外', 'https://www.taobao.com', 30),
    (4, 'B&H Photo Video', 'https://www.bhphotovideo.com', 40);

INSERT INTO dict_tag (id, name, parent_id, color, icon, sort) VALUES
    (1, '高端', NULL, '#FF5A5F', 'ri-diamond-fill', 10),
    (2, '便携', NULL, '#43A5FF', 'ri-battery-charging-line', 20),
    (3, '摄影', NULL, '#FFA500', 'ri-camera-fill', 30),
    (4, '办公', NULL, '#4CAF50', 'ri-briefcase-fill', 40),
    (5, '无人机', 3, '#A55EEA', 'ri-rocket-2-fill', 50),
    (6, '旗舰', 1, '#F7B731', 'ri-star-fill', 60);

INSERT INTO device_asset (id, name, category_id, category_path, brand_id, brand, model, serial_no, status, purchase_id, purchase_date, enabled_date, retired_date, cover_image_url, notes) VALUES
    (1, 'MacBook Pro 16" (2024)', 2, '/1/2', 1, 'Apple', 'MacBook Pro 16 M3 Max', 'MBP24-16001', '使用中', 1001, '2024-05-07', '2024-05-10', NULL, 'https://assets.example.com/macos/macbook16-2024.png', '团队剪辑主力机'),
    (2, 'MateBook 16s', 2, '/1/2', 2, 'Huawei', 'MateBook 16s 2023', 'HM16S-045', '使用中', 1002, '2024-07-11', '2024-07-12', NULL, 'https://assets.example.com/huawei/matebook16s.png', '流媒体出行备用'),
    (3, 'DJI Inspire 3', 5, '/3/5', 3, 'DJI', 'Inspire 3', 'INSP3-00123', '报废中', 1003, '2023-09-30', '2023-10-02', '2025-02-15', 'https://assets.example.com/dji/inspire3.png', '航拍试验后转售'),
    (4, 'Sony FX30 Cinema Camera', 3, '/3/4', 4, 'Sony', 'FX30', 'FX30-2024-07', '维修中', 1004, '2024-03-15', '2024-03-20', NULL, 'https://assets.example.com/sony/fx30.png', '记录团队主机');

INSERT INTO purchase (id, asset_id, type, name, platform_id, platform_name, seller, price, currency, quantity, shipping_cost, purchase_date, invoice_no, warranty_months, warranty_expire_date, attachments, notes) VALUES
    (1001, 1, 'PRIMARY', 'MacBook Pro 16" 2024', 1, 'Apple Store', 'Apple 授权经销商', 23888.00, 'CNY', 1, 0.00, '2024-05-07', 'APL-20240507-MBP', 36, '2027-05-07', JSON_ARRAY('https://assets.example.com/invoices/macbook-pro-16.pdf'), '为视频编辑团队采购'),
    (1002, 2, 'PRIMARY', 'MateBook 16s 2023', 2, '京东自营', 'JD 供应商', 9999.00, 'CNY', 1, 0.00, '2024-07-11', 'JD-20240711-MB16', 24, '2026-07-11', JSON_ARRAY('https://assets.example.com/invoices/matebook16s.pdf'), '流媒体出行备用机'),
    (1003, 3, 'PRIMARY', 'DJI Inspire 3 套装', 3, '淘宝海外', '大疆官方旗舰店', 42800.00, 'CNY', 1, 120.00, '2023-09-30', 'TB-20230930-INSP3', 12, '2024-09-30', JSON_ARRAY('https://assets.example.com/invoices/inspire3.pdf'), '用于航拍测试和素材采集'),
    (1004, 4, 'PRIMARY', 'Sony FX30 机身', 4, 'B&H Photo Video', 'B&H', 20990.00, 'USD', 1, 120.00, '2024-03-15', 'BH-20240315-FX30', 24, '2026-03-15', JSON_ARRAY('https://assets.example.com/invoices/fx30.pdf'), '记录团队主机，支持 4K 120');

INSERT INTO sale (id, asset_id, sale_scope, purchase_id, platform_id, platform_name, buyer, sale_price, fee, shipping_cost, other_cost, net_income, sale_date, attachments, notes) VALUES
    (2001, 3, 'ASSET', 1003, 2, '京东二手', '城市影业有限公司', 38900.00, 390.00, 80.00, 20.00, 38410.00, '2025-02-14', JSON_ARRAY('https://assets.example.com/sales/inspire3-sale.pdf'), '航拍试验机经翻新后出售');

INSERT INTO asset_tag_map (asset_id, tag_id) VALUES
    (1, 1), (1, 4), (1, 6),
    (2, 2), (2, 4),
    (3, 3), (3, 5),
    (4, 1), (4, 3);

INSERT INTO wishlist (id, name, category_id, brand_id, model, expected_price, image_url, status, link, notes, priority, converted_asset_id) VALUES
    (201, '内容创作灯光套装', 4, 4, 'Sony CLM-1', 12800.00, 'https://assets.example.com/wishlist/content-lighting.png', '未完成', 'https://www.sony.com/camera-lighting', '补光、直播双模式', 2, NULL),
    (202, '便携直播解决方案', 2, 1, 'Mac Studio + Studio Display', 19800.00, 'https://assets.example.com/wishlist/portable-live.png', '已完成', 'https://www.apple.com/live-production/', '已转化为 MacBook Pro', 1, 1);

INSERT INTO wishlist_tag_map (wishlist_id, tag_id) VALUES
    (201, 3), (201, 4),
    (202, 2), (202, 6);

INSERT INTO file_attachment (biz_type, biz_id, object_key, file_name, file_type, file_size, extra, created_at, updated_at) VALUES
    ('purchase', 1001, 'attachments/purchases/macbook-pro-16-invoice.pdf', 'MacBook Pro 16 Invoice.pdf', 'application/pdf', 312400, JSON_OBJECT('source', 'seed', 'tags', JSON_ARRAY('invoice')), NOW(), NOW()),
    ('sale', 2001, 'attachments/sales/inspire3-sale.pdf', 'Inspire 3 Sale.pdf', 'application/pdf', 184210, JSON_OBJECT('source', 'seed', 'tags', JSON_ARRAY('sale')), NOW(), NOW()),
    ('asset', 2, 'attachments/assets/matebook16s.png', 'MateBook 16s.png', 'image/png', 512000, JSON_OBJECT('source', 'seed', 'tags', JSON_ARRAY('preview')), NOW(), NOW());
