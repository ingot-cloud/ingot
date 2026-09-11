-- ============================================================
-- PMS RBAC 数据映射 dry-run / apply
-- 版本: V22.0-data
-- 日期: 2026-09-10
--
-- 使用：
--   1. 先执行本文件「报告」段，确认 8 类阻断查询均为空。
--   2. 将 @apply 改为 1 后执行「改写」段。禁止在阻断未清零时 apply。
--   3. 不猜测修复；歧义行必须人工处理后重跑报告。
--   4. 通配页优先关联已有 {ns}:query，否则已有 {ns}:view；二者都没有时不要猜测新建。
--      映射收口并 DROP platform_menu.permission_id 见 023_pms_menu_permission_finalize.sql。
-- Java 规则对照：AuthorizationMigrationAnalyzer
-- ============================================================

USE ingot_core;

-- 0 = dry-run（只跑报告），1 = 执行改写
SET @apply = 0;

-- ------------------------------------------------------------
-- 报告：阻断查询，任一行非空即禁止 apply
-- ------------------------------------------------------------

-- DUPLICATE_DEPT_BINDING
SELECT 'DUPLICATE_DEPT_BINDING' AS reason,
       tenant_id, user_id, platform_role, role_id, IFNULL(dept_id, 0) AS dept_id_uk, COUNT(*) AS cnt
FROM tenant_role_user_private
GROUP BY tenant_id, user_id, platform_role, role_id, IFNULL(dept_id, 0)
HAVING COUNT(*) > 1;

-- DANGLING_RELATION / CROSS_TENANT_DEPT（任职）
SELECT 'DANGLING_OR_CROSS_DEPT' AS reason, ru.id, ru.tenant_id, ru.dept_id, d.tenant_id AS dept_tenant_id
FROM tenant_role_user_private ru
LEFT JOIN tenant_dept d ON d.id = ru.dept_id AND d.deleted_at IS NULL
WHERE ru.dept_id IS NOT NULL
  AND (d.id IS NULL OR d.tenant_id <> ru.tenant_id);

-- MISSING_APP_ID
SELECT 'MISSING_APP_ID' AS reason, 'menu' AS object_type, id
FROM platform_menu
WHERE deleted_at IS NULL AND app_id IS NULL
UNION ALL
SELECT 'MISSING_APP_ID', 'permission', id
FROM platform_permission
WHERE deleted_at IS NULL AND app_id IS NULL;

-- UNKNOWN_SOURCE：菜单 permission_id 悬空
SELECT 'UNKNOWN_SOURCE' AS reason, m.id AS menu_id, m.permission_id
FROM platform_menu m
LEFT JOIN platform_permission p ON p.id = m.permission_id AND p.deleted_at IS NULL
WHERE m.deleted_at IS NULL
  AND m.permission_id IS NOT NULL
  AND m.permission_id <> 0
  AND p.id IS NULL;

-- UNKNOWN_SOURCE：MENU 托管权限对不上菜单
SELECT 'UNKNOWN_SOURCE' AS reason, p.id AS permission_id, p.source_id
FROM platform_permission p
LEFT JOIN platform_menu m ON m.id = p.source_id AND m.deleted_at IS NULL
WHERE p.deleted_at IS NULL
  AND p.source_type = '1'
  AND (p.source_id IS NULL OR m.id IS NULL OR (m.permission_id <> p.id
       AND NOT EXISTS (
           SELECT 1 FROM platform_app a
           WHERE a.id = m.app_id AND a.permission_id = p.id AND a.deleted_at IS NULL
       )));

-- WILDCARD_CODE_COLLISION：:* 转为 :** 后撞码
SELECT 'WILDCARD_CODE_COLLISION' AS reason, p.id, p.code
FROM platform_permission p
JOIN platform_permission other
  ON other.deleted_at IS NULL
 AND other.id <> p.id
 AND other.code = CONCAT(LEFT(p.code, CHAR_LENGTH(p.code) - 1), '**')
WHERE p.deleted_at IS NULL
  AND p.code LIKE '%:*'
  AND p.code NOT LIKE '%:**';

-- VIEW_CODE_COLLISION：通配页面需要 {ns}:view 但编码已存在
SELECT 'VIEW_CODE_COLLISION' AS reason, m.id AS menu_id,
       CONCAT(LEFT(p.code, CHAR_LENGTH(p.code) - 2), 'view') AS view_code
FROM platform_menu m
JOIN platform_permission p ON p.id = m.permission_id AND p.deleted_at IS NULL
JOIN platform_permission existed
  ON existed.deleted_at IS NULL
 AND existed.code = CONCAT(LEFT(p.code, CHAR_LENGTH(p.code) - 2), 'view')
WHERE m.deleted_at IS NULL
  AND m.menu_type = '1'
  AND m.access_mode = '1'
  AND (p.code LIKE '%:**' OR (p.code LIKE '%:*' AND p.code NOT LIKE '%:**'));

-- PLATFORM_CUSTOM_TENANT_DEPT：平台 CUSTOM 部门无法拆到唯一租户
SELECT 'PLATFORM_CUSTOM_TENANT_DEPT' AS reason, r.id AS role_id
FROM platform_role r
WHERE r.deleted_at IS NULL
  AND r.scope_type = 1
  AND r.scopes IS NOT NULL
  AND r.scopes <> '[]'
  AND (
      SELECT COUNT(DISTINCT d.tenant_id)
      FROM tenant_dept d
      WHERE d.deleted_at IS NULL
        AND JSON_CONTAINS(CAST(r.scopes AS JSON), CAST(d.id AS JSON), '$')
  ) <> 1;

-- ------------------------------------------------------------
-- 改写：仅当 @apply = 1 且上述查询为空时执行
-- ------------------------------------------------------------

-- 既有应用默认开放
UPDATE platform_app
SET default_access_mode = '0'
WHERE @apply = 1
  AND (default_access_mode IS NULL OR default_access_mode = '');

-- 旧 :* -> :**（不覆盖已存在编码，撞码由报告阻断）
UPDATE platform_permission p
LEFT JOIN platform_permission other
  ON other.deleted_at IS NULL
 AND other.id <> p.id
 AND other.code = CONCAT(LEFT(p.code, CHAR_LENGTH(p.code) - 1), '**')
SET p.code = CONCAT(LEFT(p.code, CHAR_LENGTH(p.code) - 1), '**'),
    p.updated_at = NOW()
WHERE @apply = 1
  AND p.deleted_at IS NULL
  AND p.code LIKE '%:*'
  AND p.code NOT LIKE '%:**'
  AND other.id IS NULL;

-- 精确 NAVIGATION -> ACTION，并解除菜单托管
UPDATE platform_permission
SET node_type = '2',
    managed = b'0',
    source_type = '2',
    source_id = NULL,
    updated_at = NOW()
WHERE @apply = 1
  AND deleted_at IS NULL
  AND node_type = '1'
  AND code NOT LIKE '%:*'
  AND code NOT LIKE '%:**';

-- 通配 NAVIGATION -> GROUP
UPDATE platform_permission
SET node_type = '0',
    managed = b'0',
    source_type = '2',
    source_id = NULL,
    updated_at = NOW()
WHERE @apply = 1
  AND deleted_at IS NULL
  AND node_type = '1'
  AND (code LIKE '%:**' OR (code LIKE '%:*' AND code NOT LIKE '%:**'));

-- 受保护页面（非目录、非按钮）关联旧精确 ACTION
INSERT INTO platform_menu_permission (id, menu_id, permission_id)
SELECT (m.id + p.id) AS id, m.id, m.permission_id
FROM platform_menu m
JOIN platform_permission p ON p.id = m.permission_id AND p.deleted_at IS NULL
WHERE @apply = 1
  AND m.deleted_at IS NULL
  AND m.menu_type = '1'
  AND m.access_mode = '1'
  AND m.permission_id IS NOT NULL
  AND m.permission_id <> 0
  AND p.node_type = '2'
  AND NOT EXISTS (
      SELECT 1 FROM platform_menu_permission mp
      WHERE mp.menu_id = m.id AND mp.permission_id = m.permission_id
  );

-- 通配页优先复用同命名空间已有 ACTION {ns}:query（菜单可引用 read）
INSERT INTO platform_menu_permission (id, menu_id, permission_id)
SELECT (m.id + q.id) AS id, m.id, q.id
FROM platform_menu m
JOIN platform_permission oldp ON oldp.id = m.permission_id AND oldp.deleted_at IS NULL
JOIN platform_permission q
  ON q.deleted_at IS NULL
 AND q.node_type = '2'
 AND q.code NOT LIKE '%:*'
 AND q.code = CONCAT(LEFT(oldp.code, CHAR_LENGTH(oldp.code) - 2), 'query')
WHERE @apply = 1
  AND m.deleted_at IS NULL
  AND m.menu_type = '1'
  AND m.access_mode = '1'
  AND oldp.code LIKE '%:**'
  AND NOT EXISTS (
      SELECT 1 FROM platform_menu_permission mp WHERE mp.menu_id = m.id
  );

-- 无 :query 时复用已有 ACTION {ns}:view
INSERT INTO platform_menu_permission (id, menu_id, permission_id)
SELECT (m.id + v.id) AS id, m.id, v.id
FROM platform_menu m
JOIN platform_permission oldp ON oldp.id = m.permission_id AND oldp.deleted_at IS NULL
JOIN platform_permission v
  ON v.deleted_at IS NULL
 AND v.node_type = '2'
 AND v.code NOT LIKE '%:*'
 AND v.code = CONCAT(LEFT(oldp.code, CHAR_LENGTH(oldp.code) - 2), 'view')
WHERE @apply = 1
  AND m.deleted_at IS NULL
  AND m.menu_type = '1'
  AND m.access_mode = '1'
  AND oldp.code LIKE '%:**'
  AND NOT EXISTS (
      SELECT 1 FROM platform_menu_permission mp WHERE mp.menu_id = m.id
  );

-- 删除按钮伪路由，保留其权限绑定
UPDATE platform_menu
SET deleted_at = NOW(),
    updated_at = NOW()
WHERE @apply = 1
  AND deleted_at IS NULL
  AND menu_type = '9';

-- 新增资源与数据规则管理操作（挂应用根 GROUP；持有 :** 的角色动态获得）
INSERT INTO platform_permission (
    id, app_id, pid, name, code, type, node_type, source_type, source_id, managed, org_type, status, remark, created_at, updated_at, deleted_at)
SELECT * FROM (
    SELECT 1275201000000000001 AS id, 1246850914065547265 AS app_id, 1246850914036187138 AS pid,
           '资源查询' AS name, 'platform:config:app:resource:query' AS code, '1' AS type, '2' AS node_type,
           '2' AS source_type, NULL AS source_id, b'0' AS managed, '0' AS org_type, '0' AS status, '' AS remark,
           NOW() AS created_at, NOW() AS updated_at, NULL AS deleted_at
    UNION ALL SELECT 1275201000000000002, 1246850914065547265, 1246850914036187138, '资源创建', 'platform:config:app:resource:create', '1', '2', '2', NULL, b'0', '0', '0', '', NOW(), NOW(), NULL
    UNION ALL SELECT 1275201000000000003, 1246850914065547265, 1246850914036187138, '资源更新', 'platform:config:app:resource:update', '1', '2', '2', NULL, b'0', '0', '0', '', NOW(), NOW(), NULL
    UNION ALL SELECT 1275201000000000004, 1246850914065547265, 1246850914036187138, '资源删除', 'platform:config:app:resource:delete', '1', '2', '2', NULL, b'0', '0', '0', '', NOW(), NOW(), NULL
    UNION ALL SELECT 1275201000000000005, 1246850914065547265, 1246850914036187138, '数据规则查询', 'platform:config:role:data-rule:query', '1', '2', '2', NULL, b'0', '0', '0', '', NOW(), NOW(), NULL
    UNION ALL SELECT 1275201000000000006, 1246850914065547265, 1246850914036187138, '数据规则设置', 'platform:config:role:data-rule:set', '1', '2', '2', NULL, b'0', '0', '0', '', NOW(), NOW(), NULL
    UNION ALL SELECT 1275201000000000007, 1247138809458483202, 1247139048185683970, '数据规则查询', 'org:contacts:role:data-rule:query', '1', '2', '2', NULL, b'0', '1', '0', '', NOW(), NOW(), NULL
    UNION ALL SELECT 1275201000000000008, 1247138809458483202, 1247139048185683970, '数据规则设置', 'org:contacts:role:data-rule:set', '1', '2', '2', NULL, b'0', '1', '0', '', NOW(), NOW(), NULL
) seed
WHERE @apply = 1
  AND NOT EXISTS (
      SELECT 1 FROM platform_permission p WHERE p.id = seed.id OR p.code = seed.code
  );
