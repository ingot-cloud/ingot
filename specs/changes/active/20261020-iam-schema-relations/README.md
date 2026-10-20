# IAM 字段说明、关系差量写入与主键评估

> 状态：validating

- Change ID：`20261020-iam-schema-relations`
- 负责人：jy
- 创建日期：2026-10-20
- 批准记录：用户完成 Plan 决策后于 2026-10-20 明确要求实施完整计划；draft → review → approved。
- 范围：manifest 全部 63 张表的说明及评估、已有库注释升级、成员部门/用户组/菜单操作/套餐应用/负责人差量写入。
- 不修改主键、字段名、公共 API，不操作实际业务数据库；不改开通重置、人群、委派和角色快照语义。
- 基线：current/iam/tenant-members、tenant-departments、role-version-application；并行租户即时授权 change 的角色/委派模型作为对应实现事实。

## 工件

- [需求](./REQUIREMENTS.md)
- [设计](./DESIGN.md)
- [任务](./TASKS.md)

## 完成记录

- 开发与隔离验证：2026-10-20完成；88个不同Java用例、54个隔离MySQL用例及11个Python工具用例通过，生成和链接核对通过。详见[验证记录](./VALIDATION.md)。
- 部署验收：待完成；验证前不修改 current，不提前归档。
- 提交：用户未要求，不创建提交。
