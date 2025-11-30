-- DigiLedger 升级脚本：V0.4 装备升级路线图（2025-11-30）
-- 新增装备升级路线、节点、关系三张表

CREATE TABLE IF NOT EXISTS equip_upgrade_route (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
  name VARCHAR(200) NOT NULL COMMENT '路线名称',
  root_asset_id BIGINT DEFAULT NULL COMMENT '起点资产ID',
  remark TEXT COMMENT '备注',
  is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  CONSTRAINT fk_upgrade_route_asset FOREIGN KEY (root_asset_id) REFERENCES device_asset(id)
) COMMENT='装备升级路线表';

CREATE TABLE IF NOT EXISTS equip_upgrade_node (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
  route_id BIGINT NOT NULL COMMENT '所属路线ID',
  asset_id BIGINT NOT NULL COMMENT '关联资产ID',
  level INT DEFAULT 1 COMMENT '层级',
  sort INT DEFAULT 0 COMMENT '排序',
  label VARCHAR(200) DEFAULT NULL COMMENT '节点标签',
  remark TEXT COMMENT '备注',
  is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  INDEX idx_upgrade_node_route (route_id),
  INDEX idx_upgrade_node_asset (asset_id),
  CONSTRAINT fk_upgrade_node_route FOREIGN KEY (route_id) REFERENCES equip_upgrade_route(id) ON DELETE CASCADE,
  CONSTRAINT fk_upgrade_node_asset FOREIGN KEY (asset_id) REFERENCES device_asset(id)
) COMMENT='装备升级节点表';

CREATE TABLE IF NOT EXISTS equip_upgrade_link (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
  route_id BIGINT NOT NULL COMMENT '所属路线ID',
  from_node_id BIGINT NOT NULL COMMENT '前代节点ID',
  to_node_id BIGINT NOT NULL COMMENT '后代节点ID',
  remark TEXT COMMENT '备注',
  is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  INDEX idx_upgrade_link_route (route_id),
  INDEX idx_upgrade_link_from (from_node_id),
  INDEX idx_upgrade_link_to (to_node_id),
  CONSTRAINT fk_upgrade_link_route FOREIGN KEY (route_id) REFERENCES equip_upgrade_route(id) ON DELETE CASCADE,
  CONSTRAINT fk_upgrade_link_from FOREIGN KEY (from_node_id) REFERENCES equip_upgrade_node(id) ON DELETE CASCADE,
  CONSTRAINT fk_upgrade_link_to FOREIGN KEY (to_node_id) REFERENCES equip_upgrade_node(id) ON DELETE CASCADE
) COMMENT='装备升级关系表';
