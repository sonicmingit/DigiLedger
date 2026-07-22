# Uniapp Mobile Completion Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 补全 uniapp 移动端核心能力，使其与后端现有接口能力形成可用闭环，并按固定顺序一次开发到完成。

**Architecture:** 采用“先修接口契约与可用性，再补业务闭环，最后补增强能力”的分阶段策略。所有阶段严格串行执行，不允许跳阶段。每阶段以可运行构建与手测清单作为闸门，闸门通过后进入下一阶段。

**Tech Stack:** uni-app (Vue3), uni.request, Spring Boot REST API, Vite(H5) + 微信小程序构建

---

## Confirmed Decisions (Locked)

- [x] 决策 1：心愿单接口统一为单数路径 `/api/wishlist`。
- [x] 决策 2：`POST /api/wishlist/{id}/convert` 采用“移动端最简字段 + 服务端自动补全”策略。
- [x] 决策 3：按本计划顺序一次开发到完成，不拆分批次，不并行跳步。

## Execution Contract (Must Follow)

- [x] 严格串行：Task 1 完成并验收后，才可进入 Task 2。
- [x] 每个 Task 都必须包含：代码完成 + 构建通过 + 手测通过 + 变更记录。
- [x] 不引入与当前 Task 无关的重构。
- [x] 若出现阻塞（后端契约冲突/运行环境故障），先记录 blocker，再最小化绕行，不改变任务顺序。

## Scope Baseline

### Current Mobile Pages

- `uniapp/digiLeder/src/pages/index/index.vue`
- `uniapp/digiLeder/src/pages/asset/list.vue`
- `uniapp/digiLeder/src/pages/asset/detail.vue`
- `uniapp/digiLeder/src/pages/asset/add.vue`
- `uniapp/digiLeder/src/pages/asset/edit.vue`
- `uniapp/digiLeder/src/pages/asset/sell.vue`
- `uniapp/digiLeder/src/pages/wishlist/list.vue`
- `uniapp/digiLeder/src/pages/wishlist/add.vue`
- `uniapp/digiLeder/src/pages/my/index.vue`
- `uniapp/digiLeder/src/pages/settings/index.vue`

### Target Backend Capabilities to Cover

- Asset: 列表筛选/详情/创建/更新/状态/出售/售出记录维护
- Wishlist: 列表/详情/创建/更新/删除/转资产
- Dict: categories/platforms/brands/tags
- Purchase: 创建/更新/删除
- Attachment: 上传/列表/删除
- Cover: suggestions/from-url

---

### Task 1: API Contract Alignment & Request Layer Normalization

**Files:**
- Modify: `uniapp/digiLeder/src/utils/request.js`
- Modify: `uniapp/digiLeder/src/pages/wishlist/list.vue`
- Modify: `uniapp/digiLeder/src/pages/wishlist/add.vue`
- Modify: `uniapp/digiLeder/src/pages/my/index.vue`
- Modify: `uniapp/digiLeder/src/components/upload-image.vue`

- [ ] 统一移动端心愿单路径为 `/wishlist`（单数），替换所有 `/wishlists`。
- [ ] 在 request 层补充路径常量（assets/wishlist/dict/purchases/attachments）以降低后续错拼风险。
- [ ] 保证图片上传走与 request 一致的 baseUrl 解析策略。
- [ ] 输出“接口映射表”到文档附录（本计划文档更新）。

**Verification:**
- [ ] Run: `npm run build:h5`（目录 `uniapp/digiLeder`）
- [ ] Run: `npm run build:mp-weixin`（目录 `uniapp/digiLeder`）
- [ ] 手测：心愿单列表可加载，新增成功，“我的”页心愿统计正确。

**Gate:** 若任一页面仍请求 `/wishlists`，Task 1 不通过。

---

### Task 2: Wishlist Convert Minimal-Field Flow (Backend-Compatible)

**Files:**
- Modify: `uniapp/digiLeder/src/pages/wishlist/list.vue`
- Modify: `uniapp/digiLeder/src/pages/wishlist/add.vue`
- Create: `uniapp/digiLeder/src/utils/wishlist-convert.js`

- [ ] 实现心愿项 -> 资产创建请求体构造器（最简字段：name/status/purchaseDate/targetCostValue/targetCostStrategy/purchases）。
- [ ] 转资产按钮改为传 body 到 `POST /wishlist/{id}/convert`。
- [ ] 增加失败提示（字段缺失、接口返回错误）并避免 silent fail。

**Verification:**
- [ ] Run: `npm run build:h5`
- [ ] 手测：任意心愿条目可成功转资产，资产列表出现新记录。
- [ ] 手测：缺少预算时依然能转（使用默认策略值）。

**Gate:** 转资产请求必须不再是空 body。

---

### Task 3: Wishlist Detail/Edit Page Closure

**Files:**
- Create: `uniapp/digiLeder/src/pages/wishlist/detail.vue`
- Modify: `uniapp/digiLeder/src/pages/wishlist/list.vue`
- Modify: `uniapp/digiLeder/src/pages.json`

- [ ] 新增心愿详情页，支持 `GET /wishlist/{id}` 展示。
- [ ] 同页支持编辑保存 `PUT /wishlist/{id}`。
- [ ] 列表项点击从 no-op 改为进入详情页。

**Verification:**
- [ ] Run: `npm run build:h5`
- [ ] 手测：列表 -> 详情 -> 编辑 -> 返回列表，数据正确刷新。

**Gate:** 列表点击必须有有效跳转且保存后数据可见。

---

### Task 4: Asset List Search & Multi-Filters

**Files:**
- Modify: `uniapp/digiLeder/src/pages/asset/list.vue`
- Create: `uniapp/digiLeder/src/components/filter-drawer.vue`
- Create: `uniapp/digiLeder/src/utils/dict-cache.js`

- [ ] 增加关键词搜索（q）。
- [ ] 增加分类/平台/标签筛选，接字典接口。
- [ ] 支持筛选条件组合查询并可一键重置。

**Verification:**
- [ ] Run: `npm run build:h5`
- [ ] 手测：组合筛选（状态+关键词+分类）结果正确。

**Gate:** 筛选参数必须反映到 API query。

---

### Task 5: Asset Detail Deep Management (Purchases)

**Files:**
- Modify: `uniapp/digiLeder/src/pages/asset/detail.vue`
- Create: `uniapp/digiLeder/src/components/purchase-editor-sheet.vue`

- [ ] 详情页新增购买记录区块。
- [ ] 支持新增/编辑/删除购买记录（`/api/purchases`）。
- [ ] 购买记录变更后回刷资产详情指标。

**Verification:**
- [ ] Run: `npm run build:h5`
- [ ] 手测：新增附属购买 -> 编辑 -> 删除全流程成功。

**Gate:** 购买记录操作后详情页展示与后端一致。

---

### Task 6: Attachment Management in Mobile

**Files:**
- Modify: `uniapp/digiLeder/src/pages/asset/detail.vue`
- Create: `uniapp/digiLeder/src/components/attachment-panel.vue`
- Modify: `uniapp/digiLeder/src/components/upload-image.vue`

- [ ] 增加附件列表展示（按 biz_type/biz_id）。
- [ ] 增加附件上传与删除。
- [ ] 统一上传错误提示及重试入口。

**Verification:**
- [ ] Run: `npm run build:h5`
- [ ] 手测：上传 1 张图，列表可见；删除后消失。

**Gate:** 附件 CRUD 全部可用。

---

### Task 7: Cover Suggestion & Apply

**Files:**
- Modify: `uniapp/digiLeder/src/pages/asset/detail.vue`
- Create: `uniapp/digiLeder/src/components/cover-suggestion-sheet.vue`

- [ ] 接入封面候选接口 `GET /assets/{id}/cover/suggestions`。
- [ ] 支持选择候选图并应用 `POST /assets/{id}/cover/from-url`。
- [ ] 应用成功后自动刷新当前详情封面。

**Verification:**
- [ ] Run: `npm run build:h5`
- [ ] 手测：推荐 -> 应用 -> 详情封面更新。

**Gate:** 必须存在“可见推荐列表 + 一键应用”。

---

### Task 8: Polishing, Regression & Delivery

**Files:**
- Modify: `uniapp/digiLeder/src/pages/my/index.vue`
- Modify: `uniapp/digiLeder/src/pages/settings/index.vue`
- Modify: `docs/user/BUG修复记录.md`
- Modify: `docs/user/需求变更记录.md`

- [ ] 全链路回归：资产、心愿、出售、设置、上传。
- [ ] 补齐错误态/空态/加载态文案一致性。
- [ ] 输出最终交付记录与已知限制。

**Verification:**
- [ ] Run: `npm run build:h5`
- [ ] Run: `npm run build:mp-weixin`
- [ ] 手测清单全部通过。

**Gate:** 两端构建通过 + 手测清单通过才可宣告完成。

---

## Risk Register

- 后端 `convert` 入参校验可能高于“最简字段”预期。
- 图片上传与附件上传并存时，objectKey/url 映射需统一。
- 小程序端样式与 H5 存在组件差异（需在 Task 8 统一回归）。

## Done Definition

- [ ] Task 1-8 全部完成且 Gate 全绿。
- [ ] uniapp 关键流程无 P0/P1 级阻塞。
- [ ] 文档与代码状态一致，可直接进入后续云端扩展迭代。

## Progress Log Template

- 日期：
- 完成 Task：
- 构建结果：
- 手测结果：
- 阻塞项：
- 下一步：


## Appendix: 接口映射表（2026-04-15）

| 模块 | 旧路径/调用 | 新路径/调用 | 说明 |
|---|---|---|---|
| 心愿单列表 | `/wishlists` | `/wishlist` | 单数路径统一 |
| 心愿单创建 | `POST /wishlists` | `POST /wishlist` | 单数路径统一 |
| 心愿单删除 | `DELETE /wishlists/{id}` | `DELETE /wishlist/{id}` | 单数路径统一 |
| 心愿转资产 | `POST /wishlists/{id}/convert`(空body) | `POST /wishlist/{id}/convert`(最简body) | 增加请求体构造器 |
| 字典 | 分散 hardcode | `/dict/categories|platforms|tags` | 增加缓存层 |
| 购买记录 | 无 | `/purchases` CRUD | 详情页可增删改 |
| 附件 | 上传后仅封面 | `/attachments` 列表/新增/删除 | 支持 bizType+bizId |
| 封面推荐 | 无 | `/assets/{id}/cover/suggestions` + `/cover/from-url` | 一键应用并刷新 |
