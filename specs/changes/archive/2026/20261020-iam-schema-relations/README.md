# IAM 字段说明、关系差量写入与主键评估

> 状态：completed

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
- 部署/产品验收：2026-10-20，用户明确确认“验收完成，归档spec，并且提交代码”。
- 实现提交：`57335bda130407d16f9ab51694a570a01fab0b77`（perf(iam)!: 减少关系重写并统一接收成员主键发号）。

- 完成日期：2026-10-20。
- 当前基线：[IAM数据结构与关系持久化](../../../../current/iam/database-persistence/README.md)、[租户成员](../../../../current/iam/tenant-members/SPEC.md)、[租户部门](../../../../current/iam/tenant-departments/SPEC.md)、[凭证安全](../../../../current/security/credential-security/SPEC.md)。
- 设计差异：本变更无范围偏离；后续Leaf主键与密码索引由[独立变更](../20261020-iam-recipient-primary-key-password-index/README.md)实施并一并验收。
