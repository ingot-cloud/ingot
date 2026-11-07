# Design

## 方案摘要

- 列表查询从 MyBatis-Plus `selectPage`（OFFSET + COUNT）改为键集查询：`ORDER BY occurred_at DESC, id DESC LIMIT limit + 1`，多查的一条只用于判断 `hasMore`。
- 新增 `AuditReferenceResolver`，在审计页查询完成后，按引用类型分组批量解析名称并做可见性判定，结果作为页级 `references` 返回。展示文案和枚举本地化由前端负责，后端只负责“ID → 名称 / 状态”。
- 新增两条以租户开头的复合索引，覆盖“操作者 + 时间”和“目标 + 时间”两类筛选。

## 数据模型与接口

### 接口

`GET /v1/platform/authorization/audits` 与 `GET /v1/tenant/authorization/audits`：

| 参数 | 规则 |
|---|---|
| `limit` | 1–100，默认 20；越界返回 400 |
| `cursor` | 可选，上一页响应中的 `nextCursor`；不传表示第一页 |
| `actorMemberId`、`targetType`、`targetId`、`changeType`、`from`、`until` | 沿用现有规则 |

移除 `page`、`pageSize`，响应中不再有 `total`、`page`、`pageSize`。这是破坏性变更，前后端必须同时发布。

响应 `data` 类型为 `AuditCursorPage`（`ingot-commons` 中 `model/iam` 下的新 record）：

```json
{
  "items": [ { "record": "AuditEntry（结构不变）", "capabilities": {} } ],
  "nextCursor": "MTc2MTI4NDgwMDAwMDAwMC4xMjM0NQ",
  "hasMore": true,
  "references": {
    "member:101": { "type": "member", "id": "101", "state": "VISIBLE", "name": "张三" },
    "role:88":    { "type": "role", "id": "88", "state": "DELETED", "name": "旧角色" },
    "department:7": { "type": "department", "id": "7", "state": "HIDDEN" }
  }
}
```

- `nextCursor` 仅在 `hasMore = true` 时返回。
- `references` 以 `{type}:{id}` 为键。`AuditReference(type, id, state, name)` 中，`name` 只在 `VISIBLE` 或 `DELETED`（有快照名称时）返回。
- 新增业务枚举 `AuditReferenceState { VISIBLE, HIDDEN, DELETED }`，形态对齐 `CommonStatusEnum`。
- 新增引用类型常量类 `AuditReferenceType`，包含 `member`、`department`、`group`、`role`、`role-revision`、`assignment`、`delegation`、`tenant`、`account`、`application`、`role-application-task`。这些编码与现有审计 `target_type` 字面量一致，复用写入侧已有的常量。
- `AuditEntry` 的结构不变：`before`、`after` 仍是白名单字符串差异，`actor.displayName` 保留以兼容旧读法。前端改为统一使用 `references`。

### 游标

- 编码：`base64url(无填充)` 包裹 `"{occurredAtEpochMicros}.{id}"`。
- 解码失败、数值非正、或长度超过 64 时，返回 `IamReasonCode.INVALID_ARGUMENT`。
- 游标只是排序位置，不鉴权、不签名：真正的边界条件始终由服务端注入，伪造游标最多只能在自己的可见集合里跳转。

### SQL

```sql
SELECT ... FROM iam_authorization_audit
WHERE tenant_id = ?            -- 平台域为 tenant_id IS NULL；domain 条件保留
  [AND actor_member_id = ?] [AND target_type = ? [AND target_id = ?]] [AND change_type = ?]
  [AND occurred_at >= ?] [AND occurred_at < ?]
  [AND (occurred_at < ? OR (occurred_at = ? AND id < ?))]
ORDER BY occurred_at DESC, id DESC
LIMIT ?                        -- limit + 1
```

游标条件用 MyBatis-Plus `and(w -> w.lt(...).or(o -> o.eq(...).lt(...)))` 表达，继续在 `DiagnoseAuditService` 的 Wrapper 中构建，不新增 XML。

### 索引（迁移 `032_authorization_audit_cursor_index.sql` 及 `.rollback.sql`）

| 索引 | 列 | 服务的查询 |
|---|---|---|
| 已有 `idx_iam_audit_tenant_time` | `(tenant_id, occurred_at, id)` | 无筛选、仅时间或变更类型筛选 |
| 新增 `idx_iam_audit_tenant_actor_time` | `(tenant_id, actor_member_id, occurred_at, id)` | 按操作者筛选 |
| 新增 `idx_iam_audit_tenant_target_time` | `(tenant_id, target_type, target_id, occurred_at, id)` | 按对象类型、对象筛选 |

- `change_type` 区分度低，不单独建索引。
- 迁移脚本沿用 010 的 `information_schema` 判定方式，可重复执行；`ingot_iam.sql`、`004_policy_audit_migration.sql` 和 `026_schema_comments.sql` 中的表定义同步更新。
- 回滚脚本删除这两条索引。

### 对象名称解析

`AuditReferenceResolver` 先由 `AuditReferenceCollector` 从本页记录中收集引用，再分两段解析：先展开分配、委派、版本应用任务和租户账号（各一次批量查询，补充角色、接收对象、版本、成员引用），再按类型批量查名称（`AuditReferenceMapper` 注解 SQL；成员、部门另加一次范围查询）。每种类型至多一次 `IN` 批量查询，与页内条数无关：

| 类型 | 收集来源 | 可见条件 | 名称 |
|---|---|---|---|
| member | `actor_member_id`；target `member`；`OWNER_MEMBER`；`DEPARTMENT_LEADERS` 中的成员；`RECIPIENT_SELECTION` / `SCOPE` 中的成员 | 当前域成员读取权限 + 对象范围（复用现有 `actorNames` 的逻辑），并按管理场景字段策略投影显示名 | 显示名 |
| department | target `department`；`SCOPE` 中的部门 | `TENANT_DEPARTMENT_READ` + 部门范围 | 部门名 |
| group | target `group`；`RECIPIENT_SELECTION` 中的用户组 | 用户组读取权限 | 组名 |
| role | target `role`；`ROLE` | 角色读取权限 | 角色名 |
| role-revision | `ROLE_REVISION` | 角色读取权限 | 版本名；没有版本名时为“第 N 版” |
| assignment | target `assignment`；`assignment_id` | 分配读取权限（实施时不叠加分配范围，组成部分各自按本表判定） | 角色名 · 接收对象名（各自再按上面的规则判定，不可见的部分显示为“无权查看”） |
| delegation | target `delegation`；`delegation_id` | 委派读取权限 | 管理员名（前端追加“的授权管理”） |
| tenant | target `tenant`、`entitlement`（目标 ID 是租户 ID） | 租户域：本租户；平台域：租户读取权限 | 组织名 |
| account | target `account` | 平台域：账号读取权限；租户域：映射为本租户内对应的成员，再按 member 规则判定 | 用户名或成员显示名 |
| application | target `application`、`audience` | 应用读取权限（按当前域 `iam_application.domain` 过滤，不再限于已开通应用：开通关系只决定可用性，不影响名称是否可读） | 应用名 |
| role-application-task | target `ROLE_VERSION_APPLICATION` | 角色读取权限 | 角色名 · 版本名 |

- `directory-policy`、`field-policy`、`member-export`、`member-workbook`、`department-workbook` 等单例或任务类目标不做解析，由前端按类型显示固定文案。
- “读取权限”统一用 `evaluator.evaluate(actor.context())` 的结果判断，整页只计算一次。具体 ACTION 以 `IamAction` 现有常量为准，实施时逐一核对后在 TASKS 中列明。
- 对象不存在时为 `DELETED`。如果该对象正是本条审计的目标，并且 `before.NAME` 有值，就用它作为名称；如果同一对象在本页多条记录中出现，取最新一条的快照。
- 读取 ACTION：成员 `PLATFORM_MEMBER_READ`/`TENANT_MEMBER_READ`；部门 `TENANT_DEPARTMENT_READ`；用户组 `*_GROUP_READ`；角色与版本 平台 `PLATFORM_ROLE_READ` 或 `PLATFORM_SHARED_ROLE_READ`、租户 `TENANT_ROLE_READ`；分配 `*_ASSIGNMENT_READ`；委派 `*_DELEGATION_READ`；应用 `*_APPLICATION_READ`；租户 `PLATFORM_TENANT_READ`（租户域仅本租户）；平台账号 `PLATFORM_ACCOUNT_READ`。
- 结构化值（`SCOPE`、`RECIPIENT_SELECTION`、`DEPARTMENT_LEADERS`）由 `AuditReferenceCollector` 只读解析提取其中的 ID。解析失败只影响引用收集，不改变原值返回；对应前端展示为“无法解析的配置”。

### 写入侧修正

- `MemberWorkbookService`、`DepartmentWorkbookService`、`MemberExportService`：不再写 `NAME = 类型编码`。after 改为空 Map，目标类型本身已经表达了任务种类。
- `DelegationService`（平台）：`RECIPIENT_SELECTION` 写入实际的接收对象 SubjectRef 列表（与租户的 `TenantLiveDelegationService` 一致），不再写 `created`、`before`、`after`。
- `RoleService` 发布版本（实施中补充）：before 的 `ROLE_REVISION` 原先写的是上一版序号，与 after 的版本 ID 语义不一致；改为上一版的版本 ID，首次发布时 before 为空。

## 数据流与失败处理

1. 准入（`*_AUDIT_READ`）→ 校验 `limit`、`cursor` 和筛选 → 构建 Wrapper → 查询 `limit + 1` 行。
2. 截取前 `limit` 行；如果有第 `limit + 1` 行，就用第 `limit` 行生成 `nextCursor`。
3. 收集引用 → 按类型批量查询 → 判定可见性 → 组装 `references`。
4. 映射 `AuditEntry`，复用现有的 `IamAuditValues` 读取和损坏数据 500 语义。实现位于新的 `AuthorizationAuditService`，原 `DiagnoseAuditService.listAudits` 与 `actorNames` 移除。

只读查询，不开写事务。名称解析的任何数据库异常都向上抛出，不返回半成品引用。

## 迁移与回滚

- 发布顺序：执行 032 索引迁移（在线 DDL，`ALGORITHM=INPLACE, LOCK=NONE`）→ 部署 IAM provider → 发布平台与组织前端。前后端接口不兼容，后端上线到前端上线之间，审计页会暂时不可用，应在同一窗口内完成。
- 回滚：前后端一起回退到旧版本；索引保留无害，也可以执行 rollback 脚本删除。写入侧修正产生的新记录在旧版本中仍能正常读取（after 为空或是合法的 JSON 字符串）。

## 测试策略

- 单元测试：游标编解码（非法、边界、超长）；`limit` 校验；结构化值的 ID 提取；三种状态的判定。
- MyBatis/H2 集成测试：相同 `occurred_at` 跨页；筛选 + 游标；租户隔离（伪造游标）；第一页、最后一页和空结果；SQL 次数上限（拦截器计数）。
- MVC 测试：两个域的参数绑定、400 和 403，以及响应结构快照。
- 名称解析：成员字段策略隐藏、超出数据范围、对象已删除且有或无快照名称、租户域的账号映射。
- MySQL 8：对迁移重复执行及回滚做幂等检查；在 10 万级种子数据上对三类查询做 `EXPLAIN`，确认命中索引、无 filesort。
- OpenAPI 快照与 Python 契约回归。
