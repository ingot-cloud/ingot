-- 仅调整两域 IAM 授权审计菜单名称，不改变路径、权限关联或历史审计。
SET NAMES utf8mb4;
START TRANSACTION;
UPDATE iam_menu menu
JOIN iam_application app ON app.id = menu.application_id
SET menu.name = '授权审计'
WHERE app.code IN ('iam-platform', 'iam-tenant')
  AND menu.route_name = 'security.iam.authorization.audit'
  AND menu.name <> '授权审计';
COMMIT;
