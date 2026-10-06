"""Validate the IAM cold start seed in a disposable, network-isolated MySQL container.

Run: python3 databases/iam/test_bootstrap_seed.py
Requires the mysql:8.4 image locally. Never connects to an existing database.
"""

from pathlib import Path
import subprocess
import time
import unittest
import uuid
import sys

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT))
from tools.iam.database_sources import read_source, schema_files, sources


IMAGE = "mysql:8.4"
SCHEMA_DIRECTORY = Path(__file__).parent
BOOTSTRAP = SCHEMA_DIRECTORY / "006_bootstrap.sql"
MANUAL_SEED = SCHEMA_DIRECTORY / "seed-manual-verification.sql"
STARTUP_TIMEOUT_SECONDS = 60
# The seed reserves identifiers below the allocator start so runtime identifiers never collide.
RESERVED_CEILING = 1000000


class BootstrapSeedTest(unittest.TestCase):
    """Exercise the generated cold start seed against real MySQL constraints."""

    container = None

    @classmethod
    def setUpClass(cls):
        # --pull=never and --network=none prevent image downloads and external access.
        result = subprocess.run(
            ["docker", "run", "--detach", "--rm", "--pull=never", "--network=none",
             "--name", "ingot-iam-bootstrap-test-" + uuid.uuid4().hex,
             "--env", "MYSQL_ALLOW_EMPTY_PASSWORD=yes", IMAGE],
            check=True, capture_output=True, text=True, timeout=30,
        )
        cls.container = result.stdout.strip()
        cls.addClassCleanup(cls.stop_container)
        deadline = time.monotonic() + STARTUP_TIMEOUT_SECONDS
        while time.monotonic() < deadline:
            # The final server must be running, rather than the entrypoint's temporary server.
            result = subprocess.run(
                ["docker", "exec", cls.container, "sh", "-c",
                 'test "$(cat /proc/1/comm)" = mysqld && mysqladmin --user=root ping --silent'],
                capture_output=True, text=True, timeout=5,
            )
            if result.returncode == 0:
                return
            time.sleep(1)
        raise RuntimeError("Isolated MySQL did not become ready within 60 seconds")

    @classmethod
    def stop_container(cls):
        if cls.container:
            subprocess.run(["docker", "stop", "--time", "10", cls.container],
                           check=True, capture_output=True, text=True, timeout=20)

    def sql(self, statement, *, error=None, database=True):
        command = ["docker", "exec", "--interactive", self.container,
                   "mysql", "--user=root", "--default-character-set=utf8mb4",
                   "--batch", "--skip-column-names"]
        if database:
            command += ["--database", self.database]
        result = subprocess.run(command, input=statement, capture_output=True,
                                text=True, timeout=60)
        if error is None:
            self.assertEqual(0, result.returncode, result.stderr)
        else:
            self.assertNotEqual(0, result.returncode)
            self.assertIn(f"ERROR {error} ", result.stderr)
        return result.stdout.strip()

    def count(self, table, where="1 = 1"):
        return int(self.sql(f"SELECT COUNT(*) FROM {table} WHERE {where}"))

    def setUp(self):
        self.database = "iam_bootstrap_" + uuid.uuid4().hex
        self.sql(f"CREATE DATABASE `{self.database}`", database=False)
        for schema in schema_files():
            self.sql(schema.read_text())

    def bootstrap(self):
        self.sql(BOOTSTRAP.read_text())

    def test_generated_bundle_matches_component_schema_and_initial_data(self):
        for schema in sources('frameworkSchemas'):
            self.sql(read_source(schema))
        self.bootstrap()
        tables = self.sql('SHOW TABLES').splitlines()
        definitions = {table: self.sql(f'SHOW CREATE TABLE `{table}`') for table in tables}
        counts = {table: self.count(table) for table in tables}
        self.database = 'iam_bundle_' + uuid.uuid4().hex
        self.sql(f'CREATE DATABASE `{self.database}`', database=False)
        self.sql(sources('bundle')[0].read_text())
        self.assertEqual(tables, self.sql('SHOW TABLES').splitlines())
        for table in tables:
            self.assertEqual(definitions[table], self.sql(f'SHOW CREATE TABLE `{table}`'), table)
            self.assertEqual(counts[table], self.count(table), table)
        self.assertEqual(56, len(tables))
        self.assertEqual(138, self.count('iam_action'))
        self.assertEqual(30, self.count('iam_menu'))
        for table in ['iam_account', 'iam_tenant', 'iam_role_assignment', 'iam_authorization_audit',
                      'account_lock_state', 'password_history', 'password_expiration', 'security_event']:
            self.assertEqual(0, self.count(table), table)
        self.assertEqual('1', self.sql('SELECT @@FOREIGN_KEY_CHECKS'))

    def test_full_initialization_resets_linked_data_and_restores_session_foreign_keys(self):
        # setUp created IAM tables only: the full initializer must also handle partial initialization.
        bundle = sources('bundle')[0].read_text()
        self.sql(bundle)
        tables = self.sql('SHOW TABLES').splitlines()
        counts = ' UNION ALL '.join(
            f"SELECT '{table}', COUNT(*) FROM `{table}`" for table in tables) + ' ORDER BY 1'
        expected_counts = self.sql(counts)
        expected_menu = self.sql('SHOW CREATE TABLE iam_menu')
        self.sql('CREATE TABLE iam_unmanaged_fixture (id INT PRIMARY KEY); '
                 'INSERT INTO iam_unmanaged_fixture VALUES (1);')

        for initial_fk_checks in (1, 0):
            with self.subTest(initial_fk_checks=initial_fk_checks):
                self.sql(MANUAL_SEED.read_text())
                # Organization and owner form a real FK cycle; runtime assignments reference seed roles.
                self.sql("""
                    INSERT INTO iam_tenant (id, name) VALUES (800101, '临时组织');
                    INSERT INTO iam_tenant_member (id, tenant_id, account_id, display_name)
                        VALUES (800102, 800101, 900002, '临时所有者');
                    UPDATE iam_tenant SET owner_member_id = 800102 WHERE id = 800101;
                    INSERT INTO iam_department (id, tenant_id, name)
                        VALUES (800103, 800101, '根部门');
                    INSERT INTO iam_member_department (tenant_id, member_id, department_id, is_primary)
                        VALUES (800101, 800102, 800103, TRUE);
                    UPDATE iam_menu SET view_path = NULL WHERE kind = 'DIRECTORY';
                    ALTER TABLE iam_menu ADD COLUMN temporary_fixture INT NULL;
                    UPDATE iam_action SET enabled = FALSE;
                    UPDATE biz_leaf_alloc SET max_id = 8888888 WHERE biz_tag = 'iam';
                """)
                self.assertEqual(str(initial_fk_checks), self.sql(
                    f'SET SESSION FOREIGN_KEY_CHECKS = {initial_fk_checks};\n'
                    + bundle + '\nSELECT @@SESSION.FOREIGN_KEY_CHECKS;'))
                self.assertEqual(expected_counts, self.sql(counts))
                self.assertEqual(expected_menu, self.sql('SHOW CREATE TABLE iam_menu'))
                self.assertEqual(1, self.count('iam_unmanaged_fixture'))
                self.assertEqual('0', self.sql("SELECT COUNT(*) FROM iam_menu WHERE kind = 'DIRECTORY' "
                                               "AND (view_path IS NULL OR view_path <> 'layout.main')"))
                self.assertEqual('0', self.sql('SELECT COUNT(*) FROM iam_action WHERE enabled = FALSE'))
                self.assertEqual(str(RESERVED_CEILING),
                                 self.sql("SELECT max_id FROM biz_leaf_alloc WHERE biz_tag = 'iam'"))
                self.sql("INSERT INTO iam_platform_member (id, account_id, display_name) "
                         "VALUES (999001, 999002, '非法引用')", error=1452)

    def test_moved_additive_patches_restore_the_canonical_structure(self):
        tables = ['iam_member_export', 'iam_tenant',
                  'iam_authorization_audit']
        before = {table: self.sql(f'SHOW CREATE TABLE `{table}`') for table in tables}
        # ALTER re-adds an index at the end of SHOW CREATE; compare its semantic definition.
        indexes = """SELECT INDEX_NAME, NON_UNIQUE, SEQ_IN_INDEX, COLUMN_NAME,
                            COLLATION, SUB_PART, INDEX_TYPE, IS_VISIBLE
                     FROM information_schema.statistics
                     WHERE table_schema=DATABASE() AND table_name='iam_authorization_audit'
                     ORDER BY INDEX_NAME, SEQ_IN_INDEX"""
        before_indexes = self.sql(indexes)
        self.sql('DROP TABLE iam_member_export; '
                 'ALTER TABLE iam_tenant DROP COLUMN plan_id; '
                 'ALTER TABLE iam_authorization_audit DROP INDEX idx_iam_audit_assignment_create;')
        patches = ['007_member_export.sql', '009_tenant_plan.sql',
                   '010_assignment_audit_index.sql']
        for patch in patches:
            self.sql((SCHEMA_DIRECTORY / 'migrations' / patch).read_text())
        # The guarded index and field table patches remain repeatable.
        for patch in patches[2:]:
            self.sql((SCHEMA_DIRECTORY / 'migrations' / patch).read_text())
        for table in tables:
            if table != 'iam_authorization_audit':
                self.assertEqual(before[table], self.sql(f'SHOW CREATE TABLE `{table}`'), table)
        self.assertEqual(before_indexes, self.sql(indexes))

    def test_empty_database_gets_complete_catalog_without_credentials(self):
        self.bootstrap()
        self.assertEqual(3, self.count("iam_application"))
        self.assertEqual(36, self.count("iam_resource"))
        self.assertEqual(138, self.count("iam_action"))
        self.assertEqual(30, self.count("iam_menu"))
        self.assertEqual('MASKED', self.sql("""
            SELECT JSON_UNQUOTE(JSON_EXTRACT(revision.resource_field_permissions,
                CONCAT('$."',resource.id,'".phone.visibility')))
            FROM iam_role_revision revision JOIN iam_role_definition role ON role.id=revision.role_id
            JOIN iam_application app ON app.domain=role.domain AND app.code='iam-platform'
            JOIN iam_resource resource ON resource.application_id=app.id AND resource.code='member'
            WHERE role.kind='SYSTEM' AND role.domain='PLATFORM'
        """))
        self.assertEqual(0, self.count("iam_resource", "code='resource-field-policy'"))
        self.assertEqual(2, self.count("iam_default_policy_revision"))
        self.assertEqual(1, self.count("biz_leaf_alloc", "biz_tag = 'iam'"))
        # The seed carries no identity or credential rows; those come from the security use case.
        self.assertEqual(0, self.count("iam_account"))
        self.assertEqual(0, self.count("iam_platform_member"))
        self.assertEqual(0, self.count("iam_role_assignment"))

    def test_rerun_does_not_append_permissions_to_an_existing_fixed_revision(self):
        self.bootstrap()
        self.sql("DELETE FROM iam_role_grant WHERE revision_id IN (SELECT id FROM iam_role_revision WHERE kind='SYSTEM') AND action_id IN (SELECT id FROM iam_action WHERE code='iam-platform:assignment:upgrade')")
        before = self.count("iam_role_grant")
        self.bootstrap()
        self.assertEqual(before, self.count("iam_role_grant"))

    def test_repeated_execution_is_idempotent_and_keeps_manual_changes(self):
        self.bootstrap()
        self.sql("UPDATE iam_application SET name = '人工改名' WHERE code = 'iam-tenant'")
        self.sql("UPDATE iam_action SET enabled = FALSE WHERE code = 'iam-tenant:member:export'")
        self.sql("INSERT INTO iam_plan (id, name) VALUES (700001, '业务套餐')")
        before = {table: self.count(table) for table in
                  ("iam_application", "iam_resource", "iam_action", "iam_menu",
                   "iam_menu_action", "iam_role_definition", "iam_role_revision",
                   "iam_role_grant", "iam_default_policy_revision")}

        self.bootstrap()

        self.assertEqual(before, {table: self.count(table) for table in before})
        self.assertEqual("人工改名",
                         self.sql("SELECT name FROM iam_application WHERE code = 'iam-tenant'"))
        self.assertEqual("0",
                         self.sql("SELECT enabled FROM iam_action "
                                  "WHERE code = 'iam-tenant:member:export'"))
        self.assertEqual(1, self.count("iam_plan", "id = 700001"))

    def test_governance_roles_satisfy_organization_initialization_preconditions(self):
        self.bootstrap()
        # InitializationCatalog requires exactly one enabled SYSTEM role per domain.
        self.assertEqual("1", self.sql("SELECT COUNT(*) FROM iam_role_definition "
                                       "WHERE domain = 'TENANT' AND kind = 'SYSTEM' AND enabled = TRUE"))
        self.assertEqual("1", self.sql("SELECT COUNT(*) FROM iam_role_definition "
                                       "WHERE domain = 'PLATFORM' AND kind = 'SYSTEM' AND enabled = TRUE"))
        self.assertEqual("1", self.sql("""
            SELECT COUNT(*) FROM iam_role_revision revision
            JOIN iam_role_definition role ON role.id = revision.role_id
            WHERE role.domain = 'TENANT' AND role.kind = 'SYSTEM'
        """))
        # Baseline entitlement resolution needs at least one enabled baseline tenant application.
        self.assertEqual("1", self.sql("SELECT COUNT(*) FROM iam_application "
                                       "WHERE domain = 'TENANT' AND baseline = TRUE AND enabled = TRUE"))
        self.assertEqual(1, self.count("iam_default_policy_revision", "kind = 'DIRECTORY'"))
        self.assertEqual(1, self.count("iam_default_policy_revision", "kind = 'FIELD'"))
        self.assertEqual("ALL", self.sql(
            "SELECT JSON_UNQUOTE(JSON_EXTRACT(definition, '$.scope')) "
            "FROM iam_default_policy_revision WHERE kind = 'DIRECTORY'"))
        self.assertEqual("MASKED", self.sql(
            "SELECT JSON_UNQUOTE(JSON_EXTRACT(definition, '$.fields.phone.visibility')) "
            "FROM iam_default_policy_revision WHERE kind = 'FIELD'"))

    def test_governance_grants_cover_own_domain_only(self):
        self.bootstrap()
        for role, domain, expected in (("platform-governance", "PLATFORM", 92),
                                       ("tenant-governance", "TENANT", 46)):
            covered = self.sql(f"""
                SELECT COUNT(*) FROM iam_role_grant grant_row
                JOIN iam_role_revision revision ON revision.id = grant_row.revision_id
                JOIN iam_role_definition role ON role.id = revision.role_id
                JOIN iam_action action ON action.id = grant_row.action_id
                JOIN iam_application app ON app.id = action.application_id
                WHERE role.code = '{role}' AND app.domain = '{domain}'
            """)
            self.assertEqual(str(expected), covered, role)
            leaked = self.sql(f"""
                SELECT COUNT(*) FROM iam_role_grant grant_row
                JOIN iam_role_revision revision ON revision.id = grant_row.revision_id
                JOIN iam_role_definition role ON role.id = revision.role_id
                JOIN iam_action action ON action.id = grant_row.action_id
                JOIN iam_application app ON app.id = action.application_id
                WHERE role.code = '{role}' AND app.domain <> role.domain
            """)
            self.assertEqual("0", leaked, role)

    def test_global_accounts_menu_is_in_platform_management_before_personnel(self):
        self.bootstrap()
        self.assertEqual('全局账号\tplatform.iam.accounts\t/platform/iam/accounts\t1', self.sql("""
            SELECT menu.name, menu.view_path, menu.path, menu.sort_order FROM iam_menu menu
            JOIN iam_application app ON app.id = menu.application_id AND app.code = 'iam-platform'
            JOIN iam_menu parent ON parent.id = menu.parent_id AND parent.application_id = app.id
            WHERE menu.route_name = 'platform.iam.accounts' AND parent.route_name = 'platform.iam.manage'
        """))
        self.assertEqual('iam-platform:account:read', self.sql("""
            SELECT action.code FROM iam_menu_action link
            JOIN iam_menu menu ON menu.id = link.menu_id
            JOIN iam_action action ON action.id = link.action_id
            WHERE menu.route_name = 'platform.iam.accounts'
        """))
        self.assertEqual('platform.iam.accounts\nplatform.iam.personnel\nplatform.iam.authorization',
                         self.sql("""
            SELECT menu.route_name FROM iam_menu menu
            JOIN iam_menu parent ON parent.id = menu.parent_id
            WHERE parent.route_name = 'platform.iam.manage' ORDER BY menu.sort_order, menu.id
        """))

    def test_developer_pages_and_executable_operations_belong_to_an_independent_platform_app(self):
        self.bootstrap()
        self.assertEqual('PLATFORM\t0\t开发者平台', self.sql("""
            SELECT domain, baseline, name FROM iam_application WHERE code = 'platform:develop'
        """))
        self.assertEqual('layout.main', self.sql("""
            SELECT menu.view_path FROM iam_menu menu JOIN iam_application app ON app.id = menu.application_id
            WHERE app.code = 'platform:develop' AND menu.kind = 'DIRECTORY'
        """))
        self.assertEqual('platform.develop.qrcode\tplatform:develop:qrcode\n'
                         'platform.develop.client\tplatform:develop:client:query\n'
                         'platform.develop.social\tiam-platform:social-config:read\n'
                         'platform.develop.id\tiam-platform:id-allocation:read', self.sql("""
            SELECT menu.view_path, action.code FROM iam_menu menu
            JOIN iam_application app ON app.id = menu.application_id AND app.code = 'platform:develop'
            JOIN iam_menu parent ON parent.id = menu.parent_id AND parent.application_id = app.id
            JOIN iam_menu_action link ON link.menu_id = menu.id AND link.application_id = app.id
            JOIN iam_action action ON action.id = link.action_id AND action.application_id = app.id
            WHERE menu.kind = 'PAGE' AND parent.route_name = 'platform.develop'
            ORDER BY menu.sort_order
        """))
        self.assertEqual('client\nid-allocation\nqrcode\nsocial-config', self.sql("""
            SELECT resource.code
            FROM iam_resource resource JOIN iam_application app ON app.id = resource.application_id
            WHERE app.code = 'platform:develop' ORDER BY resource.code
        """))
        self.assertEqual('15', self.sql("""
            SELECT COUNT(*) FROM iam_action action
            JOIN iam_application app ON app.id = action.application_id
            JOIN iam_resource resource ON resource.id = action.resource_id AND resource.application_id = app.id
            WHERE app.code = 'platform:develop'
        """))
        self.assertEqual('0', self.sql("""
            SELECT COUNT(*) FROM iam_resource resource JOIN iam_application app ON app.id = resource.application_id
            WHERE app.code = 'iam-platform' AND resource.code IN ('id-allocation', 'social-config', 'client', 'qrcode')
        """))

    def test_reserved_identifiers_stay_below_the_allocator_start(self):
        self.bootstrap()
        allocator_start = int(self.sql("SELECT max_id FROM biz_leaf_alloc WHERE biz_tag = 'iam'"))
        self.assertEqual(RESERVED_CEILING, allocator_start)
        for table in ("iam_application", "iam_resource", "iam_action", "iam_menu",
                      "iam_role_definition", "iam_role_revision", "iam_default_policy_revision"):
            highest = int(self.sql(f"SELECT COALESCE(MAX(id), 0) FROM {table}"))
            self.assertLess(highest, allocator_start, table)

    def test_action_menus_are_unreachable_without_a_linked_action(self):
        self.bootstrap()
        # SessionService hides ACTION menus that carry no action, so every seeded menu needs a link.
        self.assertEqual("0", self.sql("""
            SELECT COUNT(*) FROM iam_menu menu
            WHERE menu.access_mode = 'ACTION'
              AND NOT EXISTS (SELECT 1 FROM iam_menu_action link
                              WHERE link.application_id = menu.application_id
                                AND link.menu_id = menu.id)
        """))
        # Directories carry the union of their pages so an empty directory cannot stay visible.
        self.assertEqual("0", self.sql("""
            SELECT COUNT(*) FROM iam_menu page
            JOIN iam_menu_action link ON link.application_id = page.application_id
                                     AND link.menu_id = page.id
            WHERE page.kind = 'PAGE'
              AND NOT EXISTS (SELECT 1 FROM iam_menu_action parent
                              WHERE parent.application_id = page.application_id
                                AND parent.menu_id = page.parent_id
                                AND parent.action_id = link.action_id)
        """))

    def test_pages_expose_their_view_key_and_directories_use_main_layout(self):
        self.bootstrap()
        self.assertEqual("0", self.sql("SELECT COUNT(*) FROM iam_menu "
                                       "WHERE kind = 'PAGE' AND (view_path IS NULL OR view_path = '')"))
        self.assertGreater(self.count('iam_menu', "kind = 'DIRECTORY'"), 0)
        self.assertEqual("0", self.sql("SELECT COUNT(*) FROM iam_menu WHERE kind = 'DIRECTORY' "
                                       "AND (view_path IS NULL OR view_path <> 'layout.main')"))
        self.assertEqual("0", self.sql("SELECT COUNT(*) FROM iam_menu "
                                       "WHERE path IS NULL OR route_name IS NULL"))

    def test_seed_attaches_to_a_pre_existing_application_identifier(self):
        # A pre-existing application keeps its identifier and still receives the full catalog.
        self.sql("INSERT INTO iam_application (id, code, domain, name, baseline) "
                 "VALUES (500001, 'iam-tenant', 'TENANT', '既有组织治理', TRUE)")
        self.bootstrap()
        self.assertEqual(1, self.count("iam_application", "code = 'iam-tenant'"))
        self.assertEqual("500001", self.sql("SELECT id FROM iam_application WHERE code = 'iam-tenant'"))
        self.assertEqual(15, self.count("iam_resource", "application_id = 500001"))
        self.assertEqual(46, self.count("iam_action", "application_id = 500001"))
        self.assertEqual(13, self.count("iam_menu", "application_id = 500001"))
        self.assertEqual(138, self.count("iam_role_grant"))

    def test_manual_verification_seed_layers_on_the_cold_start_catalog(self):
        self.bootstrap()
        self.sql(MANUAL_SEED.read_text())
        # The fixture only adds sign-in identities; the catalog stays exactly as seeded.
        self.assertEqual(2, self.count("iam_account"))
        self.assertEqual(1, self.count("iam_platform_member"))
        self.assertEqual(2, self.count("iam_role_definition"))
        self.assertEqual(138, self.count("iam_role_grant"))
        # The governance assignment resolves the platform revision from the seed, not a fixed id.
        self.assertEqual("1", self.sql("""
            SELECT COUNT(*) FROM iam_role_assignment assignment
            JOIN iam_role_revision revision ON revision.id = assignment.revision_id
            JOIN iam_role_definition role ON role.id = revision.role_id
            WHERE assignment.platform_member_id = 910001
              AND role.domain = 'PLATFORM' AND role.kind = 'SYSTEM'
        """))


if __name__ == "__main__":
    unittest.main(verbosity=2)
