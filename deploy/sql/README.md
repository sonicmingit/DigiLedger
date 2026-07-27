# 旧版数据库脚本

该目录保留历史部署和人工升级脚本，仅供追溯旧环境使用。

从现在开始，所有数据库变更都应写入后端的 Flyway 迁移目录：

`backend/src/main/resources/db/migration/V{版本号}__{变更说明}.sql`

当前完整基线为 `V1__init.sql`。新增迁移从 `V2__...sql` 开始，版本号必须递增，已执行的迁移不得修改或删除。

历史环境若不使用 Flyway，并且已执行 `upgrade_figma_contract.sql`，可依次执行 `upgrade_route_rebuild_v1.sql` 与 `upgrade_route_wishlist_mainline_v2.sql` 补齐升级路线字段；新环境仍应优先由 Flyway 的 `V3__rebuild_upgrade_route.sql`、`V6__upgrade_route_wishlist_and_mainline.sql` 管理。
