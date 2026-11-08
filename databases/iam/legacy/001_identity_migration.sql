-- 可选历史结构：仅供明确执行历史迁移/核对的环境使用，不属于新项目默认初始化。
-- 从 004_policy_audit_migration.sql 原样迁入，不包含数据、删表、选库或自动升级。
SET time_zone = '+00:00';

CREATE TABLE iam_migration_batch (
    id VARCHAR(64) COLLATE utf8mb4_bin NOT NULL COMMENT '历史身份迁移批次及验收状态记录标识',
    source_fingerprint CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '源数据结构或内容指纹，用于识别迁移输入',
    target_identifier VARCHAR(128) COLLATE utf8mb4_bin NOT NULL COMMENT '明确选择的迁移目标标识，防止误用目标',
    rules_version VARCHAR(64) COLLATE utf8mb4_bin NOT NULL COMMENT '本批次采用的迁移规则版本',
    status VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL DEFAULT 'NEW' COMMENT '当前业务记录状态，合法值由本表CHECK及对应业务枚举限定；取值NEW,PREFLIGHTED,IMPORTED,VERIFIED,FAILED',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '记录创建时间点，UTC',
    verified_at DATETIME(6) NULL COMMENT '人工验收完成时间点，UTC；仅VERIFIED状态必填',
    PRIMARY KEY (id),
    CONSTRAINT ck_iam_batch_status CHECK (status IN ('NEW', 'PREFLIGHTED', 'IMPORTED', 'VERIFIED', 'FAILED')),
    CONSTRAINT ck_iam_batch_verified CHECK ((status = 'VERIFIED' AND verified_at IS NOT NULL) OR (status <> 'VERIFIED' AND verified_at IS NULL))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='历史身份迁移批次及验收状态';

CREATE TABLE iam_migration_mapping (
    batch_id VARCHAR(64) COLLATE utf8mb4_bin NOT NULL COMMENT '迁移批次标识；引用归属由本表外键及业务事务共同校验',
    source_table VARCHAR(64) COLLATE utf8mb4_bin NOT NULL COMMENT '原始来源表名',
    source_id VARCHAR(128) COLLATE utf8mb4_bin NOT NULL COMMENT '原始记录标识',
    target_type VARCHAR(64) COLLATE utf8mb4_bin NOT NULL COMMENT '审计或迁移目标的业务类型',
    target_id VARCHAR(128) COLLATE utf8mb4_bin NOT NULL COMMENT '目标业务标识，具体类型由target_type或entity_type解释',
    disposition VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '人工迁移处置：KEEP_MAPPED/NARROW/RECONFIGURE/ARCHIVE/SKIP；取值KEEP_MAPPED,NARROW,RECONFIGURE,ARCHIVE,SKIP',
    actor VARCHAR(128) NOT NULL COMMENT '作出处置的操作者标识',
    reason VARCHAR(1024) NOT NULL COMMENT '明确的人工处置依据',
    PRIMARY KEY (batch_id, source_table, source_id, target_type, target_id),
    CONSTRAINT fk_iam_mapping_batch FOREIGN KEY (batch_id) REFERENCES iam_migration_batch (id),
    CONSTRAINT ck_iam_mapping_disposition CHECK (disposition IN ('KEEP_MAPPED', 'NARROW', 'RECONFIGURE', 'ARCHIVE', 'SKIP'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='历史标识到目标实体的人工处置映射';

CREATE TABLE iam_migration_issue (
    id BIGINT UNSIGNED NOT NULL COMMENT '历史迁移阻塞项及处置记录记录标识',
    batch_id VARCHAR(64) COLLATE utf8mb4_bin NOT NULL COMMENT '迁移批次标识；引用归属由本表外键及业务事务共同校验',
    issue_code VARCHAR(64) COLLATE utf8mb4_bin NOT NULL COMMENT '迁移问题稳定代码',
    source_table VARCHAR(64) COLLATE utf8mb4_bin NULL COMMENT '原始来源表名',
    source_id VARCHAR(128) COLLATE utf8mb4_bin NULL COMMENT '原始记录标识',
    safe_description VARCHAR(1024) NOT NULL COMMENT '迁移问题的脱敏说明',
    disposition VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NULL COMMENT '人工迁移处置：KEEP_MAPPED/NARROW/RECONFIGURE/ARCHIVE/SKIP；取值KEEP_MAPPED,NARROW,RECONFIGURE,ARCHIVE,SKIP',
    resolved_by VARCHAR(128) NULL COMMENT '问题处置人；未处置时为空',
    resolution_reason VARCHAR(1024) NULL COMMENT '问题处置依据；未处置时为空',
    PRIMARY KEY (id),
    KEY idx_iam_issue_unresolved (batch_id, disposition, id),
    CONSTRAINT fk_iam_issue_batch FOREIGN KEY (batch_id) REFERENCES iam_migration_batch (id),
    CONSTRAINT ck_iam_issue_disposition CHECK (disposition IS NULL OR disposition IN ('KEEP_MAPPED', 'NARROW', 'RECONFIGURE', 'ARCHIVE', 'SKIP')),
    CONSTRAINT ck_iam_issue_resolution CHECK (
        (disposition IS NULL AND resolved_by IS NULL AND resolution_reason IS NULL) OR
        (disposition IS NOT NULL AND resolved_by IS NOT NULL AND resolution_reason IS NOT NULL))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='历史迁移阻塞项及处置记录';
