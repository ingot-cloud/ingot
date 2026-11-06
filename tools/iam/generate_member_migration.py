#!/usr/bin/env python3
"""从正式种子生成 App 用户管理的独立增量目录。"""

import hashlib
import re

from generate_bootstrap import (
    MEMBER_ACTIONS, MEMBER_APPLICATION, MEMBER_MENUS, ROOT, TARGET, json_literal, menu_path, quote,
)

MIGRATION = ROOT / 'databases/iam/migrations/036_member_platform_application.sql'


def build(check=False):
    """同步可预演的增量脚本，限制写入本应用的目录表。"""
    if MEMBER_APPLICATION == 'platform-member':
        # 旧目录增量属于历史版本，新目录使用完整初始化。
        expected = 'cf4fbb34dcbe051583901c47961dc8775d77f1b572b06bb569ab67384aeb55d1'
        if hashlib.sha256(MIGRATION.read_bytes()).hexdigest() != expected:
            raise SystemExit('历史036迁移摘要不一致')
        return
    app = quote(MEMBER_APPLICATION)
    _, root, _, pages = MEMBER_MENUS[0]
    actions = [{'resource': resource, 'code': code}
               for resource, codes in MEMBER_ACTIONS.items() for code in codes]
    menus = [{'route': root, 'path': menu_path(root), 'view': 'layout.main', 'kind': 'DIRECTORY',
              'actions': [code for _, _, codes in pages for code in codes]}]
    menus.extend({'route': key, 'path': menu_path(key), 'view': key, 'kind': 'PAGE', 'actions': codes}
                 for _, key, codes in pages)
    statements = re.findall(r'^INSERT INTO iam_(?:application|resource|action|menu|menu_action) \(.*?;\n',
                            TARGET.read_text(), re.MULTILINE | re.DOTALL)
    inserts = '\n'.join(statement for statement in statements if app in statement)
    sql = f"""-- App 用户管理：由 tools/iam/generate_member_migration.py 生成。
-- 停止目录写入并选择目标 IAM 库。SET @iam_member_dry_run=TRUE 可预演并回滚。
-- 客户端须在同一会话先执行 USE `ingot_iam`（按部署替换库名），或连接时传入 --database。
-- SELECT DATABASE() 应返回目标库名。若返回 NULL，首个 DROP PROCEDURE 会报 1046。
SET NAMES utf8mb4;
SET time_zone = '+00:00';

DROP PROCEDURE IF EXISTS iam_apply_member_catalog;
DELIMITER $$
CREATE PROCEDURE iam_apply_member_catalog()
BEGIN
  DECLARE app_id BIGINT UNSIGNED;
  DECLARE EXIT HANDLER FOR SQLEXCEPTION
  BEGIN
    ROLLBACK;
    RESIGNAL;
  END;
  START TRANSACTION;

  SELECT id INTO app_id FROM iam_application WHERE code = {app};
  IF EXISTS (SELECT 1 FROM iam_application WHERE id = app_id AND domain <> 'PLATFORM') THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'App 用户管理应用域冲突';
  END IF;
  IF EXISTS (
    SELECT 1 FROM iam_resource r
    WHERE r.application_id = app_id AND r.code IN ('user', 'role', 'permission')
      AND (r.scope_capabilities <> JSON_ARRAY('ALL') OR r.field_capabilities <> JSON_ARRAY()
           OR r.field_policy <> FALSE)
  ) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'App 用户管理资源能力冲突';
  END IF;
  IF EXISTS (
    SELECT 1 FROM JSON_TABLE({json_literal(actions)}, '$[*]'
      COLUMNS (code VARCHAR(128) PATH '$.code', resource_code VARCHAR(64) PATH '$.resource')) expected
    JOIN iam_action a ON a.code = expected.code
    JOIN iam_resource r ON r.application_id = a.application_id AND r.id = a.resource_id
    WHERE NOT (a.application_id <=> app_id) OR r.code <> expected.resource_code
  ) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'App 用户管理精确操作归属冲突';
  END IF;
  IF EXISTS (
    SELECT 1 FROM JSON_TABLE({json_literal(menus)}, '$[*]'
      COLUMNS (route_key VARCHAR(128) PATH '$.route', menu_path VARCHAR(512) PATH '$.path',
               view_key VARCHAR(256) PATH '$.view', menu_kind VARCHAR(16) PATH '$.kind')) expected
    JOIN iam_menu m ON m.route_name = expected.route_key OR m.path = expected.menu_path
      OR (expected.menu_kind = 'PAGE' AND m.view_path = expected.view_key)
    LEFT JOIN iam_menu parent ON parent.id = m.parent_id
    WHERE NOT (m.application_id <=> app_id AND m.route_name <=> expected.route_key
      AND m.path <=> expected.menu_path AND m.view_path <=> expected.view_key
      AND m.kind = expected.menu_kind AND m.access_mode = 'ACTION' AND m.match_mode = 'ANY'
      AND ((m.kind = 'DIRECTORY' AND m.parent_id IS NULL)
           OR (m.kind = 'PAGE' AND parent.route_name <=> {quote(root)})))
  ) OR EXISTS (
    SELECT 1 FROM iam_menu WHERE application_id = app_id
      AND route_name IN ({', '.join(quote(menu['route']) for menu in menus)})
    GROUP BY route_name HAVING COUNT(*) > 1
  ) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'App 用户管理菜单路径或归属冲突';
  END IF;
  IF EXISTS (
    SELECT 1 FROM JSON_TABLE({json_literal(menus)}, '$[*]'
      COLUMNS (route_key VARCHAR(128) PATH '$.route', action_codes JSON PATH '$.actions')) expected
    JOIN iam_menu m ON m.application_id = app_id AND m.route_name = expected.route_key
    JOIN iam_menu_action link ON link.application_id = m.application_id AND link.menu_id = m.id
    JOIN iam_action a ON a.application_id = link.application_id AND a.id = link.action_id
    WHERE NOT JSON_CONTAINS(expected.action_codes, JSON_QUOTE(a.code))
  ) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'App 用户管理菜单操作关联冲突';
  END IF;

{inserts}
  SELECT app.id AS application_id, app.code, menu.id AS menu_id, menu.route_name, menu.path,
         menu.icon, action.code AS access_action
  FROM iam_application app
  JOIN iam_menu menu ON menu.application_id = app.id
  JOIN iam_menu_action link ON link.application_id = app.id AND link.menu_id = menu.id
  JOIN iam_action action ON action.application_id = app.id AND action.id = link.action_id
  WHERE app.code = {app}
  ORDER BY menu.sort_order, menu.route_name, action.code;

  IF COALESCE(@iam_member_dry_run, FALSE) THEN
    ROLLBACK;
  ELSE
    COMMIT;
  END IF;
END$$
DELIMITER ;
CALL iam_apply_member_catalog();
DROP PROCEDURE iam_apply_member_catalog;
"""
    if check:
        if not MIGRATION.is_file() or MIGRATION.read_text() != sql:
            raise SystemExit('036 migration differs; run python3 tools/iam/generate_database.py')
    else:
        MIGRATION.write_text(sql)
