-- 补齐正式应用一级目录的 Iconify 图标，仅处理空值，保留已有自定义图标。
-- 以应用编码和路由名定位，不依赖初始化保留 ID；不修改子菜单或权限关联。
SET NAMES utf8mb4;
START TRANSACTION;
UPDATE iam_menu menu
JOIN iam_application app ON app.id = menu.application_id
JOIN (
    SELECT 'iam-platform' AS application_code, 'platform.iam.tenant' AS route_name,
           'ep:office-building' AS icon
    UNION ALL SELECT 'iam-platform', 'platform.iam.config', 'ep:setting'
    UNION ALL SELECT 'iam-platform', 'platform.iam.manage', 'ep:monitor'
    UNION ALL SELECT 'iam-platform', 'platform.iam.security', 'ep:lock'
    UNION ALL SELECT 'iam-tenant', 'org.iam.organization', 'ep:office-building'
    UNION ALL SELECT 'iam-tenant', 'org.iam.authorization.root', 'ep:connection'
    UNION ALL SELECT 'iam-tenant', 'org.iam.compliance', 'ep:lock'
    UNION ALL SELECT 'iam-tenant', 'org.iam.workspace', 'ep:grid'
    UNION ALL SELECT 'platform:develop', 'platform.develop', 'ep:cpu'
) defaults ON defaults.application_code = app.code AND defaults.route_name = menu.route_name
SET menu.icon = defaults.icon, menu.version = menu.version + 1
WHERE menu.parent_id IS NULL
  AND menu.kind = 'DIRECTORY'
  AND (menu.icon IS NULL OR TRIM(menu.icon) = '');
COMMIT;
