# 部署版本 2：后端 + frontend-figma

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
生成本地的 `config.env`、`docker-compose.yml`。请先在 `config.env` 修改密码和
`DL_STORAGE_BASE_URL` 中的主机名，再次使用 `--force` 部署。

日常更新只需运行 `./deploy/v2/deploy.sh`：脚本会先执行 `git pull --ff-only`，
检测到提交更新后询问是否重建并重启全部服务。需要无条件重建时使用 `--force`。

前端地址为 `http://<服务器地址>:<APP_PORT>`，MinIO 控制台为
`http://<服务器地址>:<MINIO_CONSOLE_PORT>`。
