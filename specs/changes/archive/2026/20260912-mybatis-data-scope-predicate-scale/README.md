# DataScope SQL 谓词规模升级

> 状态：cancelled
> 取消及归档日期：2026-11-11

2026-11-11 用户确认取消并归档。该草案依赖的旧 PMS 表、授权快照及规则组装模型已被 IAM 新模型替代，原闭包表 / 物化路径、根部门快照及 `UNION ALL` 规模策略未获批准、未实施，也未完成规模验收。下文目标、范围及配套需求、设计保留为历史方案，不作为当前实施依据。

## 元数据

- Change ID：`20260912-mybatis-data-scope-predicate-scale`
- 领域：Framework / MyBatis DataScope / PMS 部门树
- 负责人：jy
- 创建日期：2026-09-12
- 目标发布日期：不适用（已取消）
- 原前置依赖：[`20260910-pms-rbac-data-authorization`](../20260910-pms-rbac-data-authorization/) 已交付默认 `IN` / `OR` 谓词

## 目标

在行过滤继续下推数据库、授权语义不变的前提下，为超大组织树和「本人 OR 部门」热路径提供可走索引的谓词形态：部门子孙用闭包表（或等价物化路径）代替把整棵树打进一条 `IN`；`OR` 恶化时拆成 `UNION ALL`。中小规模租户保持现有 `IN` / `OR`，升级方案不是全租户默认路径。

## 范围

- 包含：`CustomDataPermissionHandler`（及必要的 SQL 改写插件）谓词策略；部门树存储（闭包表或物化路径）及维护；快照中 `DEPT_AND_CHILD` 的携带形态；触发条件与回退；索引与 EXPLAIN 验收。
- 不包含：RBAC 资源目录、角色 data-rules 配置模型、功能注解职责、授权快照缓存框架、JWT 瘦身。
- 不包含：改回内存过滤、字段级脱敏、显式拒绝规则、给全部 PMS 管理表注册行过滤。
- 不把中小规模租户的默认查询改成闭包 / `UNION ALL`。

## 工件

- [需求](./REQUIREMENTS.md)
- [设计](./DESIGN.md)
- [任务](./TASKS.md)

## 相关变更

- 父 change [`20260910-pms-rbac-data-authorization`](../20260910-pms-rbac-data-authorization/DESIGN.md) §4.6.1：已交付默认谓词；原规模升级路径从该节拆出，历史方案见本 change。
- 立项时部门模型仅为邻接表（`tenant_dept.pid`），无 path / 闭包表。
- [IAM 主变更](../20260912-iam-identity-access-management/README.md)已完成并接管授权模型；未实施本草案的通用闭包 / `UNION ALL` 策略。
- 当前能力以[授权执行与资源接入规则](../../../../current/framework/authorization-execution/SPEC.md)为准，原 PMS 模型不再作为实施依据。

## 取消及归档记录

- 取消及归档日期：2026-11-11，用户明确要求 `cancelled` 并归档。
- 完成日期：不适用；未实施、未验收，不标记 `completed`。
- 关联提交或 PR：本次归档提交（通过本文件 Git 历史定位），未创建 PR。
- 更新的 current capability：无；本次仅取消历史草案，现行授权执行基线保持有效。
- 与原设计的差异：原闭包表 / 物化路径、规模阈值、根部门快照与 `UNION ALL` 策略均未实施。当前 IAM 使用类型化范围生成 SQL，部门子孙通过加载租户树后 BFS 展开；成员部门范围使用关联表 `EXISTS`，既有 MyBatis DataScope 默认 `IN` / `OR` 路径继续保留。
- 取消原因：旧 PMS 表及授权快照、规则组装模型已被 IAM 新模型替代，原草案不再适合作为实施计划；按用户决定取消并归档。
- 未完成项处置：原需求、设计、T1–T4、V1–V2 及验收清单保留，不勾选为完成。若后续负载或执行计划证明需要规模优化，按当前 IAM/SDK 模型重新走 SDD 评估；本次不创建新 change，也不认定长 `IN` / 复杂 `OR` 的规模问题已经解决。

取消前，该草案在 IAM 主变更结项时仍保留为 `draft`，仅迁移了参考入口，未批准或实施谓词重构。本次依据用户决定结束该草案，保留完整历史。
