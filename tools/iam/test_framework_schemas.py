"""框架DDL所有权、可复用性和服务投影的一致性门禁。"""
import json
import re
import unittest
from pathlib import Path
from generate_framework_databases import ROOT, CREATE, catalog, definitions, build


class FrameworkSchemaTest(unittest.TestCase):
    def test_module_scripts_are_pure_repeatable_ddl_with_exact_ownership(self):
        data = catalog()
        self.assertEqual(5, len(data['modules']))
        self.assertEqual(6, len(definitions()))
        for entry in data['modules']:
            sql = (ROOT / entry['schema']).read_text()
            self.assertNotRegex(sql, r'(?mi)^\s*(?:USE|DROP|INSERT|UPDATE|DELETE|ALTER|GRANT|CREATE DATABASE|CREATE USER)\b')
            self.assertEqual(len(entry['tables']), sql.count('CREATE TABLE IF NOT EXISTS'))
            self.assertIn(entry['module'], sql)
            self.assertIn("SET time_zone = '+00:00'", sql)

    def test_iam_consumes_all_shared_schemas_without_defining_them_twice(self):
        from database_sources import sources, read_source, CREATE_TABLE
        expected = {ROOT / m['schema'] for m in catalog()['modules']}
        self.assertEqual(expected, set(sources('frameworkSchemas')))
        local = {t for p in sources('schemas') for t in CREATE_TABLE.findall(read_source(p))}
        shared = set(definitions())
        self.assertTrue(local.isdisjoint(shared))
        self.assertEqual(53, len(local))
        self.assertTrue({'iam_dictionary', 'iam_social_config', 'iam_account_social_binding'} <= local)

    def test_service_projections_match_the_authoritative_library_ddl(self):
        build(check=True)
        expected = definitions()
        for service, path in catalog()['serviceBundles'].items():
            text = (ROOT / path).read_text()
            actual = {m.group(1): m.group() for m in CREATE.finditer(text)}
            required = {t for e in catalog()['modules'] if service in e['services'] for t in e['tables']}
            for table in required:
                self.assertEqual(expected[table], actual[table], path + ':' + table)
                self.assertNotRegex(text, r'(?mi)^INSERT INTO\s+`?' + re.escape(table) + r'`?\b')

    def test_old_names_are_absent_from_current_persistence_code(self):
        names = catalog()['tableRenames']
        pattern = re.compile(r'(?<![A-Za-z0-9_])(' + '|'.join(map(re.escape, names)) + r')(?![A-Za-z0-9_])')
        for base in ('ingot-framework', 'ingot-service'):
            for p in (ROOT / base).rglob('*'):
                if p.suffix not in ('.java', '.xml') or '/src/main/' not in str(p) or 'build' in p.parts:
                    continue
                self.assertIsNone(pattern.search(p.read_text()), str(p))

    def test_password_models_explicitly_map_to_the_framework_tables(self):
        base = ROOT / 'ingot-framework/ingot-security/ingot-security-credential/src/main/java/com/ingot/framework/security/credential/model/domain'
        for model, table in [('PasswordHistory', 'fw_security_password_history'), ('PasswordExpiration', 'fw_security_password_expiration')]:
            self.assertIn('@TableName("' + table + '")', (base / (model + '.java')).read_text())

    def test_root_documentation_links_every_library_schema(self):
        text = (ROOT / 'databases/README.md').read_text()
        for entry in catalog()['modules']:
            self.assertIn(entry['module'], text)
            self.assertIn('../' + entry['schema'], text)
            for table in entry['tables']:
                self.assertIn(table, text)


if __name__ == '__main__':
    unittest.main()
