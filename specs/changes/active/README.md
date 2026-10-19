# Active Changes

本目录保存正在规划、评审、实施或验证的变更。

创建规则：

```text
<YYYYMMDD>-<domain>-<feature>
```

开始实施前，change 必须具备 `README.md`、`REQUIREMENTS.md`、`DESIGN.md` 和 `TASKS.md`，且状态为 `approved`。

当前变更：

- [租户拥有者动态全权限与角色即时生效](./20261014-iam-tenant-live-authorization/README.md)（validating；2026-10-14 开发与隔离验证完成，待用户产品验收，2026-10-19已本地提交）
- [IAM 多租户身份与访问管理重构](./20260912-iam-identity-access-management/README.md)（implementing；主变更状态以其 README 为准，旧 PMS 迁移已退出必需范围）
- [DataScope SQL 谓词规模升级](./20260912-mybatis-data-scope-predicate-scale/README.md)（draft）
