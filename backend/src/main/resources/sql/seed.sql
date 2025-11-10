INSERT INTO sys_setting (annual_rate, currency) VALUES (0.5000, 'CNY');

INSERT INTO device_asset (name, category, brand, model, serial_no, status, purchase_id, purchase_date, enabled_date)
VALUES ('MacBook Pro 14', 'Laptop', 'Apple', 'M3 Pro', 'SN123456', 'IN_USE', NULL, '2023-10-01', '2023-10-01');

INSERT INTO purchase (asset_id, type, price, shipping_cost, tax, other_cost, purchase_date, note)
VALUES (1, 'PRIMARY', 16888.00, 0.00, 0.00, 0.00, '2023-10-01', 'Initial purchase');

INSERT INTO device_asset (name, category, brand, model, serial_no, status, purchase_id, purchase_date, enabled_date, sale_date)
VALUES ('Sony A7M4', 'Camera', 'Sony', 'ILCE-7M4', 'SN987654', 'SOLD', NULL, '2022-05-12', '2022-05-15', '2024-02-01');

INSERT INTO purchase (asset_id, type, price, shipping_cost, tax, other_cost, purchase_date, note)
VALUES (2, 'PRIMARY', 16800.00, 200.00, 0.00, 0.00, '2022-05-12', 'Bought during sale');

INSERT INTO sale (asset_id, sale_price, fee, shipping_cost, other_cost, net_income, realized_pnl, platform, sale_date)
VALUES (2, 12000.00, 200.00, 50.00, 0.00, 11750.00, -3050.00, 'Xianyu', '2024-02-01');
