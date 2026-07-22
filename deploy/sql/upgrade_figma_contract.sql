-- Figma 重构后端契约兼容迁移（MySQL 8.3）。所有新增业务字段均允许旧数据为空。
-- 版本前置条件：仅对尚未应用本迁移的既有 DigiLedger 数据库执行一次；不要重复执行。
ALTER TABLE wishlist
  ADD COLUMN current_price DECIMAL(12,2) NULL COMMENT '最近观测价格' AFTER expected_price,
  ADD COLUMN last_price_at DATETIME NULL COMMENT '最近价格采集时间' AFTER current_price;

CREATE TABLE IF NOT EXISTS wishlist_price_history (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  wishlist_id BIGINT NOT NULL,
  price DECIMAL(12,2) NOT NULL,
  captured_at DATETIME NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_wishlist_price_time (wishlist_id, captured_at),
  CONSTRAINT fk_wishlist_price_wishlist FOREIGN KEY (wishlist_id) REFERENCES wishlist(id) ON DELETE CASCADE
) COMMENT='心愿价格观测历史';

ALTER TABLE equip_upgrade_route
  ADD COLUMN plan_year INT NULL COMMENT '计划年份' AFTER remark,
  ADD COLUMN annual_budget DECIMAL(12,2) NULL COMMENT '年度预算' AFTER plan_year;

ALTER TABLE equip_upgrade_node
  MODIFY COLUMN asset_id BIGINT NULL COMMENT '关联资产ID；纯计划节点可为空',
  ADD COLUMN title VARCHAR(200) NULL COMMENT '计划标题' AFTER remark,
  ADD COLUMN target_name VARCHAR(200) NULL COMMENT '目标物品名称' AFTER title,
  ADD COLUMN period_label VARCHAR(50) NULL COMMENT '计划周期标签' AFTER target_name,
  ADD COLUMN planned_budget DECIMAL(12,2) NULL COMMENT '计划预算' AFTER period_label,
  ADD COLUMN expected_recovery DECIMAL(12,2) NULL COMMENT '预计回收' AFTER planned_budget,
  ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'PLANNED' COMMENT '计划状态' AFTER expected_recovery;

ALTER TABLE sys_setting
  ADD COLUMN date_format VARCHAR(32) NOT NULL DEFAULT 'YYYY-MM-DD' AFTER currency,
  ADD COLUMN auto_backup_enabled TINYINT(1) NOT NULL DEFAULT 0 AFTER date_format,
  ADD COLUMN auto_backup_time VARCHAR(5) NOT NULL DEFAULT '02:00' AFTER auto_backup_enabled;

CREATE TABLE IF NOT EXISTS dashboard_monthly_snapshot (
  snapshot_month CHAR(7) PRIMARY KEY COMMENT '月份 YYYY-MM',
  total_value DECIMAL(14,2) NOT NULL DEFAULT 0,
  avg_daily_cost DECIMAL(14,2) NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) COMMENT='总览按月快照';
