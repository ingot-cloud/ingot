# IAM 接口与前端契约

> 目标契约，主 change 已进入 implementing。管理面请求/响应类型已闭合，完整端点 OpenAPI 已生成但运行时未实现，不能据此调用线上接口。实现导出的 OpenAPI 必须与此一致；实体完整字段在对应业务 DTO 中显式声明，不直接透传 ORM 实体。

## 1. 通用约定

外部前缀 /api/iam，下表路径相对该前缀。服务路由为 /v1/...。沿用仓库 R<T> 成功/错误信封；分页 data 统一为 { items, total, page, pageSize }。ID/版本为字符串，时间 ISO-8601 UTC，集合返回 [] 而非 null。

读取不允许以 tenantId query 切换租户。上下文切换通过现有认证流程重新建立身份，再 bootstrap。平台租户 path 参数只授权管理租户实体，不构成目标租户会话。

状态与种类使用语义枚举：PLATFORM/TENANT、MEMBER/GROUP、SYSTEM/SHARED/PLATFORM_CUSTOM/TENANT_CUSTOM、ADD/REMOVE/REPLACE_SCOPE、HIDDEN/MASKED/FULL。PLATFORM_CUSTOM 补齐本契约已有的平台自定义角色接口类型，不改变平台与租户隔离规则。开始时间包含、结束时间不包含。

集合 GET 支持分页及该资源明确定义的过滤项，不能接受任意字段名/SQL 排序。POST 创建返回 { id, version }；PUT 配置整体替换并传 expectedVersion；PATCH 状态显式 { status, expectedVersion }。删除被引用对象返回稳定 InUse 错误。

## 2. 主要 DTO

| 类型 | 关键字段及语义 |
|---|---|
| AuthorizationContext | domain, tenantId?, accountId, memberId；均由可信身份得到；PLATFORM 使用平台 memberId 且 tenantId 为空，TENANT 的 tenantId 必填并使用本租户 memberId |
| Bootstrap | context, profile, applications, menus, actionCodes, version, expiresAt；同一有效授权视图，不含全租户授权快照 |
| SubjectRef | type: MEMBER/GROUP, id；在当前 domain 内解析平台成员/平台组或租户成员/租户组，不混用 accountId，不接受跨域引用 |
| Selection | members[], departments[{id, includeDescendants}]；仅当前域合法引用，后台按操作返回候选 |
| ScopeExpression | kind: ALL/SELF/MEMBER_DEPARTMENTS/MANAGED_DEPARTMENTS/OBJECT_SET, parameterKey?, includeDescendants?；资源验证合法组合 |
| ActionGrant | actionId, scopes: ScopeExpression[]；范围并集，不能引用未选操作 |
| RoleRevision | id, roleId, revision, kind, baseRevisionId?, grants(完整角色), deltas(定制角色), parameterDefinitions, metadataOverrides |
| RoleDelta | actionId, operation, scopes?；仅新增/替换携带范围 |
| EffectiveRole | role/revision、合成 grants、逐操作 origin(BASE/ADDED/REMOVED/REPLACED)、参数定义、使用中的授权统计 |
| AssignmentInput | subject, roleRevisionRef{kind,id}, scopeBindings(命名参数到类型化 ID 集合), validFrom?, validUntil?, delegationGrantId? |
| DelegationInput | administratorMemberId, allowedRoleRevisionRefs[], recipientSelection, actionScopeCeilings[], validFrom?, validUntil?, maxAssignmentDuration |
| FieldAccess | 字段名到 { visibility, editable }；业务 data 只返回允许值 |
| ObjectCapabilities | actionCode 到 { allowed, reasonCode?, message? }；后端批量计算 |
| Decision | allowed, reasonCode, message, sources[], scopeSummary, fieldAccess?, version, expiresAt；来源受诊断权限限制 |
| Preview | version, valid, errors[], warnings[], impactSummary, effectiveResult；不写入任何授权 |
| UpgradePreview | oldBaseRevisionId, newBaseRevisionId, changes[], conflicts[], effectiveResult, affectedAssignments；冲突有稳定 key |
| AuditEntry | actor, context, target, changeType, before/after 安全差异, revisions, timestamp, traceId |

ScopeBindings 值使用 { kind: DEPARTMENTS/OBJECTS, ids[] }，不接收任意表达式；部门是否包含下级来自角色规则。范围/字段候选来源为资源目录能力，前端不能硬编码所有资源均有 SELF 或部门选项。

T01 已落地范围基础类型：`ingot-commons` 中的 `com.ingot.framework.commons.model.iam`。ScopeExpression、ScopeBinding、ActionGrant、RoleDelta 使用本节字段；必填集合缺失由 Bean Validation 拒绝，RoleDelta 的 REMOVE 操作省略 scopes 时输出空数组。当前已验证管理请求/响应 JSON 契约与目标端点 OpenAPI；运行时执行器和业务校验尚未完成，不作为前端联调已就绪的依据。

T01 补充字段精确定义：RoleParameterDefinition 为 `{key,kind}`，kind 使用 DEPARTMENTS/OBJECTS；RoleMetadataOverrides 为 `{name?,description?,groupName?}`。ActionScopeCeiling 为 `{actionId,scopes,scopeBindings}`，FieldRule 也携带 scopeBindings 绑定目标范围参数。maxAssignmentDuration 为正的 ISO-8601 duration 字符串，如 `PT24H`。FieldRule 的 scenario 为 MANAGEMENT/DIRECTORY。新增/替换差异必须显式携带 scopes（允许空数组），只有 REMOVE 可以省略并规范化为空数组。

范围结构约束：ALL/SELF 不携带 parameterKey 或 includeDescendants；MEMBER_DEPARTMENTS 不携带参数；MANAGED_DEPARTMENTS/OBJECT_SET 必须有非空 parameterKey，OBJECT_SET 不携带 includeDescendants。部门范围未指定 includeDescendants 时按 false；资源自身能力及绑定归属仍须业务校验。起止时间同时存在时必须 validFrom < validUntil。

详情数据返回 { record, fieldAccess, capabilities, version }；列表各 item 返回 record 与其 fieldAccess/capabilities，避免逐行请求。隐藏字段省略，脱敏字段只含脱敏字符串。更新使用有权限且实际编辑过的字段 patch，不传整个读取对象。

## 3. 身份、平台及组织接口

| 路径 | 方法与职责 |
|---|---|
| /v1/me/bootstrap | GET 当前身份、应用、菜单、操作及版本 |
| /v1/me/capabilities | GET 刷新当前操作、版本、expiresAt |
| /v1/platform/members | GET/POST 平台成员列表、创建平台成员资格；关联全局账号，不自动授予角色 |
| /v1/platform/members/{id} | GET/PATCH 平台成员资料；不编辑全局凭证或租户资料 |
| /v1/platform/members/{id}/status | PATCH 暂停/恢复平台成员资格，不改变租户成员状态 |
| /v1/platform/members/{id}/remove | POST 移出平台，不删除账号或租户成员 |
| /v1/platform/groups | GET/POST；/{id} GET/PUT/DELETE；/{id}/preview POST 引用影响；仅引用平台成员 |
| /v1/platform/tenants | GET 列表；POST 原子创建组织+所有者+基础开通 |
| /v1/platform/tenants/preview | POST 校验创建输入并展示最小初始化结果 |
| /v1/platform/tenants/{id} | GET/PATCH 组织实体；不返回租户业务数据 |
| /v1/platform/tenants/{id}/entitlements | GET/PUT 显式开通及期限 |
| /v1/platform/tenants/{id}/entitlements/preview | POST 开通或套餐应用影响 |
| /v1/platform/applications | GET/POST 应用目录 |
| /v1/platform/applications/{id} | GET/PUT/PATCH 状态/DELETE（未引用） |
| /v1/platform/applications/{id}/resources | GET/POST 资源；/{resourceId} PUT/DELETE |
| /v1/platform/applications/{id}/actions | GET/POST 操作；/{actionId} PUT/PATCH/DELETE |
| /v1/platform/applications/{id}/menus | GET/POST 导航；/{menuId} PUT/DELETE |
| /v1/platform/plans | GET/POST；/{id} GET/PUT；应用到租户须预览并显式提交 |
| /v1/tenant/members | GET/POST 成员列表、创建成员关系 |
| /v1/tenant/members/{id} | GET/PATCH 组织资料，禁止全局凭证字段 |
| /v1/tenant/members/{id}/departments | PUT 调整关系，校验两端 |
| /v1/tenant/members/{id}/status | PATCH 暂停/恢复成员资格 |
| /v1/tenant/members/{id}/remove | POST 移出组织，不删除账号 |
| /v1/tenant/members/export | POST 受独立操作、范围和字段限制的导出；GET /{id} 下载时再次校验 |
| /v1/tenant/departments | GET 树/候选；POST；/{id} GET/PUT/DELETE |
| /v1/tenant/groups | GET/POST；/{id} GET/PUT/DELETE；/{id}/preview POST 引用影响 |
| /v1/tenant/settings | GET/PUT 组织设置；所有者转交使用独立 /owner-transfer POST |
| /v1/tenant/applications | GET 已开通应用；/{id}/audience GET/PUT 可用人群 |
| /v1/tenant/applications/{id}/actions | GET 当前已开通应用的操作与范围能力候选；仅供配置，不授予这些操作 |

平台账号管理与现有安全、字典、发号等保留能力同步迁到 IAM 命名，不改变其领域语义；新授权动作必须列入资源目录。账户创建/邀请按账号与成员分离处理，既有账号不能被直接重新设置凭证，返回信息不得枚举其他组织身份。

所有候选选择接口须声明 purpose（由服务端枚举白名单定义），例如 ASSIGN_RECIPIENT、MANAGED_DEPARTMENT、DIRECTORY；不能通过更换 purpose 获得无权的全集。资源目录给出支持的操作及范围，不允许客户端提交未知 purpose。

平台 Selection 的 departments 必须为空，ScopeBindings 不接受 DEPARTMENTS；平台角色不接受 MEMBER_DEPARTMENTS/MANAGED_DEPARTMENTS。平台与租户的成员/组均从当前可信身份域解析。客户端提交的引用只有类型和 ID，不授予切换域的能力。同一账号同时出现在平台和租户时，两者使用各自的 memberId。

成员状态使用 MemberStatus：ACTIVE（有效）、SUSPENDED（暂停）、REMOVED（已移出）。状态字段描述当前成员资格，不是全局账号状态；移出使用独立 remove 命令，不能绕过该命令通过普通资料 PATCH 修改状态。全局账号 enabled 与成员状态分开保存，授权必须同时验证。

## 4. 角色、授权、策略

平台自有角色管理使用 /v1/platform/roles；共享角色发布使用 /v1/platform/shared-roles。租户聚合角色目录使用 /v1/tenant/roles，返回可用共享角色及本地角色，不为展示创建记录。

| 路径（角色、授权管理域由 platform/tenant 区分） | 职责 |
|---|---|
| /v1/{domain}/roles | GET 目录；POST 自定义角色或 tenant 基于共享角色定制 |
| /v1/{domain}/roles/{id} | GET 元数据；PATCH 状态；DELETE 仅未引用 |
| /v1/{domain}/roles/{id}/revisions | GET 版本；POST 发布新版本，不自动升级授权 |
| /v1/{domain}/roles/{id}/preview | POST 预览待发布最终定义 |
| /v1/tenant/roles/{id}/upgrade-preview | POST {newBaseRevisionId, resolutions?} 三方比较 |
| /v1/tenant/roles/{id}/upgrade | POST {expectedVersion,newBaseRevisionId,resolutions,assignmentIds[]}；未解决冲突拒绝 |
| /v1/{domain}/assignments | GET；POST {items: AssignmentInput[]} 原子分配 |
| /v1/{domain}/assignments/preview | POST 同分配输入，返回逐接收对象效果及限制 |
| /v1/{domain}/assignments/{id} | PUT 调整版本/范围/期限；DELETE 撤销，保留审计 |
| /v1/{domain}/delegations | GET/POST；/{id} GET/PUT/DELETE |
| /v1/{domain}/delegations/{id}/preview | POST 收缩/撤销的派生授权影响 |
| /v1/tenant/policies/directory | GET/PUT 默认设置及允许/禁止规则，带 expectedVersion |
| /v1/tenant/policies/fields | GET/PUT 场景、字段、查看者和目标范围规则 |
| /v1/tenant/policies/preview | POST {policyDraft, viewerMemberId, target?}；无副作用 |
| /v1/{domain}/authorization/diagnose | POST {memberId/accountId, applicationId, actionId, targetId?} |
| /v1/{domain}/authorization/audits | GET 分页事件；导出需独立权限 |
| /v1/directory/members | GET 搜索/分页；/{id} GET；均执行普通通讯录规则 |
| /v1/directory/departments | GET 可见树及必要祖先骨架 |

{domain} 是路由定义占位，只允许 platform 或 tenant，不接受任意运行时字符串转换权限域。共享角色版本发布复用角色预览/版本规则；系统治理角色不向租户开放 mutation。

字段策略项至少包含 scenario、fieldKey、viewerSelection、targetScope、visibility、editable；通讯录项包含 effect、viewerSelection、targetSelection。默认策略与显式规则分开保存，不能把默认全组织允许作为始终参与并集的规则。

## 5. 发布、错误及前端衔接

未保存草稿保留在前端；退出不产生生效版本。预览返回配置版本，提交同时携带 expectedVersion 和完整待写内容，服务器重新验证。角色发布/升级、批量授权及策略替换均为原子命令。

权限错误：ActionDenied、DataScopeDenied、DelegationExceeded、RoleRevisionUnavailable、ApplicationUnavailable、PolicyConflict、RevisionConflict、ObjectInUse、AuthorizationUnavailable。消息为可展示中文；reasonCode 为稳定枚举，不能只让前端解析文字。

401 重新认证；403 刷新能力后提示；404 显示不存在或不可访问，不区分是否真实存在；409 保留草稿并重新预览；503 禁止受保护提交、重试，不清为游客或成功空数据。

后端实现阶段须补齐上述端点的 OpenAPI schemas、字段必填/只读、合法过滤排序、完整示例、操作 code 映射并通过契约测试；不得由前端 Agent 猜测实际 DTO。业务语义变化必须先修改本契约再实施。前端开发前以本文件及后端已验证 OpenAPI 配套读取。

旧入口的逐项目标与处置见 [endpoint-mapping.json](./endpoint-mapping.json)。其中 RETIRED 表示新系统不保留该旧入口；SERVICE 和 AUTHENTICATED_SELF 仍须各自身份校验，不是匿名或超管 bypass。该表是执行接入清单，不用于自动向迁移账号授予新操作。补充保留能力的目标路由包括平台 accounts/dictionaries/id-allocations/social-configs、当前账号 me/password/me/profile 及已生成的 migration/reports；具体方法按映射清单核对。

当前公共 DTO 的已验证 schemas 和示例见 [contracts](./contracts/README.md)。这是 components 快照，包含管理命令、bootstrap、资源详情、预览/升级/诊断/审计响应及 R 信封实例；目标端点路径已列入 openapi.json，运行时仍未接入。

### 已落地的响应细节

分页 total/page/pageSize 和可见影响数量使用 JSON 数字；ID 与版本保持字符串。配置状态 ConfigurationStatus 为 ENABLED/DISABLED，持久化映射到 enabled；成员资格继续使用独立 MemberStatus。每条成员列表 item 为 ResourceDetail<MemberRecord>，包含 record、fieldAccess、capabilities、version。

MemberRecord 不包含凭证；隐藏资料以 null 表示并在 JSON 中省略，脱敏值必须由服务端先行投影。DTO 本身不执行字段策略。DepartmentRecord 的 navigationOnly 标识祖先导航骨架，不携带隐藏成员计数。

UsageSummary 与 ImpactSummary 仅披露允许查看的数量，无法披露的项省略并标记 restricted，不能用 0 代替未知值。Decision 的 sources 只包含可披露来源，受限时返回 []，fieldAccess 无可披露项时为 {}。Preview 在 errors 非空时 valid 必须为 false；无法产生有效结果时省略 effectiveResult。UpgradePreview 另外携带 version 与受限 impactSummary，冲突以稳定 key 表达。

AuditEntry 的 before/after 使用 AuditField 枚举白名单，涵盖名称、状态、角色版本、范围、接收对象、有效期、开通、策略版本和所有者。服务端仍须构造安全摘要，禁止将原始请求、凭证或未脱敏个人资料填入任何字符串值。审计 DTO 不能替代业务层脱敏和可靠落库。


### 策略与版本命令的精确定义

- DirectoryPolicyDraft 为 `{defaultRevisionId, defaultOverride?, rules[]}`，FieldPolicyDraft 为 `{defaultRevisionId, rules[]}`；默认版本与本地显式规则分别保存。DirectoryDefault 为 `{scope, selection?}`，仅 SELECTED 必须携带 selection，ALL/SELF 不允许携带。
- 两类策略 PUT 请求分别为 DirectoryPolicyInput / FieldPolicyInput：`{expectedVersion, policy}`；policy 是完整草稿，rules=[] 清除本地规则并使用固定默认版本。读取和替换成功均返回相应 ResourceDetail 包装，包含当前 version。
- PolicyDraft 为 `{kind, directory?, field?}`，DIRECTORY/FIELD 必须且只能携带匹配配置。PolicyPreviewInput 为 `{policyDraft, viewerMemberId, target?}`，target 为可选目标成员 ID。预览返回 Preview<PolicyPreviewResult>，含经过操作者权限限制的成员样例、部门导航骨架及 restricted 标志；没有总人数泄露。
- DiagnoseInput 为 `{memberId?, accountId?, applicationId, actionId, targetId?}`；两种身份必须二选一且非空。accountId 仅在当前可信域中解析成员，不支持切换或聚合域。
- MemberStatusInput 为 `{status, expectedVersion}`，只允许 ACTIVE/SUSPENDED，REMOVED 通过独立 remove 命令；ConfigurationStatusInput 使用 ENABLED/DISABLED。OwnerTransferInput 为 `{expectedVersion,newOwnerMemberId}`。
- GroupUpdateInput 为 `{expectedVersion,group:{name,description?,selection}}`；AudienceUpdateInput 为 `{expectedVersion,audience:{kind,selection?,groupIds[]}}`，ALL 不携带选择器或组，SELECTED 携带选择器和组列表。平台组的部门限制仍须按可信域校验。
- AssignmentUpdateInput / DelegationUpdateInput 分别为 `{expectedVersion,assignment}` / `{expectedVersion,delegation}`；服务端重验原委派来源、接收对象和所有派生授权，DTO 不赋予绕过资格。分配预览返回 Preview<AssignmentPreviewResult>，逐主体列出 allowed、errors、可披露 grants。
- RoleCreateInput 为 `{code,name,description?,groupName?,kind,baseRevisionId?,definition}`；kind 只允许 SHARED/PLATFORM_CUSTOM/TENANT_CUSTOM。仅 TENANT_CUSTOM 可绑定共享基础，此时 grants 必须为空。RoleDefinitionDraft 为 `{grants,deltas,parameterDefinitions,metadataOverrides?}`，完整版本与差异不能同时非空。RolePublishInput 为 `{expectedVersion,definition}`，发布不自动升级授权。预览待发布定义直接提交 RoleDefinitionDraft。
- UpgradePreviewInput 为 `{newBaseRevisionId,resolutions?}`；UpgradeInput 为 `{expectedVersion,newBaseRevisionId,resolutions,assignmentIds[]}`。UpgradeResolution 为 `{key,choice,scopes?}`，choice 为 ACCEPT_BASE/KEEP_DELTA/REPLACE_SCOPE，只有替换范围必须携带 scopes。未解决冲突拒绝提交；默认不选择既有授权。
- MemberCreateInput 为 `{accountId,displayName?,departments[]}`，不创建凭证；平台任职必须由服务拒绝非空部门。MemberProfileInput 为 `{expectedVersion,displayName?,avatar?,phone?,email?}`，禁止状态和凭证字段；phone/email 可空表示不修改，不可编辑或脱敏占位由服务拒绝。MemberDepartmentInput 为 `{expectedVersion,departments[{id,primary}]}`，部门不得重复且最多一个主部门。
- TenantCreateInput 为 `{name,ownerAccountId,ownerDisplayName?,rootDepartmentName?,avatar?,planId?}`。客户端不能提交治理版本、默认策略或任意应用清单；服务器从基础目录或指定套餐解析开通。预览返回 TenantPreviewResult，不含可回写的版本 ID。TenantUpdateInput / TenantSettingsInput 分别更新平台可见实体与租户设置。
- ApplicationDraft 可标记 baseline，仅租户域允许。组织初始化缺省开通 baseline 应用；指定 planId 时改为该套餐内租户域启用应用。EntitlementReplaceInput 为 `{expectedVersion,entitlements[{applicationId,status,validFrom?,validUntil?}]}`。组与委派影响预览返回 ReferenceImpactPreview。

管理面目标路径见 `contracts/openapi.json` 与 `contracts/routes.json`，覆盖 API 第 3、4 节列出的身份、目录、角色、授权、策略、诊断和审计接口，以及成员导出下载。控制器接入后对应操作 `x-runtime-implemented=true`。内部 RPC 与账号安全保留入口仍按 endpoint-mapping 接入，不计入此前管理面清单。该文档不是线上已发布接口证明。
