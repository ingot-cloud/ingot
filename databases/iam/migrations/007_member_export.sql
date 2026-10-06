-- 成员导出任务：共享库存储，供多实例读取状态、成员 ID 快照与过期清理。
CREATE TABLE iam_member_export (
    id BIGINT UNSIGNED NOT NULL,
    tenant_id BIGINT UNSIGNED NOT NULL,
    actor_member_id BIGINT UNSIGNED NOT NULL,
    tenant_version VARCHAR(32) COLLATE utf8mb4_bin NOT NULL,
    status VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    member_ids JSON NULL,
    failure_reason VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NULL,
    created_at DATETIME(6) NOT NULL,
    completed_at DATETIME(6) NULL,
    expires_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_iam_member_export_tenant_status (tenant_id, status, expires_at),
    CONSTRAINT ck_iam_member_export_status CHECK (status IN ('PENDING', 'RUNNING', 'SUCCEEDED', 'FAILED', 'EXPIRED')),
    CONSTRAINT ck_iam_member_export_ids CHECK (member_ids IS NULL OR JSON_TYPE(member_ids) = 'ARRAY')
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
