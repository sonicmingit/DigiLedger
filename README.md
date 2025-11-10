# DigiLedger V0.3

DigiLedger 是一款面向个人、家庭与小团队的数码资产全生命周期管理系统。本仓库包含 **Spring Boot + MyBatis** 实现的后端服务与 **Vue 3 + Vite + Element Plus** 实现的前端界面，并提供一键启动的 Docker Compose（含 MinIO）编排。

## 核心特性

- 资产主档：记录设备品牌、型号、序列号、状态等基础信息，支持标签与封面上传。
- 购买记录：支持主购 / 配件 / 服务多类投入，自动汇总投入、日均成本与使用天数。
- 出售向导：一次性记录售出价格、费用与净收入，并联动资产状态。
- 心愿单：管理待购设备，可一键转为资产。
- 对象存储：提供 `/api/files/upload` 接口接入 MinIO，统一返回体 `{code,data,msg}`。
- 前端中台：仪表盘、资产列表与心愿单均可通过 Nginx 反代访问 `/api`。

## 目录结构

```
├── backend/                 # Spring Boot 后端工程
│   ├── src/main/java        # 核心代码（控制器、服务、Mapper、实体）
│   ├── src/main/resources   # MyBatis XML、配置文件、schema & seed SQL
│   ├── src/test/java        # 单元测试样例
│   └── Dockerfile           # 后端容器镜像构建文件
├── frontend/                # Vue3 前端项目
│   ├── src                  # 页面、组件与 API 封装
│   ├── public               # 静态资源
│   └── Dockerfile           # 前端容器镜像构建文件
├── deploy/nginx/nginx.conf  # Nginx 反向代理配置
├── docker-compose.yml       # 一键启动编排（MySQL + MinIO + 后端 + 前端 + Nginx）
├── README.md                # 项目说明
└── 设计文档.md               # 业务与架构设计说明
```

## 后端快速开始

### 本地开发

1. 安装 JDK 17 与 Maven。
2. 启动 MySQL 8，并导入 `backend/src/main/resources/sql/schema.sql` 与 `seed.sql`。
3. 修改 `backend/src/main/resources/application.yml` 中的数据源配置或通过环境变量覆盖。
4. 在 `backend/` 目录运行：

   ```bash
   mvn spring-boot:run
   ```

5. 默认监听端口为 `http://localhost:8080`，REST 接口返回统一结构 `{ code, data, msg }`，错误码对齐设计文档 5.8。

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

前端提供仪表盘、资产中心（含新增/编辑/出售向导）与心愿单管理页面，均已接入统一 API。

## Docker 部署

### 一体化启动

仓库根目录提供 `docker-compose.yml`，包含以下服务：

- `mysql`：MySQL 8.3，自动挂载 schema 与种子数据。
- `minio`：对象存储，端口 `9000/9001`，用于资产封面及附件上传。
- `backend`：Spring Boot 应用，读取环境变量连接数据库。
- `frontend`：Vue 编译产物，通过 `npx serve` 输出静态页面。
- `nginx`：统一暴露 80 端口，`/api` 反向代理至后端，`/` 转发到前端。

执行以下命令即可完成一体化启动：

```bash
docker compose up --build -d
```

启动完成后访问 `http://localhost` 即可浏览前端页面，后端接口通过 `http://localhost/api` 访问；MinIO 控制台位于 `http://localhost:9001`。

### 单独部署后端

```bash
cd backend
docker build -t digiledger-backend:latest .
docker run -d --name digiledger-backend \
  -e DL_DB_HOST=<mysql-host> \
  -e DL_DB_PORT=3306 \
  -e DL_DB_NAME=digiledger \
  -e DL_DB_USER=root \
  -e DL_DB_PASS=root \
  -p 8080:8080 \
  digiledger-backend:latest
```

### 单独部署前端

```bash
cd frontend
docker build -t digiledger-frontend:latest .
docker run -d --name digiledger-frontend \
  -e VITE_API_BASE=/api \
  -p 4173:4173 \
  digiledger-frontend:latest
```

可搭配任意 Nginx/Traefik 代理将 `/api` 指向后端服务。

## 环境变量一览

| 变量名                | 默认值        | 说明                           |
|-----------------------|---------------|--------------------------------|
| `DL_DB_HOST`          | mysql         | 后端连接的 MySQL 主机          |
| `DL_DB_PORT`          | 3306          | 后端连接的 MySQL 端口          |
| `DL_DB_NAME`          | digiledger    | 数据库名称                     |
| `DL_DB_USER`          | root          | 数据库用户名                   |
| `DL_DB_PASS`          | root          | 数据库密码                     |
| `DL_SERVER_PORT`      | 8080          | Spring Boot 服务端口           |
| `DL_STORAGE_PROVIDER` | minio         | 对象存储提供方                 |
| `DL_STORAGE_ENDPOINT` | http://minio:9000 | 对象存储内部访问地址      |
| `DL_STORAGE_BUCKET`   | digiledger    | 上传使用的桶名称               |
| `DL_STORAGE_ACCESS_KEY` | minioadmin  | 存储 AccessKey                |
| `DL_STORAGE_SECRET_KEY` | minioadmin  | 存储 SecretKey                |
| `DL_STORAGE_BASE_URL` | http://localhost:9000 | 文件访问基础 URL   |
| `VITE_API_BASE`       | /api          | 前端访问后端的基础路径         |

## 下一步计划

- 补充认证授权、多用户权限模型。
- 增强导入导出能力，支持常见电商账单。
- 构建统计报表与仪表盘，支持多维分析。

更多业务细节及错误码定义可参考 `设计文档.md` 与 `deploy/openapi.yaml`。
