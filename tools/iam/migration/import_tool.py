#!/usr/bin/env python3
"""幂等导入、试运行与授权差异校验；不连接数据库，不默认覆盖源库。"""
import argparse
import hashlib
import json
from pathlib import Path
import sys

from preflight import InvalidManifest, require, preflight, identifier, FINGERPRINT

DISPOSITIONS = {'KEEP_MAPPED', 'NARROW', 'RECONFIGURE', 'ARCHIVE', 'SKIP'}
AUTH_RESULTS = {'PRESERVED', 'NARROWED', 'EXPANDED', 'UNRESOLVED'}


def load_json(path):
    return json.loads(Path(path).read_text())


def fingerprint(document):
    return hashlib.sha256(json.dumps(document, sort_keys=True, separators=(',', ':')).encode()).hexdigest()


def mapping_key(kind, old_id):
    return f'{kind}:{old_id}'


def ensure_batch(store, report):
    require(store.get('batchId') == report['batchId'])
    if store.get('sourceFingerprint') != report['sourceFingerprint']:
        raise InvalidManifest('源快照变化不能复用同一批次映射')
    require(store.get('manifestFingerprint') == report['manifestFingerprint'])


def new_store(report):
    return {
        'batchId': report['batchId'],
        'sourceFingerprint': report['sourceFingerprint'],
        'manifestFingerprint': report['manifestFingerprint'],
        'targetIdentifier': report['targetIdentifier'],
        'rulesVersion': report['rulesVersion'],
        'ids': {},
        'imported': False,
        'verified': False,
    }


def planned_rows(manifest, store):
    ids = store['ids']
    accounts = []
    for account in manifest['accounts']:
        key = mapping_key('account', account['id'])
        ids.setdefault(key, account['id'])
        accounts.append({'id': ids[key], 'sourceId': account['id'], 'enabled': account['enabled'],
                         'deleted': account['deleted']})
    members = []
    for member in manifest['memberships']:
        key = mapping_key('tenant_member', member['id'])
        ids.setdefault(key, member['id'])
        members.append({'id': ids[key], 'sourceId': member['id'],
                        'accountId': ids[mapping_key('account', member['accountId'])],
                        'tenantId': member['tenantId']})
    platform = []
    for mapping in manifest['platformMappings']:
        key = mapping_key('platform_member', mapping['memberId'])
        ids.setdefault(key, mapping['memberId'])
        platform.append({'id': ids[key], 'accountId': ids[mapping_key('account', mapping['accountId'])]})
    return {'accounts': accounts, 'tenantMembers': members, 'platformMembers': platform}


def compare_authorization(comparisons):
    require(isinstance(comparisons, list))
    issues = []
    counts = {name: 0 for name in AUTH_RESULTS}
    for row in comparisons:
        require(isinstance(row, dict) and set(row) >= {'subjectId', 'actionId', 'result'})
        result = row['result']
        require(result in AUTH_RESULTS)
        counts[result] += 1
        disposition = row.get('disposition')
        if result == 'EXPANDED' and disposition not in DISPOSITIONS:
            issues.append({'code': 'AUTHORIZATION_EXPANDED', 'reference': row['subjectId']})
        if result == 'UNRESOLVED':
            issues.append({'code': 'AUTHORIZATION_UNRESOLVED', 'reference': row['subjectId']})
    return counts, issues


def write_report(path, document):
    path = Path(path)
    path.write_text(json.dumps(document, ensure_ascii=False, indent=2) + '\n')


def prepare(args):
    manifest = load_json(args.manifest)
    report = preflight(manifest)
    if report['status'] != 'PREFLIGHTED':
        raise InvalidManifest('预检未通过，禁止试运行或导入')
    store_path = Path(args.mapping)
    if store_path.exists():
        store = load_json(store_path)
        ensure_batch(store, report)
    else:
        store = new_store(report)
    plan = planned_rows(manifest, store)
    return report, store, plan


def dry_run(args):
    report, store, plan = prepare(args)
    write_report(args.mapping, store)
    document = {
        'command': 'dry-run',
        'batchId': report['batchId'],
        'sourceFingerprint': report['sourceFingerprint'],
        'targetIdentifier': report['targetIdentifier'],
        'readyForImport': False,
        'readyForCutover': False,
        'plannedCounts': {key: len(value) for key, value in plan.items()},
        'issues': [],
    }
    write_report(args.output, document)
    return 0


def do_import(args):
    report, store, plan = prepare(args)
    target = Path(args.target_dir)
    target.mkdir(parents=True, exist_ok=True)
    for name, rows in plan.items():
        (target / f'{name}.json').write_text(json.dumps(rows, ensure_ascii=False, indent=2) + '\n')
    store['imported'] = True
    store['verified'] = False
    write_report(args.mapping, store)
    document = {
        'command': 'import',
        'batchId': report['batchId'],
        'sourceFingerprint': report['sourceFingerprint'],
        'targetIdentifier': report['targetIdentifier'],
        'imported': True,
        'verified': False,
        'readyForImport': False,
        'readyForCutover': False,
        'rowCounts': {key: len(value) for key, value in plan.items()},
        'issues': [],
    }
    write_report(args.output, document)
    return 0


def verify(args):
    report, store, plan = prepare(args)
    require(store.get('imported') is True)
    comparisons = load_json(args.authorization)
    counts, issues = compare_authorization(comparisons)
    verified = not issues
    store['verified'] = verified
    write_report(args.mapping, store)
    document = {
        'command': 'verify',
        'batchId': report['batchId'],
        'sourceFingerprint': report['sourceFingerprint'],
        'authorizationCounts': counts,
        'verified': verified,
        'readyForImport': False,
        'readyForCutover': False,
        'issues': issues,
    }
    write_report(args.output, document)
    return 0 if verified else 1


def report_cmd(args):
    store = load_json(args.mapping)
    require(identifier(store.get('batchId')))
    require(isinstance(store.get('sourceFingerprint'), str) and FINGERPRINT.fullmatch(store['sourceFingerprint']))
    document = {
        'command': 'report',
        'batchId': store['batchId'],
        'sourceFingerprint': store['sourceFingerprint'],
        'targetIdentifier': store['targetIdentifier'],
        'imported': bool(store.get('imported')),
        'verified': bool(store.get('verified')),
        'readyForImport': False,
        'readyForCutover': False,
        'mappedIdentities': len(store.get('ids', {})),
    }
    write_report(args.output, document)
    return 0


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    sub = parser.add_subparsers(dest='command', required=True)
    dry = sub.add_parser('dry-run')
    imp = sub.add_parser('import')
    ver = sub.add_parser('verify')
    rep = sub.add_parser('report')
    for item in (dry, imp, ver):
        item.add_argument('--manifest', required=True, type=Path)
        item.add_argument('--mapping', required=True, type=Path)
        item.add_argument('--output', required=True, type=Path)
    imp.add_argument('--target-dir', required=True, type=Path)
    ver.add_argument('--authorization', required=True, type=Path)
    rep.add_argument('--mapping', required=True, type=Path)
    rep.add_argument('--output', required=True, type=Path)
    args = parser.parse_args()
    try:
        if args.command == 'dry-run':
            return dry_run(args)
        if args.command == 'import':
            return do_import(args)
        if args.command == 'verify':
            return verify(args)
        return report_cmd(args)
    except (InvalidManifest, json.JSONDecodeError, OSError, TypeError, KeyError, ValueError):
        print('迁移工具失败：输入不符合批次/指纹约定；未连接数据库', file=sys.stderr)
        return 2


if __name__ == '__main__':
    raise SystemExit(main())
