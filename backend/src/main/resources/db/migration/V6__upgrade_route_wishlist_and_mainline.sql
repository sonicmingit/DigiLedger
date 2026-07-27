-- 升级路线 V2：未来节点不再使用独立“计划商品”字段，而是关联心愿单；
-- main_asset_id 标记当前正在使用的主物品，is_mainline 标记参与上下级计算的同级主物品。
ALTER TABLE equip_upgrade_route
    ADD COLUMN main_asset_id BIGINT NULL COMMENT '路线当前主物品ID' AFTER root_asset_id;

ALTER TABLE equip_upgrade_node
    ADD COLUMN wishlist_id BIGINT NULL COMMENT '关联心愿单ID，购买后自动替换为物品' AFTER asset_id,
    ADD COLUMN is_mainline TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否参与上下级主线计算' AFTER node_type;

-- 已有同级节点保留在路线中，但默认只让原有主链参与上下级价差计算。
UPDATE equip_upgrade_node node
JOIN equip_upgrade_link link ON link.route_id = node.route_id AND link.to_node_id = node.id
SET node.is_mainline = 0
WHERE link.relation_type = 'ALTERNATIVE' AND node.is_deleted = 0 AND link.is_deleted = 0;

CREATE INDEX idx_upgrade_node_route_wishlist ON equip_upgrade_node(route_id, wishlist_id, is_deleted);
CREATE INDEX idx_upgrade_node_route_mainline ON equip_upgrade_node(route_id, is_mainline, level, is_deleted);
