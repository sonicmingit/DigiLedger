# Figma Rebuild Deployment

新 PC/H5 使用独立镜像、容器名和端口，不覆盖原有 `frontend/`、
`uniapp/digiLeder/` 或根目录部署配置。

## 启动

后端默认按宿主机 `http://localhost:8080` 访问：

```powershell
docker compose -f deploy/figma/docker-compose.yml up --build
```

- PC：`http://localhost:5200`
- H5：`http://localhost:5201`

如后端地址或端口不同，可在启动前设置：

```powershell
$env:DL_API_UPSTREAM = "http://host.docker.internal:5101"
$env:DL_PC_PORT = "5200"
$env:DL_H5_PORT = "5201"
docker compose -f deploy/figma/docker-compose.yml up --build
```

`DL_API_UPSTREAM` 必须是后端源地址，不带 `/api`；Nginx 会原样代理
`/api/*` 请求。H5 设置页的主、备用地址仍保存在设备本地，默认主节点为
同源 `/api`。

## 单独构建

```powershell
docker build -f deploy/figma/pc.Dockerfile -t digiledger-figma-pc .
docker build -f deploy/figma/h5.Dockerfile -t digiledger-figma-h5 .
```
