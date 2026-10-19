"""未上线开发准备脚本在一次性MySQL验证，不连接已有业务库。"""
import json
import sys
import unittest
import uuid
from pathlib import Path

ROOT = Path(__file__).resolve().parents[3]
sys.path.insert(0, str(ROOT))
from databases.iam import test_identity_schema as schema
from tools.iam.tenant_live.legacy_fixture import read_legacy_schema

SCRIPT = ROOT / 'databases/iam/dev/prepare_tenant_live_test.sql'


class DevPrepareSqlTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        schema.IdentitySchemaTest.setUpClass.__func__(cls)

    @classmethod
    def stop_container(cls):
        schema.IdentitySchemaTest.stop_container.__func__(cls)

    sql = schema.IdentitySchemaTest.sql

    def setUp(self):
        self.database = 'iam_dev_prepare_' + uuid.uuid4().hex
        self.sql(f'CREATE DATABASE `{self.database}`', database=False)
        for path in schema.schema_files():
            self.sql(read_legacy_schema(path))
        self.sql((ROOT / 'databases/iam/006_bootstrap.sql').read_text())
        # Reproduce the old catalog without depending on the mutable repository HEAD.
        self.sql("UPDATE iam_action SET code='iam-tenant:role:publish' WHERE code='iam-tenant:role:update';"
                 "UPDATE iam_action SET code='iam-platform:shared-role:publish' WHERE code='iam-platform:shared-role:update';"
                 "UPDATE iam_action SET enabled=TRUE WHERE code='iam-tenant:role:upgrade';")
        self.sql("""
            INSERT INTO iam_account(id,username,password_hash) VALUES(1,'fixture-owner','not-a-credential'),(2,'fixture-member','not-a-credential');
            INSERT INTO iam_platform_member(id,account_id,display_name) VALUES(1001,1,'平台');
            INSERT INTO iam_tenant(id,name) VALUES(10,'未上线组织');
            INSERT INTO iam_tenant_member(id,tenant_id,account_id,display_name) VALUES(101,10,1,'拥有者'),(102,10,2,'成员');
            UPDATE iam_tenant SET owner_member_id=101 WHERE id=10;
            INSERT INTO iam_department(id,tenant_id,name) VALUES(11,10,'研发');
            INSERT INTO iam_tenant_app_entitlement(id,tenant_id,application_id,enabled,source)
                SELECT 500,10,id,TRUE,'INITIALIZATION' FROM iam_application WHERE code='iam-tenant';
            INSERT INTO iam_app_audience(tenant_id,application_id,audience_kind)
                SELECT 10,id,'ALL' FROM iam_application WHERE code='iam-tenant';
            INSERT INTO iam_role_definition(id,domain,tenant_id,kind,code,name) VALUES
                (700001,'TENANT',NULL,'SHARED','fixture-shared','保留共享'),
                (700002,'TENANT',10,'TENANT_CUSTOM','fixture-private','旧私有'),
                (700003,'PLATFORM',NULL,'PLATFORM_CUSTOM','fixture-platform','平台测试成果');
            INSERT INTO iam_role_revision(id,role_id,kind,revision,base_revision_id,metadata_overrides,resource_field_permissions) VALUES
                (700011,700001,'SHARED',1,NULL,'{}','{}'),
                (700012,700001,'SHARED',2,NULL,'{}','{}'),
                (700013,700002,'TENANT_CUSTOM',1,700012,'{}','{}'),
                (700014,700003,'PLATFORM_CUSTOM',1,NULL,'{}','{"example":"preserved"}');
            INSERT INTO iam_role_grant(revision_id,action_id,scopes)
                SELECT 700011,id,'[{"kind":"ALL"}]' FROM iam_action WHERE code='iam-tenant:member:read';
            INSERT INTO iam_role_grant(revision_id,action_id,scopes)
                SELECT 700012,id,'[{"kind":"MANAGED_DEPARTMENTS","parameterKey":"legacy-departments","includeDescendants":true}]'
                FROM iam_action WHERE code IN ('iam-tenant:member:read','iam-tenant:department:read');
            INSERT INTO iam_role_parameter(revision_id,parameter_key,binding_kind) VALUES
                (700012,'legacy-departments','DEPARTMENTS'),(700012,'unused-old-key','OBJECTS');
            INSERT INTO iam_role_grant(revision_id,action_id,scopes)
                SELECT 700014,id,'[{"kind":"ALL"}]' FROM iam_action WHERE code='iam-platform:member:read';
            INSERT INTO iam_role_assignment(id,domain,subject_type,platform_member_id,revision_id,revision_kind,scope_bindings,valid_from,source)
                VALUES(90001,'PLATFORM','MEMBER',1001,700014,'PLATFORM_CUSTOM','{}','2000-01-01','MANUAL');
            INSERT INTO iam_delegation_grant(id,domain,platform_administrator_id,max_assignment_duration_seconds)
                VALUES(90002,'PLATFORM',1001,3600);
            INSERT INTO iam_delegation_role_revision VALUES(90002,700014,'PLATFORM_CUSTOM');
            INSERT INTO iam_delegation_grant(id,domain,tenant_id,tenant_administrator_id,max_assignment_duration_seconds)
                VALUES(90003,'TENANT',10,101,3600);
            INSERT INTO iam_delegation_role_revision VALUES(90003,700012,'SHARED');
            INSERT INTO iam_role_assignment(id,domain,tenant_id,subject_type,tenant_member_id,revision_id,revision_kind,scope_bindings,valid_from,source)
                VALUES(90004,'TENANT',10,'MEMBER',102,700012,'SHARED','{}','2000-01-01','MANUAL');
            INSERT INTO iam_role_assignment(id,domain,tenant_id,subject_type,tenant_member_id,revision_id,revision_kind,scope_bindings,valid_from,source)
                SELECT 90005,'TENANT',10,'MEMBER',101,revision.id,'SYSTEM','{}','2000-01-01','INITIALIZATION'
                FROM iam_role_revision revision JOIN iam_role_definition role ON role.id=revision.role_id
                WHERE role.domain='TENANT' AND role.kind='SYSTEM' ORDER BY revision.revision DESC LIMIT 1;
            INSERT INTO iam_role_application_task(id,role_id,target_revision_id,actor_account_id,actor_member_id,created_by,selection,selection_snapshot,status,prepare_key)
                VALUES(91001,700003,700014,1,1001,1001,'ALL','{}','READY','platform-task'),
                      (91002,700001,700012,1,1001,1001,'ALL','{}','RUNNING','old-shared-task');
        """)
        self.sql((ROOT / 'databases/iam/migrations/021_tenant_live_authorization.sql').read_text())
        self.sql((ROOT / 'databases/iam/migrations/022_tenant_live_authorization_constraints.sql').read_text(), error=3819)

    def snapshot(self, selections):
        return {table: self.sql(f'SELECT * FROM {table} WHERE {condition}') for table, condition in selections.items()}

    def test_repair_022_failure_and_preserve_platform_and_shared_permissions(self):
        platform = {
            'iam_role_definition': "domain='PLATFORM'",
            'iam_role_revision': "role_id IN (SELECT id FROM iam_role_definition WHERE domain='PLATFORM')",
            'iam_role_grant': "revision_id IN (SELECT r.id FROM iam_role_revision r JOIN iam_role_definition d ON d.id=r.role_id WHERE d.domain='PLATFORM')",
            'iam_role_assignment': "domain='PLATFORM'",
            'iam_delegation_grant': "domain='PLATFORM'",
            'iam_delegation_role_revision': "delegation_id IN (SELECT id FROM iam_delegation_grant WHERE domain='PLATFORM')",
            'iam_role_application_task': "role_id IN (SELECT id FROM iam_role_definition WHERE domain='PLATFORM')",
            'iam_account': 'TRUE', 'iam_platform_member': 'TRUE', 'iam_tenant': 'TRUE',
            'iam_tenant_member': 'TRUE', 'iam_department': 'TRUE', 'iam_tenant_app_entitlement': 'TRUE',
            'iam_app_audience': 'TRUE', 'iam_default_policy_revision': 'TRUE',
        }
        before = self.snapshot(platform)
        historical = self.sql('SELECT * FROM iam_role_grant WHERE revision_id=700012')
        self.assertIn('TENANT_LIVE_TEST_READY', self.sql(SCRIPT.read_text()))
        self.assertEqual(before, self.snapshot(platform))
        self.assertEqual(historical, self.sql('SELECT * FROM iam_role_grant WHERE revision_id=700012'))
        head = self.sql('SELECT current_revision_id FROM iam_role_definition WHERE id=700001')
        self.assertEqual('2', self.sql(f'SELECT COUNT(*) FROM iam_role_grant WHERE revision_id={head}'))
        self.assertEqual('2', self.sql(f'SELECT COUNT(*) FROM iam_role_parameter WHERE revision_id={head} AND resource_id IS NOT NULL'))
        for row in self.sql(f'SELECT scopes FROM iam_role_grant WHERE revision_id={head}').splitlines():
            scope = json.loads(row)[0]
            self.assertEqual('MANAGED_DEPARTMENTS', scope['kind'])
            self.assertTrue(scope['includeDescendants'])
            self.assertRegex(scope['parameterKey'], r'^resource-[0-9]+-departments$')
        private = self.sql('SELECT current_revision_id FROM iam_role_definition WHERE id=700002')
        self.assertEqual('0', self.sql(f'SELECT COUNT(*) FROM iam_role_grant WHERE revision_id={private}'))
        self.assertEqual('1', self.sql(f'SELECT base_revision_id IS NULL FROM iam_role_revision WHERE id={private}'))
        self.assertEqual('REVOKED', self.sql('SELECT status FROM iam_role_assignment WHERE id=90004'))
        self.assertEqual('REVOKED', self.sql('SELECT status FROM iam_delegation_grant WHERE id=90003'))
        self.assertEqual('BLOCKED', self.sql('SELECT status FROM iam_role_application_task WHERE id=91002'))
        self.assertEqual('1', self.sql("SELECT COUNT(*) FROM information_schema.table_constraints WHERE constraint_schema=DATABASE() AND constraint_name='ck_iam_assignment_current_role'"))
        self.sql("UPDATE iam_role_assignment SET status='ACTIVE' WHERE id=90004", error=3819)
        self.assertEqual('0', self.sql("SELECT COUNT(*) FROM iam_action WHERE code IN ('iam-tenant:role:publish','iam-platform:shared-role:publish')"))
        self.assertEqual('1', self.sql("SELECT enabled FROM iam_action WHERE code='iam-platform:role:publish'"))
        self.assertEqual('1', self.sql("SELECT enabled FROM iam_action WHERE code='iam-platform:assignment:upgrade'"))

    def test_replay_keeps_new_current_test_data(self):
        self.sql(SCRIPT.read_text())
        self.sql("""
            INSERT INTO iam_delegation_grant(id,domain,tenant_id,tenant_administrator_id,max_assignment_duration_seconds)
                VALUES(92001,'TENANT',10,101,3600);
            INSERT INTO iam_delegation_role(delegation_id,tenant_id,role_id,role_kind,role_tenant_key)
                VALUES(92001,10,700001,'SHARED',0);
            INSERT INTO iam_role_assignment(id,domain,tenant_id,subject_type,tenant_member_id,role_id,role_kind,role_tenant_key,scope_bindings,valid_from,source)
                VALUES(92002,'TENANT',10,'MEMBER',102,700001,'SHARED',0,'{}','2000-01-01','MANUAL');
        """)
        tables = ['iam_role_definition','iam_role_revision','iam_role_grant','iam_role_parameter',
                  'iam_role_assignment','iam_delegation_grant','iam_delegation_role','iam_action','iam_menu_action']
        before = self.snapshot({table: 'TRUE' for table in tables})
        self.sql(SCRIPT.read_text())
        self.assertEqual(before, self.snapshot({table: 'TRUE' for table in tables}))
        self.assertEqual('ACTIVE', self.sql('SELECT status FROM iam_role_assignment WHERE id=92002'))
        self.assertEqual('ACTIVE', self.sql('SELECT status FROM iam_delegation_grant WHERE id=92001'))

    def test_missing_structure_fails_before_data_changes(self):
        self.sql('DROP TABLE iam_live_authorization_mapping')
        before = self.sql('SELECT * FROM iam_role_assignment')
        self.sql(SCRIPT.read_text(), error=1644)
        self.assertEqual(before, self.sql('SELECT * FROM iam_role_assignment'))


if __name__ == '__main__':
    unittest.main()
