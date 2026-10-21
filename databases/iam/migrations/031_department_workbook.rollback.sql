-- 回退部门表格导入任务与导出操作；对象存储中的文件需按路径另行清理。
DELETE rg FROM iam_role_grant rg
JOIN iam_action action ON action.id = rg.action_id
WHERE action.code = 'iam-tenant:department:export';

DELETE FROM iam_action WHERE code = 'iam-tenant:department:export';

DROP TABLE IF EXISTS iam_department_workbook;
