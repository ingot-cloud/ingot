-- 业务时间点统一 UTC；初始化连接也必须明确会话时区。
SET time_zone = '+00:00';

-- IAM 正式冷启动种子：先按顺序执行 001–005 再执行本文件，可重复执行。
-- 由 tools/iam/generate_bootstrap.py 从IAM契约及现有开发者目录生成，不要手工编辑。
-- 全部语句存在即跳过，不覆盖任何人工或业务修改；不含账号与凭证，
-- 新建行的保留号若已被占用，则落到所属区间内下一个空号。
-- 平台首个账号由 provider 冷启动器经安全框架注册用例创建。

-- 发号准备：静态保留标识全部小于起点，运行时发号不会与种子冲突。
INSERT INTO biz_leaf_alloc (biz_tag, max_id, step, description)
SELECT 'iam', 1000000, 100, 'IAM 冷启动发号' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM biz_leaf_alloc WHERE biz_tag = 'iam');

-- 应用：租户域基础应用在组织创建时缺省开通。
INSERT INTO iam_application (id, code, domain, name, icon, description, sort_order, baseline, enabled)
SELECT 100001, 'iam-platform', 'PLATFORM', '平台治理', 'ep:monitor', '平台域身份、目录与授权治理', 1, FALSE, TRUE FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM iam_application WHERE code = 'iam-platform');

INSERT INTO iam_application (id, code, domain, name, icon, description, sort_order, baseline, enabled)
SELECT 100002, 'iam-tenant', 'TENANT', '组织治理', 'ep:office-building', '组织域成员、部门与授权治理', 1, TRUE, TRUE FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM iam_application WHERE code = 'iam-tenant');

INSERT INTO iam_application (id, code, domain, name, icon, description, sort_order, baseline, enabled)
SELECT 100003, 'platform:develop', 'PLATFORM', '开发者平台', 'ep:cpu', '二维码、OAuth2客户端、社交配置与业务ID管理', 2, FALSE, TRUE FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM iam_application WHERE code = 'platform:develop');

-- 资源：声明合法范围与字段能力，供角色发布与字段策略校验取值。
INSERT INTO iam_resource (id, application_id, code, name, scope_capabilities, field_capabilities, enabled)
SELECT COALESCE((SELECT 110001 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_resource taken WHERE taken.id = 110001)), (SELECT COALESCE(MAX(taken.id), 110000) + 1 FROM iam_resource taken WHERE taken.id >= 110000 AND taken.id < 120000)), app.id, 'account', '全局账号', CAST('["ALL", "OBJECT_SET"]' AS JSON), CAST('[]' AS JSON), TRUE FROM iam_application app
WHERE app.code = 'iam-platform' AND NOT EXISTS (SELECT 1 FROM iam_resource WHERE application_id = app.id AND code = 'account');

INSERT INTO iam_resource (id, application_id, code, name, scope_capabilities, field_capabilities, enabled)
SELECT COALESCE((SELECT 110002 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_resource taken WHERE taken.id = 110002)), (SELECT COALESCE(MAX(taken.id), 110000) + 1 FROM iam_resource taken WHERE taken.id >= 110000 AND taken.id < 120000)), app.id, 'action', '操作', CAST('["ALL", "OBJECT_SET"]' AS JSON), CAST('[]' AS JSON), TRUE FROM iam_application app
WHERE app.code = 'iam-platform' AND NOT EXISTS (SELECT 1 FROM iam_resource WHERE application_id = app.id AND code = 'action');

INSERT INTO iam_resource (id, application_id, code, name, scope_capabilities, field_capabilities, enabled)
SELECT COALESCE((SELECT 110003 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_resource taken WHERE taken.id = 110003)), (SELECT COALESCE(MAX(taken.id), 110000) + 1 FROM iam_resource taken WHERE taken.id >= 110000 AND taken.id < 120000)), app.id, 'application', '应用', CAST('["ALL", "OBJECT_SET"]' AS JSON), CAST('[]' AS JSON), TRUE FROM iam_application app
WHERE app.code = 'iam-platform' AND NOT EXISTS (SELECT 1 FROM iam_resource WHERE application_id = app.id AND code = 'application');

INSERT INTO iam_resource (id, application_id, code, name, scope_capabilities, field_capabilities, enabled)
SELECT COALESCE((SELECT 110004 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_resource taken WHERE taken.id = 110004)), (SELECT COALESCE(MAX(taken.id), 110000) + 1 FROM iam_resource taken WHERE taken.id >= 110000 AND taken.id < 120000)), app.id, 'assignment', '角色授权', CAST('["ALL", "OBJECT_SET"]' AS JSON), CAST('[]' AS JSON), TRUE FROM iam_application app
WHERE app.code = 'iam-platform' AND NOT EXISTS (SELECT 1 FROM iam_resource WHERE application_id = app.id AND code = 'assignment');

INSERT INTO iam_resource (id, application_id, code, name, scope_capabilities, field_capabilities, enabled)
SELECT COALESCE((SELECT 110005 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_resource taken WHERE taken.id = 110005)), (SELECT COALESCE(MAX(taken.id), 110000) + 1 FROM iam_resource taken WHERE taken.id >= 110000 AND taken.id < 120000)), app.id, 'audit', '审计记录', CAST('["ALL"]' AS JSON), CAST('[]' AS JSON), TRUE FROM iam_application app
WHERE app.code = 'iam-platform' AND NOT EXISTS (SELECT 1 FROM iam_resource WHERE application_id = app.id AND code = 'audit');

INSERT INTO iam_resource (id, application_id, code, name, scope_capabilities, field_capabilities, enabled)
SELECT COALESCE((SELECT 110006 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_resource taken WHERE taken.id = 110006)), (SELECT COALESCE(MAX(taken.id), 110000) + 1 FROM iam_resource taken WHERE taken.id >= 110000 AND taken.id < 120000)), app.id, 'authorization', '授权诊断', CAST('["ALL"]' AS JSON), CAST('[]' AS JSON), TRUE FROM iam_application app
WHERE app.code = 'iam-platform' AND NOT EXISTS (SELECT 1 FROM iam_resource WHERE application_id = app.id AND code = 'authorization');

INSERT INTO iam_resource (id, application_id, code, name, scope_capabilities, field_capabilities, enabled)
SELECT COALESCE((SELECT 110007 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_resource taken WHERE taken.id = 110007)), (SELECT COALESCE(MAX(taken.id), 110000) + 1 FROM iam_resource taken WHERE taken.id >= 110000 AND taken.id < 120000)), app.id, 'delegation', '授权委派', CAST('["ALL", "OBJECT_SET"]' AS JSON), CAST('[]' AS JSON), TRUE FROM iam_application app
WHERE app.code = 'iam-platform' AND NOT EXISTS (SELECT 1 FROM iam_resource WHERE application_id = app.id AND code = 'delegation');

INSERT INTO iam_resource (id, application_id, code, name, scope_capabilities, field_capabilities, enabled)
SELECT COALESCE((SELECT 110008 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_resource taken WHERE taken.id = 110008)), (SELECT COALESCE(MAX(taken.id), 110000) + 1 FROM iam_resource taken WHERE taken.id >= 110000 AND taken.id < 120000)), app.id, 'dictionary', '字典', CAST('["ALL"]' AS JSON), CAST('[]' AS JSON), TRUE FROM iam_application app
WHERE app.code = 'iam-platform' AND NOT EXISTS (SELECT 1 FROM iam_resource WHERE application_id = app.id AND code = 'dictionary');

INSERT INTO iam_resource (id, application_id, code, name, scope_capabilities, field_capabilities, enabled)
SELECT COALESCE((SELECT 110009 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_resource taken WHERE taken.id = 110009)), (SELECT COALESCE(MAX(taken.id), 110000) + 1 FROM iam_resource taken WHERE taken.id >= 110000 AND taken.id < 120000)), app.id, 'entitlement', '应用开通', CAST('["ALL", "OBJECT_SET"]' AS JSON), CAST('[]' AS JSON), TRUE FROM iam_application app
WHERE app.code = 'iam-platform' AND NOT EXISTS (SELECT 1 FROM iam_resource WHERE application_id = app.id AND code = 'entitlement');

INSERT INTO iam_resource (id, application_id, code, name, scope_capabilities, field_capabilities, enabled)
SELECT COALESCE((SELECT 110010 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_resource taken WHERE taken.id = 110010)), (SELECT COALESCE(MAX(taken.id), 110000) + 1 FROM iam_resource taken WHERE taken.id >= 110000 AND taken.id < 120000)), app.id, 'group', '用户组', CAST('["ALL", "OBJECT_SET"]' AS JSON), CAST('[]' AS JSON), TRUE FROM iam_application app
WHERE app.code = 'iam-platform' AND NOT EXISTS (SELECT 1 FROM iam_resource WHERE application_id = app.id AND code = 'group');

INSERT INTO iam_resource (id, application_id, code, name, scope_capabilities, field_capabilities, enabled)
SELECT COALESCE((SELECT 110011 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_resource taken WHERE taken.id = 110011)), (SELECT COALESCE(MAX(taken.id), 110000) + 1 FROM iam_resource taken WHERE taken.id >= 110000 AND taken.id < 120000)), app.id, 'member', '成员', CAST('["ALL", "SELF", "OBJECT_SET"]' AS JSON), CAST('[{"key": "displayName", "label": "显示名", "visibilities": ["HIDDEN", "MASKED", "FULL"], "editable": true, "filterable": true, "sortable": true}, {"key": "avatar", "label": "头像", "visibilities": ["HIDDEN", "FULL"], "editable": true, "filterable": false, "sortable": false}, {"key": "phone", "label": "手机号", "visibilities": ["HIDDEN", "MASKED", "FULL"], "editable": true, "filterable": true, "sortable": false}, {"key": "email", "label": "邮箱", "visibilities": ["HIDDEN", "MASKED", "FULL"], "editable": true, "filterable": true, "sortable": false}]' AS JSON), TRUE FROM iam_application app
WHERE app.code = 'iam-platform' AND NOT EXISTS (SELECT 1 FROM iam_resource WHERE application_id = app.id AND code = 'member');

INSERT INTO iam_resource (id, application_id, code, name, scope_capabilities, field_capabilities, enabled)
SELECT COALESCE((SELECT 110012 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_resource taken WHERE taken.id = 110012)), (SELECT COALESCE(MAX(taken.id), 110000) + 1 FROM iam_resource taken WHERE taken.id >= 110000 AND taken.id < 120000)), app.id, 'menu', '菜单', CAST('["ALL", "OBJECT_SET"]' AS JSON), CAST('[]' AS JSON), TRUE FROM iam_application app
WHERE app.code = 'iam-platform' AND NOT EXISTS (SELECT 1 FROM iam_resource WHERE application_id = app.id AND code = 'menu');

INSERT INTO iam_resource (id, application_id, code, name, scope_capabilities, field_capabilities, enabled)
SELECT COALESCE((SELECT 110013 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_resource taken WHERE taken.id = 110013)), (SELECT COALESCE(MAX(taken.id), 110000) + 1 FROM iam_resource taken WHERE taken.id >= 110000 AND taken.id < 120000)), app.id, 'plan', '套餐', CAST('["ALL", "OBJECT_SET"]' AS JSON), CAST('[]' AS JSON), TRUE FROM iam_application app
WHERE app.code = 'iam-platform' AND NOT EXISTS (SELECT 1 FROM iam_resource WHERE application_id = app.id AND code = 'plan');

INSERT INTO iam_resource (id, application_id, code, name, scope_capabilities, field_capabilities, enabled)
SELECT COALESCE((SELECT 110014 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_resource taken WHERE taken.id = 110014)), (SELECT COALESCE(MAX(taken.id), 110000) + 1 FROM iam_resource taken WHERE taken.id >= 110000 AND taken.id < 120000)), app.id, 'resource', '资源', CAST('["ALL", "OBJECT_SET"]' AS JSON), CAST('[]' AS JSON), TRUE FROM iam_application app
WHERE app.code = 'iam-platform' AND NOT EXISTS (SELECT 1 FROM iam_resource WHERE application_id = app.id AND code = 'resource');

INSERT INTO iam_resource (id, application_id, code, name, scope_capabilities, field_capabilities, enabled)
SELECT COALESCE((SELECT 110015 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_resource taken WHERE taken.id = 110015)), (SELECT COALESCE(MAX(taken.id), 110000) + 1 FROM iam_resource taken WHERE taken.id >= 110000 AND taken.id < 120000)), app.id, 'role', '角色', CAST('["ALL", "OBJECT_SET"]' AS JSON), CAST('[]' AS JSON), TRUE FROM iam_application app
WHERE app.code = 'iam-platform' AND NOT EXISTS (SELECT 1 FROM iam_resource WHERE application_id = app.id AND code = 'role');

INSERT INTO iam_resource (id, application_id, code, name, scope_capabilities, field_capabilities, enabled)
SELECT COALESCE((SELECT 110016 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_resource taken WHERE taken.id = 110016)), (SELECT COALESCE(MAX(taken.id), 110000) + 1 FROM iam_resource taken WHERE taken.id >= 110000 AND taken.id < 120000)), app.id, 'shared-role', '共享角色', CAST('["ALL", "OBJECT_SET"]' AS JSON), CAST('[]' AS JSON), TRUE FROM iam_application app
WHERE app.code = 'iam-platform' AND NOT EXISTS (SELECT 1 FROM iam_resource WHERE application_id = app.id AND code = 'shared-role');

INSERT INTO iam_resource (id, application_id, code, name, scope_capabilities, field_capabilities, enabled)
SELECT COALESCE((SELECT 110017 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_resource taken WHERE taken.id = 110017)), (SELECT COALESCE(MAX(taken.id), 110000) + 1 FROM iam_resource taken WHERE taken.id >= 110000 AND taken.id < 120000)), app.id, 'tenant', '组织', CAST('["ALL", "OBJECT_SET"]' AS JSON), CAST('[]' AS JSON), TRUE FROM iam_application app
WHERE app.code = 'iam-platform' AND NOT EXISTS (SELECT 1 FROM iam_resource WHERE application_id = app.id AND code = 'tenant');

INSERT INTO iam_resource (id, application_id, code, name, scope_capabilities, field_capabilities, enabled)
SELECT COALESCE((SELECT 110018 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_resource taken WHERE taken.id = 110018)), (SELECT COALESCE(MAX(taken.id), 110000) + 1 FROM iam_resource taken WHERE taken.id >= 110000 AND taken.id < 120000)), app.id, 'application', '应用', CAST('["ALL", "OBJECT_SET"]' AS JSON), CAST('[]' AS JSON), TRUE FROM iam_application app
WHERE app.code = 'iam-tenant' AND NOT EXISTS (SELECT 1 FROM iam_resource WHERE application_id = app.id AND code = 'application');

INSERT INTO iam_resource (id, application_id, code, name, scope_capabilities, field_capabilities, enabled)
SELECT COALESCE((SELECT 110019 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_resource taken WHERE taken.id = 110019)), (SELECT COALESCE(MAX(taken.id), 110000) + 1 FROM iam_resource taken WHERE taken.id >= 110000 AND taken.id < 120000)), app.id, 'assignment', '角色授权', CAST('["ALL", "OBJECT_SET"]' AS JSON), CAST('[]' AS JSON), TRUE FROM iam_application app
WHERE app.code = 'iam-tenant' AND NOT EXISTS (SELECT 1 FROM iam_resource WHERE application_id = app.id AND code = 'assignment');

INSERT INTO iam_resource (id, application_id, code, name, scope_capabilities, field_capabilities, enabled)
SELECT COALESCE((SELECT 110020 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_resource taken WHERE taken.id = 110020)), (SELECT COALESCE(MAX(taken.id), 110000) + 1 FROM iam_resource taken WHERE taken.id >= 110000 AND taken.id < 120000)), app.id, 'audience', '应用可用人群', CAST('["ALL"]' AS JSON), CAST('[]' AS JSON), TRUE FROM iam_application app
WHERE app.code = 'iam-tenant' AND NOT EXISTS (SELECT 1 FROM iam_resource WHERE application_id = app.id AND code = 'audience');

INSERT INTO iam_resource (id, application_id, code, name, scope_capabilities, field_capabilities, enabled)
SELECT COALESCE((SELECT 110021 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_resource taken WHERE taken.id = 110021)), (SELECT COALESCE(MAX(taken.id), 110000) + 1 FROM iam_resource taken WHERE taken.id >= 110000 AND taken.id < 120000)), app.id, 'audit', '审计记录', CAST('["ALL"]' AS JSON), CAST('[]' AS JSON), TRUE FROM iam_application app
WHERE app.code = 'iam-tenant' AND NOT EXISTS (SELECT 1 FROM iam_resource WHERE application_id = app.id AND code = 'audit');

INSERT INTO iam_resource (id, application_id, code, name, scope_capabilities, field_capabilities, enabled)
SELECT COALESCE((SELECT 110022 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_resource taken WHERE taken.id = 110022)), (SELECT COALESCE(MAX(taken.id), 110000) + 1 FROM iam_resource taken WHERE taken.id >= 110000 AND taken.id < 120000)), app.id, 'authorization', '授权诊断', CAST('["ALL"]' AS JSON), CAST('[]' AS JSON), TRUE FROM iam_application app
WHERE app.code = 'iam-tenant' AND NOT EXISTS (SELECT 1 FROM iam_resource WHERE application_id = app.id AND code = 'authorization');

INSERT INTO iam_resource (id, application_id, code, name, scope_capabilities, field_capabilities, enabled)
SELECT COALESCE((SELECT 110023 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_resource taken WHERE taken.id = 110023)), (SELECT COALESCE(MAX(taken.id), 110000) + 1 FROM iam_resource taken WHERE taken.id >= 110000 AND taken.id < 120000)), app.id, 'delegation', '授权委派', CAST('["ALL", "OBJECT_SET"]' AS JSON), CAST('[]' AS JSON), TRUE FROM iam_application app
WHERE app.code = 'iam-tenant' AND NOT EXISTS (SELECT 1 FROM iam_resource WHERE application_id = app.id AND code = 'delegation');

INSERT INTO iam_resource (id, application_id, code, name, scope_capabilities, field_capabilities, enabled)
SELECT COALESCE((SELECT 110024 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_resource taken WHERE taken.id = 110024)), (SELECT COALESCE(MAX(taken.id), 110000) + 1 FROM iam_resource taken WHERE taken.id >= 110000 AND taken.id < 120000)), app.id, 'department', '部门', CAST('["ALL", "MANAGED_DEPARTMENTS", "OBJECT_SET"]' AS JSON), CAST('[]' AS JSON), TRUE FROM iam_application app
WHERE app.code = 'iam-tenant' AND NOT EXISTS (SELECT 1 FROM iam_resource WHERE application_id = app.id AND code = 'department');

INSERT INTO iam_resource (id, application_id, code, name, scope_capabilities, field_capabilities, enabled)
SELECT COALESCE((SELECT 110025 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_resource taken WHERE taken.id = 110025)), (SELECT COALESCE(MAX(taken.id), 110000) + 1 FROM iam_resource taken WHERE taken.id >= 110000 AND taken.id < 120000)), app.id, 'directory', '通讯录', CAST('["ALL", "SELF", "MEMBER_DEPARTMENTS", "MANAGED_DEPARTMENTS", "OBJECT_SET"]' AS JSON), CAST('[{"key": "displayName", "label": "显示名", "visibilities": ["HIDDEN", "MASKED", "FULL"], "editable": true, "filterable": true, "sortable": true}, {"key": "avatar", "label": "头像", "visibilities": ["HIDDEN", "FULL"], "editable": true, "filterable": false, "sortable": false}, {"key": "phone", "label": "手机号", "visibilities": ["HIDDEN", "MASKED", "FULL"], "editable": true, "filterable": true, "sortable": false}, {"key": "email", "label": "邮箱", "visibilities": ["HIDDEN", "MASKED", "FULL"], "editable": true, "filterable": true, "sortable": false}]' AS JSON), TRUE FROM iam_application app
WHERE app.code = 'iam-tenant' AND NOT EXISTS (SELECT 1 FROM iam_resource WHERE application_id = app.id AND code = 'directory');

INSERT INTO iam_resource (id, application_id, code, name, scope_capabilities, field_capabilities, enabled)
SELECT COALESCE((SELECT 110026 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_resource taken WHERE taken.id = 110026)), (SELECT COALESCE(MAX(taken.id), 110000) + 1 FROM iam_resource taken WHERE taken.id >= 110000 AND taken.id < 120000)), app.id, 'directory-policy', '通讯录可见范围策略', CAST('["ALL"]' AS JSON), CAST('[]' AS JSON), TRUE FROM iam_application app
WHERE app.code = 'iam-tenant' AND NOT EXISTS (SELECT 1 FROM iam_resource WHERE application_id = app.id AND code = 'directory-policy');

INSERT INTO iam_resource (id, application_id, code, name, scope_capabilities, field_capabilities, enabled)
SELECT COALESCE((SELECT 110027 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_resource taken WHERE taken.id = 110027)), (SELECT COALESCE(MAX(taken.id), 110000) + 1 FROM iam_resource taken WHERE taken.id >= 110000 AND taken.id < 120000)), app.id, 'field-policy', '成员字段策略', CAST('["ALL"]' AS JSON), CAST('[]' AS JSON), TRUE FROM iam_application app
WHERE app.code = 'iam-tenant' AND NOT EXISTS (SELECT 1 FROM iam_resource WHERE application_id = app.id AND code = 'field-policy');

INSERT INTO iam_resource (id, application_id, code, name, scope_capabilities, field_capabilities, enabled)
SELECT COALESCE((SELECT 110028 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_resource taken WHERE taken.id = 110028)), (SELECT COALESCE(MAX(taken.id), 110000) + 1 FROM iam_resource taken WHERE taken.id >= 110000 AND taken.id < 120000)), app.id, 'group', '用户组', CAST('["ALL", "OBJECT_SET"]' AS JSON), CAST('[]' AS JSON), TRUE FROM iam_application app
WHERE app.code = 'iam-tenant' AND NOT EXISTS (SELECT 1 FROM iam_resource WHERE application_id = app.id AND code = 'group');

INSERT INTO iam_resource (id, application_id, code, name, scope_capabilities, field_capabilities, enabled)
SELECT COALESCE((SELECT 110029 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_resource taken WHERE taken.id = 110029)), (SELECT COALESCE(MAX(taken.id), 110000) + 1 FROM iam_resource taken WHERE taken.id >= 110000 AND taken.id < 120000)), app.id, 'member', '成员', CAST('["ALL", "SELF", "MEMBER_DEPARTMENTS", "MANAGED_DEPARTMENTS", "OBJECT_SET"]' AS JSON), CAST('[{"key": "displayName", "label": "显示名", "visibilities": ["HIDDEN", "MASKED", "FULL"], "editable": true, "filterable": true, "sortable": true}, {"key": "avatar", "label": "头像", "visibilities": ["HIDDEN", "FULL"], "editable": true, "filterable": false, "sortable": false}, {"key": "phone", "label": "手机号", "visibilities": ["HIDDEN", "MASKED", "FULL"], "editable": true, "filterable": true, "sortable": false}, {"key": "email", "label": "邮箱", "visibilities": ["HIDDEN", "MASKED", "FULL"], "editable": true, "filterable": true, "sortable": false}]' AS JSON), TRUE FROM iam_application app
WHERE app.code = 'iam-tenant' AND NOT EXISTS (SELECT 1 FROM iam_resource WHERE application_id = app.id AND code = 'member');

INSERT INTO iam_resource (id, application_id, code, name, scope_capabilities, field_capabilities, enabled)
SELECT COALESCE((SELECT 110030 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_resource taken WHERE taken.id = 110030)), (SELECT COALESCE(MAX(taken.id), 110000) + 1 FROM iam_resource taken WHERE taken.id >= 110000 AND taken.id < 120000)), app.id, 'policy', '策略预览', CAST('["ALL"]' AS JSON), CAST('[]' AS JSON), TRUE FROM iam_application app
WHERE app.code = 'iam-tenant' AND NOT EXISTS (SELECT 1 FROM iam_resource WHERE application_id = app.id AND code = 'policy');

INSERT INTO iam_resource (id, application_id, code, name, scope_capabilities, field_capabilities, enabled)
SELECT COALESCE((SELECT 110031 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_resource taken WHERE taken.id = 110031)), (SELECT COALESCE(MAX(taken.id), 110000) + 1 FROM iam_resource taken WHERE taken.id >= 110000 AND taken.id < 120000)), app.id, 'role', '角色', CAST('["ALL", "OBJECT_SET"]' AS JSON), CAST('[]' AS JSON), TRUE FROM iam_application app
WHERE app.code = 'iam-tenant' AND NOT EXISTS (SELECT 1 FROM iam_resource WHERE application_id = app.id AND code = 'role');

INSERT INTO iam_resource (id, application_id, code, name, scope_capabilities, field_capabilities, enabled)
SELECT COALESCE((SELECT 110032 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_resource taken WHERE taken.id = 110032)), (SELECT COALESCE(MAX(taken.id), 110000) + 1 FROM iam_resource taken WHERE taken.id >= 110000 AND taken.id < 120000)), app.id, 'settings', '组织设置', CAST('["ALL"]' AS JSON), CAST('[]' AS JSON), TRUE FROM iam_application app
WHERE app.code = 'iam-tenant' AND NOT EXISTS (SELECT 1 FROM iam_resource WHERE application_id = app.id AND code = 'settings');

INSERT INTO iam_resource (id, application_id, code, name, scope_capabilities, field_capabilities, enabled)
SELECT COALESCE((SELECT 110033 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_resource taken WHERE taken.id = 110033)), (SELECT COALESCE(MAX(taken.id), 110000) + 1 FROM iam_resource taken WHERE taken.id >= 110000 AND taken.id < 120000)), app.id, 'client', '客户端', CAST('["ALL"]' AS JSON), CAST('[]' AS JSON), TRUE FROM iam_application app
WHERE app.code = 'platform:develop' AND NOT EXISTS (SELECT 1 FROM iam_resource WHERE application_id = app.id AND code = 'client');

INSERT INTO iam_resource (id, application_id, code, name, scope_capabilities, field_capabilities, enabled)
SELECT COALESCE((SELECT 110034 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_resource taken WHERE taken.id = 110034)), (SELECT COALESCE(MAX(taken.id), 110000) + 1 FROM iam_resource taken WHERE taken.id >= 110000 AND taken.id < 120000)), app.id, 'id-allocation', '发号', CAST('["ALL"]' AS JSON), CAST('[]' AS JSON), TRUE FROM iam_application app
WHERE app.code = 'platform:develop' AND NOT EXISTS (SELECT 1 FROM iam_resource WHERE application_id = app.id AND code = 'id-allocation');

INSERT INTO iam_resource (id, application_id, code, name, scope_capabilities, field_capabilities, enabled)
SELECT COALESCE((SELECT 110035 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_resource taken WHERE taken.id = 110035)), (SELECT COALESCE(MAX(taken.id), 110000) + 1 FROM iam_resource taken WHERE taken.id >= 110000 AND taken.id < 120000)), app.id, 'qrcode', '二维码', CAST('["ALL"]' AS JSON), CAST('[]' AS JSON), TRUE FROM iam_application app
WHERE app.code = 'platform:develop' AND NOT EXISTS (SELECT 1 FROM iam_resource WHERE application_id = app.id AND code = 'qrcode');

INSERT INTO iam_resource (id, application_id, code, name, scope_capabilities, field_capabilities, enabled)
SELECT COALESCE((SELECT 110036 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_resource taken WHERE taken.id = 110036)), (SELECT COALESCE(MAX(taken.id), 110000) + 1 FROM iam_resource taken WHERE taken.id >= 110000 AND taken.id < 120000)), app.id, 'social-config', '社会化登录配置', CAST('["ALL"]' AS JSON), CAST('[]' AS JSON), TRUE FROM iam_application app
WHERE app.code = 'platform:develop' AND NOT EXISTS (SELECT 1 FROM iam_resource WHERE application_id = app.id AND code = 'social-config');

-- 操作：精确操作码全局唯一，禁止通配。
INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120001 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120001)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-platform:account:create', '创建全局账号', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'account' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:account:create');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120002 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120002)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-platform:account:delete', '删除全局账号', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'account' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:account:delete');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120003 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120003)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-platform:account:disable', '停用全局账号', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'account' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:account:disable');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120004 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120004)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-platform:account:enable', '启用全局账号', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'account' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:account:enable');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120005 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120005)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-platform:account:lock', '锁定全局账号', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'account' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:account:lock');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120006 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120006)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-platform:account:lookup', '查找全局账号', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'account' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:account:lookup');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120007 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120007)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-platform:account:read', '查看全局账号', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'account' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:account:read');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120008 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120008)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-platform:account:reset-password', '重置密码', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'account' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:account:reset-password');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120009 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120009)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-platform:account:unlock', '解锁全局账号', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'account' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:account:unlock');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120010 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120010)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-platform:account:update', '编辑全局账号', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'account' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:account:update');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120011 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120011)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-platform:action:create', '创建操作', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'action' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:action:create');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120012 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120012)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-platform:action:delete', '删除操作', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'action' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:action:delete');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120013 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120013)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-platform:action:read', '查看操作', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'action' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:action:read');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120014 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120014)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-platform:action:status', '启停操作', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'action' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:action:status');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120015 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120015)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-platform:action:update', '编辑操作', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'action' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:action:update');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120016 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120016)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-platform:application:create', '创建应用', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'application' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:application:create');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120017 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120017)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-platform:application:delete', '删除应用', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'application' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:application:delete');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120018 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120018)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-platform:application:purge', '强制清除应用', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'application' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:application:purge');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120019 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120019)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-platform:application:read', '查看应用', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'application' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:application:read');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120020 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120020)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-platform:application:status', '启停应用', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'application' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:application:status');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120021 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120021)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-platform:application:update', '编辑应用', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'application' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:application:update');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120022 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120022)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-platform:assignment:create', '创建角色授权', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'assignment' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:assignment:create');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120023 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120023)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-platform:assignment:delete', '删除角色授权', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'assignment' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:assignment:delete');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120024 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120024)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-platform:assignment:read', '查看角色授权', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'assignment' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:assignment:read');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120025 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120025)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-platform:assignment:update', '编辑角色授权', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'assignment' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:assignment:update');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120026 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120026)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-platform:assignment:upgrade', '升级角色授权', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'assignment' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:assignment:upgrade');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120027 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120027)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-platform:audit:read', '查看审计记录', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'audit' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:audit:read');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120028 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120028)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-platform:authorization:diagnose', '诊断', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'authorization' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:authorization:diagnose');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120029 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120029)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-platform:delegation:create', '创建授权委派', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'delegation' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:delegation:create');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120030 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120030)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-platform:delegation:delete', '删除授权委派', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'delegation' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:delegation:delete');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120031 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120031)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-platform:delegation:preview', '预览授权委派', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'delegation' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:delegation:preview');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120032 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120032)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-platform:delegation:read', '查看授权委派', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'delegation' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:delegation:read');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120033 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120033)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-platform:delegation:update', '编辑授权委派', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'delegation' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:delegation:update');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120034 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120034)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-platform:dictionary:create', '创建字典', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'dictionary' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:dictionary:create');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120035 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120035)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-platform:dictionary:delete', '删除字典', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'dictionary' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:dictionary:delete');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120036 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120036)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-platform:dictionary:read', '查看字典', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'dictionary' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:dictionary:read');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120037 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120037)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-platform:dictionary:update', '编辑字典', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'dictionary' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:dictionary:update');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120038 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120038)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-platform:entitlement:preview', '预览应用开通', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'entitlement' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:entitlement:preview');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120039 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120039)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-platform:entitlement:read', '查看应用开通', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'entitlement' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:entitlement:read');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120040 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120040)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-platform:entitlement:update', '编辑应用开通', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'entitlement' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:entitlement:update');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120041 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120041)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-platform:group:create', '创建用户组', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'group' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:group:create');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120042 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120042)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-platform:group:delete', '删除用户组', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'group' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:group:delete');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120043 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120043)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-platform:group:preview', '预览用户组', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'group' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:group:preview');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120044 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120044)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-platform:group:read', '查看用户组', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'group' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:group:read');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120045 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120045)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-platform:group:update', '编辑用户组', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'group' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:group:update');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120046 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120046)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-platform:member:create', '创建成员', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'member' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:member:create');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120047 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120047)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-platform:member:read', '查看成员', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'member' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:member:read');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120048 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120048)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-platform:member:remove', '移出成员', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'member' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:member:remove');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120049 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120049)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-platform:member:status', '启停成员', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'member' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:member:status');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120050 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120050)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-platform:member:update', '编辑成员', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'member' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:member:update');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120051 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120051)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-platform:menu:create', '创建菜单', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'menu' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:menu:create');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120052 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120052)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-platform:menu:delete', '删除菜单', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'menu' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:menu:delete');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120053 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120053)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-platform:menu:read', '查看菜单', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'menu' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:menu:read');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120054 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120054)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-platform:menu:update', '编辑菜单', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'menu' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:menu:update');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120055 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120055)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-platform:plan:create', '创建套餐', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'plan' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:plan:create');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120056 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120056)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-platform:plan:read', '查看套餐', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'plan' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:plan:read');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120057 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120057)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-platform:plan:update', '编辑套餐', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'plan' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:plan:update');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120058 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120058)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-platform:resource:create', '创建资源', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'resource' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:resource:create');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120059 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120059)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-platform:resource:delete', '删除资源', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'resource' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:resource:delete');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120060 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120060)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-platform:resource:read', '查看资源', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'resource' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:resource:read');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120061 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120061)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-platform:resource:update', '编辑资源', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'resource' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:resource:update');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120062 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120062)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-platform:role:create', '创建角色', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'role' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:role:create');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120063 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120063)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-platform:role:delete', '删除角色', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'role' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:role:delete');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120064 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120064)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-platform:role:preview', '预览角色', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'role' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:role:preview');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120065 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120065)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-platform:role:publish', '发布版本角色', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'role' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:role:publish');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120066 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120066)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-platform:role:read', '查看角色', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'role' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:role:read');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120067 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120067)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-platform:role:status', '启停角色', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'role' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:role:status');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120068 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120068)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-platform:shared-role:create', '创建共享角色', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'shared-role' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:shared-role:create');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120069 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120069)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-platform:shared-role:delete', '删除共享角色', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'shared-role' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:shared-role:delete');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120070 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120070)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-platform:shared-role:preview', '预览共享角色', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'shared-role' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:shared-role:preview');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120071 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120071)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-platform:shared-role:publish', '发布版本共享角色', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'shared-role' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:shared-role:publish');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120072 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120072)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-platform:shared-role:read', '查看共享角色', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'shared-role' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:shared-role:read');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120073 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120073)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-platform:shared-role:status', '启停共享角色', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'shared-role' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:shared-role:status');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120074 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120074)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-platform:tenant:create', '创建组织', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'tenant' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:tenant:create');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120075 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120075)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-platform:tenant:preview', '预览组织', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'tenant' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:tenant:preview');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120076 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120076)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-platform:tenant:read', '查看组织', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'tenant' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:tenant:read');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120077 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120077)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-platform:tenant:update', '编辑组织', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'tenant' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:tenant:update');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120078 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120078)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-tenant:application:read', '查看应用', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-tenant' AND res.code = 'application' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-tenant:application:read');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120079 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120079)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-tenant:assignment:create', '创建角色授权', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-tenant' AND res.code = 'assignment' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-tenant:assignment:create');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120080 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120080)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-tenant:assignment:delete', '删除角色授权', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-tenant' AND res.code = 'assignment' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-tenant:assignment:delete');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120081 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120081)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-tenant:assignment:read', '查看角色授权', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-tenant' AND res.code = 'assignment' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-tenant:assignment:read');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120082 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120082)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-tenant:assignment:update', '编辑角色授权', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-tenant' AND res.code = 'assignment' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-tenant:assignment:update');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120083 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120083)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-tenant:audience:read', '查看应用可用人群', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-tenant' AND res.code = 'audience' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-tenant:audience:read');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120084 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120084)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-tenant:audience:update', '编辑应用可用人群', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-tenant' AND res.code = 'audience' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-tenant:audience:update');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120085 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120085)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-tenant:audit:read', '查看审计记录', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-tenant' AND res.code = 'audit' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-tenant:audit:read');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120086 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120086)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-tenant:authorization:diagnose', '诊断', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-tenant' AND res.code = 'authorization' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-tenant:authorization:diagnose');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120087 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120087)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-tenant:delegation:create', '创建授权委派', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-tenant' AND res.code = 'delegation' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-tenant:delegation:create');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120088 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120088)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-tenant:delegation:delete', '删除授权委派', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-tenant' AND res.code = 'delegation' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-tenant:delegation:delete');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120089 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120089)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-tenant:delegation:preview', '预览授权委派', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-tenant' AND res.code = 'delegation' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-tenant:delegation:preview');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120090 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120090)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-tenant:delegation:read', '查看授权委派', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-tenant' AND res.code = 'delegation' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-tenant:delegation:read');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120091 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120091)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-tenant:delegation:update', '编辑授权委派', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-tenant' AND res.code = 'delegation' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-tenant:delegation:update');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120092 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120092)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-tenant:department:create', '创建部门', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-tenant' AND res.code = 'department' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-tenant:department:create');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120093 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120093)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-tenant:department:delete', '删除部门', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-tenant' AND res.code = 'department' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-tenant:department:delete');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120094 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120094)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-tenant:department:read', '查看部门', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-tenant' AND res.code = 'department' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-tenant:department:read');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120095 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120095)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-tenant:department:update', '编辑部门', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-tenant' AND res.code = 'department' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-tenant:department:update');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120096 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120096)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-tenant:directory:read', '查看通讯录', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-tenant' AND res.code = 'directory' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-tenant:directory:read');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120097 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120097)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-tenant:directory-policy:read', '查看通讯录可见范围策略', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-tenant' AND res.code = 'directory-policy' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-tenant:directory-policy:read');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120098 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120098)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-tenant:directory-policy:update', '编辑通讯录可见范围策略', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-tenant' AND res.code = 'directory-policy' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-tenant:directory-policy:update');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120099 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120099)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-tenant:field-policy:read', '查看成员字段策略', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-tenant' AND res.code = 'field-policy' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-tenant:field-policy:read');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120100 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120100)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-tenant:field-policy:update', '编辑成员字段策略', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-tenant' AND res.code = 'field-policy' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-tenant:field-policy:update');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120101 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120101)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-tenant:group:create', '创建用户组', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-tenant' AND res.code = 'group' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-tenant:group:create');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120102 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120102)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-tenant:group:delete', '删除用户组', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-tenant' AND res.code = 'group' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-tenant:group:delete');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120103 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120103)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-tenant:group:preview', '预览用户组', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-tenant' AND res.code = 'group' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-tenant:group:preview');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120104 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120104)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-tenant:group:read', '查看用户组', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-tenant' AND res.code = 'group' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-tenant:group:read');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120105 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120105)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-tenant:group:update', '编辑用户组', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-tenant' AND res.code = 'group' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-tenant:group:update');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120106 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120106)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-tenant:member:create', '创建成员', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-tenant' AND res.code = 'member' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-tenant:member:create');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120107 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120107)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-tenant:member:departments', '调整任职', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-tenant' AND res.code = 'member' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-tenant:member:departments');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120108 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120108)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-tenant:member:export', '导出成员', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-tenant' AND res.code = 'member' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-tenant:member:export');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120109 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120109)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-tenant:member:read', '查看成员', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-tenant' AND res.code = 'member' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-tenant:member:read');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120110 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120110)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-tenant:member:remove', '移出成员', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-tenant' AND res.code = 'member' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-tenant:member:remove');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120111 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120111)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-tenant:member:status', '启停成员', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-tenant' AND res.code = 'member' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-tenant:member:status');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120112 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120112)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-tenant:member:update', '编辑成员', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-tenant' AND res.code = 'member' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-tenant:member:update');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120113 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120113)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-tenant:policy:preview', '预览策略预览', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-tenant' AND res.code = 'policy' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-tenant:policy:preview');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120114 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120114)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-tenant:role:create', '创建角色', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-tenant' AND res.code = 'role' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-tenant:role:create');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120115 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120115)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-tenant:role:delete', '删除角色', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-tenant' AND res.code = 'role' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-tenant:role:delete');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120116 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120116)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-tenant:role:preview', '预览角色', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-tenant' AND res.code = 'role' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-tenant:role:preview');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120117 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120117)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-tenant:role:publish', '发布版本角色', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-tenant' AND res.code = 'role' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-tenant:role:publish');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120118 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120118)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-tenant:role:read', '查看角色', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-tenant' AND res.code = 'role' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-tenant:role:read');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120119 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120119)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-tenant:role:status', '启停角色', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-tenant' AND res.code = 'role' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-tenant:role:status');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120120 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120120)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-tenant:role:upgrade', '升级角色', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-tenant' AND res.code = 'role' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-tenant:role:upgrade');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120121 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120121)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-tenant:settings:owner-transfer', '转交所有者', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-tenant' AND res.code = 'settings' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-tenant:settings:owner-transfer');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120122 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120122)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-tenant:settings:read', '查看组织设置', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-tenant' AND res.code = 'settings' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-tenant:settings:read');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120123 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120123)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-tenant:settings:update', '编辑组织设置', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-tenant' AND res.code = 'settings' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-tenant:settings:update');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120124 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120124)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'platform:develop:client:create', '创建客户端', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'platform:develop' AND res.code = 'client' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'platform:develop:client:create');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120125 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120125)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'platform:develop:client:delete', '删除客户端', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'platform:develop' AND res.code = 'client' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'platform:develop:client:delete');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120126 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120126)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'platform:develop:client:detail', '查看客户端详情', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'platform:develop' AND res.code = 'client' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'platform:develop:client:detail');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120127 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120127)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'platform:develop:client:query', '查看客户端', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'platform:develop' AND res.code = 'client' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'platform:develop:client:query');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120128 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120128)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'platform:develop:client:reset', '重置客户端密钥', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'platform:develop' AND res.code = 'client' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'platform:develop:client:reset');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120129 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120129)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'platform:develop:client:update', '编辑客户端', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'platform:develop' AND res.code = 'client' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'platform:develop:client:update');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120130 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120130)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-platform:id-allocation:create', '创建发号', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'platform:develop' AND res.code = 'id-allocation' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:id-allocation:create');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120131 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120131)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-platform:id-allocation:delete', '删除发号', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'platform:develop' AND res.code = 'id-allocation' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:id-allocation:delete');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120132 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120132)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-platform:id-allocation:read', '查看发号', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'platform:develop' AND res.code = 'id-allocation' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:id-allocation:read');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120133 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120133)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-platform:id-allocation:update', '编辑发号', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'platform:develop' AND res.code = 'id-allocation' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:id-allocation:update');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120134 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120134)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'platform:develop:qrcode', '生成二维码', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'platform:develop' AND res.code = 'qrcode' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'platform:develop:qrcode');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120135 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120135)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-platform:social-config:create', '创建社会化登录配置', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'platform:develop' AND res.code = 'social-config' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:social-config:create');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120136 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120136)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-platform:social-config:delete', '删除社会化登录配置', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'platform:develop' AND res.code = 'social-config' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:social-config:delete');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120137 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120137)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-platform:social-config:read', '查看社会化登录配置', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'platform:develop' AND res.code = 'social-config' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:social-config:read');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120138 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120138)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-platform:social-config:update', '编辑社会化登录配置', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'platform:develop' AND res.code = 'social-config' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:social-config:update');

-- 菜单：目录携带子页操作并集，子页全部不可见时目录随之隐藏。
INSERT INTO iam_menu (id, application_id, parent_id, name, path, view_path, route_name, kind, match_mode, access_mode, sort_order, enabled)
SELECT COALESCE((SELECT 130001 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_menu taken WHERE taken.id = 130001)), (SELECT COALESCE(MAX(taken.id), 130000) + 1 FROM iam_menu taken WHERE taken.id >= 130000 AND taken.id < 140000)), app.id, NULL, '组织与租户', '/platform/iam/tenant', 'layout.main', 'platform.iam.tenant', 'DIRECTORY', 'ANY', 'ACTION', 1, TRUE FROM iam_application app
WHERE app.code = 'iam-platform' AND NOT EXISTS (SELECT 1 FROM iam_menu WHERE application_id = app.id AND route_name = 'platform.iam.tenant');

INSERT INTO iam_menu (id, application_id, parent_id, name, path, view_path, route_name, kind, match_mode, access_mode, sort_order, enabled)
SELECT COALESCE((SELECT 130002 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_menu taken WHERE taken.id = 130002)), (SELECT COALESCE(MAX(taken.id), 130000) + 1 FROM iam_menu taken WHERE taken.id >= 130000 AND taken.id < 140000)), parent.application_id, parent.id, '租户管理', '/platform/iam/tenants', 'platform.iam.tenants', 'platform.iam.tenants', 'PAGE', 'ANY', 'ACTION', 1, TRUE FROM iam_menu parent JOIN iam_application app ON app.id = parent.application_id
WHERE app.code = 'iam-platform' AND parent.route_name = 'platform.iam.tenant' AND NOT EXISTS (SELECT 1 FROM iam_menu WHERE application_id = parent.application_id AND route_name = 'platform.iam.tenants');

INSERT INTO iam_menu (id, application_id, parent_id, name, path, view_path, route_name, kind, match_mode, access_mode, sort_order, enabled)
SELECT COALESCE((SELECT 130003 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_menu taken WHERE taken.id = 130003)), (SELECT COALESCE(MAX(taken.id), 130000) + 1 FROM iam_menu taken WHERE taken.id >= 130000 AND taken.id < 140000)), app.id, NULL, '应用与配置', '/platform/iam/config', 'layout.main', 'platform.iam.config', 'DIRECTORY', 'ANY', 'ACTION', 2, TRUE FROM iam_application app
WHERE app.code = 'iam-platform' AND NOT EXISTS (SELECT 1 FROM iam_menu WHERE application_id = app.id AND route_name = 'platform.iam.config');

INSERT INTO iam_menu (id, application_id, parent_id, name, path, view_path, route_name, kind, match_mode, access_mode, sort_order, enabled)
SELECT COALESCE((SELECT 130004 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_menu taken WHERE taken.id = 130004)), (SELECT COALESCE(MAX(taken.id), 130000) + 1 FROM iam_menu taken WHERE taken.id >= 130000 AND taken.id < 140000)), parent.application_id, parent.id, '应用目录', '/platform/iam/applications', 'platform.iam.applications', 'platform.iam.applications', 'PAGE', 'ANY', 'ACTION', 1, TRUE FROM iam_menu parent JOIN iam_application app ON app.id = parent.application_id
WHERE app.code = 'iam-platform' AND parent.route_name = 'platform.iam.config' AND NOT EXISTS (SELECT 1 FROM iam_menu WHERE application_id = parent.application_id AND route_name = 'platform.iam.applications');

INSERT INTO iam_menu (id, application_id, parent_id, name, path, view_path, route_name, kind, match_mode, access_mode, sort_order, enabled)
SELECT COALESCE((SELECT 130005 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_menu taken WHERE taken.id = 130005)), (SELECT COALESCE(MAX(taken.id), 130000) + 1 FROM iam_menu taken WHERE taken.id >= 130000 AND taken.id < 140000)), parent.application_id, parent.id, '套餐', '/platform/iam/plans', 'platform.iam.plans', 'platform.iam.plans', 'PAGE', 'ANY', 'ACTION', 2, TRUE FROM iam_menu parent JOIN iam_application app ON app.id = parent.application_id
WHERE app.code = 'iam-platform' AND parent.route_name = 'platform.iam.config' AND NOT EXISTS (SELECT 1 FROM iam_menu WHERE application_id = parent.application_id AND route_name = 'platform.iam.plans');

INSERT INTO iam_menu (id, application_id, parent_id, name, path, view_path, route_name, kind, match_mode, access_mode, sort_order, enabled)
SELECT COALESCE((SELECT 130006 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_menu taken WHERE taken.id = 130006)), (SELECT COALESCE(MAX(taken.id), 130000) + 1 FROM iam_menu taken WHERE taken.id >= 130000 AND taken.id < 140000)), parent.application_id, parent.id, '共享角色', '/platform/iam/shared/roles', 'platform.iam.shared.roles', 'platform.iam.shared.roles', 'PAGE', 'ANY', 'ACTION', 3, TRUE FROM iam_menu parent JOIN iam_application app ON app.id = parent.application_id
WHERE app.code = 'iam-platform' AND parent.route_name = 'platform.iam.config' AND NOT EXISTS (SELECT 1 FROM iam_menu WHERE application_id = parent.application_id AND route_name = 'platform.iam.shared.roles');

INSERT INTO iam_menu (id, application_id, parent_id, name, path, view_path, route_name, kind, match_mode, access_mode, sort_order, enabled)
SELECT COALESCE((SELECT 130007 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_menu taken WHERE taken.id = 130007)), (SELECT COALESCE(MAX(taken.id), 130000) + 1 FROM iam_menu taken WHERE taken.id >= 130000 AND taken.id < 140000)), app.id, NULL, '平台管理', '/platform/iam/manage', 'layout.main', 'platform.iam.manage', 'DIRECTORY', 'ANY', 'ACTION', 3, TRUE FROM iam_application app
WHERE app.code = 'iam-platform' AND NOT EXISTS (SELECT 1 FROM iam_menu WHERE application_id = app.id AND route_name = 'platform.iam.manage');

INSERT INTO iam_menu (id, application_id, parent_id, name, path, view_path, route_name, kind, match_mode, access_mode, sort_order, enabled)
SELECT COALESCE((SELECT 130008 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_menu taken WHERE taken.id = 130008)), (SELECT COALESCE(MAX(taken.id), 130000) + 1 FROM iam_menu taken WHERE taken.id >= 130000 AND taken.id < 140000)), parent.application_id, parent.id, '全局账号', '/platform/iam/accounts', 'platform.iam.accounts', 'platform.iam.accounts', 'PAGE', 'ANY', 'ACTION', 1, TRUE FROM iam_menu parent JOIN iam_application app ON app.id = parent.application_id
WHERE app.code = 'iam-platform' AND parent.route_name = 'platform.iam.manage' AND NOT EXISTS (SELECT 1 FROM iam_menu WHERE application_id = parent.application_id AND route_name = 'platform.iam.accounts');

INSERT INTO iam_menu (id, application_id, parent_id, name, path, view_path, route_name, kind, match_mode, access_mode, sort_order, enabled)
SELECT COALESCE((SELECT 130009 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_menu taken WHERE taken.id = 130009)), (SELECT COALESCE(MAX(taken.id), 130000) + 1 FROM iam_menu taken WHERE taken.id >= 130000 AND taken.id < 140000)), parent.application_id, parent.id, '平台人员', '/platform/iam/personnel', 'platform.iam.personnel', 'platform.iam.personnel', 'PAGE', 'ANY', 'ACTION', 2, TRUE FROM iam_menu parent JOIN iam_application app ON app.id = parent.application_id
WHERE app.code = 'iam-platform' AND parent.route_name = 'platform.iam.manage' AND NOT EXISTS (SELECT 1 FROM iam_menu WHERE application_id = parent.application_id AND route_name = 'platform.iam.personnel');

INSERT INTO iam_menu (id, application_id, parent_id, name, path, view_path, route_name, kind, match_mode, access_mode, sort_order, enabled)
SELECT COALESCE((SELECT 130010 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_menu taken WHERE taken.id = 130010)), (SELECT COALESCE(MAX(taken.id), 130000) + 1 FROM iam_menu taken WHERE taken.id >= 130000 AND taken.id < 140000)), parent.application_id, parent.id, '角色与授权', '/platform/iam/authorization', 'platform.iam.authorization', 'platform.iam.authorization', 'PAGE', 'ANY', 'ACTION', 3, TRUE FROM iam_menu parent JOIN iam_application app ON app.id = parent.application_id
WHERE app.code = 'iam-platform' AND parent.route_name = 'platform.iam.manage' AND NOT EXISTS (SELECT 1 FROM iam_menu WHERE application_id = parent.application_id AND route_name = 'platform.iam.authorization');

INSERT INTO iam_menu (id, application_id, parent_id, name, path, view_path, route_name, kind, match_mode, access_mode, sort_order, enabled)
SELECT COALESCE((SELECT 130011 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_menu taken WHERE taken.id = 130011)), (SELECT COALESCE(MAX(taken.id), 130000) + 1 FROM iam_menu taken WHERE taken.id >= 130000 AND taken.id < 140000)), app.id, NULL, '安全与运维', '/platform/iam/security', 'layout.main', 'platform.iam.security', 'DIRECTORY', 'ANY', 'ACTION', 4, TRUE FROM iam_application app
WHERE app.code = 'iam-platform' AND NOT EXISTS (SELECT 1 FROM iam_menu WHERE application_id = app.id AND route_name = 'platform.iam.security');

INSERT INTO iam_menu (id, application_id, parent_id, name, path, view_path, route_name, kind, match_mode, access_mode, sort_order, enabled)
SELECT COALESCE((SELECT 130012 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_menu taken WHERE taken.id = 130012)), (SELECT COALESCE(MAX(taken.id), 130000) + 1 FROM iam_menu taken WHERE taken.id >= 130000 AND taken.id < 140000)), parent.application_id, parent.id, '事件与审计', '/security/iam/authorization/audit', 'security.iam.authorization.audit', 'security.iam.authorization.audit', 'PAGE', 'ANY', 'ACTION', 1, TRUE FROM iam_menu parent JOIN iam_application app ON app.id = parent.application_id
WHERE app.code = 'iam-platform' AND parent.route_name = 'platform.iam.security' AND NOT EXISTS (SELECT 1 FROM iam_menu WHERE application_id = parent.application_id AND route_name = 'security.iam.authorization.audit');

INSERT INTO iam_menu (id, application_id, parent_id, name, path, view_path, route_name, kind, match_mode, access_mode, sort_order, enabled)
SELECT COALESCE((SELECT 130013 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_menu taken WHERE taken.id = 130013)), (SELECT COALESCE(MAX(taken.id), 130000) + 1 FROM iam_menu taken WHERE taken.id >= 130000 AND taken.id < 140000)), app.id, NULL, '组织管理', '/org/iam/organization', 'layout.main', 'org.iam.organization', 'DIRECTORY', 'ANY', 'ACTION', 1, TRUE FROM iam_application app
WHERE app.code = 'iam-tenant' AND NOT EXISTS (SELECT 1 FROM iam_menu WHERE application_id = app.id AND route_name = 'org.iam.organization');

INSERT INTO iam_menu (id, application_id, parent_id, name, path, view_path, route_name, kind, match_mode, access_mode, sort_order, enabled)
SELECT COALESCE((SELECT 130014 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_menu taken WHERE taken.id = 130014)), (SELECT COALESCE(MAX(taken.id), 130000) + 1 FROM iam_menu taken WHERE taken.id >= 130000 AND taken.id < 140000)), parent.application_id, parent.id, '成员与部门', '/org/iam/members', 'org.iam.members', 'org.iam.members', 'PAGE', 'ANY', 'ACTION', 1, TRUE FROM iam_menu parent JOIN iam_application app ON app.id = parent.application_id
WHERE app.code = 'iam-tenant' AND parent.route_name = 'org.iam.organization' AND NOT EXISTS (SELECT 1 FROM iam_menu WHERE application_id = parent.application_id AND route_name = 'org.iam.members');

INSERT INTO iam_menu (id, application_id, parent_id, name, path, view_path, route_name, kind, match_mode, access_mode, sort_order, enabled)
SELECT COALESCE((SELECT 130015 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_menu taken WHERE taken.id = 130015)), (SELECT COALESCE(MAX(taken.id), 130000) + 1 FROM iam_menu taken WHERE taken.id >= 130000 AND taken.id < 140000)), parent.application_id, parent.id, '用户组', '/org/iam/groups', 'org.iam.groups', 'org.iam.groups', 'PAGE', 'ANY', 'ACTION', 2, TRUE FROM iam_menu parent JOIN iam_application app ON app.id = parent.application_id
WHERE app.code = 'iam-tenant' AND parent.route_name = 'org.iam.organization' AND NOT EXISTS (SELECT 1 FROM iam_menu WHERE application_id = parent.application_id AND route_name = 'org.iam.groups');

INSERT INTO iam_menu (id, application_id, parent_id, name, path, view_path, route_name, kind, match_mode, access_mode, sort_order, enabled)
SELECT COALESCE((SELECT 130016 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_menu taken WHERE taken.id = 130016)), (SELECT COALESCE(MAX(taken.id), 130000) + 1 FROM iam_menu taken WHERE taken.id >= 130000 AND taken.id < 140000)), parent.application_id, parent.id, '组织设置', '/org/iam/settings', 'org.iam.settings', 'org.iam.settings', 'PAGE', 'ANY', 'ACTION', 3, TRUE FROM iam_menu parent JOIN iam_application app ON app.id = parent.application_id
WHERE app.code = 'iam-tenant' AND parent.route_name = 'org.iam.organization' AND NOT EXISTS (SELECT 1 FROM iam_menu WHERE application_id = parent.application_id AND route_name = 'org.iam.settings');

INSERT INTO iam_menu (id, application_id, parent_id, name, path, view_path, route_name, kind, match_mode, access_mode, sort_order, enabled)
SELECT COALESCE((SELECT 130017 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_menu taken WHERE taken.id = 130017)), (SELECT COALESCE(MAX(taken.id), 130000) + 1 FROM iam_menu taken WHERE taken.id >= 130000 AND taken.id < 140000)), app.id, NULL, '权限与应用', '/org/iam/authorization/root', 'layout.main', 'org.iam.authorization.root', 'DIRECTORY', 'ANY', 'ACTION', 2, TRUE FROM iam_application app
WHERE app.code = 'iam-tenant' AND NOT EXISTS (SELECT 1 FROM iam_menu WHERE application_id = app.id AND route_name = 'org.iam.authorization.root');

INSERT INTO iam_menu (id, application_id, parent_id, name, path, view_path, route_name, kind, match_mode, access_mode, sort_order, enabled)
SELECT COALESCE((SELECT 130018 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_menu taken WHERE taken.id = 130018)), (SELECT COALESCE(MAX(taken.id), 130000) + 1 FROM iam_menu taken WHERE taken.id >= 130000 AND taken.id < 140000)), parent.application_id, parent.id, '角色与授权', '/org/iam/authorization', 'org.iam.authorization', 'org.iam.authorization', 'PAGE', 'ANY', 'ACTION', 1, TRUE FROM iam_menu parent JOIN iam_application app ON app.id = parent.application_id
WHERE app.code = 'iam-tenant' AND parent.route_name = 'org.iam.authorization.root' AND NOT EXISTS (SELECT 1 FROM iam_menu WHERE application_id = parent.application_id AND route_name = 'org.iam.authorization');

INSERT INTO iam_menu (id, application_id, parent_id, name, path, view_path, route_name, kind, match_mode, access_mode, sort_order, enabled)
SELECT COALESCE((SELECT 130019 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_menu taken WHERE taken.id = 130019)), (SELECT COALESCE(MAX(taken.id), 130000) + 1 FROM iam_menu taken WHERE taken.id >= 130000 AND taken.id < 140000)), parent.application_id, parent.id, '应用管理', '/org/iam/applications', 'org.iam.applications', 'org.iam.applications', 'PAGE', 'ANY', 'ACTION', 2, TRUE FROM iam_menu parent JOIN iam_application app ON app.id = parent.application_id
WHERE app.code = 'iam-tenant' AND parent.route_name = 'org.iam.authorization.root' AND NOT EXISTS (SELECT 1 FROM iam_menu WHERE application_id = parent.application_id AND route_name = 'org.iam.applications');

INSERT INTO iam_menu (id, application_id, parent_id, name, path, view_path, route_name, kind, match_mode, access_mode, sort_order, enabled)
SELECT COALESCE((SELECT 130020 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_menu taken WHERE taken.id = 130020)), (SELECT COALESCE(MAX(taken.id), 130000) + 1 FROM iam_menu taken WHERE taken.id >= 130000 AND taken.id < 140000)), app.id, NULL, '安全与合规', '/org/iam/compliance', 'layout.main', 'org.iam.compliance', 'DIRECTORY', 'ANY', 'ACTION', 3, TRUE FROM iam_application app
WHERE app.code = 'iam-tenant' AND NOT EXISTS (SELECT 1 FROM iam_menu WHERE application_id = app.id AND route_name = 'org.iam.compliance');

INSERT INTO iam_menu (id, application_id, parent_id, name, path, view_path, route_name, kind, match_mode, access_mode, sort_order, enabled)
SELECT COALESCE((SELECT 130021 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_menu taken WHERE taken.id = 130021)), (SELECT COALESCE(MAX(taken.id), 130000) + 1 FROM iam_menu taken WHERE taken.id >= 130000 AND taken.id < 140000)), parent.application_id, parent.id, '成员权限', '/security/iam/member/permissions', 'security.iam.member.permissions', 'security.iam.member.permissions', 'PAGE', 'ANY', 'ACTION', 1, TRUE FROM iam_menu parent JOIN iam_application app ON app.id = parent.application_id
WHERE app.code = 'iam-tenant' AND parent.route_name = 'org.iam.compliance' AND NOT EXISTS (SELECT 1 FROM iam_menu WHERE application_id = parent.application_id AND route_name = 'security.iam.member.permissions');

INSERT INTO iam_menu (id, application_id, parent_id, name, path, view_path, route_name, kind, match_mode, access_mode, sort_order, enabled)
SELECT COALESCE((SELECT 130022 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_menu taken WHERE taken.id = 130022)), (SELECT COALESCE(MAX(taken.id), 130000) + 1 FROM iam_menu taken WHERE taken.id >= 130000 AND taken.id < 140000)), parent.application_id, parent.id, '审计', '/security/iam/authorization/audit', 'security.iam.authorization.audit', 'security.iam.authorization.audit', 'PAGE', 'ANY', 'ACTION', 2, TRUE FROM iam_menu parent JOIN iam_application app ON app.id = parent.application_id
WHERE app.code = 'iam-tenant' AND parent.route_name = 'org.iam.compliance' AND NOT EXISTS (SELECT 1 FROM iam_menu WHERE application_id = parent.application_id AND route_name = 'security.iam.authorization.audit');

INSERT INTO iam_menu (id, application_id, parent_id, name, path, view_path, route_name, kind, match_mode, access_mode, sort_order, enabled)
SELECT COALESCE((SELECT 130023 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_menu taken WHERE taken.id = 130023)), (SELECT COALESCE(MAX(taken.id), 130000) + 1 FROM iam_menu taken WHERE taken.id >= 130000 AND taken.id < 140000)), app.id, NULL, '工作台', '/org/iam/workspace', 'layout.main', 'org.iam.workspace', 'DIRECTORY', 'ANY', 'ACTION', 4, TRUE FROM iam_application app
WHERE app.code = 'iam-tenant' AND NOT EXISTS (SELECT 1 FROM iam_menu WHERE application_id = app.id AND route_name = 'org.iam.workspace');

INSERT INTO iam_menu (id, application_id, parent_id, name, path, view_path, route_name, kind, match_mode, access_mode, sort_order, enabled)
SELECT COALESCE((SELECT 130024 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_menu taken WHERE taken.id = 130024)), (SELECT COALESCE(MAX(taken.id), 130000) + 1 FROM iam_menu taken WHERE taken.id >= 130000 AND taken.id < 140000)), parent.application_id, parent.id, '工作台', '/org/iam/workbench', 'org.iam.workbench', 'org.iam.workbench', 'PAGE', 'ANY', 'ACTION', 1, TRUE FROM iam_menu parent JOIN iam_application app ON app.id = parent.application_id
WHERE app.code = 'iam-tenant' AND parent.route_name = 'org.iam.workspace' AND NOT EXISTS (SELECT 1 FROM iam_menu WHERE application_id = parent.application_id AND route_name = 'org.iam.workbench');

INSERT INTO iam_menu (id, application_id, parent_id, name, path, view_path, route_name, kind, match_mode, access_mode, sort_order, enabled)
SELECT COALESCE((SELECT 130025 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_menu taken WHERE taken.id = 130025)), (SELECT COALESCE(MAX(taken.id), 130000) + 1 FROM iam_menu taken WHERE taken.id >= 130000 AND taken.id < 140000)), parent.application_id, parent.id, '通讯录', '/org/iam/directory', 'org.iam.directory', 'org.iam.directory', 'PAGE', 'ANY', 'ACTION', 2, TRUE FROM iam_menu parent JOIN iam_application app ON app.id = parent.application_id
WHERE app.code = 'iam-tenant' AND parent.route_name = 'org.iam.workspace' AND NOT EXISTS (SELECT 1 FROM iam_menu WHERE application_id = parent.application_id AND route_name = 'org.iam.directory');

INSERT INTO iam_menu (id, application_id, parent_id, name, path, view_path, route_name, kind, match_mode, access_mode, sort_order, enabled)
SELECT COALESCE((SELECT 130026 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_menu taken WHERE taken.id = 130026)), (SELECT COALESCE(MAX(taken.id), 130000) + 1 FROM iam_menu taken WHERE taken.id >= 130000 AND taken.id < 140000)), app.id, NULL, '开发者平台', '/platform/develop', 'layout.main', 'platform.develop', 'DIRECTORY', 'ANY', 'ACTION', 1, TRUE FROM iam_application app
WHERE app.code = 'platform:develop' AND NOT EXISTS (SELECT 1 FROM iam_menu WHERE application_id = app.id AND route_name = 'platform.develop');

INSERT INTO iam_menu (id, application_id, parent_id, name, path, view_path, route_name, kind, match_mode, access_mode, sort_order, enabled)
SELECT COALESCE((SELECT 130027 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_menu taken WHERE taken.id = 130027)), (SELECT COALESCE(MAX(taken.id), 130000) + 1 FROM iam_menu taken WHERE taken.id >= 130000 AND taken.id < 140000)), parent.application_id, parent.id, '生成二维码', '/platform/develop/qrcode', 'platform.develop.qrcode', 'platform.develop.qrcode', 'PAGE', 'ANY', 'ACTION', 1, TRUE FROM iam_menu parent JOIN iam_application app ON app.id = parent.application_id
WHERE app.code = 'platform:develop' AND parent.route_name = 'platform.develop' AND NOT EXISTS (SELECT 1 FROM iam_menu WHERE application_id = parent.application_id AND route_name = 'platform.develop.qrcode');

INSERT INTO iam_menu (id, application_id, parent_id, name, path, view_path, route_name, kind, match_mode, access_mode, sort_order, enabled)
SELECT COALESCE((SELECT 130028 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_menu taken WHERE taken.id = 130028)), (SELECT COALESCE(MAX(taken.id), 130000) + 1 FROM iam_menu taken WHERE taken.id >= 130000 AND taken.id < 140000)), parent.application_id, parent.id, '客户端管理', '/platform/develop/client', 'platform.develop.client', 'platform.develop.client', 'PAGE', 'ANY', 'ACTION', 2, TRUE FROM iam_menu parent JOIN iam_application app ON app.id = parent.application_id
WHERE app.code = 'platform:develop' AND parent.route_name = 'platform.develop' AND NOT EXISTS (SELECT 1 FROM iam_menu WHERE application_id = parent.application_id AND route_name = 'platform.develop.client');

INSERT INTO iam_menu (id, application_id, parent_id, name, path, view_path, route_name, kind, match_mode, access_mode, sort_order, enabled)
SELECT COALESCE((SELECT 130029 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_menu taken WHERE taken.id = 130029)), (SELECT COALESCE(MAX(taken.id), 130000) + 1 FROM iam_menu taken WHERE taken.id >= 130000 AND taken.id < 140000)), parent.application_id, parent.id, '社交管理', '/platform/develop/social', 'platform.develop.social', 'platform.develop.social', 'PAGE', 'ANY', 'ACTION', 3, TRUE FROM iam_menu parent JOIN iam_application app ON app.id = parent.application_id
WHERE app.code = 'platform:develop' AND parent.route_name = 'platform.develop' AND NOT EXISTS (SELECT 1 FROM iam_menu WHERE application_id = parent.application_id AND route_name = 'platform.develop.social');

INSERT INTO iam_menu (id, application_id, parent_id, name, path, view_path, route_name, kind, match_mode, access_mode, sort_order, enabled)
SELECT COALESCE((SELECT 130030 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_menu taken WHERE taken.id = 130030)), (SELECT COALESCE(MAX(taken.id), 130000) + 1 FROM iam_menu taken WHERE taken.id >= 130000 AND taken.id < 140000)), parent.application_id, parent.id, '业务ID管理', '/platform/develop/id', 'platform.develop.id', 'platform.develop.id', 'PAGE', 'ANY', 'ACTION', 4, TRUE FROM iam_menu parent JOIN iam_application app ON app.id = parent.application_id
WHERE app.code = 'platform:develop' AND parent.route_name = 'platform.develop' AND NOT EXISTS (SELECT 1 FROM iam_menu WHERE application_id = parent.application_id AND route_name = 'platform.develop.id');

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
WHERE app.code = 'iam-platform' AND menu.route_name = 'platform.iam.manage' AND action.code = 'iam-platform:account:read' AND NOT EXISTS (SELECT 1 FROM iam_menu_action WHERE application_id = menu.application_id AND menu_id = menu.id AND action_id = action.id);

INSERT INTO iam_menu_action (application_id, menu_id, action_id)
SELECT menu.application_id, menu.id, action.id FROM iam_menu menu JOIN iam_application app ON app.id = menu.application_id JOIN iam_action action ON action.application_id = menu.application_id
WHERE app.code = 'iam-platform' AND menu.route_name = 'platform.iam.manage' AND action.code = 'iam-platform:assignment:read' AND NOT EXISTS (SELECT 1 FROM iam_menu_action WHERE application_id = menu.application_id AND menu_id = menu.id AND action_id = action.id);

INSERT INTO iam_menu_action (application_id, menu_id, action_id)
SELECT menu.application_id, menu.id, action.id FROM iam_menu menu JOIN iam_application app ON app.id = menu.application_id JOIN iam_action action ON action.application_id = menu.application_id
WHERE app.code = 'iam-platform' AND menu.route_name = 'platform.iam.manage' AND action.code = 'iam-platform:delegation:read' AND NOT EXISTS (SELECT 1 FROM iam_menu_action WHERE application_id = menu.application_id AND menu_id = menu.id AND action_id = action.id);

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

INSERT INTO iam_menu_action (application_id, menu_id, action_id)
SELECT menu.application_id, menu.id, action.id FROM iam_menu menu JOIN iam_application app ON app.id = menu.application_id JOIN iam_action action ON action.application_id = menu.application_id
WHERE app.code = 'platform:develop' AND menu.route_name = 'platform.develop' AND action.code = 'iam-platform:id-allocation:read' AND NOT EXISTS (SELECT 1 FROM iam_menu_action WHERE application_id = menu.application_id AND menu_id = menu.id AND action_id = action.id);

INSERT INTO iam_menu_action (application_id, menu_id, action_id)
SELECT menu.application_id, menu.id, action.id FROM iam_menu menu JOIN iam_application app ON app.id = menu.application_id JOIN iam_action action ON action.application_id = menu.application_id
WHERE app.code = 'platform:develop' AND menu.route_name = 'platform.develop' AND action.code = 'iam-platform:social-config:read' AND NOT EXISTS (SELECT 1 FROM iam_menu_action WHERE application_id = menu.application_id AND menu_id = menu.id AND action_id = action.id);

INSERT INTO iam_menu_action (application_id, menu_id, action_id)
SELECT menu.application_id, menu.id, action.id FROM iam_menu menu JOIN iam_application app ON app.id = menu.application_id JOIN iam_action action ON action.application_id = menu.application_id
WHERE app.code = 'platform:develop' AND menu.route_name = 'platform.develop' AND action.code = 'platform:develop:client:query' AND NOT EXISTS (SELECT 1 FROM iam_menu_action WHERE application_id = menu.application_id AND menu_id = menu.id AND action_id = action.id);

INSERT INTO iam_menu_action (application_id, menu_id, action_id)
SELECT menu.application_id, menu.id, action.id FROM iam_menu menu JOIN iam_application app ON app.id = menu.application_id JOIN iam_action action ON action.application_id = menu.application_id
WHERE app.code = 'platform:develop' AND menu.route_name = 'platform.develop' AND action.code = 'platform:develop:qrcode' AND NOT EXISTS (SELECT 1 FROM iam_menu_action WHERE application_id = menu.application_id AND menu_id = menu.id AND action_id = action.id);

INSERT INTO iam_menu_action (application_id, menu_id, action_id)
SELECT menu.application_id, menu.id, action.id FROM iam_menu menu JOIN iam_application app ON app.id = menu.application_id JOIN iam_action action ON action.application_id = menu.application_id
WHERE app.code = 'platform:develop' AND menu.route_name = 'platform.develop.qrcode' AND action.code = 'platform:develop:qrcode' AND NOT EXISTS (SELECT 1 FROM iam_menu_action WHERE application_id = menu.application_id AND menu_id = menu.id AND action_id = action.id);

INSERT INTO iam_menu_action (application_id, menu_id, action_id)
SELECT menu.application_id, menu.id, action.id FROM iam_menu menu JOIN iam_application app ON app.id = menu.application_id JOIN iam_action action ON action.application_id = menu.application_id
WHERE app.code = 'platform:develop' AND menu.route_name = 'platform.develop.client' AND action.code = 'platform:develop:client:query' AND NOT EXISTS (SELECT 1 FROM iam_menu_action WHERE application_id = menu.application_id AND menu_id = menu.id AND action_id = action.id);

INSERT INTO iam_menu_action (application_id, menu_id, action_id)
SELECT menu.application_id, menu.id, action.id FROM iam_menu menu JOIN iam_application app ON app.id = menu.application_id JOIN iam_action action ON action.application_id = menu.application_id
WHERE app.code = 'platform:develop' AND menu.route_name = 'platform.develop.social' AND action.code = 'iam-platform:social-config:read' AND NOT EXISTS (SELECT 1 FROM iam_menu_action WHERE application_id = menu.application_id AND menu_id = menu.id AND action_id = action.id);

INSERT INTO iam_menu_action (application_id, menu_id, action_id)
SELECT menu.application_id, menu.id, action.id FROM iam_menu menu JOIN iam_application app ON app.id = menu.application_id JOIN iam_action action ON action.application_id = menu.application_id
WHERE app.code = 'platform:develop' AND menu.route_name = 'platform.develop.id' AND action.code = 'iam-platform:id-allocation:read' AND NOT EXISTS (SELECT 1 FROM iam_menu_action WHERE application_id = menu.application_id AND menu_id = menu.id AND action_id = action.id);

CREATE TEMPORARY TABLE IF NOT EXISTS iam_bootstrap_new_revision (domain VARCHAR(16) PRIMARY KEY);
DELETE FROM iam_bootstrap_new_revision;
INSERT INTO iam_bootstrap_new_revision(domain) SELECT 'PLATFORM' FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_role_revision v JOIN iam_role_definition r ON r.id=v.role_id WHERE r.domain='PLATFORM' AND r.code='role_admin' AND r.tenant_key=0 AND v.revision=1);
INSERT INTO iam_bootstrap_new_revision(domain) SELECT 'TENANT' FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_role_revision v JOIN iam_role_definition r ON r.id=v.role_id WHERE r.domain='TENANT' AND r.code='tenant-governance' AND r.tenant_key=0 AND v.revision=1);
-- 治理角色：每个域恰好一个启用的 SYSTEM 角色，组织初始化依赖这一唯一性。
INSERT INTO iam_role_definition (id, domain, tenant_id, kind, code, name, description, enabled)
SELECT 140001, 'PLATFORM', NULL, 'SYSTEM', 'role_admin', '超级管理员', '系统平台超级管理员，受控直接分配并覆盖新增平台操作', TRUE FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM iam_role_definition WHERE domain = 'PLATFORM' AND tenant_key = 0 AND code = 'role_admin');

INSERT INTO iam_role_revision (id, role_id, kind, revision, base_revision_id, metadata_overrides, resource_field_permissions)
SELECT 141001, role.id, 'SYSTEM', 1, NULL, CAST('{}' AS JSON), (SELECT JSON_OBJECT(CAST(resource.id AS CHAR), CAST('{"displayName": {"visibility": "FULL", "editable": true}, "avatar": {"visibility": "FULL", "editable": true}, "phone": {"visibility": "FULL", "editable": true}, "email": {"visibility": "FULL", "editable": true}}' AS JSON)) FROM iam_resource resource JOIN iam_application app ON app.id=resource.application_id WHERE app.code='iam-platform' AND resource.code='member') FROM iam_role_definition role
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'role_admin' AND NOT EXISTS (SELECT 1 FROM iam_role_revision WHERE role_id = role.id AND revision = 1);

INSERT INTO iam_role_definition (id, domain, tenant_id, kind, code, name, description, enabled)
SELECT 140002, 'TENANT', NULL, 'SYSTEM', 'tenant-governance', '组织治理', '组织所有者的全部治理操作', TRUE FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM iam_role_definition WHERE domain = 'TENANT' AND tenant_key = 0 AND code = 'tenant-governance');

INSERT INTO iam_role_revision (id, role_id, kind, revision, base_revision_id, metadata_overrides, resource_field_permissions)
SELECT 141002, role.id, 'SYSTEM', 1, NULL, CAST('{}' AS JSON), CAST('{}' AS JSON) FROM iam_role_definition role
WHERE role.domain = 'TENANT' AND role.tenant_key = 0 AND role.code = 'tenant-governance' AND NOT EXISTS (SELECT 1 FROM iam_role_revision WHERE role_id = role.id AND revision = 1);

-- 治理授权：治理角色覆盖本域全部操作，范围为全域。
INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'role_admin' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:account:create' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'role_admin' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:account:delete' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'role_admin' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:account:disable' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'role_admin' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:account:enable' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'role_admin' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:account:lock' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'role_admin' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:account:lookup' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'role_admin' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:account:read' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'role_admin' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:account:reset-password' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'role_admin' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:account:unlock' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'role_admin' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:account:update' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'role_admin' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:action:create' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'role_admin' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:action:delete' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'role_admin' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:action:read' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'role_admin' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:action:status' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'role_admin' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:action:update' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'role_admin' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:application:create' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'role_admin' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:application:delete' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'role_admin' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:application:purge' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'role_admin' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:application:read' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'role_admin' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:application:status' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'role_admin' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:application:update' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'role_admin' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:assignment:create' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'role_admin' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:assignment:delete' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'role_admin' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:assignment:read' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'role_admin' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:assignment:update' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'role_admin' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:assignment:upgrade' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'role_admin' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:audit:read' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'role_admin' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:authorization:diagnose' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'role_admin' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:delegation:create' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'role_admin' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:delegation:delete' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'role_admin' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:delegation:preview' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'role_admin' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:delegation:read' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'role_admin' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:delegation:update' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'role_admin' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:dictionary:create' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'role_admin' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:dictionary:delete' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'role_admin' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:dictionary:read' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'role_admin' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:dictionary:update' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'role_admin' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:entitlement:preview' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'role_admin' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:entitlement:read' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'role_admin' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:entitlement:update' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'role_admin' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:group:create' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'role_admin' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:group:delete' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'role_admin' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:group:preview' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'role_admin' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:group:read' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'role_admin' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:group:update' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'role_admin' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:id-allocation:create' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'role_admin' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:id-allocation:delete' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'role_admin' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:id-allocation:read' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'role_admin' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:id-allocation:update' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'role_admin' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:member:create' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'role_admin' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:member:read' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'role_admin' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:member:remove' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'role_admin' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:member:status' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'role_admin' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:member:update' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'role_admin' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:menu:create' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'role_admin' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:menu:delete' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'role_admin' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:menu:read' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'role_admin' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:menu:update' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'role_admin' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:plan:create' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'role_admin' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:plan:read' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'role_admin' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:plan:update' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'role_admin' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:resource:create' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'role_admin' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:resource:delete' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'role_admin' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:resource:read' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'role_admin' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:resource:update' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'role_admin' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:role:create' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'role_admin' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:role:delete' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'role_admin' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:role:preview' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'role_admin' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:role:publish' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'role_admin' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:role:read' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'role_admin' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:role:status' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'role_admin' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:shared-role:create' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'role_admin' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:shared-role:delete' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'role_admin' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:shared-role:preview' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'role_admin' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:shared-role:publish' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'role_admin' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:shared-role:read' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'role_admin' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:shared-role:status' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'role_admin' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:social-config:create' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'role_admin' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:social-config:delete' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'role_admin' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:social-config:read' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'role_admin' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:social-config:update' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'role_admin' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:tenant:create' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'role_admin' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:tenant:preview' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'role_admin' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:tenant:read' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'role_admin' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:tenant:update' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'role_admin' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'platform:develop:client:create' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'role_admin' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'platform:develop:client:delete' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'role_admin' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'platform:develop:client:detail' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'role_admin' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'platform:develop:client:query' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'role_admin' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'platform:develop:client:reset' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'role_admin' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'platform:develop:client:update' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'role_admin' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'platform:develop:qrcode' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'TENANT' AND role.tenant_key = 0 AND role.code = 'tenant-governance' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-tenant:application:read' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'TENANT' AND role.tenant_key = 0 AND role.code = 'tenant-governance' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-tenant:assignment:create' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'TENANT' AND role.tenant_key = 0 AND role.code = 'tenant-governance' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-tenant:assignment:delete' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'TENANT' AND role.tenant_key = 0 AND role.code = 'tenant-governance' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-tenant:assignment:read' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'TENANT' AND role.tenant_key = 0 AND role.code = 'tenant-governance' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-tenant:assignment:update' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'TENANT' AND role.tenant_key = 0 AND role.code = 'tenant-governance' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-tenant:audience:read' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'TENANT' AND role.tenant_key = 0 AND role.code = 'tenant-governance' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-tenant:audience:update' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'TENANT' AND role.tenant_key = 0 AND role.code = 'tenant-governance' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-tenant:audit:read' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'TENANT' AND role.tenant_key = 0 AND role.code = 'tenant-governance' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-tenant:authorization:diagnose' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'TENANT' AND role.tenant_key = 0 AND role.code = 'tenant-governance' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-tenant:delegation:create' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'TENANT' AND role.tenant_key = 0 AND role.code = 'tenant-governance' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-tenant:delegation:delete' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'TENANT' AND role.tenant_key = 0 AND role.code = 'tenant-governance' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-tenant:delegation:preview' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'TENANT' AND role.tenant_key = 0 AND role.code = 'tenant-governance' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-tenant:delegation:read' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'TENANT' AND role.tenant_key = 0 AND role.code = 'tenant-governance' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-tenant:delegation:update' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'TENANT' AND role.tenant_key = 0 AND role.code = 'tenant-governance' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-tenant:department:create' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'TENANT' AND role.tenant_key = 0 AND role.code = 'tenant-governance' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-tenant:department:delete' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'TENANT' AND role.tenant_key = 0 AND role.code = 'tenant-governance' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-tenant:department:read' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'TENANT' AND role.tenant_key = 0 AND role.code = 'tenant-governance' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-tenant:department:update' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'TENANT' AND role.tenant_key = 0 AND role.code = 'tenant-governance' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-tenant:directory-policy:read' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'TENANT' AND role.tenant_key = 0 AND role.code = 'tenant-governance' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-tenant:directory-policy:update' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'TENANT' AND role.tenant_key = 0 AND role.code = 'tenant-governance' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-tenant:directory:read' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'TENANT' AND role.tenant_key = 0 AND role.code = 'tenant-governance' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-tenant:field-policy:read' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'TENANT' AND role.tenant_key = 0 AND role.code = 'tenant-governance' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-tenant:field-policy:update' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'TENANT' AND role.tenant_key = 0 AND role.code = 'tenant-governance' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-tenant:group:create' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'TENANT' AND role.tenant_key = 0 AND role.code = 'tenant-governance' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-tenant:group:delete' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'TENANT' AND role.tenant_key = 0 AND role.code = 'tenant-governance' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-tenant:group:preview' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'TENANT' AND role.tenant_key = 0 AND role.code = 'tenant-governance' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-tenant:group:read' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'TENANT' AND role.tenant_key = 0 AND role.code = 'tenant-governance' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-tenant:group:update' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'TENANT' AND role.tenant_key = 0 AND role.code = 'tenant-governance' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-tenant:member:create' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'TENANT' AND role.tenant_key = 0 AND role.code = 'tenant-governance' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-tenant:member:departments' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'TENANT' AND role.tenant_key = 0 AND role.code = 'tenant-governance' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-tenant:member:export' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'TENANT' AND role.tenant_key = 0 AND role.code = 'tenant-governance' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-tenant:member:read' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'TENANT' AND role.tenant_key = 0 AND role.code = 'tenant-governance' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-tenant:member:remove' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'TENANT' AND role.tenant_key = 0 AND role.code = 'tenant-governance' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-tenant:member:status' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'TENANT' AND role.tenant_key = 0 AND role.code = 'tenant-governance' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-tenant:member:update' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'TENANT' AND role.tenant_key = 0 AND role.code = 'tenant-governance' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-tenant:policy:preview' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'TENANT' AND role.tenant_key = 0 AND role.code = 'tenant-governance' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-tenant:role:create' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'TENANT' AND role.tenant_key = 0 AND role.code = 'tenant-governance' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-tenant:role:delete' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'TENANT' AND role.tenant_key = 0 AND role.code = 'tenant-governance' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-tenant:role:preview' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'TENANT' AND role.tenant_key = 0 AND role.code = 'tenant-governance' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-tenant:role:publish' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'TENANT' AND role.tenant_key = 0 AND role.code = 'tenant-governance' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-tenant:role:read' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'TENANT' AND role.tenant_key = 0 AND role.code = 'tenant-governance' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-tenant:role:status' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'TENANT' AND role.tenant_key = 0 AND role.code = 'tenant-governance' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-tenant:role:upgrade' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'TENANT' AND role.tenant_key = 0 AND role.code = 'tenant-governance' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-tenant:settings:owner-transfer' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'TENANT' AND role.tenant_key = 0 AND role.code = 'tenant-governance' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-tenant:settings:read' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'TENANT' AND role.tenant_key = 0 AND role.code = 'tenant-governance' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-tenant:settings:update' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

-- 默认策略版本：固定版本被租户策略引用；通讯录默认全组织，字段默认脱敏手机邮箱，上限缺省不额外收紧。
INSERT INTO iam_default_policy_revision (id, kind, revision, definition)
SELECT 150001, 'DIRECTORY', 1, CAST('{"scope": "ALL"}' AS JSON) FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM iam_default_policy_revision WHERE kind = 'DIRECTORY' AND revision = 1);

INSERT INTO iam_default_policy_revision (id, kind, revision, definition)
SELECT 150002, 'FIELD', 1, CAST('{"fields": {"displayName": {"visibility": "FULL", "editable": true}, "avatar": {"visibility": "FULL", "editable": true}, "phone": {"visibility": "MASKED", "editable": false}, "email": {"visibility": "MASKED", "editable": false}}}' AS JSON) FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM iam_default_policy_revision WHERE kind = 'FIELD' AND revision = 1);
