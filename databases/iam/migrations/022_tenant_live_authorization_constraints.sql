-- 正转换及verify成功、所有写入继续冻结后安装当前引用门禁；不直接执行于旧数据。
ALTER TABLE iam_role_assignment ADD CONSTRAINT ck_iam_assignment_current_role CHECK (
        (domain='PLATFORM' AND role_id IS NULL AND revision_id IS NOT NULL AND revision_kind IS NOT NULL) OR
        (domain='TENANT' AND role_id IS NOT NULL AND role_kind IS NOT NULL AND role_tenant_key IS NOT NULL AND
          ((role_kind='SHARED' AND role_tenant_key=0) OR
           (role_kind='TENANT_CUSTOM' AND role_tenant_key=tenant_id))) OR
        (domain='TENANT' AND status='REVOKED' AND role_id IS NULL AND revision_id IS NOT NULL));
