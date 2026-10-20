# IAM 接收成员显式主键与密码历史索引精简

> 状态：completed

- Change ID：`20261020-iam-recipient-primary-key-password-index`
- 负责人：jy；创建日期：2026-10-20。
- 批准记录：用户在前述需求、设计、迁移与测试计划讨论后，确认最终采用 Leaf 发号、IdType.INPUT，并明确要求按该建议修改；本变更记录该批准，实施前决策已完整。
- 范围：仅接收成员关系增加显式主键、密码历史验证后精简索引；不操作实际业务数据库、不改公共 API、全局发号器和其他表结构。
- 依赖：上一轮 `20261020-iam-schema-relations` 的字段说明、生成工具及评估结果；独立验收，不代其确认部署完成。
- 基线：current/security/credential-security、current/iam/platform-read-performance；委派模型以正在实施的 IAM 主变更及租户即时授权变更为补充。

## 工件

- [需求](REQUIREMENTS.md)
- [设计](DESIGN.md)
- [任务](TASKS.md)
- [验证记录](VALIDATION.md)

## 完成记录

- 开发与隔离验证：2026-10-20完成；正常工作区18项Java、54项隔离MySQL及11项Python工具检查通过，两版本索引18组配置对照通过；详见验证记录。
- 部署/产品验收：2026-10-20，用户明确确认“验收完成，归档spec，并且提交代码”。
- 实现提交：`57335bda130407d16f9ab51694a570a01fab0b77`（perf(iam)!: 减少关系重写并统一接收成员主键发号）。

- 完成日期：2026-10-20。
- 当前基线：[IAM数据结构与关系持久化](../../../../current/iam/database-persistence/README.md)、[租户成员](../../../../current/iam/tenant-members/SPEC.md)、[租户部门](../../../../current/iam/tenant-departments/SPEC.md)、[凭证安全](../../../../current/security/credential-security/SPEC.md)。
- 设计差异：采用用户最终确认的Leaf/IdType.INPUT；未采用早期讨论的数据库自增方案，其他表结构未扩展。
