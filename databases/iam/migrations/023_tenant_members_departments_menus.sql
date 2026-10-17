-- 已有库把组织管理下的「成员与部门」收成目录，下面挂「成员」「部门」两个子页面。
-- 兼容尚未拆分的单页，以及曾把两个页面直接挂在组织管理下的中间态。
-- 按应用编码和路由名定位，不改写已有菜单主键；新建库使用最新 006 / ingot_iam.sql，不执行本补丁。
SET NAMES utf8mb4;
SET time_zone = '+00:00';
START TRANSACTION;

INSERT INTO iam_menu (id, application_id, parent_id, name, path, view_path, route_name, icon, kind, match_mode, access_mode, sort_order, enabled)
SELECT COALESCE((SELECT 130038 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_menu taken WHERE taken.id = 130038)), (SELECT COALESCE(MAX(taken.id), 130000) + 1 FROM iam_menu taken WHERE taken.id >= 130000 AND taken.id < 140000)), parent.application_id, parent.id, '成员与部门', '/org/iam/members/root', 'layout.simple', 'org.iam.members.root', '', 'DIRECTORY', 'ANY', 'ACTION', 1, TRUE FROM iam_menu parent JOIN iam_application app ON app.id = parent.application_id
WHERE app.code = 'iam-tenant' AND parent.route_name = 'org.iam.organization' AND NOT EXISTS (SELECT 1 FROM iam_menu WHERE application_id = parent.application_id AND route_name = 'org.iam.members.root');

UPDATE iam_menu folder
JOIN iam_application app ON app.id = folder.application_id
SET folder.name = '成员与部门', folder.kind = 'DIRECTORY', folder.view_path = 'layout.simple', folder.path = '/org/iam/members/root', folder.sort_order = 1, folder.version = folder.version + 1
WHERE app.code = 'iam-tenant'
  AND folder.route_name = 'org.iam.members.root'
  AND (folder.name <> '成员与部门' OR folder.kind <> 'DIRECTORY' OR folder.view_path <> 'layout.simple' OR folder.path <> '/org/iam/members/root' OR folder.sort_order <> 1);

UPDATE iam_menu menu
JOIN iam_application app ON app.id = menu.application_id
JOIN iam_menu folder ON folder.application_id = menu.application_id AND folder.route_name = 'org.iam.members.root'
SET menu.parent_id = folder.id, menu.name = '成员', menu.sort_order = 1, menu.version = menu.version + 1
WHERE app.code = 'iam-tenant'
  AND menu.route_name = 'org.iam.members'
  AND (menu.parent_id <> folder.id OR menu.name <> '成员' OR menu.sort_order <> 1);

INSERT INTO iam_menu (id, application_id, parent_id, name, path, view_path, route_name, kind, match_mode, access_mode, sort_order, enabled)
SELECT COALESCE((SELECT 130037 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_menu taken WHERE taken.id = 130037)), (SELECT COALESCE(MAX(taken.id), 130000) + 1 FROM iam_menu taken WHERE taken.id >= 130000 AND taken.id < 140000)), parent.application_id, parent.id, '部门', '/org/iam/departments', 'org.iam.departments', 'org.iam.departments', 'PAGE', 'ANY', 'ACTION', 2, TRUE FROM iam_menu parent JOIN iam_application app ON app.id = parent.application_id
WHERE app.code = 'iam-tenant' AND parent.route_name = 'org.iam.members.root' AND NOT EXISTS (SELECT 1 FROM iam_menu WHERE application_id = parent.application_id AND route_name = 'org.iam.departments');

UPDATE iam_menu menu
JOIN iam_application app ON app.id = menu.application_id
JOIN iam_menu folder ON folder.application_id = menu.application_id AND folder.route_name = 'org.iam.members.root'
SET menu.parent_id = folder.id, menu.name = '部门', menu.sort_order = 2, menu.version = menu.version + 1
WHERE app.code = 'iam-tenant'
  AND menu.route_name = 'org.iam.departments'
  AND (menu.parent_id <> folder.id OR menu.name <> '部门' OR menu.sort_order <> 2);

UPDATE iam_menu menu
JOIN iam_application app ON app.id = menu.application_id
SET menu.sort_order = 2, menu.version = menu.version + 1
WHERE app.code = 'iam-tenant'
  AND menu.route_name = 'org.iam.groups'
  AND menu.sort_order <> 2;

UPDATE iam_menu menu
JOIN iam_application app ON app.id = menu.application_id
SET menu.sort_order = 3, menu.version = menu.version + 1
WHERE app.code = 'iam-tenant'
  AND menu.route_name = 'org.iam.settings'
  AND menu.sort_order <> 3;

INSERT INTO iam_menu_action (application_id, menu_id, action_id)
SELECT menu.application_id, menu.id, action.id FROM iam_menu menu JOIN iam_application app ON app.id = menu.application_id JOIN iam_action action ON action.application_id = menu.application_id
WHERE app.code = 'iam-tenant' AND menu.route_name = 'org.iam.organization' AND action.code = 'iam-tenant:department:read' AND NOT EXISTS (SELECT 1 FROM iam_menu_action WHERE application_id = menu.application_id AND menu_id = menu.id AND action_id = action.id);

INSERT INTO iam_menu_action (application_id, menu_id, action_id)
SELECT menu.application_id, menu.id, action.id FROM iam_menu menu JOIN iam_application app ON app.id = menu.application_id JOIN iam_action action ON action.application_id = menu.application_id
WHERE app.code = 'iam-tenant' AND menu.route_name = 'org.iam.members.root' AND action.code = 'iam-tenant:member:read' AND NOT EXISTS (SELECT 1 FROM iam_menu_action WHERE application_id = menu.application_id AND menu_id = menu.id AND action_id = action.id);

INSERT INTO iam_menu_action (application_id, menu_id, action_id)
SELECT menu.application_id, menu.id, action.id FROM iam_menu menu JOIN iam_application app ON app.id = menu.application_id JOIN iam_action action ON action.application_id = menu.application_id
WHERE app.code = 'iam-tenant' AND menu.route_name = 'org.iam.members.root' AND action.code = 'iam-tenant:department:read' AND NOT EXISTS (SELECT 1 FROM iam_menu_action WHERE application_id = menu.application_id AND menu_id = menu.id AND action_id = action.id);

INSERT INTO iam_menu_action (application_id, menu_id, action_id)
SELECT menu.application_id, menu.id, action.id FROM iam_menu menu JOIN iam_application app ON app.id = menu.application_id JOIN iam_action action ON action.application_id = menu.application_id
WHERE app.code = 'iam-tenant' AND menu.route_name = 'org.iam.members' AND action.code = 'iam-tenant:member:read' AND NOT EXISTS (SELECT 1 FROM iam_menu_action WHERE application_id = menu.application_id AND menu_id = menu.id AND action_id = action.id);

INSERT INTO iam_menu_action (application_id, menu_id, action_id)
SELECT menu.application_id, menu.id, action.id FROM iam_menu menu JOIN iam_application app ON app.id = menu.application_id JOIN iam_action action ON action.application_id = menu.application_id
WHERE app.code = 'iam-tenant' AND menu.route_name = 'org.iam.departments' AND action.code = 'iam-tenant:department:read' AND NOT EXISTS (SELECT 1 FROM iam_menu_action WHERE application_id = menu.application_id AND menu_id = menu.id AND action_id = action.id);

DELETE association
FROM iam_menu_action association
JOIN iam_menu menu ON menu.application_id = association.application_id AND menu.id = association.menu_id
JOIN iam_application app ON app.id = menu.application_id
JOIN iam_action action ON action.application_id = association.application_id AND action.id = association.action_id
WHERE app.code = 'iam-tenant'
  AND menu.route_name = 'org.iam.members'
  AND action.code = 'iam-tenant:department:read';

COMMIT;
