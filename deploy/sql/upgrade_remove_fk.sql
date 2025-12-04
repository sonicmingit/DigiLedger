-- 删除 equip_upgrade_node 表中的外键约束
-- 这样可以直接删除 device_asset 中的记录，而不会受到外键约束的影响

ALTER TABLE equip_upgrade_node DROP FOREIGN KEY fk_upgrade_node_asset;
ALTER TABLE equip_upgrade_route DROP FOREIGN KEY fk_upgrade_route_asset;