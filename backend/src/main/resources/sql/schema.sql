-- DigiLedger schema initialization（对齐设计文档 V0.3）
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

CREATE TABLE IF NOT EXISTS device_asset (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
  name VARCHAR(200) NOT NULL COMMENT '资产名称',
  category VARCHAR(100) NOT NULL COMMENT '资产分类',
  brand VARCHAR(100) COMMENT '品牌',
  model VARCHAR(200) COMMENT '型号',
  serial_no VARCHAR(200) COMMENT '序列号',
  status ENUM('使用中','已闲置','待出售','已出售','已丢弃') NOT NULL DEFAULT '使用中' COMMENT '资产状态',
  purchase_id BIGINT COMMENT '关联采购记录ID',
  purchase_date DATE COMMENT '采购日期',
  enabled_date DATE NOT NULL COMMENT '启用日期',
  retired_date DATE COMMENT '退役日期',
  tags JSON COMMENT '标签（JSON）',
  cover_image_url VARCHAR(500) COMMENT '封面图片地址',
  notes TEXT COMMENT '备注',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  INDEX idx_status (status),
  INDEX idx_cat_brand (category, brand)
) COMMENT='设备资产表';

CREATE TABLE IF NOT EXISTS purchase (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
  asset_id BIGINT NOT NULL COMMENT '关联资产ID',
  type ENUM('PRIMARY','ACCESSORY','SERVICE') NOT NULL COMMENT '采购类型',
  platform VARCHAR(100) COMMENT '采购平台',
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
  CONSTRAINT fk_purchase_asset FOREIGN KEY (asset_id) REFERENCES device_asset (id)
) COMMENT='采购记录表';

CREATE TABLE IF NOT EXISTS sale (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
  asset_id BIGINT NOT NULL COMMENT '关联资产ID',
  platform VARCHAR(100) COMMENT '出售平台',
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
  CONSTRAINT fk_sale_asset FOREIGN KEY (asset_id) REFERENCES device_asset (id)
) COMMENT='出售记录表';

CREATE TABLE IF NOT EXISTS wishlist (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
  name VARCHAR(200) NOT NULL COMMENT '心愿单名称',
  category VARCHAR(100) COMMENT '期望分类',
  brand VARCHAR(100) COMMENT '期望品牌',
  model VARCHAR(200) COMMENT '期望型号',
  expected_price DECIMAL(12,2) COMMENT '期望价格',
  planned_platform VARCHAR(100) COMMENT '计划购买平台',
  link VARCHAR(500) COMMENT '参考链接',
  notes TEXT COMMENT '备注',
  priority TINYINT DEFAULT 3 COMMENT '优先级',
  converted_asset_id BIGINT COMMENT '已转化资产ID',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  INDEX idx_wishlist_priority (priority)
) COMMENT='心愿单记录表';
