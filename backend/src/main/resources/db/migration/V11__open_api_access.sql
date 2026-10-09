CREATE TABLE open_api_access (
    id TINYINT PRIMARY KEY,
    enabled BOOLEAN NOT NULL DEFAULT FALSE,
    allow_write BOOLEAN NOT NULL DEFAULT FALSE,
    token_hash CHAR(64) NULL,
    token_prefix VARCHAR(16) NULL,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) COMMENT='Agent 对外 API 访问配置，仅存储令牌摘要';

INSERT INTO open_api_access (id, enabled, allow_write) VALUES (1, FALSE, FALSE);
