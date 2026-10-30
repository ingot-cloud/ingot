-- 将租户工作台置于一级导航首位，保留其他菜单顺序、主键与权限关联。
-- 新建库使用最新 006；已有库明确选择目标 IAM 库后执行，完成后重新加载菜单。
SET NAMES utf8mb4;
START TRANSACTION;
UPDATE iam_menu menu
JOIN iam_application app ON app.id = menu.application_id
SET menu.sort_order = 0, menu.version = menu.version + 1
WHERE app.code = 'iam-tenant'
  AND menu.route_name = 'org.iam.workspace'
  AND menu.parent_id IS NULL
  AND menu.kind = 'DIRECTORY'
  AND menu.sort_order <> 0;
COMMIT;
