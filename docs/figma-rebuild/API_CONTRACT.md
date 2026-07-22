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
