-- 升级路线重构补充迁移（MySQL 8.3）。
-- 执行前提：既有库已经执行 deploy/sql/upgrade_figma_contract.sql；本脚本仅执行一次。
-- 若服务使用 Flyway，请改由 V3__rebuild_upgrade_route.sql 自动执行，避免重复变更。

ALTER TABLE equip_upgrade_route
    ADD COLUMN route_type VARCHAR(20) NOT NULL DEFAULT 'MIXED' COMMENT '路线类型：ACTUAL/PLAN/MIXED' AFTER annual_budget,
    ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '路线状态：ACTIVE/COMPLETED/ARCHIVED' AFTER route_type;

ALTER TABLE equip_upgrade_node
    ADD COLUMN node_type VARCHAR(20) NOT NULL DEFAULT 'PLANNED' COMMENT '节点类型：ASSET/PLANNED' AFTER asset_id,
    ADD COLUMN alternative_purpose VARCHAR(100) NULL COMMENT '同级节点用途说明' AFTER label;

ALTER TABLE equip_upgrade_link
    ADD COLUMN relation_type VARCHAR(20) NOT NULL DEFAULT 'SEQUENCE' COMMENT '关系类型：SEQUENCE/ALTERNATIVE' AFTER to_node_id;

UPDATE equip_upgrade_node
SET node_type = CASE WHEN asset_id IS NULL THEN 'PLANNED' ELSE 'ASSET' END
WHERE node_type IS NULL OR node_type = 'PLANNED';

CREATE INDEX idx_upgrade_node_route_asset ON equip_upgrade_node(route_id, asset_id, is_deleted);
CREATE INDEX idx_upgrade_link_route_relation ON equip_upgrade_link(route_id, relation_type, is_deleted);
