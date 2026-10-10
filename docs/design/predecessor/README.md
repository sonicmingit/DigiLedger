# 上代产品 · 第一版

`reference.png` 是用户选定的第一版设计；正式实现使用物品数据库中的封面。`assets/` 下的生成图片仅用于本地演示，不进入前端生产包。

## 本地演示

在仓库根目录启动独立的内存数据服务：

```powershell
node docs/design/predecessor/preview-server.mjs
```

在另一个终端启动原有 PC 应用，临时将开发代理指向演示服务：

```powershell
cd fronten2.0
$env:VITE_PROXY_TARGET='http://127.0.0.1:18081'
npm run dev -- --host 127.0.0.1
```

访问 `http://127.0.0.1:5174/assets/1`。演示数据仅在内存保存，重启演示服务即可恢复。正式开发或部署使用原后端地址，不设置这个演示代理。

## 正式部署

后端增加 `V12__asset_predecessor.sql`，由既有 Flyway 配置执行。升级后端后再部署新的 PC 前端。当前本地未连接真实数据库，迁移与真实数据库读写尚未执行。

视觉检查记录见仓库根目录 `design-qa.md`。
