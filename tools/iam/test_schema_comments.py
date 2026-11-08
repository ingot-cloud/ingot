"""离线解析与生成覆盖检查；不连接已有数据库。"""
import json
import subprocess
import unittest
from pathlib import Path
from schema_model import tables, comment, COMMENT, parts
from schema_comments import build, normalized, preflight, SNAPSHOT, BASELINE, source_digest
from database_sources import ROOT


class SchemaCommentsTest(unittest.TestCase):
    def test_all_manifest_tables_and_columns_have_documentation(self):
        definitions = tables()
        self.assertEqual(59, len(definitions))
        self.assertEqual(501, sum(len(t.columns) for t in definitions))
        for table in definitions:
            self.assertTrue(comment(table.options), table.name)
            for name, definition in table.columns.items():
                self.assertTrue(comment(definition), table.name + '.' + name)
        build(check=True)

    def test_nested_generated_expression_and_literals_are_not_split(self):
        self.assertEqual(3, len(parts("id BIGINT, x JSON DEFAULT (JSON_OBJECT('a,b', (1+2))), PRIMARY KEY(id)")))
        self.assertIn('CASE WHEN', tables()[5].columns['primary_member_id'])

    def test_preflight_checks_every_table_and_rejects_drift(self):
        snapshot = json.loads(SNAPSHOT.read_text())
        calls = []
        def query(sql):
            calls.append(sql)
            return '\n'.join(name + '\t' + ddl + (' ROW_FORMAT=COMPACT' if name == 'iam_account' else '')
                             for name, ddl in snapshot['tables'].items())
        with self.assertRaisesRegex(ValueError, '未执行任何 ALTER'):
            preflight(query, snapshot)
        self.assertEqual(1, len(calls))
        self.assertEqual(59,calls[0].count("SHOW CREATE"))
        self.assertTrue(all(sql.startswith('SHOW CREATE') for sql in calls))
        with self.assertRaisesRegex(ValueError, '快照'):
            preflight(query, {**snapshot, 'source_digest': 'outdated'})

    def test_comments_and_auto_increment_watermark_are_ignored(self):
        self.assertEqual(normalized("CREATE TABLE t (`id` bigint COMMENT '标识') ENGINE=InnoDB AUTO_INCREMENT=2 COMMENT='表'"),
                         normalized("CREATE TABLE t (`id` bigint) ENGINE=InnoDB AUTO_INCREMENT=999"))

    def test_ascii_check_canonicalization_keeps_actual_schema_changes_visible(self):
        before = "CREATE TABLE t (`kind` varchar(16) CHARACTER SET ascii, CONSTRAINT c CHECK (`kind` = _utf8mb4'ACTIVE')) ENGINE=InnoDB"
        self.assertEqual(normalized(before),normalized(before.replace("_utf8mb4'ACTIVE'","_ascii'ACTIVE'")))
        self.assertNotEqual(normalized(before),normalized(before.replace('CHARACTER SET ascii','CHARACTER SET utf8mb4')))
        self.assertNotEqual(normalized(before),normalized(before.replace("'ACTIVE'","'REVOKED'")))
        self.assertNotEqual(normalized(before),normalized(before.replace("_utf8mb4'ACTIVE'","_ascii'非ASCII'")))

    def test_whitespace_inside_default_literal_is_not_ignored(self):
        self.assertNotEqual(normalized("CREATE TABLE t (v varchar(10) DEFAULT 'a b')"),
                            normalized("CREATE TABLE t (v varchar(10) DEFAULT 'a  b')"))

    def test_review_covers_every_table_and_reproduces(self):
        from generate_database_review import render, TARGET
        expected = render()
        self.assertEqual(TARGET.read_text(),expected)
        for table in tables():
            self.assertIn('### '+table.name+' — ', expected)


    def test_historical_preflight_checks_its_original_tables_not_the_new_project_set(self):
        baseline = json.loads(BASELINE.read_text())
        calls = []
        def query(sql):
            calls.append(sql)
            return '\n'.join(name + '\t' + ddl for name, ddl in baseline['tables'].items())
        preflight(query, baseline, check_source=False)
        self.assertEqual(1, len(calls))
        self.assertEqual(len(baseline['tables']), calls[0].count('SHOW CREATE'))
        self.assertIn('SHOW CREATE TABLE `iam_migration_batch`', calls[0])
        def missing_legacy(sql):
            return '\n'.join(name + '\t' + ddl for name, ddl in baseline['tables'].items()
                             if name != 'iam_migration_batch')
        with self.assertRaisesRegex(ValueError, '未执行任何 ALTER'):
            preflight(missing_legacy, baseline, check_source=False)
        incomplete = {name: ddl for name, ddl in baseline['tables'].items() if name != 'iam_migration_batch'}
        with self.assertRaisesRegex(ValueError, '全部.*历史升级目标'):
            preflight(query, {**baseline, 'tables': incomplete}, check_source=False)

    def test_workbook_blob_fields_are_documented_as_columns(self):
        workbook = next(table for table in tables() if table.name == 'iam_member_workbook')
        self.assertTrue({'workbook', 'error_report', 'password_file'} <= workbook.columns.keys())
        self.assertTrue(all('MEDIUMBLOB' not in value for value in workbook.constraints))


if __name__ == '__main__':
    unittest.main()
