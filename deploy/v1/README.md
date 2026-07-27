# 部署版本 1：后端 + frontend

在服务器克隆仓库后运行（Linux/macOS）：

```bash
chmod +x deploy/deploy.sh deploy/v1/deploy.sh
./deploy/v1/deploy.sh --force
```

Windows PowerShell：

```powershell
.\deploy\v1\deploy.ps1 -Force
```

首次执行会从已提交的 `config.env.template` 和 `docker-compose.template.yml`
生成本地的 `config.env`、`docker-compose.yml`。请先填写外部 MySQL 与对象存储
的连接信息（`DL_DB_*`、`DL_STORAGE_*`），再使用 `--force` 部署。Compose 仅启动
后端与 `frontend`，不会创建数据库、MinIO 或其他中间件。

`config.env` 是本地配置，脚本不会覆盖；`docker-compose.yml` 每次运行都会按仓库中
已提交的模板刷新，请将所有自定义连接信息保存在 `config.env`。

日常更新只需运行 `./deploy/v1/deploy.sh`：脚本会先执行 `git pull --ff-only`，
检测到提交更新后询问是否重建并重启全部服务。需要无条件重建时使用 `--force`。

前端地址为 `http://<服务器地址>:<APP_PORT>`，后端接口为
`http://<服务器地址>:<BACKEND_PORT>`。
