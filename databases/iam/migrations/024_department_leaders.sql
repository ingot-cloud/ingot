-- 部门负责人独立关系；不回填角色，不修改分配，不重建业务库。
CREATE TABLE IF NOT EXISTS iam_department_leader (
    tenant_id BIGINT UNSIGNED NOT NULL,
    department_id BIGINT UNSIGNED NOT NULL,
    member_id BIGINT UNSIGNED NOT NULL,
    is_primary BOOLEAN NOT NULL DEFAULT FALSE,
    primary_department_id BIGINT UNSIGNED GENERATED ALWAYS AS (CASE WHEN is_primary = 1 THEN department_id ELSE NULL END) STORED,
    PRIMARY KEY (tenant_id, department_id, member_id),
    UNIQUE KEY uk_iam_department_primary_leader (tenant_id, primary_department_id),
    KEY idx_iam_member_led_departments (tenant_id, member_id, department_id),
    CONSTRAINT fk_iam_department_leader_department FOREIGN KEY (tenant_id, department_id) REFERENCES iam_department (tenant_id, id),
    CONSTRAINT fk_iam_department_leader_member FOREIGN KEY (tenant_id, member_id) REFERENCES iam_tenant_member (tenant_id, id),
    CONSTRAINT ck_iam_department_leader_primary CHECK (is_primary IN (0, 1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 仅注册操作目录，普通角色不自动获得操作。
INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120999 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120999)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-tenant:department:leaders', '设置部门负责人', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-tenant' AND res.code = 'department' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-tenant:department:leaders');
