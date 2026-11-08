-- 可选历史结构：仅供明确执行历史迁移/核对的环境使用，不属于新项目默认初始化。
-- 从 004_policy_audit_migration.sql 原样迁入，不包含数据、删表、选库或自动升级。
SET time_zone = '+00:00';

CREATE TABLE iam_live_authorization_batch (
 batch_id VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY COMMENT '迁移批次标识；引用归属由本表外键及业务事务共同校验',
 source_digest CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '即时授权迁移前的源内容摘要',
 reverse_digest CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NULL COMMENT '反向迁移核验摘要；尚未生成时为空',
 state VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '迁移方向状态：FORWARD正向、REVERSE反向；取值FORWARD,REVERSE',
 decisions JSON NOT NULL DEFAULT (JSON_ARRAY()) COMMENT '已确认迁移处置决策JSON数组',
 applied_at DATETIME(6) NULL COMMENT '迁移应用时间点，UTC；尚未应用时为空',
 CONSTRAINT ck_iam_live_batch_state CHECK(state IN ('FORWARD','REVERSE'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='租户即时授权模型迁移批次';

CREATE TABLE iam_live_authorization_mapping (
 batch_id VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '迁移批次标识；引用归属由本表外键及业务事务共同校验',
 entity_type VARCHAR(32) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '迁移记录对应的实体类型',
 entity_id BIGINT UNSIGNED NOT NULL COMMENT '迁移记录对应的实体标识',
 source_row JSON NOT NULL COMMENT '迁移前原行JSON快照，仅供受控迁移与回退使用',
 target_id BIGINT UNSIGNED NULL COMMENT '目标业务标识，具体类型由target_type或entity_type解释',
 PRIMARY KEY(batch_id,entity_type,entity_id),
 FOREIGN KEY(batch_id) REFERENCES iam_live_authorization_batch(batch_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='租户即时授权迁移原行快照与目标映射';
