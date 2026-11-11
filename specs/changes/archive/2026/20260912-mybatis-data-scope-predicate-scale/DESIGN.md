# Design

## 方案摘要

父 change 已交付的默认形态保持为**小数据路径**：快照把 `DEPT_AND_CHILD` 展开成 `deptIds`，`CustomDataPermissionHandler` 生成 `IN` / `OR`。本 change 增加一条**规模路径**，只在触发条件满足时替换谓词，不改授权引擎的范围语义。

推荐实现（待 TASKS 确认后锁定）：

1. **`DEPT_AND_CHILD` 用闭包表求子孙**，业务表仍只存部门 ID。谓词形态为 `{scopeColumn} IN (SELECT descendant_id FROM tenant_dept_closure WHERE ancestor_id IN (:grantRoots) AND tenant_id = :tenantId)`，或等价的 `EXISTS` / `INNER JOIN`。快照对该范围只携带授权根部门（绑定部门或用户当前部门），不再携带整棵展开树。
2. **「本人 OR 部门」热路径**用独立 SQL 改写策略拆成 `UNION ALL`，因为 MyBatis-Plus `MultiDataPermissionHandler.getSqlSegment` 只能返回 WHERE 片段，无法表达 `UNION ALL`。该策略默认关闭。
3. **未触发时零行为变化**：继续走现有 Handler 的 `IN` / `OR`。

不推荐把物化路径写到每张业务表（业务表没有 `dept_path`）。若评审改选物化路径，路径列只加在 `tenant_dept`，业务谓词改为 `scopeColumn IN (SELECT id FROM tenant_dept WHERE path LIKE ...)`，与闭包表互斥。

## 与现状的衔接

| 组件 | 父 change 现状 | 本 change |
|---|---|---|
| `tenant_dept` | 仅 `pid` 邻接表 | 增加闭包表（推荐）或 `path` 列 |
| 授权快照 `deptIds` | `DEPT_AND_CHILD` 已展开 | 规模路径改为 `grantRoots`；小数据路径可继续展开 `IN` |
| `CustomDataPermissionHandler` | WHERE：`IN` / `=` / `OR` / `1=2` | 增加可切换的谓词策略；默认策略不变 |
| `DataScopeAOP` / `DataScopeGuard` | 只取范围帧 | 不改职责 |
| ALL / SELF / CUSTOM / 空规则 | ALL 无行条件；SELF 等值；CUSTOM `IN`；空 `1=2` | 语义与 SQL 形态均保持，不走闭包 |

触发建议（阈值在 TASKS 确认）：

- 展开后部门 ID 数 ≥ N（草案 N=1000）或 EXPLAIN 显示该 `IN` 无法用 `(tenant_id, 部门列)` 时，该请求 / 该租户切到闭包谓词。
- 「本人 OR 部门」且 EXPLAIN type 为 ALL 时，对该映射语句启用 `UNION ALL`。
- 也可按配置强制某租户走规模路径，便于灰度。

## 数据模型

推荐新增闭包表（名称待实施时与迁移编号对齐），示意：

- `tenant_id`、`ancestor_id`、`descendant_id`、`distance`
- 主键或唯一键 `(tenant_id, ancestor_id, descendant_id)`
- 索引 `(tenant_id, descendant_id)` 供反向维护
- 每个部门包含 `distance = 0` 的自身行，便于 `DEPT` 与 `DEPT_AND_CHILD` 共用同一查询而只改根集合

维护：部门创建、改父、软删时同步闭包。改父必须在同一事务内删除旧祖先闭包并重建；失败则部门变更回滚。不在查询路径即时 BFS 邻接表。

业务受保护表索引约定不变：`(tenant_id, {scopeColumn})`；SELF 热路径 `(tenant_id, {userColumn})`。闭包表不替代业务表索引。

## 谓词策略

策略枚举（实施时落到 commons / mybatis-scope，禁止调用点裸写 `"in"` / `"closure"`）：

| 策略 | 适用 | SQL 形态 |
|---|---|---|
| `EXPANDED_IN`（默认） | 中小规模、CUSTOM、DEPT | 与父 change 相同 |
| `CLOSURE_IN` | 达阈值的 `DEPT_AND_CHILD` | 部门列对闭包子孙子查询 / JOIN |
| `UNION_ALL` | 本人与部门并集且 OR 恶化 | `SELECT … WHERE 用户列 = ? UNION ALL SELECT … WHERE 部门谓词` |

`UNION_ALL` 注意：

- 必须包一层或使用 `UNION ALL` + 外层分页，避免两段各自 LIMIT 后拼起来导致分页错误。
- count 应对去重后的行（若同一行同时命中本人与部门，`UNION ALL` 会重复；应用 `UNION` 或外层 `SELECT DISTINCT` / 按主键去重，以与 `OR` 语义对齐）。
- UPDATE/DELETE 不能直接 `UNION ALL` 两张结果表。写路径保持单条 WHERE：优先闭包子查询 + `OR` 等值；仅当写语句 EXPLAIN 同样恶化时，再拆成两条按主键限定的更新，且必须同一事务、同一范围帧。草案倾向：**写语句不启用 `UNION ALL`**，只升级 SELECT。

## 数据流与失败处理

1. 授权快照仍由 PMS 组装。规模路径下 `DEPT_AND_CHILD` 只放根 ID；Handler 根据策略选择展开 `IN` 或闭包子查询。
2. 闭包表缺失、租户闭包未回填、或子查询失败：fail-closed（当前帧按无数据，`1=2`），不得退回内存过滤，也不得 fail-open 去掉行条件。
3. 触发器误判（阈值刚超过但索引仍好）只影响计划，不影响可见行；可用配置关闭规模路径。
4. ALL 在进入 Handler 前即 skip，规模策略不可覆盖。

## 迁移与回滚

- 迁移：建闭包表；按 `tenant_dept` 现网邻接表回填（含 `distance=0`）；部门写路径接维护逻辑。
- 灰度：默认 `EXPANDED_IN`；配置打开指定租户 `CLOSURE_IN`；观察 EXPLAIN 与延迟后再扩大。
- 回滚：关闭规模策略即回到父 change 谓词；闭包表可保留不读。若已改快照不再展开子孙，回滚策略时须同时恢复快照展开，否则默认 `IN` 只含根部门、可见范围变窄——迁移与回滚必须把「快照形态」和「谓词策略」绑在同一开关。

## 测试策略

- 小数据：沿用 `CustomDataPermissionHandlerTest`，策略默认时 SQL 字符串不变。
- 对照：同一授权根，展开 `IN` 与闭包子查询的 SELECT 结果集一致（含软删、改父后）。
- 阈值：低于 N 不生成闭包子查询；高于 N 不生成超长 `IN`。
- ALL / SELF / CUSTOM / `1=2` 回归。
- `UNION ALL`：分页页码、count、本人与部门重叠行只出现一次。
- 部门改父、软删的闭包维护事务测试。
- 用 EXPLAIN 固定一套「规模样本」（模拟上千部门）作为验收夹具，不要求每个环境都有生产级组织树。
