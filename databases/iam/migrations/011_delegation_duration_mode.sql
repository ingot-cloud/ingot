-- 已有 IAM 库升级；新建库使用更新后的 003，不重复执行本文件。
-- 发布顺序：本 DDL → 全部后端节点 → 前端。旧记录保持 LIMITED。
ALTER TABLE iam_delegation_grant
    DROP CHECK ck_iam_delegation_duration,
    ADD COLUMN assignment_duration_mode VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL DEFAULT 'LIMITED',
    MODIFY COLUMN max_assignment_duration_seconds BIGINT UNSIGNED NULL,
    ADD CONSTRAINT ck_iam_delegation_duration CHECK (
      max_assignment_duration_nanos < 1000000000 AND
      ((assignment_duration_mode='LIMITED' AND max_assignment_duration_seconds IS NOT NULL
        AND (max_assignment_duration_seconds > 0 OR max_assignment_duration_nanos > 0))
       OR (assignment_duration_mode='UNLIMITED' AND domain='PLATFORM'
        AND max_assignment_duration_seconds IS NULL AND max_assignment_duration_nanos=0)));
-- 回退前必须先调整/撤销 UNLIMITED 委派与长期派生记录，不得直接降级后端。
-- 核查历史自我分配（不删除）：
SELECT a.id,a.delegation_grant_id FROM iam_role_assignment a
JOIN iam_delegation_grant d ON d.id=a.delegation_grant_id
WHERE a.domain='PLATFORM' AND a.status='ACTIVE' AND
 (a.platform_member_id=d.platform_administrator_id OR EXISTS(
   SELECT 1 FROM iam_platform_group_member gm
   WHERE gm.group_id=a.platform_group_id AND gm.member_id=d.platform_administrator_id));
