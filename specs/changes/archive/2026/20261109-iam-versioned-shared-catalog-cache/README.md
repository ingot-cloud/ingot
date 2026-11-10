# IAM 共享目录版本校验与长缓存

> 状态：completed

| 项目 | 值 |
|---|---|
| Change ID | `20261109-iam-versioned-shared-catalog-cache` |
| 领域 | IAM |
| 创建日期 | 2026-11-09 |
| 完成日期 | 2026-11-10 |
| 关联实现提交 | `c57394f4` |

## 目标与范围

用户于 2026-11-09 批准先分别合并 `bootstrap` 菜单和两个 `action-catalog` 的目录查询。应用存在性、菜单准入及租户授权保持实时。操作目录使用单条 SQL 作为生产对照，取代首期短期目录缓存；首期已合并的租户开通与人群查询继续保留。

采集平时和高峰调用频次，在相同 QPS 下评估单条 SQL 与七天版本化 Redis。七天方案仅在隔离环境试验；依据结果选择值得接入的入口，之后再确认生产版本字段、写入与部署方案。角色缓存留待后续。

## 工件

- [需求与验收标准](./REQUIREMENTS.md)
- [详细设计与查询预算](./DESIGN.md)
- [实施及验证任务](./TASKS.md)
- [调用频次与等 QPS 验证](./VALIDATION.md)
- [目标环境验收与采集口径](./TARGET-ACCEPTANCE.md)

## 关联规格

- [首期批量查询归档](../20261109-iam-entitlement-batch-action-catalog-cache/README.md)
- [首期隔离实测与批量查询现场验收](../20261109-iam-entitlement-batch-action-catalog-cache/VALIDATION.md)
- [租户即时授权](../../../../current/iam/tenant-live-authorization/SPEC.md)
- [平台读取性能](../../../../current/iam/platform-read-performance/SPEC.md)
- [数据库持久化](../../../../current/iam/database-persistence/SPEC.md)

## 评审记录

用户批准单条 SQL 与等 QPS 评估的实施顺序，无生产数据库迁移或新增配置。原七天缓存设计作为实验候选。

隔离对照通过后，用户确认保留三个入口的合并直查。七天版本化 Redis 作为隔离实验，后续根据大目录实际读取量、同版本复用与数据库成本单独评审。角色缓存仍属后续工作。

2026-11-10 用户确认按功能正确性收口。平台和租户的本机真实会话、单条目录 SQL 及隔离双节点回归均已通过。完整窗口的自然流量与版本复用观察延期。最终实现采用实时直查，保留首期的开通与人群批量查询。已同步 [读取性能基线](../../../../current/iam/platform-read-performance/SPEC.md#登录菜单与应用操作目录) 和 [租户即时授权基线](../../../../current/iam/tenant-live-authorization/SPEC.md#普通成员的应用开通与人群)。[验收记录](./TARGET-ACCEPTANCE.md)
