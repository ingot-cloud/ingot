"""Migration017 on disposable mysql:8.4; never connects to a user database."""
import json
from pathlib import Path
import sys
import unittest
ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT))
from databases.iam import test_bootstrap_seed as support

class RoleRevisionApplicationMigrationTest(unittest.TestCase):
    setUpClass = classmethod(support.BootstrapSeedTest.setUpClass.__func__)
    stop_container = classmethod(support.BootstrapSeedTest.stop_container.__func__)
    sql = support.BootstrapSeedTest.sql
    count = support.BootstrapSeedTest.count
    bootstrap = support.BootstrapSeedTest.bootstrap

    def setUp(self):
        support.BootstrapSeedTest.setUp(self)
        self.bootstrap()
        self.sql("""
          DROP TABLE iam_role_application_item;
          DROP TABLE iam_role_application_task;
          ALTER TABLE iam_role_revision DROP COLUMN version_name,DROP COLUMN remark,DROP COLUMN metadata_version;
          ALTER TABLE iam_role_assignment DROP INDEX ix_iam_assignment_revision_status;
        """)

    def snapshot(self):
        return self.sql("SELECT id,role_id,kind,revision,COALESCE(base_revision_id,0),CAST(resource_field_permissions AS CHAR) FROM iam_role_revision ORDER BY id")

    def test_additive_repeatable_preserves_permissions_and_fixed_assignments(self):
        before = self.snapshot()
        assignments = self.sql("SELECT id,revision_id,scope_bindings,version FROM iam_role_assignment ORDER BY id")
        migration = (Path(__file__).parent / 'migrations/017_role_revision_application.sql').read_text()
        self.sql(migration)
        self.sql("UPDATE iam_role_revision SET version_name='客服日常权限',remark='仅查询',metadata_version=1 WHERE id=141001")
        self.sql(migration)
        self.assertEqual(before, self.snapshot())
        self.assertEqual(assignments, self.sql("SELECT id,revision_id,scope_bindings,version FROM iam_role_assignment ORDER BY id"))
        self.assertEqual('客服日常权限\t仅查询\t1', self.sql('SELECT version_name,remark,metadata_version FROM iam_role_revision WHERE id=141001'))
        self.assertEqual(0, self.count('iam_role_application_task'))
        self.assertEqual(0, self.count('iam_role_application_item'))
        self.assertEqual('1', self.sql("SELECT COUNT(DISTINCT index_name) FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='iam_role_assignment' AND index_name='ix_iam_assignment_revision_status'"))

    def test_database_enforces_one_running_application_per_role(self):
        migration = (Path(__file__).parent / 'migrations/017_role_revision_application.sql').read_text()
        self.sql(migration)
        # Existing system role is used only to exercise DB constraints; service refuses its application.
        role, revision = self.sql('SELECT role_id,id FROM iam_role_revision ORDER BY id LIMIT 1').split('\t')
        command = f"INSERT INTO iam_role_application_task(id,role_id,target_revision_id,actor_account_id,actor_member_id,created_by,selection,selection_snapshot,status,prepare_key) VALUES(%s,{role},{revision},1,1,1,'ALL','{{}}','RUNNING',%s)"
        self.sql(command % (98701, "'first'"))
        self.sql(command % (98702, "'second'"), error=1062)
        self.sql('UPDATE iam_role_application_task SET status=\'PAUSED\' WHERE id=98701')
        self.sql(command % (98702, "'second'"))
        self.assertEqual(2, self.count('iam_role_application_task'))

if __name__ == '__main__': unittest.main()
