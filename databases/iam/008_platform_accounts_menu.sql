-- 已有库补齐「全局账号」菜单，排在「平台人员」前面。
UPDATE iam_menu menu
JOIN iam_application app ON app.id = menu.application_id
SET menu.sort_order = 2
WHERE app.code = 'iam-platform' AND menu.route_name = 'platform.iam.personnel';

UPDATE iam_menu menu
JOIN iam_application app ON app.id = menu.application_id
SET menu.sort_order = 3
WHERE app.code = 'iam-platform' AND menu.route_name = 'platform.iam.authorization';

INSERT INTO iam_menu (id, application_id, parent_id, name, path, view_path, route_name, kind, match_mode, access_mode, sort_order, enabled)
SELECT 130025, parent.application_id, parent.id, '全局账号', '/platform/iam/accounts', 'platform.iam.accounts', 'platform.iam.accounts', 'PAGE', 'ANY', 'ACTION', 1, TRUE FROM iam_menu parent JOIN iam_application app ON app.id = parent.application_id
WHERE app.code = 'iam-platform' AND parent.route_name = 'platform.iam.manage' AND NOT EXISTS (SELECT 1 FROM iam_menu WHERE application_id = parent.application_id AND route_name = 'platform.iam.accounts');

INSERT INTO iam_menu_action (application_id, menu_id, action_id)
SELECT menu.application_id, menu.id, action.id FROM iam_menu menu JOIN iam_application app ON app.id = menu.application_id JOIN iam_action action ON action.application_id = menu.application_id
WHERE app.code = 'iam-platform' AND menu.route_name = 'platform.iam.manage' AND action.code = 'iam-platform:account:read' AND NOT EXISTS (SELECT 1 FROM iam_menu_action WHERE application_id = menu.application_id AND menu_id = menu.id AND action_id = action.id);

INSERT INTO iam_menu_action (application_id, menu_id, action_id)
SELECT menu.application_id, menu.id, action.id FROM iam_menu menu JOIN iam_application app ON app.id = menu.application_id JOIN iam_action action ON action.application_id = menu.application_id
WHERE app.code = 'iam-platform' AND menu.route_name = 'platform.iam.accounts' AND action.code = 'iam-platform:account:read' AND NOT EXISTS (SELECT 1 FROM iam_menu_action WHERE application_id = menu.application_id AND menu_id = menu.id AND action_id = action.id);
