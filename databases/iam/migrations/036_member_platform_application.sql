-- App 用户管理：由 tools/iam/generate_member_migration.py 生成。
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

  SELECT id INTO app_id FROM iam_application WHERE code = 'platform:member';
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
    SELECT 1 FROM JSON_TABLE(CAST('[{"resource": "user", "code": "platform:member:user:query"}, {"resource": "user", "code": "platform:member:user:detail"}, {"resource": "user", "code": "platform:member:user:create"}, {"resource": "user", "code": "platform:member:user:update"}, {"resource": "user", "code": "platform:member:user:delete"}, {"resource": "user", "code": "platform:member:user:reset-pwd"}, {"resource": "user", "code": "platform:member:user:roles:query"}, {"resource": "user", "code": "platform:member:user:roles:set"}, {"resource": "user", "code": "platform:member:user:lock"}, {"resource": "role", "code": "platform:member:role:query"}, {"resource": "role", "code": "platform:member:role:create"}, {"resource": "role", "code": "platform:member:role:update"}, {"resource": "role", "code": "platform:member:role:delete"}, {"resource": "role", "code": "platform:member:role:user:assign"}, {"resource": "role", "code": "platform:member:role:permissions:query"}, {"resource": "role", "code": "platform:member:role:permissions:set"}, {"resource": "permission", "code": "platform:member:permission:query"}, {"resource": "permission", "code": "platform:member:permission:create"}, {"resource": "permission", "code": "platform:member:permission:update"}, {"resource": "permission", "code": "platform:member:permission:delete"}]' AS JSON), '$[*]'
      COLUMNS (code VARCHAR(128) PATH '$.code', resource_code VARCHAR(64) PATH '$.resource')) expected
    JOIN iam_action a ON a.code = expected.code
    JOIN iam_resource r ON r.application_id = a.application_id AND r.id = a.resource_id
    WHERE NOT (a.application_id <=> app_id) OR r.code <> expected.resource_code
  ) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'App 用户管理精确操作归属冲突';
  END IF;
  IF EXISTS (
    SELECT 1 FROM JSON_TABLE(CAST('[{"route": "member.root", "path": "/member", "view": "layout.main", "kind": "DIRECTORY", "actions": ["platform:member:user:query", "platform:member:role:query", "platform:member:permission:query"]}, {"route": "member.user", "path": "/member/users", "view": "member.user", "kind": "PAGE", "actions": ["platform:member:user:query"]}, {"route": "member.role", "path": "/member/roles", "view": "member.role", "kind": "PAGE", "actions": ["platform:member:role:query"]}, {"route": "member.permission", "path": "/member/permissions", "view": "member.permission", "kind": "PAGE", "actions": ["platform:member:permission:query"]}]' AS JSON), '$[*]'
      COLUMNS (route_key VARCHAR(128) PATH '$.route', menu_path VARCHAR(512) PATH '$.path',
               view_key VARCHAR(256) PATH '$.view', menu_kind VARCHAR(16) PATH '$.kind')) expected
    JOIN iam_menu m ON m.route_name = expected.route_key OR m.path = expected.menu_path
      OR (expected.menu_kind = 'PAGE' AND m.view_path = expected.view_key)
    LEFT JOIN iam_menu parent ON parent.id = m.parent_id
    WHERE NOT (m.application_id <=> app_id AND m.route_name <=> expected.route_key
      AND m.path <=> expected.menu_path AND m.view_path <=> expected.view_key
      AND m.kind = expected.menu_kind AND m.access_mode = 'ACTION' AND m.match_mode = 'ANY'
      AND ((m.kind = 'DIRECTORY' AND m.parent_id IS NULL)
           OR (m.kind = 'PAGE' AND parent.route_name <=> 'member.root')))
  ) OR EXISTS (
    SELECT 1 FROM iam_menu WHERE application_id = app_id
      AND route_name IN ('member.root', 'member.user', 'member.role', 'member.permission')
    GROUP BY route_name HAVING COUNT(*) > 1
  ) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'App 用户管理菜单路径或归属冲突';
  END IF;
  IF EXISTS (
    SELECT 1 FROM JSON_TABLE(CAST('[{"route": "member.root", "path": "/member", "view": "layout.main", "kind": "DIRECTORY", "actions": ["platform:member:user:query", "platform:member:role:query", "platform:member:permission:query"]}, {"route": "member.user", "path": "/member/users", "view": "member.user", "kind": "PAGE", "actions": ["platform:member:user:query"]}, {"route": "member.role", "path": "/member/roles", "view": "member.role", "kind": "PAGE", "actions": ["platform:member:role:query"]}, {"route": "member.permission", "path": "/member/permissions", "view": "member.permission", "kind": "PAGE", "actions": ["platform:member:permission:query"]}]' AS JSON), '$[*]'
      COLUMNS (route_key VARCHAR(128) PATH '$.route', action_codes JSON PATH '$.actions')) expected
    JOIN iam_menu m ON m.application_id = app_id AND m.route_name = expected.route_key
    JOIN iam_menu_action link ON link.application_id = m.application_id AND link.menu_id = m.id
    JOIN iam_action a ON a.application_id = link.application_id AND a.id = link.action_id
    WHERE NOT JSON_CONTAINS(expected.action_codes, JSON_QUOTE(a.code))
  ) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'App 用户管理菜单操作关联冲突';
  END IF;

INSERT INTO iam_application (id, code, domain, name, icon, description, sort_order, baseline, enabled)
SELECT COALESCE((SELECT 100005 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_application taken WHERE taken.id = 100005)), (SELECT COALESCE(MAX(taken.id), 100000) + 1 FROM iam_application taken WHERE taken.id >= 100000 AND taken.id < 110000)), 'platform:member', 'PLATFORM', 'App 用户管理', 'ep:iphone', '管理 App 用户、角色及权限', 4, FALSE, TRUE FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM iam_application WHERE code = 'platform:member');

INSERT INTO iam_resource (id, application_id, code, name, scope_capabilities, field_capabilities, field_policy, enabled)
SELECT COALESCE((SELECT 110040 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_resource taken WHERE taken.id = 110040)), (SELECT COALESCE(MAX(taken.id), 110000) + 1 FROM iam_resource taken WHERE taken.id >= 110000 AND taken.id < 120000)), app.id, 'permission', 'App 权限', CAST('["ALL"]' AS JSON), CAST('[]' AS JSON), FALSE, TRUE FROM iam_application app
WHERE app.code = 'platform:member' AND NOT EXISTS (SELECT 1 FROM iam_resource WHERE application_id = app.id AND code = 'permission');

INSERT INTO iam_resource (id, application_id, code, name, scope_capabilities, field_capabilities, field_policy, enabled)
SELECT COALESCE((SELECT 110041 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_resource taken WHERE taken.id = 110041)), (SELECT COALESCE(MAX(taken.id), 110000) + 1 FROM iam_resource taken WHERE taken.id >= 110000 AND taken.id < 120000)), app.id, 'role', 'App 角色', CAST('["ALL"]' AS JSON), CAST('[]' AS JSON), FALSE, TRUE FROM iam_application app
WHERE app.code = 'platform:member' AND NOT EXISTS (SELECT 1 FROM iam_resource WHERE application_id = app.id AND code = 'role');

INSERT INTO iam_resource (id, application_id, code, name, scope_capabilities, field_capabilities, field_policy, enabled)
SELECT COALESCE((SELECT 110042 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_resource taken WHERE taken.id = 110042)), (SELECT COALESCE(MAX(taken.id), 110000) + 1 FROM iam_resource taken WHERE taken.id >= 110000 AND taken.id < 120000)), app.id, 'user', 'App 用户', CAST('["ALL"]' AS JSON), CAST('[]' AS JSON), FALSE, TRUE FROM iam_application app
WHERE app.code = 'platform:member' AND NOT EXISTS (SELECT 1 FROM iam_resource WHERE application_id = app.id AND code = 'user');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120153 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120153)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'platform:member:permission:create', '新增权限', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'platform:member' AND res.code = 'permission' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'platform:member:permission:create');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120154 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120154)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'platform:member:permission:delete', '删除权限', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'platform:member' AND res.code = 'permission' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'platform:member:permission:delete');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120155 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120155)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'platform:member:permission:query', '查看权限', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'platform:member' AND res.code = 'permission' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'platform:member:permission:query');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120156 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120156)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'platform:member:permission:update', '编辑或启停权限', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'platform:member' AND res.code = 'permission' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'platform:member:permission:update');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120157 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120157)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'platform:member:role:create', '新增角色', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'platform:member' AND res.code = 'role' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'platform:member:role:create');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120158 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120158)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'platform:member:role:delete', '删除角色', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'platform:member' AND res.code = 'role' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'platform:member:role:delete');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120159 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120159)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'platform:member:role:permissions:query', '查看角色权限', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'platform:member' AND res.code = 'role' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'platform:member:role:permissions:query');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120160 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120160)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'platform:member:role:permissions:set', '配置角色权限', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'platform:member' AND res.code = 'role' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'platform:member:role:permissions:set');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120161 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120161)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'platform:member:role:query', '查看角色', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'platform:member' AND res.code = 'role' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'platform:member:role:query');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120162 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120162)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'platform:member:role:update', '编辑或启停角色', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'platform:member' AND res.code = 'role' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'platform:member:role:update');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120163 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120163)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'platform:member:role:user:assign', '分配角色用户', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'platform:member' AND res.code = 'role' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'platform:member:role:user:assign');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120164 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120164)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'platform:member:user:create', '新增用户', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'platform:member' AND res.code = 'user' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'platform:member:user:create');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120165 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120165)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'platform:member:user:delete', '删除用户', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'platform:member' AND res.code = 'user' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'platform:member:user:delete');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120166 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120166)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'platform:member:user:detail', '查看用户详情', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'platform:member' AND res.code = 'user' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'platform:member:user:detail');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120167 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120167)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'platform:member:user:lock', '锁定或解锁用户', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'platform:member' AND res.code = 'user' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'platform:member:user:lock');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120168 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120168)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'platform:member:user:query', '查看用户列表', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'platform:member' AND res.code = 'user' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'platform:member:user:query');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120169 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120169)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'platform:member:user:reset-pwd', '重置用户密码', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'platform:member' AND res.code = 'user' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'platform:member:user:reset-pwd');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120170 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120170)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'platform:member:user:roles:query', '查看用户角色', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'platform:member' AND res.code = 'user' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'platform:member:user:roles:query');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120171 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120171)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'platform:member:user:roles:set', '配置用户角色', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'platform:member' AND res.code = 'user' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'platform:member:user:roles:set');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120172 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120172)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'platform:member:user:update', '编辑或启停用户', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'platform:member' AND res.code = 'user' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'platform:member:user:update');

INSERT INTO iam_menu (id, application_id, parent_id, name, path, view_path, route_name, icon, kind, match_mode, access_mode, sort_order, enabled)
SELECT COALESCE((SELECT 130038 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_menu taken WHERE taken.id = 130038)), (SELECT COALESCE(MAX(taken.id), 130000) + 1 FROM iam_menu taken WHERE taken.id >= 130000 AND taken.id < 140000)), app.id, NULL, 'App 用户管理', '/member', 'layout.main', 'member.root', 'ep:iphone', 'DIRECTORY', 'ANY', 'ACTION', 1, TRUE FROM iam_application app
WHERE app.code = 'platform:member' AND NOT EXISTS (SELECT 1 FROM iam_menu WHERE application_id = app.id AND route_name = 'member.root');

INSERT INTO iam_menu (id, application_id, parent_id, name, path, view_path, route_name, icon, kind, match_mode, access_mode, sort_order, enabled)
SELECT COALESCE((SELECT 130039 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_menu taken WHERE taken.id = 130039)), (SELECT COALESCE(MAX(taken.id), 130000) + 1 FROM iam_menu taken WHERE taken.id >= 130000 AND taken.id < 140000)), parent.application_id, parent.id, '用户管理', '/member/users', 'member.user', 'member.user', 'ep:user', 'PAGE', 'ANY', 'ACTION', 1, TRUE FROM iam_menu parent JOIN iam_application app ON app.id = parent.application_id
WHERE app.code = 'platform:member' AND parent.route_name = 'member.root' AND NOT EXISTS (SELECT 1 FROM iam_menu WHERE application_id = parent.application_id AND route_name = 'member.user');

INSERT INTO iam_menu (id, application_id, parent_id, name, path, view_path, route_name, icon, kind, match_mode, access_mode, sort_order, enabled)
SELECT COALESCE((SELECT 130040 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_menu taken WHERE taken.id = 130040)), (SELECT COALESCE(MAX(taken.id), 130000) + 1 FROM iam_menu taken WHERE taken.id >= 130000 AND taken.id < 140000)), parent.application_id, parent.id, '角色管理', '/member/roles', 'member.role', 'member.role', 'ep:avatar', 'PAGE', 'ANY', 'ACTION', 2, TRUE FROM iam_menu parent JOIN iam_application app ON app.id = parent.application_id
WHERE app.code = 'platform:member' AND parent.route_name = 'member.root' AND NOT EXISTS (SELECT 1 FROM iam_menu WHERE application_id = parent.application_id AND route_name = 'member.role');

INSERT INTO iam_menu (id, application_id, parent_id, name, path, view_path, route_name, icon, kind, match_mode, access_mode, sort_order, enabled)
SELECT COALESCE((SELECT 130041 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_menu taken WHERE taken.id = 130041)), (SELECT COALESCE(MAX(taken.id), 130000) + 1 FROM iam_menu taken WHERE taken.id >= 130000 AND taken.id < 140000)), parent.application_id, parent.id, '权限管理', '/member/permissions', 'member.permission', 'member.permission', 'ep:key', 'PAGE', 'ANY', 'ACTION', 3, TRUE FROM iam_menu parent JOIN iam_application app ON app.id = parent.application_id
WHERE app.code = 'platform:member' AND parent.route_name = 'member.root' AND NOT EXISTS (SELECT 1 FROM iam_menu WHERE application_id = parent.application_id AND route_name = 'member.permission');

INSERT INTO iam_menu_action (application_id, menu_id, action_id)
SELECT menu.application_id, menu.id, action.id FROM iam_menu menu JOIN iam_application app ON app.id = menu.application_id JOIN iam_action action ON action.application_id = menu.application_id
WHERE app.code = 'platform:member' AND menu.route_name = 'member.root' AND action.code = 'platform:member:permission:query' AND NOT EXISTS (SELECT 1 FROM iam_menu_action WHERE application_id = menu.application_id AND menu_id = menu.id AND action_id = action.id);

INSERT INTO iam_menu_action (application_id, menu_id, action_id)
SELECT menu.application_id, menu.id, action.id FROM iam_menu menu JOIN iam_application app ON app.id = menu.application_id JOIN iam_action action ON action.application_id = menu.application_id
WHERE app.code = 'platform:member' AND menu.route_name = 'member.root' AND action.code = 'platform:member:role:query' AND NOT EXISTS (SELECT 1 FROM iam_menu_action WHERE application_id = menu.application_id AND menu_id = menu.id AND action_id = action.id);

INSERT INTO iam_menu_action (application_id, menu_id, action_id)
SELECT menu.application_id, menu.id, action.id FROM iam_menu menu JOIN iam_application app ON app.id = menu.application_id JOIN iam_action action ON action.application_id = menu.application_id
WHERE app.code = 'platform:member' AND menu.route_name = 'member.root' AND action.code = 'platform:member:user:query' AND NOT EXISTS (SELECT 1 FROM iam_menu_action WHERE application_id = menu.application_id AND menu_id = menu.id AND action_id = action.id);

INSERT INTO iam_menu_action (application_id, menu_id, action_id)
SELECT menu.application_id, menu.id, action.id FROM iam_menu menu JOIN iam_application app ON app.id = menu.application_id JOIN iam_action action ON action.application_id = menu.application_id
WHERE app.code = 'platform:member' AND menu.route_name = 'member.user' AND action.code = 'platform:member:user:query' AND NOT EXISTS (SELECT 1 FROM iam_menu_action WHERE application_id = menu.application_id AND menu_id = menu.id AND action_id = action.id);

INSERT INTO iam_menu_action (application_id, menu_id, action_id)
SELECT menu.application_id, menu.id, action.id FROM iam_menu menu JOIN iam_application app ON app.id = menu.application_id JOIN iam_action action ON action.application_id = menu.application_id
WHERE app.code = 'platform:member' AND menu.route_name = 'member.role' AND action.code = 'platform:member:role:query' AND NOT EXISTS (SELECT 1 FROM iam_menu_action WHERE application_id = menu.application_id AND menu_id = menu.id AND action_id = action.id);

INSERT INTO iam_menu_action (application_id, menu_id, action_id)
SELECT menu.application_id, menu.id, action.id FROM iam_menu menu JOIN iam_application app ON app.id = menu.application_id JOIN iam_action action ON action.application_id = menu.application_id
WHERE app.code = 'platform:member' AND menu.route_name = 'member.permission' AND action.code = 'platform:member:permission:query' AND NOT EXISTS (SELECT 1 FROM iam_menu_action WHERE application_id = menu.application_id AND menu_id = menu.id AND action_id = action.id);

  SELECT app.id AS application_id, app.code, menu.id AS menu_id, menu.route_name, menu.path,
         menu.icon, action.code AS access_action
  FROM iam_application app
  JOIN iam_menu menu ON menu.application_id = app.id
  JOIN iam_menu_action link ON link.application_id = app.id AND link.menu_id = menu.id
  JOIN iam_action action ON action.application_id = app.id AND action.id = link.action_id
  WHERE app.code = 'platform:member'
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
