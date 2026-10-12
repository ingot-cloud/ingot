"""在一次性、无网络 MySQL 验证020；不连接现有库。"""
import unittest
import test_bootstrap_seed as seed
SCHEMA_DIRECTORY = seed.SCHEMA_DIRECTORY


class CapabilityMigrationTest(seed.BootstrapSeedTest):
    def patch(self, error=None):
        return self.sql((SCHEMA_DIRECTORY / 'migrations/020_platform_capability_completion.sql').read_text(), error=error)

    def legacy(self):
        self.bootstrap()
        # 模拟本轮之前未收录正式安全目录，但人工维护了一组凭证目录。
        self.sql("""
          DELETE g FROM iam_role_grant g JOIN iam_action a ON a.id=g.action_id JOIN iam_application app ON app.id=a.application_id WHERE app.code='platform:security';
          DELETE ma FROM iam_menu_action ma JOIN iam_application app ON app.id=ma.application_id WHERE app.code='platform:security';
          DELETE m FROM iam_menu m JOIN iam_application app ON app.id=m.application_id WHERE app.code='platform:security' AND m.parent_id IS NOT NULL;
          DELETE m FROM iam_menu m JOIN iam_application app ON app.id=m.application_id WHERE app.code='platform:security';
          DELETE a FROM iam_action a JOIN iam_application app ON app.id=a.application_id WHERE app.code='platform:security';
          DELETE r FROM iam_resource r JOIN iam_application app ON app.id=r.application_id WHERE app.code='platform:security';
          DELETE FROM iam_application WHERE code='platform:security';
          INSERT INTO iam_resource(id,application_id,code,name,scope_capabilities,field_capabilities)
            SELECT 600001,id,'credential-policy','人工凭证策略',CAST('["ALL"]' AS JSON),CAST('[]' AS JSON) FROM iam_application WHERE code='iam-platform';
          INSERT INTO iam_action(id,application_id,resource_id,code,name)
            SELECT 600002,id,600001,'iam-platform:credential-policy:read','人工读操作' FROM iam_application WHERE code='iam-platform';
          INSERT INTO iam_menu(id,application_id,name,path,view_path,route_name,kind,icon,hidden,is_cache,access_mode,match_mode)
            SELECT 600003,id,'旧凭证页','/platform/security/credential','security.credential','security.credential','PAGE','mdi:custom',TRUE,TRUE,'OPEN','ALL' FROM iam_application WHERE code='iam-platform';
          INSERT INTO iam_menu(id,application_id,name,path,view_path,route_name,kind)
            SELECT 600010,id,'安全与运维','/platform/iam/security','layout.main','platform.iam.security','DIRECTORY' FROM iam_application WHERE code='iam-platform';
          UPDATE iam_menu SET parent_id=600010 WHERE id=600003 OR view_path='platform.iam.authorization.audit';
          INSERT INTO iam_menu_action SELECT application_id,600003,600002 FROM iam_resource WHERE id=600001;
          INSERT INTO iam_role_grant(revision_id,action_id,scopes) VALUES(141001,600002,CAST('[{"kind":"ALL"}]' AS JSON));
          UPDATE iam_menu SET view_path='security.iam.authorization.audit',route_name='security.iam.authorization.audit',path='/security/iam/authorization/audit' WHERE view_path IN ('platform.iam.authorization.audit','org.iam.authorization.audit');
          UPDATE iam_menu SET view_path='security.iam.member.permissions',route_name='security.iam.member.permissions' WHERE view_path='org.iam.member.policies';
          UPDATE iam_menu SET view_path='platform.config.dict',route_name='platform.config.dict',path='/platform/config/dict' WHERE view_path='platform.settings.dictionaries';
        """)

    def test_preserves_ids_custom_settings_and_frozen_authorization(self):
        self.legacy()
        protected='SELECT revision_id,action_id,scopes FROM iam_role_grant ORDER BY revision_id,action_id'
        grants=self.sql(protected)
        versions=self.sql('SELECT * FROM iam_role_revision ORDER BY id')
        self.sql("""
          INSERT INTO iam_account(id,username,password_hash) VALUES(700001,'migration-reader','unused');
          INSERT INTO iam_platform_member(id,account_id,display_name) VALUES(700002,700001,'迁移测试成员');
          INSERT INTO iam_delegation_grant(id,domain,platform_administrator_id,max_assignment_duration_seconds)
            VALUES(700003,'PLATFORM',700002,3600);
          INSERT INTO iam_delegation_action_ceiling(delegation_id,action_id,scopes,scope_bindings)
            VALUES(700003,600002,CAST('[{"kind":"ALL"}]' AS JSON),CAST('{}' AS JSON));
          INSERT INTO iam_role_assignment(id,domain,subject_type,platform_member_id,revision_id,revision_kind,
            scope_bindings,delegation_grant_id,valid_from,source)
            VALUES(700004,'PLATFORM','MEMBER',700002,141001,'SYSTEM',CAST('{}' AS JSON),700003,'2026-10-12 00:00:00','MANUAL');
          INSERT INTO iam_authorization_audit(id,event_id,actor_account_id,actor_member_id,domain,target_type,target_id,
            change_type,safe_before,safe_after,revisions,delegation_id,assignment_id,occurred_at)
            VALUES(700005,'capability-migration-audit',700001,700002,'PLATFORM','resource','600001','UPDATE',
              CAST('{}' AS JSON),CAST('{}' AS JSON),CAST('{}' AS JSON),700003,700004,'2026-10-12 00:00:00');
        """)
        references={table:self.sql(f'SELECT * FROM {table} ORDER BY 1') for table in
          ('iam_delegation_grant','iam_delegation_action_ceiling','iam_role_assignment','iam_authorization_audit')}
        self.patch()
        self.assertEqual(grants,self.sql(protected))
        self.assertEqual(versions,self.sql('SELECT * FROM iam_role_revision ORDER BY id'))
        for table, rows in references.items():
            self.assertEqual(rows,self.sql(f'SELECT * FROM {table} ORDER BY 1'),table)
        self.assertEqual('platform:security\t600001',self.sql("SELECT app.code,r.id FROM iam_resource r JOIN iam_application app ON app.id=r.application_id WHERE r.id=600001"))
        self.assertEqual('600001\t600002',self.sql('SELECT resource_id,id FROM iam_action WHERE id=600002'))
        self.assertEqual('/security/credential-policies\tsecurity.credential.policies\tmdi:custom\t1\t1',self.sql('SELECT path,view_path,icon,hidden,is_cache FROM iam_menu WHERE id=600003'))
        self.assertEqual('OPEN\tALL',self.sql('SELECT access_mode,match_mode FROM iam_menu WHERE id=600003'))
        self.assertEqual(16,self.count('iam_action',"code REGEXP '^iam-platform:(credential-policy|lockout-policy|login-failure-policy|session|session-policy|security-policy):'"))
        snapshot=self.sql('SELECT * FROM iam_menu ORDER BY id')
        resources=self.sql('SELECT * FROM iam_resource ORDER BY id')
        self.patch()
        self.assertEqual(snapshot,self.sql('SELECT * FROM iam_menu ORDER BY id'))
        self.assertEqual(resources,self.sql('SELECT * FROM iam_resource ORDER BY id'))
        self.assertEqual('1',self.sql('SELECT @@FOREIGN_KEY_CHECKS'))
        self.assertEqual('0',self.sql('SELECT enabled FROM iam_menu WHERE id=600010'))

    def test_duplicate_resource_rolls_back(self):
        self.legacy()
        self.sql("INSERT INTO iam_application(id,code,domain,name) VALUES(800001,'platform:security','PLATFORM','既有安全'); INSERT INTO iam_resource(id,application_id,code,name,scope_capabilities,field_capabilities) VALUES(800002,800001,'credential-policy','重复',CAST('[\"ALL\"]' AS JSON),CAST('[]' AS JSON));")
        before=self.sql('SELECT * FROM iam_menu ORDER BY id')
        self.patch(error=1644)
        self.assertEqual(before,self.sql('SELECT * FROM iam_menu ORDER BY id'))
        self.assertEqual('600001',self.sql('SELECT resource_id FROM iam_action WHERE id=600002'))

    def test_route_conflict_aborts_before_new_application(self):
        self.legacy()
        self.sql("INSERT INTO iam_menu(id,application_id,name,path,route_name,kind) SELECT 800001,id,'自定义页','/security/credential-policies','custom.route','PAGE' FROM iam_application WHERE code='iam-platform'")
        self.patch(error=1644)
        self.assertEqual(0,self.count('iam_application',"code='platform:security'"))

    def test_empty_catalog_and_occupied_reserved_id(self):
        self.sql("INSERT INTO iam_application(id,code,domain,name) VALUES(100004,'custom','PLATFORM','占用');")
        self.patch()
        self.assertEqual(154,self.count('iam_action'))
        self.assertEqual(0,self.count('iam_role_revision'))
        self.patch()
        self.assertEqual(154,self.count('iam_action'))

    def test_dry_run_restores_catalog_and_does_not_grant(self):
        self.legacy()
        before=self.sql('SELECT * FROM iam_menu ORDER BY id')
        grants=self.sql('SELECT * FROM iam_role_grant ORDER BY revision_id,action_id')
        patch=(SCHEMA_DIRECTORY / 'migrations/020_platform_capability_completion.sql').read_text()
        self.sql('SET @iam_capability_dry_run=TRUE;\n'+patch)
        self.assertEqual(before,self.sql('SELECT * FROM iam_menu ORDER BY id'))
        self.assertEqual(grants,self.sql('SELECT * FROM iam_role_grant ORDER BY revision_id,action_id'))
        self.assertEqual(0,self.count('iam_application',"code='platform:security'"))

    def test_mixed_custom_menu_fails_without_losing_links(self):
        self.legacy()
        self.sql("INSERT INTO iam_menu(id,application_id,name,route_name,kind) SELECT 700001,id,'人工入口','custom.security','PAGE' FROM iam_application WHERE code='iam-platform'; INSERT INTO iam_menu_action SELECT application_id,700001,600002 FROM iam_resource WHERE id=600001")
        links=self.sql('SELECT * FROM iam_menu_action ORDER BY menu_id,action_id')
        self.patch(error=1644)
        self.assertEqual(links,self.sql('SELECT * FROM iam_menu_action ORDER BY menu_id,action_id'))

    def test_old_directory_with_custom_child_is_preserved(self):
        self.legacy()
        self.sql("INSERT INTO iam_menu(id,application_id,parent_id,name,path,route_name,kind) SELECT 700001,id,600010,'人工运维','/custom/operations','custom.operations','PAGE' FROM iam_application WHERE code='iam-platform'")
        original=self.sql('SELECT * FROM iam_menu WHERE id=700001')
        self.patch()
        self.assertEqual('1',self.sql('SELECT enabled FROM iam_menu WHERE id=600010'))
        self.assertEqual(original,self.sql('SELECT * FROM iam_menu WHERE id=700001'))

    def test_new_directory_path_conflict_rolls_back(self):
        self.legacy()
        self.sql("INSERT INTO iam_menu(id,application_id,name,path,route_name,kind) SELECT 700001,id,'人工目录','/security','custom.security','DIRECTORY' FROM iam_application WHERE code='iam-platform'")
        before=self.sql('SELECT * FROM iam_menu ORDER BY id')
        self.patch(error=1644)
        self.assertEqual(before,self.sql('SELECT * FROM iam_menu ORDER BY id'))

    def test_selected_other_page_cannot_silently_drop_security_link(self):
        self.legacy()
        self.sql("INSERT INTO iam_menu_action SELECT application_id,id,600002 FROM iam_menu WHERE view_path='platform.config.dict'")
        original=self.sql('SELECT * FROM iam_menu_action ORDER BY menu_id,action_id')
        self.patch(error=1644)
        self.assertEqual(original,self.sql('SELECT * FROM iam_menu_action ORDER BY menu_id,action_id'))

    def test_read_any_changes_only_multi_capability_pages(self):
        self.bootstrap()
        self.sql("UPDATE iam_menu SET match_mode='ALL' WHERE view_path IN ('security.access.protection','security.sessions','org.iam.member.policies')")
        self.patch()
        self.assertEqual('3',self.sql("SELECT COUNT(*) FROM iam_menu WHERE view_path IN ('security.access.protection','security.sessions','org.iam.member.policies') AND match_mode='ANY'"))


if __name__=='__main__':
    unittest.main()
