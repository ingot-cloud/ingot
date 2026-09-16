"""Validate IAM target DDL in a disposable, network-isolated MySQL container.

Run: python3 databases/iam/test_identity_schema.py
Requires the mysql:8.4 image locally. Never connects to an existing database.
"""

from pathlib import Path
import subprocess
import re
import time
import unittest
import uuid


IMAGE = "mysql:8.4"
SCHEMA_DIRECTORY = Path(__file__).parent
STARTUP_TIMEOUT_SECONDS = 60
# Numbered data seeds carry rows, not structure; loading them would collide with these fixtures.
DATA_SEEDS = {"006_bootstrap.sql"}


def schema_files():
    """Ordered DDL files only, so fixture identifiers stay free of seeded catalog rows."""
    return [item for item in sorted(SCHEMA_DIRECTORY.glob("[0-9][0-9][0-9]_*.sql"))
            if item.name not in DATA_SEEDS]


class IdentitySchemaTest(unittest.TestCase):
    """Exercise actual MySQL uniqueness and cross-domain foreign keys."""

    container = None

    @classmethod
    def setUpClass(cls):
        # --pull=never and --network=none prevent image downloads and external access.
        result = subprocess.run(
            ["docker", "run", "--detach", "--rm", "--pull=never", "--network=none",
             "--name", "ingot-iam-identity-test-" + uuid.uuid4().hex,
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
                                text=True, timeout=20)
        if error is None:
            self.assertEqual(0, result.returncode, result.stderr)
        else:
            self.assertNotEqual(0, result.returncode)
            self.assertIn(f"ERROR {error} ", result.stderr)
        return result.stdout.strip()

    def setUp(self):
        self.database = "iam_case_" + uuid.uuid4().hex
        self.sql(f"CREATE DATABASE `{self.database}`", database=False)
        for schema in schema_files():
            self.sql(schema.read_text())
        self.sql("""
            INSERT INTO iam_account (id, username, password_hash) VALUES
                (1, 'fixture-one', 'not-a-login-credential'),
                (2, 'fixture-two', 'not-a-login-credential');
            INSERT INTO iam_platform_member (id, account_id, display_name) VALUES (1001, 1, 'Platform');
            INSERT INTO iam_tenant (id, name) VALUES (10, 'A'), (20, 'B');
            INSERT INTO iam_tenant_member (id, tenant_id, account_id, display_name) VALUES
                (101, 10, 1, 'A member'), (102, 10, 2, 'A second'), (201, 20, 1, 'B member');
            UPDATE iam_tenant SET owner_member_id = 101 WHERE id = 10;
            UPDATE iam_tenant SET owner_member_id = 201 WHERE id = 20;
            INSERT INTO iam_department (id, tenant_id, name) VALUES
                (11, 10, 'A root'), (12, 10, 'A second'), (21, 20, 'B root');
            INSERT INTO iam_platform_group (id, name) VALUES (100, 'Platform group');
            INSERT INTO iam_tenant_group (id, tenant_id, name) VALUES (110, 10, 'A group');
            INSERT INTO iam_application (id, code, domain, name) VALUES
                (1, 'iam-platform', 'PLATFORM', 'Platform'), (2, 'iam-tenant', 'TENANT', 'Tenant');
            INSERT INTO iam_resource (id, application_id, code, name, scope_capabilities, field_capabilities)
                VALUES (1, 1, 'account', 'Account', '[]', '[]'), (2, 2, 'member', 'Member', '[]', '[]');
            INSERT INTO iam_action (id, application_id, resource_id, code, name)
                VALUES (1, 1, 1, 'iam-platform:account:read', 'Read account'),
                       (2, 2, 2, 'iam-tenant:member:read', 'Read member');
            INSERT INTO iam_role_definition (id, domain, tenant_id, kind, code, name) VALUES
                (1, 'TENANT', NULL, 'SHARED', 'viewer', 'Viewer'),
                (2, 'TENANT', 10, 'TENANT_CUSTOM', 'custom', 'Custom'),
                (3, 'PLATFORM', NULL, 'PLATFORM_CUSTOM', 'operator', 'Operator');
            INSERT INTO iam_role_revision (id, role_id, kind, revision, base_revision_id, metadata_overrides)
                VALUES (1, 1, 'SHARED', 1, NULL, '{}'), (2, 2, 'TENANT_CUSTOM', 1, 1, '{}'),
                       (3, 3, 'PLATFORM_CUSTOM', 1, NULL, '{}');
        """)

    def runtime_identity_query(self, domain, account_id=1, member_id=1001, tenant_id=10):
        import xml.etree.ElementTree as ET
        mapper = (SCHEMA_DIRECTORY.parents[1] / "ingot-service/ingot-iam/ingot-iam-provider/src/main/resources/"
                  "mapper/IamIdentityMapper.xml")
        select = ET.parse(mapper).getroot().find("select[@id='" + domain.lower() + "']")
        query = "".join(select.itertext())
        # Synthetic fixtures only. Includes the memberId condition and executes production mapper SQL.
        for key, value in {"accountId": str(account_id), "memberId": str(member_id),
                           "tenantId": str(tenant_id), "active": "'ACTIVE'"}.items():
            query = query.replace("#{" + key + "}", value)
        output = self.sql(query)
        # Projection now carries member ID too; keep the historical version assertions.
        size = 2 if domain == "PLATFORM" else 3
        return "\n".join("\t".join(line.split("\t")[:size]) for line in output.splitlines())

    def test_runtime_identity_query_preserves_domain_and_account(self):
        self.assertEqual("0\t0", self.runtime_identity_query("PLATFORM"))
        self.assertEqual("0\t0\t0", self.runtime_identity_query("TENANT", member_id=101))
        self.assertEqual("", self.runtime_identity_query("PLATFORM", member_id=101))
        self.assertEqual("", self.runtime_identity_query("TENANT", member_id=101, tenant_id=20))
        self.assertEqual("", self.runtime_identity_query("TENANT", account_id=2, member_id=101))

    def test_runtime_identity_query_respects_independent_and_global_disable(self):
        self.sql("UPDATE iam_platform_member SET status='SUSPENDED' WHERE id=1001")
        self.assertEqual("", self.runtime_identity_query("PLATFORM"))
        self.assertEqual("0\t0\t0", self.runtime_identity_query("TENANT", member_id=101))
        self.sql("UPDATE iam_account SET enabled=FALSE WHERE id=1")
        self.assertEqual("", self.runtime_identity_query("TENANT", member_id=101))
        self.assertEqual("", self.runtime_identity_query("TENANT", member_id=201, tenant_id=20))

    def test_one_account_has_three_distinct_member_identities(self):
        self.assertEqual("3", self.sql("""
            SELECT COUNT(*) FROM (
                SELECT id FROM iam_platform_member WHERE account_id = 1
                UNION ALL SELECT id FROM iam_tenant_member WHERE account_id = 1
            ) identities
        """))
        self.sql("INSERT INTO iam_platform_member (id, account_id, display_name) VALUES (1002, 1, 'duplicate')",
                 error=1062)
        self.sql("INSERT INTO iam_tenant_member (id, tenant_id, account_id, display_name) VALUES (103, 10, 1, 'duplicate')",
                 error=1062)

    def test_platform_group_only_accepts_platform_members(self):
        self.sql("INSERT INTO iam_platform_group_member VALUES (100, 1001)")
        self.sql("INSERT INTO iam_platform_group_member VALUES (100, 101)", error=1452)
        self.sql("INSERT INTO iam_platform_group_member VALUES (100, 1001)", error=1062)

    def test_tenant_group_rejects_platform_and_other_tenant_members(self):
        self.sql("INSERT INTO iam_tenant_group_member VALUES (10, 110, 101)")
        self.sql("INSERT INTO iam_tenant_group_member VALUES (10, 110, 1001)", error=1452)
        self.sql("INSERT INTO iam_tenant_group_member VALUES (10, 110, 201)", error=1452)
        self.sql("INSERT INTO iam_tenant_group_member VALUES (20, 110, 201)", error=1452)
        self.sql("INSERT INTO iam_tenant_group_department VALUES (10, 110, 21, 1)", error=1452)

    def test_department_relationships_cannot_cross_tenants(self):
        self.sql("UPDATE iam_department SET parent_id = 21 WHERE id = 11", error=1452)
        self.sql("UPDATE iam_department SET parent_id = 11 WHERE id = 11", error=3819)
        self.sql("INSERT INTO iam_member_department (tenant_id, member_id, department_id) VALUES (10, 101, 21)",
                 error=1452)
        self.sql("INSERT INTO iam_member_department (tenant_id, member_id, department_id) VALUES (10, 201, 11)",
                 error=1452)

    def test_only_one_primary_department_but_multiple_memberships(self):
        self.sql("""
            INSERT INTO iam_member_department (tenant_id, member_id, department_id, is_primary)
            VALUES (10, 101, 11, 1), (10, 101, 12, 0)
        """)
        self.sql("UPDATE iam_member_department SET is_primary = 1 WHERE department_id = 12", error=1062)
        self.assertEqual("2", self.sql("SELECT COUNT(*) FROM iam_member_department WHERE member_id = 101"))

    def test_owner_must_reference_member_of_the_same_tenant(self):
        self.sql("UPDATE iam_tenant SET owner_member_id = 201 WHERE id = 10", error=1452)
        self.sql("UPDATE iam_tenant SET owner_member_id = 1001 WHERE id = 10", error=1452)

    def test_membership_status_is_independent_and_has_closed_vocabulary(self):
        self.sql("UPDATE iam_platform_member SET status = 'SUSPENDED' WHERE id = 1001")
        self.assertEqual("2", self.sql("SELECT COUNT(*) FROM iam_tenant_member WHERE account_id = 1 AND status = 'ACTIVE'"))
        self.sql("UPDATE iam_tenant_member SET status = 'REMOVED' WHERE id = 101")
        self.assertEqual("ACTIVE", self.sql("SELECT status FROM iam_tenant_member WHERE id = 201"))
        self.assertEqual("1", self.sql("SELECT enabled FROM iam_account WHERE id = 1"))
        self.sql("UPDATE iam_platform_member SET status = 'active' WHERE id = 1001", error=3819)
        self.sql("UPDATE iam_tenant_member SET status = 'UNKNOWN' WHERE id = 201", error=3819)

    def test_failed_initialization_can_roll_back_without_partial_tenant(self):
        self.sql("""
            START TRANSACTION;
            INSERT INTO iam_tenant (id, name) VALUES (30, 'Not committed');
            INSERT INTO iam_tenant_member (id, tenant_id, account_id, display_name) VALUES (301, 30, 1, 'Owner');
            UPDATE iam_tenant SET owner_member_id = 201 WHERE id = 30;
            COMMIT;
        """, error=1452)
        self.assertEqual("0", self.sql("SELECT COUNT(*) FROM iam_tenant WHERE id = 30"))
        self.assertEqual("0", self.sql("SELECT COUNT(*) FROM iam_tenant_member WHERE tenant_id = 30"))

    def test_catalog_rejects_cross_application_resource_and_menu_references(self):
        self.sql("INSERT INTO iam_action (id, application_id, resource_id, code, name) VALUES (3, 1, 2, 'bad', 'Bad')",
                 error=1452)
        self.sql("INSERT INTO iam_menu (id, application_id, name, kind) VALUES (1, 1, 'Menu', 'PAGE')")
        self.sql("INSERT INTO iam_menu_action VALUES (1, 1, 2)", error=1452)
        self.sql("INSERT INTO iam_menu_action VALUES (1, 1, 1)")
        self.sql("DELETE FROM iam_action WHERE id = 1", error=1451)

    def test_actions_are_exact_and_globally_unique(self):
        self.sql("INSERT INTO iam_action (id, application_id, resource_id, code, name) VALUES (3, 2, 2, 'iam-tenant:**', 'Bad')",
                 error=3819)
        self.sql("INSERT INTO iam_action (id, application_id, resource_id, code, name) VALUES (3, 2, 2, 'iam-platform:account:read', 'Duplicate')",
                 error=1062)

    def test_custom_revision_requires_shared_base_and_fixed_kind(self):
        self.sql("INSERT INTO iam_role_revision (id, role_id, kind, revision, base_revision_id, metadata_overrides) VALUES (4, 2, 'TENANT_CUSTOM', 2, 2, '{}')",
                 error=1452)
        self.sql("INSERT INTO iam_role_revision (id, role_id, kind, revision, metadata_overrides) VALUES (4, 1, 'TENANT_CUSTOM', 2, '{}')",
                 error=1452)
        self.sql("INSERT INTO iam_role_revision (id, role_id, kind, revision, metadata_overrides) VALUES (4, 1, 'SHARED', 1, '{}')",
                 error=1062)
        self.sql("INSERT INTO iam_role_revision (id, role_id, kind, revision, metadata_overrides) VALUES (4, 1, 'SHARED', 2, '{}')")
        self.assertEqual("1", self.sql("SELECT base_revision_id FROM iam_role_revision WHERE id = 2"))

    def test_single_delta_per_action_and_remove_has_no_scope(self):
        self.sql("INSERT INTO iam_role_delta VALUES (2, 2, 'REMOVE', '[]')")
        self.sql("INSERT INTO iam_role_delta VALUES (2, 2, 'ADD', '[]')", error=1062)
        self.sql("UPDATE iam_role_delta SET scopes = '[{\"kind\":\"ALL\"}]' WHERE revision_id = 2", error=3819)
        self.sql("INSERT INTO iam_role_grant VALUES (1, 999, '[]')", error=1452)

    def test_explicit_entitlement_and_audience_references(self):
        self.sql("INSERT INTO iam_app_audience (tenant_id, application_id, audience_kind) VALUES (10, 2, 'ALL')", error=1452)
        self.sql("INSERT INTO iam_tenant_app_entitlement (id, tenant_id, application_id, enabled, source) VALUES (1, 10, 2, 1, 'MANUAL')")
        self.sql("INSERT INTO iam_app_audience (tenant_id, application_id, audience_kind) VALUES (10, 2, 'SELECTED')")
        self.sql("INSERT INTO iam_audience_member VALUES (10, 2, 201)", error=1452)
        self.sql("INSERT INTO iam_audience_member VALUES (10, 2, 101)")
        self.sql("UPDATE iam_tenant_app_entitlement SET valid_from = '2026-09-13', valid_until = '2026-09-13' WHERE id = 1", error=3819)

    def test_delegation_recipients_are_domain_bound(self):
        self.sql("""
            INSERT INTO iam_delegation_grant (id, domain, platform_administrator_id, max_assignment_duration_seconds)
                VALUES (1, 'PLATFORM', 1001, 3600);
            INSERT INTO iam_delegation_grant (id, domain, tenant_id, tenant_administrator_id, max_assignment_duration_seconds)
                VALUES (2, 'TENANT', 10, 101, 3600);
        """)
        self.sql("INSERT INTO iam_delegation_recipient_member (delegation_id, domain, platform_member_id) VALUES (1, 'PLATFORM', 1001)")
        self.sql("INSERT INTO iam_delegation_recipient_member (delegation_id, domain, tenant_id, tenant_member_id) VALUES (1, 'TENANT', 10, 101)", error=1452)
        self.sql("INSERT INTO iam_delegation_recipient_department VALUES (1, 10, 11, 1)", error=1452)
        self.sql("INSERT INTO iam_delegation_recipient_department VALUES (2, 10, 11, 1)")
        self.sql("UPDATE iam_delegation_grant SET max_assignment_duration_seconds = 0 WHERE id = 1", error=3819)

    def test_assignment_cannot_mix_subject_domain_or_delegation(self):
        self.sql("INSERT INTO iam_delegation_grant (id, domain, platform_administrator_id, max_assignment_duration_seconds) VALUES (1, 'PLATFORM', 1001, 3600)")
        self.sql("""
            INSERT INTO iam_role_assignment (id, domain, tenant_id, subject_type, tenant_member_id,
                revision_id, revision_kind, scope_bindings, valid_from, source)
            VALUES (1, 'TENANT', 10, 'MEMBER', 101, 1, 'SHARED', '{}', '2026-09-13', 'MANUAL')
        """)
        self.sql("UPDATE iam_role_assignment SET delegation_grant_id = 1 WHERE id = 1", error=1452)
        self.sql("UPDATE iam_role_assignment SET tenant_member_id = 201 WHERE id = 1", error=1452)
        self.sql("UPDATE iam_role_assignment SET platform_member_id = 1001 WHERE id = 1", error=3819)
        self.sql("UPDATE iam_role_assignment SET revision_kind = 'TENANT_CUSTOM' WHERE id = 1", error=1452)
        self.sql("DELETE FROM iam_role_revision WHERE id = 1", error=1451)

    def test_policy_default_and_selector_domains_are_enforced(self):
        self.sql("INSERT INTO iam_default_policy_revision VALUES (1, 'DIRECTORY', 1, '{}'), (2, 'FIELD', 1, '{}')")
        self.sql("INSERT INTO iam_policy_selector VALUES (1, 10), (2, 20)")
        self.sql("INSERT INTO iam_directory_policy (tenant_id, default_revision_id) VALUES (10, 2)", error=1452)
        self.sql("INSERT INTO iam_directory_policy (tenant_id, default_revision_id) VALUES (10, 1)")
        self.sql("UPDATE iam_directory_policy SET default_selector_id = 1 WHERE tenant_id = 10", error=3819)
        self.sql("UPDATE iam_directory_policy SET default_scope = 'SELECTED', default_selector_id = 2 WHERE tenant_id = 10", error=1452)
        self.sql("INSERT INTO iam_policy_selector_member VALUES (10, 1, 201)", error=1452)
        self.sql("INSERT INTO iam_directory_rule VALUES (1, 10, 'ALLOW', 1, 2)", error=1452)

    def test_field_policy_never_allows_editable_hidden_or_masked_value(self):
        self.sql("INSERT INTO iam_default_policy_revision VALUES (2, 'FIELD', 1, '{}')")
        self.sql("INSERT INTO iam_policy_selector VALUES (1, 10)")
        self.sql("INSERT INTO iam_field_policy (tenant_id, default_revision_id) VALUES (10, 2)")
        self.sql("INSERT INTO iam_field_rule VALUES (1, 10, 'MANAGEMENT', 'phone', 1, '[]', '{}', 'MASKED', 0)")
        self.sql("UPDATE iam_field_rule SET editable = 1 WHERE id = 1", error=3819)
        self.sql("UPDATE iam_field_rule SET visibility = 'HIDDEN', editable = 1 WHERE id = 1", error=3819)

    def test_migration_batch_requires_verification_metadata_and_explicit_resolution(self):
        self.sql("INSERT INTO iam_migration_batch (id, source_fingerprint, target_identifier, rules_version) VALUES ('batch', REPEAT('a', 64), 'isolated-fixture', '1')")
        self.sql("UPDATE iam_migration_batch SET status = 'VERIFIED' WHERE id = 'batch'", error=3819)
        self.sql("INSERT INTO iam_migration_mapping VALUES ('batch', 'sys_user', '1', 'Account', '1', 'KEEP_MAPPED', 'fixture', 'explicit mapping')")
        self.sql("INSERT INTO iam_migration_mapping VALUES ('batch', 'sys_user', '1', 'Account', '1', 'KEEP_MAPPED', 'fixture', 'duplicate')", error=1062)
        self.sql("INSERT INTO iam_migration_issue (id, batch_id, issue_code, safe_description) VALUES (1, 'batch', 'OwnerAmbiguous', 'owner mapping required')")
        self.sql("UPDATE iam_migration_issue SET disposition = 'SKIP' WHERE id = 1", error=3819)


if __name__ == "__main__":
    unittest.main(verbosity=2)
