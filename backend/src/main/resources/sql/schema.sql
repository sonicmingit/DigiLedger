-- DigiLedger schema initialization（对齐设计文档 V0.3）
CREATE TABLE IF NOT EXISTS sys_setting (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  currency VARCHAR(10) NOT NULL DEFAULT 'CNY',
  storage_provider VARCHAR(32) NOT NULL DEFAULT 'minio',
  storage_endpoint VARCHAR(255),
  storage_region VARCHAR(64),
  storage_bucket VARCHAR(128) DEFAULT 'digiledger',
  storage_access_key VARCHAR(128),
  storage_secret_key VARCHAR(128),
  storage_base_url VARCHAR(255),
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS device_asset (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(200) NOT NULL,
  category VARCHAR(100) NOT NULL,
  brand VARCHAR(100),
  model VARCHAR(200),
  serial_no VARCHAR(200),
  status ENUM('使用中','已闲置','待出售','已出售','已丢弃') NOT NULL DEFAULT '使用中',
  purchase_id BIGINT,
  purchase_date DATE,
  enabled_date DATE NOT NULL,
  retired_date DATE,
  tags JSON,
  cover_image_url VARCHAR(500),
  notes TEXT,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_status (status),
  INDEX idx_cat_brand (category, brand)
);

CREATE TABLE IF NOT EXISTS purchase (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  asset_id BIGINT NOT NULL,
  type ENUM('PRIMARY','ACCESSORY','SERVICE') NOT NULL,
  platform VARCHAR(100),
  seller VARCHAR(200),
  price DECIMAL(12,2) NOT NULL,
  currency VARCHAR(10) DEFAULT 'CNY',
  quantity INT DEFAULT 1,
  shipping_cost DECIMAL(12,2) DEFAULT 0,
  purchase_date DATE NOT NULL,
  invoice_no VARCHAR(100),
  warranty_months INT,
  warranty_expire_date DATE,
  attachments JSON,
  notes TEXT,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_purchase_asset (asset_id),
  CONSTRAINT fk_purchase_asset FOREIGN KEY (asset_id) REFERENCES device_asset (id)
);

CREATE TABLE IF NOT EXISTS sale (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  asset_id BIGINT NOT NULL,
  platform VARCHAR(100),
  buyer VARCHAR(200),
  sale_price DECIMAL(12,2) NOT NULL,
  fee DECIMAL(12,2) DEFAULT 0,
  shipping_cost DECIMAL(12,2) DEFAULT 0,
  other_cost DECIMAL(12,2) DEFAULT 0,
  net_income DECIMAL(12,2),
  sale_date DATE NOT NULL,
  attachments JSON,
  notes TEXT,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_sale_asset (asset_id),
  CONSTRAINT fk_sale_asset FOREIGN KEY (asset_id) REFERENCES device_asset (id)
);

CREATE TABLE IF NOT EXISTS wishlist (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(200) NOT NULL,
  category VARCHAR(100),
  brand VARCHAR(100),
  model VARCHAR(200),
  expected_price DECIMAL(12,2),
  planned_platform VARCHAR(100),
  link VARCHAR(500),
  notes TEXT,
  priority TINYINT DEFAULT 3,
  converted_asset_id BIGINT,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_wishlist_priority (priority)
);
