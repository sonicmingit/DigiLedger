# DigiLedger 远程 MCP

MCP 提供与 DigiLedger Skill 对应的九个工具，直接调用相同的物品与字典服务。
传输采用 [MCP Streamable HTTP](https://modelcontextprotocol.io/specification/2025-11-25/basic/transports)：
无状态、单个 JSON-RPC 请求通过 HTTP POST 发送，返回 `application/json`。
无需安装 Python、Skill 或本地桥接进程。不提供旧版 HTTP+SSE 长连接。

## 配置连接

1. 在 PC 系统设置 → 开放 API / Skill / MCP 生成 Token，选择只读或读写权限，启用开放 API / MCP 并保存。
2. MCP 默认地址为 `https://你的系统地址/api/open/mcp`。可自定义域名、IP、端口和反向代理地址；自定义外部路径须由网关转发至 `/api/open/mcp`。
3. 在 MCP 客户端选择 HTTP / Streamable HTTP，填写地址，并配置 `Authorization: Bearer <Token>` 请求头。使用与 Skill 完全相同的 Token。

通用客户端配置示例（具体字段名称以客户端要求为准）：

```json
{
  "mcpServers": {
    "digiledger": {
      "type": "http",
      "url": "https://你的系统地址/api/open/mcp",
      "headers": {
        "Authorization": "Bearer <你的 Token>"
      }
    }
  }
}
```

两套 PC 设置页均可修改地址和复制配置。地址仅保存在当前浏览器，Token 不会写入浏览器持久存储。
重新生成 Token 会立即使 Skill 和 MCP 的旧 Token 失效；关闭开放 API 会同时停止两种访问。
本服务使用手动配置的 Bearer Token，不提供 OAuth 登录流程，客户端需要支持自定义认证头。

## 工具

| 工具 | 参数 | 权限 |
| --- | --- | --- |
| `list_assets` | 可选 `keyword`、`status`、`category_id`、`brand_id`、`platform_id`、`tag_ids`（整数数组）、`page`、`page_size` | 只读 |
| `get_asset` | `id` | 只读 |
| `list_categories` | 无 | 只读 |
| `list_brands` | 无 | 只读 |
| `list_platforms` | 无 | 只读 |
| `list_tags` | 无 | 只读 |
| `create_asset` | 与开放 API 新增物品的 JSON 字段相同，必填 `name`、`categoryId`、`status`，可含 `purchases` | 读写 |
| `update_asset` | `id`、`patch`，只修改 patch 中的主档字段 | 读写 |
| `delete_asset` | `id`，仅在用户明确要求删除时调用 | 读写 |

只读模式的 `tools/list` 返回六个查询工具；读写模式返回全部九个工具。
即使客户端缓存了工具，服务器仍逐次检查写权限。权限切换后请刷新客户端工具列表。
工具参数和字段约束通过 `tools/list` 的 JSON Schema 提供；业务规则详见同目录 [api.md](api.md)。
工具执行结果为 `content` 中的 JSON 文本；业务、校验或权限失败时 `isError: true`。
新增返回 `{"id":42}`；编辑和删除成功返回 `{"success":true}`。
写入没有幂等保证，超时后先查询实际状态，避免直接重试造成重复物品。

## 协议与部署

- 支持 `initialize`、`notifications/initialized`、`ping`、`tools/list`、`tools/call`。
- 支持协议版本 `2025-03-26`、`2025-06-18`、`2025-11-25`；客户端初始化协商版本后，通过 `MCP-Protocol-Version` 请求头发送该版本。
- POST 请求使用 `Content-Type: application/json`、`Accept: application/json, text/event-stream`；每次请求携带 Bearer Token。
- 通知返回 HTTP 202 空响应；请求返回 JSON-RPC 响应。解析或请求格式错误返回 HTTP 400；未知方法和工具返回 JSON-RPC 错误。
- 不创建会话，不要求 `MCP-Session-Id`。GET、DELETE 返回 HTTP 405，无需 SSE 通道或断线重放；不支持 JSON-RPC 批量请求。
- 缺失或错误 Token 返回 HTTP 401；接口关闭返回 HTTP 403。所有 MCP 响应禁止缓存。
- 浏览器跨域来源使用现有 `DL_CORS_ALLOWED_ORIGIN_PATTERNS` 配置，非法 Origin 返回 HTTP 403；无 Origin 的远程客户端可直接携带 Token 访问。

公网建议使用 HTTPS。网关需允许 `/api/open/mcp` 的 POST / OPTIONS 及 Authorization、MCP-Protocol-Version、Content-Type 请求头。
继续将管理页面和内部 `/api/settings/**` 等接口放在可信网络或已有网关认证之后；本 Token 只保护 `/api/open/**`。
自定义 MCP 地址不会修改服务监听端口或路由；通过反向代理映射公网地址即可。当前实例为共享账本。

本指南可通过 `/api/agent-resources/mcp.md` 获取，也包含在 Skill 下载包的 `references/mcp.md` 中。
