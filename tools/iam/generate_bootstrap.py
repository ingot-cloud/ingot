#!/usr/bin/env python3
"""生成 IAM 正式冷启动种子 databases/iam/006_bootstrap.sql。

IAM操作由 contracts/routes.json 推导，开发者补充目录对齐现有Auth契约及页面；
菜单树按 change 的 FRONTEND.md 第 1 节维护；治理角色授予本域全部 ACTION。种子不含任何
凭证，平台首个账号由 provider 侧的冷启动器经安全框架用例创建。
"""
import argparse
import json
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
CHANGE = ROOT / 'specs/changes/active/20260912-iam-identity-access-management'
ROUTES = CHANGE / 'contracts/routes.json'
TARGET = ROOT / 'databases/iam/006_bootstrap.sql'

# 静态保留标识区间，必须小于发号器起点，避免与运行时发号冲突。
LEAF_TAG = 'iam'
LEAF_MAX_ID = 1000000
LEAF_STEP = 100
APPLICATION_BASE = 100000
RESOURCE_BASE = 110000
ACTION_BASE = 120000
# 新增操作固定保留号；避免改变历史目录操作的标识。
ADDITIONAL_ACTION_IDS = {
    'iam-tenant:department:set-leaders': 120999,
    'iam-tenant:department:export': 121200,
}
MENU_BASE = 130000
ROLE_BASE = 140000
REVISION_BASE = 141000
POLICY_BASE = 150000
DIRECTORY_VIEW_PATH = 'layout.main'
# 二级及以下带子集的目录走简单布局，避免再套一层完整壳。
NESTED_DIRECTORY_VIEW_PATH = 'layout.simple'
DEVELOPER_APPLICATION = 'platform-developer'
SECURITY_APPLICATION = 'platform-security'
MEMBER_APPLICATION = 'platform-member'
SECURITY_RESOURCES = {'credential-policy', 'account-lockout-policy', 'login-failure-policy',
                      'session', 'session-concurrency-policy', 'access-policy', 'block-event'}

APPLICATIONS = {
    'iam-platform': {'id': APPLICATION_BASE + 1, 'domain': 'PLATFORM', 'name': '平台治理', 'icon': 'ep:monitor',
                     'description': '平台域身份、目录与授权治理', 'sort_order': 1, 'baseline': False},
    'iam-tenant': {'id': APPLICATION_BASE + 2, 'domain': 'TENANT', 'name': '组织治理', 'icon': 'ep:office-building',
                   'description': '组织域成员、部门与授权治理', 'sort_order': 1, 'baseline': True},
    DEVELOPER_APPLICATION: {'id': APPLICATION_BASE + 3, 'domain': 'PLATFORM', 'name': '开发者平台', 'icon': 'ep:cpu',
                            'description': '二维码、OAuth2客户端、社交配置与业务ID管理',
                            'sort_order': 2, 'baseline': False},
    SECURITY_APPLICATION: {'id': APPLICATION_BASE + 4, 'domain': 'PLATFORM', 'name': '安全中心', 'icon': 'ep:lock', 'description': '凭证、账号、访问防护及会话安全管理', 'sort_order': 3, 'baseline': False},
    MEMBER_APPLICATION: {'id': APPLICATION_BASE + 5, 'domain': 'PLATFORM', 'name': 'App 用户管理',
                         'icon': 'ep:iphone', 'description': '管理 App 用户、角色及权限',
                         'sort_order': 4, 'baseline': False},
}

# 治理角色：每个域恰好一个启用的 SYSTEM 角色，组织初始化依赖这一唯一性。
ROLE_CONSTANTS = (ROOT / 'ingot-framework/ingot-commons/src/main/java/com/ingot/framework/commons/constants/RoleConstants.java').read_text()
ROLE_PREFIX = re.search(r'PLATFORM_ROLE_CODE_PREFIX\s*=\s*"([^"]+)"', ROLE_CONSTANTS).group(1)
ADMIN_ROLE_CODE = ROLE_PREFIX + re.search(r'ROLE_ADMIN_CODE\s*=\s*PLATFORM_ROLE_CODE_PREFIX\s*\+\s*"([^"]+)"', ROLE_CONSTANTS).group(1)

GOVERNANCE_ROLES = {
    'PLATFORM': {'id': ROLE_BASE + 1, 'revision_id': REVISION_BASE + 1, 'code': ADMIN_ROLE_CODE,
                 'name': '超级管理员', 'description': '系统平台超级管理员，受控直接分配并覆盖新增平台操作', 'application': 'iam-platform'},
    'TENANT': {'id': ROLE_BASE + 2, 'revision_id': REVISION_BASE + 2, 'code': 'tenant-governance',
               'name': '组织治理系统角色', 'description': '组织治理的兼容系统角色', 'application': 'iam-tenant'},
}

DEFAULT_POLICIES = {
    'FIELD': POLICY_BASE + 2,
}

# 资源展示名与能力声明。范围能力约束管理面可选的 scope，字段能力约束字段策略可配置项。
MEMBER_SCOPES = ['ALL', 'SELF', 'MEMBER_DEPARTMENTS', 'MANAGED_DEPARTMENTS', 'OBJECT_SET']
DEPARTMENT_SCOPES = ['ALL', 'MANAGED_DEPARTMENTS', 'OBJECT_SET']
OBJECT_SCOPES = ['ALL', 'OBJECT_SET']
SINGLETON_SCOPES = ['ALL']

MEMBER_FIELDS = [
    {'key': 'displayName', 'label': '显示名', 'visibilities': ['HIDDEN', 'MASKED', 'FULL'],
     'editable': True, 'filterable': False, 'mask': {'kind': 'ALL'}},
    {'key': 'avatar', 'label': '头像', 'visibilities': ['HIDDEN', 'FULL'],
     'editable': True, 'filterable': False},
    {'key': 'phone', 'label': '手机号', 'visibilities': ['HIDDEN', 'MASKED', 'FULL'],
     'editable': True, 'filterable': True, 'mask': {'kind': 'PHONE'}},
    {'key': 'email', 'label': '邮箱', 'visibilities': ['HIDDEN', 'MASKED', 'FULL'],
     'editable': True, 'filterable': True, 'mask': {'kind': 'EMAIL'}},
]
PLATFORM_MEMBER_FIELDS = [dict(field, filterable=field['key'] == 'displayName') for field in MEMBER_FIELDS] + [
    {'key': key, 'label': label, 'visibilities': ['HIDDEN', 'FULL'], 'editable': False, 'filterable': False}
    for key, label in [('joinedAt', '加入时间'), ('lastLoginAt', '最后登录'), ('updatedAt', '更新时间')]
]
# 目录能力上限不等同默认筛选授权；已有租户策略保持原值。
TENANT_MEMBER_FIELDS = [dict(field, filterable=True) if field['key'] == 'displayName' else dict(field) for field in MEMBER_FIELDS]

RESOURCES = {
    'action': ('操作', OBJECT_SCOPES, []),
    'application': ('应用', OBJECT_SCOPES, []),
    'role-assignment': ('角色授权', OBJECT_SCOPES, []),
    'application-audience': ('应用可用人群', SINGLETON_SCOPES, []),
    'authorization-audit': ('审计记录', SINGLETON_SCOPES, []),
    'authorization-diagnosis': ('授权诊断', SINGLETON_SCOPES, []),
    'delegation': ('授权委派', OBJECT_SCOPES, []),
    'department': ('部门', DEPARTMENT_SCOPES, []),
    'application-entitlement': ('应用开通', OBJECT_SCOPES, []),
    'field-policy': ('字段策略', SINGLETON_SCOPES, []),
    'group': ('用户组', OBJECT_SCOPES, []),
    'member': ('成员', MEMBER_SCOPES, MEMBER_FIELDS),
    'menu': ('菜单', OBJECT_SCOPES, []),
    'plan': ('套餐', OBJECT_SCOPES, []),
    'resource': ('资源', OBJECT_SCOPES, []),
    'role': ('角色', OBJECT_SCOPES, []),
    'settings': ('组织设置', SINGLETON_SCOPES, []),
    'shared-role': ('共享角色', OBJECT_SCOPES, []),
    'tenant': ('组织', OBJECT_SCOPES, []),
    'account': ('全局账号', OBJECT_SCOPES, []),
    'dictionary': ('字典', SINGLETON_SCOPES, []),
    'id-allocation': ('发号', SINGLETON_SCOPES, []),
    'social-config': ('社会化登录配置', SINGLETON_SCOPES, []),
    'account-lockout-policy': ('账号锁定策略', SINGLETON_SCOPES, []),
    'credential-policy': ('凭证策略', SINGLETON_SCOPES, []),
    'login-failure-policy': ('登录失败防护', SINGLETON_SCOPES, []),
    'session': ('在线会话', SINGLETON_SCOPES, []),
    'session-concurrency-policy': ('会话并发策略', SINGLETON_SCOPES, []),
    'access-policy': ('安全策略', SINGLETON_SCOPES, []),
    'oauth-client': ('客户端', SINGLETON_SCOPES, []),
    'qr-code': ('二维码', SINGLETON_SCOPES, []),
    'workbench': ('组织工作台', SINGLETON_SCOPES, []),
    'block-event': ('历史封禁记录', SINGLETON_SCOPES, []),
}

# Auth注解与原页面使用的精确码；不得改码或用通配码代替。
# 社交/发号的IAM操作仍保留现有API码，但目录归属由元数据显式指定。
DEVELOPER_ACTIONS = {
    'oauth-client': {
        'platform-developer:oauth-client:read-list': '查看客户端',
        'platform-developer:oauth-client:read-detail': '查看客户端详情',
        'platform-developer:oauth-client:create': '创建客户端',
        'platform-developer:oauth-client:update': '编辑客户端',
        'platform-developer:oauth-client:delete': '删除客户端',
        'platform-developer:oauth-client:reset-secret': '重置客户端密钥',
    },
    'qr-code': {'platform-developer:qr-code:generate': '生成二维码'},
}

# ACTION 展示名由动词加资源名合成，保证同一操作码在两个域下命名一致。
VERBS = {
    'create': '创建',
    'delete': '删除',
    'set-departments': '调整任职',
    'set-leaders': '设置部门负责人',
    'run': '诊断',
    'export': '导出',
    'transfer-owner': '转交所有者',
    'preview': '预览',
    'publish-version': '发布版本',
    'read': '查看',
    'remove': '移出',
    'change-status': '启停',
    'update': '编辑',
    'apply-version': '升级',
    'lookup': '查找',
    'enable': '启用',
    'disable': '停用',
    'lock': '锁定',
    'unlock': '解锁',
    'reset-password': '重置密码',
    'revoke': '撤销',
    'purge': '强制清除',
    'refresh': '刷新',
}

# 无宾语动词直接作为展示名，避免出现「转交所有者组织设置」这类拼接。

STANDALONE_VERBS = {'transfer-owner', 'set-departments', 'set-leaders', 'run', 'reset-password', 'revoke'}

MEMBER_ACTIONS = {
    'user': {
        f'{MEMBER_APPLICATION}:user:read-list': '查看用户列表',
        f'{MEMBER_APPLICATION}:user:read-detail': '查看用户详情',
        f'{MEMBER_APPLICATION}:user:create': '创建 App 用户',
        f'{MEMBER_APPLICATION}:user:update': '编辑 App 用户',
        f'{MEMBER_APPLICATION}:user:delete': '删除用户',
        f'{MEMBER_APPLICATION}:user:reset-password': '重置用户密码',
        f'{MEMBER_APPLICATION}:user:read-roles': '查看用户角色',
        f'{MEMBER_APPLICATION}:user:set-roles': '配置用户角色',
        f'{MEMBER_APPLICATION}:user:lock': '锁定 App 用户',
        f'{MEMBER_APPLICATION}:user:unlock': '解锁 App 用户',
        f'{MEMBER_APPLICATION}:user:enable': '启用 App 用户',
        f'{MEMBER_APPLICATION}:user:disable': '停用 App 用户',
    },
    'role': {
        f'{MEMBER_APPLICATION}:role:read': '查看角色',
        f'{MEMBER_APPLICATION}:role:create': '创建 App 角色',
        f'{MEMBER_APPLICATION}:role:update': '编辑 App 角色',
        f'{MEMBER_APPLICATION}:role:change-status': '启停 App 角色',
        f'{MEMBER_APPLICATION}:role:delete': '删除角色',
        f'{MEMBER_APPLICATION}:role:assign-users': '分配角色用户',
        f'{MEMBER_APPLICATION}:role:read-permissions': '查看角色权限',
        f'{MEMBER_APPLICATION}:role:set-permissions': '配置角色权限',
    },
    'permission': {
        f'{MEMBER_APPLICATION}:permission:read': '查看权限',
        f'{MEMBER_APPLICATION}:permission:create': '创建 App 权限',
        f'{MEMBER_APPLICATION}:permission:update': '编辑 App 权限',
        f'{MEMBER_APPLICATION}:permission:change-status': '启停 App 权限',
        f'{MEMBER_APPLICATION}:permission:delete': '删除权限',
    },
}

MEMBER_RESOURCES = {
    'user': ('App 用户', SINGLETON_SCOPES, []),
    'role': ('App 角色', SINGLETON_SCOPES, []),
    'permission': ('App 权限', SINGLETON_SCOPES, []),
}

# 菜单树按 change 的 FRONTEND.md 第 1 节：平台与组织各最多四个一级目录。
# view_path 为前端 definePluginPages 生成的 canonical 键，path 为浏览器路由。
# 目录节点携带其子页 ACTION 的并集并使用 ANY，子页全部不可见时目录随之隐藏。
# 一级目录使用两管理台已离线打包的 ep 图标集。
PLATFORM_MENUS = [
    ('租户管理', 'platform.iam.tenant', 'ep:office-building', [
        ('租户列表', 'platform.iam.tenants', ['iam-platform:tenant:read']),
    ]),
    ('应用与配置', 'platform.iam.config', 'ep:setting', [
        ('应用目录', 'platform.iam.applications', ['iam-platform:application:read']),
        ('租户套餐', 'platform.iam.plans', ['iam-platform:plan:read']),
        ('共享角色', 'platform.iam.shared.roles', ['iam-platform:shared-role:read']),
    ]),
    ('平台管理', 'platform.iam.manage', 'ep:monitor', [
        ('全局账号', 'platform.iam.accounts', ['iam-platform:account:read']),
        ('平台成员与用户组', 'platform.iam.personnel', ['iam-platform:member:read', 'iam-platform:group:read']),
        ('角色与授权', 'platform.iam.authorization',
         ['iam-platform:role:read', 'iam-platform:role-assignment:read', 'iam-platform:delegation:read', 'iam-platform:authorization-diagnosis:run']),
        ('平台授权审计', 'platform.iam.authorization.audit', ['iam-platform:authorization-audit:read']),
    ]),
    ('系统设置', 'platform.settings', 'ep:setting', [
        ('字典管理', 'platform.settings.dictionaries', ['iam-platform:dictionary:read']),
    ]),
]

TENANT_MENUS = [
    ('组织管理', 'org.iam.organization', 'ep:office-building', [
        ('成员与部门', 'org.iam.members.root', '', [
            ('成员', 'org.iam.members', ['iam-tenant:member:read']),
            ('部门', 'org.iam.departments', ['iam-tenant:department:read']),
        ]),
        ('用户组', 'org.iam.groups', ['iam-tenant:group:read']),
        ('组织设置', 'org.iam.settings', ['iam-tenant:settings:read']),
    ]),
    ('权限与应用', 'org.iam.authorization.root', 'ep:connection', [
        ('角色与授权', 'org.iam.authorization',
         ['iam-tenant:role:read', 'iam-tenant:role-assignment:read', 'iam-tenant:delegation:read', 'iam-tenant:authorization-diagnosis:run']),
        ('已开通应用', 'org.iam.applications', ['iam-tenant:application:read']),
    ]),
    ('安全与合规', 'org.iam.compliance', 'ep:lock', [
        ('资源字段策略', 'org.iam.field.policies',
         ['iam-tenant:field-policy:read']),
        ('组织授权审计', 'org.iam.authorization.audit', ['iam-tenant:authorization-audit:read']),
    ]),
    ('工作台', 'org.iam.workspace', 'ep:grid', [
        ('组织概览', 'org.iam.workbench', ['iam-tenant:workbench:read']),
    ]),
]

DEVELOPER_MENUS = [
    ('开发者平台', 'platform.develop', 'ep:cpu', [
        ('生成二维码', 'platform.develop.qrcode', ['platform-developer:qr-code:generate']),
        ('OAuth 2.0 客户端', 'platform.develop.client', ['platform-developer:oauth-client:read-list']),
        ('社会化登录配置', 'platform.develop.social', ['platform-developer:social-config:read']),
        ('业务 ID 发号', 'platform.develop.id', ['platform-developer:id-allocation:read']),
    ]),
]

SECURITY_MENUS = [('安全中心', 'security.root', 'ep:lock', [
    ('凭证策略', 'security.credential.policies', ['platform-security:credential-policy:read']),
    ('账号保护', 'security.account.protection', ['platform-security:account-lockout-policy:read']),
    ('访问防护', 'security.access.protection', ['platform-security:access-policy:read', 'platform-security:login-failure-policy:read', 'platform-security:block-event:read']),
    ('会话管理', 'security.sessions', ['platform-security:session:read', 'platform-security:session-concurrency-policy:read']),
])]

MEMBER_MENUS = [('App 用户管理', 'member.root', 'ep:iphone', [
    ('App 用户', 'member.user', [f'{MEMBER_APPLICATION}:user:read-list']),
    ('App 角色', 'member.role', [f'{MEMBER_APPLICATION}:role:read']),
    ('App 权限', 'member.permission', [f'{MEMBER_APPLICATION}:permission:read']),
])]

PAGE_ICONS = {'member.user': 'ep:user', 'member.role': 'ep:avatar', 'member.permission': 'ep:key'}

DIRECTORY_PATHS = {
    'member.root': '/member',
    'security.root': '/security',
    'platform.settings': '/platform/settings',
    'org.iam.members.root': '/org/iam/members/root',
}

PAGE_PATHS = {
    'member.user': '/member/users',
    'member.role': '/member/roles',
    'member.permission': '/member/permissions',
    'platform.settings.dictionaries': '/platform/settings/dictionaries',
    'security.credential.policies': '/security/credential-policies',
    'security.account.protection': '/security/account-protection',
    'security.access.protection': '/security/access-protection',
    'security.sessions': '/security/sessions',
    'platform.iam.authorization.audit': '/platform/authorization-audit',
    'org.iam.authorization.audit': '/org/authorization-audit',
    'org.iam.field.policies': '/org/field-policies',
}

MENUS = {'iam-platform': PLATFORM_MENUS, 'iam-tenant': TENANT_MENUS,
         DEVELOPER_APPLICATION: DEVELOPER_MENUS, SECURITY_APPLICATION: SECURITY_MENUS,
         MEMBER_APPLICATION: MEMBER_MENUS}

# 仅覆盖展示顺序，保留种子生成顺序及既有保留 ID。
ROOT_MENU_SORT_ORDERS = {'org.iam.workspace': 0}


def quote(value):
    """转义 SQL 字符串字面量。"""
    return "'" + value.replace('\\', '\\\\').replace("'", "''") + "'"


def json_literal(value):
    """把 Python 结构写成 MySQL 可解析的 JSON 字符串字面量。"""
    return "CAST(" + quote(json.dumps(value, ensure_ascii=False)) + " AS JSON)"


def menu_path(view_key):
    """由 canonical 视图键推导默认浏览器路由，与前端 toDefaultMenuPath 一致。"""
    return PAGE_PATHS.get(view_key, DIRECTORY_PATHS.get(view_key, '/' + view_key.replace('.', '/')))


def route_name(view_key):
    """路由名称使用 canonical 视图键，全局唯一且与前端命名路由一致。"""
    return view_key


RESOURCE_NAMES = {
    'iam-platform': {'account': '全局账号', 'action': '应用操作', 'application': '应用目录', 'role-assignment': '平台角色分配', 'authorization-audit': '平台授权审计', 'authorization-diagnosis': '平台权限诊断', 'delegation': '平台授权委派', 'dictionary': '字典', 'application-entitlement': '租户应用开通', 'group': '平台用户组', 'member': '平台成员', 'menu': '应用菜单', 'plan': '租户套餐', 'resource': '应用资源', 'role': '平台角色', 'shared-role': '共享角色', 'tenant': '租户'},
    'iam-tenant': {'application': '已开通应用', 'role-assignment': '组织角色分配', 'application-audience': '应用可用人群', 'authorization-audit': '组织授权审计', 'authorization-diagnosis': '组织权限诊断', 'delegation': '组织授权委派', 'department': '部门', 'field-policy': '资源字段策略', 'group': '组织用户组', 'member': '组织成员', 'role': '组织角色', 'settings': '组织设置', 'workbench': '组织工作台'},
    'platform-developer': {'oauth-client': 'OAuth 2.0 客户端', 'id-allocation': '业务 ID 发号', 'qr-code': '二维码', 'social-config': '社会化登录配置'},
    'platform-security': {'credential-policy': '凭证策略', 'account-lockout-policy': '账号锁定策略', 'login-failure-policy': '登录失败防护', 'access-policy': '访问防护策略', 'session': '在线会话', 'session-concurrency-policy': '会话并发策略', 'block-event': '历史封禁记录'},
    'platform-member': {'permission': 'App 权限', 'role': 'App 角色', 'user': 'App 用户'},
}


def action_name(application, resource, code):
    """按应用内资源显示名生成精确操作名称。"""
    for actions in (*DEVELOPER_ACTIONS.values(), *MEMBER_ACTIONS.values()):
        if code in actions:
            return actions[code]
    suffix = code.rsplit(':', 1)[-1]
    name = RESOURCE_NAMES[application][resource]
    if suffix == 'run':
        return '运行' + name
    if suffix == 'publish-version':
        return '发布' + name + '版本'
    if suffix == 'apply-version':
        return '应用角色版本'
    if suffix == 'reset-password':
        return '重置' + name + '密码'
    if suffix == 'update' and application == 'iam-tenant' and resource == 'role':
        return '保存组织角色权限'
    if suffix in STANDALONE_VERBS:
        return VERBS[suffix]
    return VERBS[suffix] + name


def collect(routes):
    """从现有执行契约推导归属，独立开发者目录不改写接口精确码。"""
    catalog = {code: {} for code in APPLICATIONS}
    for item in routes:
        code = item.get('action')
        if not code:
            continue
        application, resource, _ = code.split(':', 2)
        if application not in catalog:
            raise SystemExit(f'未登记的应用命名空间 {application}')
        if resource not in RESOURCES:
            raise SystemExit(f'未登记的资源 {resource}，请补充 RESOURCES')
        catalog[application].setdefault(resource, set()).add(code)
    action_source = (ROOT / 'ingot-framework/ingot-commons/src/main/java/com/ingot/framework/commons/model/iam/IamAction.java').read_text()
    for code in re.findall(r'\"(platform-security:[^\"]+)\"', action_source):
        resource = code.split(':')[1]
        if resource in SECURITY_RESOURCES:
            catalog[SECURITY_APPLICATION].setdefault(resource, set()).add(code)
    for resource, actions in DEVELOPER_ACTIONS.items():
        catalog[DEVELOPER_APPLICATION].setdefault(resource, set()).update(actions)
    for resource, actions in MEMBER_ACTIONS.items():
        catalog[MEMBER_APPLICATION][resource] = set(actions)
    return catalog


def reserved_id(table, preferred, low, high):
    """空库用预定保留号；该号已被占用则落到区间内下一个空号。

    目录按排序重算序号时，中间插入新行不能改写已有库的主键。子行仍按自然键
    解析父行，不依赖这里算出的标识。
    """
    return (f"COALESCE("
            f"(SELECT {preferred} FROM DUAL "
            f"WHERE NOT EXISTS (SELECT 1 FROM {table} taken WHERE taken.id = {preferred})), "
            f"(SELECT COALESCE(MAX(taken.id), {low}) + 1 FROM {table} taken "
            f"WHERE taken.id >= {low} AND taken.id < {high}))")


def is_directory_node(node):
    """四级元组是目录：名称、键、图标、子节点。"""
    return len(node) == 4 and isinstance(node[3], list)


def is_page_node(node):
    """三级元组是页面：名称、键、操作码。"""
    return len(node) == 3 and isinstance(node[2], list)


def collect_actions(nodes):
    """目录携带全部后代页面的操作并集。"""
    actions = set()
    for node in nodes:
        if is_page_node(node):
            actions.update(node[2])
        elif is_directory_node(node):
            actions.update(collect_actions(node[3]))
        else:
            raise SystemExit(f'无法识别的菜单节点 {node!r}')
    return actions


def insert(table, columns, values, exists, source='DUAL', where=None):
    """存在即跳过的插入语句，重复执行不覆盖任何既有行。

    父行一律由 source/where 按自然键解析，保留标识只用于本次新建的行；
    目标库已存在同编码但不同标识的父行时，子行跟随既有父行而不是硬编码标识。
    """
    conditions = ([where] if where else []) + [f"NOT EXISTS (SELECT 1 FROM {table} WHERE {exists})"]
    return (f"INSERT INTO {table} ({', '.join(columns)})\n"
            f"SELECT {', '.join(values)} FROM {source}\n"
            f"WHERE {' AND '.join(conditions)};\n")


def build(check=False):
    routes = json.loads(ROUTES.read_text())
    catalog = collect(routes)
    stable_ids = json.loads((ROOT / 'tools/iam/tenant_live/action_ids.json').read_text())
    stable_ids.update(ADDITIONAL_ACTION_IDS)

    lines = [
        '-- 业务时间点统一 UTC；初始化连接也必须明确会话时区。',
        "SET time_zone = '+00:00';",
        '',
        '-- IAM 正式冷启动种子：先按顺序执行 001–005 再执行本文件，可重复执行。',
        '-- 由 tools/iam/generate_bootstrap.py 从IAM契约及现有开发者目录生成，不要手工编辑。',
        '-- 全部语句存在即跳过，不覆盖任何人工或业务修改；不含账号与凭证，',
        '-- 新建行的保留号若已被占用，则落到所属区间内下一个空号。',
        '-- 平台首个账号由 provider 冷启动器经安全框架注册用例创建。',
        '',
        '-- 发号准备：静态保留标识全部小于起点，运行时发号不会与种子冲突。',
    ]
    lines.append(
        f"INSERT INTO fw_id_leaf_alloc (biz_tag, max_id, step, description)\n"
        f"SELECT {quote(LEAF_TAG)}, {LEAF_MAX_ID}, {LEAF_STEP}, {quote('IAM 冷启动发号')} FROM DUAL\n"
        f"WHERE NOT EXISTS (SELECT 1 FROM fw_id_leaf_alloc WHERE biz_tag = {quote(LEAF_TAG)});\n")

    lines.append('-- 应用：租户域基础应用在组织创建时缺省开通。')
    for code, meta in APPLICATIONS.items():
        lines.append(insert(
            'iam_application',
            ['id', 'code', 'domain', 'name', 'icon', 'description', 'sort_order', 'baseline', 'enabled'],
            [reserved_id('iam_application', meta['id'], APPLICATION_BASE, RESOURCE_BASE), quote(code), quote(meta['domain']), quote(meta['name']),
             quote(meta['icon']), quote(meta['description']), str(meta['sort_order']),
             'TRUE' if meta['baseline'] else 'FALSE', 'TRUE'],
            f"code = {quote(code)}"))

    resource_count = 0
    lines.append('-- 资源：声明合法范围与字段能力，供角色发布与字段策略校验取值。')
    for application in APPLICATIONS:
        for resource in sorted(catalog[application]):
            resource_count += 1
            resources = MEMBER_RESOURCES if application == MEMBER_APPLICATION else RESOURCES
            _, scopes, fields = resources[resource]
            name = RESOURCE_NAMES[application][resource]
            if resource == 'member': fields = PLATFORM_MEMBER_FIELDS if application == 'iam-platform' else TENANT_MEMBER_FIELDS
            if APPLICATIONS[application]['domain'] == 'PLATFORM':
                scopes = [scope for scope in scopes if scope not in ('MEMBER_DEPARTMENTS', 'MANAGED_DEPARTMENTS')]
            lines.append(insert(
                'iam_resource',
                ['id', 'application_id', 'code', 'name', 'scope_capabilities', 'field_capabilities',
                 'field_policy', 'enabled'],
                [reserved_id('iam_resource', RESOURCE_BASE + resource_count, RESOURCE_BASE, ACTION_BASE),
                 'app.id', quote(resource), quote(name),
                 json_literal(scopes), json_literal(fields),
                 'TRUE' if application == 'iam-tenant' and resource == 'member' else 'FALSE', 'TRUE'],
                f"application_id = app.id AND code = {quote(resource)}",
                source='iam_application app', where=f"app.code = {quote(application)}"))

    action_count = 0
    lines.append('-- 操作：精确操作码全局唯一，禁止通配。')
    for application in APPLICATIONS:
        for resource in sorted(catalog[application]):
            for code in sorted(catalog[application][resource]):
                action_count += 1
                lines.append(insert(
                    'iam_action',
                    ['id', 'application_id', 'resource_id', 'code', 'name', 'enabled'],
                    [reserved_id('iam_action', stable_ids.get(code,ACTION_BASE + action_count), ACTION_BASE, MENU_BASE),
                     'res.application_id', 'res.id', quote(code),
                     quote(action_name(application, resource, code)), 'TRUE'],
                    f"code = {quote(code)}",
                    source='iam_resource res JOIN iam_application app ON app.id = res.application_id',
                    where=f"app.code = {quote(application)} AND res.code = {quote(resource)}"))

    lines.append('-- 菜单：目录携带子页操作并集，子页全部不可见时目录随之隐藏。')
    menu_count = 0
    menu_links = []

    def emit_children(application, parent_key, children):
        nonlocal menu_count
        for child_order, child in enumerate(children, start=1):
            parent_source = ('iam_menu parent JOIN iam_application app ON app.id = parent.application_id')
            parent_where = (f"app.code = {quote(application)} "
                            f"AND parent.route_name = {quote(parent_key)}")
            if is_directory_node(child):
                name, key, icon, nested = child
                menu_count += 1
                actions = sorted(collect_actions(nested))
                lines.append(insert(
                    'iam_menu',
                    ['id', 'application_id', 'parent_id', 'name', 'path', 'view_path', 'route_name',
                     'icon', 'kind', 'match_mode', 'access_mode', 'sort_order', 'enabled'],
                    [reserved_id('iam_menu', MENU_BASE + menu_count, MENU_BASE, ROLE_BASE),
                     'parent.application_id', 'parent.id',
                     quote(name), quote(menu_path(key)), quote(NESTED_DIRECTORY_VIEW_PATH), quote(key),
                     quote(icon or ''), quote('DIRECTORY'), quote('ANY'), quote('ACTION'),
                     str(child_order), 'TRUE'],
                    f"application_id = parent.application_id AND route_name = {quote(key)}",
                    source=parent_source, where=parent_where))
                menu_links.extend((application, key, code) for code in actions)
                emit_children(application, key, nested)
                continue
            if not is_page_node(child):
                raise SystemExit(f'无法识别的菜单节点 {child!r}')
            page_name, view_key, codes = child
            menu_count += 1
            icon_columns = ['icon'] if view_key in PAGE_ICONS else []
            icon_values = [quote(PAGE_ICONS[view_key])] if icon_columns else []
            lines.append(insert(
                'iam_menu',
                ['id', 'application_id', 'parent_id', 'name', 'path', 'view_path', 'route_name',
                 *icon_columns, 'kind', 'match_mode', 'access_mode', 'sort_order', 'enabled'],
                [reserved_id('iam_menu', MENU_BASE + menu_count, MENU_BASE, ROLE_BASE),
                 'parent.application_id', 'parent.id',
                 quote(page_name), quote(menu_path(view_key)), quote(view_key), quote(view_key),
                 *icon_values, quote('PAGE'), quote('ANY'), quote('ACTION'), str(child_order), 'TRUE'],
                f"application_id = parent.application_id AND route_name = {quote(view_key)}",
                source=parent_source, where=parent_where))
            menu_links.extend((application, view_key, code) for code in sorted(set(codes)))

    for application, tree in MENUS.items():
        for order, (directory_name, directory_key, directory_icon, pages) in enumerate(tree, start=1):
            menu_count += 1
            directory_actions = sorted(collect_actions(pages))
            lines.append(insert(
                'iam_menu',
                ['id', 'application_id', 'parent_id', 'name', 'path', 'view_path', 'route_name',
                 'icon', 'kind', 'match_mode', 'access_mode', 'sort_order', 'enabled'],
                [reserved_id('iam_menu', MENU_BASE + menu_count, MENU_BASE, ROLE_BASE),
                 'app.id', 'NULL', quote(directory_name),
                 quote(menu_path(directory_key)), quote(DIRECTORY_VIEW_PATH), quote(directory_key),
                 quote(directory_icon), quote('DIRECTORY'), quote('ANY'), quote('ACTION'),
                 str(ROOT_MENU_SORT_ORDERS.get(directory_key, order)), 'TRUE'],
                f"application_id = app.id AND route_name = {quote(directory_key)}",
                source='iam_application app', where=f"app.code = {quote(application)}"))
            menu_links += [(application, directory_key, code) for code in directory_actions]
            emit_children(application, directory_key, pages)

    lines.append('-- 菜单与操作关联：ACTION 访问模式下缺少关联的菜单一律不可见。')
    known = {}
    for application in catalog:
        for codes in catalog[application].values():
            for code in codes:
                if code in known:
                    raise SystemExit(f'重复操作码 {code}')
                known[code] = application
    for application, menu_key, code in menu_links:
        if code not in known:
            raise SystemExit(f'菜单引用了目录中不存在的操作 {code}')
        if known[code] != application:
            raise SystemExit(f'菜单 {menu_key} 跨应用引用操作 {code}')
        lines.append(insert(
            'iam_menu_action',
            ['application_id', 'menu_id', 'action_id'],
            ['menu.application_id', 'menu.id', 'action.id'],
            'application_id = menu.application_id AND menu_id = menu.id AND action_id = action.id',
            source='iam_menu menu JOIN iam_application app ON app.id = menu.application_id '
                   'JOIN iam_action action ON action.application_id = menu.application_id',
            where=f"app.code = {quote(application)} AND menu.route_name = {quote(menu_key)} "
                  f"AND action.code = {quote(code)}"))

    lines.append('CREATE TEMPORARY TABLE IF NOT EXISTS iam_bootstrap_new_revision (domain VARCHAR(16) PRIMARY KEY);')
    lines.append('DELETE FROM iam_bootstrap_new_revision;')
    for domain, role in GOVERNANCE_ROLES.items():
        lines.append(f"INSERT INTO iam_bootstrap_new_revision(domain) SELECT {quote(domain)} FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_role_revision v JOIN iam_role_definition r ON r.id=v.role_id WHERE r.domain={quote(domain)} AND r.code={quote(role['code'])} AND r.tenant_key=0 AND v.revision=1);")
    lines.append('-- 治理角色：每个域恰好一个启用的 SYSTEM 角色，组织初始化依赖这一唯一性。')
    for domain, role in GOVERNANCE_ROLES.items():
        lines.append(insert(
            'iam_role_definition',
            ['id', 'domain', 'tenant_id', 'kind', 'code', 'name', 'description', 'enabled'],
            [str(role['id']), quote(domain), 'NULL', quote('SYSTEM'), quote(role['code']),
             quote(role['name']), quote(role['description']), 'TRUE'],
            f"domain = {quote(domain)} AND tenant_key = 0 AND code = {quote(role['code'])}"))
        lines.append(insert(
            'iam_role_revision',
            ['id', 'role_id', 'kind', 'revision', 'base_revision_id', 'metadata_overrides', 'resource_field_permissions'],
            [str(role['revision_id']), 'role.id', quote('SYSTEM'), '1', 'NULL', json_literal({}),
             "(SELECT JSON_OBJECT(CAST(resource.id AS CHAR), " + json_literal({
                 'visibility': {field['key']: 'FULL' for field in PLATFORM_MEMBER_FIELDS},
                 'operations': {field['key']: {'editable': field['editable'], 'filterable': field['filterable']}
                                for field in PLATFORM_MEMBER_FIELDS}}) + ") FROM iam_resource resource JOIN iam_application app ON app.id=resource.application_id WHERE app.code='iam-platform' AND resource.code='member')"
             if domain == 'PLATFORM' else json_literal({})],
            'role_id = role.id AND revision = 1',
            source='iam_role_definition role',
            where=f"role.domain = {quote(domain)} AND role.tenant_key = 0 "
                  f"AND role.code = {quote(role['code'])}"))

    lines.append('-- 治理授权：治理角色覆盖本域全部操作，范围为全域。')
    for domain, role in GOVERNANCE_ROLES.items():
        codes = sorted(code for application in catalog if APPLICATIONS[application]['domain'] == domain
                       for resource in catalog[application] for code in catalog[application][resource])
        for code in codes:
            lines.append(insert(
                'iam_role_grant',
                ['revision_id', 'action_id', 'scopes'],
                ['revision.id', 'action.id', json_literal([{'kind': 'ALL'}])],
                'revision_id = revision.id AND action_id = action.id',
                source='iam_role_revision revision '
                       'JOIN iam_role_definition role ON role.id = revision.role_id '
                       'JOIN iam_action action '
                       'JOIN iam_application app ON app.id = action.application_id',
                where=f"role.domain = {quote(domain)} AND role.tenant_key = 0 "
                      f"AND role.code = {quote(role['code'])} AND revision.revision = 1 "
                      f"AND app.domain = role.domain AND action.code = {quote(code)} "
                      f"AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain)"))

    lines.append('-- 默认策略版本：固定版本被租户字段策略引用；字段默认脱敏手机邮箱，上限缺省不额外收紧。')
    field_default = {
        'fields': {item['key']: 'MASKED' if item['key'] in ('phone', 'email') else 'FULL' for item in MEMBER_FIELDS},
        'operations': {item['key']: {'editable': True, 'filterable': item['filterable']} for item in MEMBER_FIELDS},
    }
    definitions = {
        'FIELD': field_default,
    }
    for kind, policy_id in DEFAULT_POLICIES.items():
        lines.append(insert(
            'iam_default_policy_revision',
            ['id', 'resource_id', 'kind', 'revision', 'definition'],
            [str(policy_id), 'resource.id', quote(kind), '1', json_literal(definitions[kind])],
            f"resource_id = resource.id AND kind = {quote(kind)} AND revision = 1",
            source='iam_resource resource JOIN iam_application app ON app.id = resource.application_id',
            where="app.domain = 'TENANT' AND app.code = 'iam-tenant' AND resource.code = 'member'"))

    content = '\n'.join(lines)
    if check:
        if not TARGET.is_file() or TARGET.read_text() != content:
            raise SystemExit('006_bootstrap.sql differs; run python3 tools/iam/generate_database.py')
    else:
        TARGET.write_text(content)
    print(f'{len(APPLICATIONS)} applications, {resource_count} resources, {action_count} actions, '
          f'{menu_count} menus, {len(menu_links)} menu actions, '
          f'{sum(1 for app in catalog for resource in catalog[app] for _ in catalog[app][resource])} grants')


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--check', action='store_true', help='只读核对正式种子')
    build(check=parser.parse_args().check)
