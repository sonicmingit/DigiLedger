-- 外接服务与搜图偏好的最终初始化数据。
-- 凭据为空、默认禁用；在系统设置中保存并启用后才会参与运行。
-- 此迁移仅保留当前有效的连接器，不包含已废弃的 Bing / 百度试验连接器。
-- MySQL 5.7 不支持 ADD COLUMN IF NOT EXISTS；使用 information_schema 兼容新库与
-- 已提前创建该字段的旧库，避免重复列错误。
SET @image_search_provider_column_exists = (
  SELECT COUNT(*)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'sys_setting'
    AND column_name = 'image_search_provider_codes'
);
SET @image_search_provider_column_sql = IF(
  @image_search_provider_column_exists = 0,
  'ALTER TABLE sys_setting ADD COLUMN image_search_provider_codes JSON NULL COMMENT ''启用的搜图服务编码列表''',
  'SELECT 1'
);
PREPARE image_search_provider_column_statement FROM @image_search_provider_column_sql;
EXECUTE image_search_provider_column_statement;
DEALLOCATE PREPARE image_search_provider_column_statement;

INSERT IGNORE INTO external_api_config
  (api_code, display_name, base_url, auth_type, credential_secret, config_json, timeout_ms, enabled)
VALUES
  ('MT_PHOTOS', 'MT Photos 图库', 'https://mtmt.tech', 'API_KEY', NULL, NULL, 15000, 0),
  ('JD_UNION_PRODUCT_SEARCH', '京东联盟商品搜索', 'https://api.jd.com/routerjson', 'API_KEY', NULL, '{"appKey":"","unionId":""}', 15000, 0),
  ('TAOBAO_UNION_PRODUCT_SEARCH', '淘宝联盟商品搜索', 'https://eco.taobao.com/router/rest', 'API_KEY', NULL, '{"appKey":"","adzoneId":""}', 15000, 0),
  ('ICECAT_PRODUCT_CATALOG', 'Icecat 产品目录', 'https://live.icecat.biz/api', 'API_KEY', NULL, '{"language":"zh"}', 15000, 0),
  ('BRAVE_IMAGE_SEARCH', 'Brave 图片搜索', 'https://api.search.brave.com/res/v1/images/search', 'API_KEY', NULL, '{"country":"CN","searchLang":"zh-hans","safeSearch":"strict"}', 15000, 0),
  ('SERPAPI_GOOGLE_IMAGES', 'SerpApi Google 商品图片搜索', 'https://serpapi.com/search.json', 'API_KEY', NULL, '{"domains":["jd.com","taobao.com","tmall.com"],"gl":"cn","hl":"zh-CN"}', 30000, 0),
  ('REMOVE_BG', 'remove.bg 智能抠图', 'https://api.remove.bg/v1.0/removebg', 'API_KEY', NULL, NULL, 120000, 0);
