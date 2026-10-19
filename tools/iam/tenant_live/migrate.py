#!/usr/bin/env python3
"""租户实时角色只读预检及正反转换。只接受IAM目标库授权表导出，不读取凭证。"""
import argparse
import copy
import hashlib
import json
import re
import os
from datetime import datetime, timezone
from pathlib import Path

MODEL = 'tenant-live-v1'
RENAME = {'iam-tenant:role:publish': 'iam-tenant:role:update',
          'iam-platform:shared-role:publish': 'iam-platform:shared-role:update'}
ROLE_KINDS = {'SHARED', 'TENANT_CUSTOM'}
TABLES = ('iam_tenant', 'iam_tenant_member', 'iam_account', 'iam_application', 'iam_resource',
          'iam_action', 'iam_role_definition', 'iam_role_revision', 'iam_role_grant', 'iam_role_delta',
          'iam_role_parameter', 'iam_role_assignment', 'iam_delegation_grant',
          'iam_delegation_role_revision', 'iam_delegation_action_ceiling',
          'iam_delegation_recipient_member', 'iam_delegation_recipient_department',
          'iam_delegation_role', 'iam_delegation_resource_ceiling','iam_menu','iam_menu_action',
          'iam_department','iam_member_department','iam_tenant_group','iam_tenant_group_member','iam_tenant_group_department')


def canonical(value):
    return json.dumps(value, sort_keys=True, ensure_ascii=False, separators=(',', ':'))


def digest(source):
    return hashlib.sha256(canonical(source).encode()).hexdigest()


def obj(value):
    return json.loads(value) if isinstance(value, str) else value


def rows(source, table):
    return source.get(table, [])


def index(source, table):
    return {int(row['id']): row for row in rows(source, table)}


def instant(value):
    if not value:
        return None
    parsed=datetime.fromisoformat(value.replace('Z', '+00:00'))
    return parsed.replace(tzinfo=timezone.utc) if parsed.tzinfo is None else parsed.astimezone(timezone.utc)


def active(row, now):
    return row.get('status', 'ACTIVE') == 'ACTIVE' and (not row.get('valid_until') or instant(row['valid_until']) > now)


def normalize_ceiling(scopes, bindings):
    expressions=[]
    for value in obj(scopes):
        expression={k:v for k,v in value.items() if v is not None}
        if expression['kind'] in ('MEMBER_DEPARTMENTS','MANAGED_DEPARTMENTS'):
            expression['includeDescendants']=bool(expression.get('includeDescendants'))
        expressions.append(expression)
    return {'scopes':sorted(expressions,key=canonical),'scopeBindings':{key:{'kind':value['kind'],'ids':sorted(set(value['ids']))} for key,value in obj(bindings).items()}}


def preflight(source, batch_id, resolutions=None):
    if not re.fullmatch(r'[a-zA-Z0-9_-]{1,64}', batch_id):
        raise ValueError('批次ID只能由字母、数字、下划线或连字符组成')
    source_hash = digest(source)
    resolutions = resolutions or {'sourceDigest': source_hash, 'decisions': []}
    if resolutions.get('sourceDigest') != source_hash:
        raise ValueError('来源已变化，处置文件和旧预检作废')
    decisions = {}
    for decision in resolutions.get('decisions', []):
        if set(decision) != {'conflictId', 'actor', 'reason', 'at', 'value'} or not decision['actor'] or not decision['reason'] or not instant(decision['at']) or datetime.fromisoformat(decision['at'].replace('Z','+00:00')).tzinfo is None:
            raise ValueError('处置必须记录conflictId、actor、reason、带时区at和明确value')
        if decision['conflictId'] in decisions:
            raise ValueError('重复处置')
        decisions[decision['conflictId']] = decision['value']
    roles = index(source, 'iam_role_definition')
    revisions = index(source, 'iam_role_revision')
    actions = index(source, 'iam_action')
    tenants = index(source, 'iam_tenant')
    members = index(source, 'iam_tenant_member')
    accounts = index(source, 'iam_account')
    conflicts, converted_roles, assignments, delegations = [], [], [], []
    codes={a.get('code'):a for a in actions.values()}
    for before,after in RENAME.items():
        if before in codes and after in codes and codes[before]['id']!=codes[after]['id']:
            conflicts.append({'conflictId':'CATALOG_RENAME:'+before,'detail':'publish和update同时存在，须先显式处置来源；不能改写历史引用'})
    now = datetime.now(timezone.utc)
    allocation = max([int(row.get('id') or 0) for table in TABLES for row in rows(source, table)] + [1000000])

    def allocate():
        nonlocal allocation
        allocation += 1
        if allocation >= 2**64:
            raise ValueError('ID空间耗尽')
        return allocation

    def conflict(kind, entity, detail):
        key = f'{kind}:{entity}'
        if kind not in {'SYSTEM_ASSIGNMENT','ASSIGNMENT_BINDINGS','RESOURCE_CEILING','MISSING_CEILING'} or key not in decisions:
            conflicts.append({'conflictId': key, 'detail': detail})
            return None
        return decisions[key]

    def synthesize(revision_id, visited=None):
        visited = set() if visited is None else set(visited)
        revision_id = int(revision_id)
        if revision_id in visited or revision_id not in revisions:
            raise ValueError('循环或悬空基础版本')
        visited.add(revision_id)
        revision = revisions[revision_id]
        base = revision.get('base_revision_id')
        grants, parameters = {}, {}
        if base:
            base_revision = revisions.get(int(base))
            base_role = roles.get(int(base_revision['role_id'])) if base_revision else None
            if not base_role or base_role['kind'] != 'SHARED' or base_role['domain'] != 'TENANT':
                raise ValueError('基础必须为租户域历史共享版本')
            grants, parameters = synthesize(base, visited)
        for row in rows(source, 'iam_role_grant'):
            if int(row['revision_id']) == revision_id:
                grants[int(row['action_id'])] = obj(row['scopes'])
        for row in rows(source, 'iam_role_parameter'):
            if int(row['revision_id']) == revision_id:
                parameters[row['parameter_key']] = row['binding_kind']
        deltas = [d for d in rows(source, 'iam_role_delta') if int(d['revision_id']) == revision_id]
        for operation in ('REMOVE', 'REPLACE_SCOPE', 'ADD'):
            for delta in deltas:
                if delta['operation'] != operation:
                    continue
                key = int(delta['action_id'])
                if operation == 'REMOVE':
                    if key not in grants:
                        raise ValueError('REMOVE引用不存在基础权限')
                    del grants[key]
                elif operation == 'REPLACE_SCOPE':
                    if key not in grants:
                        raise ValueError('REPLACE_SCOPE引用不存在基础权限')
                    grants[key] = obj(delta['scopes'])
                else:
                    if key in grants:
                        raise ValueError('ADD重复基础权限')
                    grants[key] = obj(delta['scopes'])
        return grants, parameters

    def valid_bindings(parameters,bindings,tenant_id):
        for parameter in parameters:
            binding=bindings.get(parameter['key'])
            if not isinstance(binding,dict) or binding.get('kind')!=parameter['kind'] or not isinstance(binding.get('ids'),list):
                return False
            for identity in binding['ids']:
                if not isinstance(identity,str) or not identity.isdigit() or int(identity)<=0:
                    return False
                if parameter['kind']=='DEPARTMENTS' and 'iam_department' in source:
                    department=index(source,'iam_department').get(int(identity))
                    if not department or int(department['tenant_id'])!=int(tenant_id):
                        return False
        return True

    for tenant in tenants.values():
        owner = members.get(int(tenant.get('owner_member_id') or 0))
        account = accounts.get(int(owner.get('account_id') or 0)) if owner else None
        if not owner or int(owner['tenant_id']) != int(tenant['id']) or owner.get('status') != 'ACTIVE' or not account or not account.get('enabled') or account.get('deleted_at'):
            conflict('INVALID_OWNER', tenant['id'], '拥有者不是本组织有效成员和账号，必须先修复源数据')
    for role in roles.values():
        if role['domain'] != 'TENANT' or role['kind'] not in ROLE_KINDS:
            continue
        history = [r for r in revisions.values() if int(r['role_id']) == int(role['id'])]
        if not history:
            conflict('MISSING_REVISION', role['id'], '没有已发布权限，不能默认空角色')
            continue
        latest = max(history, key=lambda r: (int(r['revision']), int(r['id'])))
        try:
            grants, parameter_kinds = synthesize(latest['id'])
            parameters = {}
            for action_id, scopes in grants.items():
                action = actions.get(action_id)
                if not action:
                    raise ValueError('悬空操作')
                resource = index(source, 'iam_resource').get(int(action['resource_id']))
                app = index(source, 'iam_application').get(int(action['application_id']))
                if not resource or not app or app['domain'] != 'TENANT' or int(resource['application_id']) != int(app['id']):
                    raise ValueError('操作域或资源归属非法')
                for scope in scopes:
                    if scope.get('kind') not in {'ALL','SELF','MEMBER_DEPARTMENTS','MANAGED_DEPARTMENTS','OBJECT_SET'} or scope['kind'] not in obj(resource.get('scope_capabilities') or ['ALL','SELF','MEMBER_DEPARTMENTS','MANAGED_DEPARTMENTS','OBJECT_SET']):
                        raise ValueError('资源不支持的范围类型')
                    if (scope['kind'] in {'MANAGED_DEPARTMENTS','OBJECT_SET'}) != bool(scope.get('parameterKey')):
                        raise ValueError('范围参数声明不合法')
                    key = scope.get('parameterKey')
                    if key:
                        kind = 'DEPARTMENTS' if scope['kind'] == 'MANAGED_DEPARTMENTS' else 'OBJECTS'
                        if parameter_kinds.get(key) != kind:
                            raise ValueError('参数未声明或类型不兼容')
                        parameter = {'key': key, 'kind': kind, 'resourceId': str(action['resource_id'])}
                        if key in parameters and parameters[key] != parameter:
                            raise ValueError('同参数跨资源或类型')
                        parameters[key] = parameter
            for historical in rows(source,'iam_role_parameter'):
                revision=revisions.get(int(historical['revision_id']))
                if revision and int(revision['role_id'])==int(role['id']) and historical['parameter_key'] in parameters and historical['binding_kind']!=parameters[historical['parameter_key']]['kind']:
                    raise ValueError('历史参数键已使用其他类型，最新定义须使用新键后重新预检')
            converted_roles.append({'roleId': int(role['id']), 'kind': role['kind'], 'tenantId': role.get('tenant_id'),
                'sourceRevisionId': int(latest['id']), 'snapshotId': allocate(), 'revision': int(latest['revision']) + 1,
                'grants': [{'actionId': str(a), 'scopes': s} for a, s in sorted(grants.items())],
                'parameters': list(parameters.values())})
        except (ValueError, KeyError, TypeError) as error:
            conflict('INVALID_ROLE', role['id'], str(error))
    current = {r['roleId']: r for r in converted_roles}
    for assignment in rows(source, 'iam_role_assignment'):
        if assignment['domain'] != 'TENANT':
            continue
        revision = revisions.get(int(assignment.get('revision_id') or 0))
        role = roles.get(int(revision['role_id'])) if revision else None
        target = {'id': int(assignment['id']), 'status': assignment['status'], 'bindings': obj(assignment['scope_bindings'])}
        if role and role['kind'] == 'SYSTEM':
            owner = tenants.get(int(assignment['tenant_id']), {}).get('owner_member_id')
            if assignment.get('subject_type') == 'MEMBER' and assignment.get('tenant_member_id') == owner and assignment.get('source') == 'INITIALIZATION':
                target['status'] = 'REVOKED'
                target['retiredOwner'] = True
                assignments.append(target)
                continue
            decision = conflict('SYSTEM_ASSIGNMENT', assignment['id'], '非拥有者SYSTEM分配须明确撤销或映射角色和绑定')
            if not decision:
                continue
            if decision.get('revoke') is True:
                target['status'] = 'REVOKED'; target['retiredSystem'] = True; assignments.append(target); continue
            role = roles.get(int(decision.get('roleId') or 0))
            target['bindings'] = decision.get('scopeBindings', {})
        if not role or int(role['id']) not in current or role['kind'] == 'TENANT_CUSTOM' and role.get('tenant_id') != assignment['tenant_id']:
            conflict('ASSIGNMENT_ROLE', assignment['id'], '悬空或跨组织角色')
            continue
        target['roleId'] = int(role['id']); target['roleKind'] = role['kind']; target['roleTenantKey'] = int(role.get('tenant_id') or 0)
        definition = current[int(role['id'])]
        historical_grants,_=synthesize(revision['id']) if revision else ({},{})
        historical_resources={scope['parameterKey']:str(actions[action_id]['resource_id']) for action_id,scopes in historical_grants.items() for scope in scopes if scope.get('parameterKey') and action_id in actions}
        missing = [p for p in definition['parameters'] if p['key'] not in target['bindings'] or target['bindings'][p['key']].get('kind') != p['kind'] or historical_resources.get(p['key'],p['resourceId'])!=p['resourceId']]
        if missing and active(assignment, now):
            decision = conflict('ASSIGNMENT_BINDINGS', assignment['id'], missing)
            if decision:
                target['bindings'] = decision['scopeBindings']
                if any(target['bindings'].get(p['key'], {}).get('kind') != p['kind'] for p in definition['parameters']):
                    conflicts.append({'conflictId': f'ASSIGNMENT_BINDINGS:{assignment["id"]}', 'detail': '处置仍缺当前参数'})
        if active(assignment,now) and not missing and not valid_bindings(definition['parameters'],target['bindings'],assignment['tenant_id']):
            conflict('INVALID_ASSIGNMENT_BINDING',assignment['id'],'参数ID、类型或租户归属非法，须先修复来源')
        old, _ = synthesize(revision['id']) if revision else ({}, {})
        target['addedActionIds'] = sorted(set(int(g['actionId']) for g in definition['grants']) - old.keys())
        target['removedActionIds'] = sorted(old.keys() - set(int(g['actionId']) for g in definition['grants']))
        target['changedScopeActionIds'] = [g['actionId'] for g in definition['grants'] if int(g['actionId']) in old and canonical(old[int(g['actionId'])]) != canonical(g['scopes'])]
        assignments.append(target)
    for delegation in rows(source, 'iam_delegation_grant'):
        if delegation['domain'] != 'TENANT':
            continue
        allowed = []
        for item in rows(source, 'iam_delegation_role_revision'):
            if int(item['delegation_id']) != int(delegation['id']):
                continue
            revision = revisions.get(int(item['revision_id']))
            role = roles.get(int(revision['role_id'])) if revision else None
            if not role or int(role['id']) not in current or role['kind'] == 'TENANT_CUSTOM' and role.get('tenant_id') != delegation['tenant_id']:
                conflict('DELEGATION_ROLE', delegation['id'], '非法白名单角色，须修复源数据')
            else:
                allowed.append(int(role['id']))
        ceilings = {}
        for ceiling in rows(source, 'iam_delegation_action_ceiling'):
            if int(ceiling['delegation_id']) != int(delegation['id']):
                continue
            action = actions.get(int(ceiling['action_id']))
            if not action:
                conflict('DELEGATION_ACTION', delegation['id'], '悬空操作上限'); continue
            resource = int(action['resource_id'])
            definition = {'resourceId': str(resource), **normalize_ceiling(ceiling['scopes'],ceiling['scope_bindings'])}
            if resource in ceilings and canonical(ceilings[resource]) != canonical(definition):
                decision = conflict('RESOURCE_CEILING', f'{delegation["id"]}:{resource}', '同资源逐操作范围不一致，禁止自动合并')
                if decision:
                    definition = decision['resourceScopeCeiling']
            ceilings[resource] = definition
        required = {int(actions[int(g['actionId'])]['resource_id']) for role_id in allowed for g in current[role_id]['grants']}
        for resource in sorted(required - ceilings.keys()):
            decision = conflict('MISSING_CEILING', f'{delegation["id"]}:{resource}', '目标当前角色新增资源，须补齐上限')
            if decision:
                ceilings[resource] = decision['resourceScopeCeiling']
        if not allowed or delegation.get('assignment_duration_mode', 'LIMITED') != 'LIMITED':
            conflict('DELEGATION_EMPTY_OR_DURATION', delegation['id'], '租户白名单不可空且只能有限时长')
        delegations.append({'id': int(delegation['id']), 'tenantId': int(delegation['tenant_id']), 'roleIds': sorted(set(allowed)), 'ceilings': list(ceilings.values())})
    return {'model': MODEL, 'batchId': batch_id, 'sourceDigest': source_hash, 'source': source,
        'roles': converted_roles, 'assignments': assignments, 'delegations': delegations,
        'conflicts': conflicts, 'decisions': resolutions['decisions'], 'ready': not conflicts}


def sql_value(value):
    if value is None:
        return 'NULL'
    if isinstance(value, bool):
        return 'TRUE' if value else 'FALSE'
    if isinstance(value, int):
        return str(value)
    text = canonical(value) if isinstance(value, (list, dict)) else str(value)
    return "CONVERT(0x" + text.encode().hex() + " USING utf8mb4)"


def insert(table, values):
    return f'INSERT INTO {table}(' + ','.join(values) + ') VALUES(' + ','.join(sql_value(v) for v in values.values()) + ');'


def source_guards(source):
    """停写窗口仍验证全部导出事实，拒绝来源变化后的正反转换。"""
    output=[]
    for table in TABLES:
        if table not in source:
            continue
        output.append(f"IF (SELECT COUNT(*) FROM {table})!={len(source[table])} THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='来源行数变化，重新预检'; END IF;")
        for row in source[table]:
            conditions=[]
            for column,value in row.items():
                if not re.fullmatch(r'[a-z_]+',column):
                    raise ValueError('非法导出列')
                if table=='iam_account' and column not in {'id','enabled','deleted_at','version'}:
                    raise ValueError('账号导出含未授权字段')
                if column in {'scopes','scope_bindings','metadata_overrides','resource_field_permissions','scope_capabilities','field_capabilities'} and value is not None:
                    operand=sql_value(obj(value))
                    conditions.append(f'JSON_CONTAINS(`{column}`,CAST({operand} AS JSON)) AND JSON_CONTAINS(CAST({operand} AS JSON),`{column}`)')
                else:
                    conditions.append(f'`{column}` <=> {sql_value(value)}')
            output.append(f"IF NOT EXISTS(SELECT 1 FROM {table} WHERE "+' AND '.join(conditions)+") THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='来源字段变化，重新预检'; END IF;")
    return output


def forward(report, source=None):
    if source is not None and digest(source) != report['sourceDigest']:
        raise ValueError('来源变化，不能执行旧报告')
    if not report['ready'] or report['conflicts']:
        raise ValueError('仍有阻断冲突，禁止生成转换')
    source = report['source']
    output = ['SET time_zone=\'+00:00\';', 'DROP PROCEDURE IF EXISTS iam_live_forward;', 'DELIMITER $$', 'CREATE PROCEDURE iam_live_forward()', 'main: BEGIN',
              'DECLARE done_state VARCHAR(16);', 'DECLARE done_digest CHAR(64);',
              'DECLARE EXIT HANDLER FOR SQLEXCEPTION BEGIN ROLLBACK; RESIGNAL; END;', 'START TRANSACTION;',
              f"SELECT state,source_digest INTO done_state,done_digest FROM iam_live_authorization_batch WHERE batch_id={sql_value(report['batchId'])} FOR UPDATE;",
              f"IF done_state='FORWARD' AND done_digest={sql_value(report['sourceDigest'])} THEN COMMIT; LEAVE main; END IF;",
              "IF done_state IS NOT NULL THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='批次已存在且内容或方向不符'; END IF;",
              insert('iam_live_authorization_batch', {'batch_id': report['batchId'], 'source_digest': report['sourceDigest'], 'state': 'FORWARD', 'decisions': report['decisions']})]
    output += source_guards(source)
    for table in ('iam_role_definition', 'iam_role_assignment', 'iam_delegation_grant'):
        for row in rows(source, table):
            if row.get('domain') != 'TENANT':
                continue
            output.append(f"IF NOT EXISTS(SELECT 1 FROM {table} WHERE id={int(row['id'])} AND version={int(row.get('version') or 0)}) THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='来源并发锁变化，重新预检'; END IF;")
            output.append(insert('iam_live_authorization_mapping', {'batch_id': report['batchId'], 'entity_type': table.removeprefix('iam_'), 'entity_id': int(row['id']), 'source_row': row, 'target_id': next((r['snapshotId'] for r in report['roles'] if r['roleId'] == int(row['id'])), None) if table == 'iam_role_definition' else None}))
    for role in report['roles']:
        output.append(insert('iam_role_revision', {'id': role['snapshotId'], 'role_id': role['roleId'], 'kind': role['kind'], 'revision': role['revision'], 'base_revision_id': None, 'metadata_overrides': {}, 'resource_field_permissions': {}}))
        for grant in role['grants']:
            output.append(insert('iam_role_grant', {'revision_id': role['snapshotId'], 'action_id': int(grant['actionId']), 'scopes': grant['scopes']}))
        for parameter in role['parameters']:
            output.append(insert('iam_role_parameter', {'revision_id': role['snapshotId'], 'parameter_key': parameter['key'], 'binding_kind': parameter['kind'], 'resource_id': int(parameter['resourceId'])}))
        output.append(f"UPDATE iam_role_definition SET current_revision_id={role['snapshotId']} WHERE id={role['roleId']} AND domain='TENANT';")
    for assignment in report['assignments']:
        values = {'role_id': assignment.get('roleId'), 'role_kind': assignment.get('roleKind'), 'role_tenant_key': assignment.get('roleTenantKey'), 'status': assignment['status'], 'scope_bindings': assignment['bindings']}
        output.append('UPDATE iam_role_assignment SET ' + ','.join(k+'='+sql_value(v) for k, v in values.items()) + f" WHERE id={assignment['id']} AND domain='TENANT';")
    for action in rows(source,'iam_action'):
        if action.get('code') in RENAME or action.get('code')=='iam-tenant:role:upgrade':
            output.append(insert('iam_live_authorization_mapping',{'batch_id':report['batchId'],'entity_type':'action','entity_id':int(action['id']),'source_row':action}))
    upgrade_ids={int(action['id']) for action in rows(source,'iam_action') if action.get('code')=='iam-tenant:role:upgrade'}
    for relation in rows(source,'iam_menu_action'):
        if int(relation['action_id']) in upgrade_ids:
            output.append(insert('iam_live_authorization_mapping',{'batch_id':report['batchId'],'entity_type':'retired_menu_action','entity_id':int(relation['menu_id']),'source_row':relation,'target_id':int(relation['action_id'])}))
    output.append("DELETE ma FROM iam_menu_action ma JOIN iam_action a ON a.id=ma.action_id WHERE a.code='iam-tenant:role:upgrade';")
    current = {r['roleId']: r for r in report['roles']}
    for delegation in report['delegations']:
        for role_id in delegation['roleIds']:
            role = current[role_id]
            output.append(insert('iam_delegation_role', {'delegation_id': delegation['id'], 'tenant_id': delegation['tenantId'], 'domain': 'TENANT', 'role_id': role_id, 'role_kind': role['kind'], 'role_tenant_key': int(role.get('tenantId') or 0)}))
        for ceiling in delegation['ceilings']:
            output.append(insert('iam_delegation_resource_ceiling', {'delegation_id': delegation['id'], 'resource_id': int(ceiling['resourceId']), 'scopes': ceiling['scopes'], 'scope_bindings': ceiling['scopeBindings']}))
    for before, after in RENAME.items():
        output.append(f'UPDATE iam_action SET code={sql_value(after)} WHERE code={sql_value(before)};')
    output.append("UPDATE iam_action SET enabled=FALSE WHERE code='iam-tenant:role:upgrade';")
    output += [f"UPDATE iam_live_authorization_batch SET applied_at=CURRENT_TIMESTAMP(6) WHERE batch_id={sql_value(report['batchId'])};", 'COMMIT;', 'END$$', 'DELIMITER ;', 'CALL iam_live_forward();', 'DROP PROCEDURE iam_live_forward;']
    return '\n'.join(output) + '\n'


def reverse(source, batch_id):
    """从当前新库反向生成固定引用，保留新增角色、分配、委派、转交和审计。"""
    roles, revisions, actions = index(source, 'iam_role_definition'), index(source, 'iam_role_revision'), index(source, 'iam_action')
    output = ["SET time_zone='+00:00';", 'DROP PROCEDURE IF EXISTS iam_live_reverse;', 'DELIMITER $$', 'CREATE PROCEDURE iam_live_reverse()', 'main: BEGIN',
        'DECLARE done_state VARCHAR(16);', 'DECLARE done_digest CHAR(64);',
        'DECLARE EXIT HANDLER FOR SQLEXCEPTION BEGIN ROLLBACK; RESIGNAL; END;',
        f"SELECT state,reverse_digest INTO done_state,done_digest FROM iam_live_authorization_batch WHERE batch_id={sql_value(batch_id)};",
        f"IF done_state='REVERSE' AND done_digest={sql_value(digest(source))} THEN LEAVE main; END IF;",
        "IF done_state IS NULL OR done_state!='FORWARD' THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='无正转换批次，禁止逆转换'; END IF;",
        "IF EXISTS(SELECT 1 FROM information_schema.table_constraints WHERE constraint_schema=DATABASE() AND table_name='iam_role_assignment' AND constraint_name='ck_iam_assignment_current_role') THEN ALTER TABLE iam_role_assignment DROP CHECK ck_iam_assignment_current_role; END IF;",
        'START TRANSACTION;']
    output += source_guards(source)
    for assignment in rows(source, 'iam_role_assignment'):
        if assignment['domain'] != 'TENANT' or not assignment.get('role_id'):
            continue
        role = roles.get(int(assignment['role_id']))
        revision = revisions.get(int(role.get('current_revision_id') or 0)) if role else None
        if not revision or int(revision['role_id']) != int(role['id']) or revision.get('base_revision_id'):
            raise ValueError('当前完整快照缺失或不属于本角色，禁止逆转换')
        output.append(f"UPDATE iam_role_assignment SET revision_id={revision['id']},revision_kind={sql_value(role['kind'])} WHERE id={assignment['id']} AND domain='TENANT';")
    for delegation in rows(source, 'iam_delegation_grant'):
        if delegation['domain'] != 'TENANT':
            continue
        output.append(f"DELETE FROM iam_delegation_role_revision WHERE delegation_id={delegation['id']};")
        output.append(f"DELETE FROM iam_delegation_action_ceiling WHERE delegation_id={delegation['id']};")
        ceilings = {int(c['resource_id']): c for c in rows(source, 'iam_delegation_resource_ceiling') if int(c['delegation_id']) == int(delegation['id'])}
        action_ceilings = {}
        for allowed in rows(source, 'iam_delegation_role'):
            if int(allowed['delegation_id']) != int(delegation['id']):
                continue
            role = roles.get(int(allowed['role_id']))
            revision_id = int(role.get('current_revision_id') or 0) if role else 0
            if revision_id not in revisions:
                raise ValueError('逆转换白名单缺少当前快照')
            output.append(insert('iam_delegation_role_revision', {'delegation_id': int(delegation['id']), 'revision_id': revision_id, 'revision_kind': role['kind']}))
            for grant in rows(source, 'iam_role_grant'):
                if int(grant['revision_id']) != revision_id:
                    continue
                action = actions.get(int(grant['action_id']))
                ceiling = ceilings.get(int(action['resource_id'])) if action else None
                if ceiling is None:
                    # 新资源无上限保持安全关闭，用空对象上限表达旧模型同一结果。
                    ceiling = {'scopes': [], 'scope_bindings': {}}
                action_ceilings[int(grant['action_id'])] = ceiling
        for action_id, ceiling in action_ceilings.items():
            output.append(insert('iam_delegation_action_ceiling', {'delegation_id': int(delegation['id']), 'action_id': action_id, 'scopes': obj(ceiling['scopes']), 'scope_bindings': obj(ceiling['scope_bindings'])}))
    allocation = max([int(row.get('id') or 0) for table in TABLES for row in rows(source, table)] + [1000000])
    systems = [r for r in roles.values() if r['domain'] == 'TENANT' and r['kind'] == 'SYSTEM']
    if len(systems) != 1:
        raise ValueError('逆转换须恰好一个历史租户SYSTEM角色；缺失时先按报告显式恢复目录')
    system = systems[0]
    allocation += 1
    revision_id = allocation
    revision_number = max([int(r['revision']) for r in revisions.values() if int(r['role_id']) == int(system['id'])] + [0]) + 1
    output.append(insert('iam_role_revision', {'id': revision_id, 'role_id': int(system['id']), 'kind': 'SYSTEM', 'revision': revision_number, 'metadata_overrides': {}, 'resource_field_permissions': {}}))
    applications = index(source, 'iam_application')
    for action in actions.values():
        if applications.get(int(action['application_id']), {}).get('domain') == 'TENANT':
            output.append(insert('iam_role_grant', {'revision_id': revision_id, 'action_id': int(action['id']), 'scopes': [{'kind': 'ALL'}]}))
    output.append("UPDATE iam_role_assignment SET status='REVOKED' WHERE domain='TENANT' AND source='INITIALIZATION' AND revision_kind='SYSTEM';")
    for tenant in rows(source, 'iam_tenant'):
        allocation += 1
        output.append(insert('iam_role_assignment', {'id': allocation, 'domain': 'TENANT', 'tenant_id': int(tenant['id']), 'subject_type': 'MEMBER', 'tenant_member_id': int(tenant['owner_member_id']), 'revision_id': revision_id, 'revision_kind': 'SYSTEM', 'scope_bindings': {}, 'valid_from': datetime.now(timezone.utc).strftime('%Y-%m-%d %H:%M:%S.%f'), 'status': 'ACTIVE', 'source': 'INITIALIZATION'}))
    for before, after in RENAME.items():
        output.append(f'UPDATE iam_action SET code={sql_value(before)} WHERE code={sql_value(after)};')
    output.append(f"UPDATE iam_action a JOIN iam_live_authorization_mapping m ON m.entity_id=a.id AND m.entity_type='action' AND m.batch_id={sql_value(batch_id)} SET a.enabled=JSON_UNQUOTE(JSON_EXTRACT(m.source_row,'$.enabled')) IN ('1','true') WHERE a.code='iam-tenant:role:upgrade';")
    output.append(f"INSERT IGNORE INTO iam_menu_action(menu_id,action_id) SELECT entity_id,target_id FROM iam_live_authorization_mapping WHERE batch_id={sql_value(batch_id)} AND entity_type='retired_menu_action';")
    output.append(f"UPDATE iam_live_authorization_batch SET state='REVERSE',reverse_digest={sql_value(digest(source))},applied_at=CURRENT_TIMESTAMP(6) WHERE batch_id={sql_value(batch_id)};")
    output += ['COMMIT;', 'END$$', 'DELIMITER ;', 'CALL iam_live_reverse();', 'DROP PROCEDURE iam_live_reverse;']
    return '\n'.join(output) + '\n'


def verify(source):
    """验证目标引用、完整定义、参数和资源委派；不执行任何SQL。"""
    conflicts=[]
    role_map=index(source,'iam_role_definition');revision_map=index(source,'iam_role_revision')
    actions=index(source,'iam_action');resources=index(source,'iam_resource');applications=index(source,'iam_application')
    members=index(source,'iam_tenant_member');accounts=index(source,'iam_account')
    def issue(entity,identity,reason):conflicts.append({'entity':entity,'id':identity,'reason':reason})
    for tenant in rows(source,'iam_tenant'):
        owner=members.get(int(tenant.get('owner_member_id') or 0));account=accounts.get(int(owner.get('account_id') or 0)) if owner else None
        if not owner or owner['tenant_id']!=tenant['id'] or owner.get('status')!='ACTIVE' or not account or not account.get('enabled') or account.get('deleted_at'):issue('tenant',tenant['id'],'拥有者无效')
    for role in role_map.values():
        if role['domain']!='TENANT' or role['kind'] not in ROLE_KINDS:continue
        revision=revision_map.get(int(role.get('current_revision_id') or 0))
        if not revision or int(revision['role_id'])!=int(role['id']) or revision.get('base_revision_id'):
            issue('role',role['id'],'当前完整指针非法');continue
        parameters={p['parameter_key']:p for p in rows(source,'iam_role_parameter') if p['revision_id']==revision['id']}
        used=set()
        for grant in rows(source,'iam_role_grant'):
            if grant['revision_id']!=revision['id']:continue
            action=actions.get(int(grant['action_id']));resource=resources.get(int(action['resource_id'])) if action else None;app=applications.get(int(action['application_id'])) if action else None
            if not action or not resource or not app or app['domain']!='TENANT' or resource['application_id']!=app['id']:
                issue('role',role['id'],'权限的应用资源归属非法');continue
            for scope in obj(grant['scopes']):
                key=scope.get('parameterKey')
                if key:
                    used.add(key);parameter=parameters.get(key);kind='DEPARTMENTS' if scope['kind']=='MANAGED_DEPARTMENTS' else 'OBJECTS'
                    if not parameter or parameter.get('binding_kind')!=kind or parameter.get('resource_id')!=resource['id']:issue('role',role['id'],'参数类型或资源归属非法')
        if used!=set(parameters):issue('role',role['id'],'存在未使用参数')
    for assignment in rows(source,'iam_role_assignment'):
        if assignment['domain']!='TENANT':continue
        role=role_map.get(int(assignment.get('role_id') or 0))
        if not role and assignment['status']=='REVOKED' and assignment.get('revision_id'):continue
        if not role or role['domain']!='TENANT' or role['kind'] not in ROLE_KINDS or role['kind']=='TENANT_CUSTOM' and role.get('tenant_id')!=assignment['tenant_id'] or assignment.get('role_kind')!=role['kind'] or assignment.get('role_tenant_key')!=int(role.get('tenant_id') or 0):issue('assignment',assignment['id'],'当前角色归属非法')
    delegations=index(source,'iam_delegation_grant')
    for allowed in rows(source,'iam_delegation_role'):
        parent=delegations.get(int(allowed['delegation_id']));role=role_map.get(int(allowed['role_id']))
        if not parent or parent['domain']!='TENANT' or parent['tenant_id']!=allowed['tenant_id'] or not role or role['domain']!='TENANT' or role['kind'] not in ROLE_KINDS or role['kind']=='TENANT_CUSTOM' and role.get('tenant_id')!=parent['tenant_id']:issue('delegation',allowed['delegation_id'],'白名单归属非法')
    for ceiling in rows(source,'iam_delegation_resource_ceiling'):
        resource=resources.get(int(ceiling['resource_id']));app=applications.get(int(resource['application_id'])) if resource else None
        if not app or app['domain']!='TENANT':issue('delegation',ceiling['delegation_id'],'资源上限域非法')
    return {'digest':digest(source),'conflicts':conflicts,'valid':not conflicts}


def main():
    os.umask(0o077)
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('mode', choices=('preflight', 'forward', 'reverse', 'verify'))
    parser.add_argument('--source', required=True, type=Path)
    parser.add_argument('--batch', required=True)
    parser.add_argument('--report', type=Path)
    parser.add_argument('--resolutions', type=Path)
    parser.add_argument('--output', required=True, type=Path)
    args = parser.parse_args()
    source = json.loads(args.source.read_text())
    if args.mode == 'preflight':
        report = preflight(source, args.batch, json.loads(args.resolutions.read_text()) if args.resolutions else None)
        args.output.write_text(json.dumps(report, ensure_ascii=False, indent=2) + '\n')
        print(f'预检 {args.batch}: {len(report["conflicts"])} 项阻断；来源 {report["sourceDigest"]}')
        return 0 if report['ready'] else 2
    if args.mode == 'forward':
        if not args.report:
            parser.error('forward需要--report')
        args.output.write_text(forward(json.loads(args.report.read_text()), source))
    elif args.mode == 'reverse':
        args.output.write_text(reverse(source, args.batch))
    else:
        report=verify(source)
        args.output.write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n')
        if not report['valid']:return 2
    print(f'已生成 {args.output}；尚未执行数据库转换')
    return 0


if __name__ == '__main__':
    raise SystemExit(main())
