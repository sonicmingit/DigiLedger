# DigiLedger V0.3.8 产品设计

> 约定 & 范围
>
> - 均摊成本：**按天计算**。
> - 折旧：**不再计算折旧**（仅展示总投入、使用天数、日均成本）。
> - 架构：**前后端分离**。后端 `Spring Boot + MyBatis + MySQL`；前端 `Vue3 + Vite + Element Plus`。
> - 部署：**Docker/Compose 一键启动**（含 MySQL、后端、前端、Nginx 反代）。
> - 接口返回体：统一为 `{ code, data, msg }`，`code=200` 表示成功。
> - 版本：V0.3.8（心愿单关联跳转、出售成本指标、上传回显修复版）。

------

## 1. 目标与业务概述

### 0. 文案与术语规范（个人用途）

- 面向用户的**前端/界面文案**统一使用：**物品**（不使用“资产”等商业化用语）。
- 示例：物品名称、物品列表、物品状态、物品详情、物品图片。
- **后端/数据库命名**暂保持 `asset` 兼容，避免大范围重命名带来迁移成本；后续如需统一为 `item`，将提供数据迁移脚本与兼容层。

------

## 1. 目标与业务概述

### 1.1 产品目标

- 管理个人/家庭/小团队的**数码资产全生命周期**：购买 → 使用 → 维护/配件 → 转售/报废。
- 实时输出**成本均摊**（总投入、使用天数、日均成本）等简化指标。

### 1.2 关键业务要点

- **购买记录**：主购买 + 配件/服务扩展，支持附件（发票/截图）。
- **使用统计**：按天均摊；启用日期至当前/售出/报废。
- **折旧**：不计算折旧，仅展示总投入与日均成本。
- **转售**：记录售出平台、价格、费用、净收入；资产状态联动，支持附件与亏损/日月均使用价格快照。
- **统计/报表**：按类别、平台、年份聚合；仪表盘 KPI。
- **导入导出**：CSV 导入资产（含主购买）、列表导出。
- **心愿单详情**：展示已关联的资产列表，支持点击跳转物品详情，并在资产被删除时给出中文提示。

------

## 2. 术语与状态机

- 资产（Asset）：一台设备的主档（如：iPhone 14 Pro 256G）。
- 购买（Purchase）：资产的购买行为（主购买 / 配件 / 服务）。
- 使用（Usage）：按天均摊的前提下，可不强制记录小时级使用日志（后续再做）。
- 售出（Sale）：设备交易记录，记价格、费用、净收入、盈亏、平台等。
- 状态（中文）：`使用中`、`已闲置`、`待出售`、`已出售`、`已丢弃`、`心愿`（心愿为单独列表）。

**状态机（资产）**：

- `使用中` ↔ `已闲置` ↔ `待出售` → `已出售`
- 任意 → `已丢弃`
- `已出售/已丢弃` 支持撤销（保留审计日志）。

**心愿单**：独立列表，不参与上述状态流转；心愿条目可一键“转为资产”。

心愿单列表新增“详情”抽屉；**新增/编辑**不再直接修改 `status`，`status` 仅通过“**已购买**”转换产生。

------

## 3. 成本与均摊模型（无折旧）

### 3.1 总投入

```
总投入 total_invest = Σ( purchase.price + purchase.shipping_cost )
  其中 purchase.type ∈ { PRIMARY, ACCESSORY, SERVICE }
```

### 3.2 使用天数（按天法）

```
use_days =
  若状态 ∈ {使用中, 已闲置, 待出售}: days_between(enabled_date, today)
  若状态 = 已出售:                 days_between(enabled_date, sale_date)
  若状态 = 已丢弃:                 days_between(enabled_date, retired_date)
```

> `days_between(a,b)` 为自然日差，含首不含尾；最低为 1，避免除零。

> **说明**：售出后的物品全部以对应售出记录的 `sale_date` 作为截止时间，形成“使用天数快照”，不再使用当前日期。

### 3.3 平均日成本（按天均摊）

```
avg_cost_per_day = total_invest / max(use_days, 1)
```

### 3.4 售出使用成本指标

- **亏损价格 loss_amount** = 购买投入（含运费） - 售出价格；正值代表亏损，负值表示收益。
- **日均使用价格 daily_usage_cost** = loss_amount ÷ use_days（使用天数为 0 时强制为 0）。
- **月均使用价格 monthly_usage_cost** = loss_amount ÷ (use_days ÷ 30)，以 30 天折算 1 个月；若使用天数不足 1 天则同样返回 0。
- 以上指标均在售出时即时计算，并与该条售出记录一并返回给前端展示，作为固定快照。

------

## 4. 数据模型（MySQL 8）

### 4.1 表结构（DDL）

```sql
CREATE TABLE sys_setting (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  currency  VARCHAR(10) NOT NULL DEFAULT 'CNY',
  storage_provider VARCHAR(32) NOT NULL DEFAULT 'minio',
  storage_endpoint VARCHAR(255),
  storage_region   VARCHAR(64),
  storage_bucket   VARCHAR(128) DEFAULT 'digiledger',
  storage_access_key VARCHAR(128),
  storage_secret_key VARCHAR(128),
  storage_base_url VARCHAR(255),
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE device_asset (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(200) NOT NULL,
  category_id BIGINT,
  category_path VARCHAR(500),
  brand_id BIGINT,
  brand VARCHAR(100),
  model VARCHAR(200),
  serial_no VARCHAR(200),
  status ENUM('使用中','已闲置','待出售','已出售','已丢弃') NOT NULL DEFAULT '使用中',
  purchase_id BIGINT,
  purchase_date DATE,
  enabled_date DATE, -- UI 不展示，后端默认= purchase_date
  retired_date DATE,
  cover_image_url VARCHAR(500),
  notes TEXT,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_status (status),
  INDEX idx_cat (category_id),
  INDEX idx_brand (brand_id)
);

CREATE TABLE purchase (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  asset_id BIGINT NOT NULL,
  type ENUM('PRIMARY','ACCESSORY','SERVICE') NOT NULL,
  name VARCHAR(200), -- 新增：配件/服务名称
  platform_id BIGINT,
  platform_name VARCHAR(100),
  seller VARCHAR(200),
  price DECIMAL(12,2) NOT NULL,
  currency VARCHAR(10) DEFAULT 'CNY',
  quantity INT DEFAULT 1,
  shipping_cost DECIMAL(12,2) DEFAULT 0,
  purchase_date DATE NOT NULL,
  invoice_no VARCHAR(100),
  warranty_months INT,
  warranty_expire_date DATE,
  product_link VARCHAR(1000), -- V0.3.8 新增：外部商品链接
  attachments JSON,
  notes TEXT,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_asset (asset_id),
  CONSTRAINT fk_purchase_asset FOREIGN KEY (asset_id) REFERENCES device_asset(id)
);

CREATE TABLE sale (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  asset_id BIGINT NOT NULL,
  platform_id BIGINT,
  platform_name VARCHAR(100),
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
  INDEX idx_asset (asset_id),
  CONSTRAINT fk_sale_asset FOREIGN KEY (asset_id) REFERENCES device_asset(id)
);

-- 售出响应 DTO 额外包含：use_days、loss_amount、daily_usage_cost、monthly_usage_cost，用于前端展示，不额外持久化。

CREATE TABLE op_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  biz_type VARCHAR(50) NOT NULL,
  biz_id BIGINT,
  action VARCHAR(50) NOT NULL,
  content JSON,
  operator VARCHAR(100) DEFAULT 'system',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_biz (biz_type, biz_id)
);

CREATE TABLE dict_category (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(100) NOT NULL,
  parent_id BIGINT DEFAULT NULL,
  level INT DEFAULT 1,
  sort INT DEFAULT 0,
  UNIQUE KEY uk_cat_parent_name (parent_id, name)
);

CREATE TABLE dict_platform (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(100) UNIQUE NOT NULL,
  link VARCHAR(255),
  sort INT DEFAULT 0
);

CREATE TABLE dict_tag (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(100) NOT NULL,
  parent_id BIGINT DEFAULT NULL,
  color VARCHAR(16),
  icon VARCHAR(64),
  sort INT DEFAULT 0,
  UNIQUE KEY uk_tag_parent_name (parent_id, name)
);

CREATE TABLE dict_brand (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(100) UNIQUE NOT NULL,
  alias VARCHAR(200),
  initial CHAR(1),
  sort INT DEFAULT 0
);

CREATE TABLE asset_tag_map (
  asset_id BIGINT NOT NULL,
  tag_id BIGINT NOT NULL,
  PRIMARY KEY(asset_id, tag_id),
  INDEX idx_tag (tag_id)
);
```

### 4.2 约束与说明（更新）

- 类别选择：支持 **父级或叶子**；`category_path` 冗余整条路径用于快速过滤。
- 品牌：引入 `dict_brand` 字典；前端可下拉新建。
- 心愿单与物品共享类目/品牌/标签逻辑。

### 4.3 心愿单（独立表）

```sql
CREATE TABLE wishlist (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(200) NOT NULL,
  category_id BIGINT,
  brand_id BIGINT,
  model VARCHAR(200),
  expected_price DECIMAL(12,2),
  image_url VARCHAR(500),
  link VARCHAR(500),
  status ENUM('未购买','已购买') NOT NULL DEFAULT '未购买',
  notes TEXT,
  converted_asset_id BIGINT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
CREATE TABLE wishlist (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(200) NOT NULL,
  category VARCHAR(100),
  brand VARCHAR(100),
  model VARCHAR(200),
  expected_price DECIMAL(12,2),
  planned_platform VARCHAR(100),
  link VARCHAR(500),
  notes TEXT,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  converted_asset_id BIGINT NULL  -- 若已购买可关联生成的资产ID
);
```

### 4.4 字典/配置表

```sql
CREATE TABLE dict_category (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(100) UNIQUE NOT NULL,
  sort INT DEFAULT 0
);
CREATE TABLE dict_platform (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(100) UNIQUE NOT NULL,
  link VARCHAR(255),
  sort INT DEFAULT 0
);
```

------

## 5. API 设计（REST）

### 5.0 文案对齐

- 接口资源名仍使用 `assets`，但前端文案展示为 **物品**。

### 5.1 系统设置与字典

- `GET /api/settings`、`PUT /api/settings`（对象存储/货币等）
- **类别（多级）**
  - `GET /api/dict/categories/tree`：树形
  - `POST /api/dict/categories`：新增 `{ name, parent_id? }`
  - `PUT /api/dict/categories/{id}`、`DELETE /api/dict/categories/{id}`
- **平台（单级）**
  - `GET /api/dict/platforms`
  - `POST /api/dict/platforms` `{ name, link?, sort? }`
  - `PUT /api/dict/platforms/{id}`、`DELETE /api/dict/platforms/{id}`
- **标签（多级）**
  - `GET /api/dict/tags/tree`
  - `POST /api/dict/tags` `{ name, parent_id?, color?, icon?, sort? }`
  - `PUT /api/dict/tags/{id}`、`DELETE /api/dict/tags/{id}`
- **品牌（单级）**
  - `GET /api/dict/brands`
  - `POST /api/dict/brands` `{ name, alias?, initial?, sort? }`
  - `PUT /api/dict/brands/{id}`、`DELETE /api/dict/brands/{id}`

> 物品创建/编辑使用：`category_id`（父或叶）、`platform_id`、`brand_id`、`tag_ids[]`。查询支持 `category_id`（含子级）、`platform_id`、`tag_ids[]` 过滤；**列表字段“购买时间”替代“启用日期”。**

- `POST /api/files/upload` （multipart）→ `{ url, objectKey }`

### 5.2 文件上传（对象存储）

- `POST /api/files/upload` （multipart）→ 返回 `{ url, objectKey }`
- **URL 为相对路径**（例如：`/oss/digiledger/xxx.png`），前端以网关基址拼接为可访问地址；推荐仅在数据库持久化 `objectKey`，避免后期域名/网关迁移带来的批量改动。
- `DELETE /api/files/{objectKey}` → 删除对象（可选）

> 说明：后端适配 MinIO / S3 兼容协议；相对 URL 的前缀与桶映射由网关/Nginx 统一转发。

### 5.3 物品（assets）

- `POST /api/assets`
- `GET /api/assets`：支持 `category_id / platform_id / tag_ids[] / status / q / page / size / view`
  - `view=list|card`（列表或卡片视图提示）
- `GET /api/assets/{id}`（含 metrics + 标签 + 类别路径）
- `PUT /api/assets/{id}`、`PATCH /api/assets/{id}/status`（**支持行内状态修改**）、`DELETE /api/assets/{id}`

### 5.4 购买记录（purchases）

- 同前；新增字段 `name`（配件/服务名称）。
- 详情页支持编辑与删除购买记录。
- 详情页提供“新增附属品（配件/服务）”快捷入口 → 创建 `type=ACCESSORY|SERVICE` 的 purchase。
- 同前（平台改为 `platform_id`，后端自动冗余 `platform_name`）

### 5.5 售出记录（sales & sell wizard）

- 平台改为 `platform_id`，后端自动冗余 `platform_name`。
- **新增售出范围**：`sale_scope = ASSET | ACCESSORY`
  - `ASSET`：出售主商品，创建 `sale` 后将资产状态流转为**已出售**。
  - `ACCESSORY`：出售配件，需传 `purchase_id` 指向对应配件购买记录；**不**改变资产状态，仅记录收益与附件。
- 便捷接口（向导）：`POST /api/assets/{id}/sell` 支持 `sale_scope` 和 `purchase_id`。


### 5.6 心愿单（wishlist）

- 同前

### 5.7 统计（metrics）

- 同前

### 5.8 错误码（见前）

### 5.0 返回体与通用规范

- 成功：`{ code: 200, data: <payload>, msg: 'success' }`
- 失败：`{ code: <4xx/5xx>, data: null, msg: '<错误描述>' }`
- 分页：`page`（1 起）、`size`；返回 `{ list, total, page, size }`
- 时间：ISO8601；DB 使用 `DATE/DATETIME`。

### 5.1 系统设置

- `GET /api/settings` → `{ currency, storage_* }`
- `PUT /api/settings` → body: `{ currency?, storage_provider?, storage_endpoint?, storage_region?, storage_bucket?, storage_access_key?, storage_secret_key?, storage_base_url? }`

### 5.2 文件上传（对象存储）

- `POST /api/files/upload` （multipart）→ 返回 `{ url, objectKey }`
- `DELETE /api/files/{objectKey}` → 删除对象（可选）

> 说明：后端适配 MinIO / S3 兼容协议；URL 使用 `storage_base_url + objectKey` 拼接。

### 5.3 资产

- `POST /api/assets`：创建资产（含主购买）
- `GET /api/assets`：查询，参数：`status?（使用中|已闲置|待出售|已出售|已丢弃） category? brand? platform? tag? q? year? page? size? sort?`
- `GET /api/assets/{id}`：详情（附 metrics：`total_invest/use_days/avg_cost_per_day`）
- `PUT /api/assets/{id}`：更新
- `PATCH /api/assets/{id}/status`：`{ status }`
- `DELETE /api/assets/{id}`：限制删除

### 5.4 购买记录

- `POST /api/purchases`、`PUT /api/purchases/{id}`、`DELETE /api/purchases/{id}`
- `PATCH /api/assets/{id}/primary-purchase`：切换主购买

### 5.5 售出记录

- `POST /api/sales`（写入后置资产状态为 **已出售**）
- `PUT /api/sales/{id}`、`DELETE /api/sales/{id}`（撤销后资产状态回滚至原状态，默认回 `待出售` 或 `已闲置`）
- **便捷接口（向导）**：`POST /api/assets/{id}/sell` → 一次性完成售出（参数同 `SaleCreateReq`），并将资产状态从 `待出售` → `已出售`。

**SaleCreateReq（请求体）**

```json
{
  "platform": "闲鱼",
  "buyer": "张三",
  "sale_price": 5200.00,
  "fee": 50.00,
  "shipping_cost": 12.00,
  "other_cost": 0.00,
  "sale_date": "2025-03-08",
  "attachments": ["oss://.../screenshot1.png"],
  "notes": "95新，含原装配件"
}
```

> 校验：`sale_price >= 0`，各费用 `>= 0`；`sale_date >= enabled_date`；资产当前状态需为 `待出售` 或 `已闲置/使用中`（允许直接售出则自动置 `待出售` 再 `已出售`）。

### 5.6 心愿单（独立）

- `POST /api/wishlist`：新增心愿条目（仅基础信息）
- `GET /api/wishlist`：列表/查询
- `PUT /api/wishlist/{id}`、`DELETE /api/wishlist/{id}`
- `POST /api/wishlist/{id}/convert`：将心愿条目**转为资产**（生成资产并回写 `converted_asset_id`），前端入口为“以后买/已买”
- `POST /api/wishlist`：新增心愿条目（仅基础信息）
- `GET /api/wishlist`：列表/查询
- `PUT /api/wishlist/{id}`、`DELETE /api/wishlist/{id}`
- `POST /api/wishlist/{id}/convert`：将心愿条目**转为资产**（生成资产并回写 `converted_asset_id`），前端入口为“以后买/已买”

### 5.7 统计

- `GET /api/metrics/overview`：在用资产数、总投入、当年投入、近30天购买/售出
- `GET /api/metrics/by-category`
- `GET /api/metrics/by-platform`

### 5.8 错误码（总览）

- `200` 成功
- `400` 参数错误
- `404` 不存在
- `409` 业务冲突
- `413` 文件过大（上传）
- `415` 不支持的媒体类型（上传）
- `500` 服务器错误

#### 5.8.1 错误码字典（细化）

| code | 场景                   | 说明                                                |
| ---- | ---------------------- | --------------------------------------------------- |
| 200  | SUCCESS                | 正常                                                |
| 400  | VALIDATION_ERROR       | 字段校验失败（见前端校验清单）                      |
| 404  | ASSET_NOT_FOUND        | 资产不存在                                          |
| 404  | PURCHASE_NOT_FOUND     | 购买记录不存在                                      |
| 404  | SALE_NOT_FOUND         | 售出记录不存在                                      |
| 409  | ASSET_DELETE_CONFLICT  | 资产存在购买/售出，禁止删除                         |
| 409  | SALE_STATUS_CONFLICT   | 资产状态与售出流程不一致（如非“待出售”却创建 sale） |
| 409  | DATE_RANGE_CONFLICT    | 日期不合法（sale_date < enabled_date 等）           |
| 413  | FILE_TOO_LARGE         | 图片超出限制（默认 5MB）                            |
| 415  | UNSUPPORTED_MEDIA_TYPE | 非法图片类型（仅 jpg/png/webp）                     |
| 500  | INTERNAL_ERROR         | 服务器错误                                          |

------

## 6. 计算与服务层细则

### 6.1 Metrics 计算（简化）

1. 汇总 `purchase` 得出 `total_invest`。
2. 计算 `use_days`（依据当前状态的日期边界）。
3. 输出 `avg_cost_per_day = total_invest / max(use_days,1)`。

### 6.2 数据一致性

- `sale` 写入/撤销与资产状态的联动。
- 心愿单与资产互斥：心愿条目 `convert` 后标记 `converted_asset_id`，避免重复生成。

------

## 7. 后端工程设计

### 7.1 包结构（建议）

```
com.digiledger
├─ common
│  ├─ api   (统一返回体、异常处理、错误码)
│  ├─ util  (Date, Money, Validator)
├─ config   (MyBatis、Jackson、CORS、Swagger)
├─ domain
│  ├─ entity (PO: Asset, Purchase, Sale, Setting, OpLog)
│  ├─ dto    (请求/响应 DTO：AssetCreateReq, AssetDetailResp ...)
│  ├─ vo     (列表/详情 VO)
├─ mapper   (MyBatis Mapper 接口 + XML)
├─ service  (接口与实现)
├─ controller (REST)
└─ bootstrap (启动类)
```

### 7.2 统一返回体

```java
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ApiResp<T> {
  private int code;   // 200 success
  private T data;
  private String msg;
  public static <T> ApiResp<T> ok(T data){return new ApiResp<>(200, data, "success");}
  public static <T> ApiResp<T> fail(int code,String msg){return new ApiResp<>(code,null,msg);}  
}
```

### 7.3 关键 DTO（简要）

- `AssetCreateReq`：含 `purchase` 内嵌对象；服务层落表时需事务。
- `AssetDetailResp`：`asset + purchases + sale + metrics`。
- `SaleCreateReq`：写入后变更 `asset.status`。

### 7.4 MyBatis 建议

- MapperXML 中编写分页查询（`LIMIT #{offset}, #{size}`）。
- 公共拦截器（可选）：统一填充 `created_at/updated_at`。

### 7.5 校验规则

- 使用 `Hibernate Validator`：金额非负、日期关系合法、枚举校验。

### 7.6 Swagger/OpenAPI

- 集成 `springdoc-openapi`，暴露 `/swagger-ui.html`。

------

## 8. 前端工程设计（Vue3 + Vite + Element Plus + H5 适配）

### 8.1 菜单与导航

- **资产总览**（原“主页”）：KPI 概览、快速筛选、按**顶级类别**TAB 切换展示近期物品卡片；支持“**创建物品**”**弹窗**（H5 端切换到独立页面）。
- **物品列表**：查询/增删改查，支持状态/分类（多级，**允许选父级**）/平台/标签（多级）过滤；支持**表格/卡片**两种视图切换；提供**状态快捷 TAB**（使用中/已闲置/待出售/已出售）。
- **心愿单**：独立列表；条目带“已购买”按钮 → 触发 `convert`（沿用物品新增逻辑）；列表支持“详情”抽屉查看；新增/编辑不再直接修改状态，状态仅能通过“已购买”转换产生。
- **已售出列表**：
  - 页面聚合**待出售**与**已出售**两类卡片区
  - 每条 **待出售** 支持“一键出售向导”：弹出表单（售价/平台/费用/日期/买家/备注/附件上传） → 成功后移入“已出售”区
  - 出售支持 **主商品/配件** 两种范围；仅主商品出售会变更资产状态
- **系统设置**：
  - **物品类别**（**多级**）：树形维护，支持排序/拖拽/禁用；**下拉新建改为弹窗选择父层级后新建**，避免层级歧义
  - **购买平台**（**单级**）：名称、链接、排序。
  - **标签**（**多级/多选**）：可选颜色、图标；用于列表过滤与可视化。
  - **品牌**（**单级**）：名称、别名（可选）、首字母、排序。
  - **主题**：新增主题色切换（浅色/深色 + 霓虹配色预设）。


#### 8.1.1 一键出售向导（前端交互）

- 入口：已售出列表页中的 `待出售` 卡片按钮 `去出售`
- 表单字段：`platform, buyer, sale_price, fee, shipping_cost, other_cost, sale_date, notes, attachments`
- 校验：
  - `sale_price` 必填且 `>= 0`
  - 费用类 `>= 0`
  - `sale_date` 不能早于资产 `enabled_date`
- 提交：调用 `POST /api/assets/{id}/sell`
- 回显：成功后，当前卡片从“待出售”区移入“已出售”区，显示 `net_income = sale_price - fee - shipping_cost - other_cost`

### 8.2 目录建议

```
src
├─ api        // asset.ts, purchase.ts, sale.ts, metrics.ts, setting.ts, file.ts, wishlist.ts
├─ router
├─ pages
│  ├─ dashboard/index.vue
│  ├─ asset/{list.vue, form.vue, detail.vue}
│  ├─ wishlist/{list.vue, form.vue}
│  ├─ sold/index.vue
│  └─ setting/{dict.vue, storage.vue}
├─ components // UploadBox(对象存储直传/后端转发)、StatCard、SearchBar
├─ styles     // 数码风格主题（深色+霓虹高亮），支持 H5 响应式
└─ utils      // 日期/金额/适配器
```

### 8.3 H5 适配与数码风格

- 使用 **flex/grid** + 断点（xs/sm/md）布局；按钮与表格列在小屏收敛为抽屉/卡片。
- 主题建议：深色基底、蓝紫霓虹高亮、数字化卡片（适合数码产品管理）。
- 上传组件：移动端支持拍照/相册选择，自动压缩与进度提示。
- 列表卡片视图：展示**物品图片、名称、价格（主商品）、购买天数、日均成本、购买日期**；卡片右上角快速状态切换。
- 资产总览页：
  - KPI：**非已出售数量/总数量**；总投入；近30天趋势。
  - 顶级类别 TAB：按顶级类别过滤下方列表；每个卡片缩略图采用等比缩放裁剪（短边对齐），保证行高统一。
  - “创建物品”默认弹窗（H5 使用独立页）。

### 8.4 表单字段校验清单（前端）

**物品（新建/编辑）**

- `name`（物品名称）：必填，1~200 字
- `category_id`：必填（多级类别，**允许父级**）
- `brand_id`：可选（来自品牌字典，可**下拉新建**与搜索）
- `model`：0~200 字
- `tag_ids[]`：多选，可空（下拉新建与搜索）
- `purchase_date`：必填（UI 保留该字段；`enabled_date` 后端默认= `purchase_date`）
- `cover_image_url`：可选，图片类型限制（jpg/png/webp）

**购买记录**

- `type`：必填，`PRIMARY/ACCESSORY/SERVICE`
- `platform_id`：推荐选择（可空，支持**下拉新建**与搜索）
- `name`：当 `type ∈ {ACCESSORY,SERVICE}` 时**必填**（配件/服务名称）
- `price`：必填，`>= 0`
- `shipping_cost`：`>= 0`
- `purchase_date`：必填，`<= 今天`
- `type`：必填，`PRIMARY/ACCESSORY/SERVICE`
- `platform_id`：推荐选择（可空）
- `price`：必填，`>= 0`
- `shipping_cost`：`>= 0`
- `purchase_date`：必填，`<= 今天`

**售出记录/出售向导**

- `platform_id`：推荐选择（字典）
- `sale_price`：必填，`>= 0`
- `fee / shipping_cost / other_cost`：`>= 0`
- `sale_date`：必填，`>= enabled_date`
- **展示控制**：仅当物品状态为“已出售”时在详情页展示“售出记录”区块。

**心愿单**

- `name`：必填
- `category_id/brand_id`：沿用物品字典（**删除平台字段**）
- `image_url`：可选（上传）
- `status`：`未购买/已购买`（新增）
- `expected_price`：`>= 0`
- `link`：URL 校验（http/https）
- **已购买流程**：点击“已购买”→ 走物品新增流程并带入同名字段；成功后心愿单标记为“已购买”。
- `name`：必填
- `expected_price`：`>= 0`
- `planned_platform`：可选
- `link`：URL 校验（http/https）

**上传限制**

- 单图 ≤ 5MB；类型限 jpg/png/webp；最多 9 张/物品（可在设置里调整）。

------

## 9. 部署与环境

### 9.1 环境变量（后端）

- `DL_DB_HOST/PORT/NAME/USER/PASS`
- `DL_SERVER_PORT`（默认 `8080`）
- **对象存储**：`DL_STORAGE_PROVIDER`、`DL_STORAGE_ENDPOINT`、`DL_STORAGE_REGION`、`DL_STORAGE_BUCKET`、`DL_STORAGE_ACCESS_KEY`、`DL_STORAGE_SECRET_KEY`、`DL_STORAGE_BASE_URL`

### 9.2 Dockerfile（后端示例）

```dockerfile
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY target/digiledger-backend.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-Duser.timezone=Asia/Shanghai","-jar","/app/app.jar"]
```

### 9.3 Dockerfile（前端示例）

```dockerfile
FROM node:22-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
RUN npm run build

FROM nginx:alpine
COPY --from=build /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
```

### 9.4 Nginx 前端反代（`frontend/nginx.conf`）

```nginx
server {
  listen 80;
  server_name _;
  root /usr/share/nginx/html;
  index index.html;

  location /api/ {
    proxy_pass http://backend:8080/;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
  }

  location / {
    try_files $uri $uri/ /index.html;
  }
}
```

### 9.5 docker-compose.yml（集成 MinIO）

```yaml
version: "3.9"
services:
  db:
    image: mysql:8.0
    container_name: dl-mysql
    environment:
      MYSQL_ROOT_PASSWORD: root
      MYSQL_DATABASE: digiledger
      MYSQL_USER: dl_user
      MYSQL_PASSWORD: dl_pass
    ports:
      - "3307:3306"
    volumes:
      - db_data:/var/lib/mysql
      - ./deploy/sql:/docker-entrypoint-initdb.d
    command: ["mysqld","--character-set-server=utf8mb4","--collation-server=utf8mb4_0900_ai_ci"]

  minio:
    image: minio/minio:latest
    command: server /data --console-address ":9001"
    environment:
      MINIO_ROOT_USER: minioadmin
      MINIO_ROOT_PASSWORD: minioadmin
    ports:
      - "9000:9000"
      - "9001:9001"
    volumes:
      - minio_data:/data

  backend:
    build: ./backend
    image: digiledger/backend:latest
    container_name: dl-backend
    environment:
      DL_DB_HOST: db
      DL_DB_PORT: 3306
      DL_DB_NAME: digiledger
      DL_DB_USER: dl_user
      DL_DB_PASS: dl_pass
      DL_STORAGE_PROVIDER: minio
      DL_STORAGE_ENDPOINT: http://minio:9000
      DL_STORAGE_BUCKET: digiledger
      DL_STORAGE_ACCESS_KEY: minioadmin
      DL_STORAGE_SECRET_KEY: minioadmin
      DL_STORAGE_BASE_URL: http://localhost:9000/digiledger
    depends_on:
      - db
      - minio

  frontend:
    build: ./frontend
    image: digiledger/frontend:latest
    container_name: dl-frontend
    depends_on:
      - backend
    ports:
      - "8088:80"

volumes:
  db_data:
  minio_data:
```

### 9.6 部署步骤

1. **准备目录结构**

```
DigiLedger/
├─ backend/
├─ frontend/
└─ deploy/
   └─ sql/  # init.sql, seed.sql
```

1. **后端打包**：`cd backend && mvn -DskipTests package`
2. **前端构建**：`cd frontend && npm ci && npm run build`
3. **Compose 启动**：在项目根目录执行 `docker compose up -d --build`
4. 访问前端：`http://<host>:8088`

------

## 10. 初始化 SQL（deploy/sql/init.sql 摘要）

```sql
CREATE DATABASE IF NOT EXISTS digiledger DEFAULT CHARACTER SET utf8mb4;
USE digiledger;
-- 建表（见 4. 数据模型）
-- 初始设置
INSERT INTO sys_setting (currency, storage_provider, storage_bucket) VALUES ('CNY','minio','digiledger');
```

> 可选 `seed.sql`：插入示例资产/购买/心愿条目，方便联调。

------

## 11. 统计 SQL 参考

- 总投入：

```sql
SELECT asset_id, SUM(price + shipping_cost) AS total_invest
FROM purchase
GROUP BY asset_id;
```

- 按类别投入：

```sql
SELECT a.category, SUM(p.price + p.shipping_cost) AS invest
FROM device_asset a
JOIN purchase p ON a.id = p.asset_id
GROUP BY a.category
ORDER BY invest DESC;
```

- 心愿单数量：

```sql
SELECT COUNT(*) FROM wishlist;
```

--- 统计 SQL 参考

- 总投入：

```sql
SELECT asset_id, SUM(price + shipping_cost) AS total_invest
FROM purchase
GROUP BY asset_id;
```

- 按类别投入：

```sql
SELECT a.category, SUM(p.price + p.shipping_cost) AS invest
FROM device_asset a
JOIN purchase p ON a.id = p.asset_id
GROUP BY a.category
ORDER BY invest DESC;
```

- 时间序列（按月购买额）：

```sql
SELECT DATE_FORMAT(purchase_date, '%Y-%m') AS ym, SUM(price + shipping_cost) AS invest
FROM purchase
GROUP BY ym
ORDER BY ym;
```

------

## 12. 验证与测试

### 12.1 单元测试

- 计算：`use_days`、`avg_cost_per_day`。
- 状态流转：`使用中 ↔ 已闲置 ↔ 待出售 → 已出售`；撤销恢复。
- 心愿单：`convert` 后避免重复生成资产。
- 下拉新建：类别/平台/标签/品牌的**并发创建**与去重校验。

### 12.2 接口契约

- Swagger/OpenAPI 字段必填与类型；分页边界；上传大小与类型限制（图片：jpg/png/webp ≤ 5MB）。

### 12.3 UI 验收（含 H5）

- 资产总览：非已出售数量/总数量展示正确；TAB 按顶级类别过滤正确。
- 物品列表：表格/卡片视图切换；行内状态修改；多选批量**设置标签**生效。
- 物品新增：下拉新建类别/标签/平台/品牌，搜索可用；“今天”快捷日期选择可用。
- 物品详情：主购买文案改为“**主商品**”；新增/编辑/删除购买记录功能可用；售出记录仅在已出售时显示。
- 心愿单：图片上传；状态切换与 TAB 正确；“已购买”生成物品并带入字段。

### 12.1 单元测试

- 计算：`use_days`、`avg_cost_per_day`。
- 状态流转：`使用中 ↔ 已闲置 ↔ 待出售 → 已出售`；撤销恢复。
- 心愿单：`convert` 后避免重复生成资产。

### 12.2 接口契约

- Swagger/OpenAPI 字段必填与类型；分页边界；上传大小与类型限制（图片：jpg/png/webp ≤ 5MB）。

### 12.3 UI 验收（含 H5）

- 主页 KPI、趋势与快捷筛选在移动端良好展示。
- 物品列表在小屏切换为卡片流；上传图片可拍照。
- 心愿单条目“已买”→ 生成资产后，心愿列表正确标记。

------

## 13. 路线图（非 MVP）

- 多用户与权限
- 订单导入（京东/淘宝）
- 价格趋势与估值
- 通知中心（心愿降价/保修到期）
- 标签统计与可视化

--- 路线图（非 MVP）

- 账户体系（多用户/团队/权限）。
- 附件对象存储（MinIO/S3）与 CDN。
- 订单解析（京东/淘宝导入）。
- 行情估值抓取与价格快照。
- 通知（保修到期、出售提醒）。
- 使用时长模式（usage_log）与多维度成本核算。

------

### 附：示例后端配置（application.yml）

```yaml
server:
  port: ${DL_SERVER_PORT:8080}

spring:
  datasource:
    url: jdbc:mysql://${DL_DB_HOST:localhost}:${DL_DB_PORT:3306}/${DL_DB_NAME:digiledger}?useSSL=false&allowPublicKeyRetrieval=true&characterEncoding=utf8
    username: ${DL_DB_USER:root}
    password: ${DL_DB_PASS:root}
    driver-class-name: com.mysql.cj.jdbc.Driver
  jackson:
    time-zone: Asia/Shanghai

storage:
  provider: ${DL_STORAGE_PROVIDER:minio}
  endpoint: ${DL_STORAGE_ENDPOINT:http://localhost:9000}
  region: ${DL_STORAGE_REGION:}
  bucket: ${DL_STORAGE_BUCKET:digiledger}
  access-key: ${DL_STORAGE_ACCESS_KEY:}
  secret-key: ${DL_STORAGE_SECRET_KEY:}
  base-url: ${DL_STORAGE_BASE_URL:}

mybatis:
  mapper-locations: classpath:/mapper/*.xml
  configuration:
    map-underscore-to-camel-case: true

logging:
  level:
    root: INFO
    com.digiledger: DEBUG
```

### 附：前端环境变量（.env.production 示例）

```
VITE_API_BASE=/api
```

### 附：前端 Axios 实例（/src/api/http.ts）

```ts
import axios from 'axios'
const http = axios.create({ baseURL: import.meta.env.VITE_API_BASE || '/api', timeout: 15000 })
http.interceptors.response.use(
  (resp) => { const { code, data, msg } = resp.data; if (code !== 200) return Promise.reject(new Error(msg||'Error')); return data },
  (err) => Promise.reject(err)
)
export default http
```

------

## 14. 交付物与样例（OpenAPI / Postman / CSV / Seed）

> 路径建议：均位于项目根目录 `deploy/` 下，Codex 可直接读取。

### 14.1 OpenAPI 3.0 规范（`deploy/openapi.yaml`）

```yaml
openapi: 3.0.3
info:
  title: DigiLedger API
  version: 0.3.4
  description: 数码物品管理（方案A，按天均摊，不含折旧）
servers:
  - url: /api
paths:
  /settings:
    get:
      summary: 获取系统设置
      responses:
        '200': { $ref: '#/components/responses/OkSetting' }
    put:
      summary: 更新系统设置
      requestBody:
        required: true
        content:
          application/json:
            schema: { $ref: '#/components/schemas/SettingUpdateReq' }
      responses:
        '200': { $ref: '#/components/responses/OkSetting' }

  /files/upload:
    post:
      summary: 上传图片（MinIO/S3 兼容）
      requestBody:
        required: true
        content:
          multipart/form-data:
            schema:
              type: object
              properties:
                file:
                  type: string
                  format: binary
      responses:
        '200':
          description: OK
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/ApiResp'
              examples:
                ok:
                  value: { code: 200, data: { url: 'http://.../digiledger/obj.png', objectKey: 'obj.png' }, msg: 'success' }
        '413': { $ref: '#/components/responses/FileTooLarge' }
        '415': { $ref: '#/components/responses/UnsupportedMedia' }

  # 字典 - 品牌（新增）
  /dict/brands:
    get:
      summary: 品牌列表
      responses:
        '200': { $ref: '#/components/responses/Ok' }
    post:
      summary: 新增品牌
      requestBody:
        required: true
        content:
          application/json:
            schema: { $ref: '#/components/schemas/BrandCreateReq' }
      responses:
        '200': { $ref: '#/components/responses/Ok' }
  /dict/brands/{id}:
    put:
      summary: 更新品牌
      parameters: [{ in: path, name: id, required: true, schema: { type: integer } }]
      requestBody:
        required: true
        content:
          application/json:
            schema: { $ref: '#/components/schemas/BrandUpdateReq' }
      responses:
        '200': { $ref: '#/components/responses/Ok' }
    delete:
      summary: 删除品牌
      parameters: [{ in: path, name: id, required: true, schema: { type: integer } }]
      responses:
        '200': { $ref: '#/components/responses/Ok' }

  /assets:
    get:
      summary: 查询物品
      parameters:
        - in: query
          name: status
          schema: { type: string, enum: [使用中, 已闲置, 待出售, 已出售, 已丢弃] }
        - in: query
          name: category_id
          schema: { type: integer, minimum: 1 }
          description: 支持父级，服务端包含子孙类目
        - in: query
          name: brand_id
          schema: { type: integer, minimum: 1 }
        - in: query
          name: platform_id
          schema: { type: integer, minimum: 1 }
        - in: query
          name: tag_ids
          style: form
          explode: true
          schema: { type: array, items: { type: integer } }
        - in: query
          name: q
          schema: { type: string }
        - in: query
          name: view
          schema: { type: string, enum: [list, card] }
        - in: query
          name: page
          schema: { type: integer, minimum: 1, default: 1 }
        - in: query
          name: size
          schema: { type: integer, minimum: 1, maximum: 200, default: 20 }
      responses:
        '200': { $ref: '#/components/responses/OkAssetPage' }
    post:
      summary: 新建物品（可内嵌主购买）
      requestBody:
        required: true
        content:
          application/json:
            schema: { $ref: '#/components/schemas/AssetCreateReq' }
      responses:
        '200': { $ref: '#/components/responses/OkAsset' }

  /assets/{id}:
    get:
      summary: 物品详情（含 metrics）
      parameters: [{ in: path, name: id, required: true, schema: { type: integer } }]
      responses:
        '200': { $ref: '#/components/responses/OkAssetDetail' }
        '404': { $ref: '#/components/responses/NotFound' }
    put:
      summary: 更新物品
      parameters: [{ in: path, name: id, required: true, schema: { type: integer } }]
      requestBody:
        required: true
        content: { application/json: { schema: { $ref: '#/components/schemas/AssetUpdateReq' } } }
      responses:
        '200': { $ref: '#/components/responses/OkAsset' }
    delete:
      summary: 删除物品（有购买/销售记录时禁止）
      parameters: [{ in: path, name: id, required: true, schema: { type: integer } }]
      responses:
        '200': { $ref: '#/components/responses/Ok' }
        '409': { $ref: '#/components/responses/Conflict' }

  /assets/{id}/status:
    patch:
      summary: 行内修改物品状态
      parameters: [{ in: path, name: id, required: true, schema: { type: integer } }]
      requestBody:
        required: true
        content:
          application/json:
            schema:
              type: object
              properties:
                status: { type: string, enum: [使用中, 已闲置, 待出售, 已出售, 已丢弃] }
      responses:
        '200': { $ref: '#/components/responses/OkAsset' }

  /assets/{id}/sell:
    post:
      summary: 一键出售（向导接口）
      parameters: [{ in: path, name: id, required: true, schema: { type: integer } }]
      requestBody:
        required: true
        content: { application/json: { schema: { $ref: '#/components/schemas/SaleCreateReq' } } }
      responses:
        '200': { $ref: '#/components/responses/OkSale' }
        '409': { $ref: '#/components/responses/Conflict' }

  /purchases:
    post:
      summary: 新建购买记录
      requestBody:
        required: true
        content: { application/json: { schema: { $ref: '#/components/schemas/PurchaseCreateReq' } } }
      responses:
        '200': { $ref: '#/components/responses/OkPurchase' }

  /sales:
    post:
      summary: 新建售出记录
      requestBody:
        required: true
        content: { application/json: { schema: { $ref: '#/components/schemas/SaleCreateReq' } } }
      responses:
        '200': { $ref: '#/components/responses/OkSale' }

  /wishlist:
    get:
      summary: 心愿单列表
      parameters:
        - in: query
          name: status
          schema: { type: string, enum: [未购买, 已购买] }
      responses:
        '200': { $ref: '#/components/responses/OkWishlistPage' }
    post:
      summary: 新增心愿
      requestBody:
        required: true
        content: { application/json: { schema: { $ref: '#/components/schemas/WishlistCreateReq' } } }
      responses:
        '200': { $ref: '#/components/responses/OkWishlist' }

  /wishlist/{id}:
    put:
      summary: 更新心愿
      parameters: [{ in: path, name: id, required: true, schema: { type: integer } }]
      requestBody:
        required: true
        content: { application/json: { schema: { $ref: '#/components/schemas/WishlistUpdateReq' } } }
      responses:
        '200': { $ref: '#/components/responses/OkWishlist' }
    delete:
      summary: 删除心愿
      parameters: [{ in: path, name: id, required: true, schema: { type: integer } }]
      responses:
        '200': { $ref: '#/components/responses/Ok' }

  /wishlist/{id}/convert:
    post:
      summary: 心愿 → 物品（已购买）
      parameters: [{ in: path, name: id, required: true, schema: { type: integer } }]
      responses:
        '200': { $ref: '#/components/responses/OkAsset' }

components:
  responses:
    Ok: { description: OK, content: { application/json: { schema: { $ref: '#/components/schemas/ApiResp' } } } }
    OkSetting: { description: OK, content: { application/json: { schema: { $ref: '#/components/schemas/ApiRespSetting' } } } }
    OkAsset: { description: OK, content: { application/json: { schema: { $ref: '#/components/schemas/ApiRespAsset' } } } }
    OkAssetDetail: { description: OK, content: { application/json: { schema: { $ref: '#/components/schemas/ApiRespAssetDetail' } } } }
    OkAssetPage: { description: OK, content: { application/json: { schema: { $ref: '#/components/schemas/ApiRespAssetPage' } } } }
    OkPurchase: { description: OK, content: { application/json: { schema: { $ref: '#/components/schemas/ApiRespPurchase' } } } }
    OkSale: { description: OK, content: { application/json: { schema: { $ref: '#/components/schemas/ApiRespSale' } } } }
    OkWishlist: { description: OK, content: { application/json: { schema: { $ref: '#/components/schemas/ApiRespWishlist' } } } }
    OkWishlistPage: { description: OK, content: { application/json: { schema: { $ref: '#/components/schemas/ApiRespWishlistPage' } } } }
    NotFound: { description: Not Found }
    Conflict: { description: Conflict }
    FileTooLarge: { description: Payload Too Large }
    UnsupportedMedia: { description: Unsupported Media Type }

  schemas:
    ApiResp:
      type: object
      properties: { code: { type: integer }, data: {}, msg: { type: string } }
      required: [code]
    ApiRespSetting: { allOf: [ { $ref: '#/components/schemas/ApiResp' } ] }
    ApiRespAsset:   { allOf: [ { $ref: '#/components/schemas/ApiResp' } ] }
    ApiRespAssetDetail: { allOf: [ { $ref: '#/components/schemas/ApiResp' } ] }
    ApiRespAssetPage: { allOf: [ { $ref: '#/components/schemas/ApiResp' } ] }
    ApiRespPurchase:{ allOf: [ { $ref: '#/components/schemas/ApiResp' } ] }
    ApiRespSale:    { allOf: [ { $ref: '#/components/schemas/ApiResp' } ] }
    ApiRespWishlist:{ allOf: [ { $ref: '#/components/schemas/ApiResp' } ] }
    ApiRespWishlistPage:{ allOf: [ { $ref: '#/components/schemas/ApiResp' } ] }

    SettingUpdateReq:
      type: object
      properties:
        currency: { type: string, example: CNY }
        storage_provider: { type: string, example: minio }
        storage_endpoint: { type: string, example: http://minio:9000 }
        storage_region: { type: string }
        storage_bucket: { type: string }
        storage_access_key: { type: string }
        storage_secret_key: { type: string }
        storage_base_url: { type: string }

    # 物品
    AssetCreateReq:
      type: object
      required: [name, category_id, purchase_date]
      properties:
        name: { type: string }
        category_id: { type: integer, minimum: 1 }
        brand_id: { type: integer, minimum: 1 }
        tag_ids: { type: array, items: { type: integer } }
        model: { type: string }
        purchase_date: { type: string, format: date, description: UI 提交；后端默认 enabled_date = purchase_date }
        cover_image_url: { type: string }
        status: { type: string, enum: [使用中, 已闲置, 待出售, 已出售, 已丢弃] }
        purchase: { $ref: '#/components/schemas/PurchaseCreateReq' }

    AssetUpdateReq:
      type: object
      properties:
        name: { type: string }
        category_id: { type: integer, minimum: 1 }
        brand_id: { type: integer, minimum: 1 }
        tag_ids: { type: array, items: { type: integer } }
        model: { type: string }
        purchase_date: { type: string, format: date }
        cover_image_url: { type: string }
        status: { type: string, enum: [使用中, 已闲置, 待出售, 已出售, 已丢弃] }

    # 购买
    urchaseCreateReq:
      type: object
      required: [asset_id, type, price, purchase_date]
      properties:
        asset_id: { type: integer }
        type: { type: string, enum: [PRIMARY, ACCESSORY, SERVICE] }
        name: { type: string, description: 配件/服务名称，type ∈ {ACCESSORY,SERVICE} 时必填 }
        platform_id: { type: integer }
        seller: { type: string }
        price: { type: number, format: double }
        quantity: { type: integer, example: 1 }
        shipping_cost: { type: number, format: double, example: 0 }
        purchase_date: { type: string, format: date }
        attachments: { type: array, items: { type: string } }
        notes: { type: string }

    # 售出
    SaleCreateReq:
      type: object
      required: [sale_price, sale_date]
      properties:
        sale_scope: { type: string, enum: [ASSET, ACCESSORY], default: ASSET, description: ASSET=出售主商品并变更资产状态, ACCESSORY=出售配件仅记录不改状态 }
        asset_id: { type: integer, description: 当 sale_scope=ACCESSORY 也需传所属资产ID }
        purchase_id: { type: integer, description: 当 sale_scope=ACCESSORY 必填，指向被出售的配件购买记录ID }
        platform_id: { type: integer }
        buyer: { type: string }
        sale_price: { type: number, format: double }
        fee: { type: number, format: double, default: 0 }
        shipping_cost: { type: number, format: double, default: 0 }
        other_cost: { type: number, format: double, default: 0 }
        sale_date: { type: string, format: date }
        attachments: { type: array, items: { type: string } }
        notes: { type: string }


    # 心愿单
    WishlistCreateReq:
      type: object
      required: [name]
      properties:
        name: { type: string }
        category_id: { type: integer }
        brand_id: { type: integer }
        model: { type: string }
        expected_price: { type: number, format: double }
        image_url: { type: string }
        link: { type: string }
        notes: { type: string }


    WishlistUpdateReq:
      allOf: [ { $ref: '#/components/schemas/WishlistCreateReq' } ]
```

### 14.2 Postman 集合（`deploy/postman/DigiLedger.postman_collection.json`）

```json
{
  "info": {"name": "DigiLedger API", "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json"},
  "variable": [{"key": "baseUrl", "value": "http://localhost:8080/api"}],
  "item": [
    {"name": "Settings - GET", "request": {"method": "GET", "url": "{{baseUrl}}/settings"}},
    {"name": "Upload - POST /files/upload", "request": {"method": "POST", "url": "{{baseUrl}}/files/upload","body": {"mode": "formdata","formdata": [{"key": "file","type": "file","src": "./sample.jpg"}]}}},
    {"name": "Asset - POST /assets", "request": {"method": "POST","header": [{"key":"Content-Type","value":"application/json"}],"url": "{{baseUrl}}/assets","body": {"mode": "raw","raw": "{
  \"name\": \"Sony A7M4\",
  \"category\": \"相机\",
  \"brand\": \"Sony\",
  \"enabled_date\": \"2025-01-10\",
  \"purchase\": {
    \"type\": \"PRIMARY\",
    \"platform\": \"京东\",
    \"price\": 13999,
    \"shipping_cost\": 0,
    \"purchase_date\": \"2025-01-09\"
  }
}"}}},
    {"name": "Asset - GET /assets", "request": {"method": "GET", "url": "{{baseUrl}}/assets?page=1&size=10"}},
    {"name": "Sell Wizard - POST /assets/{id}/sell", "request": {"method": "POST","header": [{"key":"Content-Type","value":"application/json"}],"url": "{{baseUrl}}/assets/1/sell","body": {"mode": "raw","raw": "{
  \"platform\": \"闲鱼\",
  \"buyer\": \"张三\",
  \"sale_price\": 11800,
  \"fee\": 80,
  \"shipping_cost\": 20,
  \"sale_date\": \"2025-03-08\"
}"}}},
    {"name": "Wishlist - POST /wishlist", "request": {"method": "POST","header": [{"key":"Content-Type","value":"application/json"}],"url": "{{baseUrl}}/wishlist","body": {"mode": "raw","raw": "{
  \"name\": \"Canon RF 85mm F1.8\",
  \"category\": \"镜头\",
  \"brand\": \"Canon\",
  \"expected_price\": 2999,
  \"planned_platform\": \"京东\",
  \"link\": \"https://item.jd.com/xxx.html\"
}"}}}
  ]
}
```

### 14.3 CSV 模板（`deploy/csv/`）

**assets.csv**

```
name,category,brand,model,enabled_date,status,cover_image_url,notes
Sony A7M4,相机,Sony,A7M4,2025-01-10,使用中,,机身+原装电池
DJI Mini 4 Pro,无人机,DJI,Mini4Pro,2024-12-11,已闲置,,用于旅行航拍
```

**purchases.csv**

```
asset_name,type,platform,seller,price,currency,quantity,shipping_cost,purchase_date,invoice_no,notes
Sony A7M4,PRIMARY,京东,京东自营,13999,CNY,1,0,2025-01-09,,首发购买
Sony A7M4,ACCESSORY,淘宝,电池店,399,CNY,1,0,2025-01-12,,原厂电池
DJI Mini 4 Pro,PRIMARY,天猫,大疆旗舰店,5999,CNY,1,12,2024-12-10,,含延保
```

**wishlist.csv**

```
name,category,brand,model,expected_price,planned_platform,link,notes
Canon RF 85mm F1.8,镜头,Canon,,2999,京东,https://item.jd.com/xxx.html,人像镜头
```

> 导入建议：先导入 `assets.csv` 生成资产，再用 `purchases.csv` 关联到已存在的 `asset_name`（后端可做名称->ID 映射或前端先查 ID）。

### 14.4 示例种子数据（`deploy/sql/seed.sql`）

```sql
USE digiledger;
-- 类别 & 平台
INSERT INTO dict_category(name,sort) VALUES ('相机',1),('镜头',2),('电池',3),('无人机',4);
INSERT INTO dict_platform(name,link,sort) VALUES ('京东','https://jd.com',1),('淘宝','https://taobao.com',2),('闲鱼','https://goofish.com',3);

-- 资产：相机主体
INSERT INTO device_asset(name,category,brand,model,status,purchase_date,enabled_date,notes)
VALUES ('Sony A7M4','相机','Sony','A7M4','使用中','2025-01-09','2025-01-10','机身+原装电池');
SET @a7m4_id = LAST_INSERT_ID();

-- 主购买 + 配件
INSERT INTO purchase(asset_id,type,platform,price,shipping_cost,purchase_date,notes)
VALUES
(@a7m4_id,'PRIMARY','京东',13999,0,'2025-01-09','首发购买'),
(@a7m4_id,'ACCESSORY','淘宝',399,0,'2025-01-12','原厂电池');

-- 资产：镜头
INSERT INTO device_asset(name,category,brand,model,status,purchase_date,enabled_date,notes)
VALUES ('Sigma 28-70 F2.8 E','镜头','Sigma','28-70/2.8','已闲置','2024-11-02','2024-11-03','便携变焦');
SET @lens_id = LAST_INSERT_ID();
INSERT INTO purchase(asset_id,type,platform,price,shipping_cost,purchase_date)
VALUES (@lens_id,'PRIMARY','淘宝',3999,12,'2024-11-02');

-- 资产：无人机
INSERT INTO device_asset(name,category,brand,model,status,purchase_date,enabled_date)
VALUES ('DJI Mini 4 Pro','无人机','DJI','Mini4Pro','待出售','2024-12-10','2024-12-11');
SET @dji_id = LAST_INSERT_ID();
INSERT INTO purchase(asset_id,type,platform,price,shipping_cost,purchase_date)
VALUES (@dji_id,'PRIMARY','天猫',5999,12,'2024-12-10');

-- 心愿单
INSERT INTO wishlist(name,category,brand,expected_price,planned_platform,link,notes)
VALUES ('Canon RF 85mm F1.8','镜头','Canon',2999,'京东','https://item.jd.com/xxx.html','人像镜头');
```

------

## 更新日志（Changelog）

- **V0.3.5 （当前）**
  - 文件上传改为**相对路径**（数据库仅存objectKey，前端以基址拼接）。
  - 类别下拉“新建”改为**弹窗选择父级后新建**，避免层级不明。
  - 购买记录表单：去掉“币种/发票编号”，`name`字段置顶，“上传凭证”改为“上传附件”。
  - 出售记录支持**主商品/配件**两种范围，仅主商品出售变更资产状态。
  - 心愿单：新增“详情”抽屉，新增/编辑不再直接修改状态，状态仅由“已购买”转换产生。
  - 物品中心：新增**状态快捷TAB**（使用中/已闲置/待出售/已出售）。
  - 系统设置：新增**主题色切换**（浅色/深色+霓虹预设）。

- **V0.3.4**
  - 前端：资产总览重命名与首页弹窗创建；顶级类别 TAB；卡片视图（图片等比缩放）；列表行内状态修改；多选批量设标签；日期选择支持“今天”。
  - 表单：物品新增改用 `purchase_date`（`enabled_date` 后端默认= `purchase_date`，UI 隐藏）；配件/服务购买新增 `name` 字段；下拉均支持**搜索 + 新建**（类别/平台/标签/品牌）。
  - 数据库：新增 `dict_brand`；`purchase.name`；`device_asset.brand_id`；`wishlist` 调整为 `category_id/brand_id/image_url/status`。
  - API：新增 `/api/dict/brands`；`/api/assets` 支持 `view` 参数；购买记录支持编辑/删除与新增附属品入口。
  - 展示：详情页“主购”→“主商品”，售出记录仅在已出售时显示。
- **V0.3.3** 系统设置与字典（类别/平台/标签）、文案、查询过滤等。
- **V0.3.2** OpenAPI、Postman、CSV、seed.sql。
- **V0.3.1** 错误码字典、校验清单、“一键出售向导”。
- **V0.3** 方案A（无折旧）、MinIO 对接、模块拆分。

------

## 15. 需求变更记录（用户表述 → 规范落地）

> 按日期倒序记录每次用户自然语言修改 → 规格落地内容，便于版本追溯。

### 2025-11-13（V0.3.5）

- **文件上传**：URL改为相对地址，数据库仅存objectKey。
- **类别下拉新建**：改弹窗选择父层级再创建，明确层级关系。
- **购买记录**：移除币种、发票编号，name置顶；上传凭证改为上传附件。
- **出售记录**：支持主商品/配件区分，新增`sale_scope`和`purchase_id`。
- **心愿单**：新增详情抽屉；新增/编辑不再修改状态，仅通过“已购买”转换。
- **物品中心**：新增状态快捷TAB。
- **主题**：新增主题色切换功能。

### 2025-11-12（V0.3.4）

- **首页/资产总览**：创建物品弹窗（H5 独立页）；顶级类别 TAB；KPI 改为“非已出售/总数”。
- **物品中心**：支持父级类别；购买时间字段；卡片视图；批量设标签；行内改状态。
- **新建物品**：下拉新建类别/标签/平台/品牌；品牌字典；删除报废/启用日期（启用=购买）；配件/服务新增名称；日期支持“今天”。
- **物品详情**：主购改主商品；新增附属品；购买记录可编辑/删除；售出记录按状态显示。
- **心愿单**：操作改已购买；支持图片上传；类目/品牌沿用物品字典；移除平台；状态未购买/已购买；TAB 切换；已购买生成物品后自动标记。
- **系统设置**：优化类别 UI；新增品牌字典。

### 2025-11-10（V0.3.3）

- 增加系统设置菜单及 CRUD：类别、平台、标签、品牌。
- 调整文案“资产”→“物品”。

### 2025-11-08（V0.3.2）

- 新增 OpenAPI、Postman、CSV 模板与 seed.sql。

### 2025-11-06（V0.3.1）

- 错误码字典、前端校验清单；新增一键出售向导。

### 2025-11-04（V0.3）

- 采用方案A（无折旧）；接入 MinIO 对象存储；模块划分与前后端架构明确。