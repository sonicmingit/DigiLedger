-- DigiLedger schema initialization（对齐设计文档 V0.3.4）
CREATE TABLE IF NOT EXISTS sys_setting (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
  currency VARCHAR(10) NOT NULL DEFAULT 'CNY' COMMENT '默认币种',
  storage_provider VARCHAR(32) NOT NULL DEFAULT 'minio' COMMENT '对象存储服务提供商',
  storage_endpoint VARCHAR(255) COMMENT '对象存储访问地址',
  storage_region VARCHAR(64) COMMENT '对象存储区域',
  storage_bucket VARCHAR(128) DEFAULT 'digiledger' COMMENT '对象存储桶名称',
  storage_access_key VARCHAR(128) COMMENT '对象存储 AccessKey',
  storage_secret_key VARCHAR(128) COMMENT '对象存储 SecretKey',
  storage_base_url VARCHAR(255) COMMENT '对象存储基础访问 URL',
  default_cover_provider VARCHAR(64) DEFAULT NULL COMMENT '默认智能找图服务',
  date_format VARCHAR(32) NOT NULL DEFAULT 'YYYY-MM-DD' COMMENT '日期展示格式',
  auto_backup_enabled TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否启用自动备份',
  auto_backup_time VARCHAR(5) NOT NULL DEFAULT '02:00' COMMENT '自动备份时间 HH:mm',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) COMMENT='系统设置表';

CREATE TABLE IF NOT EXISTS dict_category (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
  name VARCHAR(100) NOT NULL COMMENT '类别名称',
  parent_id BIGINT DEFAULT NULL COMMENT '父级ID',
  level INT NOT NULL DEFAULT 1 COMMENT '层级',
  sort INT NOT NULL DEFAULT 0 COMMENT '排序值',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  UNIQUE KEY uk_category_parent_name (parent_id, name),
  INDEX idx_category_parent (parent_id),
  CONSTRAINT fk_category_parent FOREIGN KEY (parent_id) REFERENCES dict_category(id) ON DELETE SET NULL
) COMMENT='类别字典（树形）';

CREATE TABLE IF NOT EXISTS dict_platform (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
  name VARCHAR(100) NOT NULL COMMENT '平台名称',
  link VARCHAR(255) COMMENT '平台链接',
  sort INT NOT NULL DEFAULT 0 COMMENT '排序值',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  UNIQUE KEY uk_platform_name (name)
) COMMENT='平台字典';

CREATE TABLE IF NOT EXISTS dict_tag (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
  name VARCHAR(100) NOT NULL COMMENT '标签名称',
  parent_id BIGINT DEFAULT NULL COMMENT '父级ID',
  color VARCHAR(16) COMMENT '颜色值',
  icon VARCHAR(64) COMMENT '图标',
  sort INT NOT NULL DEFAULT 0 COMMENT '排序值',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  UNIQUE KEY uk_tag_parent_name (parent_id, name),
  INDEX idx_tag_parent (parent_id),
  CONSTRAINT fk_tag_parent FOREIGN KEY (parent_id) REFERENCES dict_tag(id) ON DELETE SET NULL
) COMMENT='标签字典（树形）';

CREATE TABLE IF NOT EXISTS dict_brand (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
  name VARCHAR(100) NOT NULL COMMENT '品牌名称',
  alias VARCHAR(100) COMMENT '别名',
  initial VARCHAR(10) COMMENT '首字母',
  sort INT NOT NULL DEFAULT 0 COMMENT '排序值',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  UNIQUE KEY uk_brand_name (name)
) COMMENT='品牌字典';

CREATE TABLE IF NOT EXISTS device_asset (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
  name VARCHAR(200) NOT NULL COMMENT '物品名称',
  category_id BIGINT COMMENT '类别ID',
  category_path VARCHAR(500) COMMENT '类别路径（/1/12/118）',
  brand_id BIGINT COMMENT '品牌ID',
  brand VARCHAR(100) COMMENT '品牌名称',
  model VARCHAR(200) COMMENT '型号',
  serial_no VARCHAR(200) COMMENT '序列号',
  status ENUM('使用中','已闲置','待出售','已出售','已丢弃') NOT NULL DEFAULT '使用中' COMMENT '状态',
  purchase_id BIGINT COMMENT '关联采购记录ID',
  purchase_date DATE COMMENT '采购日期',
  retired_date DATE COMMENT '退役日期',
  cover_image_url VARCHAR(500) COMMENT '封面图片地址',
  notes TEXT COMMENT '备注',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  INDEX idx_status (status),
  INDEX idx_category (category_id),
  INDEX idx_category_path (category_path(191)),
  INDEX idx_brand (brand_id),
  CONSTRAINT fk_asset_category FOREIGN KEY (category_id) REFERENCES dict_category(id),
  CONSTRAINT fk_asset_brand FOREIGN KEY (brand_id) REFERENCES dict_brand(id)
) COMMENT='设备物品表';

CREATE TABLE IF NOT EXISTS purchase (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
  asset_id BIGINT NOT NULL COMMENT '关联资产ID',
  type ENUM('PRIMARY','ACCESSORY','SERVICE') NOT NULL COMMENT '采购类型',
  name VARCHAR(200) COMMENT '配件/服务名称',
  platform_id BIGINT COMMENT '采购平台ID',
  platform_name VARCHAR(100) COMMENT '采购平台名称',
  seller VARCHAR(200) COMMENT '卖家',
  price DECIMAL(12,2) NOT NULL COMMENT '采购金额',
  currency VARCHAR(10) DEFAULT 'CNY' COMMENT '货币类型',
  quantity INT DEFAULT 1 COMMENT '数量',
  shipping_cost DECIMAL(12,2) DEFAULT 0 COMMENT '运费',
  purchase_date DATE NOT NULL COMMENT '采购日期',
  invoice_no VARCHAR(100) COMMENT '发票编号',
  warranty_months INT COMMENT '保修月数',
  warranty_expire_date DATE COMMENT '保修到期日',
  attachments JSON COMMENT '附件（JSON）',
  product_link VARCHAR(5000) COMMENT '商品链接',
  notes TEXT COMMENT '备注',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  INDEX idx_purchase_asset (asset_id),
  CONSTRAINT fk_purchase_asset FOREIGN KEY (asset_id) REFERENCES device_asset (id),
  CONSTRAINT fk_purchase_platform FOREIGN KEY (platform_id) REFERENCES dict_platform(id)
) COMMENT='采购记录表';

CREATE TABLE IF NOT EXISTS sale (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
  asset_id BIGINT NOT NULL COMMENT '关联资产ID',
  sale_scope VARCHAR(20) NOT NULL DEFAULT 'ASSET' COMMENT '出售范围',
  purchase_id BIGINT COMMENT '关联购买记录ID',
  platform_id BIGINT COMMENT '出售平台ID',
  platform_name VARCHAR(100) COMMENT '出售平台名称',
  buyer VARCHAR(200) COMMENT '买家',
  sale_price DECIMAL(12,2) NOT NULL COMMENT '出售金额',
  fee DECIMAL(12,2) DEFAULT 0 COMMENT '手续费',
  shipping_cost DECIMAL(12,2) DEFAULT 0 COMMENT '运费',
  other_cost DECIMAL(12,2) DEFAULT 0 COMMENT '其他成本',
  net_income DECIMAL(12,2) COMMENT '净收入',
  sale_date DATE NOT NULL COMMENT '出售日期',
  attachments JSON COMMENT '附件（JSON）',
  notes TEXT COMMENT '备注',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  INDEX idx_sale_asset (asset_id),
  INDEX idx_sale_purchase (purchase_id),
  CONSTRAINT fk_sale_asset FOREIGN KEY (asset_id) REFERENCES device_asset (id),
  CONSTRAINT fk_sale_purchase FOREIGN KEY (purchase_id) REFERENCES purchase(id),
  CONSTRAINT fk_sale_platform FOREIGN KEY (platform_id) REFERENCES dict_platform(id)
) COMMENT='出售记录表';

CREATE TABLE IF NOT EXISTS asset_tag_map (
  asset_id BIGINT NOT NULL COMMENT '物品ID',
  tag_id BIGINT NOT NULL COMMENT '标签ID',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (asset_id, tag_id),
  INDEX idx_asset_tag_tag (tag_id),
  CONSTRAINT fk_asset_tag_asset FOREIGN KEY (asset_id) REFERENCES device_asset(id) ON DELETE CASCADE,
  CONSTRAINT fk_asset_tag_tag FOREIGN KEY (tag_id) REFERENCES dict_tag(id) ON DELETE CASCADE
) COMMENT='物品标签映射表';

CREATE TABLE IF NOT EXISTS wishlist (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
  name VARCHAR(200) NOT NULL COMMENT '心愿单名称',
  category_id BIGINT COMMENT '目标类别ID',
  brand_id BIGINT COMMENT '目标品牌ID',
  model VARCHAR(200) COMMENT '期望型号',
  expected_price DECIMAL(12,2) COMMENT '期望价格',
  current_price DECIMAL(12,2) COMMENT '最近观测价格',
  last_price_at DATETIME COMMENT '最近价格采集时间',
  image_url VARCHAR(500) COMMENT '商品图片',
  status ENUM('未购买','已购买') NOT NULL DEFAULT '未购买' COMMENT '购买状态',
  link VARCHAR(500) COMMENT '参考链接',
  notes TEXT COMMENT '备注',
  priority TINYINT NOT NULL DEFAULT 3 COMMENT '优先级',
  converted_asset_id BIGINT COMMENT '已转化资产ID',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  INDEX idx_wishlist_priority (priority),
  INDEX idx_wishlist_status (status),
  CONSTRAINT fk_wishlist_category FOREIGN KEY (category_id) REFERENCES dict_category(id),
  CONSTRAINT fk_wishlist_brand FOREIGN KEY (brand_id) REFERENCES dict_brand(id),
  CONSTRAINT fk_wishlist_asset FOREIGN KEY (converted_asset_id) REFERENCES device_asset(id)
) COMMENT='心愿单记录表';

CREATE TABLE IF NOT EXISTS wishlist_price_history (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  wishlist_id BIGINT NOT NULL,
  price DECIMAL(12,2) NOT NULL,
  captured_at DATETIME NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_wishlist_price_time (wishlist_id, captured_at),
  CONSTRAINT fk_wishlist_price_wishlist FOREIGN KEY (wishlist_id) REFERENCES wishlist(id) ON DELETE CASCADE
) COMMENT='心愿价格观测历史';

CREATE TABLE IF NOT EXISTS wishlist_tag_map (
  wishlist_id BIGINT NOT NULL COMMENT '心愿ID',
  tag_id BIGINT NOT NULL COMMENT '标签ID',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (wishlist_id, tag_id),
  INDEX idx_wishlist_tag_tag (tag_id),
  CONSTRAINT fk_wishlist_tag_wishlist FOREIGN KEY (wishlist_id) REFERENCES wishlist(id) ON DELETE CASCADE,
  CONSTRAINT fk_wishlist_tag_tag FOREIGN KEY (tag_id) REFERENCES dict_tag(id) ON DELETE CASCADE
) COMMENT='心愿标签映射表';

CREATE TABLE IF NOT EXISTS file_attachment (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
  biz_type VARCHAR(50) DEFAULT NULL COMMENT '业务类型',
  biz_id BIGINT DEFAULT NULL COMMENT '业务主键ID',
  object_key VARCHAR(500) NOT NULL COMMENT '对象存储相对路径',
  file_name VARCHAR(255) DEFAULT NULL COMMENT '原始文件名',
  file_type VARCHAR(100) DEFAULT NULL COMMENT '文件类型',
  file_size BIGINT DEFAULT NULL COMMENT '文件大小（字节）',
  extra JSON DEFAULT NULL COMMENT '扩展字段（JSON）',
  is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '是否已删除',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  INDEX idx_attachment_biz (biz_type, biz_id),
  INDEX idx_attachment_object (object_key)
) COMMENT='统一附件表';

-- 装备升级路线主表
CREATE TABLE IF NOT EXISTS equip_upgrade_route (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
  name VARCHAR(200) NOT NULL COMMENT '路线名称',
  root_asset_id BIGINT DEFAULT NULL COMMENT '起点资产ID',
  remark TEXT COMMENT '备注',
  plan_year INT COMMENT '计划年份',
  annual_budget DECIMAL(12,2) COMMENT '年度预算',
  is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
  -- ,CONSTRAINT fk_upgrade_route_asset FOREIGN KEY (root_asset_id) REFERENCES device_asset(id)
) COMMENT='装备升级路线表';

-- 升级节点表
CREATE TABLE IF NOT EXISTS equip_upgrade_node (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
  route_id BIGINT NOT NULL COMMENT '所属路线ID',
  asset_id BIGINT DEFAULT NULL COMMENT '关联资产ID；纯计划节点可为空',
  level INT DEFAULT 1 COMMENT '层级',
  sort INT DEFAULT 0 COMMENT '排序',
  label VARCHAR(200) DEFAULT NULL COMMENT '节点标签',
  remark TEXT COMMENT '备注',
  title VARCHAR(200) COMMENT '计划标题',
  target_name VARCHAR(200) COMMENT '目标物品名称',
  period_label VARCHAR(50) COMMENT '计划周期标签',
  planned_budget DECIMAL(12,2) COMMENT '计划预算',
  expected_recovery DECIMAL(12,2) COMMENT '预计回收',
  status VARCHAR(20) NOT NULL DEFAULT 'PLANNED' COMMENT '计划状态',
  is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  INDEX idx_upgrade_node_route (route_id),
  INDEX idx_upgrade_node_asset (asset_id),
  CONSTRAINT fk_upgrade_node_route FOREIGN KEY (route_id) REFERENCES equip_upgrade_route(id) ON DELETE CASCADE
  -- ,CONSTRAINT fk_upgrade_node_asset FOREIGN KEY (asset_id) REFERENCES device_asset(id)
) COMMENT='装备升级节点表';

-- 升级关系表
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

CREATE TABLE IF NOT EXISTS dashboard_monthly_snapshot (
  snapshot_month CHAR(7) PRIMARY KEY COMMENT '月份 YYYY-MM',
  total_value DECIMAL(14,2) NOT NULL DEFAULT 0,
  avg_daily_cost DECIMAL(14,2) NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) COMMENT='总览按月快照';
