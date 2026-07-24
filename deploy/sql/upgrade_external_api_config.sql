-- 外接 API 通用配置表：保存私有图库、价格服务等连接信息。
CREATE TABLE IF NOT EXISTS external_api_config (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
  api_code VARCHAR(64) NOT NULL COMMENT '连接器编码，如 MT_PHOTOS',
  display_name VARCHAR(100) NOT NULL COMMENT '显示名称',
  base_url VARCHAR(500) NOT NULL COMMENT '服务基地址',
  auth_type VARCHAR(32) NOT NULL DEFAULT 'API_KEY' COMMENT '鉴权方式',
  credential_secret TEXT NULL COMMENT '后端凭据，接口不会返回明文',
  config_json TEXT NULL COMMENT '连接器扩展配置 JSON',
  timeout_ms INT NOT NULL DEFAULT 15000 COMMENT '请求超时毫秒数',
  enabled TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否启用',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_external_api_config_code (api_code)
) COMMENT='外接 API 连接配置表';
