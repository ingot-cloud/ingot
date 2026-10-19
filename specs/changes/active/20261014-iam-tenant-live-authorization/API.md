# API：租户当前角色授权

> 目标契约，状态随本 change 评审；尚未实现、未生成 OpenAPI。后端文件为权威，前端 API.md 按字节复制，复制来源和 SHA-256 在前端 SOURCES.md 记录。

## 1. 服务、类型及兼容范围

- 服务：IAM；控制器路径下表使用 `/v1`，浏览器前缀沿用 `/api/iam`。
- 可信身份：TENANT 必须有当前 tenantId/memberId/accountId；PLATFORM 不带 tenantId。请求不允许选择任意租户或提交拥有者资格。
- 包装沿用 `R<T>`；详情 `ResourceDetail<T>={record,fieldAccess,capabilities,version}`；页 `PageResponse<T>={items,total,page,pageSize}`。ID、锁及视图指纹为字符串，分页/计数字段沿用既有计数序列化约定，不重新定义。
- page 缺省 1，pageSize 缺省 20、最大 200；先按权限边界过滤再计数及分页。选择器采用分页与已选 ID 回显，不拿大页模拟全集。
- 时间点输入带时区 ISO-8601，响应 UTC Z；validFrom 包含、validUntil 不包含。Duration 使用 ISO-8601；日期、Cron、OAuth/JWT 数字时间不改变。
- 配置 `version/expectedVersion` 是不展示的并发锁，bootstrap `version` 是权限同步指纹；不等于角色业务版本。
- 新 Tenant/Shared-current DTO 与平台固定版本 DTO 分离。平台自有 `/v1/platform/roles`、平台分配/委派和 role-applications 契约不转换为本模型。

## 2. 操作码与资格

| 操作码 | 覆盖 |
|---|---|
| iam-tenant:role:read/create/status/delete/preview | 私有角色目录、创建、元数据/启停、删除、预览；既有码保留 |
| iam-tenant:role:update（新语义） | 保存私有角色当前权限；由旧 role:publish 原 ACTION ID 改名，不重建授权引用 |
| iam-platform:shared-role:read/create/status/delete/preview | 共享角色对应管理行为；既有码保留 |
| iam-platform:shared-role:update（新语义） | 保存共享角色当前权限；旧 shared-role:publish 原 ID 改名 |
| iam-tenant:assignment:read/create/update/delete | 直接治理或有效委派提供的受限分配入口 |
| iam-tenant:delegation:read/create/update/delete/preview | 委派治理；只凭委派不获得这些操作 |
| iam-tenant:settings:owner-transfer | 额外要求当前真实拥有者，不能仅凭同码角色授权执行 |

这里的斜杠仅为表格缩写，每个操作都是精确独立 ACTION。角色编辑和委派管理要求独立治理资格；来自委派的治理 ACTION 不满足。拥有者动态获得有效开通操作，但转交仍核对身份。角色 read 仅允许查看，不授予写资格。

旧 `iam-tenant:role:upgrade` 退休；历史操作码/审计保留说明。迁移保留旧 publish ACTION ID 与原字段能力、菜单关联，改码后更新生成来源与常量；平台自有角色授权仍引用该同一 ACTION ID，固定版本内容不改。旧升级菜单/关联在迁移中退出，不能指向新 update 扩权。

## 3. 角色契约

### 3.1 输入

| 类型 | 字段与校验 |
|---|---|
| CurrentRoleCreateInput | `{code,name,description?,groupName?,definition}`；code 域/组织内唯一且创建后不可修改；端点决定 SHARED/TENANT_CUSTOM，不接受 kind=SYSTEM |
| CurrentRoleProfileInput | `{expectedVersion,name?,description?,groupName?,status}`；status 为 ENABLED/DISABLED；角色元数据不得带权限或来源字段 |
| CurrentRoleDefinition | `{grants,parameterDefinitions}`；两集合必填，可空；没有 deltas/baseRevisionId/sourceRevisionId/versionName/remark/resourceFieldPermissions |
| CurrentRoleGrant | `{actionId,scopes:ScopeExpression[]}`；操作唯一，目录/域/资源及能力必须合法；空 scopes 不产生对象范围 |
| CurrentRoleParameter | `{key,kind,resourceId}`；kind=DEPARTMENTS/OBJECTS；唯一且被对应资源操作引用，旧 key 不得换资源或类型 |
| CurrentRolePermissionInput | `{expectedVersion,definition}`；同事务保存完整当前定义，不提供应用版本或分配筛选 |

ScopeExpression 沿用 `{kind,parameterKey?,includeDescendants?}`：ALL/SELF 不带参数，MEMBER_DEPARTMENTS 可声明下级，MANAGED_DEPARTMENTS 必须绑定部门参数，OBJECT_SET 必须绑定对象参数。ScopeBinding 沿用 `{kind,ids:string[]}`，空集合不授予对象。每个实际操作的参数必须能证明属于对应 resourceId，候选目录返回可用稳定键。

私有角色不配置普通角色字段权限，继续使用租户字段策略。共享角色权限草稿也不接受平台自有角色字段配置；拥有者字段全权由服务器推导。

### 3.2 输出

- `CurrentRoleSummary`：`{id,code,name,description?,groupName?,kind,status}`，kind 仅 SHARED/TENANT_CUSTOM；不返回权限版本/基础版本。详情 capabilities 对共享角色租户写操作为拒绝。
- `CurrentRoleGrantList`：`{items:CurrentRoleCatalogAction[],definition:CurrentRoleDefinition}`；一次返回该角色完整当前已选权限，目录内容含停用状态，用于回显。无 revision 摘要。
- `CurrentRolePermissionResult`：`{roleId,version,scopeIssueAssignments}`；计数仅可披露范围内，受限时省略并保留 Preview 的 restricted 说明。version 为更新后的配置锁，不为版本名称。
- `CurrentRolePreviewResult`：`{definition,addedActionIds,removedActionIds,changedScopeActionIds,scopeIssueAssignments}`；由 `Preview<T>={version,valid,errors,warnings,impactSummary,effectiveResult?}` 包装。
- `CurrentRoleCatalogAction` 至少含 `actionId,actionCode,actionName,applicationId,applicationName,resourceId,resourceName,enabled,scopeCapabilities,parameterDefinitions`；私有角色候选仅当前租户有效开通且启用的可编辑目录。旧定义中停用操作可回显，不能新选。

### 3.3 路由

`{rolePath}` 在此表仅表示两个明确控制器：`/v1/tenant/roles` 或 `/v1/platform/shared-roles`，不是运行时任意域路由。

| 方法与路径 | 请求 | data | 资格与边界 |
|---|---|---|---|
| GET {rolePath} | page/pageSize/name?/status?/kind? | PageResponse<ResourceDetail<CurrentRoleSummary>> | 租户可见共享和本地角色；平台仅 SHARED；kind 白名单 |
| POST {rolePath} | CurrentRoleCreateInput | CreatedResource{id,version} | tenant 固定创建私有；platform 固定共享，创建首个当前完整快照 |
| POST {rolePath}/preview | CurrentRoleCreateInput | Preview<CurrentRolePreviewResult> | CREATE 与独立治理；只预览创建草稿，不分配角色/快照 ID，提交时重新校验目录 |
| GET {rolePath}/{id} | id | ResourceDetail<CurrentRoleSummary> | 角色 READ；跨组织私有返回 404 |
| PATCH {rolePath}/{id} | CurrentRoleProfileInput | ResourceDetail<CurrentRoleSummary> | STATUS 与独立治理；租户只能写自己的私有角色 |
| DELETE {rolePath}/{id} | id | CreatedResource | DELETE 与独立治理；分配/委派等实际引用拒绝物理删除，历史审计保留不抹除 |
| GET {rolePath}/{id}/grants | id | CurrentRoleGrantList | READ；服务器读当前定义，不接受 revisionId |
| POST {rolePath}/{id}/preview | CurrentRolePermissionInput | Preview<CurrentRolePreviewResult> | PREVIEW 与独立治理；校验当前锁及影响，warnings 不伪造 errors |
| PUT {rolePath}/{id}/grants | CurrentRolePermissionInput | CurrentRolePermissionResult | UPDATE 与独立治理；提交重验，修改当前权限并自动生效 |
| GET /v1/tenant/roles/grant-catalog | applicationId,page/pageSize,keyword?,ids? | PageResponse<CurrentRoleCatalogAction> | CREATE/UPDATE/PREVIEW 任一独立资格；已选回显不绕过资源边界 |

平台共享角色的候选目录继续复用平台已授权的应用/资源/ACTION 目录接口；不新增跨平台读取捷径。预览影响摘要不能枚举其他租户私有成员资料，平台仅按其组织管理可见边界披露计数；隐藏部分 restricted=true。

创建预览的 version 是本次目录依赖指纹，不是已有角色锁；创建提交仍使用 CurrentRoleCreateInput，不携带或信任预览作为授权证明。已有角色预览使用 CurrentRolePermissionInput.expectedVersion。两类预览都不要求客户端处理业务版本。

## 4. 租户分配

### 4.1 类型

`TenantAssignmentInput={subject:{type:MEMBER|GROUP,id},roleId,scopeBindings,validFrom?,validUntil?,delegationGrantId?}`。subject.id 为成员或组 ID，不是账号 ID；scopeBindings 必填对象。缺省 validFrom 使用提交瞬时，validUntil 为空表示直接长期分配；来源委派仍受有限时长和来源到期约束。

`TenantAssignmentBatchInput={items:TenantAssignmentInput[]}`，非空、原子；仅凭委派时同次批次只能来自一条本人委派。`TenantAssignmentUpdateInput={expectedVersion,assignment:TenantAssignmentInput}`。

`TenantAssignmentRecord={id,assignment,status,source,subjectName?,roleName?,roleKind,delegationSummary?,createdAt?,grantedBy?,effectiveStatus,scopeIssues}`，保留现有 ACTIVE/REVOKED 与计算状态，不含 revisionNumber/versionName/roleRevisionRef。`scopeIssues` 是当前定义下计算的待配置原因，不是持久分配状态。

`ScopeIssue={resourceId,actionIds,parameterKey?,reason}`，reason 只允许 MISSING_BINDING/INVALID_BINDING/MISSING_RESOURCE_CEILING；不包含范围外对象资料。资源/操作不可披露时不返回其明细，仅给出受限提示。

### 4.2 路由

| 方法与路径 | 请求 | data |
|---|---|---|
| GET /v1/tenant/assignments | page/pageSize；subjectType?/keyword?/effectiveStatus? | PageResponse<ResourceDetail<TenantAssignmentRecord>>；权限过滤在分页前 |
| POST /v1/tenant/assignments | TenantAssignmentBatchInput | CreatedResource；任一输入失败整体回滚 |
| POST /v1/tenant/assignments/preview | TenantAssignmentBatchInput | Preview<AssignmentPreviewResult>；逐主体解释当前权限与范围 |
| PUT /v1/tenant/assignments/{id} | TenantAssignmentUpdateInput | ResourceDetail<TenantAssignmentRecord> |
| DELETE /v1/tenant/assignments/{id} | id | CreatedResource；撤销并保留审计 |
| GET /v1/tenant/assignments/options | kind=ROLE/MEMBER/GROUP/RESOURCE/APPLICATION/DEPARTMENT；delegationGrantId?；keyword?/ids?/page/pageSize | PageResponse<TenantAssignmentOption> |
| GET /v1/tenant/assignments/roles/{id} | delegationGrantId? | CurrentRoleGrantList；只返回当前租户已开通及允许披露的定义资料，不补通用 ROLE_READ |
| GET /v1/tenant/assignments/scope-candidates | roleId,parameterKey；delegationGrantId?；keyword?/ids?/page/pageSize/tree?/parentId? | AuthorizationCandidatePage；不再接受 revisionId |

TenantAssignmentOption 为 `{id,name,kind,roleKind?}`，kind 为上述六值；ROLE 的 id 是角色 ID。直接治理者在本租户边界内查询；受限管理员必须传一条本人有效 delegationGrantId，角色/人员/组/资源/范围都取该来源限制。已选回显和总数使用相同过滤，不接受旧 ID 越界回显。组写入校验其全部展开成员；空组不生成有效派生授权。

编辑新分配时缺少当前必填绑定返回 400；存量角色新增资源形成 scopeIssues 时保留原有效操作，普通编辑补齐当前全部必要参数。不得自动填充 ALL 或从其他分配拷贝范围。

## 5. 租户委派

`TenantDelegationInput={administratorMemberId,allowedRoleIds:string[],recipientSelection:Selection,resourceScopeCeilings:ResourceScopeCeiling[],validFrom?,validUntil?,maxAssignmentDuration,assignmentDurationMode?}`。

- `Selection={members:string[],departments:{id,includeDescendants}[]}` 沿用当前语义，缺省集合为空；必须有有效可接收人群，禁止跨组织/自授权。
- `ResourceScopeCeiling={resourceId,scopes:ScopeExpression[],scopeBindings:Map<string,ScopeBinding>}`；resourceId 唯一；参数在此委派内绑定并校验实际对象归属。
- allowedRoleIds 非空且不重复，只允许共享/本地私有；新建委派须为当前白名单涉及的每个可执行资源补齐上限。后续角色增加新资源可使旧委派产生待补提示，新增操作不需逐操作上限。
- assignmentDurationMode 缺省 LIMITED，租户不接受 UNLIMITED；maxAssignmentDuration 为正 ISO-8601 Duration。派生分配结束时间必填且不超过允许时长；执行始终受来源有效期约束。

`TenantDelegationRecord={id,delegation:TenantDelegationInput,status,administratorName?,scopeIssues}`。Update 为 `{expectedVersion,delegation:TenantDelegationInput}`。删除是撤销，不清历史。

| 方法与路径 | 请求 | data / 资格 |
|---|---|---|
| GET /v1/tenant/delegations | page/pageSize,administratorName? | PageResponse<ResourceDetail<TenantDelegationRecord>>；DELEGATION_READ 与独立治理 |
| POST /v1/tenant/delegations | TenantDelegationInput | CreatedResource；CREATE 与独立治理 |
| GET /v1/tenant/delegations/{id} | id | ResourceDetail<TenantDelegationRecord>；READ 与独立治理 |
| PUT /v1/tenant/delegations/{id} | TenantDelegationUpdateInput | ResourceDetail<TenantDelegationRecord>；UPDATE 与独立治理 |
| DELETE /v1/tenant/delegations/{id} | id | CreatedResource；DELETE 与独立治理 |
| POST /v1/tenant/delegations/{id}/preview | TenantDelegationUpdateInput | Preview<ReferenceImpactPreview>；PREVIEW 与独立治理 |
| GET /v1/tenant/delegations/mine | 无可替代成员/租户参数 | 当前本人有效委派的受限摘要列表；仅凭当前有效身份，严格按本人过滤 |
| GET /v1/tenant/delegations/scope-candidates | resourceId,parameterKey,keyword?/ids?/page/pageSize | AuthorizationCandidatePage；委派 CREATE/UPDATE 独立资格，实际资源范围受治理权约束 |

mine 返回 `{id,allowedRoles:[{id,name,kind}],recipientSummary,validFrom?,validUntil?,maxAssignmentDuration,scopeIssues}`，无其他管理员或范围外成员资料。失效委派不作为入口资格；可用截止参与会话 expiresAt。委派链的状态、期限、角色/资源、接收者和组变化在执行时持续校验。

## 6. 会话、拥有者、诊断与资源契约

- `/v1/me/bootstrap` 与 `/v1/me/capabilities` 保持现有包装、actionCodes/version/expiresAt；bootstrap 增加 `tenantOwner:boolean`（平台恒 false），仅用于身份展示。前端仍按服务器 actionCodes/capabilities 判断按钮，不能以该标志 bypass。
- `/v1/tenant/settings` 继续返回 ownerMemberId 及显示名，配置 version 为并发锁。`POST /v1/tenant/settings/owner-transfer` 输入 `{expectedVersion,newOwnerMemberId}`，返回 CreatedResource；只允许当前拥有者和同组织有效目标。创建组织 ownerAccountId 输入保持。
- 应用和菜单读取拥有者按有效开通过滤，普通成员按人群及权限过滤；期限不延长。共有的应用/ACTION/资源目录仍逐域校验。
- 诊断保留 `/v1/tenant/authorization/diagnose`；租户来源输出角色 ID/名称及委派资源限制，拥有者解释为当前组织拥有者，不返回角色业务版本或伪造 assignmentId。历史审计可保留当时 revisionId，仅在历史说明中使用，不能拿去分配。
- 资源授权 v2 请求与 ActionDecision 包装保持；拥有者返回本租户 ALL/FULL 的真实范围与字段条款，platformAdministrator 恒为平台专用；未注册资源拒绝，不允许客户端提交 tenantOwner。

## 7. 校验、错误与退出

| HTTP / code | 情况 |
|---|---|
| 400 / InvalidArgument | 不合法范围/域/重复 ID/旧版本输入字段/不带时区时间/租户 UNLIMITED/新分配缺参数 |
| 401 / IdentityInvalid | 账号、成员、组织或当前上下文无效 |
| 403 / ActionDenied、DataScopeDenied、DelegationExceeded | 缺操作/目标越界/来源委派越界；错误不能披露不可见对象 |
| 403 / RoleUnavailable（新增） | 已归属可见但停用的当前角色；平台旧 RoleRevisionUnavailable 继续使用 |
| 404 / ObjectNotFound | 不存在或不可见的角色/成员/分配/委派 |
| 409 / RevisionConflict | 技术 expectedVersion 与当前锁冲突；提示重新读取及预览，不显示业务版本 |
| 400 / ObjectInUse | 被引用角色删除 |
| 410 / EndpointRetired（新增） | 已认证请求访问退出的租户/共享业务版本或升级路由 |
| 503 / AuthorizationUnavailable | 当前权限、目录或约束无法可靠加载；不使用旧允许结论 |

新增错误及 ScopeIssue reason 使用 commons 命名枚举，遵循 EnumUtils 契约；本轮只定义目标，不修改枚举。仍要求必须改密的账号使用现有 PasswordChangeRequired 限制。

退出 TENANT 和平台 SHARED 的 `GET/POST /{id}/revisions`、固定版本详情/metadata/grants、tenant upgrade-preview/upgrade，以及 sourceRevisionId 查询参数。入口仅保留认证后的明确 410 提示，不读取或重解释旧版本 ID；未认证仍返回认证错误。平台自有版本路由及批量应用不退出。

上述新角色、分配、委派 DTO 必须显式拒绝 `roleRevisionRef/allowedRoleRevisionRefs/baseRevisionId/deltas/versionName/sourceRevisionId` 等退出字段，不能依赖 JSON 忽略未知字段而静默改变含义。配置 expectedVersion 与并发错误 code 名称保留。

## 8. 例子

保存私有角色当前权限：

```json
{"expectedVersion":"4","definition":{"grants":[{"actionId":"120100","scopes":[{"kind":"MANAGED_DEPARTMENTS","parameterKey":"member-managed-departments","includeDescendants":true}]}],"parameterDefinitions":[{"key":"member-managed-departments","kind":"DEPARTMENTS","resourceId":"110100"}]}}
```

直接分配角色（示例 ID 仅用于说明，必须由服务器实际目录和候选取得）：

```json
{"items":[{"subject":{"type":"MEMBER","id":"501"},"roleId":"140100","scopeBindings":{"member-managed-departments":{"kind":"DEPARTMENTS","ids":["601"]}},"validFrom":"2026-10-14T08:00:00Z"}]}
```

委派定义：

```json
{"administratorMemberId":"502","allowedRoleIds":["140100"],"recipientSelection":{"members":[],"departments":[{"id":"601","includeDescendants":true}]},"resourceScopeCeilings":[{"resourceId":"110100","scopes":[{"kind":"MANAGED_DEPARTMENTS","parameterKey":"member-managed-departments","includeDescendants":true}],"scopeBindings":{"member-managed-departments":{"kind":"DEPARTMENTS","ids":["601"]}}}],"validFrom":"2026-10-14T08:00:00Z","validUntil":"2026-11-14T08:00:00Z","maxAssignmentDuration":"P7D","assignmentDurationMode":"LIMITED"}
```

前端 API 副本不得单独修改字段。实施时 Java DTO/控制器生成 routes/schema/OpenAPI/internal contract 并对照本工件；发现偏离先修改权威 Spec 并重新确认，不能先修改副本或运行契约。

## 实施细化：候选依赖

`assignments/options.kind` 另提供 `APPLICATION` 与 `DEPARTMENT`，用于当前角色应用选择和独立委派接收部门；分页、搜索和 ids 回显复用同一接口。APPLICATION 仅本组织有效开通应用，DEPARTMENT 仅本组织部门。当前角色创建/编辑/预览或独立委派资格可使用相应依赖选择器，不额外要求应用管理/通讯录读取资格；受限委派来源不得使用这两种独立候选。受限部门参数按来源范围平铺，避免因不可见祖先无法选择获准部门。

本人委派摘要增加 `directAssignmentActions:string[]`（独立分配 CREATE/UPDATE 精确码），前端据此提供直接治理来源；仅凭委派时自动选择本人来源。委派派生角色的角色写入、分配和委派治理码不作为独立入口返回，分配入口只由本人有效委派注入；业务操作及获准的角色读取保持原数据范围。
