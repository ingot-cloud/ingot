# 租户即时授权实际库发布验收

> 状态：cancelled
> 创建日期：2026-11-06
> 归档日期：2026-11-10
> 负责人：jy

用户于 2026-11-06 确认即时授权产品验收通过，要求将保留既有 IAM 数据的发布另行跟踪。该范围包括来源库预检、备份恢复、部署窗口切换和回滚演练。

2026-11-10 用户确认按新库首次部署，无需迁移既有数据。旧库迁移验收退出发布范围，原需求、设计和未执行的任务保留供追溯。首次部署采用现有初始化流程，验收按 [IAM 主变更](../../../active/20260912-iam-identity-access-management/README.md) 的空库初始化和联调门禁进行。

- [首次部署数据库基线](../../../../current/iam/database-persistence/SPEC.md)
- [已验收产品变更](../20261014-iam-tenant-live-authorization/README.md)
- [当前能力](../../../../current/iam/tenant-live-authorization/README.md)
- [迁移与回滚手册](../20261014-iam-tenant-live-authorization/MIGRATION.md)
- [已有隔离验收报告](../20261014-iam-tenant-live-authorization/VALIDATION.md)
- [需求](./REQUIREMENTS.md) · [设计](./DESIGN.md) · [任务](./TASKS.md)
