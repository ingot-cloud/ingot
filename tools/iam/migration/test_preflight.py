#!/usr/bin/env python3
"""用合成脱敏元数据验证预检阻塞规则；不接入实际业务快照。"""
import copy
import json
from pathlib import Path
import subprocess
import sys
import tempfile
import unittest

from preflight import CATALOG, InvalidManifest, IssueCode, PreflightStatus, preflight


def fixture():
    catalog = json.loads(CATALOG.read_text())['tables']
    counts = {'sys_user': 2, 'sys_tenant': 1, 'sys_user_tenant': 2, 'tenant_dept': 1, 'platform_permission': 1}
    return {
        'batchId': 'fixture-1', 'sourceFingerprint': 'a' * 64,
        'sourceIdentifier': 'source-snapshot', 'targetIdentifier': 'isolated-target', 'rulesVersion': 'fixture-v1',
        'tables': [{'name': name, 'columns': spec['columns'], 'rowCount': counts.get(name, 0)} for name, spec in catalog.items()],
        'accounts': [{'id': '1', 'enabled': True, 'deleted': False, 'credentialVerified': True},
                     {'id': '2', 'enabled': False, 'deleted': False, 'credentialVerified': True}],
        'tenants': [{'id': '10', 'enabled': True, 'deleted': False}],
        'memberships': [{'id': '101', 'accountId': '1', 'tenantId': '10'}, {'id': '102', 'accountId': '2', 'tenantId': '10'}],
        'departments': [{'id': '11', 'tenantId': '10', 'parentId': None}],
        'permissions': [{'id': '21', 'code': 'legacy:*'}],
        'ownerMappings': [{'tenantId': '10', 'accountId': '1', 'actor': 'reviewer', 'reason': '显式确认所有者'}],
        'actionMappings': [{'permissionId': '21', 'actionIds': ['31'], 'actor': 'reviewer', 'reason': '仅映射当前查看操作'}],
        'targetActions': [{'id': '31', 'code': 'iam-tenant:member:read'}],
        'platformIdentityReviewed': True,
        'platformCandidates': [{'accountId': '1'}],
        'platformMappings': [{'accountId': '1', 'memberId': '1001', 'actor': 'reviewer', 'reason': '确认独立平台成员'}],
    }


class PreflightTest(unittest.TestCase):
    def codes(self, document):
        return {item['code'] for item in preflight(document)['issues']}

    def test_complete_metadata_is_not_import_or_cutover_approval(self):
        document = fixture()
        before = copy.deepcopy(document)
        report = preflight(document)
        self.assertEqual(PreflightStatus.PREFLIGHTED, report['status'])
        self.assertFalse(report['readyForImport'])
        self.assertFalse(report['readyForCutover'])
        self.assertEqual({'enabled': 1, 'disabled': 1, 'deleted': 0}, report['accountStateCounts'])
        self.assertEqual(before, document)

    def test_unknown_table_and_column_drift_block(self):
        document = fixture()
        document['tables'].append({'name': 'unreviewed_table', 'columns': ['id'], 'rowCount': 0})
        document['tables'][0]['columns'] = ['unexpected_column']
        self.assertTrue({IssueCode.UNKNOWN_TABLE, IssueCode.SCHEMA_DRIFT} <= self.codes(document))

    def test_missing_table_and_incomplete_facts_block(self):
        document = fixture()
        document['tables'].pop(0)
        document['accounts'].pop()
        self.assertTrue({IssueCode.MISSING_TABLE, IssueCode.FACTS_INCOMPLETE} <= self.codes(document))

    def test_owner_is_not_inferred_from_members_or_role_names(self):
        document = fixture()
        document['ownerMappings'] = []
        self.assertIn(IssueCode.OWNER_UNRESOLVED, self.codes(document))
        document['ownerMappings'] = [{'tenantId': '10', 'accountId': '2', 'actor': 'reviewer', 'reason': '停用账号不能当所有者'}]
        self.assertIn(IssueCode.OWNER_UNRESOLVED, self.codes(document))

    def test_member_pairs_and_orphans_block(self):
        document = fixture()
        document['memberships'][1]['accountId'] = '1'
        self.assertIn(IssueCode.DUPLICATE_IDENTITY, self.codes(document))
        document['memberships'][1]['accountId'] = '999'
        self.assertIn(IssueCode.ORPHAN_REFERENCE, self.codes(document))

    def test_department_cycles_and_cross_tenant_parents_block(self):
        document = fixture()
        document['departments'][0]['parentId'] = '11'
        self.assertIn(IssueCode.DEPARTMENT_CYCLE, self.codes(document))
        document['departments'].append({'id': '12', 'tenantId': '20', 'parentId': None})
        document['departments'][0]['parentId'] = '12'
        self.assertIn(IssueCode.CROSS_TENANT_REFERENCE, self.codes(document))

    def test_long_department_chain_does_not_require_recursive_traversal(self):
        document = fixture()
        document['departments'] = [{'id': str(i), 'tenantId': '10', 'parentId': str(i + 1) if i < 10000 else None}
                                   for i in range(1, 10001)]
        for table in document['tables']:
            if table['name'] == 'tenant_dept':
                table['rowCount'] = 10000
        self.assertNotIn(IssueCode.DEPARTMENT_CYCLE, self.codes(document))

    def test_unknown_credential_format_blocks_without_reporting_hash(self):
        document = fixture()
        document['accounts'][0]['credentialVerified'] = False
        self.assertIn(IssueCode.CREDENTIAL_UNVERIFIED, self.codes(document))
        document['accounts'][0]['passwordHash'] = 'secret-must-not-be-reported'
        with self.assertRaises(InvalidManifest) as error:
            preflight(document)
        self.assertNotIn('secret-must-not-be-reported', str(error.exception))

    def test_wildcard_needs_explicit_exact_action_mapping(self):
        document = fixture()
        document['actionMappings'] = []
        self.assertIn(IssueCode.ACTION_UNRESOLVED, self.codes(document))
        document = fixture()
        document['targetActions'][0]['code'] = 'iam-tenant:*'
        self.assertIn(IssueCode.ACTION_UNRESOLVED, self.codes(document))

    def test_platform_identity_requires_explicit_mapping(self):
        document = fixture()
        document['platformMappings'] = []
        self.assertIn(IssueCode.PLATFORM_IDENTITY_UNREVIEWED, self.codes(document))

    def test_reports_never_echo_review_text_or_legacy_codes(self):
        document = fixture()
        document['ownerMappings'][0]['reason'] = 'private-review-text'
        text = json.dumps(preflight(document))
        self.assertNotIn('private-review-text', text)
        self.assertNotIn('legacy:*', text)

    def test_same_database_identity_is_rejected(self):
        document = fixture()
        document['targetIdentifier'] = document['sourceIdentifier']
        self.assertIn(IssueCode.SAME_SOURCE_TARGET, self.codes(document))

    def test_cli_never_overwrites_source_or_reuses_changed_batch(self):
        with tempfile.TemporaryDirectory() as directory:
            source, output = Path(directory) / 'source.json', Path(directory) / 'report.json'
            source.write_text(json.dumps(fixture()))
            script = str(Path(__file__).with_name('preflight.py'))
            run = lambda target: subprocess.run([sys.executable, script, '--manifest', str(source), '--output', str(target)], capture_output=True, text=True)
            before = source.read_bytes()
            self.assertEqual(2, run(source).returncode)
            self.assertEqual(before, source.read_bytes())
            self.assertEqual(0, run(output).returncode)
            report = output.read_bytes()
            changed = fixture(); changed['rulesVersion'] = 'changed-version'
            source.write_text(json.dumps(changed))
            self.assertEqual(2, run(output).returncode)
            self.assertEqual(report, output.read_bytes())


if __name__ == '__main__':
    unittest.main()
