"""本地无网络 MySQL：注释升级幂等、漂移阻断、数据保留及主关系并发约束。"""
import json
import os
import subprocess
import sys
import threading
import unittest
from database_sources import sources, read_source
from schema_model import COMMENT, tables
from schema_comments import upgrade, normalized, SNAPSHOT, read_definitions
from isolated_mysql import IsolatedMysql


class SchemaCommentsMysqlTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.mysql = IsolatedMysql(os.environ.get('IAM_SCHEMA_MYSQL_IMAGE','mysql:8.4')).__enter__()
        cls.addClassCleanup(lambda: cls.mysql.__exit__(None, None, None))

    def setUp(self):
        self.database = 'iam_comments'
        self.mysql.sql('DROP DATABASE IF EXISTS ' + self.database + '; CREATE DATABASE ' + self.database)
        for source in sources('schemas') + sources('frameworkSchemas'):
            self.mysql.sql(COMMENT.sub('', read_source(source)), self.database)
        # 026已固定在027/028之前；不能用最新DDL冒充旧版注释迁移输入。
        self.restore_026_structure(self.database)
        self.mysql.sql("INSERT INTO iam_account(id,username,password_hash) VALUES(1,'fixture','fixture');"
                       "INSERT INTO iam_tenant(id,name) VALUES(10,'fixture');"
                       "INSERT INTO iam_tenant_member(id,tenant_id,account_id,display_name) VALUES(101,10,1,'fixture');"
                       "INSERT INTO iam_department(id,tenant_id,name) VALUES(11,10,'A'),(12,10,'B');"
                       "INSERT INTO iam_member_department(tenant_id,member_id,department_id,is_primary) VALUES(10,101,11,1),(10,101,12,0)", self.database)

    def query(self, sql):
        return self.mysql.sql(sql, self.database)

    def restore_026_structure(self, database):
        self.mysql.sql('ALTER TABLE iam_delegation_recipient_member DROP PRIMARY KEY,DROP COLUMN id;'
                       'ALTER TABLE password_history ADD INDEX idx_user_id(user_id);', database)

    def test_upgrade_matches_new_schema_preserves_rows_and_is_repeatable(self):
        before = self.query('SELECT * FROM iam_member_department ORDER BY department_id')
        upgrade(self.query, apply=False)
        upgrade(self.query, apply=True)
        first = read_definitions(self.query, [t.name for t in tables()])
        self.assertEqual('fixture', self.query("SELECT display_name FROM iam_tenant_member WHERE id=101").strip())
        upgrade(self.query, apply=True)
        self.assertEqual(first, read_definitions(self.query, [t.name for t in tables()]))
        self.assertEqual(before, self.query('SELECT * FROM iam_member_department ORDER BY department_id'))
        reference = 'iam_new_comments'
        self.mysql.sql('DROP DATABASE IF EXISTS ' + reference + ';CREATE DATABASE ' + reference)
        for source in sources('schemas') + sources('frameworkSchemas'):
            self.mysql.sql(read_source(source), reference)
        self.restore_026_structure(reference)
        reference_definitions = read_definitions(lambda sql: self.mysql.sql(sql,reference), [t.name for t in tables()])
        for table in tables():
            self.assertEqual(normalized(first[table.name]), normalized(reference_definitions[table.name]), table.name)
        self.assertEqual('0', self.query("SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND column_comment='' ").strip())

    def test_drift_or_missing_table_stops_before_any_alter(self):
        self.query('ALTER TABLE iam_account MODIFY COLUMN username VARCHAR(65) COLLATE utf8mb4_bin NOT NULL')
        with self.assertRaises(ValueError):
            upgrade(self.query, apply=True)
        self.assertEqual('0', self.query("SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND column_comment<>''").strip())
        self.query('DROP TABLE password_history')
        with self.assertRaises(ValueError):
            upgrade(self.query, apply=True)

    def test_unique_primary_and_concurrent_version_check(self):
        with self.assertRaises(RuntimeError):
            self.query('UPDATE iam_member_department SET is_primary=1 WHERE department_id=12')
        # Same transaction ordering as the shared writer: demote existing before promoting new.
        self.query('START TRANSACTION; UPDATE iam_member_department SET is_primary=0 WHERE tenant_id=10 AND member_id=101 AND department_id=11;'
                   'UPDATE iam_member_department SET is_primary=1 WHERE tenant_id=10 AND member_id=101 AND department_id=12; COMMIT;')
        self.assertEqual('12', self.query('SELECT primary_member_id*0+department_id FROM iam_member_department WHERE is_primary=1').strip())
        # Two transactions contend on the same parent; only the original expected version can apply.
        barrier = threading.Barrier(2)
        results, errors = [], []
        def contender():
            try:
                barrier.wait(timeout=10)
                result = self.query('START TRANSACTION;SELECT id FROM iam_tenant WHERE id=10 FOR UPDATE;'
                                    'SELECT id FROM iam_tenant_member WHERE tenant_id=10 AND id=101 FOR UPDATE;'
                                    'UPDATE iam_tenant_member SET version=version+1 WHERE tenant_id=10 AND id=101 AND version=0;'
                                    'SELECT ROW_COUNT();COMMIT;')
                results.append(int(result.strip().splitlines()[-1]))
            except BaseException as error:
                errors.append(error)
        threads = [threading.Thread(target=contender) for _ in range(2)]
        for thread in threads:thread.start()
        for thread in threads:thread.join(timeout=20)
        self.assertFalse(any(thread.is_alive() for thread in threads))
        self.assertFalse(errors, errors)
        self.assertEqual([0,1], sorted(results))
        self.assertEqual('1', self.query('SELECT version FROM iam_tenant_member WHERE id=101').strip())


if __name__ == '__main__':
    unittest.main()
