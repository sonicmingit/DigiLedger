# DigiLedger 对外 API v1

用于 Agent 和第三方客户端查询、创建、编辑物品。基地址：`https://你的系统地址/api/open/v1`。
所有请求携带 `Authorization: Bearer <Token>`；JSON 请求使用 `Content-Type: application/json`。

## 开通与安装

1. PC 系统设置 → 开放 API / Agent Skill，生成 Token。完整 Token 只在生成时显示，数据库只保存 SHA-256 摘要；重新生成后旧 Token 立即失效。
2. 默认只读；需要新增、编辑、删除时选择读写权限，启用开放 API 并保存。
3. 下载 `/api/agent-resources/digiledger-skill.zip`，解压得到 `digiledger` 文件夹，安装到支持 SKILL.md 的 Agent 技能目录。Codex 的用户技能目录为 `~/.agents/skills/digiledger/`（Windows 为 `%USERPROFILE%\.agents\skills\digiledger\`）。确保该目录内直接包含 `SKILL.md`，重新加载技能或重启 Agent。
4. 在 Agent 运行环境设置 `DIGILEDGER_API_URL` 与 `DIGILEDGER_API_TOKEN`，并安装 Python 3.9+。脚本无需第三方依赖。

PowerShell 当前会话示例（从此会话启动 Agent 才能继承环境变量）：

```powershell
$env:DIGILEDGER_API_URL = 'https://你的系统地址/api/open/v1'
$env:DIGILEDGER_API_TOKEN = '<在本机填入 Token>'
python "$env:USERPROFILE/.agents/skills/digiledger/scripts/digiledger.py" list --keyword 相机
```

macOS / Linux 当前会话示例：

```bash
export DIGILEDGER_API_URL='https://你的系统地址/api/open/v1'
export DIGILEDGER_API_TOKEN='<在本机填入 Token>'
python3 ~/.agents/skills/digiledger/scripts/digiledger.py list --keyword 相机
```

Codex 安装目录依据 [官方技能说明](https://learn.chatgpt.com/docs/build-skills)。如果使用自定义技能搜索目录，请以宿主 Agent 的配置为准。

技能本身不携带凭据。下载文档和技能包无需 Token。

## 接口清单

以下路径均相对于基地址。

| 方法 | 路径 | 权限 | 用途 |
| --- | --- | --- | --- |
| GET | `/assets` | 只读 | 分页查询物品 |
| GET | `/assets/{id}` | 只读 | 物品详情，包含购买、出售、标签与成本 |
| POST | `/assets` | 读写 | 新增物品，可同时创建购买记录 |
| PATCH | `/assets/{id}` | 读写 | 局部编辑物品主档 |
| DELETE | `/assets/{id}` | 读写 | 删除物品，保留已有业务冲突限制 |
| GET | `/dict/categories/tree` | 只读 | 分类树 |
| GET | `/dict/brands` | 只读 | 品牌列表 |
| GET | `/dict/platforms` | 只读 | 购买/出售平台列表 |
| GET | `/dict/tags/tree` | 只读 | 标签树 |

### 查询

`GET /assets?keyword=相机&page=1&page_size=20`

支持 `keyword`、`status`、`category_id`、`brand_id`、`platform_id`、`tag_ids`（逗号分隔，如 `1,2`）。
`page` 默认 1，范围 1–1000000；`page_size` 默认 20，范围 1–100。按购买日期倒序。

成功返回 `{"code":200,"data":{"records":[],"total":0,"page":1,"pageSize":20},"msg":"OK"}`。
`records` 为摘要，查询详情取得完整字段。完整清单需按 `total` 继续分页。

### 新增

`POST /assets`，返回新物品 ID，例如 `{"code":200,"data":42,"msg":"OK"}`。

| 字段 | 类型 | 要求 |
| --- | --- | --- |
| name | string | 必填，1–200 字 |
| categoryId | integer | 必填，从分类树取得真实 ID |
| status | string | 必填：使用中、已闲置、待出售、已出售、已丢弃 |
| brandId / brand | integer / string | 可选，字典品牌 ID / 自定义品牌名称（最多 100 字） |
| model / serialNo | string | 可选，各最多 200 字 |
| specifications / notes | string | 可选，配置规格 / 备注 |
| purchaseDate / retiredDate | string | 可选，YYYY-MM-DD；退役日期不得早于购买日期 |
| coverImageUrl | string | 可选，最多 500 字；现有封面地址或对象存储 key |
| relatedLinks | array | 可选，最多 20 条 `{url,description}`；url 必填最多 2000 字，description 最多 200 字 |
| manualUseMonths | integer | 可选，非负 |
| tagIds | integer[] | 可选，从标签树取得真实 ID |
| purchases | array | 可选，新增时同时录入购买记录 |

购买记录：`type`、`price`、`purchaseDate` 必填。`type` 为 `PRIMARY`（主购）、`ACCESSORY`（配件）、`SERVICE`（服务）；配件/服务还需 `name`。`price` 非负；日期为 `YYYY-MM-DD`。
可选 `platformId`、`seller`（最多 200 字）、`shippingCost`（非负）、`quantity`（至少 1，默认 1）、`warrantyMonths`（非负）、`warrantyExpireDate`、`productLink`（最多 1000 字）、`attachments`（字符串数组，每项最多 500 字）、`notes`。
配件不保存独立质保信息。主购记录会决定物品的购买日期。

示例中的 `categoryId` 必须替换为系统实际 ID；价格和日期仅在用户已提供时提交：

```json
{
  "name": "索尼 A7C II",
  "categoryId": 3,
  "brand": "索尼",
  "model": "ILCE-7CM2",
  "status": "使用中",
  "specifications": "银色机身",
  "purchases": [{"type":"PRIMARY","price":12999,"purchaseDate":"2026-10-09","quantity":1}]
}
```

没有价格或购买日期时可以只创建主档，不要伪造购买记录。

### 局部编辑

`PATCH /assets/42`，例如 `{"notes":"放在书房","serialNo":"ABC123"}`。

只提交要改的字段；允许新增表中的主档字段，但不允许 `purchases`、`id`、成本或出售记录等字段。未知字段报错。
未提交字段、标签、链接和购买记录均保留；显式 `null` 清空可空字段，`tagIds: []` 和 `relatedLinks: []` 清空相应集合；必填字段不能清空。
继承系统状态规则：将状态设为“已丢弃”会清理出售记录；此操作应由用户明确要求。
存在主购记录时购买日期以主购记录为准。编辑后 GET 详情核验。

### 删除

`DELETE /assets/42`。仅当用户明确要求删除时执行。存在出售记录或非唯一主购记录时返回 409；唯一主购记录会与物品一同删除。

## 响应与错误

统一响应 `{code,data,msg}`。成功 `code=200`；无数据响应会省略 `data`。
鉴权失败使用 HTTP 401；开放接口关闭或只读 Token 写入使用 HTTP 403。
业务和字段校验沿用系统响应（HTTP 200，需检查 `code`）：400 参数错误，404 物品不存在，409 日期或关联记录冲突，500 内部错误。
无效 JSON 等错误应修正请求；不要只看 HTTP 状态判断成功。
写请求无幂等保证，也无自动重试。超时后先查询是否已成功，再决定是否重试，避免重复物品。

## 部署边界

本 Token 仅保护 `/api/open/**`，不会自动给现有管理页面和 `/api/settings/**` 等内部接口增加登录。
实例目前是共享账本，Token 可查询该实例的全部物品；读写权限可修改全部物品，不提供多用户隔离。
公网接入应使用 HTTPS；通过网关把现有 PC 管理页面和内部 `/api/**` 路由保留在可信网络或已有认证之后，仅向 Agent 开放 `/api/open/v1/**`。下载资源 `/api/agent-resources/**` 可公开。
Agent 使用 Token 不能变更 Token 设置或启用权限；这些由 PC 系统设置管理。

机器可读文档：`/api/agent-resources/openapi.json`。本指南在 `backend/src/main/resources/agent-skill/digiledger/references/api.md` 维护，同时随技能包发布。
