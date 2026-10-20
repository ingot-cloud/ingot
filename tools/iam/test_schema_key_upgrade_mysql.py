"""027/028迁移演练；仅使用隔离MySQL，不接受实际库连接。"""
import json
import os
import unittest
from database_sources import sources, read_source
from isolated_mysql import IsolatedMysql
from schema_comments import SNAPSHOT, upgrade as upgrade_comments, normalized, read_definitions
from schema_key_upgrade import upgrade, RECIPIENT, PASSWORD, ALLOCATOR, LONG_MAX, preflight


class SchemaKeyUpgradeMysqlTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.mysql = IsolatedMysql(os.environ.get('IAM_SCHEMA_MYSQL_IMAGE', 'mysql:8.4')).__enter__()
        cls.addClassCleanup(lambda: cls.mysql.__exit__(None, None, None))

    def query(self, statement):
        return self.mysql.sql(statement, 'key_upgrade')

    def setUp(self):
        self.mysql.sql('DROP DATABASE IF EXISTS key_upgrade;CREATE DATABASE key_upgrade')
        for source in sources('schemas') + sources('frameworkSchemas'):
            self.query(read_source(source))
        self.query('ALTER TABLE iam_delegation_recipient_member DROP PRIMARY KEY,DROP COLUMN id;'
                   'ALTER TABLE password_history ADD INDEX idx_user_id(user_id);'
                   "INSERT INTO biz_leaf_alloc(biz_tag,max_id,step) VALUES('iam',100000,100);")
        self.query("INSERT INTO iam_account(id,username,password_hash) VALUES(1,'a','fixture'),(2,'b','fixture');"
                   "INSERT INTO iam_platform_member(id,account_id,display_name) VALUES(1001,1,'a'),(1002,2,'b');"
                   "INSERT INTO iam_tenant(id,name) VALUES(10,'tenant');"
                   "INSERT INTO iam_tenant_member(id,tenant_id,account_id,display_name) VALUES(101,10,1,'a'),(102,10,2,'b');"
                   "INSERT INTO iam_delegation_grant(id,domain,platform_administrator_id,max_assignment_duration_seconds) VALUES(300,'PLATFORM',1001,3600);"
                   "INSERT INTO iam_delegation_grant(id,domain,tenant_id,tenant_administrator_id,max_assignment_duration_seconds) VALUES(301,'TENANT',10,101,3600);"
                   "INSERT INTO iam_delegation_recipient_member(delegation_id,domain,platform_member_id) VALUES(300,'PLATFORM',1002);"
                   "INSERT INTO iam_delegation_recipient_member(delegation_id,domain,tenant_id,tenant_member_id) VALUES(301,'TENANT',10,102);"
                   "INSERT INTO password_history(user_id,password_hash,sequence_number,version,created_at,updated_at) VALUES(1,'synthetic-hash',1,7,'2026-01-01','2026-01-02');")

    def projection(self):
        return (self.query('SELECT delegation_id,domain,tenant_id,tenant_key,platform_member_id,tenant_member_id,member_id FROM iam_delegation_recipient_member ORDER BY delegation_id'),
                self.query('SELECT * FROM password_history ORDER BY id'))

    def assert_read_only_rejection(self):
        calls = []
        def observed(sql):
            calls.append(sql); return self.query(sql)
        with self.assertRaises((ValueError, RuntimeError)): upgrade(observed, apply=True)
        self.assertTrue(all(sql.startswith(('SHOW CREATE', 'SELECT')) for sql in calls), calls)

    def test_upgrade_preserves_rows_matches_new_schema_and_repeats(self):
        before = self.projection()
        snapshot = json.loads(SNAPSHOT.read_text())['tables']
        old = read_definitions(self.query, snapshot)
        calls = []
        upgrade(lambda sql: (calls.append(sql), self.query(sql))[1])
        self.assertTrue(all(sql.startswith(('SHOW CREATE', 'SELECT')) for sql in calls))
        upgrade(self.query, apply=True)
        self.assertEqual(before, self.projection())
        self.assertEqual('100000\n100001', self.query('SELECT id FROM iam_delegation_recipient_member ORDER BY id').strip())
        self.assertEqual('100002', self.query("SELECT max_id FROM biz_leaf_alloc WHERE biz_tag='iam'").strip())
        current = read_definitions(self.query, snapshot)
        for name, ddl in current.items():
            self.assertEqual(normalized(snapshot[name]), normalized(ddl), name)
            if name not in [RECIPIENT, PASSWORD]: self.assertEqual(normalized(old[name]), normalized(ddl), name)
        upgrade(self.query, apply=True)
        self.assertEqual(current, read_definitions(self.query, snapshot))
        with self.assertRaises(ValueError): upgrade_comments(self.query, apply=True)

    def test_duplicate_recipient_and_cross_domain_constraints_remain(self):
        upgrade(self.query, apply=True)
        for statement in [
            "INSERT INTO iam_delegation_recipient_member(id,delegation_id,domain,platform_member_id) VALUES(100003,300,'PLATFORM',1002)",
            "INSERT INTO iam_delegation_recipient_member(id,delegation_id,domain,tenant_id,tenant_member_id) VALUES(100004,300,'TENANT',10,102)",
            "INSERT INTO iam_delegation_recipient_member(id,delegation_id,domain,tenant_member_id) VALUES(100005,300,'PLATFORM',102)",
            "INSERT INTO iam_delegation_recipient_member(delegation_id,domain,platform_member_id) VALUES(300,'PLATFORM',1001)",
        ]:
            with self.assertRaises(RuntimeError): self.query(statement)

    def test_nullable_column_and_committed_fill_resume_without_reallocation(self):
        self.query("ALTER TABLE iam_delegation_recipient_member ADD id BIGINT UNSIGNED NULL")
        self.assertEqual('nullable', preflight(self.query)[RECIPIENT])
        upgrade(self.query, apply=True)
        upgrade(self.query, rollback=True)
        self.query('ALTER TABLE iam_delegation_recipient_member ADD id BIGINT UNSIGNED NULL;'
                   'UPDATE iam_delegation_recipient_member SET id=delegation_id;')
        high = self.query("SELECT max_id FROM biz_leaf_alloc WHERE biz_tag='iam'")
        upgrade(self.query, apply=True)
        self.assertEqual('300\n301', self.query('SELECT id FROM iam_delegation_recipient_member ORDER BY id').strip())
        self.assertEqual(high, self.query("SELECT max_id FROM biz_leaf_alloc WHERE biz_tag='iam'"))

    def test_backfill_failure_rolls_back_reservation_and_can_resume(self):
        # 在UPDATE上注入失败，覆盖已更新高水位但未提交的事务中断。
        self.query("CREATE TRIGGER fail_027 BEFORE UPDATE ON iam_delegation_recipient_member FOR EACH ROW SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='fixture rollback'")
        with self.assertRaises(RuntimeError): upgrade(self.query, apply=True)
        self.assertEqual('100000', self.query("SELECT max_id FROM biz_leaf_alloc WHERE biz_tag='iam'").strip())
        self.assertEqual('2', self.query('SELECT COUNT(*) FROM iam_delegation_recipient_member WHERE id IS NULL').strip())
        self.query('DROP TRIGGER fail_027')
        upgrade(self.query, apply=True)
        self.assertEqual('100000\n100001', self.query('SELECT id FROM iam_delegation_recipient_member ORDER BY id').strip())

    def test_rollback_restores_structure_and_preserves_high_watermark(self):
        before = self.projection()
        upgrade(self.query, apply=True)
        high = self.query("SELECT max_id FROM biz_leaf_alloc WHERE biz_tag='iam'")
        upgrade(self.query, rollback=True)
        upgrade(self.query, rollback=True)
        self.assertEqual(before, self.projection())
        self.assertEqual('old', preflight(self.query)[RECIPIENT])
        self.assertEqual('old', preflight(self.query)[PASSWORD])
        self.assertEqual(high, self.query("SELECT max_id FROM biz_leaf_alloc WHERE biz_tag='iam'"))
        upgrade(self.query, apply=True)
        self.assertEqual('100002\n100003', self.query('SELECT id FROM iam_delegation_recipient_member ORDER BY id').strip())

    def test_empty_relation_does_not_consume_ids(self):
        self.query('DELETE FROM iam_delegation_recipient_member')
        upgrade(self.query, apply=True)
        self.assertEqual('100000', self.query("SELECT max_id FROM biz_leaf_alloc WHERE biz_tag='iam'").strip())

    def test_incoming_id_reference_rejects_rollback_before_index_recreation(self):
        upgrade(self.query, apply=True)
        self.query('CREATE TABLE fixture_reference(id BIGINT UNSIGNED,FOREIGN KEY(id) REFERENCES iam_delegation_recipient_member(id))')
        calls = []
        with self.assertRaises(ValueError):
            upgrade(lambda sql: (calls.append(sql), self.query(sql))[1], rollback=True)
        self.assertTrue(all(sql.startswith(('SHOW CREATE', 'SELECT')) for sql in calls))

    def test_rollback_does_not_require_a_new_available_leaf_range(self):
        self.query('ALTER TABLE iam_delegation_recipient_member ADD id BIGINT UNSIGNED NULL;'
                   f"UPDATE biz_leaf_alloc SET max_id={LONG_MAX} WHERE biz_tag='iam';")
        upgrade(self.query, rollback=True)
        self.assertEqual('old', preflight(self.query, reserving=False)[RECIPIENT])
        self.assertEqual(str(LONG_MAX), self.query("SELECT max_id FROM biz_leaf_alloc WHERE biz_tag='iam'").strip())

    def test_drift_missing_allocator_overflow_and_bad_ids_reject_before_ddl(self):
        self.query('ALTER TABLE password_history DROP INDEX uk_user_sequence')
        self.assert_read_only_rejection()
        self.query('ALTER TABLE password_history ADD UNIQUE INDEX uk_user_sequence(user_id,sequence_number);'
                   "DELETE FROM biz_leaf_alloc WHERE biz_tag='iam';")
        self.assert_read_only_rejection()
        self.query(f"INSERT INTO biz_leaf_alloc(biz_tag,max_id,step) VALUES('iam',{LONG_MAX},100)")
        self.assert_read_only_rejection()
        self.query("UPDATE biz_leaf_alloc SET max_id=100000 WHERE biz_tag='iam';"
                   'ALTER TABLE iam_delegation_recipient_member ADD id BIGINT UNSIGNED NULL;'
                   'UPDATE iam_delegation_recipient_member SET id=1;')
        self.assert_read_only_rejection()


if __name__ == '__main__': unittest.main()
