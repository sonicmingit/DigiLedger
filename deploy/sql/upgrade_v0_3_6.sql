-- DigiLedger V0.3.6 升级脚本：统一附件管理
-- 1. 新增统一附件表
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

-- 2. 示例迁移：将旧 URL 转换为 objectKey 写入附件表
--    注意：请在执行前确认 `storage_base_url`、桶名称与历史 URL 规则保持一致。
--    以下语句假设原字段内存储 `/oss/{bucket}/` 或完整 URL。
INSERT INTO file_attachment (biz_type, biz_id, object_key, file_name, created_at, updated_at)
SELECT 'ASSET', a.id, REPLACE(REPLACE(a.cover_image_url, CONCAT('/oss/', s.storage_bucket, '/'), ''), CONCAT(COALESCE(s.storage_base_url, ''), CASE WHEN s.storage_base_url IS NULL OR s.storage_base_url = '' THEN '' ELSE '/' END), '') AS object_key,
       NULL AS file_name, NOW(), NOW()
FROM device_asset a
CROSS JOIN sys_setting s
WHERE a.cover_image_url IS NOT NULL AND a.cover_image_url <> '';

-- 需要mysql版本支持
INSERT INTO file_attachment (biz_type, biz_id, object_key, file_name, created_at, updated_at)
SELECT 'PURCHASE', p.id, JSON_UNQUOTE(JSON_EXTRACT(j.value, '$')) AS object_key,
       NULL, NOW(), NOW()
FROM purchase p
CROSS JOIN sys_setting s
CROSS JOIN JSON_TABLE(p.attachments, '$[*]' COLUMNS (value VARCHAR(500) PATH '$')) AS j
WHERE p.attachments IS NOT NULL;

INSERT INTO file_attachment (biz_type, biz_id, object_key, file_name, created_at, updated_at)
SELECT 'SALE', s.id, JSON_UNQUOTE(JSON_EXTRACT(j.value, '$')) AS object_key,
       NULL, NOW(), NOW()
FROM sale s
CROSS JOIN sys_setting cfg
CROSS JOIN JSON_TABLE(s.attachments, '$[*]' COLUMNS (value VARCHAR(500) PATH '$')) AS j
WHERE s.attachments IS NOT NULL;

INSERT INTO file_attachment (biz_type, biz_id, object_key, file_name, created_at, updated_at)
SELECT 'WISHLIST', w.id, REPLACE(REPLACE(w.image_url, CONCAT('/oss/', cfg.storage_bucket, '/'), ''), CONCAT(COALESCE(cfg.storage_base_url, ''), CASE WHEN cfg.storage_base_url IS NULL OR cfg.storage_base_url = '' THEN '' ELSE '/' END), '') AS object_key,
       NULL, NOW(), NOW()
FROM wishlist w
CROSS JOIN sys_setting cfg
WHERE w.image_url IS NOT NULL AND w.image_url <> '';

-- 3. 迁移完成后，可视情况将原有 URL 字段标记为废弃或保留兼容。
