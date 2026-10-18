# 部署版本 2：后端 + fronten2.0

部署前必须在本地 `config.env` 中增加 `DL_AUTH_PASSWORD`（非空，不限制长度），没有默认密码；未设置时所有业务数据均拒绝访问。已有部署的 `config.env` 会保留，请手动补上该变量。

公网通过 HTTPS 访问，保持 `DL_AUTH_COOKIE_SECURE=true`；本机 HTTP 调试才设置为 `false`。登录后七天未操作会过期；退出、后端重启后需要重新登录。修改环境变量中的密码并重建后端即可重置密码，同时使全部旧会话失效。无需创建用户或迁移数据库。

PC 前端与 API 推荐同源反代。跨站 H5 / Capacitor Android 需要 `DL_AUTH_COOKIE_SAME_SITE=none`、`DL_AUTH_COOKIE_SECURE=true` 和 HTTPS，并通过 `DL_CORS_ALLOWED_ORIGIN_PATTERNS` 列出准确的客户端来源（例如 `https://localhost`）。客户端必须允许第三方 Cookie；如浏览器阻止跨站 Cookie，请改为同站部署 H5。

内置 Nginx 会保留访问域名、端口和协议，Compose 后端启用转发头解析。同源 PC 访问无需额外加入 CORS 白名单。使用外层 HTTPS 反代时，请将原始 `Host`（含非默认端口）传给前端容器，并覆盖设置 `X-Forwarded-Proto=https`。转发头由可信代理维护；后端端口应仅向可信客户端或代理开放。更新代理配置后需重建前后端两个容器。

对外 API / MCP 仍使用原有独立 Token。物品图片与附件的 `/oss/**` 入口同样需要登录，已有对象存储应保持私有，避免外部直链绕过本系统。

在服务器克隆仓库后运行（Linux/macOS）：

```bash
chmod +x deploy/deploy.sh deploy/v2/deploy.sh
./deploy/v2/deploy.sh --force
```

Windows PowerShell：

```powershell
.\deploy\v2\deploy.ps1 -Force
```

首次执行会从已提交的 `config.env.template` 和 `docker-compose.template.yml`
生成本地的 `config.env`、`docker-compose.yml`。请先填写外部 MySQL 与对象存储
的连接信息（`DL_DB_*`、`DL_STORAGE_*`），再使用 `--force` 部署。Compose 仅启动
后端与 `fronten2.0`，不会创建数据库、MinIO 或其他中间件。

`config.env` 是本地配置，脚本不会覆盖；`docker-compose.yml` 每次运行都会按仓库中
已提交的模板刷新，请将所有可配置项（端口、数据库、对象存储、Flyway）保存在
`config.env`。

日常更新只需运行 `./deploy/v2/deploy.sh`：脚本会先执行 `git pull --ff-only`，
检测到提交更新后询问是否重建并重启全部服务。需要无条件重建时使用 `--force`。

前端地址为 `http://<服务器地址>:<APP_PORT>`，后端接口为
`http://<服务器地址>:<BACKEND_PORT>`。
