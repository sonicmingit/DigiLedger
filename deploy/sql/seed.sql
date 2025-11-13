INSERT INTO sys_setting (currency, storage_provider, storage_bucket)
VALUES ('CNY', 'minio', 'digiledger');

INSERT INTO dict_category (id, name, parent_id, level, sort) VALUES
    (1, '数码设备', NULL, 1, 10),
    (2, '电脑', 1, 2, 10),
    (3, '笔记本', 2, 3, 10),
    (4, '台式机', 2, 3, 20),
    (5, '摄影器材', 1, 2, 20),
    (6, '相机', 5, 3, 10),
    (7, '镜头', 5, 3, 20),
    (8, '音频设备', 1, 2, 30),
    (9, '耳机', 8, 3, 10),
    (10, '智能设备', 1, 2, 40),
    (11, '手机', 10, 3, 10),
    (12, '平板', 10, 3, 20);

INSERT INTO dict_brand (id, name, alias, initial, sort) VALUES
    (1, 'Apple', '苹果', 'A', 10),
    (2, 'Lenovo', '联想', 'L', 20),
    (3, 'Sony', NULL, 'S', 30),
    (4, 'Fujifilm', '富士', 'F', 40),
    (5, 'Canon', '佳能', 'C', 50),
    (6, 'NZXT', NULL, 'N', 60),
    (7, 'Sennheiser', NULL, 'S', 70);

INSERT INTO dict_platform (id, name, link, sort) VALUES
    (1, 'Apple Store', 'https://www.apple.com/store', 10),
    (2, '京东', 'https://www.jd.com', 20),
    (3, '淘宝', 'https://www.taobao.com', 30),
    (4, '闲鱼', 'https://2.taobao.com', 40);

INSERT INTO dict_tag (id, name, parent_id, color, icon, sort) VALUES
    (1, '创作', NULL, '#6266F1', 'ri-magic-fill', 10),
    (2, '主力机', NULL, '#2EC4B6', 'ri-flashlight-fill', 20),
    (3, '摄影', NULL, '#FF6B6B', 'ri-camera-lens-fill', 30),
    (4, '移动办公', NULL, '#F7B32B', 'ri-macbook-fill', 40),
    (5, '音频', NULL, '#4CC9F0', 'ri-headphone-fill', 50),
    (6, '备用', NULL, '#94A3B8', 'ri-inbox-archive-fill', 60);

INSERT INTO device_asset (name, category_id, category_path, brand_id, brand, model, serial_no, status, purchase_date, enabled_date, retired_date, cover_image_url, notes)
VALUES
    ('MacBook Pro 14" (2023)', 3, '/1/2/3', 1, 'Apple', 'MacBook Pro 14 M3 Pro', 'C02MBP001', '使用中', '2023-10-03', '2023-10-05', NULL, 'https://cdn.example.com/assets/macbook-pro-14.png', '剪辑与移动办公主力'),
    ('ThinkPad X1 Carbon Gen11', 3, '/1/2/3', 2, 'Lenovo', '21HM-CTO', 'PF11TPC02', '使用中', '2024-01-15', '2024-01-18', NULL, 'https://cdn.example.com/assets/x1-carbon-gen11.png', '客户演示与会议记录'),
    ('NZXT Creator Station', 4, '/1/2/4', 6, 'NZXT', 'H5 Elite / Ryzen 9 7950X', 'DIY-202311-001', '使用中', '2023-11-20', '2023-11-25', NULL, 'https://cdn.example.com/assets/nzxt-creator.png', '4K 渲染与批量导出专用'),
    ('Sony Alpha 7 IV', 6, '/1/5/6', 3, 'Sony', 'ILCE-7M4', 'SN-A7M4-4587', '已闲置', '2022-06-01', '2022-06-05', NULL, 'https://cdn.example.com/assets/sony-a7iv.png', '曾作为主力全画幅机身，现轮换备用'),
    ('Fujifilm X100VI', 6, '/1/5/6', 4, 'Fujifilm', 'X100VI', 'FFX100VI-0032', '使用中', '2024-03-02', '2024-03-03', NULL, 'https://cdn.example.com/assets/x100vi.png', '日常街拍与采访随身机'),
    ('Canon RF 24-70mm F2.8L', 7, '/1/5/7', 5, 'Canon', 'RF24-70 F2.8L IS USM', 'RF2470-8891', '使用中', '2022-08-15', '2022-08-18', NULL, 'https://cdn.example.com/assets/rf-24-70.png', '常驻棚拍标准变焦镜头'),
    ('AirPods Pro (2nd Gen)', 9, '/1/8/9', 1, 'Apple', 'AirPods Pro 2 USB-C', 'APPRO2-1122', '已闲置', '2023-09-10', '2023-09-12', NULL, 'https://cdn.example.com/assets/airpods-pro-2.png', '转为会议备用耳机'),
    ('Sony WH-1000XM5', 9, '/1/8/9', 3, 'Sony', 'WH-1000XM5', 'WH1000-6677', '待出售', '2022-12-20', '2022-12-22', NULL, 'https://cdn.example.com/assets/wh1000xm5.png', '计划在二手平台出手'),
    ('iPhone 15 Pro 256G', 11, '/1/10/11', 1, 'Apple', 'iPhone 15 Pro', 'DX3V15P256', '使用中', '2023-09-22', '2023-09-25', NULL, 'https://cdn.example.com/assets/iphone-15-pro.png', '主力移动拍摄与通讯设备'),
    ('iPad Pro 12.9" (2021)', 12, '/1/10/12', 1, 'Apple', 'iPad Pro 12.9 M1', 'DLXIPAD12921', '已出售', '2021-05-25', '2021-05-27', '2024-04-02', 'https://cdn.example.com/assets/ipad-pro-2021.png', '已升级至最新款，旧设备完成转售');

INSERT INTO purchase (asset_id, type, name, platform_id, platform_name, seller, price, currency, quantity, shipping_cost, purchase_date, invoice_no, warranty_months, warranty_expire_date, attachments, notes)
VALUES
    (1, 'PRIMARY', NULL, 1, 'Apple Store', 'Apple', 16888.00, 'CNY', 1, 0.00, '2023-10-03', 'APL-20231003', 36, '2026-10-03', JSON_ARRAY('https://cdn.example.com/docs/macbook-pro-14-invoice.pdf'), '含 AppleCare+ 服务'),
    (2, 'PRIMARY', NULL, 2, '京东', '联想官方旗舰店', 12499.00, 'CNY', 1, 20.00, '2024-01-15', 'JD-20240115-001', 36, '2027-01-15', JSON_ARRAY('https://cdn.example.com/docs/x1-carbon-order.png'), '加购企业延保服务'),
    (3, 'PRIMARY', NULL, 2, '京东', 'NZXT 授权店', 21888.00, 'CNY', 1, 120.00, '2023-11-20', 'JD-20231120-888', 24, '2025-11-20', JSON_ARRAY('https://cdn.example.com/docs/nzxt-creator-invoice.pdf'), '整机含 4TB RAID 阵列'),
    (4, 'PRIMARY', NULL, 2, '京东', '索尼官方旗舰店', 16800.00, 'CNY', 1, 0.00, '2022-06-01', 'JD-20220601-SONY', 12, '2023-06-01', JSON_ARRAY('https://cdn.example.com/docs/a7iv-receipt.png'), '含原厂手柄套装'),
    (5, 'PRIMARY', NULL, 1, 'Apple Store', 'Apple', 10999.00, 'CNY', 1, 0.00, '2024-03-02', 'APL-20240302', 12, '2025-03-02', JSON_ARRAY('https://cdn.example.com/docs/x100vi-invoice.pdf'), '首发当天购买'),
    (6, 'PRIMARY', '标准变焦镜头', 3, '淘宝', '佳能影像官方店', 15800.00, 'CNY', 1, 35.00, '2022-08-15', 'TB-20220815-RF', 24, '2024-08-15', JSON_ARRAY('https://cdn.example.com/docs/rf-2470-receipt.jpg'), '含原厂 UV 镜'),
    (7, 'PRIMARY', NULL, 1, 'Apple Store', 'Apple', 1899.00, 'CNY', 1, 0.00, '2023-09-10', 'APL-20230910', 12, '2024-09-10', JSON_ARRAY('https://cdn.example.com/docs/airpods-pro2.png'), '搭配 Mac 使用'),
    (8, 'PRIMARY', NULL, 2, '京东', '索尼京东自营', 2599.00, 'CNY', 1, 0.00, '2022-12-20', 'JD-20221220-SONY', 24, '2024-12-20', JSON_ARRAY('https://cdn.example.com/docs/wh1000xm5-invoice.png'), '含官方收纳包'),
    (9, 'PRIMARY', NULL, 1, 'Apple Store', 'Apple', 9999.00, 'CNY', 1, 0.00, '2023-09-22', 'APL-20230922', 12, '2024-09-22', JSON_ARRAY('https://cdn.example.com/docs/iphone15pro-invoice.pdf'), '含 AppleCare+ 全保'),
    (10, 'PRIMARY', NULL, 1, 'Apple Store', 'Apple', 8499.00, 'CNY', 1, 0.00, '2021-05-25', 'APL-20210525', 24, '2023-05-25', JSON_ARRAY('https://cdn.example.com/docs/ipad-pro-2021-invoice.pdf'), '配套妙控键盘另购');

INSERT INTO sale (asset_id, platform_id, platform_name, buyer, sale_price, fee, shipping_cost, other_cost, net_income, sale_date, notes)
VALUES
    (10, 4, '闲鱼', '李先生', 6500.00, 130.00, 25.00, 0.00, 6345.00, '2024-04-02', '附赠妙控键盘');

INSERT INTO asset_tag_map (asset_id, tag_id) VALUES
    (1, 1), (1, 2), (1, 4),
    (2, 4),
    (3, 1), (3, 2),
    (4, 1), (4, 3), (4, 6),
    (5, 1), (5, 3),
    (6, 3),
    (7, 5), (7, 6),
    (8, 5),
    (9, 1), (9, 2),
    (10, 4);

INSERT INTO wishlist (name, category_id, brand_id, image_url, status, link, notes, priority)
VALUES
    ('XDR 参考级显示器', 2, 1, 'https://cdn.example.com/assets/pro-display-xdr.png', '未购买', 'https://www.apple.com/cn/pro-display-xdr/', '预计下半年采购', 2);
