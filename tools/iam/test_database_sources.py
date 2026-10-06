"""检查初始化工件漂移、唯一清单和框架DDL跨库风险。"""

import contextlib
import io
import re
import unittest

from database_sources import CREATE_TABLE, ROOT, initialization_files, read_source, render_bundle, schema_files, sources
from generate_bootstrap import build


class DatabaseSourcesTest(unittest.TestCase):
    def test_generated_snapshots_are_reproducible(self):
        with contextlib.redirect_stdout(io.StringIO()):
            build(check=True)
        self.assertEqual(sources('bundle')[0].read_text(), render_bundle())

    def test_manifest_excludes_patches_and_manual_credentials(self):
        self.assertEqual(5, len(schema_files()))
        self.assertEqual(8, len(initialization_files()))
        self.assertEqual(sorted(schema_files()), schema_files())
        self.assertTrue(all('/migrations/' not in str(path) for path in initialization_files()))
        self.assertNotIn(ROOT / 'databases/iam/seed-manual-verification.sql', initialization_files())

    def test_framework_projection_stays_in_the_selected_database(self):
        statements = '\n'.join(read_source(path) for path in sources('frameworkSchemas'))
        self.assertEqual(['account_lock_state', 'password_history', 'password_expiration'],
                         CREATE_TABLE.findall(statements))
        self.assertNotRegex(statements, r'(?mi)^\s*(?:USE|INSERT|DROP|CREATE DATABASE)\b')

    def test_bundle_resets_each_manifest_table_before_creating_and_checks_seed_foreign_keys(self):
        bundle = render_bundle()
        tables = CREATE_TABLE.findall(bundle)
        self.assertEqual(56, len(tables))
        self.assertEqual(len(tables), len(set(tables)))
        self.assertNotIn('iam_resource_field_policy', tables)
        self.assertIn('iam_field_policy', tables)
        self.assertIn('iam_member_export', tables)
        self.assertNotRegex(bundle, r'(?mi)^\s*(?:USE|(?:CREATE|DROP) DATABASE)\b')
        drops = list(re.finditer(r'(?m)^DROP TABLE IF EXISTS `([a-z_]+)`;', bundle))
        self.assertEqual(list(reversed(tables)), [drop.group(1) for drop in drops])
        disable = bundle.index('SET SESSION FOREIGN_KEY_CHECKS = 0;')
        enable = bundle.index('SET SESSION FOREIGN_KEY_CHECKS = 1;')
        first_create = CREATE_TABLE.search(bundle).start()
        self.assertLess(disable, drops[0].start())
        self.assertLess(drops[-1].end(), enable)
        self.assertLess(enable, first_create)
        self.assertIn('SET @iam_init_previous_foreign_key_checks = @@SESSION.FOREIGN_KEY_CHECKS;',
                      bundle[:disable])
        self.assertTrue(bundle.rstrip().endswith(
            'SET SESSION FOREIGN_KEY_CHECKS = @iam_init_previous_foreign_key_checks;'))
        self.assertEqual(1, bundle.count('SET SESSION FOREIGN_KEY_CHECKS = 0;'))
        inserted = re.findall(r'(?mi)^INSERT INTO\s+`?([a-z_]+)', bundle)
        self.assertTrue(set(inserted).isdisjoint({'iam_account', 'iam_tenant', 'iam_role_assignment',
                                               'iam_authorization_audit', 'security_event', 'password_history'}))


if __name__ == '__main__':
    unittest.main()
