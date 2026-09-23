-- IAM 正式冷启动种子：先按顺序执行 001–005 再执行本文件，可重复执行。
-- 由 tools/iam/generate_bootstrap.py 从 contracts/routes.json 生成，不要手工编辑。
-- 全部语句存在即跳过，不覆盖任何人工或业务修改；不含账号与凭证，
-- 平台首个账号由 provider 冷启动器经安全框架注册用例创建。

-- 发号准备：静态保留标识全部小于起点，运行时发号不会与种子冲突。
INSERT INTO biz_leaf_alloc (biz_tag, max_id, step, description)
SELECT 'iam', 1000000, 100, 'IAM 冷启动发号' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM biz_leaf_alloc WHERE biz_tag = 'iam');

-- 应用：租户域基础应用在组织创建时缺省开通。
INSERT INTO iam_application (id, code, domain, name, description, sort_order, baseline, enabled)
SELECT 100001, 'iam-platform', 'PLATFORM', '平台治理', '平台域身份、目录与授权治理', 1, FALSE, TRUE FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM iam_application WHERE code = 'iam-platform');

INSERT INTO iam_application (id, code, domain, name, description, sort_order, baseline, enabled)
SELECT 100002, 'iam-tenant', 'TENANT', '组织治理', '组织域成员、部门与授权治理', 1, TRUE, TRUE FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM iam_application WHERE code = 'iam-tenant');

-- 资源：声明合法范围与字段能力，供角色发布与字段策略校验取值。
INSERT INTO iam_resource (id, application_id, code, name, scope_capabilities, field_capabilities, enabled)
SELECT 110001, app.id, 'account', '全局账号', CAST('["ALL", "OBJECT_SET"]' AS JSON), CAST('[]' AS JSON), TRUE FROM iam_application app
WHERE app.code = 'iam-platform' AND NOT EXISTS (SELECT 1 FROM iam_resource WHERE application_id = app.id AND code = 'account');

INSERT INTO iam_resource (id, application_id, code, name, scope_capabilities, field_capabilities, enabled)
SELECT 110002, app.id, 'action', '操作', CAST('["ALL", "OBJECT_SET"]' AS JSON), CAST('[]' AS JSON), TRUE FROM iam_application app
WHERE app.code = 'iam-platform' AND NOT EXISTS (SELECT 1 FROM iam_resource WHERE application_id = app.id AND code = 'action');

INSERT INTO iam_resource (id, application_id, code, name, scope_capabilities, field_capabilities, enabled)
SELECT 110003, app.id, 'application', '应用', CAST('["ALL", "OBJECT_SET"]' AS JSON), CAST('[]' AS JSON), TRUE FROM iam_application app
WHERE app.code = 'iam-platform' AND NOT EXISTS (SELECT 1 FROM iam_resource WHERE application_id = app.id AND code = 'application');

INSERT INTO iam_resource (id, application_id, code, name, scope_capabilities, field_capabilities, enabled)
SELECT 110004, app.id, 'assignment', '角色授权', CAST('["ALL", "OBJECT_SET"]' AS JSON), CAST('[]' AS JSON), TRUE FROM iam_application app
WHERE app.code = 'iam-platform' AND NOT EXISTS (SELECT 1 FROM iam_resource WHERE application_id = app.id AND code = 'assignment');

INSERT INTO iam_resource (id, application_id, code, name, scope_capabilities, field_capabilities, enabled)
SELECT 110005, app.id, 'audit', '审计记录', CAST('["ALL"]' AS JSON), CAST('[]' AS JSON), TRUE FROM iam_application app
WHERE app.code = 'iam-platform' AND NOT EXISTS (SELECT 1 FROM iam_resource WHERE application_id = app.id AND code = 'audit');

INSERT INTO iam_resource (id, application_id, code, name, scope_capabilities, field_capabilities, enabled)
SELECT 110006, app.id, 'authorization', '授权诊断', CAST('["ALL"]' AS JSON), CAST('[]' AS JSON), TRUE FROM iam_application app
WHERE app.code = 'iam-platform' AND NOT EXISTS (SELECT 1 FROM iam_resource WHERE application_id = app.id AND code = 'authorization');

INSERT INTO iam_resource (id, application_id, code, name, scope_capabilities, field_capabilities, enabled)
SELECT 110007, app.id, 'credential-policy', '凭证策略', CAST('["ALL"]' AS JSON), CAST('[]' AS JSON), TRUE FROM iam_application app
WHERE app.code = 'iam-platform' AND NOT EXISTS (SELECT 1 FROM iam_resource WHERE application_id = app.id AND code = 'credential-policy');

INSERT INTO iam_resource (id, application_id, code, name, scope_capabilities, field_capabilities, enabled)
SELECT 110008, app.id, 'delegation', '授权委派', CAST('["ALL", "OBJECT_SET"]' AS JSON), CAST('[]' AS JSON), TRUE FROM iam_application app
WHERE app.code = 'iam-platform' AND NOT EXISTS (SELECT 1 FROM iam_resource WHERE application_id = app.id AND code = 'delegation');

INSERT INTO iam_resource (id, application_id, code, name, scope_capabilities, field_capabilities, enabled)
SELECT 110009, app.id, 'dictionary', '字典', CAST('["ALL"]' AS JSON), CAST('[]' AS JSON), TRUE FROM iam_application app
WHERE app.code = 'iam-platform' AND NOT EXISTS (SELECT 1 FROM iam_resource WHERE application_id = app.id AND code = 'dictionary');

INSERT INTO iam_resource (id, application_id, code, name, scope_capabilities, field_capabilities, enabled)
SELECT 110010, app.id, 'entitlement', '应用开通', CAST('["ALL", "OBJECT_SET"]' AS JSON), CAST('[]' AS JSON), TRUE FROM iam_application app
WHERE app.code = 'iam-platform' AND NOT EXISTS (SELECT 1 FROM iam_resource WHERE application_id = app.id AND code = 'entitlement');

INSERT INTO iam_resource (id, application_id, code, name, scope_capabilities, field_capabilities, enabled)
SELECT 110011, app.id, 'group', '用户组', CAST('["ALL", "OBJECT_SET"]' AS JSON), CAST('[]' AS JSON), TRUE FROM iam_application app
WHERE app.code = 'iam-platform' AND NOT EXISTS (SELECT 1 FROM iam_resource WHERE application_id = app.id AND code = 'group');

INSERT INTO iam_resource (id, application_id, code, name, scope_capabilities, field_capabilities, enabled)
SELECT 110012, app.id, 'id-allocation', '发号', CAST('["ALL"]' AS JSON), CAST('[]' AS JSON), TRUE FROM iam_application app
WHERE app.code = 'iam-platform' AND NOT EXISTS (SELECT 1 FROM iam_resource WHERE application_id = app.id AND code = 'id-allocation');

INSERT INTO iam_resource (id, application_id, code, name, scope_capabilities, field_capabilities, enabled)
SELECT 110013, app.id, 'lockout-policy', '账号锁定策略', CAST('["ALL"]' AS JSON), CAST('[]' AS JSON), TRUE FROM iam_application app
WHERE app.code = 'iam-platform' AND NOT EXISTS (SELECT 1 FROM iam_resource WHERE application_id = app.id AND code = 'lockout-policy');

INSERT INTO iam_resource (id, application_id, code, name, scope_capabilities, field_capabilities, enabled)
SELECT 110014, app.id, 'login-failure-policy', '登录失败防护', CAST('["ALL"]' AS JSON), CAST('[]' AS JSON), TRUE FROM iam_application app
WHERE app.code = 'iam-platform' AND NOT EXISTS (SELECT 1 FROM iam_resource WHERE application_id = app.id AND code = 'login-failure-policy');

INSERT INTO iam_resource (id, application_id, code, name, scope_capabilities, field_capabilities, enabled)
SELECT 110015, app.id, 'member', '成员', CAST('["ALL", "SELF", "MEMBER_DEPARTMENTS", "MANAGED_DEPARTMENTS", "OBJECT_SET"]' AS JSON), CAST('[{"key": "displayName", "label": "显示名", "visibilities": ["HIDDEN", "MASKED", "FULL"], "editable": true, "filterable": true, "sortable": true}, {"key": "avatar", "label": "头像", "visibilities": ["HIDDEN", "FULL"], "editable": true, "filterable": false, "sortable": false}, {"key": "phone", "label": "手机号", "visibilities": ["HIDDEN", "MASKED", "FULL"], "editable": true, "filterable": true, "sortable": false}, {"key": "email", "label": "邮箱", "visibilities": ["HIDDEN", "MASKED", "FULL"], "editable": true, "filterable": true, "sortable": false}]' AS JSON), TRUE FROM iam_application app
WHERE app.code = 'iam-platform' AND NOT EXISTS (SELECT 1 FROM iam_resource WHERE application_id = app.id AND code = 'member');

INSERT INTO iam_resource (id, application_id, code, name, scope_capabilities, field_capabilities, enabled)
SELECT 110016, app.id, 'menu', '菜单', CAST('["ALL", "OBJECT_SET"]' AS JSON), CAST('[]' AS JSON), TRUE FROM iam_application app
WHERE app.code = 'iam-platform' AND NOT EXISTS (SELECT 1 FROM iam_resource WHERE application_id = app.id AND code = 'menu');

INSERT INTO iam_resource (id, application_id, code, name, scope_capabilities, field_capabilities, enabled)
SELECT 110017, app.id, 'plan', '套餐', CAST('["ALL", "OBJECT_SET"]' AS JSON), CAST('[]' AS JSON), TRUE FROM iam_application app
WHERE app.code = 'iam-platform' AND NOT EXISTS (SELECT 1 FROM iam_resource WHERE application_id = app.id AND code = 'plan');

INSERT INTO iam_resource (id, application_id, code, name, scope_capabilities, field_capabilities, enabled)
SELECT 110018, app.id, 'resource', '资源', CAST('["ALL", "OBJECT_SET"]' AS JSON), CAST('[]' AS JSON), TRUE FROM iam_application app
WHERE app.code = 'iam-platform' AND NOT EXISTS (SELECT 1 FROM iam_resource WHERE application_id = app.id AND code = 'resource');

INSERT INTO iam_resource (id, application_id, code, name, scope_capabilities, field_capabilities, enabled)
SELECT 110019, app.id, 'role', '角色', CAST('["ALL", "OBJECT_SET"]' AS JSON), CAST('[]' AS JSON), TRUE FROM iam_application app
WHERE app.code = 'iam-platform' AND NOT EXISTS (SELECT 1 FROM iam_resource WHERE application_id = app.id AND code = 'role');

INSERT INTO iam_resource (id, application_id, code, name, scope_capabilities, field_capabilities, enabled)
SELECT 110020, app.id, 'security-policy', '安全策略', CAST('["ALL"]' AS JSON), CAST('[]' AS JSON), TRUE FROM iam_application app
WHERE app.code = 'iam-platform' AND NOT EXISTS (SELECT 1 FROM iam_resource WHERE application_id = app.id AND code = 'security-policy');

INSERT INTO iam_resource (id, application_id, code, name, scope_capabilities, field_capabilities, enabled)
SELECT 110021, app.id, 'session', '在线会话', CAST('["ALL"]' AS JSON), CAST('[]' AS JSON), TRUE FROM iam_application app
WHERE app.code = 'iam-platform' AND NOT EXISTS (SELECT 1 FROM iam_resource WHERE application_id = app.id AND code = 'session');

INSERT INTO iam_resource (id, application_id, code, name, scope_capabilities, field_capabilities, enabled)
SELECT 110022, app.id, 'session-policy', '会话并发策略', CAST('["ALL"]' AS JSON), CAST('[]' AS JSON), TRUE FROM iam_application app
WHERE app.code = 'iam-platform' AND NOT EXISTS (SELECT 1 FROM iam_resource WHERE application_id = app.id AND code = 'session-policy');

INSERT INTO iam_resource (id, application_id, code, name, scope_capabilities, field_capabilities, enabled)
SELECT 110023, app.id, 'shared-role', '共享角色', CAST('["ALL", "OBJECT_SET"]' AS JSON), CAST('[]' AS JSON), TRUE FROM iam_application app
WHERE app.code = 'iam-platform' AND NOT EXISTS (SELECT 1 FROM iam_resource WHERE application_id = app.id AND code = 'shared-role');

INSERT INTO iam_resource (id, application_id, code, name, scope_capabilities, field_capabilities, enabled)
SELECT 110024, app.id, 'social-config', '社会化登录配置', CAST('["ALL"]' AS JSON), CAST('[]' AS JSON), TRUE FROM iam_application app
WHERE app.code = 'iam-platform' AND NOT EXISTS (SELECT 1 FROM iam_resource WHERE application_id = app.id AND code = 'social-config');

INSERT INTO iam_resource (id, application_id, code, name, scope_capabilities, field_capabilities, enabled)
SELECT 110025, app.id, 'tenant', '组织', CAST('["ALL", "OBJECT_SET"]' AS JSON), CAST('[]' AS JSON), TRUE FROM iam_application app
WHERE app.code = 'iam-platform' AND NOT EXISTS (SELECT 1 FROM iam_resource WHERE application_id = app.id AND code = 'tenant');

INSERT INTO iam_resource (id, application_id, code, name, scope_capabilities, field_capabilities, enabled)
SELECT 110026, app.id, 'application', '应用', CAST('["ALL", "OBJECT_SET"]' AS JSON), CAST('[]' AS JSON), TRUE FROM iam_application app
WHERE app.code = 'iam-tenant' AND NOT EXISTS (SELECT 1 FROM iam_resource WHERE application_id = app.id AND code = 'application');

INSERT INTO iam_resource (id, application_id, code, name, scope_capabilities, field_capabilities, enabled)
SELECT 110027, app.id, 'assignment', '角色授权', CAST('["ALL", "OBJECT_SET"]' AS JSON), CAST('[]' AS JSON), TRUE FROM iam_application app
WHERE app.code = 'iam-tenant' AND NOT EXISTS (SELECT 1 FROM iam_resource WHERE application_id = app.id AND code = 'assignment');

INSERT INTO iam_resource (id, application_id, code, name, scope_capabilities, field_capabilities, enabled)
SELECT 110028, app.id, 'audience', '应用可用人群', CAST('["ALL"]' AS JSON), CAST('[]' AS JSON), TRUE FROM iam_application app
WHERE app.code = 'iam-tenant' AND NOT EXISTS (SELECT 1 FROM iam_resource WHERE application_id = app.id AND code = 'audience');

INSERT INTO iam_resource (id, application_id, code, name, scope_capabilities, field_capabilities, enabled)
SELECT 110029, app.id, 'audit', '审计记录', CAST('["ALL"]' AS JSON), CAST('[]' AS JSON), TRUE FROM iam_application app
WHERE app.code = 'iam-tenant' AND NOT EXISTS (SELECT 1 FROM iam_resource WHERE application_id = app.id AND code = 'audit');

INSERT INTO iam_resource (id, application_id, code, name, scope_capabilities, field_capabilities, enabled)
SELECT 110030, app.id, 'authorization', '授权诊断', CAST('["ALL"]' AS JSON), CAST('[]' AS JSON), TRUE FROM iam_application app
WHERE app.code = 'iam-tenant' AND NOT EXISTS (SELECT 1 FROM iam_resource WHERE application_id = app.id AND code = 'authorization');

INSERT INTO iam_resource (id, application_id, code, name, scope_capabilities, field_capabilities, enabled)
SELECT 110031, app.id, 'delegation', '授权委派', CAST('["ALL", "OBJECT_SET"]' AS JSON), CAST('[]' AS JSON), TRUE FROM iam_application app
WHERE app.code = 'iam-tenant' AND NOT EXISTS (SELECT 1 FROM iam_resource WHERE application_id = app.id AND code = 'delegation');

INSERT INTO iam_resource (id, application_id, code, name, scope_capabilities, field_capabilities, enabled)
SELECT 110032, app.id, 'department', '部门', CAST('["ALL", "MANAGED_DEPARTMENTS", "OBJECT_SET"]' AS JSON), CAST('[]' AS JSON), TRUE FROM iam_application app
WHERE app.code = 'iam-tenant' AND NOT EXISTS (SELECT 1 FROM iam_resource WHERE application_id = app.id AND code = 'department');

INSERT INTO iam_resource (id, application_id, code, name, scope_capabilities, field_capabilities, enabled)
SELECT 110033, app.id, 'directory', '通讯录', CAST('["ALL", "SELF", "MEMBER_DEPARTMENTS", "MANAGED_DEPARTMENTS", "OBJECT_SET"]' AS JSON), CAST('[{"key": "displayName", "label": "显示名", "visibilities": ["HIDDEN", "MASKED", "FULL"], "editable": true, "filterable": true, "sortable": true}, {"key": "avatar", "label": "头像", "visibilities": ["HIDDEN", "FULL"], "editable": true, "filterable": false, "sortable": false}, {"key": "phone", "label": "手机号", "visibilities": ["HIDDEN", "MASKED", "FULL"], "editable": true, "filterable": true, "sortable": false}, {"key": "email", "label": "邮箱", "visibilities": ["HIDDEN", "MASKED", "FULL"], "editable": true, "filterable": true, "sortable": false}]' AS JSON), TRUE FROM iam_application app
WHERE app.code = 'iam-tenant' AND NOT EXISTS (SELECT 1 FROM iam_resource WHERE application_id = app.id AND code = 'directory');

INSERT INTO iam_resource (id, application_id, code, name, scope_capabilities, field_capabilities, enabled)
SELECT 110034, app.id, 'directory-policy', '通讯录可见范围策略', CAST('["ALL"]' AS JSON), CAST('[]' AS JSON), TRUE FROM iam_application app
WHERE app.code = 'iam-tenant' AND NOT EXISTS (SELECT 1 FROM iam_resource WHERE application_id = app.id AND code = 'directory-policy');

INSERT INTO iam_resource (id, application_id, code, name, scope_capabilities, field_capabilities, enabled)
SELECT 110035, app.id, 'field-policy', '成员字段策略', CAST('["ALL"]' AS JSON), CAST('[]' AS JSON), TRUE FROM iam_application app
WHERE app.code = 'iam-tenant' AND NOT EXISTS (SELECT 1 FROM iam_resource WHERE application_id = app.id AND code = 'field-policy');

INSERT INTO iam_resource (id, application_id, code, name, scope_capabilities, field_capabilities, enabled)
SELECT 110036, app.id, 'group', '用户组', CAST('["ALL", "OBJECT_SET"]' AS JSON), CAST('[]' AS JSON), TRUE FROM iam_application app
WHERE app.code = 'iam-tenant' AND NOT EXISTS (SELECT 1 FROM iam_resource WHERE application_id = app.id AND code = 'group');

INSERT INTO iam_resource (id, application_id, code, name, scope_capabilities, field_capabilities, enabled)
SELECT 110037, app.id, 'member', '成员', CAST('["ALL", "SELF", "MEMBER_DEPARTMENTS", "MANAGED_DEPARTMENTS", "OBJECT_SET"]' AS JSON), CAST('[{"key": "displayName", "label": "显示名", "visibilities": ["HIDDEN", "MASKED", "FULL"], "editable": true, "filterable": true, "sortable": true}, {"key": "avatar", "label": "头像", "visibilities": ["HIDDEN", "FULL"], "editable": true, "filterable": false, "sortable": false}, {"key": "phone", "label": "手机号", "visibilities": ["HIDDEN", "MASKED", "FULL"], "editable": true, "filterable": true, "sortable": false}, {"key": "email", "label": "邮箱", "visibilities": ["HIDDEN", "MASKED", "FULL"], "editable": true, "filterable": true, "sortable": false}]' AS JSON), TRUE FROM iam_application app
WHERE app.code = 'iam-tenant' AND NOT EXISTS (SELECT 1 FROM iam_resource WHERE application_id = app.id AND code = 'member');

INSERT INTO iam_resource (id, application_id, code, name, scope_capabilities, field_capabilities, enabled)
SELECT 110038, app.id, 'policy', '策略预览', CAST('["ALL"]' AS JSON), CAST('[]' AS JSON), TRUE FROM iam_application app
WHERE app.code = 'iam-tenant' AND NOT EXISTS (SELECT 1 FROM iam_resource WHERE application_id = app.id AND code = 'policy');

INSERT INTO iam_resource (id, application_id, code, name, scope_capabilities, field_capabilities, enabled)
SELECT 110039, app.id, 'role', '角色', CAST('["ALL", "OBJECT_SET"]' AS JSON), CAST('[]' AS JSON), TRUE FROM iam_application app
WHERE app.code = 'iam-tenant' AND NOT EXISTS (SELECT 1 FROM iam_resource WHERE application_id = app.id AND code = 'role');

INSERT INTO iam_resource (id, application_id, code, name, scope_capabilities, field_capabilities, enabled)
SELECT 110040, app.id, 'settings', '组织设置', CAST('["ALL"]' AS JSON), CAST('[]' AS JSON), TRUE FROM iam_application app
WHERE app.code = 'iam-tenant' AND NOT EXISTS (SELECT 1 FROM iam_resource WHERE application_id = app.id AND code = 'settings');

-- 操作：精确操作码全局唯一，禁止通配。
INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT 120001, res.application_id, res.id, 'iam-platform:account:create', '创建全局账号', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'account' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:account:create');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT 120002, res.application_id, res.id, 'iam-platform:account:delete', '删除全局账号', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'account' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:account:delete');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT 120003, res.application_id, res.id, 'iam-platform:account:disable', '停用全局账号', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'account' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:account:disable');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT 120004, res.application_id, res.id, 'iam-platform:account:enable', '启用全局账号', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'account' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:account:enable');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT 120005, res.application_id, res.id, 'iam-platform:account:lock', '锁定全局账号', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'account' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:account:lock');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT 120006, res.application_id, res.id, 'iam-platform:account:lookup', '查找全局账号', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'account' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:account:lookup');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT 120007, res.application_id, res.id, 'iam-platform:account:read', '查看全局账号', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'account' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:account:read');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT 120008, res.application_id, res.id, 'iam-platform:account:reset-password', '重置密码', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'account' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:account:reset-password');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT 120009, res.application_id, res.id, 'iam-platform:account:unlock', '解锁全局账号', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'account' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:account:unlock');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT 120010, res.application_id, res.id, 'iam-platform:account:update', '编辑全局账号', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'account' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:account:update');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT 120011, res.application_id, res.id, 'iam-platform:action:create', '创建操作', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'action' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:action:create');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT 120012, res.application_id, res.id, 'iam-platform:action:delete', '删除操作', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'action' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:action:delete');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT 120013, res.application_id, res.id, 'iam-platform:action:read', '查看操作', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'action' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:action:read');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT 120014, res.application_id, res.id, 'iam-platform:action:status', '启停操作', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'action' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:action:status');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT 120015, res.application_id, res.id, 'iam-platform:action:update', '编辑操作', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'action' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:action:update');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT 120016, res.application_id, res.id, 'iam-platform:application:create', '创建应用', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'application' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:application:create');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT 120017, res.application_id, res.id, 'iam-platform:application:delete', '删除应用', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'application' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:application:delete');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT 120018, res.application_id, res.id, 'iam-platform:application:read', '查看应用', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'application' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:application:read');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT 120019, res.application_id, res.id, 'iam-platform:application:status', '启停应用', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'application' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:application:status');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT 120020, res.application_id, res.id, 'iam-platform:application:update', '编辑应用', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'application' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:application:update');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT 120021, res.application_id, res.id, 'iam-platform:assignment:create', '创建角色授权', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'assignment' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:assignment:create');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT 120022, res.application_id, res.id, 'iam-platform:assignment:delete', '删除角色授权', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'assignment' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:assignment:delete');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT 120023, res.application_id, res.id, 'iam-platform:assignment:read', '查看角色授权', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'assignment' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:assignment:read');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT 120024, res.application_id, res.id, 'iam-platform:assignment:update', '编辑角色授权', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'assignment' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:assignment:update');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT 120025, res.application_id, res.id, 'iam-platform:audit:read', '查看审计记录', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'audit' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:audit:read');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT 120026, res.application_id, res.id, 'iam-platform:authorization:diagnose', '诊断', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'authorization' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:authorization:diagnose');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT 120027, res.application_id, res.id, 'iam-platform:credential-policy:create', '创建凭证策略', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'credential-policy' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:credential-policy:create');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT 120028, res.application_id, res.id, 'iam-platform:credential-policy:delete', '删除凭证策略', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'credential-policy' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:credential-policy:delete');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT 120029, res.application_id, res.id, 'iam-platform:credential-policy:read', '查看凭证策略', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'credential-policy' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:credential-policy:read');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT 120030, res.application_id, res.id, 'iam-platform:credential-policy:update', '编辑凭证策略', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'credential-policy' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:credential-policy:update');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT 120031, res.application_id, res.id, 'iam-platform:delegation:create', '创建授权委派', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'delegation' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:delegation:create');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT 120032, res.application_id, res.id, 'iam-platform:delegation:delete', '删除授权委派', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'delegation' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:delegation:delete');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT 120033, res.application_id, res.id, 'iam-platform:delegation:preview', '预览授权委派', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'delegation' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:delegation:preview');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT 120034, res.application_id, res.id, 'iam-platform:delegation:read', '查看授权委派', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'delegation' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:delegation:read');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT 120035, res.application_id, res.id, 'iam-platform:delegation:update', '编辑授权委派', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'delegation' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:delegation:update');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT 120036, res.application_id, res.id, 'iam-platform:dictionary:create', '创建字典', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'dictionary' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:dictionary:create');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT 120037, res.application_id, res.id, 'iam-platform:dictionary:delete', '删除字典', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'dictionary' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:dictionary:delete');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT 120038, res.application_id, res.id, 'iam-platform:dictionary:read', '查看字典', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'dictionary' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:dictionary:read');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT 120039, res.application_id, res.id, 'iam-platform:dictionary:update', '编辑字典', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'dictionary' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:dictionary:update');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT 120040, res.application_id, res.id, 'iam-platform:entitlement:preview', '预览应用开通', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'entitlement' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:entitlement:preview');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT 120041, res.application_id, res.id, 'iam-platform:entitlement:read', '查看应用开通', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'entitlement' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:entitlement:read');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT 120042, res.application_id, res.id, 'iam-platform:entitlement:update', '编辑应用开通', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'entitlement' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:entitlement:update');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT 120043, res.application_id, res.id, 'iam-platform:group:create', '创建用户组', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'group' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:group:create');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT 120044, res.application_id, res.id, 'iam-platform:group:delete', '删除用户组', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'group' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:group:delete');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT 120045, res.application_id, res.id, 'iam-platform:group:preview', '预览用户组', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'group' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:group:preview');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT 120046, res.application_id, res.id, 'iam-platform:group:read', '查看用户组', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'group' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:group:read');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT 120047, res.application_id, res.id, 'iam-platform:group:update', '编辑用户组', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'group' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:group:update');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT 120048, res.application_id, res.id, 'iam-platform:id-allocation:create', '创建发号', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'id-allocation' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:id-allocation:create');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT 120049, res.application_id, res.id, 'iam-platform:id-allocation:delete', '删除发号', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'id-allocation' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:id-allocation:delete');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT 120050, res.application_id, res.id, 'iam-platform:id-allocation:read', '查看发号', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'id-allocation' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:id-allocation:read');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT 120051, res.application_id, res.id, 'iam-platform:id-allocation:update', '编辑发号', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'id-allocation' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:id-allocation:update');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT 120052, res.application_id, res.id, 'iam-platform:lockout-policy:read', '查看账号锁定策略', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'lockout-policy' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:lockout-policy:read');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT 120053, res.application_id, res.id, 'iam-platform:lockout-policy:update', '编辑账号锁定策略', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'lockout-policy' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:lockout-policy:update');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT 120054, res.application_id, res.id, 'iam-platform:login-failure-policy:read', '查看登录失败防护', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'login-failure-policy' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:login-failure-policy:read');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT 120055, res.application_id, res.id, 'iam-platform:login-failure-policy:update', '编辑登录失败防护', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'login-failure-policy' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:login-failure-policy:update');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT 120056, res.application_id, res.id, 'iam-platform:member:create', '创建成员', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'member' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:member:create');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT 120057, res.application_id, res.id, 'iam-platform:member:read', '查看成员', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'member' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:member:read');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT 120058, res.application_id, res.id, 'iam-platform:member:remove', '移出成员', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'member' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:member:remove');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT 120059, res.application_id, res.id, 'iam-platform:member:status', '启停成员', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'member' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:member:status');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT 120060, res.application_id, res.id, 'iam-platform:member:update', '编辑成员', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'member' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:member:update');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT 120061, res.application_id, res.id, 'iam-platform:menu:create', '创建菜单', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'menu' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:menu:create');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT 120062, res.application_id, res.id, 'iam-platform:menu:delete', '删除菜单', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'menu' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:menu:delete');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT 120063, res.application_id, res.id, 'iam-platform:menu:read', '查看菜单', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'menu' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:menu:read');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT 120064, res.application_id, res.id, 'iam-platform:menu:update', '编辑菜单', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'menu' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:menu:update');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT 120065, res.application_id, res.id, 'iam-platform:plan:create', '创建套餐', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'plan' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:plan:create');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT 120066, res.application_id, res.id, 'iam-platform:plan:read', '查看套餐', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'plan' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:plan:read');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT 120067, res.application_id, res.id, 'iam-platform:plan:update', '编辑套餐', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'plan' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:plan:update');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT 120068, res.application_id, res.id, 'iam-platform:resource:create', '创建资源', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'resource' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:resource:create');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT 120069, res.application_id, res.id, 'iam-platform:resource:delete', '删除资源', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'resource' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:resource:delete');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT 120070, res.application_id, res.id, 'iam-platform:resource:read', '查看资源', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'resource' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:resource:read');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT 120071, res.application_id, res.id, 'iam-platform:resource:update', '编辑资源', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'resource' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:resource:update');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT 120072, res.application_id, res.id, 'iam-platform:role:create', '创建角色', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'role' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:role:create');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT 120073, res.application_id, res.id, 'iam-platform:role:delete', '删除角色', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'role' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:role:delete');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT 120074, res.application_id, res.id, 'iam-platform:role:preview', '预览角色', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'role' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:role:preview');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT 120075, res.application_id, res.id, 'iam-platform:role:publish', '发布版本角色', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'role' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:role:publish');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT 120076, res.application_id, res.id, 'iam-platform:role:read', '查看角色', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'role' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:role:read');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT 120077, res.application_id, res.id, 'iam-platform:role:status', '启停角色', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'role' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:role:status');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT 120078, res.application_id, res.id, 'iam-platform:security-policy:create', '创建安全策略', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'security-policy' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:security-policy:create');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT 120079, res.application_id, res.id, 'iam-platform:security-policy:delete', '删除安全策略', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'security-policy' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:security-policy:delete');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT 120080, res.application_id, res.id, 'iam-platform:security-policy:read', '查看安全策略', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'security-policy' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:security-policy:read');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT 120081, res.application_id, res.id, 'iam-platform:security-policy:update', '编辑安全策略', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'security-policy' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:security-policy:update');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT 120082, res.application_id, res.id, 'iam-platform:session:read', '查看在线会话', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'session' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:session:read');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT 120083, res.application_id, res.id, 'iam-platform:session:revoke', '撤销', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'session' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:session:revoke');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT 120084, res.application_id, res.id, 'iam-platform:session-policy:read', '查看会话并发策略', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'session-policy' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:session-policy:read');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT 120085, res.application_id, res.id, 'iam-platform:session-policy:update', '编辑会话并发策略', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'session-policy' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:session-policy:update');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT 120086, res.application_id, res.id, 'iam-platform:shared-role:create', '创建共享角色', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'shared-role' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:shared-role:create');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT 120087, res.application_id, res.id, 'iam-platform:shared-role:delete', '删除共享角色', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'shared-role' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:shared-role:delete');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT 120088, res.application_id, res.id, 'iam-platform:shared-role:preview', '预览共享角色', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'shared-role' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:shared-role:preview');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT 120089, res.application_id, res.id, 'iam-platform:shared-role:publish', '发布版本共享角色', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'shared-role' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:shared-role:publish');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT 120090, res.application_id, res.id, 'iam-platform:shared-role:read', '查看共享角色', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'shared-role' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:shared-role:read');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT 120091, res.application_id, res.id, 'iam-platform:shared-role:status', '启停共享角色', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'shared-role' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:shared-role:status');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT 120092, res.application_id, res.id, 'iam-platform:social-config:create', '创建社会化登录配置', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'social-config' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:social-config:create');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT 120093, res.application_id, res.id, 'iam-platform:social-config:delete', '删除社会化登录配置', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'social-config' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:social-config:delete');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT 120094, res.application_id, res.id, 'iam-platform:social-config:read', '查看社会化登录配置', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'social-config' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:social-config:read');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT 120095, res.application_id, res.id, 'iam-platform:social-config:update', '编辑社会化登录配置', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'social-config' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:social-config:update');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT 120096, res.application_id, res.id, 'iam-platform:tenant:create', '创建组织', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'tenant' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:tenant:create');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT 120097, res.application_id, res.id, 'iam-platform:tenant:preview', '预览组织', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'tenant' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:tenant:preview');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT 120098, res.application_id, res.id, 'iam-platform:tenant:read', '查看组织', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'tenant' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:tenant:read');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT 120099, res.application_id, res.id, 'iam-platform:tenant:update', '编辑组织', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'tenant' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:tenant:update');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT 120100, res.application_id, res.id, 'iam-tenant:application:read', '查看应用', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-tenant' AND res.code = 'application' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-tenant:application:read');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT 120101, res.application_id, res.id, 'iam-tenant:assignment:create', '创建角色授权', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-tenant' AND res.code = 'assignment' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-tenant:assignment:create');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT 120102, res.application_id, res.id, 'iam-tenant:assignment:delete', '删除角色授权', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-tenant' AND res.code = 'assignment' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-tenant:assignment:delete');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT 120103, res.application_id, res.id, 'iam-tenant:assignment:read', '查看角色授权', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-tenant' AND res.code = 'assignment' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-tenant:assignment:read');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT 120104, res.application_id, res.id, 'iam-tenant:assignment:update', '编辑角色授权', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-tenant' AND res.code = 'assignment' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-tenant:assignment:update');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT 120105, res.application_id, res.id, 'iam-tenant:audience:read', '查看应用可用人群', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-tenant' AND res.code = 'audience' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-tenant:audience:read');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT 120106, res.application_id, res.id, 'iam-tenant:audience:update', '编辑应用可用人群', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-tenant' AND res.code = 'audience' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-tenant:audience:update');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT 120107, res.application_id, res.id, 'iam-tenant:audit:read', '查看审计记录', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-tenant' AND res.code = 'audit' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-tenant:audit:read');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT 120108, res.application_id, res.id, 'iam-tenant:authorization:diagnose', '诊断', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-tenant' AND res.code = 'authorization' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-tenant:authorization:diagnose');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT 120109, res.application_id, res.id, 'iam-tenant:delegation:create', '创建授权委派', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-tenant' AND res.code = 'delegation' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-tenant:delegation:create');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT 120110, res.application_id, res.id, 'iam-tenant:delegation:delete', '删除授权委派', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-tenant' AND res.code = 'delegation' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-tenant:delegation:delete');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT 120111, res.application_id, res.id, 'iam-tenant:delegation:preview', '预览授权委派', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-tenant' AND res.code = 'delegation' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-tenant:delegation:preview');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT 120112, res.application_id, res.id, 'iam-tenant:delegation:read', '查看授权委派', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-tenant' AND res.code = 'delegation' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-tenant:delegation:read');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT 120113, res.application_id, res.id, 'iam-tenant:delegation:update', '编辑授权委派', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-tenant' AND res.code = 'delegation' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-tenant:delegation:update');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT 120114, res.application_id, res.id, 'iam-tenant:department:create', '创建部门', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-tenant' AND res.code = 'department' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-tenant:department:create');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT 120115, res.application_id, res.id, 'iam-tenant:department:delete', '删除部门', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-tenant' AND res.code = 'department' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-tenant:department:delete');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT 120116, res.application_id, res.id, 'iam-tenant:department:read', '查看部门', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-tenant' AND res.code = 'department' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-tenant:department:read');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT 120117, res.application_id, res.id, 'iam-tenant:department:update', '编辑部门', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-tenant' AND res.code = 'department' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-tenant:department:update');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT 120118, res.application_id, res.id, 'iam-tenant:directory:read', '查看通讯录', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-tenant' AND res.code = 'directory' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-tenant:directory:read');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT 120119, res.application_id, res.id, 'iam-tenant:directory-policy:read', '查看通讯录可见范围策略', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-tenant' AND res.code = 'directory-policy' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-tenant:directory-policy:read');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT 120120, res.application_id, res.id, 'iam-tenant:directory-policy:update', '编辑通讯录可见范围策略', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-tenant' AND res.code = 'directory-policy' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-tenant:directory-policy:update');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT 120121, res.application_id, res.id, 'iam-tenant:field-policy:read', '查看成员字段策略', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-tenant' AND res.code = 'field-policy' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-tenant:field-policy:read');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT 120122, res.application_id, res.id, 'iam-tenant:field-policy:update', '编辑成员字段策略', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-tenant' AND res.code = 'field-policy' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-tenant:field-policy:update');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT 120123, res.application_id, res.id, 'iam-tenant:group:create', '创建用户组', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-tenant' AND res.code = 'group' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-tenant:group:create');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT 120124, res.application_id, res.id, 'iam-tenant:group:delete', '删除用户组', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-tenant' AND res.code = 'group' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-tenant:group:delete');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT 120125, res.application_id, res.id, 'iam-tenant:group:preview', '预览用户组', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-tenant' AND res.code = 'group' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-tenant:group:preview');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT 120126, res.application_id, res.id, 'iam-tenant:group:read', '查看用户组', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-tenant' AND res.code = 'group' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-tenant:group:read');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT 120127, res.application_id, res.id, 'iam-tenant:group:update', '编辑用户组', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-tenant' AND res.code = 'group' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-tenant:group:update');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT 120128, res.application_id, res.id, 'iam-tenant:member:create', '创建成员', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-tenant' AND res.code = 'member' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-tenant:member:create');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT 120129, res.application_id, res.id, 'iam-tenant:member:departments', '调整任职', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-tenant' AND res.code = 'member' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-tenant:member:departments');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT 120130, res.application_id, res.id, 'iam-tenant:member:export', '导出成员', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-tenant' AND res.code = 'member' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-tenant:member:export');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT 120131, res.application_id, res.id, 'iam-tenant:member:read', '查看成员', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-tenant' AND res.code = 'member' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-tenant:member:read');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT 120132, res.application_id, res.id, 'iam-tenant:member:remove', '移出成员', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-tenant' AND res.code = 'member' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-tenant:member:remove');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT 120133, res.application_id, res.id, 'iam-tenant:member:status', '启停成员', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-tenant' AND res.code = 'member' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-tenant:member:status');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT 120134, res.application_id, res.id, 'iam-tenant:member:update', '编辑成员', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-tenant' AND res.code = 'member' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-tenant:member:update');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT 120135, res.application_id, res.id, 'iam-tenant:policy:preview', '预览策略预览', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-tenant' AND res.code = 'policy' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-tenant:policy:preview');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT 120136, res.application_id, res.id, 'iam-tenant:role:create', '创建角色', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-tenant' AND res.code = 'role' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-tenant:role:create');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT 120137, res.application_id, res.id, 'iam-tenant:role:delete', '删除角色', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-tenant' AND res.code = 'role' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-tenant:role:delete');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT 120138, res.application_id, res.id, 'iam-tenant:role:preview', '预览角色', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-tenant' AND res.code = 'role' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-tenant:role:preview');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT 120139, res.application_id, res.id, 'iam-tenant:role:publish', '发布版本角色', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-tenant' AND res.code = 'role' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-tenant:role:publish');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT 120140, res.application_id, res.id, 'iam-tenant:role:read', '查看角色', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-tenant' AND res.code = 'role' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-tenant:role:read');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT 120141, res.application_id, res.id, 'iam-tenant:role:status', '启停角色', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-tenant' AND res.code = 'role' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-tenant:role:status');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT 120142, res.application_id, res.id, 'iam-tenant:role:upgrade', '升级角色', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-tenant' AND res.code = 'role' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-tenant:role:upgrade');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT 120143, res.application_id, res.id, 'iam-tenant:settings:owner-transfer', '转交所有者', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-tenant' AND res.code = 'settings' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-tenant:settings:owner-transfer');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT 120144, res.application_id, res.id, 'iam-tenant:settings:read', '查看组织设置', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-tenant' AND res.code = 'settings' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-tenant:settings:read');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT 120145, res.application_id, res.id, 'iam-tenant:settings:update', '编辑组织设置', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-tenant' AND res.code = 'settings' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-tenant:settings:update');

-- 菜单：目录携带子页操作并集，子页全部不可见时目录随之隐藏。
INSERT INTO iam_menu (id, application_id, parent_id, name, path, view_path, route_name, kind, match_mode, access_mode, sort_order, enabled)
SELECT 130001, app.id, NULL, '组织与租户', '/platform/iam/tenant', NULL, 'platform.iam.tenant', 'DIRECTORY', 'ANY', 'ACTION', 1, TRUE FROM iam_application app
WHERE app.code = 'iam-platform' AND NOT EXISTS (SELECT 1 FROM iam_menu WHERE application_id = app.id AND route_name = 'platform.iam.tenant');

INSERT INTO iam_menu (id, application_id, parent_id, name, path, view_path, route_name, kind, match_mode, access_mode, sort_order, enabled)
SELECT 130002, parent.application_id, parent.id, '租户管理', '/platform/iam/tenants', 'platform.iam.tenants', 'platform.iam.tenants', 'PAGE', 'ANY', 'ACTION', 1, TRUE FROM iam_menu parent JOIN iam_application app ON app.id = parent.application_id
WHERE app.code = 'iam-platform' AND parent.route_name = 'platform.iam.tenant' AND NOT EXISTS (SELECT 1 FROM iam_menu WHERE application_id = parent.application_id AND route_name = 'platform.iam.tenants');

INSERT INTO iam_menu (id, application_id, parent_id, name, path, view_path, route_name, kind, match_mode, access_mode, sort_order, enabled)
SELECT 130003, app.id, NULL, '应用与配置', '/platform/iam/config', NULL, 'platform.iam.config', 'DIRECTORY', 'ANY', 'ACTION', 2, TRUE FROM iam_application app
WHERE app.code = 'iam-platform' AND NOT EXISTS (SELECT 1 FROM iam_menu WHERE application_id = app.id AND route_name = 'platform.iam.config');

INSERT INTO iam_menu (id, application_id, parent_id, name, path, view_path, route_name, kind, match_mode, access_mode, sort_order, enabled)
SELECT 130004, parent.application_id, parent.id, '应用目录', '/platform/iam/applications', 'platform.iam.applications', 'platform.iam.applications', 'PAGE', 'ANY', 'ACTION', 1, TRUE FROM iam_menu parent JOIN iam_application app ON app.id = parent.application_id
WHERE app.code = 'iam-platform' AND parent.route_name = 'platform.iam.config' AND NOT EXISTS (SELECT 1 FROM iam_menu WHERE application_id = parent.application_id AND route_name = 'platform.iam.applications');

INSERT INTO iam_menu (id, application_id, parent_id, name, path, view_path, route_name, kind, match_mode, access_mode, sort_order, enabled)
SELECT 130005, parent.application_id, parent.id, '套餐', '/platform/iam/plans', 'platform.iam.plans', 'platform.iam.plans', 'PAGE', 'ANY', 'ACTION', 2, TRUE FROM iam_menu parent JOIN iam_application app ON app.id = parent.application_id
WHERE app.code = 'iam-platform' AND parent.route_name = 'platform.iam.config' AND NOT EXISTS (SELECT 1 FROM iam_menu WHERE application_id = parent.application_id AND route_name = 'platform.iam.plans');

INSERT INTO iam_menu (id, application_id, parent_id, name, path, view_path, route_name, kind, match_mode, access_mode, sort_order, enabled)
SELECT 130006, parent.application_id, parent.id, '共享角色', '/platform/iam/shared/roles', 'platform.iam.shared.roles', 'platform.iam.shared.roles', 'PAGE', 'ANY', 'ACTION', 3, TRUE FROM iam_menu parent JOIN iam_application app ON app.id = parent.application_id
WHERE app.code = 'iam-platform' AND parent.route_name = 'platform.iam.config' AND NOT EXISTS (SELECT 1 FROM iam_menu WHERE application_id = parent.application_id AND route_name = 'platform.iam.shared.roles');

INSERT INTO iam_menu (id, application_id, parent_id, name, path, view_path, route_name, kind, match_mode, access_mode, sort_order, enabled)
SELECT 130007, app.id, NULL, '平台管理', '/platform/iam/manage', NULL, 'platform.iam.manage', 'DIRECTORY', 'ANY', 'ACTION', 3, TRUE FROM iam_application app
WHERE app.code = 'iam-platform' AND NOT EXISTS (SELECT 1 FROM iam_menu WHERE application_id = app.id AND route_name = 'platform.iam.manage');

INSERT INTO iam_menu (id, application_id, parent_id, name, path, view_path, route_name, kind, match_mode, access_mode, sort_order, enabled)
SELECT 130025, parent.application_id, parent.id, '全局账号', '/platform/iam/accounts', 'platform.iam.accounts', 'platform.iam.accounts', 'PAGE', 'ANY', 'ACTION', 1, TRUE FROM iam_menu parent JOIN iam_application app ON app.id = parent.application_id
WHERE app.code = 'iam-platform' AND parent.route_name = 'platform.iam.manage' AND NOT EXISTS (SELECT 1 FROM iam_menu WHERE application_id = parent.application_id AND route_name = 'platform.iam.accounts');

INSERT INTO iam_menu (id, application_id, parent_id, name, path, view_path, route_name, kind, match_mode, access_mode, sort_order, enabled)
SELECT 130008, parent.application_id, parent.id, '平台人员', '/platform/iam/personnel', 'platform.iam.personnel', 'platform.iam.personnel', 'PAGE', 'ANY', 'ACTION', 2, TRUE FROM iam_menu parent JOIN iam_application app ON app.id = parent.application_id
WHERE app.code = 'iam-platform' AND parent.route_name = 'platform.iam.manage' AND NOT EXISTS (SELECT 1 FROM iam_menu WHERE application_id = parent.application_id AND route_name = 'platform.iam.personnel');

INSERT INTO iam_menu (id, application_id, parent_id, name, path, view_path, route_name, kind, match_mode, access_mode, sort_order, enabled)
SELECT 130009, parent.application_id, parent.id, '角色与授权', '/platform/iam/authorization', 'platform.iam.authorization', 'platform.iam.authorization', 'PAGE', 'ANY', 'ACTION', 3, TRUE FROM iam_menu parent JOIN iam_application app ON app.id = parent.application_id
WHERE app.code = 'iam-platform' AND parent.route_name = 'platform.iam.manage' AND NOT EXISTS (SELECT 1 FROM iam_menu WHERE application_id = parent.application_id AND route_name = 'platform.iam.authorization');

INSERT INTO iam_menu (id, application_id, parent_id, name, path, view_path, route_name, kind, match_mode, access_mode, sort_order, enabled)
SELECT 130010, app.id, NULL, '安全与运维', '/platform/iam/security', NULL, 'platform.iam.security', 'DIRECTORY', 'ANY', 'ACTION', 4, TRUE FROM iam_application app
WHERE app.code = 'iam-platform' AND NOT EXISTS (SELECT 1 FROM iam_menu WHERE application_id = app.id AND route_name = 'platform.iam.security');

INSERT INTO iam_menu (id, application_id, parent_id, name, path, view_path, route_name, kind, match_mode, access_mode, sort_order, enabled)
SELECT 130011, parent.application_id, parent.id, '事件与审计', '/security/iam/authorization/audit', 'security.iam.authorization.audit', 'security.iam.authorization.audit', 'PAGE', 'ANY', 'ACTION', 1, TRUE FROM iam_menu parent JOIN iam_application app ON app.id = parent.application_id
WHERE app.code = 'iam-platform' AND parent.route_name = 'platform.iam.security' AND NOT EXISTS (SELECT 1 FROM iam_menu WHERE application_id = parent.application_id AND route_name = 'security.iam.authorization.audit');

INSERT INTO iam_menu (id, application_id, parent_id, name, path, view_path, route_name, kind, match_mode, access_mode, sort_order, enabled)
SELECT 130012, app.id, NULL, '组织管理', '/org/iam/organization', NULL, 'org.iam.organization', 'DIRECTORY', 'ANY', 'ACTION', 1, TRUE FROM iam_application app
WHERE app.code = 'iam-tenant' AND NOT EXISTS (SELECT 1 FROM iam_menu WHERE application_id = app.id AND route_name = 'org.iam.organization');

INSERT INTO iam_menu (id, application_id, parent_id, name, path, view_path, route_name, kind, match_mode, access_mode, sort_order, enabled)
SELECT 130013, parent.application_id, parent.id, '成员与部门', '/org/iam/members', 'org.iam.members', 'org.iam.members', 'PAGE', 'ANY', 'ACTION', 1, TRUE FROM iam_menu parent JOIN iam_application app ON app.id = parent.application_id
WHERE app.code = 'iam-tenant' AND parent.route_name = 'org.iam.organization' AND NOT EXISTS (SELECT 1 FROM iam_menu WHERE application_id = parent.application_id AND route_name = 'org.iam.members');

INSERT INTO iam_menu (id, application_id, parent_id, name, path, view_path, route_name, kind, match_mode, access_mode, sort_order, enabled)
SELECT 130014, parent.application_id, parent.id, '用户组', '/org/iam/groups', 'org.iam.groups', 'org.iam.groups', 'PAGE', 'ANY', 'ACTION', 2, TRUE FROM iam_menu parent JOIN iam_application app ON app.id = parent.application_id
WHERE app.code = 'iam-tenant' AND parent.route_name = 'org.iam.organization' AND NOT EXISTS (SELECT 1 FROM iam_menu WHERE application_id = parent.application_id AND route_name = 'org.iam.groups');

INSERT INTO iam_menu (id, application_id, parent_id, name, path, view_path, route_name, kind, match_mode, access_mode, sort_order, enabled)
SELECT 130015, parent.application_id, parent.id, '组织设置', '/org/iam/settings', 'org.iam.settings', 'org.iam.settings', 'PAGE', 'ANY', 'ACTION', 3, TRUE FROM iam_menu parent JOIN iam_application app ON app.id = parent.application_id
WHERE app.code = 'iam-tenant' AND parent.route_name = 'org.iam.organization' AND NOT EXISTS (SELECT 1 FROM iam_menu WHERE application_id = parent.application_id AND route_name = 'org.iam.settings');

INSERT INTO iam_menu (id, application_id, parent_id, name, path, view_path, route_name, kind, match_mode, access_mode, sort_order, enabled)
SELECT 130016, app.id, NULL, '权限与应用', '/org/iam/authorization/root', NULL, 'org.iam.authorization.root', 'DIRECTORY', 'ANY', 'ACTION', 2, TRUE FROM iam_application app
WHERE app.code = 'iam-tenant' AND NOT EXISTS (SELECT 1 FROM iam_menu WHERE application_id = app.id AND route_name = 'org.iam.authorization.root');

INSERT INTO iam_menu (id, application_id, parent_id, name, path, view_path, route_name, kind, match_mode, access_mode, sort_order, enabled)
SELECT 130017, parent.application_id, parent.id, '角色与授权', '/org/iam/authorization', 'org.iam.authorization', 'org.iam.authorization', 'PAGE', 'ANY', 'ACTION', 1, TRUE FROM iam_menu parent JOIN iam_application app ON app.id = parent.application_id
WHERE app.code = 'iam-tenant' AND parent.route_name = 'org.iam.authorization.root' AND NOT EXISTS (SELECT 1 FROM iam_menu WHERE application_id = parent.application_id AND route_name = 'org.iam.authorization');

INSERT INTO iam_menu (id, application_id, parent_id, name, path, view_path, route_name, kind, match_mode, access_mode, sort_order, enabled)
SELECT 130018, parent.application_id, parent.id, '应用管理', '/org/iam/applications', 'org.iam.applications', 'org.iam.applications', 'PAGE', 'ANY', 'ACTION', 2, TRUE FROM iam_menu parent JOIN iam_application app ON app.id = parent.application_id
WHERE app.code = 'iam-tenant' AND parent.route_name = 'org.iam.authorization.root' AND NOT EXISTS (SELECT 1 FROM iam_menu WHERE application_id = parent.application_id AND route_name = 'org.iam.applications');

INSERT INTO iam_menu (id, application_id, parent_id, name, path, view_path, route_name, kind, match_mode, access_mode, sort_order, enabled)
SELECT 130019, app.id, NULL, '安全与合规', '/org/iam/compliance', NULL, 'org.iam.compliance', 'DIRECTORY', 'ANY', 'ACTION', 3, TRUE FROM iam_application app
WHERE app.code = 'iam-tenant' AND NOT EXISTS (SELECT 1 FROM iam_menu WHERE application_id = app.id AND route_name = 'org.iam.compliance');

INSERT INTO iam_menu (id, application_id, parent_id, name, path, view_path, route_name, kind, match_mode, access_mode, sort_order, enabled)
SELECT 130020, parent.application_id, parent.id, '成员权限', '/security/iam/member/permissions', 'security.iam.member.permissions', 'security.iam.member.permissions', 'PAGE', 'ANY', 'ACTION', 1, TRUE FROM iam_menu parent JOIN iam_application app ON app.id = parent.application_id
WHERE app.code = 'iam-tenant' AND parent.route_name = 'org.iam.compliance' AND NOT EXISTS (SELECT 1 FROM iam_menu WHERE application_id = parent.application_id AND route_name = 'security.iam.member.permissions');

INSERT INTO iam_menu (id, application_id, parent_id, name, path, view_path, route_name, kind, match_mode, access_mode, sort_order, enabled)
SELECT 130021, parent.application_id, parent.id, '审计', '/security/iam/authorization/audit', 'security.iam.authorization.audit', 'security.iam.authorization.audit', 'PAGE', 'ANY', 'ACTION', 2, TRUE FROM iam_menu parent JOIN iam_application app ON app.id = parent.application_id
WHERE app.code = 'iam-tenant' AND parent.route_name = 'org.iam.compliance' AND NOT EXISTS (SELECT 1 FROM iam_menu WHERE application_id = parent.application_id AND route_name = 'security.iam.authorization.audit');

INSERT INTO iam_menu (id, application_id, parent_id, name, path, view_path, route_name, kind, match_mode, access_mode, sort_order, enabled)
SELECT 130022, app.id, NULL, '工作台', '/org/iam/workspace', NULL, 'org.iam.workspace', 'DIRECTORY', 'ANY', 'ACTION', 4, TRUE FROM iam_application app
WHERE app.code = 'iam-tenant' AND NOT EXISTS (SELECT 1 FROM iam_menu WHERE application_id = app.id AND route_name = 'org.iam.workspace');

INSERT INTO iam_menu (id, application_id, parent_id, name, path, view_path, route_name, kind, match_mode, access_mode, sort_order, enabled)
SELECT 130023, parent.application_id, parent.id, '工作台', '/org/iam/workbench', 'org.iam.workbench', 'org.iam.workbench', 'PAGE', 'ANY', 'ACTION', 1, TRUE FROM iam_menu parent JOIN iam_application app ON app.id = parent.application_id
WHERE app.code = 'iam-tenant' AND parent.route_name = 'org.iam.workspace' AND NOT EXISTS (SELECT 1 FROM iam_menu WHERE application_id = parent.application_id AND route_name = 'org.iam.workbench');

INSERT INTO iam_menu (id, application_id, parent_id, name, path, view_path, route_name, kind, match_mode, access_mode, sort_order, enabled)
SELECT 130024, parent.application_id, parent.id, '通讯录', '/org/iam/directory', 'org.iam.directory', 'org.iam.directory', 'PAGE', 'ANY', 'ACTION', 2, TRUE FROM iam_menu parent JOIN iam_application app ON app.id = parent.application_id
WHERE app.code = 'iam-tenant' AND parent.route_name = 'org.iam.workspace' AND NOT EXISTS (SELECT 1 FROM iam_menu WHERE application_id = parent.application_id AND route_name = 'org.iam.directory');

-- 菜单与操作关联：ACTION 访问模式下缺少关联的菜单一律不可见。
INSERT INTO iam_menu_action (application_id, menu_id, action_id)
SELECT menu.application_id, menu.id, action.id FROM iam_menu menu JOIN iam_application app ON app.id = menu.application_id JOIN iam_action action ON action.application_id = menu.application_id
WHERE app.code = 'iam-platform' AND menu.route_name = 'platform.iam.tenant' AND action.code = 'iam-platform:tenant:read' AND NOT EXISTS (SELECT 1 FROM iam_menu_action WHERE application_id = menu.application_id AND menu_id = menu.id AND action_id = action.id);

INSERT INTO iam_menu_action (application_id, menu_id, action_id)
SELECT menu.application_id, menu.id, action.id FROM iam_menu menu JOIN iam_application app ON app.id = menu.application_id JOIN iam_action action ON action.application_id = menu.application_id
WHERE app.code = 'iam-platform' AND menu.route_name = 'platform.iam.tenants' AND action.code = 'iam-platform:tenant:read' AND NOT EXISTS (SELECT 1 FROM iam_menu_action WHERE application_id = menu.application_id AND menu_id = menu.id AND action_id = action.id);

INSERT INTO iam_menu_action (application_id, menu_id, action_id)
SELECT menu.application_id, menu.id, action.id FROM iam_menu menu JOIN iam_application app ON app.id = menu.application_id JOIN iam_action action ON action.application_id = menu.application_id
WHERE app.code = 'iam-platform' AND menu.route_name = 'platform.iam.config' AND action.code = 'iam-platform:application:read' AND NOT EXISTS (SELECT 1 FROM iam_menu_action WHERE application_id = menu.application_id AND menu_id = menu.id AND action_id = action.id);

INSERT INTO iam_menu_action (application_id, menu_id, action_id)
SELECT menu.application_id, menu.id, action.id FROM iam_menu menu JOIN iam_application app ON app.id = menu.application_id JOIN iam_action action ON action.application_id = menu.application_id
WHERE app.code = 'iam-platform' AND menu.route_name = 'platform.iam.config' AND action.code = 'iam-platform:plan:read' AND NOT EXISTS (SELECT 1 FROM iam_menu_action WHERE application_id = menu.application_id AND menu_id = menu.id AND action_id = action.id);

INSERT INTO iam_menu_action (application_id, menu_id, action_id)
SELECT menu.application_id, menu.id, action.id FROM iam_menu menu JOIN iam_application app ON app.id = menu.application_id JOIN iam_action action ON action.application_id = menu.application_id
WHERE app.code = 'iam-platform' AND menu.route_name = 'platform.iam.config' AND action.code = 'iam-platform:shared-role:read' AND NOT EXISTS (SELECT 1 FROM iam_menu_action WHERE application_id = menu.application_id AND menu_id = menu.id AND action_id = action.id);

INSERT INTO iam_menu_action (application_id, menu_id, action_id)
SELECT menu.application_id, menu.id, action.id FROM iam_menu menu JOIN iam_application app ON app.id = menu.application_id JOIN iam_action action ON action.application_id = menu.application_id
WHERE app.code = 'iam-platform' AND menu.route_name = 'platform.iam.applications' AND action.code = 'iam-platform:application:read' AND NOT EXISTS (SELECT 1 FROM iam_menu_action WHERE application_id = menu.application_id AND menu_id = menu.id AND action_id = action.id);

INSERT INTO iam_menu_action (application_id, menu_id, action_id)
SELECT menu.application_id, menu.id, action.id FROM iam_menu menu JOIN iam_application app ON app.id = menu.application_id JOIN iam_action action ON action.application_id = menu.application_id
WHERE app.code = 'iam-platform' AND menu.route_name = 'platform.iam.plans' AND action.code = 'iam-platform:plan:read' AND NOT EXISTS (SELECT 1 FROM iam_menu_action WHERE application_id = menu.application_id AND menu_id = menu.id AND action_id = action.id);

INSERT INTO iam_menu_action (application_id, menu_id, action_id)
SELECT menu.application_id, menu.id, action.id FROM iam_menu menu JOIN iam_application app ON app.id = menu.application_id JOIN iam_action action ON action.application_id = menu.application_id
WHERE app.code = 'iam-platform' AND menu.route_name = 'platform.iam.shared.roles' AND action.code = 'iam-platform:shared-role:read' AND NOT EXISTS (SELECT 1 FROM iam_menu_action WHERE application_id = menu.application_id AND menu_id = menu.id AND action_id = action.id);

INSERT INTO iam_menu_action (application_id, menu_id, action_id)
SELECT menu.application_id, menu.id, action.id FROM iam_menu menu JOIN iam_application app ON app.id = menu.application_id JOIN iam_action action ON action.application_id = menu.application_id
WHERE app.code = 'iam-platform' AND menu.route_name = 'platform.iam.manage' AND action.code = 'iam-platform:assignment:read' AND NOT EXISTS (SELECT 1 FROM iam_menu_action WHERE application_id = menu.application_id AND menu_id = menu.id AND action_id = action.id);

INSERT INTO iam_menu_action (application_id, menu_id, action_id)
SELECT menu.application_id, menu.id, action.id FROM iam_menu menu JOIN iam_application app ON app.id = menu.application_id JOIN iam_action action ON action.application_id = menu.application_id
WHERE app.code = 'iam-platform' AND menu.route_name = 'platform.iam.manage' AND action.code = 'iam-platform:delegation:read' AND NOT EXISTS (SELECT 1 FROM iam_menu_action WHERE application_id = menu.application_id AND menu_id = menu.id AND action_id = action.id);

INSERT INTO iam_menu_action (application_id, menu_id, action_id)
SELECT menu.application_id, menu.id, action.id FROM iam_menu menu JOIN iam_application app ON app.id = menu.application_id JOIN iam_action action ON action.application_id = menu.application_id
WHERE app.code = 'iam-platform' AND menu.route_name = 'platform.iam.manage' AND action.code = 'iam-platform:account:read' AND NOT EXISTS (SELECT 1 FROM iam_menu_action WHERE application_id = menu.application_id AND menu_id = menu.id AND action_id = action.id);

INSERT INTO iam_menu_action (application_id, menu_id, action_id)
SELECT menu.application_id, menu.id, action.id FROM iam_menu menu JOIN iam_application app ON app.id = menu.application_id JOIN iam_action action ON action.application_id = menu.application_id
WHERE app.code = 'iam-platform' AND menu.route_name = 'platform.iam.manage' AND action.code = 'iam-platform:member:read' AND NOT EXISTS (SELECT 1 FROM iam_menu_action WHERE application_id = menu.application_id AND menu_id = menu.id AND action_id = action.id);

INSERT INTO iam_menu_action (application_id, menu_id, action_id)
SELECT menu.application_id, menu.id, action.id FROM iam_menu menu JOIN iam_application app ON app.id = menu.application_id JOIN iam_action action ON action.application_id = menu.application_id
WHERE app.code = 'iam-platform' AND menu.route_name = 'platform.iam.manage' AND action.code = 'iam-platform:role:read' AND NOT EXISTS (SELECT 1 FROM iam_menu_action WHERE application_id = menu.application_id AND menu_id = menu.id AND action_id = action.id);

INSERT INTO iam_menu_action (application_id, menu_id, action_id)
SELECT menu.application_id, menu.id, action.id FROM iam_menu menu JOIN iam_application app ON app.id = menu.application_id JOIN iam_action action ON action.application_id = menu.application_id
WHERE app.code = 'iam-platform' AND menu.route_name = 'platform.iam.accounts' AND action.code = 'iam-platform:account:read' AND NOT EXISTS (SELECT 1 FROM iam_menu_action WHERE application_id = menu.application_id AND menu_id = menu.id AND action_id = action.id);

INSERT INTO iam_menu_action (application_id, menu_id, action_id)
SELECT menu.application_id, menu.id, action.id FROM iam_menu menu JOIN iam_application app ON app.id = menu.application_id JOIN iam_action action ON action.application_id = menu.application_id
WHERE app.code = 'iam-platform' AND menu.route_name = 'platform.iam.personnel' AND action.code = 'iam-platform:member:read' AND NOT EXISTS (SELECT 1 FROM iam_menu_action WHERE application_id = menu.application_id AND menu_id = menu.id AND action_id = action.id);

INSERT INTO iam_menu_action (application_id, menu_id, action_id)
SELECT menu.application_id, menu.id, action.id FROM iam_menu menu JOIN iam_application app ON app.id = menu.application_id JOIN iam_action action ON action.application_id = menu.application_id
WHERE app.code = 'iam-platform' AND menu.route_name = 'platform.iam.authorization' AND action.code = 'iam-platform:assignment:read' AND NOT EXISTS (SELECT 1 FROM iam_menu_action WHERE application_id = menu.application_id AND menu_id = menu.id AND action_id = action.id);

INSERT INTO iam_menu_action (application_id, menu_id, action_id)
SELECT menu.application_id, menu.id, action.id FROM iam_menu menu JOIN iam_application app ON app.id = menu.application_id JOIN iam_action action ON action.application_id = menu.application_id
WHERE app.code = 'iam-platform' AND menu.route_name = 'platform.iam.authorization' AND action.code = 'iam-platform:delegation:read' AND NOT EXISTS (SELECT 1 FROM iam_menu_action WHERE application_id = menu.application_id AND menu_id = menu.id AND action_id = action.id);

INSERT INTO iam_menu_action (application_id, menu_id, action_id)
SELECT menu.application_id, menu.id, action.id FROM iam_menu menu JOIN iam_application app ON app.id = menu.application_id JOIN iam_action action ON action.application_id = menu.application_id
WHERE app.code = 'iam-platform' AND menu.route_name = 'platform.iam.authorization' AND action.code = 'iam-platform:role:read' AND NOT EXISTS (SELECT 1 FROM iam_menu_action WHERE application_id = menu.application_id AND menu_id = menu.id AND action_id = action.id);

INSERT INTO iam_menu_action (application_id, menu_id, action_id)
SELECT menu.application_id, menu.id, action.id FROM iam_menu menu JOIN iam_application app ON app.id = menu.application_id JOIN iam_action action ON action.application_id = menu.application_id
WHERE app.code = 'iam-platform' AND menu.route_name = 'platform.iam.security' AND action.code = 'iam-platform:audit:read' AND NOT EXISTS (SELECT 1 FROM iam_menu_action WHERE application_id = menu.application_id AND menu_id = menu.id AND action_id = action.id);

INSERT INTO iam_menu_action (application_id, menu_id, action_id)
SELECT menu.application_id, menu.id, action.id FROM iam_menu menu JOIN iam_application app ON app.id = menu.application_id JOIN iam_action action ON action.application_id = menu.application_id
WHERE app.code = 'iam-platform' AND menu.route_name = 'security.iam.authorization.audit' AND action.code = 'iam-platform:audit:read' AND NOT EXISTS (SELECT 1 FROM iam_menu_action WHERE application_id = menu.application_id AND menu_id = menu.id AND action_id = action.id);

INSERT INTO iam_menu_action (application_id, menu_id, action_id)
SELECT menu.application_id, menu.id, action.id FROM iam_menu menu JOIN iam_application app ON app.id = menu.application_id JOIN iam_action action ON action.application_id = menu.application_id
WHERE app.code = 'iam-tenant' AND menu.route_name = 'org.iam.organization' AND action.code = 'iam-tenant:department:read' AND NOT EXISTS (SELECT 1 FROM iam_menu_action WHERE application_id = menu.application_id AND menu_id = menu.id AND action_id = action.id);

INSERT INTO iam_menu_action (application_id, menu_id, action_id)
SELECT menu.application_id, menu.id, action.id FROM iam_menu menu JOIN iam_application app ON app.id = menu.application_id JOIN iam_action action ON action.application_id = menu.application_id
WHERE app.code = 'iam-tenant' AND menu.route_name = 'org.iam.organization' AND action.code = 'iam-tenant:group:read' AND NOT EXISTS (SELECT 1 FROM iam_menu_action WHERE application_id = menu.application_id AND menu_id = menu.id AND action_id = action.id);

INSERT INTO iam_menu_action (application_id, menu_id, action_id)
SELECT menu.application_id, menu.id, action.id FROM iam_menu menu JOIN iam_application app ON app.id = menu.application_id JOIN iam_action action ON action.application_id = menu.application_id
WHERE app.code = 'iam-tenant' AND menu.route_name = 'org.iam.organization' AND action.code = 'iam-tenant:member:read' AND NOT EXISTS (SELECT 1 FROM iam_menu_action WHERE application_id = menu.application_id AND menu_id = menu.id AND action_id = action.id);

INSERT INTO iam_menu_action (application_id, menu_id, action_id)
SELECT menu.application_id, menu.id, action.id FROM iam_menu menu JOIN iam_application app ON app.id = menu.application_id JOIN iam_action action ON action.application_id = menu.application_id
WHERE app.code = 'iam-tenant' AND menu.route_name = 'org.iam.organization' AND action.code = 'iam-tenant:settings:read' AND NOT EXISTS (SELECT 1 FROM iam_menu_action WHERE application_id = menu.application_id AND menu_id = menu.id AND action_id = action.id);

INSERT INTO iam_menu_action (application_id, menu_id, action_id)
SELECT menu.application_id, menu.id, action.id FROM iam_menu menu JOIN iam_application app ON app.id = menu.application_id JOIN iam_action action ON action.application_id = menu.application_id
WHERE app.code = 'iam-tenant' AND menu.route_name = 'org.iam.members' AND action.code = 'iam-tenant:department:read' AND NOT EXISTS (SELECT 1 FROM iam_menu_action WHERE application_id = menu.application_id AND menu_id = menu.id AND action_id = action.id);

INSERT INTO iam_menu_action (application_id, menu_id, action_id)
SELECT menu.application_id, menu.id, action.id FROM iam_menu menu JOIN iam_application app ON app.id = menu.application_id JOIN iam_action action ON action.application_id = menu.application_id
WHERE app.code = 'iam-tenant' AND menu.route_name = 'org.iam.members' AND action.code = 'iam-tenant:member:read' AND NOT EXISTS (SELECT 1 FROM iam_menu_action WHERE application_id = menu.application_id AND menu_id = menu.id AND action_id = action.id);

INSERT INTO iam_menu_action (application_id, menu_id, action_id)
SELECT menu.application_id, menu.id, action.id FROM iam_menu menu JOIN iam_application app ON app.id = menu.application_id JOIN iam_action action ON action.application_id = menu.application_id
WHERE app.code = 'iam-tenant' AND menu.route_name = 'org.iam.groups' AND action.code = 'iam-tenant:group:read' AND NOT EXISTS (SELECT 1 FROM iam_menu_action WHERE application_id = menu.application_id AND menu_id = menu.id AND action_id = action.id);

INSERT INTO iam_menu_action (application_id, menu_id, action_id)
SELECT menu.application_id, menu.id, action.id FROM iam_menu menu JOIN iam_application app ON app.id = menu.application_id JOIN iam_action action ON action.application_id = menu.application_id
WHERE app.code = 'iam-tenant' AND menu.route_name = 'org.iam.settings' AND action.code = 'iam-tenant:settings:read' AND NOT EXISTS (SELECT 1 FROM iam_menu_action WHERE application_id = menu.application_id AND menu_id = menu.id AND action_id = action.id);

INSERT INTO iam_menu_action (application_id, menu_id, action_id)
SELECT menu.application_id, menu.id, action.id FROM iam_menu menu JOIN iam_application app ON app.id = menu.application_id JOIN iam_action action ON action.application_id = menu.application_id
WHERE app.code = 'iam-tenant' AND menu.route_name = 'org.iam.authorization.root' AND action.code = 'iam-tenant:application:read' AND NOT EXISTS (SELECT 1 FROM iam_menu_action WHERE application_id = menu.application_id AND menu_id = menu.id AND action_id = action.id);

INSERT INTO iam_menu_action (application_id, menu_id, action_id)
SELECT menu.application_id, menu.id, action.id FROM iam_menu menu JOIN iam_application app ON app.id = menu.application_id JOIN iam_action action ON action.application_id = menu.application_id
WHERE app.code = 'iam-tenant' AND menu.route_name = 'org.iam.authorization.root' AND action.code = 'iam-tenant:assignment:read' AND NOT EXISTS (SELECT 1 FROM iam_menu_action WHERE application_id = menu.application_id AND menu_id = menu.id AND action_id = action.id);

INSERT INTO iam_menu_action (application_id, menu_id, action_id)
SELECT menu.application_id, menu.id, action.id FROM iam_menu menu JOIN iam_application app ON app.id = menu.application_id JOIN iam_action action ON action.application_id = menu.application_id
WHERE app.code = 'iam-tenant' AND menu.route_name = 'org.iam.authorization.root' AND action.code = 'iam-tenant:delegation:read' AND NOT EXISTS (SELECT 1 FROM iam_menu_action WHERE application_id = menu.application_id AND menu_id = menu.id AND action_id = action.id);

INSERT INTO iam_menu_action (application_id, menu_id, action_id)
SELECT menu.application_id, menu.id, action.id FROM iam_menu menu JOIN iam_application app ON app.id = menu.application_id JOIN iam_action action ON action.application_id = menu.application_id
WHERE app.code = 'iam-tenant' AND menu.route_name = 'org.iam.authorization.root' AND action.code = 'iam-tenant:role:read' AND NOT EXISTS (SELECT 1 FROM iam_menu_action WHERE application_id = menu.application_id AND menu_id = menu.id AND action_id = action.id);

INSERT INTO iam_menu_action (application_id, menu_id, action_id)
SELECT menu.application_id, menu.id, action.id FROM iam_menu menu JOIN iam_application app ON app.id = menu.application_id JOIN iam_action action ON action.application_id = menu.application_id
WHERE app.code = 'iam-tenant' AND menu.route_name = 'org.iam.authorization' AND action.code = 'iam-tenant:assignment:read' AND NOT EXISTS (SELECT 1 FROM iam_menu_action WHERE application_id = menu.application_id AND menu_id = menu.id AND action_id = action.id);

INSERT INTO iam_menu_action (application_id, menu_id, action_id)
SELECT menu.application_id, menu.id, action.id FROM iam_menu menu JOIN iam_application app ON app.id = menu.application_id JOIN iam_action action ON action.application_id = menu.application_id
WHERE app.code = 'iam-tenant' AND menu.route_name = 'org.iam.authorization' AND action.code = 'iam-tenant:delegation:read' AND NOT EXISTS (SELECT 1 FROM iam_menu_action WHERE application_id = menu.application_id AND menu_id = menu.id AND action_id = action.id);

INSERT INTO iam_menu_action (application_id, menu_id, action_id)
SELECT menu.application_id, menu.id, action.id FROM iam_menu menu JOIN iam_application app ON app.id = menu.application_id JOIN iam_action action ON action.application_id = menu.application_id
WHERE app.code = 'iam-tenant' AND menu.route_name = 'org.iam.authorization' AND action.code = 'iam-tenant:role:read' AND NOT EXISTS (SELECT 1 FROM iam_menu_action WHERE application_id = menu.application_id AND menu_id = menu.id AND action_id = action.id);

INSERT INTO iam_menu_action (application_id, menu_id, action_id)
SELECT menu.application_id, menu.id, action.id FROM iam_menu menu JOIN iam_application app ON app.id = menu.application_id JOIN iam_action action ON action.application_id = menu.application_id
WHERE app.code = 'iam-tenant' AND menu.route_name = 'org.iam.applications' AND action.code = 'iam-tenant:application:read' AND NOT EXISTS (SELECT 1 FROM iam_menu_action WHERE application_id = menu.application_id AND menu_id = menu.id AND action_id = action.id);

INSERT INTO iam_menu_action (application_id, menu_id, action_id)
SELECT menu.application_id, menu.id, action.id FROM iam_menu menu JOIN iam_application app ON app.id = menu.application_id JOIN iam_action action ON action.application_id = menu.application_id
WHERE app.code = 'iam-tenant' AND menu.route_name = 'org.iam.compliance' AND action.code = 'iam-tenant:audit:read' AND NOT EXISTS (SELECT 1 FROM iam_menu_action WHERE application_id = menu.application_id AND menu_id = menu.id AND action_id = action.id);

INSERT INTO iam_menu_action (application_id, menu_id, action_id)
SELECT menu.application_id, menu.id, action.id FROM iam_menu menu JOIN iam_application app ON app.id = menu.application_id JOIN iam_action action ON action.application_id = menu.application_id
WHERE app.code = 'iam-tenant' AND menu.route_name = 'org.iam.compliance' AND action.code = 'iam-tenant:directory-policy:read' AND NOT EXISTS (SELECT 1 FROM iam_menu_action WHERE application_id = menu.application_id AND menu_id = menu.id AND action_id = action.id);

INSERT INTO iam_menu_action (application_id, menu_id, action_id)
SELECT menu.application_id, menu.id, action.id FROM iam_menu menu JOIN iam_application app ON app.id = menu.application_id JOIN iam_action action ON action.application_id = menu.application_id
WHERE app.code = 'iam-tenant' AND menu.route_name = 'org.iam.compliance' AND action.code = 'iam-tenant:field-policy:read' AND NOT EXISTS (SELECT 1 FROM iam_menu_action WHERE application_id = menu.application_id AND menu_id = menu.id AND action_id = action.id);

INSERT INTO iam_menu_action (application_id, menu_id, action_id)
SELECT menu.application_id, menu.id, action.id FROM iam_menu menu JOIN iam_application app ON app.id = menu.application_id JOIN iam_action action ON action.application_id = menu.application_id
WHERE app.code = 'iam-tenant' AND menu.route_name = 'security.iam.member.permissions' AND action.code = 'iam-tenant:directory-policy:read' AND NOT EXISTS (SELECT 1 FROM iam_menu_action WHERE application_id = menu.application_id AND menu_id = menu.id AND action_id = action.id);

INSERT INTO iam_menu_action (application_id, menu_id, action_id)
SELECT menu.application_id, menu.id, action.id FROM iam_menu menu JOIN iam_application app ON app.id = menu.application_id JOIN iam_action action ON action.application_id = menu.application_id
WHERE app.code = 'iam-tenant' AND menu.route_name = 'security.iam.member.permissions' AND action.code = 'iam-tenant:field-policy:read' AND NOT EXISTS (SELECT 1 FROM iam_menu_action WHERE application_id = menu.application_id AND menu_id = menu.id AND action_id = action.id);

INSERT INTO iam_menu_action (application_id, menu_id, action_id)
SELECT menu.application_id, menu.id, action.id FROM iam_menu menu JOIN iam_application app ON app.id = menu.application_id JOIN iam_action action ON action.application_id = menu.application_id
WHERE app.code = 'iam-tenant' AND menu.route_name = 'security.iam.authorization.audit' AND action.code = 'iam-tenant:audit:read' AND NOT EXISTS (SELECT 1 FROM iam_menu_action WHERE application_id = menu.application_id AND menu_id = menu.id AND action_id = action.id);

INSERT INTO iam_menu_action (application_id, menu_id, action_id)
SELECT menu.application_id, menu.id, action.id FROM iam_menu menu JOIN iam_application app ON app.id = menu.application_id JOIN iam_action action ON action.application_id = menu.application_id
WHERE app.code = 'iam-tenant' AND menu.route_name = 'org.iam.workspace' AND action.code = 'iam-tenant:application:read' AND NOT EXISTS (SELECT 1 FROM iam_menu_action WHERE application_id = menu.application_id AND menu_id = menu.id AND action_id = action.id);

INSERT INTO iam_menu_action (application_id, menu_id, action_id)
SELECT menu.application_id, menu.id, action.id FROM iam_menu menu JOIN iam_application app ON app.id = menu.application_id JOIN iam_action action ON action.application_id = menu.application_id
WHERE app.code = 'iam-tenant' AND menu.route_name = 'org.iam.workspace' AND action.code = 'iam-tenant:directory:read' AND NOT EXISTS (SELECT 1 FROM iam_menu_action WHERE application_id = menu.application_id AND menu_id = menu.id AND action_id = action.id);

INSERT INTO iam_menu_action (application_id, menu_id, action_id)
SELECT menu.application_id, menu.id, action.id FROM iam_menu menu JOIN iam_application app ON app.id = menu.application_id JOIN iam_action action ON action.application_id = menu.application_id
WHERE app.code = 'iam-tenant' AND menu.route_name = 'org.iam.workbench' AND action.code = 'iam-tenant:application:read' AND NOT EXISTS (SELECT 1 FROM iam_menu_action WHERE application_id = menu.application_id AND menu_id = menu.id AND action_id = action.id);

INSERT INTO iam_menu_action (application_id, menu_id, action_id)
SELECT menu.application_id, menu.id, action.id FROM iam_menu menu JOIN iam_application app ON app.id = menu.application_id JOIN iam_action action ON action.application_id = menu.application_id
WHERE app.code = 'iam-tenant' AND menu.route_name = 'org.iam.directory' AND action.code = 'iam-tenant:directory:read' AND NOT EXISTS (SELECT 1 FROM iam_menu_action WHERE application_id = menu.application_id AND menu_id = menu.id AND action_id = action.id);

-- 治理角色：每个域恰好一个启用的 SYSTEM 角色，组织初始化依赖这一唯一性。
INSERT INTO iam_role_definition (id, domain, tenant_id, kind, code, name, description, enabled)
SELECT 140001, 'PLATFORM', NULL, 'SYSTEM', 'platform-governance', '平台治理', '平台域全部治理操作', TRUE FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM iam_role_definition WHERE domain = 'PLATFORM' AND tenant_key = 0 AND code = 'platform-governance');

INSERT INTO iam_role_revision (id, role_id, kind, revision, base_revision_id, metadata_overrides)
SELECT 141001, role.id, 'SYSTEM', 1, NULL, CAST('{}' AS JSON) FROM iam_role_definition role
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'platform-governance' AND NOT EXISTS (SELECT 1 FROM iam_role_revision WHERE role_id = role.id AND revision = 1);

INSERT INTO iam_role_definition (id, domain, tenant_id, kind, code, name, description, enabled)
SELECT 140002, 'TENANT', NULL, 'SYSTEM', 'tenant-governance', '组织治理', '组织所有者的全部治理操作', TRUE FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM iam_role_definition WHERE domain = 'TENANT' AND tenant_key = 0 AND code = 'tenant-governance');

INSERT INTO iam_role_revision (id, role_id, kind, revision, base_revision_id, metadata_overrides)
SELECT 141002, role.id, 'SYSTEM', 1, NULL, CAST('{}' AS JSON) FROM iam_role_definition role
WHERE role.domain = 'TENANT' AND role.tenant_key = 0 AND role.code = 'tenant-governance' AND NOT EXISTS (SELECT 1 FROM iam_role_revision WHERE role_id = role.id AND revision = 1);

-- 治理授权：治理角色覆盖本域全部操作，范围为全域。
INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'platform-governance' AND revision.revision = 1 AND action.code = 'iam-platform:account:create' AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'platform-governance' AND revision.revision = 1 AND action.code = 'iam-platform:account:delete' AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'platform-governance' AND revision.revision = 1 AND action.code = 'iam-platform:account:disable' AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'platform-governance' AND revision.revision = 1 AND action.code = 'iam-platform:account:enable' AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'platform-governance' AND revision.revision = 1 AND action.code = 'iam-platform:account:lock' AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'platform-governance' AND revision.revision = 1 AND action.code = 'iam-platform:account:lookup' AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'platform-governance' AND revision.revision = 1 AND action.code = 'iam-platform:account:read' AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'platform-governance' AND revision.revision = 1 AND action.code = 'iam-platform:account:reset-password' AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'platform-governance' AND revision.revision = 1 AND action.code = 'iam-platform:account:unlock' AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'platform-governance' AND revision.revision = 1 AND action.code = 'iam-platform:account:update' AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'platform-governance' AND revision.revision = 1 AND action.code = 'iam-platform:action:create' AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'platform-governance' AND revision.revision = 1 AND action.code = 'iam-platform:action:delete' AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'platform-governance' AND revision.revision = 1 AND action.code = 'iam-platform:action:read' AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'platform-governance' AND revision.revision = 1 AND action.code = 'iam-platform:action:status' AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'platform-governance' AND revision.revision = 1 AND action.code = 'iam-platform:action:update' AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'platform-governance' AND revision.revision = 1 AND action.code = 'iam-platform:application:create' AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'platform-governance' AND revision.revision = 1 AND action.code = 'iam-platform:application:delete' AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'platform-governance' AND revision.revision = 1 AND action.code = 'iam-platform:application:read' AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'platform-governance' AND revision.revision = 1 AND action.code = 'iam-platform:application:status' AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'platform-governance' AND revision.revision = 1 AND action.code = 'iam-platform:application:update' AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'platform-governance' AND revision.revision = 1 AND action.code = 'iam-platform:assignment:create' AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'platform-governance' AND revision.revision = 1 AND action.code = 'iam-platform:assignment:delete' AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'platform-governance' AND revision.revision = 1 AND action.code = 'iam-platform:assignment:read' AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'platform-governance' AND revision.revision = 1 AND action.code = 'iam-platform:assignment:update' AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'platform-governance' AND revision.revision = 1 AND action.code = 'iam-platform:audit:read' AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'platform-governance' AND revision.revision = 1 AND action.code = 'iam-platform:authorization:diagnose' AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'platform-governance' AND revision.revision = 1 AND action.code = 'iam-platform:credential-policy:create' AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'platform-governance' AND revision.revision = 1 AND action.code = 'iam-platform:credential-policy:delete' AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'platform-governance' AND revision.revision = 1 AND action.code = 'iam-platform:credential-policy:read' AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'platform-governance' AND revision.revision = 1 AND action.code = 'iam-platform:credential-policy:update' AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'platform-governance' AND revision.revision = 1 AND action.code = 'iam-platform:delegation:create' AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'platform-governance' AND revision.revision = 1 AND action.code = 'iam-platform:delegation:delete' AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'platform-governance' AND revision.revision = 1 AND action.code = 'iam-platform:delegation:preview' AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'platform-governance' AND revision.revision = 1 AND action.code = 'iam-platform:delegation:read' AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'platform-governance' AND revision.revision = 1 AND action.code = 'iam-platform:delegation:update' AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'platform-governance' AND revision.revision = 1 AND action.code = 'iam-platform:dictionary:create' AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'platform-governance' AND revision.revision = 1 AND action.code = 'iam-platform:dictionary:delete' AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'platform-governance' AND revision.revision = 1 AND action.code = 'iam-platform:dictionary:read' AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'platform-governance' AND revision.revision = 1 AND action.code = 'iam-platform:dictionary:update' AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'platform-governance' AND revision.revision = 1 AND action.code = 'iam-platform:entitlement:preview' AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'platform-governance' AND revision.revision = 1 AND action.code = 'iam-platform:entitlement:read' AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'platform-governance' AND revision.revision = 1 AND action.code = 'iam-platform:entitlement:update' AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'platform-governance' AND revision.revision = 1 AND action.code = 'iam-platform:group:create' AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'platform-governance' AND revision.revision = 1 AND action.code = 'iam-platform:group:delete' AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'platform-governance' AND revision.revision = 1 AND action.code = 'iam-platform:group:preview' AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'platform-governance' AND revision.revision = 1 AND action.code = 'iam-platform:group:read' AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'platform-governance' AND revision.revision = 1 AND action.code = 'iam-platform:group:update' AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'platform-governance' AND revision.revision = 1 AND action.code = 'iam-platform:id-allocation:create' AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'platform-governance' AND revision.revision = 1 AND action.code = 'iam-platform:id-allocation:delete' AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'platform-governance' AND revision.revision = 1 AND action.code = 'iam-platform:id-allocation:read' AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'platform-governance' AND revision.revision = 1 AND action.code = 'iam-platform:id-allocation:update' AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'platform-governance' AND revision.revision = 1 AND action.code = 'iam-platform:lockout-policy:read' AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'platform-governance' AND revision.revision = 1 AND action.code = 'iam-platform:lockout-policy:update' AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'platform-governance' AND revision.revision = 1 AND action.code = 'iam-platform:login-failure-policy:read' AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'platform-governance' AND revision.revision = 1 AND action.code = 'iam-platform:login-failure-policy:update' AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'platform-governance' AND revision.revision = 1 AND action.code = 'iam-platform:member:create' AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'platform-governance' AND revision.revision = 1 AND action.code = 'iam-platform:member:read' AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'platform-governance' AND revision.revision = 1 AND action.code = 'iam-platform:member:remove' AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'platform-governance' AND revision.revision = 1 AND action.code = 'iam-platform:member:status' AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'platform-governance' AND revision.revision = 1 AND action.code = 'iam-platform:member:update' AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'platform-governance' AND revision.revision = 1 AND action.code = 'iam-platform:menu:create' AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'platform-governance' AND revision.revision = 1 AND action.code = 'iam-platform:menu:delete' AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'platform-governance' AND revision.revision = 1 AND action.code = 'iam-platform:menu:read' AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'platform-governance' AND revision.revision = 1 AND action.code = 'iam-platform:menu:update' AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'platform-governance' AND revision.revision = 1 AND action.code = 'iam-platform:plan:create' AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'platform-governance' AND revision.revision = 1 AND action.code = 'iam-platform:plan:read' AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'platform-governance' AND revision.revision = 1 AND action.code = 'iam-platform:plan:update' AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'platform-governance' AND revision.revision = 1 AND action.code = 'iam-platform:resource:create' AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'platform-governance' AND revision.revision = 1 AND action.code = 'iam-platform:resource:delete' AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'platform-governance' AND revision.revision = 1 AND action.code = 'iam-platform:resource:read' AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'platform-governance' AND revision.revision = 1 AND action.code = 'iam-platform:resource:update' AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'platform-governance' AND revision.revision = 1 AND action.code = 'iam-platform:role:create' AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'platform-governance' AND revision.revision = 1 AND action.code = 'iam-platform:role:delete' AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'platform-governance' AND revision.revision = 1 AND action.code = 'iam-platform:role:preview' AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'platform-governance' AND revision.revision = 1 AND action.code = 'iam-platform:role:publish' AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'platform-governance' AND revision.revision = 1 AND action.code = 'iam-platform:role:read' AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'platform-governance' AND revision.revision = 1 AND action.code = 'iam-platform:role:status' AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'platform-governance' AND revision.revision = 1 AND action.code = 'iam-platform:security-policy:create' AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'platform-governance' AND revision.revision = 1 AND action.code = 'iam-platform:security-policy:delete' AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'platform-governance' AND revision.revision = 1 AND action.code = 'iam-platform:security-policy:read' AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'platform-governance' AND revision.revision = 1 AND action.code = 'iam-platform:security-policy:update' AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'platform-governance' AND revision.revision = 1 AND action.code = 'iam-platform:session-policy:read' AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'platform-governance' AND revision.revision = 1 AND action.code = 'iam-platform:session-policy:update' AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'platform-governance' AND revision.revision = 1 AND action.code = 'iam-platform:session:read' AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'platform-governance' AND revision.revision = 1 AND action.code = 'iam-platform:session:revoke' AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'platform-governance' AND revision.revision = 1 AND action.code = 'iam-platform:shared-role:create' AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'platform-governance' AND revision.revision = 1 AND action.code = 'iam-platform:shared-role:delete' AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'platform-governance' AND revision.revision = 1 AND action.code = 'iam-platform:shared-role:preview' AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'platform-governance' AND revision.revision = 1 AND action.code = 'iam-platform:shared-role:publish' AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'platform-governance' AND revision.revision = 1 AND action.code = 'iam-platform:shared-role:read' AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'platform-governance' AND revision.revision = 1 AND action.code = 'iam-platform:shared-role:status' AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'platform-governance' AND revision.revision = 1 AND action.code = 'iam-platform:social-config:create' AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'platform-governance' AND revision.revision = 1 AND action.code = 'iam-platform:social-config:delete' AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'platform-governance' AND revision.revision = 1 AND action.code = 'iam-platform:social-config:read' AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'platform-governance' AND revision.revision = 1 AND action.code = 'iam-platform:social-config:update' AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'platform-governance' AND revision.revision = 1 AND action.code = 'iam-platform:tenant:create' AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'platform-governance' AND revision.revision = 1 AND action.code = 'iam-platform:tenant:preview' AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'platform-governance' AND revision.revision = 1 AND action.code = 'iam-platform:tenant:read' AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'platform-governance' AND revision.revision = 1 AND action.code = 'iam-platform:tenant:update' AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action
WHERE role.domain = 'TENANT' AND role.tenant_key = 0 AND role.code = 'tenant-governance' AND revision.revision = 1 AND action.code = 'iam-tenant:application:read' AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action
WHERE role.domain = 'TENANT' AND role.tenant_key = 0 AND role.code = 'tenant-governance' AND revision.revision = 1 AND action.code = 'iam-tenant:assignment:create' AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action
WHERE role.domain = 'TENANT' AND role.tenant_key = 0 AND role.code = 'tenant-governance' AND revision.revision = 1 AND action.code = 'iam-tenant:assignment:delete' AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action
WHERE role.domain = 'TENANT' AND role.tenant_key = 0 AND role.code = 'tenant-governance' AND revision.revision = 1 AND action.code = 'iam-tenant:assignment:read' AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action
WHERE role.domain = 'TENANT' AND role.tenant_key = 0 AND role.code = 'tenant-governance' AND revision.revision = 1 AND action.code = 'iam-tenant:assignment:update' AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action
WHERE role.domain = 'TENANT' AND role.tenant_key = 0 AND role.code = 'tenant-governance' AND revision.revision = 1 AND action.code = 'iam-tenant:audience:read' AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action
WHERE role.domain = 'TENANT' AND role.tenant_key = 0 AND role.code = 'tenant-governance' AND revision.revision = 1 AND action.code = 'iam-tenant:audience:update' AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action
WHERE role.domain = 'TENANT' AND role.tenant_key = 0 AND role.code = 'tenant-governance' AND revision.revision = 1 AND action.code = 'iam-tenant:audit:read' AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action
WHERE role.domain = 'TENANT' AND role.tenant_key = 0 AND role.code = 'tenant-governance' AND revision.revision = 1 AND action.code = 'iam-tenant:authorization:diagnose' AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action
WHERE role.domain = 'TENANT' AND role.tenant_key = 0 AND role.code = 'tenant-governance' AND revision.revision = 1 AND action.code = 'iam-tenant:delegation:create' AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action
WHERE role.domain = 'TENANT' AND role.tenant_key = 0 AND role.code = 'tenant-governance' AND revision.revision = 1 AND action.code = 'iam-tenant:delegation:delete' AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action
WHERE role.domain = 'TENANT' AND role.tenant_key = 0 AND role.code = 'tenant-governance' AND revision.revision = 1 AND action.code = 'iam-tenant:delegation:preview' AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action
WHERE role.domain = 'TENANT' AND role.tenant_key = 0 AND role.code = 'tenant-governance' AND revision.revision = 1 AND action.code = 'iam-tenant:delegation:read' AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action
WHERE role.domain = 'TENANT' AND role.tenant_key = 0 AND role.code = 'tenant-governance' AND revision.revision = 1 AND action.code = 'iam-tenant:delegation:update' AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action
WHERE role.domain = 'TENANT' AND role.tenant_key = 0 AND role.code = 'tenant-governance' AND revision.revision = 1 AND action.code = 'iam-tenant:department:create' AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action
WHERE role.domain = 'TENANT' AND role.tenant_key = 0 AND role.code = 'tenant-governance' AND revision.revision = 1 AND action.code = 'iam-tenant:department:delete' AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action
WHERE role.domain = 'TENANT' AND role.tenant_key = 0 AND role.code = 'tenant-governance' AND revision.revision = 1 AND action.code = 'iam-tenant:department:read' AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action
WHERE role.domain = 'TENANT' AND role.tenant_key = 0 AND role.code = 'tenant-governance' AND revision.revision = 1 AND action.code = 'iam-tenant:department:update' AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action
WHERE role.domain = 'TENANT' AND role.tenant_key = 0 AND role.code = 'tenant-governance' AND revision.revision = 1 AND action.code = 'iam-tenant:directory-policy:read' AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action
WHERE role.domain = 'TENANT' AND role.tenant_key = 0 AND role.code = 'tenant-governance' AND revision.revision = 1 AND action.code = 'iam-tenant:directory-policy:update' AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action
WHERE role.domain = 'TENANT' AND role.tenant_key = 0 AND role.code = 'tenant-governance' AND revision.revision = 1 AND action.code = 'iam-tenant:directory:read' AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action
WHERE role.domain = 'TENANT' AND role.tenant_key = 0 AND role.code = 'tenant-governance' AND revision.revision = 1 AND action.code = 'iam-tenant:field-policy:read' AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action
WHERE role.domain = 'TENANT' AND role.tenant_key = 0 AND role.code = 'tenant-governance' AND revision.revision = 1 AND action.code = 'iam-tenant:field-policy:update' AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action
WHERE role.domain = 'TENANT' AND role.tenant_key = 0 AND role.code = 'tenant-governance' AND revision.revision = 1 AND action.code = 'iam-tenant:group:create' AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action
WHERE role.domain = 'TENANT' AND role.tenant_key = 0 AND role.code = 'tenant-governance' AND revision.revision = 1 AND action.code = 'iam-tenant:group:delete' AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action
WHERE role.domain = 'TENANT' AND role.tenant_key = 0 AND role.code = 'tenant-governance' AND revision.revision = 1 AND action.code = 'iam-tenant:group:preview' AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action
WHERE role.domain = 'TENANT' AND role.tenant_key = 0 AND role.code = 'tenant-governance' AND revision.revision = 1 AND action.code = 'iam-tenant:group:read' AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action
WHERE role.domain = 'TENANT' AND role.tenant_key = 0 AND role.code = 'tenant-governance' AND revision.revision = 1 AND action.code = 'iam-tenant:group:update' AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action
WHERE role.domain = 'TENANT' AND role.tenant_key = 0 AND role.code = 'tenant-governance' AND revision.revision = 1 AND action.code = 'iam-tenant:member:create' AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action
WHERE role.domain = 'TENANT' AND role.tenant_key = 0 AND role.code = 'tenant-governance' AND revision.revision = 1 AND action.code = 'iam-tenant:member:departments' AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action
WHERE role.domain = 'TENANT' AND role.tenant_key = 0 AND role.code = 'tenant-governance' AND revision.revision = 1 AND action.code = 'iam-tenant:member:export' AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action
WHERE role.domain = 'TENANT' AND role.tenant_key = 0 AND role.code = 'tenant-governance' AND revision.revision = 1 AND action.code = 'iam-tenant:member:read' AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action
WHERE role.domain = 'TENANT' AND role.tenant_key = 0 AND role.code = 'tenant-governance' AND revision.revision = 1 AND action.code = 'iam-tenant:member:remove' AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action
WHERE role.domain = 'TENANT' AND role.tenant_key = 0 AND role.code = 'tenant-governance' AND revision.revision = 1 AND action.code = 'iam-tenant:member:status' AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action
WHERE role.domain = 'TENANT' AND role.tenant_key = 0 AND role.code = 'tenant-governance' AND revision.revision = 1 AND action.code = 'iam-tenant:member:update' AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action
WHERE role.domain = 'TENANT' AND role.tenant_key = 0 AND role.code = 'tenant-governance' AND revision.revision = 1 AND action.code = 'iam-tenant:policy:preview' AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action
WHERE role.domain = 'TENANT' AND role.tenant_key = 0 AND role.code = 'tenant-governance' AND revision.revision = 1 AND action.code = 'iam-tenant:role:create' AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action
WHERE role.domain = 'TENANT' AND role.tenant_key = 0 AND role.code = 'tenant-governance' AND revision.revision = 1 AND action.code = 'iam-tenant:role:delete' AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action
WHERE role.domain = 'TENANT' AND role.tenant_key = 0 AND role.code = 'tenant-governance' AND revision.revision = 1 AND action.code = 'iam-tenant:role:preview' AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action
WHERE role.domain = 'TENANT' AND role.tenant_key = 0 AND role.code = 'tenant-governance' AND revision.revision = 1 AND action.code = 'iam-tenant:role:publish' AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action
WHERE role.domain = 'TENANT' AND role.tenant_key = 0 AND role.code = 'tenant-governance' AND revision.revision = 1 AND action.code = 'iam-tenant:role:read' AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action
WHERE role.domain = 'TENANT' AND role.tenant_key = 0 AND role.code = 'tenant-governance' AND revision.revision = 1 AND action.code = 'iam-tenant:role:status' AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action
WHERE role.domain = 'TENANT' AND role.tenant_key = 0 AND role.code = 'tenant-governance' AND revision.revision = 1 AND action.code = 'iam-tenant:role:upgrade' AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action
WHERE role.domain = 'TENANT' AND role.tenant_key = 0 AND role.code = 'tenant-governance' AND revision.revision = 1 AND action.code = 'iam-tenant:settings:owner-transfer' AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action
WHERE role.domain = 'TENANT' AND role.tenant_key = 0 AND role.code = 'tenant-governance' AND revision.revision = 1 AND action.code = 'iam-tenant:settings:read' AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action
WHERE role.domain = 'TENANT' AND role.tenant_key = 0 AND role.code = 'tenant-governance' AND revision.revision = 1 AND action.code = 'iam-tenant:settings:update' AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

-- 默认策略版本：固定版本被租户策略引用；通讯录默认全组织，字段默认脱敏手机邮箱，上限缺省不额外收紧。
INSERT INTO iam_default_policy_revision (id, kind, revision, definition)
SELECT 150001, 'DIRECTORY', 1, CAST('{"scope": "ALL"}' AS JSON) FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM iam_default_policy_revision WHERE kind = 'DIRECTORY' AND revision = 1);

INSERT INTO iam_default_policy_revision (id, kind, revision, definition)
SELECT 150002, 'FIELD', 1, CAST('{"fields": {"displayName": {"visibility": "FULL", "editable": true}, "avatar": {"visibility": "FULL", "editable": true}, "phone": {"visibility": "MASKED", "editable": false}, "email": {"visibility": "MASKED", "editable": false}}}' AS JSON) FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM iam_default_policy_revision WHERE kind = 'FIELD' AND revision = 1);
