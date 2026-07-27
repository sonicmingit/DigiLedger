# 升级路线接口说明

> 基础地址：`/api/upgrade-routes`。所有接口使用统一返回体 `{ code, data, msg }`，成功时 `code=200`。

## 写入接口

| 方法 | 路径 | 用途 |
| --- | --- | --- |
| `GET` | `/` | 查询路线列表 |
| `POST` | `/` | 原子创建路线与第一个节点 |
| `PUT` | `/{id}` | 编辑路线名称、说明、年度和状态 |
| `DELETE` | `/{id}` | 逻辑删除路线 |
| `POST` | `/{routeId}/nodes` | 新增节点；可基于锚点写入上级、同级、下级 |
| `PUT` | `/{routeId}/nodes/{nodeId}` | 编辑节点标签与说明 |
| `DELETE` | `/{routeId}/nodes/{nodeId}` | 从路线移除节点和关联关系 |
| `POST` | `/{routeId}/links` | 兼容旧客户端的手动连线接口 |
| `DELETE` | `/{routeId}/links/{linkId}` | 删除连线 |
| `GET` | `/{routeId}/graph` | 读取路线图与计算结果 |

## 创建路线

`POST /api/upgrade-routes` 使用 `firstNode` 同时创建路线和第一个节点。`firstNode.nodeType` 为 `ASSET` 时必须传 `assetId`；为 `PLANNED` 时必须传 `targetName`，可附带 `periodLabel`、`plannedBudget`。

```json
{
  "name": "手机升级路线",
  "routeType": "MIXED",
  "status": "ACTIVE",
  "planYear": 2026,
  "annualBudget": 8000,
  "firstNode": { "nodeType": "ASSET", "assetId": 101 }
}
```

`planYear`、`annualBudget` 与独立计划节点保留为历史接口兼容字段，新页面不再写入。首节点可改为 `{ "nodeType": "WISHLIST", "wishlistId": 22 }` 关联待购物品；心愿购买后会自动替换成真实物品。

## 锚点式新增节点

`POST /api/upgrade-routes/{routeId}/nodes` 除节点字段外，新增 `anchorNodeId` 和 `position`：

- `BEFORE`：上级，建立“新节点 → 锚点”的顺序关系。
- `AFTER`：下级，建立“锚点 → 新节点”的顺序关系。
- `ALTERNATIVE`：同级，建立并行候选关系，`alternativePurpose` 可说明用途。

新增节点可附带 `remark`（显示在物品详情）和 `mainline`。同级主物品 `mainline=true` 会参与上下级计算；备用物品 `mainline=false` 仍纳入路线总收支，但不生成主线价差。

服务端会拒绝重复物品、自环、跨路线连线、形成闭环的关系，以及已归档路线的写入。真实物品只能使用一次；计划物品可以并行存在。

## 图谱计算字段

`GET /{routeId}/graph` 的 `nodes` 会带回物品图片、名称、购买时间、购买金额、使用天数和主商品出售情况。`links` 的主要计算字段如下：

| 字段 | 说明 |
| --- | --- |
| `purchaseGapDays` | 两个实际物品购买日期相差的自然日；缺数据时为空 |
| `purchasePriceDelta` | 下级购买价减上级购买价 |
| `replacementNetOutflow` | 下级购买价减上级主商品出售净收入；只有满足出售条件时计算 |
| `calculationStatus` | 计算缺失原因或可用状态 |

`actualSummary` 只汇总实际物品：`totalSpend`、`totalIncome`、`netInvestment`、`dailyCost` 等；`planSummary` 独立汇总计划预算、预计回收和预计净投入。`warnings` 返回图结构或数据完整性提示。

## 出售的边界

路线页面通过既有物品出售接口写入出售记录。路线计算只将主商品出售记录纳入出售收入和升级补款；配件出售不会覆盖主商品的路线收益。

## 当前主物品与自动排序

编辑路线时传 `mainAssetId` 可设置当前正在使用的主物品，该物品必须已经存在于路线中。每次添加上下级或将同级设为主物品后，服务端会按主线物品的主购买日期重新生成顺序关系、代际和价差；不需要手动调整连线。
