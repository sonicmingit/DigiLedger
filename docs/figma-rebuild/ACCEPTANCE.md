# Figma 重构验收清单

## 隔离性

- [x] 当前分支为 `codex/figma-rebuild-pc-h5`。
- [x] 旧 `frontend/` 与 `uniapp/digiLeder/` 无改动。
- [x] 新 PC 和 H5 使用独立目录、依赖与构建产物。

## PC

- [x] 总览、物品中心、详情、心愿单、升级路线、系统设置六个路由可访问。
- [x] 真实 API 的加载、空态、失败态可验证。
- [x] 新增/编辑、状态、购买、出售、上传、心愿和字典核心流程可操作。
- [x] TypeScript 检查与生产构建通过。
- [x] 1440 × 900 截图与 Figma 对照复核。

## H5 / Android

- [x] 七个 Figma 页面和四项底部导航可访问。
- [x] 393 × 852、安全区和 48px 触控标准通过视觉复核。
- [x] 主、备用服务器可保存、测试和手动切换。
- [x] GET/HEAD 故障转移规则和写请求不重放规则有测试或可复现验证。
- [x] H5 构建通过。
- [x] app-android 编译通过；正式 APK 所需包名/签名项明确记录。

## 后端

- [x] 现有接口保持兼容。
- [x] Dashboard、心愿价格、升级计划、偏好、导出接口符合 API 契约。
- [x] SQL 迁移可重复执行或明确版本前置条件。
- [x] OpenAPI 同步。
- [x] `mvn test` 通过。

## 集成

- [x] PC、H5 使用同一字段定义和状态映射。
- [x] 构建产物不写入旧前端目录。
- [x] 独立部署说明和端口约定完整。
- [x] Git 变更仅包含本次授权范围。

## 验证记录（2026-07-22）

- PC：`npm run build` 通过；六张 1440 × 900 页面截图逐张复核。
- H5：`npm run build:h5` 通过；七张 393 × 852 页面截图逐张复核。
- Android 资源：`npm run build:app-android` 通过，输出 `uniapp-figma/dist/build/app`。
- 后端：`mvn test` 通过，3 个测试、0 失败、0 错误。
- OpenAPI：YAML 解析通过，OpenAPI 3.0.3，共 29 个路径。
- 部署：`deploy/v2/docker-compose.template.yml` 为后端 + `frontend-figma` 的统一部署模板；使用
  `deploy/v2/deploy.sh --force` 或 `deploy/v2/deploy.ps1 -Force` 执行部署。
  当前未启动，因此本轮未执行镜像构建。
- 实库迁移与真机 APK 发布未在本机执行；正式发布仍需数据库备份、正式包名、
  DCloud AppID、签名证书和真机网络/权限验证。
