-- 仅补齐租户成员显示名的实际筛选能力；不修改角色、分配、字段策略或历史默认版本。
-- 与包含 OrganizationMemberNameFilter 清单及姓名SQL校验的新版后端同批部署。
UPDATE iam_resource res
JOIN iam_application app ON app.id = res.application_id
JOIN JSON_TABLE(res.field_capabilities, '$[*]' COLUMNS (
    field_position FOR ORDINALITY,
    field_key VARCHAR(128) PATH '$.key'
)) AS capability ON capability.field_key = 'displayName'
SET res.field_capabilities = JSON_SET(res.field_capabilities,
        CONCAT('$[', capability.field_position - 1, '].filterable'), CAST('true' AS JSON)),
    res.version = res.version + 1
WHERE app.domain = 'TENANT' AND app.code = 'iam-tenant' AND res.code = 'member'
  AND COALESCE(JSON_EXTRACT(res.field_capabilities,
        CONCAT('$[', capability.field_position - 1, '].filterable')) = CAST('true' AS JSON), FALSE) = FALSE;
