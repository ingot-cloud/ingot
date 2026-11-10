# IAM SQL 防火墙兼容性检查与修复

> 状态：completed

2026-11-07 用户在验收时报告 GET /v1/tenant/members/{id}/groups 报错，附件堆栈定位到 IamTenantGroupMemberMapper.countGroupsOfMember：Druid1.2.27拒绝WHERE 1=1。按用户报错修复授权，恢复现有查询契约。

初始修复成员所属租户组的动态计数条件。用户随后明确要求检查 IAM 其他 SQL，并修复同类 Druid 拦截问题及执行回归，范围扩展到 IAM Mapper、SQL Provider、XML 与手写 Wrapper 片段；不修改业务权限、Druid配置、表结构、历史数据和数据库升级脚本。不提前更新current或归档，Git commit 仅按用户明确要求创建。

approved → implementing：依据用户明确报告的接口缺陷开始修复。

初始 SQL 修复通过 20 项定向回归和 IAM 打包。证据见[验证记录](VALIDATION.md)与[test-results.json](test-results.json)。

2026-11-07 扩展方案已按用户授权确认：枚举真实 Mapper 最终 SQL，覆盖可选筛选、权限范围、递归、JSON、排序和分页，使用当前 Druid 版本及仓库 Wall 配置验证；只修复有复现证据的兼容问题，并保持参数绑定、空范围拒绝、租户隔离及查询结果语义。验收以 SQL 防护检查、受影响查询的语义回归及 IAM 测试为准。

扩展检查完成：59 个 Mapper 接口的 187 条自定义语句以 778 个参数场景检查，补充防护拒绝对照；修复空范围 SQL。IAM 完整回归 1,451 项通过，另补跑隔离 MySQL/HTTP 26 项通过。剩余性能、浏览器和 MinIO 3 项可选环境测试未执行。重新打包及边界检查通过。详见[验证记录](VALIDATION.md)、[覆盖清单](sql-wall-coverage.json)、[完整回归结果](extended-test-results.json)和[MySQL 集成结果](mysql-test-results.json)。

## 验收与归档

- 完成日期为 2026-11-10，关联实现提交 `ed90e3c1`。
- 本机开发环境通过真实租户拥有者会话复测原成员所属组接口及受影响查询。另用真实受限会话确认操作权限拒绝。现场样本和覆盖边界见[验证记录](VALIDATION.md#2026-11-10-本机真实会话复测)。
- 已更新 [租户成员](../../../../current/iam/tenant-members/SPEC.md#成员所属用户组)和 [IAM 数据库持久化](../../../../current/iam/database-persistence/SPEC.md#动态范围与-sql-防火墙)基线。
- 最终实现遵循扩展后的设计。初始单接口范围按用户授权扩展到 IAM 同类动态范围查询，接口契约、防护配置及数据库结构保持兼容。
