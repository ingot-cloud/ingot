# IAM 接口与前端契约

> 目标契约，状态随主 change 为 review。尚未实现，不能据此调用线上接口。实现导出的 OpenAPI 必须与此一致；实体完整字段在对应业务 DTO 中显式声明，不直接透传 ORM 实体。

## 1. 通用约定

外部前缀 /api/iam，下表路径相对该前缀。服务路由为 /v1/...。沿用仓库 R<T> 成功/错误信封；分页 data 统一为 { items, total, page, pageSize }。ID/版本为字符串，时间 ISO-8601 UTC，集合返回 [] 而非 null。

读取不允许以 tenantId query 切换租户。上下文切换通过现有认证流程重新建立身份，再 bootstrap。平台租户 path 参数只授权管理租户实体，不构成目标租户会话。

状态与种类使用语义枚举：PLATFORM/TENANT、MEMBER/GROUP、SYSTEM/SHARED/TENANT_CUSTOM、ADD/REMOVE/REPLACE_SCOPE、HIDDEN/MASKED/FULL。开始时间包含、结束时间不包含。

集合 GET 支持分页及该资源明确定义的过滤项，不能接受任意字段名/SQL 排序。POST 创建返回 { id, version }；PUT 配置整体替换并传 expectedVersion；PATCH 状态显式 { status, expectedVersion }。删除被引用对象返回稳定 InUse 错误。

## 2. 主要 DTO

| 类型 | 关键字段及语义 |
|---|---|
| AuthorizationContext | domain, tenantId?, accountId, memberId?；均由可信身份得到 |
| Bootstrap | context, profile, applications, menus, actionCodes, version, expiresAt；同一有效授权视图，不含全租户授权快照 |
| SubjectRef | type: MEMBER/GROUP, id；成员为 memberId，不混用 accountId |
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

详情数据返回 { record, fieldAccess, capabilities, version }；列表各 item 返回 record 与其 fieldAccess/capabilities，避免逐行请求。隐藏字段省略，脱敏字段只含脱敏字符串。更新使用有权限且实际编辑过的字段 patch，不传整个读取对象。

## 3. 身份、平台及组织接口

| 路径 | 方法与职责 |
|---|---|
| /v1/me/bootstrap | GET 当前身份、应用、菜单、操作及版本 |
| /v1/me/capabilities | GET 刷新当前操作、版本、expiresAt |
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
| /v1/tenant/members/export | POST 受独立操作、范围和字段限制的导出 |
| /v1/tenant/departments | GET 树/候选；POST；/{id} GET/PUT/DELETE |
| /v1/tenant/groups | GET/POST；/{id} GET/PUT/DELETE；/{id}/preview POST 引用影响 |
| /v1/tenant/settings | GET/PUT 组织设置；所有者转交使用独立 /owner-transfer POST |
| /v1/tenant/applications | GET 已开通应用；/{id}/audience GET/PUT 可用人群 |

平台账号管理与现有安全、字典、发号等保留能力同步迁到 IAM 命名，不改变其领域语义；新授权动作必须列入资源目录。账户创建/邀请按账号与成员分离处理，既有账号不能被直接重新设置凭证，返回信息不得枚举其他组织身份。

所有候选选择接口须声明 purpose（由服务端枚举白名单定义），例如 ASSIGN_RECIPIENT、MANAGED_DEPARTMENT、DIRECTORY；不能通过更换 purpose 获得无权的全集。资源目录给出支持的操作及范围，不允许客户端提交未知 purpose。

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
