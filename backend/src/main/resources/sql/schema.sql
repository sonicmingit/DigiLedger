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
  enabled_date DATE NOT NULL COMMENT '启用日期',
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
  CONSTRAINT fk_sale_asset FOREIGN KEY (asset_id) REFERENCES device_asset (id),
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
  category_id BIGINT COMMENT '期望分类ID',
  brand_id BIGINT COMMENT '期望品牌ID',
  model VARCHAR(200) COMMENT '期望型号',
  expected_price DECIMAL(12,2) COMMENT '期望价格',
  image_url VARCHAR(500) COMMENT '商品图片',
  status ENUM('未购买','已购买') NOT NULL DEFAULT '未购买' COMMENT '购买状态',
  link VARCHAR(500) COMMENT '参考链接',
  notes TEXT COMMENT '备注',
  priority TINYINT NOT NULL DEFAULT 3 COMMENT '优先级（1-5）',
  converted_asset_id BIGINT COMMENT '已转化资产ID',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  INDEX idx_wishlist_priority (priority),
  INDEX idx_wishlist_status (status),
  CONSTRAINT fk_wishlist_category FOREIGN KEY (category_id) REFERENCES dict_category(id),
  CONSTRAINT fk_wishlist_brand FOREIGN KEY (brand_id) REFERENCES dict_brand(id),
  CONSTRAINT fk_wishlist_asset FOREIGN KEY (converted_asset_id) REFERENCES device_asset(id)
) COMMENT='心愿单记录表';
