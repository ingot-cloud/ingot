"""Validate the IAM cold start seed in a disposable, network-isolated MySQL container.

Run: python3 databases/iam/test_bootstrap_seed.py
Requires the mysql:8.4 image locally. Never connects to an existing database.
"""

from pathlib import Path
import subprocess
import time
import unittest
import uuid


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
                   "mysql", "--user=root", "--batch", "--skip-column-names"]
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
        for schema in sorted(SCHEMA_DIRECTORY.glob("[0-9][0-9][0-9]_*.sql")):
            if schema == BOOTSTRAP:
                continue
            self.sql(schema.read_text())

    def bootstrap(self):
        self.sql(BOOTSTRAP.read_text())

    def test_empty_database_gets_complete_catalog_without_credentials(self):
        self.bootstrap()
        self.assertEqual(2, self.count("iam_application"))
        self.assertEqual(40, self.count("iam_resource"))
        self.assertEqual(145, self.count("iam_action"))
        self.assertEqual(24, self.count("iam_menu"))
        self.assertEqual(2, self.count("iam_default_policy_revision"))
        self.assertEqual(1, self.count("biz_leaf_alloc", "biz_tag = 'iam'"))
        # The seed carries no identity or credential rows; those come from the security use case.
        self.assertEqual(0, self.count("iam_account"))
        self.assertEqual(0, self.count("iam_platform_member"))
        self.assertEqual(0, self.count("iam_role_assignment"))

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
        for role, namespace, other in (("platform-governance", "iam-platform", "iam-tenant"),
                                       ("tenant-governance", "iam-tenant", "iam-platform")):
            covered = self.sql(f"""
                SELECT COUNT(*) FROM iam_role_grant grant_row
                JOIN iam_role_revision revision ON revision.id = grant_row.revision_id
                JOIN iam_role_definition role ON role.id = revision.role_id
                JOIN iam_action action ON action.id = grant_row.action_id
                WHERE role.code = '{role}' AND action.code LIKE '{namespace}:%'
            """)
            total = self.sql(f"SELECT COUNT(*) FROM iam_action WHERE code LIKE '{namespace}:%'")
            self.assertEqual(total, covered, role)
            leaked = self.sql(f"""
                SELECT COUNT(*) FROM iam_role_grant grant_row
                JOIN iam_role_revision revision ON revision.id = grant_row.revision_id
                JOIN iam_role_definition role ON role.id = revision.role_id
                JOIN iam_action action ON action.id = grant_row.action_id
                WHERE role.code = '{role}' AND action.code LIKE '{other}:%'
            """)
            self.assertEqual("0", leaked, role)

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

    def test_pages_expose_a_view_registration_key_and_directories_do_not(self):
        self.bootstrap()
        self.assertEqual("0", self.sql("SELECT COUNT(*) FROM iam_menu "
                                       "WHERE kind = 'PAGE' AND (view_path IS NULL OR view_path = '')"))
        self.assertEqual("0", self.sql("SELECT COUNT(*) FROM iam_menu "
                                       "WHERE kind = 'DIRECTORY' AND view_path IS NOT NULL"))
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
        self.assertEqual(145, self.count("iam_role_grant"))

    def test_manual_verification_seed_layers_on_the_cold_start_catalog(self):
        self.bootstrap()
        self.sql(MANUAL_SEED.read_text())
        # The fixture only adds sign-in identities; the catalog stays exactly as seeded.
        self.assertEqual(2, self.count("iam_account"))
        self.assertEqual(1, self.count("iam_platform_member"))
        self.assertEqual(2, self.count("iam_role_definition"))
        self.assertEqual(145, self.count("iam_role_grant"))
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
