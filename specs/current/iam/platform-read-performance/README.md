# 平台管理读取性能

平台人员、角色与授权的十个同步 GET 在请求内分别复用普通和实时授权来源，并批量读取列表附属数据，保持既有权限和业务语义。

- 负责人：jy；验收日期：2026-10-14。
- 关联模块：`ingot-iam-provider`。
- [当前读取与查询约束](SPEC.md)
- [已验收变更](../../../changes/archive/2026/20261013-iam-platform-read-performance/README.md)
- [验收依据](../../../changes/archive/2026/20261013-iam-platform-read-performance/VALIDATION.md)
- [角色版本应用规则](../role-version-application/SPEC.md)
