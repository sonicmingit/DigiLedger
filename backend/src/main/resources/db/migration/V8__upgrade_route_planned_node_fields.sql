-- 兼容已在 Flyway 纳管前创建的历史数据库：这些字段原先只存在于初始化脚本中，
-- 因基线版本设为 V1 而不会补建，导致创建含计划节点的路线时插入失败。
ALTER TABLE equip_upgrade_node
    ADD COLUMN title VARCHAR(200) NULL COMMENT '计划标题' AFTER remark,
    ADD COLUMN target_name VARCHAR(200) NULL COMMENT '目标物品名称' AFTER title,
    ADD COLUMN period_label VARCHAR(50) NULL COMMENT '计划周期标签' AFTER target_name,
    ADD COLUMN planned_budget DECIMAL(12,2) NULL COMMENT '计划预算' AFTER period_label,
    ADD COLUMN expected_recovery DECIMAL(12,2) NULL COMMENT '预计回收' AFTER planned_budget,
    ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'PLANNED' COMMENT '计划状态' AFTER expected_recovery;
