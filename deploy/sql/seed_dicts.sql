-- 常见数码产品类别 / 平台 / 标签 / 品牌 初始化数据
-- 仅用于开发与本地测试环境，请勿直接用于生产
/*常见数码类别：相机、镜头、配件、音频设备、电脑与平板、灯光、存储、稳定器等；
常见购物平台：京东、淘宝、天猫、拼多多、Apple 官网、小米商城、B&H、亚马逊中国；
通用标签：主力机、备机、工作用、日常记录、旅行、视频拍摄、直播、轻便、全画幅、APS‑C 等；
常见品牌：索尼、佳能、尼康、富士、大疆、苹果、小米、华为、适马、腾龙。*/
-- 类别（相机、镜头、配件等）
INSERT INTO dict_category (id, name, parent_id, level, sort)
VALUES
  (1, '相机', NULL, 1, 10),
  (2, '镜头', NULL, 1, 20),
  (3, '配件', NULL, 1, 30),
  (4, '音频设备', NULL, 1, 40),
  (5, '电脑与平板', NULL, 1, 50),
  (6, '摄影灯光', NULL, 1, 60),
  (7, '存储卡与硬盘', NULL, 1, 70),
  (8, '稳定器与支架', NULL, 1, 80);

-- 购物平台
INSERT INTO dict_platform (id, name, link, sort)
VALUES
  (1, '京东', 'https://www.jd.com', 10),
  (2, '淘宝', 'https://www.taobao.com', 20),
  (3, '天猫', 'https://www.tmall.com', 30),
  (4, '拼多多', 'https://www.pinduoduo.com', 40),
  (5, 'Apple 官网', 'https://www.apple.com/cn', 50),
  (6, '小米商城', 'https://www.mi.com', 60),
  (7, 'B&H', 'https://www.bhphotovideo.com', 70),
  (8, '亚马逊中国', 'https://www.amazon.cn', 80);

-- 标签（用途 / 特性）
INSERT INTO dict_tag (id, name, parent_id, color, icon, sort)
VALUES
  (1, '主力机', NULL, '#4ade80', NULL, 10),
  (2, '备机', NULL, '#a855f7', NULL, 20),
  (3, '工作用', NULL, '#0ea5e9', NULL, 30),
  (4, '日常记录', NULL, '#f97316', NULL, 40),
  (5, '旅行', NULL, '#22c55e', NULL, 50),
  (6, '视频拍摄', NULL, '#eab308', NULL, 60),
  (7, '直播', NULL, '#ec4899', NULL, 70),
  (8, '轻便', NULL, '#64748b', NULL, 80),
  (9, '全画幅', NULL, '#f97316', NULL, 90),
  (10, 'APS-C', NULL, '#38bdf8', NULL, 100);

-- 品牌
INSERT INTO dict_brand (id, name, alias, initial, sort)
VALUES
  (1, '索尼', 'Sony', 'S', 10),
  (2, '佳能', 'Canon', 'J', 20),
  (3, '尼康', 'Nikon', 'N', 30),
  (4, '富士', 'Fujifilm', 'F', 40),
  (5, '大疆', 'DJI', 'D', 50),
  (6, '苹果', 'Apple', 'A', 60),
  (7, '小米', 'Xiaomi', 'X', 70),
  (8, '华为', 'HUAWEI', 'H', 80),
  (9, '适马', 'Sigma', 'S', 90),
  (10, '腾龙', 'Tamron', 'T', 100);

