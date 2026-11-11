# Active Changes

本目录保存正在规划、评审、实施或验证的变更。

创建规则：

```text
<YYYYMMDD>-<domain>-<feature>
```

开始实施前，change 必须具备 `README.md`、`REQUIREMENTS.md`、`DESIGN.md` 和 `TASKS.md`，且状态为 `approved`。

当前变更：


- [同源双入口目标环境发布验收](./20261031-security-same-origin-release-acceptance/README.md)（draft；双域首次部署，复用已有验收，目标环境与失败恢复单列）

已完成的[部门负责人独立维护](../archive/2026/20261019-iam-department-leaders/README.md)于2026-10-20按用户验收确认移入archive。

本轮已验收归档：

- [IAM 多租户身份与访问管理重构](../archive/2026/20260912-iam-identity-access-management/README.md)（completed，2026-11-11 用户确认通过）

- [IAM 共享目录版本校验与长缓存](../archive/2026/20261109-iam-versioned-shared-catalog-cache/README.md)（completed，2026-11-10）
- [IAM 应用开通批量查询与操作目录缓存](../archive/2026/20261109-iam-entitlement-batch-action-catalog-cache/README.md)（completed，2026-11-10）
- [IAM 历史代码清理、职责分层与命名重构](../archive/2026/20261107-iam-legacy-cleanup-package-refactor/README.md)（completed，2026-11-10）
- [IAM SQL 防火墙兼容性检查与修复](../archive/2026/20261107-iam-member-groups-wall-filter/README.md)（completed，2026-11-10）
- [IAM 登录后接口现场与发布验收](../archive/2026/20261110-iam-post-login-release-acceptance/README.md)（completed，2026-11-10）
- [IAM 登录后接口提速与场景提示](../archive/2026/20261109-iam-post-login-read-performance/README.md)（completed，2026-11-10）
- [受保护接口的无令牌响应](../archive/2026/20261109-security-missing-bearer-401/README.md)（completed，2026-11-09）
- [IAM 功能目录与授权补齐](../archive/2026/20261106-iam-resource-catalog/README.md)（completed，2026-11-07）
- [授权审计游标分页与可读展示](../archive/2026/20261024-iam-authorization-audit-readable/README.md)（completed，2026-11-07）
- [20261020-iam-member-batch](../archive/2026/20261020-iam-member-batch/README.md)（completed，2026-10-21）
- [20261020-iam-tenant-member-rejoin](../archive/2026/20261020-iam-tenant-member-rejoin/README.md)（completed，2026-10-20）
- [20261020-iam-recipient-primary-key-password-index](../archive/2026/20261020-iam-recipient-primary-key-password-index/README.md)（completed，2026-10-20）
- [20261020-iam-schema-relations](../archive/2026/20261020-iam-schema-relations/README.md)（completed，2026-10-20）

- [同源登录已验收实现](../archive/2026/20261031-security-same-origin-login/README.md)（completed，2026-10-31）。

已取消归档：

- [DataScope SQL 谓词规模升级](../archive/2026/20260912-mybatis-data-scope-predicate-scale/README.md)（cancelled，2026-11-11 用户确认取消；旧 PMS 授权模型已被 IAM 替代，原规模策略未实施）

- [租户即时授权实际库发布验收](../archive/2026/20261106-iam-tenant-live-authorization-release/README.md)（cancelled，2026-11-10 确认按新库首次部署，无数据迁移）
- [IAM帮助内容方案（已取消）](../archive/2026/20261103-iam-help-center/README.md)（cancelled；2026-11-03 用户改用前端静态帮助，保留历史）
