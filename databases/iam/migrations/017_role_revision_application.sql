-- 角色版本信息与持久分批应用，MySQL8.0.16+。明确选择IAM库后执行，可重复运行。
-- 发布顺序：增量数据库 -> 全部后端 -> 前端。回退前停止任务领取，保留表列及审计。
-- 不改写历史权限、分配、版本号或版本ID；已完成应用通过新任务恢复。
SET time_zone = '+00:00';
DROP PROCEDURE IF EXISTS ingot_iam_role_application_017;
DELIMITER $$
CREATE PROCEDURE ingot_iam_role_application_017()
BEGIN
    IF DATABASE() IS NULL OR NOT EXISTS(SELECT 1 FROM information_schema.tables
        WHERE table_schema=DATABASE() AND table_name='iam_role_revision') THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Select existing IAM database before migration017';
    END IF;
    IF NOT EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='iam_role_revision' AND column_name='version_name') THEN
        ALTER TABLE iam_role_revision ADD COLUMN version_name VARCHAR(128) NULL;
    END IF;
    IF NOT EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='iam_role_revision' AND column_name='remark') THEN
        ALTER TABLE iam_role_revision ADD COLUMN remark VARCHAR(512) NULL;
    END IF;
    IF NOT EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='iam_role_revision' AND column_name='metadata_version') THEN
        ALTER TABLE iam_role_revision ADD COLUMN metadata_version BIGINT UNSIGNED NOT NULL DEFAULT 0;
    END IF;
    IF NOT EXISTS(SELECT 1 FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='iam_role_assignment' AND index_name='ix_iam_assignment_revision_status') THEN
        ALTER TABLE iam_role_assignment ADD INDEX ix_iam_assignment_revision_status(revision_id,status,id);
    END IF;
END$$
DELIMITER ;
CALL ingot_iam_role_application_017();
DROP PROCEDURE ingot_iam_role_application_017;

-- 持久角色版本应用：平台域，明细不展开组成员。
CREATE TABLE IF NOT EXISTS iam_role_application_task (
    id BIGINT UNSIGNED NOT NULL PRIMARY KEY,
    role_id BIGINT UNSIGNED NOT NULL,
    target_revision_id BIGINT UNSIGNED NOT NULL,
    actor_account_id BIGINT UNSIGNED NOT NULL,
    actor_member_id BIGINT UNSIGNED NOT NULL,
    created_by BIGINT UNSIGNED NOT NULL,
    selection VARCHAR(32) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    selection_snapshot JSON NOT NULL,
    status VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    version BIGINT UNSIGNED NOT NULL DEFAULT 0,
    prepare_key VARCHAR(128) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    confirm_key VARCHAR(128) CHARACTER SET ascii COLLATE ascii_bin NULL,
    lease_owner VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NULL,
    lease_until DATETIME(6) NULL,
    failure_reason VARCHAR(512) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    running_role_id BIGINT UNSIGNED GENERATED ALWAYS AS (CASE WHEN status='RUNNING' THEN role_id ELSE NULL END) STORED,
    UNIQUE KEY uk_iam_role_application_prepare(role_id,prepare_key),
    UNIQUE KEY uk_iam_role_application_running(running_role_id),
    KEY ix_iam_role_application_role(role_id,id),
    KEY ix_iam_role_application_claim(status,lease_until,id),
    CONSTRAINT fk_iam_role_application_role FOREIGN KEY(role_id) REFERENCES iam_role_definition(id),
    CONSTRAINT fk_iam_role_application_target FOREIGN KEY(target_revision_id) REFERENCES iam_role_revision(id),
    CONSTRAINT ck_iam_role_application_status CHECK(status IN ('PREPARING','READY','BLOCKED','RUNNING','PAUSED','SUCCEEDED')),
    CONSTRAINT ck_iam_role_application_selection CHECK(selection IN ('ALL','SOURCE_REVISIONS','ASSIGNMENTS'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS iam_role_application_item (
    id BIGINT UNSIGNED NOT NULL PRIMARY KEY,
    task_id BIGINT UNSIGNED NOT NULL,
    assignment_id BIGINT UNSIGNED NOT NULL,
    original_snapshot JSON NOT NULL,
    expected_assignment_version BIGINT UNSIGNED NOT NULL,
    configured_bindings JSON NOT NULL,
    preview_snapshot JSON NULL,
    status VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    failure_reason VARCHAR(512) NULL,
    UNIQUE KEY uk_iam_role_application_assignment(task_id,assignment_id),
    KEY ix_iam_role_application_items(task_id,status,id),
    CONSTRAINT fk_iam_role_application_item_task FOREIGN KEY(task_id) REFERENCES iam_role_application_task(id),
    CONSTRAINT fk_iam_role_application_item_assignment FOREIGN KEY(assignment_id) REFERENCES iam_role_assignment(id),
    CONSTRAINT ck_iam_role_application_item_status CHECK(status IN ('PENDING','BLOCKED','SUCCEEDED','EXCLUDED','REVOKED','EXPIRED','ALREADY_TARGET'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
