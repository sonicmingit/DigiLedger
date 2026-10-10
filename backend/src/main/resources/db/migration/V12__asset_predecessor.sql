ALTER TABLE device_asset
    ADD COLUMN predecessor_asset_id BIGINT NULL COMMENT '同类别的上代物品ID',
    ADD INDEX idx_asset_predecessor (predecessor_asset_id),
    ADD CONSTRAINT fk_asset_predecessor FOREIGN KEY (predecessor_asset_id)
        REFERENCES device_asset (id) ON DELETE SET NULL;
