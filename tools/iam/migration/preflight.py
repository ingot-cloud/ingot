#!/usr/bin/env python3
"""只读校验脱敏源清单与显式映射；不连接数据库，不导入数据，不标记 verified。"""
import argparse
from enum import StrEnum
import hashlib
import json
from pathlib import Path
import re
import sys

CATALOG = Path(__file__).with_name('source_catalog.json')
IDENTIFIER = re.compile(r'[A-Za-z0-9_.-]{1,128}\Z')
FINGERPRINT = re.compile(r'[a-f0-9]{64}\Z')


class IssueCode(StrEnum):
    INVALID_INPUT = 'INVALID_INPUT'
    SAME_SOURCE_TARGET = 'SAME_SOURCE_TARGET'
    UNKNOWN_TABLE = 'UNKNOWN_TABLE'
    MISSING_TABLE = 'MISSING_TABLE'
    SCHEMA_DRIFT = 'SCHEMA_DRIFT'
    ROW_COUNT_MISSING = 'ROW_COUNT_MISSING'
    FACTS_INCOMPLETE = 'FACTS_INCOMPLETE'
    CREDENTIAL_UNVERIFIED = 'CREDENTIAL_UNVERIFIED'
    DUPLICATE_IDENTITY = 'DUPLICATE_IDENTITY'
    ORPHAN_REFERENCE = 'ORPHAN_REFERENCE'
    OWNER_UNRESOLVED = 'OWNER_UNRESOLVED'
    DEPARTMENT_CYCLE = 'DEPARTMENT_CYCLE'
    CROSS_TENANT_REFERENCE = 'CROSS_TENANT_REFERENCE'
    ACTION_UNRESOLVED = 'ACTION_UNRESOLVED'
    PLATFORM_IDENTITY_UNREVIEWED = 'PLATFORM_IDENTITY_UNREVIEWED'


class PreflightStatus(StrEnum):
    BLOCKED = 'BLOCKED'
    PREFLIGHTED = 'PREFLIGHTED'


class InvalidManifest(ValueError):
    """输入包含非脱敏清单字段或不符合结构；异常不得携带原值。"""


def require(condition):
    if not condition:
        raise InvalidManifest('预检清单结构无效；仅接受文档规定的脱敏元数据字段')


def identifier(value):
    return isinstance(value, str) and IDENTIFIER.fullmatch(value) is not None


def validate_shape(document):
    require(isinstance(document, dict))
    required = {'batchId', 'sourceFingerprint', 'sourceIdentifier', 'targetIdentifier', 'rulesVersion',
                'tables', 'accounts', 'tenants', 'memberships', 'departments', 'permissions',
                'ownerMappings', 'actionMappings', 'targetActions', 'platformIdentityReviewed', 'platformCandidates', 'platformMappings'}
    require(set(document) == required)
    for key in ('batchId', 'sourceIdentifier', 'targetIdentifier', 'rulesVersion'):
        require(identifier(document[key]))
    require(isinstance(document['sourceFingerprint'], str) and FINGERPRINT.fullmatch(document['sourceFingerprint']))
    require(type(document['platformIdentityReviewed']) is bool)
    definitions = {
        'platformCandidates': {'accountId'},
        'platformMappings': {'accountId', 'memberId', 'actor', 'reason'},
        'tables': {'name', 'columns', 'rowCount'},
        'accounts': {'id', 'enabled', 'deleted', 'credentialVerified'},
        'tenants': {'id', 'enabled', 'deleted'},
        'memberships': {'id', 'accountId', 'tenantId'},
        'departments': {'id', 'tenantId', 'parentId'},
        'permissions': {'id', 'code'},
        'ownerMappings': {'tenantId', 'accountId', 'actor', 'reason'},
        'actionMappings': {'permissionId', 'actionIds', 'actor', 'reason'},
        'targetActions': {'id', 'code'},
    }
    for key, fields in definitions.items():
        rows = document[key]
        require(isinstance(rows, list))
        for row in rows:
            require(isinstance(row, dict) and set(row) == fields)
            for name in fields & {'id', 'accountId', 'tenantId', 'permissionId', 'memberId'}:
                require(identifier(row[name]))
            if key == 'tables':
                require(identifier(row['name']))
                require(isinstance(row['columns'], list) and all(identifier(c) for c in row['columns']))
                require(row['rowCount'] is None or type(row['rowCount']) is int and row['rowCount'] >= 0)
            if 'parentId' in fields:
                require(row['parentId'] is None or identifier(row['parentId']))
            for name in fields & {'enabled', 'deleted', 'credentialVerified'}:
                require(type(row[name]) is bool)
            for name in fields & {'actor', 'reason', 'code'}:
                require(isinstance(row[name], str) and bool(row[name].strip()))
            if 'actionIds' in fields:
                require(isinstance(row['actionIds'], list) and all(identifier(a) for a in row['actionIds']))


def preflight(document, catalog=None):
    validate_shape(document)
    catalog = catalog or json.loads(CATALOG.read_text())['tables']
    issues = []

    def issue(code, reference):
        # 只输出校验后的标识和固定错误码，不输出理由、权限原码或任何凭证字段。
        issues.append({'code': code, 'reference': reference})

    def indexed(key, field='id'):
        result = {}
        for row in document[key]:
            if row[field] in result:
                issue(IssueCode.DUPLICATE_IDENTITY, key + ':' + row[field])
            result[row[field]] = row
        return result

    if document['sourceIdentifier'] == document['targetIdentifier']:
        issue(IssueCode.SAME_SOURCE_TARGET, 'manifest')
    tables = indexed('tables', 'name')
    for name in sorted(set(tables) - set(catalog)):
        issue(IssueCode.UNKNOWN_TABLE, name)
    for name in sorted(set(catalog) - set(tables)):
        issue(IssueCode.MISSING_TABLE, name)
    for name, table in tables.items():
        if name in catalog and (set(table['columns']) != set(catalog[name]['columns'])
                                or len(table['columns']) != len(set(table['columns']))):
            issue(IssueCode.SCHEMA_DRIFT, name)
        if table['rowCount'] is None:
            issue(IssueCode.ROW_COUNT_MISSING, name)
    accounts = indexed('accounts')
    tenants = indexed('tenants')
    memberships = indexed('memberships')
    departments = indexed('departments')
    permissions = indexed('permissions')
    targets = indexed('targetActions')
    owners = indexed('ownerMappings', 'tenantId')
    actions = indexed('actionMappings', 'permissionId')
    for key, table in [('accounts', 'sys_user'), ('tenants', 'sys_tenant'), ('memberships', 'sys_user_tenant'),
                       ('departments', 'tenant_dept'), ('permissions', 'platform_permission')]:
        if table in tables and tables[table]['rowCount'] != len(document[key]):
            issue(IssueCode.FACTS_INCOMPLETE, table)
    for account in accounts.values():
        if not account['deleted'] and not account['credentialVerified']:
            issue(IssueCode.CREDENTIAL_UNVERIFIED, account['id'])
    relations = set()
    for member in memberships.values():
        relation = (member['accountId'], member['tenantId'])
        if relation in relations:
            issue(IssueCode.DUPLICATE_IDENTITY, member['id'])
        relations.add(relation)
        if member['accountId'] not in accounts or member['tenantId'] not in tenants:
            issue(IssueCode.ORPHAN_REFERENCE, member['id'])
    for tenant in tenants.values():
        if tenant['deleted']:
            continue
        owner = owners.get(tenant['id'])
        account = accounts.get(owner['accountId']) if owner else None
        if not owner or not account or account['deleted'] or not account['enabled'] or (owner['accountId'], tenant['id']) not in relations:
            issue(IssueCode.OWNER_UNRESOLVED, tenant['id'])
    for owner in owners.values():
        if owner['tenantId'] not in tenants:
            issue(IssueCode.ORPHAN_REFERENCE, owner['tenantId'])
    for department in departments.values():
        if department['tenantId'] not in tenants:
            issue(IssueCode.ORPHAN_REFERENCE, department['id'])
        parent_id = department['parentId']
        if parent_id is not None:
            parent = departments.get(parent_id)
            if parent is None:
                issue(IssueCode.ORPHAN_REFERENCE, department['id'])
            elif parent['tenantId'] != department['tenantId']:
                issue(IssueCode.CROSS_TENANT_REFERENCE, department['id'])
    # 每个节点只遍历一次，长部门链不会形成 O(n²) 预检。
    completed = set()
    for department_id in departments:
        path, positions = [], {}
        current_id = department_id
        while current_id in departments and current_id not in completed:
            if current_id in positions:
                for cyclic_id in path[positions[current_id]:]:
                    issue(IssueCode.DEPARTMENT_CYCLE, cyclic_id)
                break
            positions[current_id] = len(path)
            path.append(current_id)
            current_id = departments[current_id]['parentId']
        completed.update(path)
    target_codes = set()
    for target in targets.values():
        if any(token in target['code'] for token in ('*', '?')) or target['code'] in target_codes:
            issue(IssueCode.ACTION_UNRESOLVED, target['id'])
        target_codes.add(target['code'])
    for permission in permissions.values():
        mapping = actions.get(permission['id'])
        if not mapping or not mapping['actionIds'] or len(mapping['actionIds']) != len(set(mapping['actionIds'])) or any(a not in targets for a in mapping['actionIds']):
            issue(IssueCode.ACTION_UNRESOLVED, permission['id'])
    for mapping in actions.values():
        if mapping['permissionId'] not in permissions:
            issue(IssueCode.ORPHAN_REFERENCE, mapping['permissionId'])
    candidates = indexed('platformCandidates', 'accountId')
    platform_members = indexed('platformMappings', 'accountId')
    member_ids = set()
    for account_id, mapping in platform_members.items():
        account = accounts.get(account_id)
        if account_id not in candidates or not account or account['deleted'] or mapping['memberId'] in member_ids:
            issue(IssueCode.PLATFORM_IDENTITY_UNREVIEWED, account_id)
        member_ids.add(mapping['memberId'])
    for account_id in set(candidates) - set(platform_members):
        issue(IssueCode.PLATFORM_IDENTITY_UNREVIEWED, account_id)
    if not document['platformIdentityReviewed']:

        issue(IssueCode.PLATFORM_IDENTITY_UNREVIEWED, 'platform')
    return {
        **{key: document[key] for key in ('batchId', 'sourceFingerprint', 'sourceIdentifier', 'targetIdentifier', 'rulesVersion')},
        'manifestFingerprint': hashlib.sha256(json.dumps(document, sort_keys=True, separators=(',', ':')).encode()).hexdigest(),
        'status': PreflightStatus.BLOCKED if issues else PreflightStatus.PREFLIGHTED,
        'readyForImport': False, 'readyForCutover': False,
        'tables': [{'name': name, 'rowCount': tables[name]['rowCount'],
                    'disposition': catalog[name]['disposition'] if name in catalog else None} for name in sorted(tables)],
        'accountStateCounts': {'enabled': sum(a['enabled'] and not a['deleted'] for a in accounts.values()),
                               'disabled': sum(not a['enabled'] and not a['deleted'] for a in accounts.values()),
                               'deleted': sum(a['deleted'] for a in accounts.values())},
        'issues': issues,
    }


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--manifest', required=True, type=Path)
    parser.add_argument('--output', required=True, type=Path)
    args = parser.parse_args()
    try:
        require(args.manifest.resolve() != args.output.resolve())
        document = json.loads(args.manifest.read_text())
        result = preflight(document)
        if args.output.exists():
            previous = json.loads(args.output.read_text())
            require(previous.get('batchId') == result['batchId'] and previous.get('manifestFingerprint') == result['manifestFingerprint'])
        args.output.write_text(json.dumps(result, ensure_ascii=False, indent=2) + '\n')
    except (InvalidManifest, json.JSONDecodeError, OSError, TypeError, KeyError):
        print('预检失败：输入或输出不符合安全清单约定；未执行数据库操作', file=sys.stderr)
        return 2
    print(f"{result['status']}: {len(result['issues'])} blocking issues; import/cutover disabled")
    return 1 if result['issues'] else 0


if __name__ == '__main__':
    raise SystemExit(main())
