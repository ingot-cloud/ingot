# Design

## 1. 架构与事实来源

```text
JWT + Session → 当前有效租户成员
                    ↓
PMS 统一授权解析器 ← 应用状态/租户覆盖
        ↑           ← 角色、权限和资源规则
用户角色绑定（保留部门）
                    ↓
服务端有效授权快照 + 统一分层缓存
        ├─ 菜单可见性 / 前端有效权限
        ├─ API 具体操作鉴权
        └─ 该操作对应的资源范围 → SQL / 写归属校验
```

PMS 内直接调用核心解析器，外部资源服务经受保护的内部接口访问。请求身份来自已认证上下文，不以用户传入角色码代替实际成员关系。默认租户的平台运营授权与租户应用授权分域处理，租户管理员不获得平台运营角色。

当前 JWT 签发已不展开业务权限；保留 sid、用户、租户、客户端 OAuth scope 等定位字段。业务 authorities 和动态部门关系改由统一解析补全，移除旧值并集合并。保留会话撤销、用户类型、登录保护及客户端 scope 的既有职责；临时登录 scope 不作为业务 RBAC 放行依据。

## 2. 数据模型

下列字段和表名为目标模型，DDL 在实施任务中生成；本次仅生成文档。

| 对象 | 目标契约 |
|---|---|
| `platform_app` | 增加默认访问策略 OPEN/CLOSED；既有应用迁移默认 OPEN；全局 status 优先 |
| `tenant_app_config` | 保留 enabled 和有效期，唯一租户/app 关系；存在记录即显式覆盖，不因到期回退默认 |
| `platform_menu` | 保留导航字段与 access_mode，新增 permission_match_mode=ANY/ALL（默认 ANY）；移除旧 permission_id，Button 不再作为导航节点 |
| `platform_menu_permission` | menu_id + permission_id 唯一关系，表示可见性条件，不表示菜单拥有全部操作；同应用具体权限引用 |
| `platform_permission` | 保留 ID、app_id、稳定 code、状态；`node_type` 仅 GROUP / ACTION；删除 `type` / `managed` / `source_type` / `source_id`；应用根以 `platform_app.permission_id` 识别 |
| `platform_resource` | 应用内资源目录，id、app_id、稳定 code、name、status；资源代码全局使用应用命名空间，唯一约束 app_id/code |
| 权限与资源 | 具体数据操作权限关联一个 resource_id；导航专用和纯配置权限可无资源；通配跨资源通过各资源独立规则限制 |
| `platform_role_data_rule` | 平台角色、授予权限、资源、scope_type、scopes 的规则，平台默认范围不允许 CUSTOM 租户部门 ID |
| `tenant_role_data_rule_private` | tenant_id、role_id、platform_role、permission_id、resource_id、scope_type、scopes；表达租户追加规则 |
| `tenant_role_user_private` | 沿用 role_id/platform_role/user_id/dept_id/tenant_id；部门上下文必须贯穿读取、去重、授权和撤销 |

规则按角色来源、租户（私有规则）、授权权限、资源及 scope_type 唯一；同类型 CUSTOM 部门集合在一条规则中维护，不重复叠加行。租户可以为已有功能权限追加数据规则，但不能删除平台默认规则；没有对应功能授权的范围规则不得产生权限。

部门角色唯一性包含 tenant_id/user_id/platform_role/role_id/dept_id。非部门绑定也必须唯一，不能依赖 MySQL 可空唯一索引自然去重；DDL 使用规范化生成列把空部门映射到保留哨兵值，再建立唯一索引，实际部门不得使用该值。业务参数继续使用 null 表示非部门绑定。

保留现有 route_name 作为导航标识，本次不新增权限含义的 menu_code。path、view_path 和 route_name 都不能用于授权匹配。新增封闭配置值使用枚举；跨模块复用放公共基础包，禁止复制字符串语义。

## 3. 菜单与权限生命周期

1. 新建具体业务权限时显式提供稳定完整编码或基于选定业务命名空间的片段；不读取菜单 path 生成编码。普通更新不改 code 或跨应用迁移。
2. 通配统一 `:**`，匹配同应用、启用、已登记的具体权限；权限树移动不得静默变更命名空间。子节点必须满足父命名空间，否则拒绝移动。
3. 受保护页面提交 permissionIds、permissionMatchMode；服务端验证非空、同应用、存在且启用、非 GROUP，并事务保存菜单与关联。
4. OPEN 页面和目录不要求关联。目录只在所属应用有效且存在可见子页面时返回；禁用祖先隐藏其分支，不补回禁用目录。保留现有应用和菜单排序。
5. 默认先选择已有权限。快捷新建先执行独立权限创建，再返回选择结果保存菜单；两个请求不伪装成跨请求事务。菜单失败或用户取消时保留已创建权限，界面明确提示，允许后续复用。
6. 操作权限管理与菜单可见性分开。多个页面可复用 read；approve 不自动包含 read。前端通过具体有效权限决定按钮，API 独立验证同一操作。
7. 删除菜单只删除菜单及其可见性关联。仍被菜单、角色或数据规则引用的权限普通删除应拒绝；解除引用须显式操作，不随菜单级联删除。

## 4. 公共接口变化

沿用 `/v1/platform/config/apps` 下应用、菜单、权限接口族，不增加第二套同义入口。网关前缀另计，下表为 PMS 服务内路径。

### 4.1 沿用并扩展的管理接口

| Method | 路径 | 操作码 | 变化 |
|---|---|---|---|
| GET | `/v1/platform/config/apps/page` | `platform:config:app:query` | 列表含 `defaultAccessMode` |
| GET | `/v1/platform/config/apps/{appId}` | 同上 | `AppDetailVO` 增加 `defaultAccessMode` |
| POST | `/v1/platform/config/apps` | `platform:config:app:create` | `AppCreateDTO.defaultAccessMode`，缺省 OPEN |
| PUT | `/v1/platform/config/apps/{appId}` | `platform:config:app:update` | `AppUpdateDTO.defaultAccessMode` |
| PATCH | `/v1/platform/config/apps/{appId}/status` | 同上 | 不变 |
| DELETE | `/v1/platform/config/apps/{appId}?force=` | `platform:config:app:delete` | 级联删除资源目录与菜单可见性关联 |
| GET | `/v1/platform/config/apps/{appId}/menus/tree` | `platform:config:app:menu:query` | 节点返回 `permissionIds`/`permissionMatchMode`，不含 Button |
| POST | `/v1/platform/config/apps/{appId}/menus` | `platform:config:app:menu:create` | 见菜单 DTO；不再托管权限 |
| PUT | `/v1/platform/config/apps/{appId}/menus/{menuId}` | `platform:config:app:menu:update` | 同上 |
| DELETE | `/v1/platform/config/apps/{appId}/menus/{menuId}` | `platform:config:app:menu:delete` | 只删菜单及可见性关联 |
| GET | `/v1/platform/config/apps/{appId}/permissions/tree` | `platform:config:app:permission:query` | 节点含 `resourceId`/`nodeType`，不含 `type`/`managed`/`readOnly` |
| POST | `/v1/platform/config/apps/{appId}/permissions` | `platform:config:app:permission:create` | 仅 GROUP/ACTION；GROUP 必须以 `:**` 结尾 |
| PUT | `/v1/platform/config/apps/{appId}/permissions/{permissionId}` | `platform:config:app:permission:update` | 不可改 code / 跨应用 / 改 resourceId |
| DELETE | `/v1/platform/config/apps/{appId}/permissions/{permissionId}` | `platform:config:app:permission:delete` | 仍被菜单、角色或数据规则引用则拒绝 |

只读全量树保持：`GET /v1/platform/config/menu/tree`、`GET /v1/platform/config/permission/tree`。租户应用授权保持 `GET /v1/platform/org/tenant/apps`、`PUT /v1/platform/org/tenant/app/status`。

### 4.2 新增资源目录

基址 `/v1/platform/config/apps/{appId}/resources`。

| Method | 路径 | 操作码 |
|---|---|---|
| GET | `` | `platform:config:app:resource:query` |
| POST | `` | `platform:config:app:resource:create` |
| PUT | `/{resourceId}` | `platform:config:app:resource:update` |
| DELETE | `/{resourceId}` | `platform:config:app:resource:delete` |

`AppResourceCreateDTO`：`code`（应用内稳定编码，创建后不可改）、`name`、`status`。`AppResourceUpdateDTO`：`name`、`status`。删除时若仍被权限或数据规则引用则拒绝。

### 4.3 新增角色数据规则

整体设置只替换当前操作者可管理的来源层：平台入口写 `platform_role_data_rule`；租户入口写 `tenant_role_data_rule_private`，不得删除或改写平台默认层。

| Method | 路径 | 操作码 |
|---|---|---|
| GET | `/v1/platform/config/role/{id}/data-rules` | `platform:config:role:data-rule:query` |
| PUT | `/v1/platform/config/role/{id}/data-rules` | `platform:config:role:data-rule:set` |
| GET | `/v1/org/role/{id}/data-rules` | `org:contacts:role:data-rule:query` |
| PUT | `/v1/org/role/{id}/data-rules` | `org:contacts:role:data-rule:set` |

`RoleDataRuleItemDTO`：`permissionId`、`resourceId`、`scopeType`、`scopes`（CUSTOM 为部门 ID 列表，其它类型为空数组）。`RoleDataRuleSetDTO.items` 为该来源层完整替换集。平台规则 `scopeType=CUSTOM` 拒绝租户部门 ID。

### 4.4 当前用户有效权限与内部快照

| Method | 路径 | 鉴权 | 返回 |
|---|---|---|---|
| GET | `/v1/auth/user/permissions` | 登录态；禁止 query 指定其它用户 | `UserEffectivePermissionVO`：`permissions`（启用具体权限码）、`version`、`generatedAt`、`expiresAt` |
| GET | `/v1/auth/user/menus` | 登录态 | 导航树，不含 Button；同源快照 |
| GET | `/v1/auth/user/info` | 登录态 | 不再内嵌可冒充业务授权的 authorities 集合 |
| POST | `/inner/authorization/snapshot` | `@Permit(INNER)` | `AuthorizationSnapshotDTO` |

`AuthorizationSnapshotDTO` 为具体类型，禁止直接序列化 `RoleType`：

- `tenantId`、`userId`
- `roleBindings`：`roleId`、`platformRole`、`roleCode`、`deptId`、`filterDept`
- `permissionCodes`：当前启用的具体权限码
- `resourceRules`：`resourceCode`、`permissionCode`、`scopeType`、`deptIds`、`self`
- `source`、`version`、`generatedAt`、`expiresAt`

内部调用只使用认证传递的租户/用户；请求体若带身份且与上下文不一致则 403。旧 `RemotePmsDataScopeService` 四个路径在消费点迁完后同次发布删除：

- `POST /inner/dataScope/role/roleListByCodes`
- `GET /inner/dataScope/dept/selfAndDescendantList/{deptId}`
- `GET /inner/dataScope/dept/userSelfAndDescendantDeptList/{userId}`（纠正 Feign 少 `dataScope` 段）
- `GET /inner/dataScope/dept/userDeptIds/{userId}`（纠正 Inner 多一层 `dataScope`）

### 4.5 DTO 字段冻结

- `AppCreateDTO` / `AppUpdateDTO`：增加 `defaultAccessMode`（`AppDefaultAccessModeEnum`，OPEN=`0` / CLOSED=`1`）。
- `AppMenuCreateDTO` / `AppMenuUpdateDTO`：增加 `permissionIds`（`List<Long>`）、`permissionMatchMode`（`PermissionMatchModeEnum`，ANY=`0` 默认 / ALL=`1`）。受保护页面（`accessMode=PERMISSION` 且 `menuType=Menu`）`permissionIds` 非空；Directory / OPEN 必须为空。不再从 path 生成权限。
- `AppPermissionCreateDTO`：保留 `pid/name/code/nodeType/remark/status`，增加可选 `resourceId`。`nodeType` 仅 GROUP/ACTION；GROUP `code` 必须以 `:**` 结尾。快捷新建即本接口。
- 菜单树节点：`permissionIds`、`permissionMatchMode` 替代 `permissionId`/`permissionCode`；用户菜单与配置菜单均不返回 Button。
- 权限树节点：`resourceId`、`nodeType`（仅 GROUP/ACTION）；不再返回 `type` / `managed` / `readOnly`。全量树 `GET /v1/platform/config/permission/tree` 与角色权限树同样不再返回 `type`，改返回 `nodeType`。

### 4.6 数据权限注解与写校验

`@DataScope` 必填 `resource`、`permission`（具体操作码）。表映射来自可信代码或 `ingot.mybatis.scope` 配置：`table`、`scopeColumn`（默认 `dept_id`）、`userColumn`（默认资源归属用户字段，不固定 `created_by`）。禁止把请求参数拼进 SQL 标识。

写归属校验：`DataScopeGuard.assertWritable(resource, permission, target)`，验证目标租户、用户/部门归属。列表/详情/更新/删除走同一 `(resource, permission)` 规则。旧空参 `@DataScope` 同次发布删除。

### 4.7 错误码与 HTTP 语义

管理接口继续使用 `AssertionChecker` + i18n，不新增平行 ErrorCode 枚举。运行时授权判定：

| 语义 | HTTP | i18n / 约定 |
|---|---|---|
| 认证无效 | 401 | 既有安全过滤器 |
| 明确无权限 | 403 | `AuthorizationDenied` |
| 授权服务不可用或过期刷新失败 | 503 | `AuthorizationSnapshot.Unavailable` |
| 受保护页未关联权限 | 400 | `ApplicationResourceServiceImpl.ProtectedMenuRequiresPermissions` |
| 权限仍被引用不可删 | 400 | `ApplicationResourceServiceImpl.PermissionInUse` |
| 部门角色重复绑定 | 400 | `TenantRoleUserPrivateServiceImpl.DuplicateDeptBinding` |
| 平台 CUSTOM 含租户部门 | 400 | `RoleDataRuleService.PlatformCustomTenantDeptForbidden` |

故障不得返回成功空业务结果。空授权不写热缓存。

## 5. 功能授权、数据范围及委派

### 功能判定

验证用户/成员/角色状态 → 按实际绑定读取默认及私有授权 → 匹配启用具体权限 → 按应用和租户授权过滤。租户管理员动态获得本租户有效应用内能力，不能穿透到平台运营域。所有入口使用同一判定，不单独排除“应用根权限 ID”冒充整个应用过滤。

### 数据判定

对 `(tenant, user, resource, permission)`：

1. 验证具体操作属于该资源且用户拥有操作权限；未登记或映射不符拒绝。
2. 保留所有授予该操作的角色绑定，按精确或通配授权找到该资源规则。
3. 部门角色以绑定部门为 DEPT/DEPT_AND_CHILD 根；非部门角色使用当前所属部门；SELF 对应资源的归属用户字段。
4. 合并为 `tenantCondition AND (selfCondition OR deptCondition ...)`；ALL 仅消去该资源的行范围条件。空规则/无有效部门/无角色不解释为 ALL。
5. 不同资源或操作重新计算。嵌套上下文栈保存并恢复；注册表缺少上下文拒绝；异步必须显式建立身份及授权边界，不能靠继承 ThreadLocal 放行。
6. SELECT/UPDATE/DELETE 覆盖 MyBatis 支持的入口，测试关联查询和分页总数；跨资源 SQL 未明确提供各资源授权上下文时拒绝，不将同一范围套到所有表。
7. INSERT 及归属变更显式校验；修改既有记录需先满足原记录操作范围，再验证新归属。批量写入逐项满足，不允许部分越权后静默成功。

租户管理员对本租户已登记有效资源拥有 ALL；其他角色按规则计算。框架不默认给未登记业务资源授权。

### 委派上限

授予角色时检查该角色在本租户的完整有效功能、数据规则和拟绑定部门；编辑角色授权时检查变更后结果。子管理员的拟授予范围须能证明为自身范围的子集；无法证明则拒绝。SELF 是相对主体范围，不能直接认定给另一用户的 SELF 是授予者 SELF 的子集；按接收用户和实际绑定部门校验。

仅有当前叶子全集不允许委派未来通配，须自身持有覆盖命名空间的通配资格。全部写入口在服务层执行检查，不能依赖前端选项过滤；事务内再次核验依据，防止失效权限作为持续授权依据。

## 6. 缓存、新鲜度与故障

- 使用 LayeredCacheBuilder，按 current 框架顺序装配 L1/L2/loader，不启用授予权限的 Resilient/LKG 放行；不手写替代缓存。配置归授权消费模块。
- 用户授权键包含租户/用户；角色聚合键包含租户/角色来源/角色 ID。编译索引使用 VersionedDerivedCache，键为 source/version。
- generatedAt 起算于源端一致性读取开始，不能从缓存回填时间起算；expiresAt 不超过 generatedAt + 30 秒，且不跨越最近 validFrom/validUntil 边界。源端旧实体缓存也须纳入期限或绕过，禁止旧数据重新打新时间戳。
- 每次使用和回填均校验绝对期限；回填只保存剩余 TTL，旧在途响应不能覆盖已知更新版本。L1 TTL 必须存在。跨节点时钟偏差通过保守提前过期处理并监控。
- 事务提交后清发送节点并广播；失效包含权限、角色、用户部门绑定、应用和租户覆盖。广播失败不能重新激活旧授权，期限提供最坏时效边界。
- 合法无权限正常拒绝，不当作远端异常；空授权不写热缓存。过期且刷新失败返回授权不可用（503），明确无权限为 403，认证无效为 401。均不返回成功空数据掩盖故障。
- 会话存储原有短时故障宽限不延长业务授权期限。没有有效授权快照时即使身份处于宽限也不能执行受保护业务。
- 30 秒界限约束后续授权判定，不承诺强制中断已经通过授权并执行中的长事务。
- 记录授权写审计、失效失败、快照年龄、拒绝和 RPC 故障指标；不记录完整敏感业务数据。

## 7. 一次性迁移与回滚

1. 清点数据库实际 schema、菜单及权限、角色关系、注解/API 消费点、会话字段与前端契约。代码与 current 的差异以实际盘点记录为依据。
2. 建立版本化迁移映射清单：旧具体权限保留 ID/code；旧精确 NAVIGATION 转为独立具体访问能力；旧通配导航转 GROUP；页面入口优先复用 `{ns}:query` 或已有 `{ns}:view`；纯目录不需要新叶子。映射完成后删除 `platform_menu.permission_id`。
3. 旧 `:*` 转等价 `:**`，处理重复编码并保留全部引用。view 命名碰撞、无 app_id、来源不明、悬空关系、跨租户部门和重复任职进入阻断清单，不猜测修复。
4. 旧 Button 行迁移为独立操作权限，保留其权限绑定，移除伪路由。通配页面入口复用已有 `{ns}:query` 视为已确认的 read 引用，不另造 `:view`。
5. 旧角色范围迁移到实际授权的已登记资源规则，保留语义；平台 CUSTOM 若含具体租户部门需显式拆成对应租户规则，无法确定归属时阻断。未登记资源本次不自动套用行过滤。
6. 暂停授权写入并备份，完成同步数据库与服务升级、缓存命名空间切换；清理旧 OAuth 授权及在线会话，使旧 AT/RT 均失效。BFF 会话按原有 401 重登机制处理，不越权直接修改独立会话存储。
7. 前端菜单/权限契约联调和示例端到端通过后开放流量，不允许新旧服务混跑。
8. 回滚整体恢复数据库与服务版本，清理新会话及缓存后重登。不得把旧凭证重新恢复有效；故障期间新增写入须在恢复前冻结并核对。

发布前必须有迁移前后有效授权对照、预计变化说明、阻断清单清零和恢复演练证据。操作脚本需 dry-run 能力；生成 Spec 阶段不执行迁移。

## 8. 测试与验收

以 REQUIREMENTS 的 A01—A14 为验收索引，覆盖单元、数据库/HTTP 集成、多节点时间边界、迁移和回滚演练。资源示例至少覆盖读、审核、更新、删除、创建及归属变更；使用可控时钟测试 30 秒和应用授权边界，不靠人工等待判断。

实现阶段遵循仓库常量/枚举、JavaDoc、缓存门禁。任何新增设计偏离先回写本设计并重新确认；验收前不更新 current。

## 9. T0 盘点结论（实施依据）

代码与 current 的差异以本节约束；不得把 current 中“已统一 / 已删除列”当作实施完成证明。

| 现状 | 目标 |
|---|---|
| 权限码由 `BizMenuUtils.getMenuAuthorityCode` 从 path 派生；菜单托管 NAVIGATION，`source_id` 随菜单生命周期 | 权限编码独立；菜单只保存可见性关联；删除菜单不删权限 |
| `platform_menu.permission_id` 单关联；Button=`menu_type=9` 伪路由 | `platform_menu_permission` 多对多；ANY/ALL；树不返回 Button |
| `tenant_role_user_private.dept_id` 已有，绑定去重仅 `roleId+userId`，无唯一索引 | 唯一键含部门；空部门用生成列哨兵 `0`；业务参数仍用 null |
| 菜单走 `ApplicationAuthorizationResolver`，登录走 `IdentityUtil.getScopes` 写入 `OnlineToken.authorities` | 统一解析器；会话不再合并旧业务权限集合；JWT 仍瘦身 |
| `@DataScope` 无属性；角色级 `scope_type`；DEPT_AND_CHILD 按用户全部部门展开 | 按 `(resource, permission)`；部门角色用绑定部门 |
| PMS 用 `@Cacheable`，未用 `LayeredCacheBuilder` | 授权快照按分层缓存接入，无 Resilient/LKG 放行 |
| `tenant_app_config` 无记录默认不可用（current）；无 `default_access_mode` | 默认 OPEN；CLOSED 为申请制；显式覆盖不因到期回退 |
| 无资源目录、无按资源数据规则表 | 新增 `platform_resource` 与两套 data_rule 表 |
| 仓库无订单/公告示例，仅 `ingot-test` 的 `t_student` | T6 新增示例资源闭环 |
| Feign/Inner DataScope 路径不一致 | 随旧 RPC 删除一并消失，不单独兼容 |
| `platform_app.menu_id`/`permission_id` 仍在 DDL 与实体中（归档 DROP 未落地） | 保留 `permission_id` 作为应用根 GROUP 锚点；`menu_id` 本 change 不使用，不在本次 DROP |

空部门哨兵：生成列 `dept_id_uk = IFNULL(dept_id, 0)`。`0` 禁止作为真实部门 ID（部门主键为雪花 ID）。

GROUP 通配统一 `:**`。迁移将旧 `:*` 转为 `:**`；创建接口不再接受 `:*`。`PermissionMatcher` 在迁移完成前仍识别 `:*`，以免对照期误判。

## 10. 迁移映射清单格式

操作脚本：`databases/migrations/022_pms_rbac_data_authorization.sql`（schema）、`022_pms_rbac_data_authorization_data.sql`（数据，支持 dry-run 开关）、`023_pms_menu_permission_finalize.sql`（通配页关联叶子并 DROP `platform_menu.permission_id`）、`024_pms_permission_drop_legacy_columns.sql`（收口 NAVIGATION 并 DROP `managed`/`source_type`/`source_id`）、`025_pms_permission_drop_type.sql`（DROP `platform_permission.type`）、`rollback_022_*.sql` / `rollback_023_*.sql` / `rollback_024_*.sql` / `rollback_025_*.sql`。Java 规则与报告：`AuthorizationMigrationAnalyzer`，禁止猜测修复。

### 10.1 阻断类别

| code | 含义 |
|---|---|
| `DUPLICATE_DEPT_BINDING` | 同一 tenant/user/platform_role/role_id/规范化部门 多行 |
| `VIEW_CODE_COLLISION` | 通配页无 `{ns}:query` 可复用，且需创建 `{ns}:view` 但编码已存在且不是可复用 ACTION |
| `MISSING_APP_ID` | 菜单或权限无 `app_id` |
| `UNKNOWN_SOURCE` | 迁移快照中菜单旧 `permission_id` 悬空 |
| `DANGLING_RELATION` | 角色权限/数据规则引用不存在的权限、资源或部门 |
| `CROSS_TENANT_DEPT` | 任职或 CUSTOM scopes 指向其它租户部门 |
| `PLATFORM_CUSTOM_TENANT_DEPT` | 平台默认规则 CUSTOM 含具体租户部门，无法拆到唯一租户 |
| `WILDCARD_CODE_COLLISION` | `:*` 转 `:**` 后与已有编码冲突 |

有任一条阻断则 apply 拒绝。dry-run 只输出报告。

### 10.2 映射规则（可执行，非猜测）

1. 保留全部现有 `platform_permission.id` 与 `code`（通配改写除外）；角色绑定按 permission_id 保留。
2. 精确 NAVIGATION（非 `:**`/`:*`）：`node_type` 改为 ACTION，`managed=false`，`source_type=MANUAL`，清空 `source_id`。若原菜单为 `menu_type=Menu` 且 `access_mode=PERMISSION`，写入 `platform_menu_permission`。
3. 通配 NAVIGATION（`:**` 或 `:*`）：改为 GROUP，code 统一 `:**`。纯 Directory 不建 view、不写关联。页面菜单若需要入口：优先关联同命名空间已有 ACTION `{ns}:query`；否则已有 `{ns}:view`；否则报告待人工处理，不猜测新建。已有 `{ns}:view` 且不是可复用 ACTION 时阻断。应用根 `{appCode}:**` 保持 GROUP，不新建 view。映射完成后删除 `platform_menu.permission_id`。
4. Button 行：对应权限改为 ACTION 并保留绑定；删除菜单行，不写入可见性关联。
5. OPEN 菜单：不写可见性关联，即使旧 `permission_id` 非空。
6. 旧 `:*`：等价改为 `:**`；冲突阻断。
7. 角色级 `scope_type`/`scopes` 仅在已登记资源上生成规则；未登记资源不自动套过滤。平台 CUSTOM 含租户部门：能唯一归属则写入该租户私有规则，否则阻断。本 change 不为 PMS 用户/部门/角色表注册行过滤。
8. 既有应用 `default_access_mode=OPEN`。`tenant_app_config` 增加 `(tenant_id, app_id)` 唯一约束。
9. 新增管理操作权限种子（独立 ACTION，挂应用根 GROUP）：`platform:config:app:resource:query|create|update|delete`、`platform:config:role:data-rule:query|set`、`org:contacts:role:data-rule:query|set`。持有对应 `:**` 的角色动态获得，无需再绑叶子。
10. 映射完成后删除 `platform_permission.managed` / `source_type` / `source_id` / `type` 及 `idx_permission_source`。残留 `node_type=NAVIGATION` 按第 2、3 条再收口一次。应用根只认 `platform_app.permission_id`。`PermissionNodeTypeEnum` 删除 `NAVIGATION`；删除 `PermissionSourceTypeEnum`。旧 `type`（菜单权限/API 权限）不再作为授权事实，节点形态只认 `node_type`。

### 10.3 授权前后对照口径

对照键：`(tenant_id, user_id, permission_code)` 与 `(tenant_id, user_id, resource_code, permission_code)`。

允许差异（需在发布说明列出）：Button 不再出现在菜单树；Directory 不再作为可授予的 NAVIGATION；页面通配 NAVIGATION 改为 GROUP，可见性复用已有 `{ns}:query` 或 `{ns}:view`；默认开放使无 `tenant_app_config` 的租户应用变为可用（相对 current「无配置不可用」）。其它有效具体权限增减均视为未批准变化，阻断发布。
