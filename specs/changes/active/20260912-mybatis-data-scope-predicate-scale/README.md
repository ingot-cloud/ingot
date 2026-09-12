# DataScope SQL 谓词规模升级

> 状态：draft

## 元数据

- Change ID：`20260912-mybatis-data-scope-predicate-scale`
- 领域：Framework / MyBatis DataScope / PMS 部门树
- 负责人：jy
- 创建日期：2026-09-12
- 目标发布日期：TBD（父 change 验收后，按组织规模触发再实施）
- 前置依赖：[`20260910-pms-rbac-data-authorization`](../../archive/2026/20260910-pms-rbac-data-authorization/) 已交付默认 `IN` / `OR` 谓词

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

- 父 change [`20260910-pms-rbac-data-authorization`](../../archive/2026/20260910-pms-rbac-data-authorization/DESIGN.md) §4.6.1：已交付默认谓词；规模升级路径从该节拆出，以本 change 为准。当前默认谓词见 [data-authorization](../../../current/pms/data-authorization/SPEC.md)。
- 当前部门模型仅为邻接表（`tenant_dept.pid`），无 path / 闭包表。

## 完成记录

- 完成日期：
- 关联提交或 PR：
- 更新的 current capability：
- 与原设计的差异：
- 取消原因：
