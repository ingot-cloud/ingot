#!/usr/bin/env python3
"""生成 IAM 管理面 routes.json。x-runtime-implemented 由 implemented 字段控制。"""
import json
from pathlib import Path

CONTRACT = Path(__file__).resolve().parents[2] / 'specs/changes/active/20260912-iam-identity-access-management/contracts'


def route(path, method, operation_id, summary, domain, action, request, response, execution, example=None, implemented=False, query=None):
    item = {
        'path': path, 'method': method, 'operationId': operation_id, 'summary': summary,
        'domain': domain, 'action': action, 'request': request, 'response': response,
        'execution': execution, 'implemented': implemented,
    }
    if example:
        item['example'] = example
    if query:
        item['query'] = query
    return item


PAGE = [
    {'name': 'page', 'required': False, 'schema': {'type': 'integer', 'minimum': 1, 'default': 1}},
    {'name': 'pageSize', 'required': False, 'schema': {'type': 'integer', 'minimum': 1, 'maximum': 200, 'default': 20}},
]
NAME_STATUS = PAGE + [
    {'name': 'name', 'required': False, 'schema': {'type': 'string'}},
    {'name': 'status', 'required': False, 'schema': {'type': 'string', 'enum': ['ENABLED', 'DISABLED']}},
]
APPLICATION_LIST = NAME_STATUS + [
    {'name': 'baseline', 'required': False, 'schema': {'type': 'boolean'}},
]
PHONE_EMAIL = [
    {'name': 'phone', 'required': False, 'schema': {'type': 'string'}},
    {'name': 'email', 'required': False, 'schema': {'type': 'string'}},
]
MYBATIS_PAGE = [
    {'name': 'current', 'required': False, 'schema': {'type': 'integer', 'minimum': 1, 'default': 1}},
    {'name': 'size', 'required': False, 'schema': {'type': 'integer', 'minimum': 1, 'default': 10}},
]


def purpose(value):
    return {'name': 'purpose', 'required': True, 'schema': {'type': 'string', 'enum': [value]}}


def member_routes(domain, prefix, ns, list_query=None):
    detail = 'RResourceDetailMemberRecord'
    page = 'RPageResponseResourceDetailMemberRecord'
    created = 'RCreatedResource'
    exec_member = '可信当前域；成员管理操作；账号与成员分离；状态与任职覆盖全部关系并写审计'
    query = PAGE if list_query is None else list_query
    return [
        route(f'{prefix}/members', 'get', f'{domain.lower()}ListMembers', '成员列表', domain, f'{ns}:member:read', None, page, exec_member, query=query),
        route(f'{prefix}/members', 'post', f'{domain.lower()}CreateMember', '创建成员资格', domain, f'{ns}:member:create', 'MemberCreateInput', created, exec_member),
        route(f'{prefix}/members/{{id}}', 'get', f'{domain.lower()}GetMember', '成员详情', domain, f'{ns}:member:read', None, detail, exec_member),
        route(f'{prefix}/members/{{id}}', 'patch', f'{domain.lower()}PatchMember', '更新成员资料', domain, f'{ns}:member:update', 'MemberProfileInput', detail, exec_member),
        route(f'{prefix}/members/{{id}}/status', 'patch', f'{domain.lower()}PatchMemberStatus', '暂停或恢复成员', domain, f'{ns}:member:status', 'MemberStatusInput', created, exec_member),
        route(f'{prefix}/members/{{id}}/remove', 'post', f'{domain.lower()}RemoveMember', '移出当前域', domain, f'{ns}:member:remove', 'VersionInput', created, exec_member),
    ]


def group_routes(domain, prefix, ns):
    detail = 'RResourceDetailGroupRecord'
    page = 'RPageResponseResourceDetailGroupRecord'
    created = 'RCreatedResource'
    preview = 'RPreviewReferenceImpactPreview'
    execution = '可信当前域；组修改重验委派派生授权；平台组不得引用部门'
    return [
        route(f'{prefix}/groups', 'get', f'{domain.lower()}ListGroups', '用户组列表', domain, f'{ns}:group:read', None, page, execution, query=PAGE),
        route(f'{prefix}/groups', 'post', f'{domain.lower()}CreateGroup', '创建用户组', domain, f'{ns}:group:create', 'GroupDraft', created, execution),
        route(f'{prefix}/groups/{{id}}', 'get', f'{domain.lower()}GetGroup', '用户组详情', domain, f'{ns}:group:read', None, detail, execution),
        route(f'{prefix}/groups/{{id}}', 'put', f'{domain.lower()}PutGroup', '替换用户组', domain, f'{ns}:group:update', 'GroupUpdateInput', detail, execution),
        route(f'{prefix}/groups/{{id}}', 'delete', f'{domain.lower()}DeleteGroup', '删除未引用用户组', domain, f'{ns}:group:delete', None, created, execution),
        route(f'{prefix}/groups/{{id}}/preview', 'post', f'{domain.lower()}PreviewGroup', '预览组引用影响', domain, f'{ns}:group:preview', 'GroupUpdateInput', preview, execution),
    ]


def role_routes(domain, prefix, ns, shared=False):
    kind = 'shared-role' if shared else 'role'
    path = f'{prefix}/shared-roles' if shared else f'{prefix}/roles'
    prefix_id = domain.lower() + ('Shared' if shared else '')
    detail = 'RResourceDetailRoleSummary'
    page = 'RPageResponseResourceDetailRoleSummary'
    revision_page = 'RPageResponseResourceDetailRoleRevision'
    revision_detail = 'RResourceDetailRoleRevision'
    created = 'RCreatedResource'
    preview = 'RPreviewEffectiveRole'
    execution = '固定版本发布不自动升级授权；系统治理角色禁止租户变更；提交重验版本'
    items = [
        route(path, 'get', f'{prefix_id}ListRoles', '角色目录', domain, f'{ns}:{kind}:read', None, page, execution, query=PAGE),
        route(path, 'post', f'{prefix_id}CreateRole', '创建角色并发布首个版本', domain, f'{ns}:{kind}:create', 'RoleCreateInput', created, execution,
              'role-create' if domain == 'TENANT' and not shared else None),
        route(f'{path}/{{id}}', 'get', f'{prefix_id}GetRole', '角色元数据', domain, f'{ns}:{kind}:read', None, detail, execution),
        route(f'{path}/{{id}}', 'patch', f'{prefix_id}PatchRoleStatus', '启停角色', domain, f'{ns}:{kind}:status', 'ConfigurationStatusInput', created, execution),
        route(f'{path}/{{id}}', 'delete', f'{prefix_id}DeleteRole', '删除未引用角色', domain, f'{ns}:{kind}:delete', None, created, execution),
        route(f'{path}/{{id}}/revisions', 'get', f'{prefix_id}ListRoleRevisions', '角色版本', domain, f'{ns}:{kind}:read', None, revision_page, execution, query=PAGE),
        route(f'{path}/{{id}}/revisions', 'post', f'{prefix_id}PublishRoleRevision', '发布新版本', domain, f'{ns}:{kind}:publish', 'RolePublishInput', created, execution, 'role-publish'),
        route(f'{path}/{{id}}/preview', 'post', f'{prefix_id}PreviewRole', '预览待发布定义', domain, f'{ns}:{kind}:preview', 'RoleDefinitionDraft', preview, execution),
    ]
    return items


def assignment_routes(domain, prefix, ns):
    detail = 'RResourceDetailAssignmentRecord'
    page = 'RPageResponseResourceDetailAssignmentRecord'
    created = 'RCreatedResource'
    execution = '原子分配；逐授权合成后并集；平台不得使用部门参数；提交重验委派来源'
    return [
        route(f'{prefix}/assignments', 'get', f'{domain.lower()}ListAssignments', '授权列表', domain, f'{ns}:assignment:read', None, page, execution, query=PAGE),
        route(f'{prefix}/assignments', 'post', f'{domain.lower()}CreateAssignments', '原子批量分配', domain, f'{ns}:assignment:create', 'AssignmentBatchInput', created, execution),
        route(f'{prefix}/assignments/{{id}}', 'put', f'{domain.lower()}PutAssignment', '调整授权', domain, f'{ns}:assignment:update', 'AssignmentUpdateInput', detail, execution),
        route(f'{prefix}/assignments/{{id}}', 'delete', f'{domain.lower()}DeleteAssignment', '撤销授权并保留审计', domain, f'{ns}:assignment:delete', None, created, execution),
    ]


def delegation_routes(domain, prefix, ns):
    detail = 'RResourceDetailDelegationRecord'
    page = 'RPageResponseResourceDetailDelegationRecord'
    created = 'RCreatedResource'
    preview = 'RPreviewReferenceImpactPreview'
    execution = '治理权限创建委派；收缩取交集；来源撤销使派生授权无效'
    return [
        route(f'{prefix}/delegations', 'get', f'{domain.lower()}ListDelegations', '委派列表', domain, f'{ns}:delegation:read', None, page, execution, query=PAGE),
        route(f'{prefix}/delegations', 'post', f'{domain.lower()}CreateDelegation', '创建委派', domain, f'{ns}:delegation:create', 'DelegationInput', created, execution, 'delegation'),
        route(f'{prefix}/delegations/{{id}}', 'get', f'{domain.lower()}GetDelegation', '委派详情', domain, f'{ns}:delegation:read', None, detail, execution),
        route(f'{prefix}/delegations/{{id}}', 'put', f'{domain.lower()}PutDelegation', '调整委派', domain, f'{ns}:delegation:update', 'DelegationUpdateInput', detail, execution),
        route(f'{prefix}/delegations/{{id}}', 'delete', f'{domain.lower()}DeleteDelegation', '撤销委派', domain, f'{ns}:delegation:delete', None, created, execution),
        route(f'{prefix}/delegations/{{id}}/preview', 'post', f'{domain.lower()}PreviewDelegation', '预览委派收缩影响', domain, f'{ns}:delegation:preview', 'DelegationUpdateInput', preview, execution),
    ]


def build():
    current_exec = '认证当前身份；身份及账号同时有效；固定域；一致快照'
    routes = [
        route('/v1/me/bootstrap', 'get', 'currentBootstrap', '当前身份一致授权视图', 'CURRENT', None, None, 'RBootstrap', current_exec),
        route('/v1/me/capabilities', 'get', 'currentCapabilities', '当前身份操作能力', 'CURRENT', None, None, 'RCurrentCapabilities',
              '认证当前身份；不得跨域聚合；无可用授权服务时返回 503'),
        route('/v1/me/profile', 'get', 'currentGetProfile', '当前账号资料', 'CURRENT', None, None, 'RAccountSelfProfile',
              'AUTHENTICATED_SELF；只读当前认证账号联系资料，不含其他组织身份'),
        route('/v1/me/profile', 'patch', 'currentPatchProfile', '更新当前账号资料', 'CURRENT', None, 'AccountSelfProfileInput',
              'RAccountSelfProfile', 'AUTHENTICATED_SELF；禁止提交其他账号 ID 或凭证字段'),
        route('/v1/me/password', 'put', 'currentPutPassword', '当前账号改密', 'CURRENT', None, 'CurrentPasswordInput', 'RVoid',
              'AUTHENTICATED_SELF；沿用框架改密校验与事件；请求体加密'),
        route('/v1/directory/members', 'get', 'listDirectoryMembers', '普通通讯录成员', 'TENANT', 'iam-tenant:directory:read', None,
              'RPageResponseResourceDetailMemberRecord', '普通通讯录规则；搜索计数详情一致；无管理 ACTION',
              query=[purpose('DIRECTORY')] + PAGE + PHONE_EMAIL),
        route('/v1/directory/members/{id}', 'get', 'getDirectoryMember', '普通通讯录成员详情', 'TENANT', 'iam-tenant:directory:read', None,
              'RResourceDetailMemberRecord', '普通通讯录规则；隐藏字段省略，脱敏字段仅返回脱敏值'),
        route('/v1/directory/departments', 'get', 'listDirectoryDepartments', '普通通讯录部门树', 'TENANT', 'iam-tenant:directory:read', None,
              'RPageResponseResourceDetailDepartmentRecord', '必要祖先仅为导航骨架，不提供隐藏分支人数',
              query=[purpose('DIRECTORY')] + PAGE),
    ]
    routes += member_routes('PLATFORM', '/v1/platform', 'iam-platform')
    routes += group_routes('PLATFORM', '/v1/platform', 'iam-platform')
    routes += [
        route('/v1/platform/tenants', 'get', 'platformListTenants', '组织列表', 'PLATFORM', 'iam-platform:tenant:read', None,
              'RPageResponseResourceDetailTenantRecord',
              '平台管理组织实体，不返回租户业务数据；可选 name 包含匹配、status=ENABLED|DISABLED',
              query=NAME_STATUS),
        route('/v1/platform/tenants', 'post', 'platformCreateTenant', '原子创建组织', 'PLATFORM', 'iam-platform:tenant:create',
              'TenantCreateInput', 'RCreatedResource', '先校验创建 ACTION；目录生成计划；同事务初始化', 'tenant-create'),
        route('/v1/platform/tenants/preview', 'post', 'platformPreviewTenant', '预览组织初始化', 'PLATFORM', 'iam-platform:tenant:preview',
              'TenantCreateInput', 'RPreviewTenantPreviewResult', '无写入；结果由服务器目录解析，不接受客户端治理版本'),
        route('/v1/platform/tenants/{id}', 'get', 'platformGetTenant', '组织实体', 'PLATFORM', 'iam-platform:tenant:read', None,
              'RResourceDetailTenantRecord', '不返回租户业务成员或授权快照'),
        route('/v1/platform/tenants/{id}', 'patch', 'platformPatchTenant', '更新组织实体', 'PLATFORM', 'iam-platform:tenant:update',
              'TenantUpdateInput', 'RResourceDetailTenantRecord', '不修改租户业务资料'),
        route('/v1/platform/tenants/{id}/entitlements', 'get', 'platformListEntitlements', '组织开通', 'PLATFORM',
              'iam-platform:entitlement:read', None, 'RPageResponseResourceDetailEntitlementRecord', '开通不等于业务授权', query=PAGE),
        route('/v1/platform/tenants/{id}/entitlements', 'put', 'platformPutEntitlements', '替换组织开通', 'PLATFORM',
              'iam-platform:entitlement:update', 'EntitlementReplaceInput', 'RPageResponseResourceDetailEntitlementRecord',
              '重验版本、应用域与期限；基础治理入口不可关闭'),
        route('/v1/platform/tenants/{id}/entitlements/preview', 'post', 'platformPreviewEntitlements', '预览开通影响', 'PLATFORM',
              'iam-platform:entitlement:preview', 'EntitlementReplaceInput', 'RPreviewEntitlementPreviewResult', '无写入'),
        route('/v1/platform/applications', 'get', 'platformListApplications', '应用目录', 'PLATFORM', 'iam-platform:application:read', None,
              'RPageResponseResourceDetailApplicationRecord',
              '启用状态不等于业务授权；可选 name 包含匹配、status=ENABLED|DISABLED、baseline',
              query=APPLICATION_LIST),
        route('/v1/platform/applications', 'post', 'platformCreateApplication', '创建应用', 'PLATFORM', 'iam-platform:application:create',
              'ApplicationDraft', 'RCreatedResource', '租户域才可标记基础开通'),
        route('/v1/platform/applications/{id}', 'get', 'platformGetApplication', '应用详情', 'PLATFORM', 'iam-platform:application:read', None,
              'RResourceDetailApplicationRecord', '不把启用状态等同于业务操作授权'),
        route('/v1/platform/applications/{id}', 'put', 'platformPutApplication', '更新应用', 'PLATFORM', 'iam-platform:application:update',
              'ApplicationUpdateInput', 'RResourceDetailApplicationRecord', '不能改写命名空间或管理域'),
        route('/v1/platform/applications/{id}', 'patch', 'platformPatchApplicationStatus', '启停应用', 'PLATFORM',
              'iam-platform:application:status', 'ConfigurationStatusInput', 'RCreatedResource', '全局停用约束所有旧版本'),
        route('/v1/platform/applications/{id}', 'delete', 'platformDeleteApplication', '删除未引用应用', 'PLATFORM',
              'iam-platform:application:delete', None, 'RCreatedResource', '被引用拒绝物理删除'),
        route('/v1/platform/applications/{id}/resources', 'get', 'platformListResources', '应用资源', 'PLATFORM',
              'iam-platform:resource:read', None, 'RPageResponseResourceDetailResourceRecord', '不暴露 SQL 或适配器', query=PAGE),
        route('/v1/platform/applications/{id}/resources', 'post', 'platformCreateResource', '创建资源', 'PLATFORM',
              'iam-platform:resource:create', 'ResourceDraft', 'RCreatedResource', '范围与字段能力由资源声明'),
        route('/v1/platform/applications/{id}/resources/{resourceId}', 'put', 'platformPutResource', '更新资源', 'PLATFORM',
              'iam-platform:resource:update', 'ResourceUpdateInput', 'RResourceDetailResourceRecord', '不能改写所属应用或编码'),
        route('/v1/platform/applications/{id}/resources/{resourceId}', 'delete', 'platformDeleteResource', '删除未引用资源', 'PLATFORM',
              'iam-platform:resource:delete', None, 'RCreatedResource', '被引用拒绝物理删除'),
        route('/v1/platform/applications/{id}/actions', 'get', 'platformListActions', '应用操作', 'PLATFORM',
              'iam-platform:action:read', None, 'RPageResponseResourceDetailActionRecord', '精确操作码，禁止通配', query=PAGE),
        route('/v1/platform/applications/{id}/actions', 'post', 'platformCreateAction', '创建操作', 'PLATFORM',
              'iam-platform:action:create', 'ActionDraft', 'RCreatedResource', '操作码全局唯一且不得含通配符'),
        route('/v1/platform/applications/{id}/actions/{actionId}', 'put', 'platformPutAction', '更新操作', 'PLATFORM',
              'iam-platform:action:update', 'ActionUpdateInput', 'RResourceDetailActionRecord', '不能改写操作码或所属资源'),
        route('/v1/platform/applications/{id}/actions/{actionId}', 'patch', 'platformPatchActionStatus', '启停操作', 'PLATFORM',
              'iam-platform:action:status', 'ConfigurationStatusInput', 'RCreatedResource', '全局停用约束所有旧版本'),
        route('/v1/platform/applications/{id}/actions/{actionId}', 'delete', 'platformDeleteAction', '删除未引用操作', 'PLATFORM',
              'iam-platform:action:delete', None, 'RCreatedResource', '被引用拒绝物理删除'),
        route('/v1/platform/applications/{id}/menus', 'get', 'platformListMenus', '应用菜单', 'PLATFORM',
              'iam-platform:menu:read', None, 'RPageResponseResourceDetailMenuRecord', '导航关联独立操作', query=PAGE),
        route('/v1/platform/applications/{id}/menus', 'post', 'platformCreateMenu', '创建菜单', 'PLATFORM',
              'iam-platform:menu:create', 'MenuDraft', 'RCreatedResource', '按钮权限仍由操作目录表达'),
        route('/v1/platform/applications/{id}/menus/{menuId}', 'put', 'platformPutMenu', '更新菜单', 'PLATFORM',
              'iam-platform:menu:update', 'MenuUpdateInput', 'RResourceDetailMenuRecord', '重验本应用操作引用'),
        route('/v1/platform/applications/{id}/menus/{menuId}', 'delete', 'platformDeleteMenu', '删除菜单', 'PLATFORM',
              'iam-platform:menu:delete', None, 'RCreatedResource', '子菜单存在时拒绝'),
        route('/v1/platform/plans', 'get', 'platformListPlans', '套餐列表', 'PLATFORM', 'iam-platform:plan:read', None,
              'RPageResponseResourceDetailPlanRecord', '修改套餐不自动改变既有开通', query=PAGE),
        route('/v1/platform/plans', 'post', 'platformCreatePlan', '创建套餐', 'PLATFORM', 'iam-platform:plan:create',
              'PlanDraft', 'RCreatedResource', '应用到租户须预览并显式提交'),
        route('/v1/platform/plans/{id}', 'get', 'platformGetPlan', '套餐详情', 'PLATFORM', 'iam-platform:plan:read', None,
              'RResourceDetailPlanRecord', '修改套餐不自动改变既有开通'),
        route('/v1/platform/plans/{id}', 'put', 'platformPutPlan', '更新套餐', 'PLATFORM', 'iam-platform:plan:update',
              'PlanUpdateInput', 'RResourceDetailPlanRecord', '不影响已开通租户'),
    ]
    routes += role_routes('PLATFORM', '/v1/platform', 'iam-platform')
    routes += role_routes('PLATFORM', '/v1/platform', 'iam-platform', shared=True)
    routes += assignment_routes('PLATFORM', '/v1/platform', 'iam-platform')
    routes += [
        route('/v1/platform/assignments/preview', 'post', 'platformPreviewAssignments', '预览原子分配批次', 'PLATFORM',
              'iam-platform:assignment:create', 'AssignmentBatchInput', 'RPreviewAssignmentPreviewResult',
              '无写入；逐接收者检查角色、范围、委派和时间；平台不得使用部门；同一请求全量重验'),
    ]
    routes += delegation_routes('PLATFORM', '/v1/platform', 'iam-platform')
    routes += [
        route('/v1/platform/authorization/diagnose', 'post', 'platformDiagnose', '受限授权诊断', 'PLATFORM',
              'iam-platform:authorization:diagnose', 'DiagnoseInput', 'RDecision',
              '可信当前域解析身份；精确应用操作；限制对象及来源披露；无实际操作'),
        route('/v1/platform/authorization/audits', 'get', 'platformListAudits', '平台审计', 'PLATFORM',
              'iam-platform:audit:read', None, 'RPageResponseResourceDetailAuditEntry',
              '审计脱敏；不记录凭证明文或敏感原值', query=PAGE),
    ]
    routes += member_routes('TENANT', '/v1/tenant', 'iam-tenant', PAGE + PHONE_EMAIL)
    routes += [
        route('/v1/tenant/members/{id}/departments', 'put', 'tenantReplaceMemberDepartments', '调整成员任职', 'TENANT',
              'iam-tenant:member:departments', 'MemberDepartmentInput', 'RResourceDetailMemberRecord',
              '只校验受影响原/目标关系；覆盖全部部门写入审计'),
        route('/v1/tenant/members/export', 'post', 'tenantExportMembers', '导出成员', 'TENANT',
              'iam-tenant:member:export', 'VersionInput', 'RCreatedResource',
              '独立导出操作；登记共享任务后异步快照；字段策略与异步重验'),
        route('/v1/tenant/members/export/{id}/status', 'get', 'tenantGetMemberExportStatus', '查询成员导出任务', 'TENANT',
              'iam-tenant:member:export', None, 'RExportTask',
              '多实例可读同一任务状态；PENDING/RUNNING 可轮询；失败与过期不把快照当成功'),
        route('/v1/tenant/members/export/{id}', 'get', 'tenantDownloadMemberExport', '下载成员导出', 'TENANT',
              'iam-tenant:member:export', None, 'RPageResponseResourceDetailMemberRecord',
              '再次校验导出操作、对象范围与字段策略；PENDING/RUNNING 返回 503；失败或过期视为对象不可访问'),
        route('/v1/tenant/departments', 'get', 'tenantListDepartments', '部门树', 'TENANT', 'iam-tenant:department:read', None,
              'RPageResponseResourceDetailDepartmentRecord', '禁止跨租户；循环由服务拒绝',
              query=[purpose('MANAGED_DEPARTMENT')] + PAGE),
        route('/v1/tenant/departments', 'post', 'tenantCreateDepartment', '创建部门', 'TENANT', 'iam-tenant:department:create',
              'DepartmentDraft', 'RCreatedResource', '校验父节点归属'),
        route('/v1/tenant/departments/{id}', 'get', 'tenantGetDepartment', '部门详情', 'TENANT', 'iam-tenant:department:read', None,
              'RResourceDetailDepartmentRecord', '不携带隐藏成员计数'),
        route('/v1/tenant/departments/{id}', 'put', 'tenantPutDepartment', '更新部门', 'TENANT', 'iam-tenant:department:update',
              'DepartmentUpdateInput', 'RResourceDetailDepartmentRecord', '移动校验子树和新父节点'),
        route('/v1/tenant/departments/{id}', 'delete', 'tenantDeleteDepartment', '删除空部门', 'TENANT', 'iam-tenant:department:delete',
              None, 'RCreatedResource', '非空部门拒绝删除'),
        route('/v1/tenant/settings', 'get', 'tenantGetSettings', '组织设置', 'TENANT', 'iam-tenant:settings:read', None,
              'RResourceDetailTenantRecord', '所有者转交走独立命令'),
        route('/v1/tenant/settings', 'put', 'tenantPutSettings', '更新组织设置', 'TENANT', 'iam-tenant:settings:update',
              'TenantSettingsInput', 'RResourceDetailTenantRecord', '不能通过设置移除最后所有者'),
        route('/v1/tenant/settings/owner-transfer', 'post', 'tenantTransferOwner', '转交所有者', 'TENANT',
              'iam-tenant:settings:owner-transfer', 'OwnerTransferInput', 'RCreatedResource',
              '锁定组织及相关成员并检查版本'),
        route('/v1/tenant/applications', 'get', 'tenantListApplications', '已开通应用', 'TENANT', 'iam-tenant:application:read', None,
              'RPageResponseResourceDetailEntitlementRecord', '开通不等于业务操作权', query=PAGE),
        route('/v1/tenant/applications/{id}/audience', 'get', 'tenantGetAudience', '应用可用人群', 'TENANT',
              'iam-tenant:audience:read', None, 'RResourceDetailAudienceDraft', '停用强于授权'),
        route('/v1/tenant/applications/{id}/audience', 'put', 'tenantPutAudience', '更新可用人群', 'TENANT',
              'iam-tenant:audience:update', 'AudienceUpdateInput', 'RResourceDetailAudienceDraft',
              'ALL 不携带选择器或组'),
        route('/v1/tenant/applications/{id}/actions', 'get', 'tenantListApplicationActions', '已开通应用操作候选', 'TENANT',
              'iam-tenant:application:read', None, 'RPageResponseResourceDetailActionRecord',
              '仅供配置，不授予这些操作', query=PAGE),
    ]
    routes += group_routes('TENANT', '/v1/tenant', 'iam-tenant')
    routes += role_routes('TENANT', '/v1/tenant', 'iam-tenant')
    routes += [
        route('/v1/tenant/roles/{id}/upgrade-preview', 'post', 'tenantPreviewRoleUpgrade', '预览共享基础升级', 'TENANT',
              'iam-tenant:role:upgrade', 'UpgradePreviewInput', 'RPreviewUpgradePreview',
              '三方比较；无写入；不自动选择新旧定义'),
        route('/v1/tenant/roles/{id}/upgrade', 'post', 'tenantUpgradeRole', '提交共享基础升级', 'TENANT',
              'iam-tenant:role:upgrade', 'UpgradeInput', 'RCreatedResource',
              '未解决冲突拒绝；选定授权逐条验证，失败整次回滚', 'role-upgrade'),
    ]
    routes += assignment_routes('TENANT', '/v1/tenant', 'iam-tenant')
    routes += [
        route('/v1/tenant/assignments/preview', 'post', 'tenantPreviewAssignments', '预览原子分配批次', 'TENANT',
              'iam-tenant:assignment:create', 'AssignmentBatchInput', 'RPreviewAssignmentPreviewResult',
              '无写入；逐接收者检查角色、范围、委派和时间；平台不得使用部门；同一请求全量重验'),
        route('/v1/tenant/policies/directory', 'get', 'getDirectoryPolicy', '读取通讯录策略', 'TENANT',
              'iam-tenant:directory-policy:read', None, 'RResourceDetailDirectoryPolicyDraft',
              '可信租户；策略管理操作；引用同租户；PUT 重验版本、引用、字段上限并事务写审计'),
        route('/v1/tenant/policies/directory', 'put', 'putDirectoryPolicy', '整体替换通讯录策略', 'TENANT',
              'iam-tenant:directory-policy:update', 'DirectoryPolicyInput', 'RResourceDetailDirectoryPolicyDraft',
              '可信租户；策略管理操作；引用同租户；PUT 重验版本、引用、字段上限并事务写审计', 'directory-policy'),
        route('/v1/tenant/policies/fields', 'get', 'getFieldPolicy', '读取字段策略', 'TENANT',
              'iam-tenant:field-policy:read', None, 'RResourceDetailFieldPolicyDraft',
              '可信租户；策略管理操作；引用同租户；PUT 重验版本、引用、字段上限并事务写审计'),
        route('/v1/tenant/policies/fields', 'put', 'putFieldPolicy', '整体替换字段策略', 'TENANT',
              'iam-tenant:field-policy:update', 'FieldPolicyInput', 'RResourceDetailFieldPolicyDraft',
              '可信租户；策略管理操作；引用同租户；PUT 重验版本、引用、字段上限并事务写审计', 'field-policy'),
        route('/v1/tenant/policies/preview', 'post', 'previewPolicy', '只读策略预览', 'TENANT',
              'iam-tenant:policy:preview', 'PolicyPreviewInput', 'RPreviewPolicyPreviewResult',
              '真实引擎；无写入；查看者与目标同租户；结果与操作者可见范围求交', 'policy-preview'),
        route('/v1/tenant/authorization/diagnose', 'post', 'tenantDiagnose', '受限授权诊断', 'TENANT',
              'iam-tenant:authorization:diagnose', 'DiagnoseInput', 'RDecision',
              '可信当前域解析身份；精确应用操作；限制对象及来源披露；无实际操作'),
        route('/v1/tenant/authorization/audits', 'get', 'tenantListAudits', '租户审计', 'TENANT',
              'iam-tenant:audit:read', None, 'RPageResponseResourceDetailAuditEntry',
              '审计脱敏；不记录凭证明文或敏感原值', query=PAGE),
    ]
    routes += delegation_routes('TENANT', '/v1/tenant', 'iam-tenant')
    retained = '保留既有领域实现；平台精确 ACTION；不改存储语义'
    account_exec = '平台账号与成员分离；不返回组织关系；锁定改密启停复用安全用例'
    routes += [
        route('/v1/platform/accounts', 'get', 'platformListAccounts', '全局账号列表', 'PLATFORM',
              'iam-platform:account:read', None, 'RPageResponseResourceDetailAccountRecord', account_exec, query=PAGE),
        route('/v1/platform/accounts', 'post', 'platformCreateAccount', '创建全局账号', 'PLATFORM',
              'iam-platform:account:create', 'AccountCreateInput', 'RCreatedResource', account_exec),
        route('/v1/platform/accounts/lookup', 'post', 'platformLookupAccount', '精确查找全局账号', 'PLATFORM',
              'iam-platform:account:lookup', 'AccountLookupInput', 'RResourceDetailAccountRecord',
              '必须声明 purpose；MEMBER_CREATE 只返回 id 与登录名'),
        route('/v1/platform/accounts/{id}', 'get', 'platformGetAccount', '全局账号详情', 'PLATFORM',
              'iam-platform:account:read', None, 'RResourceDetailAccountRecord', account_exec),
        route('/v1/platform/accounts/{id}', 'patch', 'platformPatchAccount', '更新全局账号资料', 'PLATFORM',
              'iam-platform:account:update', 'AccountUpdateInput', 'RResourceDetailAccountRecord', account_exec),
        route('/v1/platform/accounts/{id}', 'delete', 'platformDeleteAccount', '删除全局账号', 'PLATFORM',
              'iam-platform:account:delete', 'VersionInput', 'RCreatedResource', '仍有成员资格时 ObjectInUse'),
        route('/v1/platform/accounts/{id}/enable', 'post', 'platformEnableAccount', '启用全局账号', 'PLATFORM',
              'iam-platform:account:enable', 'VersionInput', 'RCreatedResource', account_exec),
        route('/v1/platform/accounts/{id}/disable', 'post', 'platformDisableAccount', '停用全局账号', 'PLATFORM',
              'iam-platform:account:disable', 'VersionInput', 'RCreatedResource', account_exec),
        route('/v1/platform/accounts/{id}/lock', 'post', 'platformLockAccount', '锁定全局账号', 'PLATFORM',
              'iam-platform:account:lock', 'AccountLockInput', 'RCreatedResource', account_exec),
        route('/v1/platform/accounts/{id}/unlock', 'post', 'platformUnlockAccount', '解锁全局账号', 'PLATFORM',
              'iam-platform:account:unlock', 'VersionInput', 'RCreatedResource', account_exec),
        route('/v1/platform/accounts/{id}/reset-password', 'post', 'platformResetAccountPassword', '重置全局账号密码', 'PLATFORM',
              'iam-platform:account:reset-password', 'VersionInput', 'RAccountSecret',
              '仅重置返回一次性明文初始密码'),
        route('/v1/platform/dictionaries', 'get', 'platformListDictionaries', '平台字典', 'PLATFORM',
              'iam-platform:dictionary:read', None, 'RJson', retained,
              query=[{'name': 'view', 'required': False, 'schema': {'type': 'string', 'enum': ['tree', 'page', 'items'], 'default': 'page'}},
                     {'name': 'code', 'required': False, 'schema': {'type': 'string'}}] + MYBATIS_PAGE),
        route('/v1/platform/dictionaries', 'post', 'platformCreateDictionary', '创建平台字典', 'PLATFORM',
              'iam-platform:dictionary:create', None, 'RVoid', retained),
        route('/v1/platform/dictionaries/sort', 'put', 'platformSortDictionaries', '批量排序字典', 'PLATFORM',
              'iam-platform:dictionary:update', None, 'RVoid', retained),
        route('/v1/platform/dictionaries/{id}', 'put', 'platformPutDictionary', '更新平台字典', 'PLATFORM',
              'iam-platform:dictionary:update', None, 'RVoid', retained),
        route('/v1/platform/dictionaries/{id}', 'delete', 'platformDeleteDictionary', '删除平台字典', 'PLATFORM',
              'iam-platform:dictionary:delete', None, 'RVoid', retained),
        route('/v1/platform/dictionaries/{id}/status/{status}', 'patch', 'platformPatchDictionaryStatus', '切换字典状态', 'PLATFORM',
              'iam-platform:dictionary:update', None, 'RVoid', retained),
        route('/v1/platform/id-allocations', 'get', 'platformListIdAllocations', '发号配置列表', 'PLATFORM',
              'iam-platform:id-allocation:read', None, 'RJson', retained, query=MYBATIS_PAGE),
        route('/v1/platform/id-allocations', 'post', 'platformCreateIdAllocation', '创建发号配置', 'PLATFORM',
              'iam-platform:id-allocation:create', None, 'RVoid', retained),
        route('/v1/platform/id-allocations/{id}', 'put', 'platformPutIdAllocation', '更新发号配置', 'PLATFORM',
              'iam-platform:id-allocation:update', None, 'RVoid', retained),
        route('/v1/platform/id-allocations/{id}', 'delete', 'platformDeleteIdAllocation', '删除发号配置', 'PLATFORM',
              'iam-platform:id-allocation:delete', None, 'RVoid', retained),
        route('/v1/platform/social-configs', 'get', 'platformListSocialConfigs', '社会化登录配置列表', 'PLATFORM',
              'iam-platform:social-config:read', None, 'RJson', retained, query=MYBATIS_PAGE),
        route('/v1/platform/social-configs', 'post', 'platformCreateSocialConfig', '创建社会化登录配置', 'PLATFORM',
              'iam-platform:social-config:create', None, 'RVoid', retained),
        route('/v1/platform/social-configs/{id}', 'put', 'platformPutSocialConfig', '更新社会化登录配置', 'PLATFORM',
              'iam-platform:social-config:update', None, 'RVoid', retained),
        route('/v1/platform/social-configs/{id}', 'delete', 'platformDeleteSocialConfig', '删除社会化登录配置', 'PLATFORM',
              'iam-platform:social-config:delete', None, 'RVoid', retained),
    ]
    for item in routes:
        item['implemented'] = True
    seen = set()
    for item in routes:
        key = (item['path'], item['method'])
        if key in seen:
            raise SystemExit(f'duplicate route {key}')
        seen.add(key)
    (CONTRACT / 'routes.json').write_text(json.dumps(routes, ensure_ascii=False, indent=2) + '\n')
    print(f'{len({item["path"] for item in routes})} paths, {len(routes)} operations')


if __name__ == '__main__':
    build()
