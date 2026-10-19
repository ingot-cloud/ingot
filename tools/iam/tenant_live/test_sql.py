"""正反转换在离线MySQL容器演练，测试只创建隔离库。"""
import json
import sys
import subprocess
import unittest
from pathlib import Path

ROOT=Path(__file__).resolve().parents[3]
sys.path.insert(0,str(ROOT))
sys.path.insert(0,str(Path(__file__).parent))
from databases.iam import test_identity_schema as schema
from migrate import preflight,forward,reverse,digest,verify
from export import export
from tools.iam.tenant_live.legacy_fixture import read_legacy_schema

class MigrationSqlTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):schema.IdentitySchemaTest.setUpClass.__func__(cls)
    @classmethod
    def stop_container(cls):schema.IdentitySchemaTest.stop_container.__func__(cls)
    def setUp(self):
        schema.IdentitySchemaTest.setUp(self)
        self.sql("UPDATE iam_resource SET scope_capabilities='[\"ALL\",\"SELF\"]'")
    sql=schema.IdentitySchemaTest.sql
    def source(self):return export(['docker','exec','--interactive',self.container,'mysql','--user=root'],self.database)
    def test_forward_replay_and_reverse_preserve_new_writes(self):
        self.sql("ALTER TABLE iam_role_assignment DROP CHECK ck_iam_assignment_current_role")
        self.sql("""
         INSERT INTO iam_role_grant VALUES(1,2,'[{"kind":"ALL"}]');
         INSERT INTO iam_role_revision(id,role_id,kind,revision,metadata_overrides) VALUES(5,1,'SHARED',2,'{}');
         INSERT INTO iam_role_grant VALUES(5,2,'[{"kind":"SELF"}]');
         INSERT INTO iam_role_definition(id,domain,kind,code,name) VALUES(4,'TENANT','SYSTEM','tenant-governance','旧治理');
         INSERT INTO iam_role_revision(id,role_id,kind,revision,metadata_overrides) VALUES(4,4,'SYSTEM',1,'{}');
         INSERT INTO iam_role_assignment(id,domain,tenant_id,subject_type,tenant_member_id,revision_id,revision_kind,scope_bindings,valid_from,source) VALUES
         (50,'TENANT',10,'MEMBER',101,4,'SYSTEM','{}','2000-01-01','INITIALIZATION'),
         (51,'TENANT',10,'MEMBER',102,1,'SHARED','{}','2000-01-01','MANUAL'),
         (52,'TENANT',10,'MEMBER',102,2,'TENANT_CUSTOM','{}','2000-01-01','MANUAL');
        """)
        report=preflight(self.source(),'sql-fixture');self.assertTrue(report['ready'],report['conflicts'])
        self.sql(forward(report))
        self.assertTrue(verify(self.source())['valid'],verify(self.source()))
        self.assertEqual('REVOKED',self.sql('SELECT status FROM iam_role_assignment WHERE id=50'))
        self.assertEqual('1\tSHARED',self.sql('SELECT role_id,role_kind FROM iam_role_assignment WHERE id=51'))
        self.assertEqual('[{"kind": "ALL"}]',self.sql('SELECT g.scopes FROM iam_role_definition d JOIN iam_role_grant g ON g.revision_id=d.current_revision_id WHERE d.id=2'))
        count=self.sql('SELECT COUNT(*) FROM iam_role_revision')
        self.sql(forward(report));self.assertEqual(count,self.sql('SELECT COUNT(*) FROM iam_role_revision'))
        self.sql((ROOT/'databases/iam/migrations/022_tenant_live_authorization_constraints.sql').read_text())
        self.sql("""
          INSERT INTO iam_role_definition(id,domain,tenant_id,kind,code,name) VALUES(10000,'TENANT',20,'TENANT_CUSTOM','new-private','新私有');
          INSERT INTO iam_role_revision(id,role_id,kind,revision,metadata_overrides) VALUES(10001,10000,'TENANT_CUSTOM',1,'{}');
          UPDATE iam_role_definition SET current_revision_id=10001 WHERE id=10000;
          INSERT INTO iam_role_grant VALUES(10001,2,'[{"kind":"ALL"}]');
          INSERT INTO iam_role_assignment(id,domain,tenant_id,subject_type,tenant_member_id,role_id,role_kind,role_tenant_key,scope_bindings,valid_from,source) VALUES(10002,'TENANT',20,'MEMBER',201,10000,'TENANT_CUSTOM',20,'{}','2000-01-01','MANUAL');
          UPDATE iam_tenant SET owner_member_id=102,version=version+1 WHERE id=10;
          INSERT INTO iam_delegation_grant(id,domain,tenant_id,tenant_administrator_id,max_assignment_duration_seconds,max_assignment_duration_nanos,assignment_duration_mode) VALUES(20000,'TENANT',10,101,86400,0,'LIMITED');
          INSERT INTO iam_delegation_role(delegation_id,tenant_id,role_id,role_kind,role_tenant_key) VALUES(20000,10,1,'SHARED',0);
          INSERT INTO iam_delegation_resource_ceiling VALUES(20000,2,'[{"kind":"SELF"}]','{}');
          INSERT INTO iam_delegation_recipient_member(domain,tenant_id,delegation_id,tenant_member_id) VALUES('TENANT',10,20000,102);

        """)
        current=self.source();back=reverse(current,'sql-fixture');self.sql(back)
        self.assertEqual('10001',self.sql('SELECT revision_id FROM iam_role_assignment WHERE id=10002'))
        self.assertEqual('1',self.sql("SELECT COUNT(*) FROM iam_role_assignment WHERE tenant_id=10 AND tenant_member_id=102 AND source='INITIALIZATION' AND status='ACTIVE'"))
        self.assertEqual('1',self.sql('SELECT COUNT(*) FROM iam_role_definition WHERE id=10000'))
        self.assertEqual('1',self.sql('SELECT COUNT(*) FROM iam_delegation_role_revision WHERE delegation_id=20000'))
        self.assertEqual('[{\"kind\": \"SELF\"}]',self.sql('SELECT scopes FROM iam_delegation_action_ceiling WHERE delegation_id=20000 AND action_id=2'))
        count=self.sql('SELECT COUNT(*) FROM iam_role_revision');self.sql(back);self.assertEqual(count,self.sql('SELECT COUNT(*) FROM iam_role_revision'))
    def test_observation_window_backup_restores_original_facts(self):
        self.sql("ALTER TABLE iam_role_assignment DROP CHECK ck_iam_assignment_current_role")
        self.sql("INSERT INTO iam_role_definition(id,domain,kind,code,name) VALUES(99,'TENANT','SYSTEM','tenant-governance','旧治理');INSERT INTO iam_role_revision(id,role_id,kind,revision,metadata_overrides) VALUES(99,99,'SYSTEM',1,'{}')")
        original=self.source()
        backup=subprocess.check_output(['docker','exec',self.container,'mysqldump','--user=root','--skip-comments','--set-gtid-purged=OFF',self.database],text=True)
        report=preflight(original,'backup-window');self.assertTrue(report['ready'],report['conflicts']);self.sql(forward(report))
        restored='iam_restore_'+self.database.removeprefix('iam_case_')
        self.sql(f"CREATE DATABASE `{restored}`",database=False)
        self.database=restored;self.sql(backup)
        self.assertEqual(digest(original),digest(self.source()))
        self.assertEqual('0',self.sql("SELECT COUNT(*) FROM iam_live_authorization_batch"))

    def test_current_source_change_is_detected_inside_transaction(self):
        report=preflight(self.source(),'stale');self.assertTrue(report['ready'],report['conflicts'])
        self.sql("UPDATE iam_role_definition SET version=version+1 WHERE id=1")
        self.sql(forward(report),error=1644)
        self.assertEqual('0',self.sql("SELECT COUNT(*) FROM iam_live_authorization_batch WHERE batch_id='stale'"))

    def test_incremental_structure_matches_new_database(self):
        canonical=self.database
        original=self.sql("SELECT table_name,column_name,column_type,is_nullable,COALESCE(column_default,'<NULL>'),extra,COALESCE(collation_name,'') FROM information_schema.columns WHERE table_schema=DATABASE() ORDER BY table_name,column_name")
        self.database='iam_incremental_'+canonical.removeprefix('iam_case_')
        self.sql(f"CREATE DATABASE `{self.database}`",database=False)
        for path in schema.schema_files():
            source=read_legacy_schema(path)
            self.sql(source)
        self.sql((ROOT/'databases/iam/migrations/021_tenant_live_authorization.sql').read_text())
        self.sql((ROOT/'databases/iam/migrations/022_tenant_live_authorization_constraints.sql').read_text())
        actual=self.sql("SELECT table_name,column_name,column_type,is_nullable,COALESCE(column_default,'<NULL>'),extra,COALESCE(collation_name,'') FROM information_schema.columns WHERE table_schema=DATABASE() ORDER BY table_name,column_name")
        self.assertEqual(original,actual)
        query="SELECT table_name,index_name,non_unique,seq_in_index,column_name FROM information_schema.statistics WHERE table_schema=DATABASE() ORDER BY table_name,index_name,seq_in_index"
        actual=self.sql(query);self.database=canonical;self.assertEqual(self.sql(query),actual)
    def test_reverse_rejects_writes_since_export_and_can_resume(self):
        self.sql("ALTER TABLE iam_role_assignment DROP CHECK ck_iam_assignment_current_role")
        self.sql("INSERT INTO iam_role_definition(id,domain,kind,code,name) VALUES(99,'TENANT','SYSTEM','tenant-governance','旧治理');INSERT INTO iam_role_revision(id,role_id,kind,revision,metadata_overrides) VALUES(99,99,'SYSTEM',1,'{}')")
        report=preflight(self.source(),'reverse-stale');self.assertTrue(report['ready'],report['conflicts']);self.sql(forward(report))
        current=self.source();back=reverse(current,'reverse-stale')
        self.sql("UPDATE iam_role_definition SET version=version+1 WHERE id=1")
        self.sql(back,error=1644)
        self.assertEqual('FORWARD',self.sql("SELECT state FROM iam_live_authorization_batch WHERE batch_id='reverse-stale'"))
        self.sql(reverse(self.source(),'reverse-stale'))
        self.assertEqual('REVERSE',self.sql("SELECT state FROM iam_live_authorization_batch WHERE batch_id='reverse-stale'"))

if __name__=='__main__':unittest.main()
