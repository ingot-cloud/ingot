"""Exercise migration 016 against old and mixed IAM contracts in isolated MySQL.

Run: python3 databases/iam/test_field_access_migration.py
Uses only a disposable mysql:8.4 container, never an existing database.
"""
import json
from pathlib import Path
import sys
import unittest

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT))
from databases.iam import test_bootstrap_seed as bootstrap_support
from tools.iam.generate_bootstrap import MEMBER_FIELDS

MIGRATION = Path(__file__).parent / 'migrations/016_field_access_control.sql'


class FieldAccessMigrationTest(unittest.TestCase):
    """Validate preservation, conservative grants, failure recovery and repeatability."""

    setUpClass = classmethod(bootstrap_support.BootstrapSeedTest.setUpClass.__func__)
    stop_container = classmethod(bootstrap_support.BootstrapSeedTest.stop_container.__func__)
    sql = bootstrap_support.BootstrapSeedTest.sql
    count = bootstrap_support.BootstrapSeedTest.count
    bootstrap = bootstrap_support.BootstrapSeedTest.bootstrap

    def setUp(self):
        bootstrap_support.BootstrapSeedTest.setUp(self)
        self.bootstrap()

    def legacy(self):
        self.sql("""
            ALTER TABLE iam_field_policy DROP CHECK ck_iam_field_operations;
            ALTER TABLE iam_field_policy DROP COLUMN operation_rules;
            ALTER TABLE iam_field_rule ADD COLUMN editable BOOLEAN NOT NULL DEFAULT FALSE;
            ALTER TABLE iam_field_rule ADD CONSTRAINT ck_iam_field_rule_editable
                CHECK (editable IN (0,1) AND (editable=0 OR visibility='FULL'));
        """)
        old_fields = [{k: v for k, v in f.items() if k != 'mask'} | {'sortable': f['key'] == 'displayName'}
                      for f in MEMBER_FIELDS]
        self.sql("UPDATE iam_resource SET field_capabilities = " + self.literal(old_fields) +
                 " WHERE code IN ('member','directory')")
        old_permissions = {'110011': {f['key']: {'visibility': 'FULL', 'editable': f['editable']}
                                    for f in MEMBER_FIELDS}}
        self.sql("UPDATE iam_role_revision SET resource_field_permissions = " + self.literal(old_permissions) +
                 " WHERE id=141001")
        old_defaults = {'fields': {f['key']: {'visibility': 'MASKED' if f['key'] in ('phone', 'email') else 'FULL',
                                            'editable': f['key'] not in ('phone', 'email')} for f in MEMBER_FIELDS}}
        self.sql("UPDATE iam_default_policy_revision SET definition = " + self.literal(old_defaults) + " WHERE kind='FIELD'")

    @staticmethod
    def literal(value):
        # Synthetic fixtures only; double backslashes and quote SQL text deliberately.
        return "'" + json.dumps(value, ensure_ascii=False).replace('\\', '\\\\').replace("'", "''") + "'"

    def migrate(self, error=None):
        return self.sql(MIGRATION.read_text(), error=error)

    def value(self, query):
        # mysql --batch escapes backslashes; HEX preserves arbitrary JSON logical keys.
        projection, tail = query.removeprefix('SELECT ').split(' FROM ', 1)
        encoded = self.sql(f'SELECT HEX({projection}) FROM {tail}')
        return json.loads(bytes.fromhex(encoded).decode('utf-8'))

    def rules(self):
        self.sql("""
            INSERT INTO iam_account (id, username, password_hash) VALUES (1, 'migration-fixture', 'not-a-credential');
            INSERT INTO iam_tenant (id, name) VALUES (10, 'Migration fixture');
            INSERT INTO iam_tenant_member (id, tenant_id, account_id, display_name) VALUES (101,10,1,'Fixture');
            UPDATE iam_tenant SET owner_member_id=101 WHERE id=10;
            INSERT INTO iam_department (id, tenant_id, name) VALUES (21,10,'Root');
            INSERT INTO iam_policy_selector (id, tenant_id) VALUES (501,10);
            INSERT INTO iam_policy_selector_member VALUES (10,501,101);
            INSERT INTO iam_policy_selector_department VALUES (10,501,21,TRUE);
            INSERT INTO iam_field_policy (tenant_id, default_revision_id) VALUES (10,150002);
            INSERT INTO iam_field_rule
                (id,tenant_id,scenario,field_key,viewer_selector_id,target_scope,scope_bindings,visibility,editable) VALUES
                (601,10,'MANAGEMENT','phone',501,'[{"kind":"ALL"}]','{}','FULL',TRUE),
                (602,10,'MANAGEMENT','email',501,'[{"kind":"SELF"}]','{}','FULL',TRUE),
                (603,10,'MANAGEMENT','displayName',501,'[{"kind":"SELF"}]','{}','FULL',FALSE),
                (604,10,'DIRECTORY','phone',501,'[]','{}','MASKED',FALSE);
        """)

    def test_old_resource_role_and_default_json_convert_without_new_grants(self):
        self.legacy()
        grants_before = self.sql('SELECT COUNT(*) FROM iam_role_grant')
        self.migrate()
        fields = self.value('SELECT field_capabilities FROM iam_resource WHERE id=110011')
        by_key = {f['key']: f for f in fields}
        self.assertEqual({'kind': 'PHONE'}, by_key['phone']['mask'])
        self.assertEqual({'kind': 'EMAIL'}, by_key['email']['mask'])
        self.assertEqual({'kind': 'ALL'}, by_key['displayName']['mask'])
        self.assertNotIn('mask', by_key['avatar'])
        self.assertTrue(all('sortable' not in f for f in fields))
        self.assertFalse(by_key['phone']['filterable'])
        self.assertEqual({'HIDDEN', 'FULL'}, set(by_key['joinedAt']['visibilities']))
        self.assertFalse(by_key['joinedAt']['editable'])
        role = self.value('SELECT resource_field_permissions FROM iam_role_revision WHERE id=141001')['110011']
        self.assertEqual({f['key']: 'FULL' for f in MEMBER_FIELDS}, role['visibility'])
        self.assertNotIn('joinedAt', role['visibility'])
        self.assertEqual({'editable': True, 'filterable': False}, role['operations']['phone'])
        self.assertEqual(grants_before, self.sql('SELECT COUNT(*) FROM iam_role_grant'))
        default = self.value("SELECT definition FROM iam_default_policy_revision WHERE kind='FIELD'")
        self.assertEqual('MASKED', default['fields']['phone'])
        self.assertEqual({'editable': False, 'filterable': True}, default['operations']['phone'])
        self.assertEqual('0', self.sql("SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='iam_field_rule' AND column_name='editable'"))

    def test_scoped_rules_close_global_edit_preserve_visibility_and_viewer(self):
        self.legacy()
        self.rules()
        original_privacy = self.sql('SELECT id, target_scope,scope_bindings,visibility FROM iam_field_rule ORDER BY id')
        self.migrate()
        self.assertEqual(original_privacy, self.sql('SELECT id,target_scope,scope_bindings,visibility FROM iam_field_rule ORDER BY id'))
        operations = self.value('SELECT operation_rules FROM iam_field_policy WHERE tenant_id=10')
        self.assertEqual(3, len(operations))
        by_key = {f['fieldKey']: f for f in operations}
        self.assertTrue(by_key['phone']['operations']['editable'])
        self.assertFalse(by_key['email']['operations']['editable'])
        self.assertFalse(by_key['displayName']['operations']['editable'])
        self.assertEqual({'members': ['101'], 'departments': [{'id': '21', 'includeDescendants': True}]}, by_key['phone']['viewerSelection'])
        self.assertTrue(all(f['actionCode'] == 'iam-tenant:member:update' for f in operations))
        self.assertEqual(2, self.count('iam_field_access_016_legacy_rule', 'requires_review=TRUE'))
        self.assertEqual(1, self.count('iam_account'))
        self.assertEqual(1, self.count('iam_tenant_member'))

    def test_repeat_keeps_versions_json_and_legacy_review_unchanged(self):
        self.legacy()
        self.rules()
        self.migrate()
        tables = ['iam_resource', 'iam_role_revision', 'iam_default_policy_revision', 'iam_field_policy', 'iam_field_access_016_legacy_rule']
        before = {t: self.sql(f'SELECT * FROM {t} ORDER BY 1') for t in tables}
        output = self.migrate()
        self.assertIn(self.database + '\t0\t0\t0\t0', output)
        for table in tables:
            self.assertEqual(before[table], self.sql(f'SELECT * FROM {table} ORDER BY 1'), table)

    def test_current_schema_and_seed_are_noop(self):
        tables = ['iam_resource', 'iam_role_revision', 'iam_default_policy_revision']
        before = {t: self.sql(f'SELECT * FROM {t} ORDER BY 1') for t in tables}
        output = self.migrate()
        self.assertIn(self.database + '\t0\t0\t0\t0', output)
        for table in tables:
            self.assertEqual(before[table], self.sql(f'SELECT * FROM {table} ORDER BY 1'))

    def test_mixed_and_quoted_logical_keys_preserve_new_masks_and_permissions(self):
        self.legacy()
        key = 'contact."phone'
        field = {'key': key, 'label': 'Contact', 'visibilities': ['FULL', 'MASKED', 'HIDDEN'],
                 'editable': True, 'filterable': False, 'mask': {'kind': 'KEEP_EDGES', 'prefix': 1, 'suffix': 2}}
        self.sql("UPDATE iam_resource SET field_capabilities = JSON_ARRAY_APPEND(field_capabilities,'$',CAST(" + self.literal(field) + " AS JSON)) WHERE id=110011")
        old = {'110011': {key: {'visibility': 'MASKED', 'editable': False}}}
        self.sql('UPDATE iam_role_revision SET resource_field_permissions=' + self.literal(old) + ' WHERE id=141001')
        new = {'fields': {'phone': 'MASKED'}, 'operations': {'phone': {'editable': True, 'filterable': True}}}
        self.sql("INSERT INTO iam_default_policy_revision VALUES (900001,'FIELD',2," + self.literal(new) + ")")
        self.migrate()
        by_key = {f['key']: f for f in self.value('SELECT field_capabilities FROM iam_resource WHERE id=110011')}
        self.assertEqual(field, by_key[key])
        role = self.value('SELECT resource_field_permissions FROM iam_role_revision WHERE id=141001')['110011']
        self.assertEqual('MASKED', role['visibility'][key])
        self.assertFalse(role['operations'][key]['editable'])
        self.assertEqual(new, self.value('SELECT definition FROM iam_default_policy_revision WHERE id=900001'))

    def test_legacy_ceiling_keeps_edit_denial_and_all_versions_are_converted(self):
        self.legacy()
        old = {'fields': {'phone': {'visibility': 'FULL', 'editable': True}},
               'ceiling': {'phone': {'visibility': 'FULL', 'editable': False}}}
        self.sql("INSERT INTO iam_default_policy_revision VALUES (900001,'FIELD',2," + self.literal(old) + ")")
        self.migrate()
        converted = self.value('SELECT definition FROM iam_default_policy_revision WHERE id=900001')
        self.assertEqual('FULL', converted['ceiling']['phone'])
        self.assertFalse(converted['operationCeiling']['phone']['editable'])
        self.assertTrue(converted['operations']['phone']['editable'])
        self.assertFalse(converted['operations']['email']['editable'])

    def test_custom_historical_roles_preserve_hidden_fields_and_action_scopes(self):
        self.legacy()
        old = {'110011': {'phone': {'visibility': 'MASKED', 'editable': False},
                         'email': {'visibility': 'HIDDEN', 'editable': False}}}
        new = {'110011': {'visibility': {'phone': 'MASKED'},
                         'operations': {'phone': {'editable': True, 'filterable': False}}}}
        self.sql("INSERT INTO iam_role_definition (id,domain,kind,code,name) VALUES (900001,'PLATFORM','PLATFORM_CUSTOM','migration-role','Migration role')")
        for revision, definition in enumerate([old, new, {'110011': {}}], start=1):
            self.sql("INSERT INTO iam_role_revision (id,role_id,kind,revision,metadata_overrides,resource_field_permissions) VALUES (" +
                     f"{900001 + revision},900001,'PLATFORM_CUSTOM',{revision},'{{}}'," + self.literal(definition) + ")")
        self.sql("INSERT INTO iam_role_grant (revision_id,action_id,scopes) SELECT 900002,id,'[{\"kind\":\"SELF\"}]' FROM iam_action WHERE code='iam-platform:member:read'")
        original = self.sql('SELECT * FROM iam_role_grant WHERE revision_id=900002')
        self.migrate()
        self.assertEqual(original, self.sql('SELECT * FROM iam_role_grant WHERE revision_id=900002'))
        converted = self.value('SELECT resource_field_permissions FROM iam_role_revision WHERE id=900002')['110011']
        self.assertEqual({'phone': 'MASKED', 'email': 'HIDDEN'}, converted['visibility'])
        self.assertTrue(all(v == {'editable': False, 'filterable': False} for v in converted['operations'].values()))
        self.assertEqual(new, self.value('SELECT resource_field_permissions FROM iam_role_revision WHERE id=900003'))
        self.assertEqual({'visibility': {}, 'operations': {}}, self.value('SELECT resource_field_permissions FROM iam_role_revision WHERE id=900004')['110011'])

    def test_bad_visibility_array_aborts_without_partial_resource_updates(self):
        self.legacy()
        bad = [{'key': 'phone', 'label': 'Phone', 'visibilities': ['FULL', 'FULL'], 'editable': True, 'filterable': True}]
        self.sql('UPDATE iam_resource SET field_capabilities=' + self.literal(bad) + ' WHERE id=110029')
        before = self.sql('SELECT field_capabilities,version FROM iam_resource WHERE id=110011')
        self.migrate(error=1644)
        self.assertEqual(before, self.sql('SELECT field_capabilities,version FROM iam_resource WHERE id=110011'))

    def test_legacy_logical_keys_named_visibility_and_operations_are_not_new_protocol(self):
        self.legacy()
        old = {'110011': {'visibility': {'visibility': 'FULL', 'editable': False},
                         'operations': {'visibility': 'HIDDEN', 'editable': False}}}
        self.sql('UPDATE iam_role_revision SET resource_field_permissions=' + self.literal(old) + ' WHERE id=141001')
        self.migrate()
        converted = self.value('SELECT resource_field_permissions FROM iam_role_revision WHERE id=141001')['110011']
        self.assertEqual({'visibility': 'FULL', 'operations': 'HIDDEN'}, converted['visibility'])
        self.assertTrue(all(v == {'editable': False, 'filterable': False} for v in converted['operations'].values()))

    def test_bad_legacy_role_aborts_dml_and_can_retry_after_correction(self):
        self.legacy()
        self.sql("UPDATE iam_role_revision SET resource_field_permissions='{\"110011\":{\"phone\":{\"visibility\":\"BAD\",\"editable\":true}}}' WHERE id=141001")
        original = self.sql('SELECT field_capabilities,version FROM iam_resource WHERE id=110011')
        self.migrate(error=1644)
        self.assertEqual(original, self.sql('SELECT field_capabilities,version FROM iam_resource WHERE id=110011'))
        self.assertEqual('1', self.sql("SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='iam_field_rule' AND column_name='editable'"))
        self.sql("UPDATE iam_role_revision SET resource_field_permissions='{\"110011\":{\"phone\":{\"visibility\":\"MASKED\",\"editable\":false}}}' WHERE id=141001")
        self.migrate()
        self.assertFalse(self.value('SELECT resource_field_permissions FROM iam_role_revision WHERE id=141001')['110011']['operations']['phone']['editable'])


if __name__ == '__main__':
    unittest.main()
