-- 旧环境的升级路线 V2 兼容迁移；仅在已执行 upgrade_route_rebuild_v1.sql 后执行一次。
-- 使用 Flyway 的环境由 V6__upgrade_route_wishlist_and_mainline.sql 自动执行，不要重复执行。
ALTER TABLE equip_upgrade_route ADD COLUMN main_asset_id BIGINT NULL COMMENT '路线当前主物品ID' AFTER root_asset_id;
ALTER TABLE equip_upgrade_node
    ADD COLUMN wishlist_id BIGINT NULL COMMENT '关联心愿单ID，购买后自动替换为物品' AFTER asset_id,
    ADD COLUMN is_mainline TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否参与上下级主线计算' AFTER node_type;
UPDATE equip_upgrade_node node
JOIN equip_upgrade_link link ON link.route_id = node.route_id AND link.to_node_id = node.id
SET node.is_mainline = 0
WHERE link.relation_type = 'ALTERNATIVE' AND node.is_deleted = 0 AND link.is_deleted = 0;
CREATE INDEX idx_upgrade_node_route_wishlist ON equip_upgrade_node(route_id, wishlist_id, is_deleted);
CREATE INDEX idx_upgrade_node_route_mainline ON equip_upgrade_node(route_id, is_mainline, level, is_deleted);
