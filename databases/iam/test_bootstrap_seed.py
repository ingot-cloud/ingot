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
import re

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT))
sys.path.insert(0, str(ROOT / 'tools/iam'))
from schema_comments import normalized
from tools.iam.generate_bootstrap import ADMIN_ROLE_CODE
from tools.iam.database_sources import read_source, schema_files, sources, legacy_schema_files


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

    def test_picker_name_capability_does_not_grant_default_filter_permission(self):
        self.bootstrap()
        self.assertEqual('true', self.sql("""SELECT JSON_EXTRACT(res.field_capabilities,'$[0].filterable') FROM iam_resource res
            JOIN iam_application app ON app.id=res.application_id WHERE app.code='iam-tenant' AND res.code='member'"""))
        self.assertEqual('false', self.sql("SELECT JSON_EXTRACT(definition,'$.operations.displayName.filterable') FROM iam_default_policy_revision WHERE kind='FIELD' ORDER BY revision DESC LIMIT 1"))

    def test_picker_name_patch_is_key_based_idempotent_and_preserves_policies(self):
        self.bootstrap()
        policies = self.sql("SELECT id,definition FROM iam_default_policy_revision ORDER BY id")
        others = self.sql("""SELECT res.id,res.field_capabilities,res.version FROM iam_resource res JOIN iam_application app ON app.id=res.application_id
            WHERE app.code!='iam-tenant' OR res.code!='member' ORDER BY res.id""")
        roles = self.sql("SELECT id,resource_field_permissions FROM iam_role_revision ORDER BY id")
        self.sql("""UPDATE iam_resource res JOIN iam_application app ON app.id=res.application_id SET res.version=8,
            res.field_capabilities=JSON_ARRAY(JSON_OBJECT('key','phone','filterable',CAST('true' AS JSON)),JSON_OBJECT('key','displayName','filterable',CAST('false' AS JSON)))
            WHERE app.code='iam-tenant' AND res.code='member'""")
        patch = (SCHEMA_DIRECTORY / 'migrations' / '025_member_picker_name_filter.sql').read_text()
        self.sql(patch)
        check = """SELECT JSON_EXTRACT(res.field_capabilities,'$[1].filterable'),JSON_EXTRACT(res.field_capabilities,'$[0].filterable'),res.version
            FROM iam_resource res JOIN iam_application app ON app.id=res.application_id WHERE app.code='iam-tenant' AND res.code='member'"""
        self.assertEqual('true\ttrue\t9',self.sql(check))
        self.sql(patch)
        self.assertEqual('true\ttrue\t9',self.sql(check))
        self.assertEqual(policies,self.sql("SELECT id,definition FROM iam_default_policy_revision ORDER BY id"))
        self.assertEqual(roles,self.sql("SELECT id,resource_field_permissions FROM iam_role_revision ORDER BY id"))
        self.assertEqual(others,self.sql("""SELECT res.id,res.field_capabilities,res.version FROM iam_resource res JOIN iam_application app ON app.id=res.application_id
            WHERE app.code!='iam-tenant' OR res.code!='member' ORDER BY res.id"""))

    def test_root_menu_icons(self):
        self.bootstrap()
        self.assertEqual(
            'iam-platform\tplatform.iam.config\tep:setting\n'
            'iam-platform\tplatform.iam.manage\tep:monitor\n'
            'iam-platform\tplatform.iam.tenant\tep:office-building\n'
            'iam-platform\tplatform.settings\tep:setting\n'
            'iam-tenant\torg.iam.authorization.root\tep:connection\n'
            'iam-tenant\torg.iam.compliance\tep:lock\n'
            'iam-tenant\torg.iam.organization\tep:office-building\n'
            'iam-tenant\torg.iam.workspace\tep:grid\n'
            'platform-developer\tplatform.develop\tep:cpu\n'
            'platform-member\tmember.root\tep:iphone\n'
            'platform-security\tsecurity.root\tep:lock',
            self.sql("""
                SELECT app.code, menu.route_name, menu.icon FROM iam_menu menu
                JOIN iam_application app ON app.id = menu.application_id
                WHERE menu.parent_id IS NULL AND menu.kind = 'DIRECTORY'
                ORDER BY app.code, menu.route_name
            """))
        self.assertEqual('0', self.sql("SELECT COUNT(*) FROM iam_menu "
                                       "WHERE parent_id IS NOT NULL AND icon IS NOT NULL AND TRIM(icon) <> '' "
                                       "AND application_id <> (SELECT id FROM iam_application WHERE code='platform-member')"))

    def test_member_catalog_upgrade_dry_run_repeat_conflict_and_rollback(self):
        directory = SCHEMA_DIRECTORY / 'migrations'
        migration = (directory / '036_member_platform_application.sql').read_text()
        rollback = (directory / '036_member_platform_application.rollback.sql').read_text()
        self.sql("INSERT INTO iam_application (id,code,domain,name) VALUES (200001,'existing','PLATFORM','已有应用')")
        self.sql("INSERT INTO iam_menu (id,application_id,name,path,kind) "
                 "VALUES (300001,200001,'已有页面','/member/users','PAGE')")
        self.sql(migration, error=1644)
        self.assertEqual(1, self.count('iam_application'))
        self.sql('DELETE FROM iam_menu WHERE id = 300001')
        self.sql('SET @iam_member_dry_run = TRUE;\n' + migration)
        self.assertEqual(1, self.count('iam_application'))
        report = self.sql(migration)
        self.assertEqual(report, self.sql(migration))
        self.assertEqual(20, self.count('iam_action'))
        self.assertEqual(0, self.count('iam_role_grant'))
        self.assertEqual(
            '/member\tep:iphone\t3\n/member/users\tep:user\t1\n'
            '/member/roles\tep:avatar\t1\n/member/permissions\tep:key\t1',
            self.sql("""SELECT m.path,m.icon,COUNT(*) FROM iam_menu m
                JOIN iam_menu_action a ON a.menu_id=m.id GROUP BY m.id ORDER BY m.parent_id,m.sort_order"""),
        )
        self.sql(rollback)
        self.sql(rollback)
        self.assertEqual(1, self.count('iam_application', "code = 'platform:member' AND NOT enabled AND version = 1"))
        self.assertEqual(20, self.count('iam_action'))

    def test_root_menu_icon_patch_is_scoped_and_preserves_custom_icons(self):
        # Applications and menus can have IDs different from the seed's reserved IDs.
        self.sql("INSERT INTO iam_application (id, code, domain, name) "
                 "VALUES (500001, 'iam-platform', 'PLATFORM', '既有平台治理')")
        self.bootstrap()
        self.sql("UPDATE iam_application SET code='platform:develop' WHERE code='platform-developer'")
        root_condition = "parent_id IS NULL AND kind = 'DIRECTORY'"
        old_roots = " AND route_name NOT IN ('platform.settings','security.root','member.root')"
        expected_icons = self.sql("SELECT id, icon FROM iam_menu WHERE " + root_condition + old_roots
                                  + " AND route_name <> 'org.iam.workspace' ORDER BY id")
        self.sql("""
            UPDATE iam_menu SET icon = CASE route_name
                WHEN 'platform.iam.config' THEN ''
                WHEN 'org.iam.organization' THEN '   '
                WHEN 'org.iam.workspace' THEN 'mdi:home'
                ELSE NULL END, version = 11
            WHERE parent_id IS NULL AND kind = 'DIRECTORY';
            INSERT INTO iam_application (id, code, domain, name)
                VALUES (600001, 'custom-app', 'PLATFORM', '自定义应用');
            INSERT INTO iam_menu (id, application_id, parent_id, name, route_name, kind)
                VALUES (600002, 600001, NULL, '其他应用目录', 'platform.iam.config', 'DIRECTORY'),
                       (600003, 500001, NULL, '一级页面', 'platform.iam.config', 'PAGE'),
                       (600004, 500001, 600003, '子目录', 'platform.iam.config', 'DIRECTORY');
        """)
        unchanged_columns = """
            SELECT id, application_id, parent_id, name, path, view_path, route_name, kind,
                   match_mode, access_mode, sort_order, hidden, is_cache, props, route_params, enabled
            FROM iam_menu ORDER BY id
        """
        before = self.sql(unchanged_columns)
        bindings = self.sql('SELECT * FROM iam_menu_action ORDER BY menu_id, action_id')
        untouched_condition = "parent_id IS NOT NULL OR kind = 'PAGE' OR application_id = 600001 "
        untouched = self.sql('SELECT id, icon, version FROM iam_menu WHERE '
                             + untouched_condition + ' ORDER BY id')
        patch = (SCHEMA_DIRECTORY / 'migrations/019_root_menu_icons.sql').read_text()
        self.sql(patch)
        after = self.sql('SELECT id, icon, version FROM iam_menu ORDER BY id')
        self.sql(patch)
        self.assertEqual(after, self.sql('SELECT id, icon, version FROM iam_menu ORDER BY id'))
        self.assertEqual(expected_icons, self.sql("SELECT id, icon FROM iam_menu WHERE "
                         + root_condition + old_roots + " AND route_name <> 'org.iam.workspace' "
                         "AND application_id <> 600001 ORDER BY id"))
        self.assertEqual('mdi:home\t11', self.sql("SELECT icon, version FROM iam_menu "
                                               "WHERE route_name = 'org.iam.workspace'"))
        self.assertEqual('7', self.sql('SELECT COUNT(*) FROM iam_menu WHERE version = 12'))
        self.assertEqual(untouched, self.sql('SELECT id, icon, version FROM iam_menu WHERE '
                                             + untouched_condition + ' ORDER BY id'))
        self.assertEqual(before, self.sql(unchanged_columns))
        self.assertEqual(bindings, self.sql('SELECT * FROM iam_menu_action ORDER BY menu_id, action_id'))

    def test_authorization_audit_name_migration_is_scoped_and_repeatable(self):
        self.bootstrap()
        self.sql("UPDATE iam_menu SET route_name='security.iam.authorization.audit' WHERE view_path IN ('platform.iam.authorization.audit','org.iam.authorization.audit')")
        condition = "route_name = 'security.iam.authorization.audit'"
        original = self.sql(f"SELECT id, application_id, path, view_path, access_mode, match_mode, version FROM iam_menu WHERE {condition} ORDER BY id")
        self.assertEqual(2, self.count('iam_menu', condition))
        self.sql("UPDATE iam_menu SET name = '旧名称' WHERE " + condition)
        other = self.sql("SELECT id, name FROM iam_menu WHERE " + condition.replace(' = ', ' <> ') + " ORDER BY id")
        bindings = self.sql("SELECT * FROM iam_menu_action ORDER BY menu_id, action_id")
        migration = (SCHEMA_DIRECTORY / 'migrations/018_authorization_audit_menu.sql').read_text()
        self.sql(migration)
        self.sql(migration)
        self.assertEqual(2, self.count('iam_menu', condition + " AND name = '授权审计'"))
        self.assertEqual(original, self.sql(f"SELECT id, application_id, path, view_path, access_mode, match_mode, version FROM iam_menu WHERE {condition} ORDER BY id"))
        self.assertEqual(other, self.sql("SELECT id, name FROM iam_menu WHERE " + condition.replace(' = ', ' <> ') + " ORDER BY id"))
        self.assertEqual(bindings, self.sql("SELECT * FROM iam_menu_action ORDER BY menu_id, action_id"))

    def test_generated_bundle_matches_component_schema_and_initial_data(self):
        for schema in sources('frameworkSchemas'):
            self.sql(read_source(schema))
        self.bootstrap()
        self.sql(sources('initialAccounts')[0].read_text())
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
        self.assertEqual(59, len(tables))
        self.assertEqual(181, self.count('iam_action'))
        self.assertEqual(41, self.count('iam_menu'))
        for table in ['iam_tenant', 'iam_tenant_member', 'iam_authorization_audit',
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
                # Organization and owner form a real FK cycle; runtime assignments reference seed roles.
                self.sql("""
                    INSERT INTO iam_account (id, username, password_hash)
                        VALUES (900002, 'temporary-owner', 'fixture');
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
                                               "AND parent_id IS NULL "
                                               "AND (view_path IS NULL OR view_path <> 'layout.main')"))
                self.assertEqual('0', self.sql("SELECT COUNT(*) FROM iam_menu WHERE kind = 'DIRECTORY' "
                                               "AND parent_id IS NOT NULL "
                                               "AND (view_path IS NULL OR view_path <> 'layout.simple')"))
                self.assertEqual(0, self.count('iam_action', 'NOT enabled'))
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
                self.assertEqual(normalized(before[table]), normalized(self.sql(f'SHOW CREATE TABLE `{table}`')), table)
        self.assertEqual(before_indexes, self.sql(indexes))

    def test_catalog_seed_keeps_identity_creation_separate(self):
        self.bootstrap()
        self.assertEqual(5, self.count("iam_application"))
        self.assertEqual(44, self.count("iam_resource"))
        self.assertEqual(181, self.count("iam_action"))
        self.assertEqual(41, self.count("iam_menu"))
        self.assertEqual('FULL', self.sql("""
            SELECT JSON_UNQUOTE(JSON_EXTRACT(revision.resource_field_permissions,
                CONCAT('$."',resource.id,'".visibility.phone')))
            FROM iam_role_revision revision JOIN iam_role_definition role ON role.id=revision.role_id
            JOIN iam_application app ON app.domain=role.domain AND app.code='iam-platform'
            JOIN iam_resource resource ON resource.application_id=app.id AND resource.code='member'
            WHERE role.kind='SYSTEM' AND role.domain='PLATFORM'
        """))
        self.assertEqual(0, self.count("iam_resource", "code='resource-field-policy'"))
        self.assertEqual(1, self.count("iam_default_policy_revision"))
        self.assertEqual(1, self.count("biz_leaf_alloc", "biz_tag = 'iam'"))
        # The catalog component does not create identities before the account seed runs.
        self.assertEqual(0, self.count("iam_account"))
        self.assertEqual(0, self.count("iam_platform_member"))
        self.assertEqual(0, self.count("iam_role_assignment"))

    def test_runtime_rebuild_preserves_optional_legacy_tables_and_rows(self):
        for source in legacy_schema_files():
            self.sql(read_source(source))
        self.sql("INSERT INTO iam_migration_batch(id,source_fingerprint,target_identifier,rules_version) "
                 "VALUES('preserved',REPEAT('a',64),'isolated-fixture','1')")
        self.sql("INSERT INTO sys_tenant_plan_record(id,tenant_id,plan_id,type,duration,unit,created_at) "
                 "VALUES(1,10,1,'0',12,'1',UTC_TIMESTAMP())")
        self.sql(sources('bundle')[0].read_text())
        self.assertEqual('preserved', self.sql('SELECT id FROM iam_migration_batch'))
        self.assertEqual('12', self.sql('SELECT duration FROM sys_tenant_plan_record WHERE id=1'))
        self.assertEqual(1, self.count('iam_account'))
        self.assertEqual(0, self.count('iam_tenant'))
        for source in legacy_schema_files():
            for table in re.findall(r'^CREATE TABLE\s+`?([a-z_]+)', read_source(source), re.M):
                self.assertIn(table, self.sql('SHOW TABLES').splitlines())

    def test_full_initialization_creates_one_platform_admin_and_no_tenant(self):
        self.sql(sources('bundle')[0].read_text())
        self.assertEqual(1, self.count('iam_account'))
        self.assertEqual('platform\t1\t1', self.sql("""
            SELECT username, enabled, must_change_password FROM iam_account
        """))
        self.assertRegex(self.sql('SELECT password_hash FROM iam_account'),
                         r'^\{bcrypt\}\$2a\$10\$[./A-Za-z0-9]{53}$')
        self.assertEqual(1, self.count('iam_platform_member'))
        self.assertEqual(1, self.count('iam_role_assignment'))
        self.assertEqual(f'{ADMIN_ROLE_CODE}\tINITIALIZATION', self.sql("""
            SELECT role.code, assignment.source
            FROM iam_account account
            JOIN iam_platform_member member ON member.account_id = account.id AND member.status = 'ACTIVE'
            JOIN iam_role_assignment assignment ON assignment.platform_member_id = member.id
                AND assignment.domain = 'PLATFORM' AND assignment.subject_type = 'MEMBER'
                AND assignment.status = 'ACTIVE' AND assignment.valid_from <= UTC_TIMESTAMP(6)
                AND assignment.valid_until IS NULL AND assignment.delegation_grant_id IS NULL
            JOIN iam_role_revision revision ON revision.id = assignment.revision_id
            JOIN iam_role_definition role ON role.id = revision.role_id AND role.kind = 'SYSTEM'
                AND role.domain = 'PLATFORM' AND role.enabled
            WHERE account.username = 'platform'
        """))
        self.assertEqual(0, self.count('iam_tenant'))
        self.assertEqual(0, self.count('iam_tenant_member'))
        self.sql("UPDATE iam_account SET password_hash = 'changed', must_change_password = FALSE")
        self.sql(sources('initialAccounts')[0].read_text())
        self.assertEqual('changed\t0', self.sql('SELECT password_hash, must_change_password FROM iam_account'))
        self.assertEqual(1, self.count('iam_platform_member'))
        self.assertEqual(1, self.count('iam_role_assignment'))

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
        self.assertEqual(1, self.count("iam_default_policy_revision", "kind = 'FIELD'"))
        self.assertEqual("MASKED", self.sql(
            "SELECT JSON_UNQUOTE(JSON_EXTRACT(definition, '$.fields.phone')) "
            "FROM iam_default_policy_revision WHERE kind = 'FIELD'"))

    def test_governance_grants_cover_own_domain_only(self):
        self.bootstrap()
        for role, domain, expected in ((ADMIN_ROLE_CODE, "PLATFORM", 137),
                                       ("tenant-governance", "TENANT", 44)):
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
        self.assertEqual('platform.iam.accounts\nplatform.iam.personnel\nplatform.iam.authorization\nplatform.iam.authorization.audit',
                         self.sql("""
            SELECT menu.route_name FROM iam_menu menu
            JOIN iam_menu parent ON parent.id = menu.parent_id
            WHERE parent.route_name = 'platform.iam.manage' ORDER BY menu.sort_order, menu.id
        """))

    def test_developer_pages_and_executable_operations_belong_to_an_independent_platform_app(self):
        self.bootstrap()
        self.assertEqual('PLATFORM\t0\t开发者平台', self.sql("""
            SELECT domain, baseline, name FROM iam_application WHERE code = 'platform-developer'
        """))
        self.assertEqual('layout.main', self.sql("""
            SELECT menu.view_path FROM iam_menu menu JOIN iam_application app ON app.id = menu.application_id
            WHERE app.code = 'platform-developer' AND menu.kind = 'DIRECTORY'
        """))
        self.assertEqual('platform.develop.qrcode\tplatform-developer:qr-code:generate\n'
                         'platform.develop.client\tplatform-developer:oauth-client:read-list\n'
                         'platform.develop.social\tplatform-developer:social-config:read\n'
                         'platform.develop.id\tplatform-developer:id-allocation:read', self.sql("""
            SELECT menu.view_path, action.code FROM iam_menu menu
            JOIN iam_application app ON app.id = menu.application_id AND app.code = 'platform-developer'
            JOIN iam_menu parent ON parent.id = menu.parent_id AND parent.application_id = app.id
            JOIN iam_menu_action link ON link.menu_id = menu.id AND link.application_id = app.id
            JOIN iam_action action ON action.id = link.action_id AND action.application_id = app.id
            WHERE menu.kind = 'PAGE' AND parent.route_name = 'platform.develop'
            ORDER BY menu.sort_order
        """))
        self.assertEqual('id-allocation\noauth-client\nqr-code\nsocial-config', self.sql("""
            SELECT resource.code
            FROM iam_resource resource JOIN iam_application app ON app.id = resource.application_id
            WHERE app.code = 'platform-developer' ORDER BY resource.code
        """))
        self.assertEqual('15', self.sql("""
            SELECT COUNT(*) FROM iam_action action
            JOIN iam_application app ON app.id = action.application_id
            JOIN iam_resource resource ON resource.id = action.resource_id AND resource.application_id = app.id
            WHERE app.code = 'platform-developer'
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

    def test_rebuilt_minimal_roles_open_only_their_authorized_page_entries(self):
        self.sql(sources('bundle')[0].read_text())
        self.sql("INSERT INTO iam_tenant (id, name) VALUES (850000, '隔离授权组织')")
        fixtures = [
            ('platform-member-read', 'PLATFORM', ['iam-platform:member:read'], ['platform.iam.personnel']),
            ('platform-group-read', 'PLATFORM', ['iam-platform:group:read'], ['platform.iam.personnel']),
            ('platform-personnel-read', 'PLATFORM', ['iam-platform:member:read', 'iam-platform:group:read'],
             ['platform.iam.personnel']),
            ('platform-diagnosis', 'PLATFORM', ['iam-platform:authorization-diagnosis:run'], ['platform.iam.authorization']),
            ('platform-no-actions', 'PLATFORM', [], []),
            ('tenant-application-read', 'TENANT', ['iam-tenant:application:read'], ['org.iam.applications']),
            ('tenant-workbench', 'TENANT', ['iam-tenant:workbench:read'], ['org.iam.workbench']),
            ('tenant-diagnosis', 'TENANT', ['iam-tenant:authorization-diagnosis:run'], ['org.iam.authorization']),
            ('security-policy-editor', 'PLATFORM', ['platform-security:access-policy:read',
                                                    'platform-security:access-policy:update'], ['security.access.protection']),
            ('security-block-event-reader', 'PLATFORM', ['platform-security:block-event:read'],
             ['security.access.protection']),
            ('app-user-editor', 'PLATFORM', ['platform-member:user:read-list',
                                             'platform-member:user:update'], ['member.user']),
        ]
        for index, (code, domain, actions, routes) in enumerate(fixtures):
            with self.subTest(role=code):
                role, revision = 850001 + index, 851001 + index
                account, member, assignment = 852001 + index, 853001 + index, 854001 + index
                kind, tenant = ('PLATFORM_CUSTOM', 'NULL') if domain == 'PLATFORM' else ('TENANT_CUSTOM', '850000')
                self.sql(f"""
                    INSERT INTO iam_account(id, username, password_hash) VALUES ({account}, '{code}', 'fixture');
                    INSERT INTO iam_role_definition(id, domain, tenant_id, kind, code, name)
                        VALUES ({role}, '{domain}', {tenant}, '{kind}', '{code}', '{code}');
                    INSERT INTO iam_role_revision(id, role_id, kind, revision, metadata_overrides)
                        VALUES ({revision}, {role}, '{kind}', 1, JSON_OBJECT());
                """)
                if domain == 'PLATFORM':
                    self.sql(f"""
                        INSERT INTO iam_platform_member(id, account_id, display_name)
                            VALUES ({member}, {account}, '{code}');
                        INSERT INTO iam_role_assignment(id, domain, subject_type, platform_member_id,
                            revision_id, revision_kind, scope_bindings, valid_from, source)
                            VALUES ({assignment}, '{domain}', 'MEMBER', {member},
                                {revision}, '{kind}', JSON_OBJECT(), UTC_TIMESTAMP(6), 'MANUAL');
                    """)
                else:
                    self.sql(f"""
                        UPDATE iam_role_definition SET current_revision_id={revision} WHERE id={role};
                        INSERT INTO iam_tenant_member(id, tenant_id, account_id, display_name)
                            VALUES ({member}, {tenant}, {account}, '{code}');
                        INSERT INTO iam_role_assignment(id, domain, tenant_id, subject_type, tenant_member_id,
                            role_id, role_kind, role_tenant_key, scope_bindings, valid_from, source)
                            VALUES ({assignment}, '{domain}', {tenant}, 'MEMBER', {member},
                                {role}, '{kind}', {tenant}, JSON_OBJECT(), UTC_TIMESTAMP(6), 'MANUAL');
                    """)
                if actions:
                    codes = ','.join("'" + action + "'" for action in actions)
                    self.sql(f"""
                        INSERT INTO iam_role_grant(revision_id, action_id, scopes)
                        SELECT {revision}, action.id, JSON_ARRAY(JSON_OBJECT('kind', 'ALL'))
                        FROM iam_action action JOIN iam_application app ON app.id = action.application_id
                        WHERE app.domain = '{domain}' AND action.enabled AND action.code IN ({codes});
                    """)
                self.assertEqual(sorted(actions), self.sql(f"""
                    SELECT action.code FROM iam_role_grant g JOIN iam_action action ON action.id = g.action_id
                    WHERE g.revision_id = {revision} ORDER BY action.code
                """).splitlines())
                self.assertEqual(routes, self.sql(f"""
                    SELECT DISTINCT menu.route_name FROM iam_menu menu
                    JOIN iam_menu_action link ON link.menu_id = menu.id
                    JOIN iam_role_assignment a ON a.id = {assignment} AND a.status = 'ACTIVE'
                    LEFT JOIN iam_role_definition r ON r.id = a.role_id AND r.enabled
                    JOIN iam_role_grant g ON g.action_id = link.action_id
                        AND g.revision_id = COALESCE(a.revision_id, r.current_revision_id)
                    WHERE menu.enabled AND menu.kind = 'PAGE' AND menu.access_mode = 'ACTION' AND menu.match_mode = 'ANY'
                    ORDER BY menu.route_name
                """).splitlines())
        self.assertEqual(len(fixtures) + 1, self.count('iam_role_assignment'))
        self.assertEqual(80, self.count('iam_menu_action'))
        self.assertEqual(29, self.count('iam_menu', "kind = 'PAGE'"))
        self.assertEqual(0, self.count('iam_action', "code IN ('platform:develop:client:query', "
                                      "'iam-tenant:role:upgrade', 'iam-platform:authorization:diagnose')"))

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

    def test_pages_expose_their_view_key_and_directories_use_layout(self):
        self.bootstrap()
        self.assertEqual("0", self.sql("SELECT COUNT(*) FROM iam_menu "
                                       "WHERE kind = 'PAGE' AND (view_path IS NULL OR view_path = '')"))
        self.assertGreater(self.count('iam_menu', "kind = 'DIRECTORY'"), 0)
        self.assertEqual("0", self.sql("SELECT COUNT(*) FROM iam_menu WHERE kind = 'DIRECTORY' "
                                       "AND parent_id IS NULL "
                                       "AND (view_path IS NULL OR view_path <> 'layout.main')"))
        self.assertEqual("0", self.sql("SELECT COUNT(*) FROM iam_menu WHERE kind = 'DIRECTORY' "
                                       "AND parent_id IS NOT NULL "
                                       "AND (view_path IS NULL OR view_path <> 'layout.simple')"))
        self.assertEqual("0", self.sql("SELECT COUNT(*) FROM iam_menu "
                                       "WHERE path IS NULL OR route_name IS NULL"))

    def test_seed_attaches_to_a_pre_existing_application_identifier(self):
        # A pre-existing application keeps its identifier and still receives the full catalog.
        self.sql("INSERT INTO iam_application (id, code, domain, name, baseline) "
                 "VALUES (500001, 'iam-tenant', 'TENANT', '既有组织治理', TRUE)")
        self.bootstrap()
        self.assertEqual(1, self.count("iam_application", "code = 'iam-tenant'"))
        self.assertEqual("500001", self.sql("SELECT id FROM iam_application WHERE code = 'iam-tenant'"))
        self.assertEqual(13, self.count("iam_resource", "application_id = 500001"))
        self.assertEqual(44, self.count("iam_action", "application_id = 500001"))
        self.assertEqual(14, self.count("iam_menu", "application_id = 500001"))
        self.assertEqual(181, self.count("iam_role_grant"))

    def test_manual_verification_seed_layers_on_the_cold_start_catalog(self):
        self.bootstrap()
        self.sql(MANUAL_SEED.read_text())
        # The fixture only adds sign-in identities; the catalog stays exactly as seeded.
        self.assertEqual(2, self.count("iam_account"))
        self.assertEqual(1, self.count("iam_platform_member"))
        self.assertEqual(2, self.count("iam_role_definition"))
        self.assertEqual(181, self.count("iam_role_grant"))
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
