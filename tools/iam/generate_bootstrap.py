#!/usr/bin/env python3
"""生成 IAM 正式冷启动种子 databases/iam/006_bootstrap.sql。

应用、资源与 ACTION 目录由 contracts/routes.json 推导，菜单树按 change 的
FRONTEND.md 第 1 节维护在本文件；治理角色授予本域全部 ACTION。种子不含任何
凭证，平台首个账号由 provider 侧的冷启动器经安全框架用例创建。
"""
import json
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
MENU_BASE = 130000
ROLE_BASE = 140000
REVISION_BASE = 141000
POLICY_BASE = 150000

APPLICATIONS = {
    'iam-platform': {'id': APPLICATION_BASE + 1, 'domain': 'PLATFORM', 'name': '平台治理',
                     'description': '平台域身份、目录与授权治理', 'sort_order': 1, 'baseline': False},
    'iam-tenant': {'id': APPLICATION_BASE + 2, 'domain': 'TENANT', 'name': '组织治理',
                   'description': '组织域成员、部门与授权治理', 'sort_order': 1, 'baseline': True},
}

# 治理角色：每个域恰好一个启用的 SYSTEM 角色，组织初始化依赖这一唯一性。
GOVERNANCE_ROLES = {
    'PLATFORM': {'id': ROLE_BASE + 1, 'revision_id': REVISION_BASE + 1, 'code': 'platform-governance',
                 'name': '平台治理', 'description': '平台域全部治理操作', 'application': 'iam-platform'},
    'TENANT': {'id': ROLE_BASE + 2, 'revision_id': REVISION_BASE + 2, 'code': 'tenant-governance',
               'name': '组织治理', 'description': '组织所有者的全部治理操作', 'application': 'iam-tenant'},
}

DEFAULT_POLICIES = {
    'DIRECTORY': POLICY_BASE + 1,
    'FIELD': POLICY_BASE + 2,
}

# 资源展示名与能力声明。范围能力约束管理面可选的 scope，字段能力约束字段策略可配置项。
MEMBER_SCOPES = ['ALL', 'SELF', 'MEMBER_DEPARTMENTS', 'MANAGED_DEPARTMENTS', 'OBJECT_SET']
DEPARTMENT_SCOPES = ['ALL', 'MANAGED_DEPARTMENTS', 'OBJECT_SET']
OBJECT_SCOPES = ['ALL', 'OBJECT_SET']
SINGLETON_SCOPES = ['ALL']

MEMBER_FIELDS = [
    {'key': 'displayName', 'label': '显示名', 'visibilities': ['HIDDEN', 'MASKED', 'FULL'],
     'editable': True, 'filterable': True, 'sortable': True},
    {'key': 'avatar', 'label': '头像', 'visibilities': ['HIDDEN', 'FULL'],
     'editable': True, 'filterable': False, 'sortable': False},
    {'key': 'phone', 'label': '手机号', 'visibilities': ['HIDDEN', 'MASKED', 'FULL'],
     'editable': True, 'filterable': True, 'sortable': False},
    {'key': 'email', 'label': '邮箱', 'visibilities': ['HIDDEN', 'MASKED', 'FULL'],
     'editable': True, 'filterable': True, 'sortable': False},
]

RESOURCES = {
    'action': ('操作', OBJECT_SCOPES, []),
    'application': ('应用', OBJECT_SCOPES, []),
    'assignment': ('角色授权', OBJECT_SCOPES, []),
    'audience': ('应用可用人群', SINGLETON_SCOPES, []),
    'audit': ('审计记录', SINGLETON_SCOPES, []),
    'authorization': ('授权诊断', SINGLETON_SCOPES, []),
    'delegation': ('授权委派', OBJECT_SCOPES, []),
    'department': ('部门', DEPARTMENT_SCOPES, []),
    'directory': ('通讯录', MEMBER_SCOPES, MEMBER_FIELDS),
    'directory-policy': ('通讯录可见范围策略', SINGLETON_SCOPES, []),
    'entitlement': ('应用开通', OBJECT_SCOPES, []),
    'field-policy': ('成员字段策略', SINGLETON_SCOPES, []),
    'group': ('用户组', OBJECT_SCOPES, []),
    'member': ('成员', MEMBER_SCOPES, MEMBER_FIELDS),
    'menu': ('菜单', OBJECT_SCOPES, []),
    'plan': ('套餐', OBJECT_SCOPES, []),
    'policy': ('策略预览', SINGLETON_SCOPES, []),
    'resource': ('资源', OBJECT_SCOPES, []),
    'role': ('角色', OBJECT_SCOPES, []),
    'settings': ('组织设置', SINGLETON_SCOPES, []),
    'shared-role': ('共享角色', OBJECT_SCOPES, []),
    'tenant': ('组织', OBJECT_SCOPES, []),
    'account': ('全局账号', OBJECT_SCOPES, []),
    'dictionary': ('字典', SINGLETON_SCOPES, []),
    'id-allocation': ('发号', SINGLETON_SCOPES, []),
    'social-config': ('社会化登录配置', SINGLETON_SCOPES, []),
    'lockout-policy': ('账号锁定策略', SINGLETON_SCOPES, []),
    'credential-policy': ('凭证策略', SINGLETON_SCOPES, []),
    'login-failure-policy': ('登录失败防护', SINGLETON_SCOPES, []),
    'session': ('在线会话', SINGLETON_SCOPES, []),
    'session-policy': ('会话并发策略', SINGLETON_SCOPES, []),
    'security-policy': ('安全策略', SINGLETON_SCOPES, []),
}

# ACTION 展示名由动词加资源名合成，保证同一操作码在两个域下命名一致。
VERBS = {
    'create': '创建',
    'delete': '删除',
    'departments': '调整任职',
    'diagnose': '诊断',
    'export': '导出',
    'owner-transfer': '转交所有者',
    'preview': '预览',
    'publish': '发布版本',
    'read': '查看',
    'remove': '移出',
    'status': '启停',
    'update': '编辑',
    'upgrade': '升级',
    'lookup': '查找',
    'enable': '启用',
    'disable': '停用',
    'lock': '锁定',
    'unlock': '解锁',
    'reset-password': '重置密码',
    'revoke': '撤销',
}

# 无宾语动词直接作为展示名，避免出现「转交所有者组织设置」这类拼接。
STANDALONE_VERBS = {'owner-transfer', 'departments', 'diagnose', 'reset-password', 'revoke'}

# 菜单树按 change 的 FRONTEND.md 第 1 节：平台与组织各最多四个一级目录。
# view_path 为前端 definePluginPages 生成的 canonical 键，path 为浏览器路由。
# 目录节点携带其子页 ACTION 的并集并使用 ANY，子页全部不可见时目录随之隐藏。
PLATFORM_MENUS = [
    ('组织与租户', 'platform.iam.tenant', [
        ('租户管理', 'platform.iam.tenants', ['iam-platform:tenant:read']),
    ]),
    ('应用与配置', 'platform.iam.config', [
        ('应用目录', 'platform.iam.applications', ['iam-platform:application:read']),
        ('套餐', 'platform.iam.plans', ['iam-platform:plan:read']),
        ('共享角色', 'platform.iam.shared.roles', ['iam-platform:shared-role:read']),
    ]),
    ('平台管理', 'platform.iam.manage', [
        ('平台人员', 'platform.iam.personnel', ['iam-platform:member:read']),
        ('角色与授权', 'platform.iam.authorization',
         ['iam-platform:role:read', 'iam-platform:assignment:read', 'iam-platform:delegation:read']),
    ]),
    ('安全与运维', 'platform.iam.security', [
        ('事件与审计', 'security.iam.authorization.audit', ['iam-platform:audit:read']),
    ]),
]

TENANT_MENUS = [
    ('组织管理', 'org.iam.organization', [
        ('成员与部门', 'org.iam.members', ['iam-tenant:member:read', 'iam-tenant:department:read']),
        ('用户组', 'org.iam.groups', ['iam-tenant:group:read']),
        ('组织设置', 'org.iam.settings', ['iam-tenant:settings:read']),
    ]),
    ('权限与应用', 'org.iam.authorization.root', [
        ('角色与授权', 'org.iam.authorization',
         ['iam-tenant:role:read', 'iam-tenant:assignment:read', 'iam-tenant:delegation:read']),
        ('应用管理', 'org.iam.applications', ['iam-tenant:application:read']),
    ]),
    ('安全与合规', 'org.iam.compliance', [
        ('成员权限', 'security.iam.member.permissions',
         ['iam-tenant:directory-policy:read', 'iam-tenant:field-policy:read']),
        ('审计', 'security.iam.authorization.audit', ['iam-tenant:audit:read']),
    ]),
    ('工作台', 'org.iam.workspace', [
        ('工作台', 'org.iam.workbench', ['iam-tenant:application:read']),
        ('通讯录', 'org.iam.directory', ['iam-tenant:directory:read']),
    ]),
]

MENUS = {'iam-platform': PLATFORM_MENUS, 'iam-tenant': TENANT_MENUS}


def quote(value):
    """转义 SQL 字符串字面量。"""
    return "'" + value.replace('\\', '\\\\').replace("'", "''") + "'"


def json_literal(value):
    """把 Python 结构写成 MySQL 可解析的 JSON 字符串字面量。"""
    return "CAST(" + quote(json.dumps(value, ensure_ascii=False)) + " AS JSON)"


def menu_path(view_key):
    """由 canonical 视图键推导默认浏览器路由，与前端 toDefaultMenuPath 一致。"""
    return '/' + view_key.replace('.', '/')


def route_name(view_key):
    """路由名称使用 canonical 视图键，全局唯一且与前端命名路由一致。"""
    return view_key


def action_name(code):
    """由操作码合成展示名，例如 iam-tenant:member:read → 查看成员。"""
    _, resource, suffix = code.split(':', 2)
    verb = VERBS[suffix]
    if suffix in STANDALONE_VERBS:
        return verb
    return verb + RESOURCES[resource][0]


def collect(routes):
    """从契约推导 (应用 → 资源 → 操作) 目录，操作码全局唯一。"""
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
    return catalog


def insert(table, columns, values, exists, source='DUAL', where=None):
    """存在即跳过的插入语句，重复执行不覆盖任何既有行。

    父行一律由 source/where 按自然键解析，保留标识只用于本次新建的行；
    目标库已存在同编码但不同标识的父行时，子行跟随既有父行而不是硬编码标识。
    """
    conditions = ([where] if where else []) + [f"NOT EXISTS (SELECT 1 FROM {table} WHERE {exists})"]
    return (f"INSERT INTO {table} ({', '.join(columns)})\n"
            f"SELECT {', '.join(values)} FROM {source}\n"
            f"WHERE {' AND '.join(conditions)};\n")


def build():
    routes = json.loads(ROUTES.read_text())
    catalog = collect(routes)

    lines = [
        '-- IAM 正式冷启动种子：先按顺序执行 001–005 再执行本文件，可重复执行。',
        '-- 由 tools/iam/generate_bootstrap.py 从 contracts/routes.json 生成，不要手工编辑。',
        '-- 全部语句存在即跳过，不覆盖任何人工或业务修改；不含账号与凭证，',
        '-- 平台首个账号由 provider 冷启动器经安全框架注册用例创建。',
        '',
        '-- 发号准备：静态保留标识全部小于起点，运行时发号不会与种子冲突。',
    ]
    lines.append(
        f"INSERT INTO biz_leaf_alloc (biz_tag, max_id, step, description)\n"
        f"SELECT {quote(LEAF_TAG)}, {LEAF_MAX_ID}, {LEAF_STEP}, {quote('IAM 冷启动发号')} FROM DUAL\n"
        f"WHERE NOT EXISTS (SELECT 1 FROM biz_leaf_alloc WHERE biz_tag = {quote(LEAF_TAG)});\n")

    lines.append('-- 应用：租户域基础应用在组织创建时缺省开通。')
    for code, meta in APPLICATIONS.items():
        lines.append(insert(
            'iam_application',
            ['id', 'code', 'domain', 'name', 'description', 'sort_order', 'baseline', 'enabled'],
            [str(meta['id']), quote(code), quote(meta['domain']), quote(meta['name']),
             quote(meta['description']), str(meta['sort_order']),
             'TRUE' if meta['baseline'] else 'FALSE', 'TRUE'],
            f"code = {quote(code)}"))

    resource_count = 0
    lines.append('-- 资源：声明合法范围与字段能力，供角色发布与字段策略校验取值。')
    for application in APPLICATIONS:
        for resource in sorted(catalog[application]):
            resource_count += 1
            name, scopes, fields = RESOURCES[resource]
            lines.append(insert(
                'iam_resource',
                ['id', 'application_id', 'code', 'name', 'scope_capabilities', 'field_capabilities',
                 'enabled'],
                [str(RESOURCE_BASE + resource_count), 'app.id', quote(resource), quote(name),
                 json_literal(scopes), json_literal(fields), 'TRUE'],
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
                    [str(ACTION_BASE + action_count), 'res.application_id', 'res.id', quote(code),
                     quote(action_name(code)), 'TRUE'],
                    f"code = {quote(code)}",
                    source='iam_resource res JOIN iam_application app ON app.id = res.application_id',
                    where=f"app.code = {quote(application)} AND res.code = {quote(resource)}"))

    lines.append('-- 菜单：目录携带子页操作并集，子页全部不可见时目录随之隐藏。')
    menu_count = 0
    menu_links = []
    for application, tree in MENUS.items():
        for order, (directory_name, directory_key, pages) in enumerate(tree, start=1):
            menu_count += 1
            directory_actions = sorted({code for _, _, codes in pages for code in codes})
            lines.append(insert(
                'iam_menu',
                ['id', 'application_id', 'parent_id', 'name', 'path', 'view_path', 'route_name',
                 'kind', 'match_mode', 'access_mode', 'sort_order', 'enabled'],
                [str(MENU_BASE + menu_count), 'app.id', 'NULL', quote(directory_name),
                 quote(menu_path(directory_key)), 'NULL', quote(directory_key),
                 quote('DIRECTORY'), quote('ANY'), quote('ACTION'), str(order), 'TRUE'],
                f"application_id = app.id AND route_name = {quote(directory_key)}",
                source='iam_application app', where=f"app.code = {quote(application)}"))
            menu_links += [(application, directory_key, code) for code in directory_actions]
            for page_order, (page_name, view_key, codes) in enumerate(pages, start=1):
                menu_count += 1
                lines.append(insert(
                    'iam_menu',
                    ['id', 'application_id', 'parent_id', 'name', 'path', 'view_path', 'route_name',
                     'kind', 'match_mode', 'access_mode', 'sort_order', 'enabled'],
                    [str(MENU_BASE + menu_count), 'parent.application_id', 'parent.id',
                     quote(page_name), quote(menu_path(view_key)), quote(view_key), quote(view_key),
                     quote('PAGE'), quote('ANY'), quote('ACTION'), str(page_order), 'TRUE'],
                    f"application_id = parent.application_id AND route_name = {quote(view_key)}",
                    source='iam_menu parent JOIN iam_application app ON app.id = parent.application_id',
                    where=f"app.code = {quote(application)} "
                          f"AND parent.route_name = {quote(directory_key)}"))
                menu_links += [(application, view_key, code) for code in sorted(set(codes))]

    lines.append('-- 菜单与操作关联：ACTION 访问模式下缺少关联的菜单一律不可见。')
    known = {code for application in catalog for codes in catalog[application].values()
             for code in codes}
    for application, menu_key, code in menu_links:
        if code not in known:
            raise SystemExit(f'菜单引用了目录中不存在的操作 {code}')
        lines.append(insert(
            'iam_menu_action',
            ['application_id', 'menu_id', 'action_id'],
            ['menu.application_id', 'menu.id', 'action.id'],
            'application_id = menu.application_id AND menu_id = menu.id AND action_id = action.id',
            source='iam_menu menu JOIN iam_application app ON app.id = menu.application_id '
                   'JOIN iam_action action ON action.application_id = menu.application_id',
            where=f"app.code = {quote(application)} AND menu.route_name = {quote(menu_key)} "
                  f"AND action.code = {quote(code)}"))

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
            ['id', 'role_id', 'kind', 'revision', 'base_revision_id', 'metadata_overrides'],
            [str(role['revision_id']), 'role.id', quote('SYSTEM'), '1', 'NULL', json_literal({})],
            'role_id = role.id AND revision = 1',
            source='iam_role_definition role',
            where=f"role.domain = {quote(domain)} AND role.tenant_key = 0 "
                  f"AND role.code = {quote(role['code'])}"))

    lines.append('-- 治理授权：治理角色覆盖本域全部操作，范围为全域。')
    for domain, role in GOVERNANCE_ROLES.items():
        application = role['application']
        codes = sorted(code for resource in catalog[application]
                       for code in catalog[application][resource])
        for code in codes:
            lines.append(insert(
                'iam_role_grant',
                ['revision_id', 'action_id', 'scopes'],
                ['revision.id', 'action.id', json_literal([{'kind': 'ALL'}])],
                'revision_id = revision.id AND action_id = action.id',
                source='iam_role_revision revision '
                       'JOIN iam_role_definition role ON role.id = revision.role_id '
                       'JOIN iam_action action',
                where=f"role.domain = {quote(domain)} AND role.tenant_key = 0 "
                      f"AND role.code = {quote(role['code'])} AND revision.revision = 1 "
                      f"AND action.code = {quote(code)}"))

    lines.append('-- 默认策略版本：固定版本被租户策略引用；通讯录默认全组织，字段默认脱敏手机邮箱，上限缺省不额外收紧。')
    field_default = {
        'fields': {
            item['key']: {
                'visibility': 'MASKED' if item['key'] in ('phone', 'email') else 'FULL',
                'editable': item['key'] not in ('phone', 'email'),
            }
            for item in MEMBER_FIELDS
        }
    }
    definitions = {
        'DIRECTORY': {'scope': 'ALL'},
        'FIELD': field_default,
    }
    for kind, policy_id in DEFAULT_POLICIES.items():
        lines.append(insert(
            'iam_default_policy_revision',
            ['id', 'kind', 'revision', 'definition'],
            [str(policy_id), quote(kind), '1', json_literal(definitions[kind])],
            f"kind = {quote(kind)} AND revision = 1"))

    TARGET.write_text('\n'.join(lines))
    print(f'{len(APPLICATIONS)} applications, {resource_count} resources, {action_count} actions, '
          f'{menu_count} menus, {len(menu_links)} menu actions, {len(known)} grants')


if __name__ == '__main__':
    build()
