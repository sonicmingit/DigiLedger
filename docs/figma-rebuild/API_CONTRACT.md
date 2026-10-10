# DigiLedger Figma 重构 API 契约

## 1. 通用约定

- 默认前缀：`/api`
- 返回体：`{ "code": 200, "data": ..., "msg": "" }`
- 前端仅在 `code === 200` 时把 `data` 作为成功结果。
- 日期使用 `YYYY-MM-DD`，时间使用 ISO-8601。
- 金额使用十进制字符串或 JSON number，前端统一转换为安全的展示模型。

## 2. 直接复用的现有接口

- 物品：`/api/assets`、`/api/assets/{id}`、状态、出售、购买记录、封面建议。
- 字典：分类树、品牌、标签树、平台的 CRUD。
- 文件：文件与附件上传、附件删除、未使用附件清理。
- 心愿单：列表、详情、创建、更新、删除、转物品。
- 升级路线：路线 CRUD、图结构、节点和连线新增/删除。

## 3. 新增或补齐接口

### 3.1 总览与统计

`GET /api/dashboard/summary`

```json
{
  "totalAssetValue": 48620,
  "assetCount": 28,
  "activeCount": 19,
  "idleCount": 4,
  "pendingSaleCount": 3,
  "avgDailyCost": 36.4,
  "monthValueChangeRate": 3.2,
  "monthCostChangeRate": -8.6,
  "statusDistribution": [{ "status": "使用中", "count": 19 }],
  "categoryDistribution": [{ "categoryId": 1, "categoryName": "数码", "value": 32000, "count": 12 }],
  "valueTrend": [{ "month": "2026-07", "value": 48620 }],
  "recentAssets": []
}
```

PC 总览与 H5 数据统计共用该接口；无历史快照时趋势允许由当前数据返回单点，但字段必须稳定。

`GET /api/dashboard/spending` 为新版 PC 的购买支出分析接口。可选查询参数：
`dateFrom`、`dateTo`（含两端，`YYYY-MM-DD`）、`categoryId`（包含子分类）、
`type`（`PRIMARY`／`ACCESSORY`／`SERVICE`）、`platformId`、`q`、`page`（默认 1）、`pageSize`（默认 20，最大 100）。
金额按每笔购买的 `price + shippingCost` 计算，包含已出售物品，不抵扣出售收入；未传日期时统计全部时间。
响应 `data` 包含 `totalSpend`、`primarySpend`、`accessorySpend`、`serviceSpend`、
`purchaseCount`、`assetCount`、`monthlyTrend: [{month, amount}]`、
`categoryBreakdown: [{categoryId, categoryName, amount, purchaseCount}]` 和
`records: {total, page, pageSize, items}`。流水项提供购买 ID、物品 ID 与名称、分类 ID、
购买类型与名称、平台、购买日期、价格、运费及支出金额。分类金额互不重叠；选中父分类后按其直属子分类汇总，直接归属父分类的支出显示为“本分类直属”。

物品创建／更新请求和详情响应新增可选 `specifications` 文本字段，用于主商品配置规格。
配件购买的 `warrantyMonths` 与 `warrantyExpireDate` 在创建／更新时忽略并清空；历史记录不批量迁移。

### 3.2 心愿单价格观察

- `PATCH /api/wishlist/{id}/price`
  - 请求：`{ "currentPrice": 7799, "capturedAt": "2026-07-22T12:00:00+08:00" }`
- `GET /api/wishlist/{id}/price-history`
  - 响应：`[{ "price": 7799, "capturedAt": "..." }]`
- `POST /api/wishlist/{id}/mark-purchased`

心愿单 DTO 增加可选字段：`currentPrice`、`priceChangeRate`、`lastPriceAt`。旧数据必须兼容空值。

### 3.3 升级路线计划字段

路线请求/响应增加可选字段：

```json
{
  "planYear": 2026,
  "annualBudget": 18000
}
```

节点请求/响应增加可选字段：

```json
{
  "title": "通勤音频升级",
  "targetName": "下一代降噪耳机",
  "periodLabel": "Q3",
  "plannedBudget": 3500,
  "expectedRecovery": 900,
  "status": "READY"
}
```

- 新增 `PUT /api/upgrade-routes/{routeId}/nodes/{nodeId}`。
- 节点状态：`PLANNED | READY | EXECUTING | COMPLETED | CANCELLED`。
- 路线列表/图响应应返回可直接汇总的预算、预计回收和状态字段。

### 3.4 设置和导出

- `GET /api/settings/preferences`
- `PUT /api/settings/preferences`
- `GET /api/data/export?format=json|csv`

偏好模型：`currency`、`dateFormat`、`autoBackupEnabled`、`autoBackupTime`。H5 主备服务器地址属于设备本地配置，不上传到该接口。

## 4. H5 主备服务器行为

本地配置模型：

```ts
type ServerProfile = {
  primaryUrl: string
  secondaryUrl: string
  preferred: 'primary' | 'secondary'
  autoFailover: boolean
  timeoutMs: number
}
```

- 自动规范化尾部 `/api` 与斜杠。
- GET/HEAD 在断网、超时或 5xx 时可向另一节点重试一次。
- 4xx 和业务错误不触发切换。
- 写请求不自动跨节点重放，避免重复创建；失败时提示用户检测或手动切换。
- 设置页必须提供两个节点的独立连接测试和当前节点状态。

## 5. 并行开发规则

- PC/H5 可先按本契约建立类型和 API 封装。
- 后端新增字段必须保持现有接口向后兼容。
- 契约变更由根协调者统一处理，子 Agent 不直接修改本文件。

## 6. 上代产品关联（PC 物品编辑与详情）

- `POST /api/assets`、`PUT /api/assets/{id}` 新增可选 `predecessorAssetId`；省略字段保留原关联，显式 `null` 解除关联。开放 API 的物品 PATCH 同样支持此字段。
- `GET /api/assets/predecessor-options?category_id={id}&q={关键词}&exclude_asset_id={当前物品ID}` 按同一分类 ID 搜索名称、品牌、型号，最多返回 20 条，排除自身及会形成循环的物品。
- 详情增加 `predecessorAssetId` 与可空 `predecessorAsset`，后者包含 `id/name/categoryId/categoryPath/brandName/model/status/coverImageUrl/primaryPrice/primaryPurchaseDate/primaryPriceDelta/purchaseGapDays`。
- 差价 = 当前主商品购买价 − 上代主商品购买价；不含运费、配件和服务。购买间隔 = 两件主商品购买日期之间的自然日数。存在多条主商品记录时，取最早购买日期，同日取 ID 较大的记录。
- 缺少主商品价格或日期，对应指标返回 `null`；零价格、零天数保留。负间隔保留符号，页面提示核对日期。
- 服务端拒绝自身关联、跨分类关联和循环关联；已被其他分类的物品引用时，修改分类需先解除引用。删除上代物品时数据库自动将引用置空。
- 数据库迁移：`V12__asset_predecessor.sql`。
