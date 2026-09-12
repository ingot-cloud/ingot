# 数据授权 SPEC

> 记录当前已上线并验收的有效事实：按资源与操作的数据范围、请求期快照、默认 SQL 谓词。

## 1. 资源目录 `platform_resource`

| 字段 | 说明 |
|---|---|
| `id` | 资源 ID |
| `app_id` | 所属应用 |
| `code` | 应用内稳定编码，创建后不可改；全局使用时与应用命名空间组合 |
| `name` / `status` | 名称 / 状态 |

唯一约束 `(app_id, code)`。未登记资源不自动获得行级过滤。仍被权限或数据规则引用时拒绝删除。

## 2. 角色数据规则

范围只存在于规则表，不在角色实体上。

| 表 | 含义 |
|---|---|
| `platform_role_data_rule` | 平台默认规则。`scope_type=CUSTOM` 禁止写入租户部门 ID |
| `tenant_role_data_rule_private` | 租户追加规则；不能删除或改写平台默认层 |

规则按角色来源、租户（私有）、授权权限、资源及 `scope_type` 唯一；同类型 CUSTOM 部门集合在一条规则中维护。没有对应功能授权的范围规则不产生权限。缺少规则默认无数据授权，不从角色全局 ALL 兜底。

`DataScopeTypeEnum`：`ALL(0)` / `CUSTOM(1)` / `DEPT_AND_CHILD(2)` / `DEPT(3)` / `SELF(9)`。

- `ALL`：当前租户当前资源全部行，SQL 不加行条件。
- `SELF`：资源归属用户字段等值，默认列 `created_by`，可由表映射覆盖。
- `DEPT`：仅绑定或当前部门。
- `DEPT_AND_CHILD`：组装快照时展开为部门 ID 列表。
- `CUSTOM`：当前租户指定部门 ID。

部门角色的 DEPT / DEPT_AND_CHILD 以**角色绑定部门**为根；非部门角色使用用户当前所属部门。租户管理员对已登记且本租户可访问的资源写成 ALL。

## 3. 部门角色绑定

`tenant_role_user_private` 唯一性包含 `tenant_id / user_id / platform_role / role_id / dept_id`。空部门用生成列 `dept_id_uk = IFNULL(dept_id, 0)` 做唯一索引，业务参数仍用 `null` 表示非部门绑定。真实部门 ID 不得为 `0`。同一用户同角色可分别绑定多个部门；离开部门同步移除对应绑定。

## 4. 授权快照

`AuthorizationSnapshotDTO` 为菜单、前端权限列表、API 功能准入与数据范围的同源事实：

- `tenantId`、`userId`
- `roleBindings`：`roleId`、`platformRole`、`roleCode`、`deptId`、`filterDept`
- `permissionCodes`：当前启用的**具体**权限码（通配已展开）
- `resourceRules`：`resourceCode`、`permissionCode`、`scopeType`、`deptIds`、`self`
- `source`、`version`、`generatedAt`、`expiresAt`

禁止直接序列化角色领域类型。空授权（无具体权限且无资源规则）不写热缓存。

组装：`AuthorizationSnapshotAssembler` 调用 `EffectiveAuthorizationService`，合并平台默认规则与租户追加规则（并集），展开 `DEPT_AND_CHILD`，并把租户管理员写成资源 ALL。`expiresAt` 不超过 `generatedAt + 30s`，且不跨越最近 `tenant_app_config.validFrom/validUntil` 边界。`generatedAt` 起算于源端组装开始，缓存回填不得重置。

内部接口：`POST /inner/authorization/snapshot`（`@Permit(INNER)`）。只使用认证传递的租户/用户；请求体若带身份且与上下文不一致则 403。旧 `RemotePmsDataScopeService` 四个路径已删除。

当前用户：`GET /v1/auth/user/permissions` 返回同一快照的具体权限码与期限字段，禁止指定其它用户。

## 5. 请求期接入

```text
JWT + Session → 当前有效租户成员
        ↓
AuthorizationSnapshotFilter（跳过 /inner/**）
        ├─ 快照绑到 AuthorizationSnapshotHolder
        └─ 把 permissionCodes 合并进 Authentication（供 PreAuthorize）
        ↓
@AdminOrHasAnyAuthority / @HasAnyAuthority     # 功能准入
@DataScope(resource, permission)               # 行过滤
DataScopeGuard.assertWritable(...)             # 写归属
```

会话存储短时故障宽限不延长业务授权期限。没有有效快照时即使身份处于宽限也不能执行受保护业务。过期刷新失败返回 503（`AuthorizationSnapshot.Unavailable`），明确无权限 403，认证无效 401。故障不得返回成功空业务结果。

登录 `OnlineToken.authorities` 只含角色码（及客户端 scope）；业务权限码不以会话集合并为准。

## 6. 注解职责

| 层 | API | 只回答 |
|---|---|---|
| 功能准入 | `@AdminOrHasAnyAuthority` / `@HasAnyAuthority` | 当前方法能不能进；超管 `ROLE_ADMIN` 短路 |
| 数据范围 | `@DataScope(resource, permission)` | 从快照取规则改 SQL。`permission` 是规则键，不是第二道功能鉴权 |
| 写归属 | `DataScopeGuard.assertWritable(resource, permission, target)` | INSERT / 改部门 / 改归属人是否落在同一规则内 |

`@DataScope` / Guard **不再**因快照缺少权限码抛 `AuthorizationDenied`。无匹配规则：读压空帧 → SQL `1=2`；写 `ds_forbidden`。漏配功能注解时读空写拒绝，不能当成可省略 PreAuthorize；明确无功能权仍由方法安全返回 403。

消费约定：

- 纯功能接口只加功能注解。
- 列表/详情/更新/删除：功能注解 + `@DataScope`；更新若改归属再加 Guard。
- 创建：功能注解 + Guard；INSERT 不靠 `@DataScope` 改 SQL。
- 查询、更新、删除使用相同 `(resource, permission)`。
- 嵌套调用用上下文栈保存并恢复；异步必须显式建立身份及授权边界，不能靠继承 ThreadLocal 放行。
- 旧空参 `@DataScope` 已删除。

表映射来自 `@DataScopeTable` 或 `ingot.mybatis.scope`：`table`、`scopeColumn`（默认 `dept_id`）、`userColumn`（默认 `created_by`）。禁止把请求参数拼进 SQL 标识。未登记到 `tables` 的表不加本层谓词。

## 7. 默认 SQL 谓词

行过滤在数据库完成，禁止内存筛。租户条件由 `TenantLineInnerInterceptor` 追加；`CustomDataPermissionHandler` 只在当前帧上追加本人 / 部门谓词。分页、count、UPDATE、DELETE 走同一谓词。

| 范围 | SQL |
|---|---|
| ALL / skip | 不加行条件 |
| SELF | `{userColumn} = :userId` |
| 部门（DEPT / DEPT_AND_CHILD / CUSTOM） | `{scopeColumn} IN (:deptIds)` |
| 本人与部门并集 | `({userColumn} = :userId OR {scopeColumn} IN (:deptIds))` |
| 无规则 / 空部门 / 无上下文 / 跨资源表套用当前帧 | `1 = 2` |

受保护业务表须具备 `(tenant_id, {scopeColumn})` 索引；SELF 热路径另加 `(tenant_id, {userColumn})`。

本形态适合单租户、部门大约几十到几百、索引齐全的查询。超大 `IN` 改闭包表、`OR` 恶化改 `UNION ALL` 尚未上线，见 active change `20260912-mybatis-data-scope-predicate-scale`。

## 8. 缓存与失效

消费模块配置前缀 `ingot.mybatis.scope`，框架只收 `LayeredCacheSettings`。

| 项 | 现行 |
|---|---|
| 缓存名 | `authorization-snapshot` |
| 键 | `{tenantId}:{userId}` |
| L2 Redis key | `in:auth:snapshot:{tenantId}:{userId}` |
| L1/L2 TTL | 默认 30s，装配时 cap 到上限 30s |
| Resilient / LKG / 地板 | **关闭** |
| 空授权 | 不写热缓存（`cacheable` 过滤） |
| 失效 | 授权写事务提交后本节点 `evictAll`，并广播 `authorization.invalidate`；订阅方全量清热缓存 |
| 派生缓存 | 未使用 `VersionedDerivedCache` |

每次读取校验 `expiresAt`；过期则 evict 再加载，刷新失败 503。广播失败不能重新激活旧授权，期限提供最坏时效边界。30 秒界限约束后续判定，不中断已经通过授权并执行中的长事务。

## 9. 管理接口

资源目录：`/v1/platform/config/apps/{appId}/resources`，操作码 `platform:config:app:resource:query|create|update|delete`。

角色数据规则（整层替换当前操作者可管理的来源层）：

| Method | 路径 | 操作码 |
|---|---|---|
| GET/PUT | `/v1/platform/config/role/{id}/data-rules` | `platform:config:role:data-rule:query\|set` |
| GET/PUT | `/v1/org/role/{id}/data-rules` | `org:contacts:role:data-rule:query\|set` |

`RoleDataRuleItemDTO`：`permissionId`、`resourceId`、`scopeType`、`scopes`（CUSTOM 为部门 ID 列表，其它类型为空数组）。

委派：全部授权写入口在服务层校验拟授予功能、数据范围与绑定部门不超过操作者自身；SELF 按接收用户与实际绑定部门校验，不能把授予者 SELF 直接当成接收者 SELF 的子集。

## 10. 运行时错误

| 语义 | HTTP | 约定 |
|---|---|---|
| 认证无效 | 401 | 既有安全过滤器；Guard `ds_401` |
| 明确无权限 | 403 | `AuthorizationDenied`；写无规则 `ds_forbidden` |
| 授权服务不可用或过期刷新失败 | 503 | `AuthorizationSnapshot.Unavailable` |
| 受保护页未关联权限 | 400 | `ProtectedMenuRequiresPermissions` |
| 权限仍被引用不可删 | 400 | `PermissionInUse` |
| 部门角色重复绑定 | 400 | `DuplicateDeptBinding` |
| 平台 CUSTOM 含租户部门 | 400 | `PlatformCustomTenantDeptForbidden` |
