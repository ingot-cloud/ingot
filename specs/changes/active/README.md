# Active Changes

本目录保存正在规划、评审、实施或验证的变更。

创建规则：

```text
<YYYYMMDD>-<domain>-<feature>
```

开始实施前，change 必须具备 `README.md`、`REQUIREMENTS.md`、`DESIGN.md` 和 `TASKS.md`，且状态为 `approved`。

当前变更：

- [租户即时授权实际库发布验收](./20261106-iam-tenant-live-authorization-release/README.md)（draft；实际来源库预检、备份恢复、切换及回滚）
- [授权审计游标分页与可读展示](./20261024-iam-authorization-audit-readable/README.md)（implementing）
- [同源双入口目标环境发布验收](./20261031-security-same-origin-release-acceptance/README.md)（draft；原实现已本地验收归档，实际环境证据单列）
- [IAM 多租户身份与访问管理重构](./20260912-iam-identity-access-management/README.md)（implementing；主变更状态以其 README 为准，旧 PMS 迁移已退出必需范围）
- [DataScope SQL 谓词规模升级](./20260912-mybatis-data-scope-predicate-scale/README.md)（draft）

已完成的[部门负责人独立维护](../archive/2026/20261019-iam-department-leaders/README.md)于2026-10-20按用户验收确认移入archive。

本轮已验收归档：

- [20261020-iam-member-batch](../archive/2026/20261020-iam-member-batch/README.md)（completed，2026-10-21）
- [20261020-iam-tenant-member-rejoin](../archive/2026/20261020-iam-tenant-member-rejoin/README.md)（completed，2026-10-20）
- [20261020-iam-recipient-primary-key-password-index](../archive/2026/20261020-iam-recipient-primary-key-password-index/README.md)（completed，2026-10-20）
- [20261020-iam-schema-relations](../archive/2026/20261020-iam-schema-relations/README.md)（completed，2026-10-20）

- [同源登录已验收实现](../archive/2026/20261031-security-same-origin-login/README.md)（completed，2026-10-31）。

- [IAM帮助内容方案（已取消）](../archive/2026/20261103-iam-help-center/README.md)（cancelled；2026-11-03 用户改用前端静态帮助，保留历史）
