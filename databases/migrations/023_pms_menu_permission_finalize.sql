-- ============================================================
-- PMS 菜单可见性收口：通配页关联叶子、删除 platform_menu.permission_id
-- 版本: V23.0
-- 日期: 2026-09-11
--
-- 前置：已执行 022 schema 与 022_data（@apply = 1）。
-- 本脚本可重复执行：关联 INSERT 带 NOT EXISTS；列不存在则跳过 DROP。
-- 通配页映射：优先 {ns}:query，否则已有 {ns}:view；二者都没有则保留待人工处理（见文末 SELECT）。
-- ============================================================

USE ingot_core;

-- ------------------------------------------------------------
-- 报告：仍挂在通配 GROUP 上、且没有 :query / :view 可复用的受保护页
-- ------------------------------------------------------------
SELECT 'WILDCARD_PAGE_NO_LEAF' AS reason, m.id AS menu_id, m.name, oldp.code AS old_code
FROM platform_menu m
JOIN information_schema.COLUMNS c
  ON c.TABLE_SCHEMA = DATABASE()
 AND c.TABLE_NAME = 'platform_menu'
 AND c.COLUMN_NAME = 'permission_id'
JOIN platform_permission oldp ON oldp.id = m.permission_id AND oldp.deleted_at IS NULL
WHERE m.deleted_at IS NULL
  AND m.menu_type = '1'
  AND m.access_mode = '1'
  AND oldp.code LIKE '%:**'
  AND NOT EXISTS (
      SELECT 1 FROM platform_menu_permission mp WHERE mp.menu_id = m.id
  )
  AND NOT EXISTS (
      SELECT 1 FROM platform_permission q
      WHERE q.deleted_at IS NULL
        AND q.node_type = '2'
        AND q.code NOT LIKE '%:*'
        AND q.code IN (
            CONCAT(LEFT(oldp.code, CHAR_LENGTH(oldp.code) - 2), 'query'),
            CONCAT(LEFT(oldp.code, CHAR_LENGTH(oldp.code) - 2), 'view')
        )
  );

-- ------------------------------------------------------------
-- 通配页关联已有叶子（列仍在时执行）
-- ------------------------------------------------------------
INSERT INTO platform_menu_permission (id, menu_id, permission_id)
SELECT (m.id + q.id) AS id, m.id, q.id
FROM platform_menu m
JOIN information_schema.COLUMNS c
  ON c.TABLE_SCHEMA = DATABASE()
 AND c.TABLE_NAME = 'platform_menu'
 AND c.COLUMN_NAME = 'permission_id'
JOIN platform_permission oldp ON oldp.id = m.permission_id AND oldp.deleted_at IS NULL
JOIN platform_permission q
  ON q.deleted_at IS NULL
 AND q.node_type = '2'
 AND q.code NOT LIKE '%:*'
 AND q.code = CONCAT(LEFT(oldp.code, CHAR_LENGTH(oldp.code) - 2), 'query')
WHERE m.deleted_at IS NULL
  AND m.menu_type = '1'
  AND m.access_mode = '1'
  AND oldp.code LIKE '%:**'
  AND NOT EXISTS (
      SELECT 1 FROM platform_menu_permission mp WHERE mp.menu_id = m.id
  );

INSERT INTO platform_menu_permission (id, menu_id, permission_id)
SELECT (m.id + v.id) AS id, m.id, v.id
FROM platform_menu m
JOIN information_schema.COLUMNS c
  ON c.TABLE_SCHEMA = DATABASE()
 AND c.TABLE_NAME = 'platform_menu'
 AND c.COLUMN_NAME = 'permission_id'
JOIN platform_permission oldp ON oldp.id = m.permission_id AND oldp.deleted_at IS NULL
JOIN platform_permission v
  ON v.deleted_at IS NULL
 AND v.node_type = '2'
 AND v.code NOT LIKE '%:*'
 AND v.code = CONCAT(LEFT(oldp.code, CHAR_LENGTH(oldp.code) - 2), 'view')
WHERE m.deleted_at IS NULL
  AND m.menu_type = '1'
  AND m.access_mode = '1'
  AND oldp.code LIKE '%:**'
  AND NOT EXISTS (
      SELECT 1 FROM platform_menu_permission mp WHERE mp.menu_id = m.id
  );

-- ------------------------------------------------------------
-- DROP 旧单关联列
-- ------------------------------------------------------------
SET @drop_permission_id = (
    SELECT IF(COUNT(*) > 0,
              'ALTER TABLE `platform_menu` DROP COLUMN `permission_id`',
              'SELECT 1')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'platform_menu'
      AND COLUMN_NAME = 'permission_id'
);
PREPARE drop_permission_id_stmt FROM @drop_permission_id;
EXECUTE drop_permission_id_stmt;
DEALLOCATE PREPARE drop_permission_id_stmt;
