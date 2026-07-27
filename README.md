# DigiLedger V0.3.4

DigiLedger 是一款面向个人、家庭与小团队的数码物品（后端沿用 `asset` 命名）全生命周期管理系统。本仓库包含 **Spring Boot + MyBatis** 实现的后端服务与 **Vue 3 + Vite + Element Plus** 实现的前端界面，并提供一键部署的 Docker Compose 编排。

## 核心特性

- 资产总览：全新“资产总览”页面呈现在库/总数、总投入与 30 天趋势，并提供按类别切换与快捷创建入口。
- 物品主档：记录设备品牌、型号、序列号、状态等基础信息，支持多级类别、标签与封面上传。
- 字典中心：系统设置页面统一维护 **物品类别（树形）**、**来源/出售平台** 与 **标签（树形 + 颜色/图标）**，前端自动联动。
- 购买记录：支持主购 / 配件 / 服务多类投入，支持平台字典选择，自动汇总投入、日均成本与使用天数。
- 出售向导：一次性记录售出平台、价格、费用与净收入，并联动物品状态。
- 心愿单：管理待购设备，支持图片、标签、状态筛选，可一键标记“已购买”并转为物品。
- 对象存储：提供 `/api/files/upload` 接口接入 MinIO，统一返回体 `{code,data,msg}`。
- 前端中台：仪表盘、物品列表与心愿单均可通过 Nginx 反代访问 `/api`。

## 目录结构

```
├── backend/                 # Spring Boot 后端工程
│   ├── src/main/java        # 核心代码（控制器、服务、Mapper、实体）
│   ├── src/main/resources   # MyBatis XML、配置文件、Flyway 迁移脚本
│   ├── src/test/java        # 单元测试样例
│   └── Dockerfile           # 后端容器镜像构建文件
├── frontend/                # Vue3 前端项目（部署版本 1）
│   ├── src                  # 页面、组件与 API 封装
│   ├── public               # 静态资源
├── frontend-figma/          # Figma 重构前端（部署版本 2）
├── deploy/v1/               # 后端 + frontend 一键部署
├── deploy/v2/               # 后端 + frontend-figma 一键部署
├── README.md                # 项目说明
└── 设计文档.md               # 业务与架构设计说明
```

## 后端快速开始

### 本地开发

1. 安装 JDK 17 与 Maven。
2. 启动 MySQL 8；后端启动时会自动执行 Flyway 数据库迁移，初始化基线为 `backend/src/main/resources/db/migration/V1__init.sql`。
3. 修改 `backend/src/main/resources/application.yml` 中的数据源配置或通过环境变量覆盖。
4. 在 `backend/` 目录运行：

   ```bash
   mvn spring-boot:run
   ```

5. 默认监听端口为 `http://localhost:8080`，REST 接口返回统一结构 `{ code, data, msg }`，错误码对齐设计文档 5.8。

后续任何数据库结构或初始数据调整，都必须新增版本化脚本，例如 `backend/src/main/resources/db/migration/V2__add_example_table.sql`。已执行的迁移不得修改或删除。

### 外接服务配置

MT Photos、Bing 图片搜索、Google CSE 与 remove.bg 的地址、密钥和超时均保存在 `external_api_config` 表，不再从 `application.yml` 读取。首次启动会执行 `V2__seed_external_api_configs.sql` 创建默认禁用记录；在“系统设置 → 外接 API”中保存凭据并启用后，可直接进行搜索或配置测试。

### 单元测试

```bash
cd backend
mvn test
```

## 前端快速开始

1. 安装 Node.js 18+ 与 npm。
2. 在 `frontend/` 目录运行：

   ```bash
   npm install
   npm run dev
   ```

3. Vite 开发服务器默认运行在 `http://localhost:5173`，会通过代理将 `/api` 请求转发至 `http://localhost:8080`。

4. 生产构建：

```bash
npm run build
npm run preview
```

前端提供资产总览、物品中心（含新增/编辑/出售向导、卡片视图、批量标签）与心愿单管理页面，均已接入统一 API 与系统字典。

> 📸 前端新界面截图占位：请在实际部署后补充最新截图。

## API & 调试工具

- OpenAPI 规范：`deploy/openapi.yaml`
- Postman 集合：`deploy/postman/DigiLedger.postman_collection.json`（覆盖字典、物品创建/查询等核心流程）

## Docker 部署

部署资料集中在 `deploy/`，两套方案均会编译后端和对应前端源码，并启动
后端与前端反向代理；MySQL、对象存储等依赖由外部服务提供：

- 版本 1：`deploy/v1`，后端 + `frontend`。
- 版本 2：`deploy/v2`，后端 + `frontend-figma`。

在已克隆的仓库中选择一个版本执行：

```bash
chmod +x deploy/deploy.sh deploy/v1/deploy.sh
./deploy/v1/deploy.sh --force
```

Windows PowerShell 可执行：

```powershell
.\deploy\v1\deploy.ps1 -Force
```

首次执行会生成不提交到 Git 的 `config.env` 和 `docker-compose.yml`；两者的
模板均已提交。填写生成的 `config.env` 中的外部数据库与对象存储连接信息后，
使用 `--force` 完成首次部署。日常执行部署脚本时会先更新 Git 代码；发现更新
后会询问是否重新构建部署。详细说明见 [版本 1](deploy/v1/README.md) 与
[版本 2](deploy/v2/README.md)。

部署脚本会保留本地 `config.env`，并在每次执行时从已提交的模板刷新
`docker-compose.yml`，使编排更新能自动生效。

## 环境变量一览

| 变量名                | 默认值        | 说明                           |
|-----------------------|---------------|--------------------------------|
| `DL_DB_HOST`          | -             | 外部 MySQL 主机                |
| `DL_DB_PORT`          | 3306          | 后端连接的 MySQL 端口          |
| `DL_DB_NAME`          | digiledger    | 数据库名称                     |
| `DL_DB_USER`          | -             | 外部数据库用户名               |
| `DL_DB_PASS`          | -             | 外部数据库密码                 |
| `DL_SERVER_PORT`      | 8080          | Spring Boot 服务端口           |
| `DL_STORAGE_PROVIDER` | minio         | 对象存储提供方                 |
| `DL_STORAGE_ENDPOINT` | -             | 外部对象存储服务地址           |
| `DL_STORAGE_BUCKET`   | digiledger    | 上传使用的桶名称               |
| `DL_STORAGE_ACCESS_KEY` | -           | 存储 AccessKey                |
| `DL_STORAGE_SECRET_KEY` | -           | 存储 SecretKey                |
| `DL_STORAGE_BASE_URL` | -             | 对象存储的公网访问基础 URL     |
| `DL_BING_IMAGE_API_KEY` | (空)        | Bing 图片搜索 API Key，用于智能找图 |

## 下一步计划

- 补充认证授权、多用户权限模型。
- 增强导入导出能力，支持常见电商账单。
- 构建统计报表与仪表盘，支持多维分析。

更多业务细节及错误码定义可参考 `设计文档.md` 与 `deploy/openapi.yaml`。
