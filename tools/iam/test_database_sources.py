"""检查初始化工件漂移、唯一清单和框架DDL跨库风险。"""

import contextlib
import io
import re
import unittest

from database_sources import CREATE_TABLE, DATABASE_NAME, ROOT, initialization_files, legacy_schema_files, read_source, render_bundle, schema_files, sources
from generate_bootstrap import build


class DatabaseSourcesTest(unittest.TestCase):
    def test_generated_snapshots_are_reproducible(self):
        with contextlib.redirect_stdout(io.StringIO()):
            build(check=True)
        self.assertEqual(sources('bundle')[0].read_text(), render_bundle())

    def test_manifest_excludes_patches_and_manual_credentials(self):
        self.assertEqual(5, len(schema_files()))
        self.assertEqual(12, len(initialization_files()))
        self.assertEqual(sorted(schema_files()), schema_files())
        self.assertTrue(all('/migrations/' not in str(path) for path in initialization_files()))
        self.assertNotIn(ROOT / 'databases/iam/seed-manual-verification.sql', initialization_files())
        self.assertNotIn(ROOT / 'databases/iam/dev/prepare_tenant_live_test.sql', initialization_files())

    def test_framework_projection_stays_in_the_selected_database(self):
        statements = '\n'.join(read_source(path) for path in sources('frameworkSchemas'))
        self.assertEqual(['fw_security_account_lock_state', 'fw_security_password_history', 'fw_security_password_expiration',
                          'fw_security_event', 'fw_id_leaf_alloc', 'fw_oss_asset_upload'],
                         CREATE_TABLE.findall(statements))
        self.assertNotRegex(statements, r'(?mi)^\s*(?:USE|INSERT|DROP|CREATE DATABASE)\b')

    def test_bundle_recreates_only_the_iam_database_before_creating_and_checks_seed_foreign_keys(self):
        bundle = render_bundle()
        tables = CREATE_TABLE.findall(bundle)
        self.assertEqual(59, len(tables))
        self.assertEqual(len(tables), len(set(tables)))
        self.assertNotIn('iam_resource_field_policy', tables)
        self.assertIn('iam_field_policy', tables)
        self.assertIn('iam_member_export', tables)
        self.assertIn('iam_member_workbook', tables)
        self.assertIn('iam_department_workbook', tables)
        self.assertIn('iam_download_task', tables)
        self.assertIn('iam_department_leader', tables)
        self.assertEqual([DATABASE_NAME], re.findall(r'(?m)^DROP DATABASE IF EXISTS `([a-z_]+)`;', bundle))
        self.assertEqual([DATABASE_NAME], re.findall(r'(?m)^CREATE DATABASE `([a-z_]+)`', bundle))
        self.assertEqual([DATABASE_NAME], re.findall(r'(?m)^USE `([a-z_]+)`;', bundle))
        self.assertNotRegex(bundle, r'(?mi)^\s*(?:DROP TABLE|CREATE USER|GRANT)\b')
        self.assertNotRegex(bundle, r'(?mi)^SOURCE[ \t]')
        drop = bundle.index(f'DROP DATABASE IF EXISTS `{DATABASE_NAME}`;')
        create = bundle.index(f'CREATE DATABASE `{DATABASE_NAME}`')
        use = bundle.index(f'USE `{DATABASE_NAME}`;')
        disable = bundle.index('SET SESSION FOREIGN_KEY_CHECKS = 0;')
        enable = bundle.index('SET SESSION FOREIGN_KEY_CHECKS = 1;')
        first_create = CREATE_TABLE.search(bundle).start()
        self.assertLess(disable, drop)
        self.assertLess(drop, create)
        self.assertLess(create, use)
        self.assertLess(use, enable)
        self.assertLess(enable, first_create)
        self.assertIn('SET @iam_init_previous_foreign_key_checks = @@SESSION.FOREIGN_KEY_CHECKS;',
                      bundle[:disable])
        self.assertTrue(bundle.rstrip().endswith(
            'SET SESSION FOREIGN_KEY_CHECKS = @iam_init_previous_foreign_key_checks;'))
        self.assertEqual(1, bundle.count('SET SESSION FOREIGN_KEY_CHECKS = 0;'))
        inserted = re.findall(r'(?mi)^INSERT INTO\s+`?([a-z_]+)', bundle)
        self.assertEqual(['iam_account', 'iam_platform_member', 'iam_role_assignment'], inserted[-3:])
        self.assertTrue(set(inserted[:-3]).isdisjoint(inserted[-3:]))
        self.assertTrue(set(inserted).isdisjoint({'iam_tenant', 'iam_tenant_member',
                                               'iam_authorization_audit', 'fw_security_event', 'fw_security_password_history'}))


    def test_legacy_definitions_are_opt_in_and_not_created_by_first_deployment(self):
        legacy = {table for path in legacy_schema_files() for table in CREATE_TABLE.findall(read_source(path))}
        self.assertEqual({'iam_migration_batch', 'iam_migration_mapping', 'iam_migration_issue',
                          'iam_live_authorization_batch', 'iam_live_authorization_mapping',
                          'sys_tenant_plan_record'}, legacy)
        self.assertTrue(set(initialization_files()).isdisjoint(legacy_schema_files()))
        bundle = render_bundle()
        self.assertTrue(legacy.isdisjoint(CREATE_TABLE.findall(bundle)))
        self.assertIn(f'DROP DATABASE IF EXISTS `{DATABASE_NAME}`;', bundle)
        for path in legacy_schema_files():
            self.assertNotRegex(read_source(path), r'(?mi)^\s*(?:USE|INSERT|DROP|ALTER|CREATE DATABASE)\b')

    def test_runtime_foreign_keys_only_reference_runtime_tables(self):
        from schema_model import tables
        definitions = tables()
        names = {table.name for table in definitions}
        for table in definitions:
            for constraint in table.constraints:
                for target in re.findall(r'REFERENCES\s+`?([a-z_]+)', constraint, re.I):
                    self.assertIn(target, names, table.name + ' -> ' + target)


if __name__ == '__main__':
    unittest.main()
