# Tasks

## 决策记录及实施门禁

| 决策 | 结果 | 来源/状态 |
|---|---|---|
| 默认谓词 | 中小规模仍为父 change 的 `IN` / `OR`；本 change 不是全租户默认改写 | 已随父 change 确认；本 change 继承 |
| `DEPT_AND_CHILD` 规模实现 | 推荐闭包表 + 子查询/JOIN；与物化路径互斥 | draft，待确认 |
| `UNION ALL` | 仅 SELECT 热路径；写语句保持单条 WHERE | draft，待确认 |
| 触发阈值 | 草案：展开部门数 ≥ 1000 或 EXPLAIN 无法走部门索引 | draft，待确认 |
| 快照形态 | 规模路径只带授权根 ID，与谓词策略同一开关，避免回滚后可见范围变窄 | draft，待确认 |
| 失败 | 闭包不可用 fail-closed（`1=2`），禁止内存过滤与 fail-open | 继承父 change 原则 |
| 前置 | 父 change `20260910-pms-rbac-data-authorization` 验收后再实施 | 本 change 约束 |

状态为 `draft`。需求与设计待确认后改为 `approved` 方可实施。确认前不修改业务代码。

## 实施任务

- [ ] T1：锁定闭包表 vs 物化路径、触发阈值、快照根 ID 开关
  - 依赖：本目录 DESIGN 评审
  - 验收：上表待确认项全部有明确结果并回写本文件
- [ ] T2：闭包表（或路径列）DDL、回填、部门写路径维护与回滚脚本
  - 依赖：T1
  - 验收：改父/软删后子孙查询与邻接表 BFS 对照一致
- [ ] T3：谓词策略接入 `CustomDataPermissionHandler`（及 SELECT 的 `UNION ALL` 改写插件，若 T1 启用）
  - 依赖：T1、T2
  - 验收：默认策略 SQL 与父 change 单测一致；规模策略无超长 `IN`；ALL 不加条件
- [ ] T4：快照组装与策略开关绑定；回滚开关同时恢复展开 `IN`
  - 依赖：T3
  - 验收：开关关闭后可见行与父 change 语义一致，不会只留下根部门

## 验证任务

- [ ] V1：小数据回归 + 规模样本 EXPLAIN + 分页/count 对照
- [ ] V2：更新 `specs/current/` 中数据权限 SQL 契约（父 change 落地的 capability）

## 完成检查

- [ ] 实现与 DESIGN 一致
- [ ] REQUIREMENTS 验收标准全部满足
- [ ] Current 已更新
- [ ] Change 已记录完成信息并归档
