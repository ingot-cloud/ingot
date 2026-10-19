#!/usr/bin/env python3
"""生成020离线目录迁移；业务更新全部在事务内，绝不改固定角色版本。"""
import re
import hashlib
import json
from generate_bootstrap import ROOT, TARGET, PAGE_PATHS, SECURITY_RESOURCES, quote, collect, ROUTES

MIGRATION = ROOT / 'databases/iam/migrations/020_platform_capability_completion.sql'
MAPPINGS = [
 ('iam-platform','platform.config.dict','platform.settings.dictionaries','platform.settings','字典管理'),
 ('iam-platform','security.iam.authorization.audit','platform.iam.authorization.audit','platform.iam.manage','平台授权审计'),
 ('iam-tenant','security.iam.authorization.audit','org.iam.authorization.audit','org.iam.compliance','组织授权审计'),
 ('iam-tenant','security.iam.member.permissions','org.iam.member.policies','org.iam.compliance','成员访问策略'),
 ('platform:security','security.credential','security.credential.policies','security.root','凭证策略'),
 ('platform:security','security.account.protection','security.account.protection','security.root','账号保护'),
 ('platform:security','security.access.protection','security.access.protection','security.root','访问防护'),
 ('platform:security','security.sessions','security.sessions','security.root','会话管理'),
]


def build(check=False):
    # 已发布020冻结；当前角色迁移由021及tenant_live工具负责，不能用新契约重写旧升级历史。
    seed = TARGET.read_text()
    if 'iam-tenant:role:update' in seed:
        expected=(ROOT/'tools/iam/tenant_live/legacy-capability-migration.sha256').read_text().strip()
        if hashlib.sha256(MIGRATION.read_bytes()).hexdigest()!=expected:
            raise SystemExit('历史020迁移摘要不一致，禁止自动覆盖')
        return
    # 保留完整语句，初始化中的角色、策略、发号器不属于增量目录升级。
    statements = re.findall(r'INSERT INTO (?:iam_application|iam_resource|iam_action|iam_menu|iam_menu_action) .*?;\n', seed, re.S)
    applications = [s for s in statements if s.startswith('INSERT INTO iam_application ')]
    directories = [s for s in statements if s.startswith('INSERT INTO iam_menu ') and "'DIRECTORY'" in s]
    rest = [s for s in statements if s not in applications and s not in directories]
    rows = []
    for app, old, new, parent, name in MAPPINGS:
        force_any = new in {'security.access.protection','security.sessions','org.iam.member.policies'}
        rows.append('('+', '.join(quote(v) for v in (app,old,new,parent,PAGE_PATHS[new],name))+', '+('TRUE' if force_any else 'FALSE')+')')
    resources = ','.join(quote(v) for v in sorted(SECURITY_RESOURCES))
    action_codes = sorted(code for codes in collect(json.loads(ROUTES.read_text()))['platform:security'].values() for code in codes)
    known_actions = ','.join(quote(code) for code in action_codes)
    sql = '''-- 生成来源 tools/iam/generate_capability_migration.py；停止目标库所有写入并完整备份后执行。
-- 不使用 --force；任一冲突 SIGNAL 中止，业务更新整笔回滚；不关闭外键。
SET time_zone = '+00:00';
DROP PROCEDURE IF EXISTS iam_complete_platform_capabilities;
DELIMITER $$
CREATE PROCEDURE iam_complete_platform_capabilities()
BEGIN
  DECLARE source_app BIGINT UNSIGNED;
  DECLARE security_app BIGINT UNSIGNED;
  DECLARE bridge_base BIGINT UNSIGNED;
  DECLARE EXIT HANDLER FOR SQLEXCEPTION
  BEGIN
    ROLLBACK;
    DROP TEMPORARY TABLE IF EXISTS capability_resource_moves,capability_links,capability_selected,capability_page_map;
    RESIGNAL;
  END;
  DROP TEMPORARY TABLE IF EXISTS capability_resource_moves,capability_links,capability_selected,capability_page_map;
  START TRANSACTION;
  CREATE TEMPORARY TABLE capability_page_map (
    application_code VARCHAR(64) COLLATE utf8mb4_bin, old_key VARCHAR(256) COLLATE utf8mb4_bin,
    new_key VARCHAR(256) COLLATE utf8mb4_bin, parent_key VARCHAR(128) COLLATE utf8mb4_bin,
    new_path VARCHAR(512), new_name VARCHAR(128), force_any BOOLEAN);
  INSERT INTO capability_page_map VALUES
''' + ',\n'.join(rows) + ';\n'
    sql += '''
  IF EXISTS (SELECT 1 FROM iam_application WHERE code IN ('iam-platform','platform:security') AND domain <> 'PLATFORM')
     OR EXISTS (SELECT 1 FROM iam_application WHERE code='iam-tenant' AND domain <> 'TENANT') THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='应用域冲突';
  END IF;
  CREATE TEMPORARY TABLE capability_selected AS
    SELECT m.id, m.path AS old_path, p.*, a.code AS old_application FROM capability_page_map p
    JOIN iam_application a ON a.code=p.application_code OR (p.application_code='platform:security' AND a.code='iam-platform')
    JOIN iam_menu m ON m.application_id=a.id AND m.kind='PAGE'
      AND (m.view_path IN (p.old_key,p.new_key) OR m.route_name IN (p.old_key,p.new_key));
  IF EXISTS (SELECT 1 FROM capability_selected GROUP BY application_code,new_key HAVING COUNT(*) > 1) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='同一页面存在重复目录，请人工解决';
  END IF;
  IF EXISTS (SELECT 1 FROM capability_page_map p JOIN iam_menu m ON (m.path=p.new_path OR m.route_name=p.new_key OR m.view_path=p.new_key)
       LEFT JOIN capability_selected s ON s.id=m.id WHERE s.id IS NULL) THEN
    SELECT 'CONFLICT_PAGE' AS kind,m.id,m.path,m.route_name,m.view_path,p.new_key FROM capability_page_map p
      JOIN iam_menu m ON (m.path=p.new_path OR m.route_name=p.new_key OR m.view_path=p.new_key)
      LEFT JOIN capability_selected s ON s.id=m.id WHERE s.id IS NULL;
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='目标页面路径或注册键被占用';
  END IF;
  IF EXISTS (SELECT 1 FROM iam_menu c JOIN capability_selected s ON c.parent_id=s.id) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='目标页面包含自定义子项，不能自动迁移';
  END IF;
  IF EXISTS (SELECT 1 FROM capability_page_map p JOIN iam_application app ON app.code=p.application_code
      JOIN iam_menu m ON m.application_id=app.id AND m.route_name=p.parent_key
      GROUP BY app.id,p.parent_key HAVING COUNT(DISTINCT m.id)>1) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='目标父目录重复';
  END IF;
  IF EXISTS (SELECT 1 FROM capability_page_map p JOIN iam_application app ON app.code=p.application_code
      JOIN iam_menu m ON m.application_id=app.id AND m.route_name=p.parent_key WHERE m.kind<>'DIRECTORY') THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='目标父目录冲突';
  END IF;
  IF EXISTS (SELECT 1 FROM iam_menu m JOIN capability_page_map p ON m.view_path IN (p.old_key,p.new_key) OR m.route_name IN (p.old_key,p.new_key)
      JOIN iam_application app ON app.id=m.application_id AND (app.code=p.application_code OR (p.application_code='platform:security' AND app.code='iam-platform'))
      WHERE m.kind<>'PAGE') THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='页面类型冲突';
  END IF;
  SELECT 'PAGE' AS kind, p.application_code, s.old_application, s.id, p.old_key, p.new_key, p.new_path,
    IF(s.id IS NULL,'CREATE','MIGRATE_OR_KEEP') AS operation
    FROM capability_page_map p LEFT JOIN capability_selected s ON s.application_code=p.application_code AND s.new_key=p.new_key ORDER BY p.application_code,p.new_key;
'''
    sql += f'''
  IF EXISTS (SELECT 1 FROM iam_menu m JOIN iam_application app ON app.id=m.application_id
      WHERE (m.path='/security' AND NOT (app.code='platform:security' AND (m.route_name <=> 'security.root') AND m.kind='DIRECTORY'))
         OR (m.path='/platform/settings' AND NOT (app.code='iam-platform' AND (m.route_name <=> 'platform.settings') AND m.kind='DIRECTORY'))) THEN
    SELECT 'CONFLICT_DIRECTORY' AS kind,m.id,app.code,m.path,m.route_name FROM iam_menu m JOIN iam_application app ON app.id=m.application_id
      WHERE m.path IN ('/security','/platform/settings');
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='新目录路径被占用';
  END IF;
  SELECT 'APPLICATION' AS kind,'platform:security' AS code,id,IF(id IS NULL,'CREATE','KEEP') AS operation
    FROM (SELECT (SELECT id FROM iam_application WHERE code='platform:security') AS id) target;
  SELECT 'RESOURCE' AS kind,c.code,old.id AS source_id,target.id AS target_id,
    CASE WHEN old.id IS NOT NULL AND target.id IS NOT NULL THEN 'CONFLICT'
         WHEN old.id IS NOT NULL THEN 'MIGRATE' WHEN target.id IS NOT NULL THEN 'KEEP' ELSE 'CREATE' END AS operation
    FROM JSON_TABLE({quote(json.dumps(sorted(SECURITY_RESOURCES)))},'$[*]' COLUMNS(code VARCHAR(64) PATH '$')) c
    LEFT JOIN iam_resource old ON old.code=c.code AND old.application_id=(SELECT id FROM iam_application WHERE code='iam-platform')
    LEFT JOIN iam_resource target ON target.code=c.code AND target.application_id=(SELECT id FROM iam_application WHERE code='platform:security');
  SELECT 'ACTION' AS kind,c.code,a.id,app.code AS source_application,
    CASE WHEN a.id IS NULL THEN 'CREATE' WHEN app.code='platform:security' THEN 'KEEP' ELSE 'MIGRATE' END AS operation
    FROM JSON_TABLE({quote(json.dumps(action_codes))},'$[*]' COLUMNS(code VARCHAR(128) PATH '$')) c
    LEFT JOIN iam_action a ON a.code=c.code LEFT JOIN iam_application app ON app.id=a.application_id;
'''
    sql += '\n'.join(applications)
    sql += '''
  SET source_app=(SELECT id FROM iam_application WHERE code='iam-platform');
  SET security_app=(SELECT id FROM iam_application WHERE code='platform:security');
'''
    sql += f'''
  IF EXISTS (SELECT 1 FROM iam_resource old JOIN iam_resource target ON target.application_id=security_app AND target.code=old.code
      WHERE old.application_id=source_app AND old.code IN ({resources})) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='安全资源同时存在于两个应用，禁止合并 ID';
  END IF;
  IF EXISTS (SELECT 1 FROM iam_resource WHERE code IN ({resources}) AND application_id IN (source_app,security_app)
      AND (scope_capabilities <> CAST('["ALL"]' AS JSON) OR JSON_LENGTH(field_capabilities) <> 0)) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='既有安全资源能力与 ALL 契约冲突';
  END IF;
  IF EXISTS (SELECT 1 FROM iam_action a JOIN iam_resource r ON r.id=a.resource_id
      WHERE a.code REGEXP '^iam-platform:(credential-policy|lockout-policy|login-failure-policy|session|session-policy|security-policy):'
      AND (a.application_id NOT IN (source_app,security_app) OR r.code <> SUBSTRING_INDEX(SUBSTRING_INDEX(a.code,':',2),':',-1))) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='精确安全操作的资源关联冲突';
  END IF;
  IF EXISTS (SELECT 1 FROM iam_action a JOIN iam_resource r ON r.id=a.resource_id
      WHERE r.code IN ({resources}) AND r.application_id IN (source_app,security_app)
      AND a.code NOT IN ({known_actions})) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='安全资源存在未知操作，需人工确认';
  END IF;
'''
    sql += '''
  IF EXISTS (SELECT 1 FROM iam_menu_action ma JOIN iam_action a ON a.id=ma.action_id
      JOIN iam_menu m ON m.id=ma.menu_id LEFT JOIN capability_selected s ON s.id=m.id
      WHERE a.application_id=source_app AND a.code REGEXP '^iam-platform:(credential-policy|lockout-policy|login-failure-policy|session|session-policy|security-policy):'
      AND (s.id IS NULL OR s.application_code<>'platform:security') AND NOT (m.kind='DIRECTORY' AND m.route_name='platform.iam.security')) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='安全操作被自定义菜单引用，需人工拆分';
  END IF;
  IF EXISTS (SELECT 1 FROM iam_menu old JOIN iam_menu_action ma ON ma.menu_id=old.id
      JOIN iam_action a ON a.id=ma.action_id JOIN iam_menu child ON child.parent_id=old.id
      LEFT JOIN capability_selected s ON s.id=child.id
      WHERE old.application_id=source_app AND old.route_name='platform.iam.security' AND old.kind='DIRECTORY'
        AND a.application_id=source_app AND a.code REGEXP '^iam-platform:(credential-policy|lockout-policy|login-failure-policy|session|session-policy|security-policy):'
        AND s.id IS NULL) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='保留的旧目录包含安全操作关联，需人工拆分';
  END IF;
  IF EXISTS (SELECT 1 FROM iam_menu_action ma JOIN capability_selected s ON s.id=ma.menu_id
      JOIN iam_action a ON a.id=ma.action_id JOIN iam_application app ON app.code=s.application_code
      WHERE a.application_id<>app.id AND NOT (s.application_code='platform:security' AND a.application_id=source_app
        AND a.code REGEXP '^iam-platform:(credential-policy|lockout-policy|login-failure-policy|session|session-policy|security-policy):')) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='目标菜单包含跨应用操作';
  END IF;
  CREATE TEMPORARY TABLE capability_links AS SELECT ma.menu_id,ma.action_id FROM iam_menu_action ma
    JOIN capability_selected s ON s.id=ma.menu_id;
  DELETE ma FROM iam_menu_action ma JOIN capability_selected s ON s.id=ma.menu_id;
  DELETE ma FROM iam_menu_action ma JOIN iam_action a ON a.id=ma.action_id
    WHERE a.application_id=source_app AND a.code REGEXP '^iam-platform:(credential-policy|lockout-policy|login-failure-policy|session|session-policy|security-policy):';
  SET bridge_base=(SELECT COALESCE(MAX(id),190000) FROM iam_resource);
  CREATE TEMPORARY TABLE capability_resource_moves AS
    SELECT id,code,bridge_base+ROW_NUMBER() OVER (ORDER BY id) AS bridge_id FROM iam_resource
''' + f'    WHERE application_id=source_app AND code IN ({resources});\n'
    sql += '''
  SELECT 'RESOURCE' AS kind,id,code FROM capability_resource_moves;
  INSERT INTO iam_resource (id,application_id,code,name,scope_capabilities,field_capabilities)
    SELECT bridge_id,source_app,CONCAT('__security_bridge_',id),'事务内外键桥接',CAST('["ALL"]' AS JSON),CAST('[]' AS JSON)
      FROM capability_resource_moves;
  UPDATE iam_action a JOIN capability_resource_moves r ON a.resource_id=r.id SET a.resource_id=r.bridge_id;
  UPDATE iam_resource r JOIN capability_resource_moves moving ON moving.id=r.id SET r.application_id=security_app,r.version=r.version+1;
  UPDATE iam_action a JOIN capability_resource_moves r ON a.resource_id=r.bridge_id
    SET a.application_id=security_app,a.resource_id=r.id,a.version=a.version+1;
  DELETE r FROM iam_resource r JOIN capability_resource_moves moving ON moving.bridge_id=r.id;
'''
    sql += '\n'.join(directories)
    sql += '''
  UPDATE iam_menu m JOIN capability_selected s ON s.id=m.id
    JOIN iam_application app ON app.code=s.application_code
    JOIN iam_menu parent ON parent.application_id=app.id AND parent.route_name=s.parent_key AND parent.kind='DIRECTORY'
    SET m.application_id=app.id,m.parent_id=parent.id,m.path=s.new_path,m.view_path=s.new_key,m.route_name=s.new_key,m.name=s.new_name,m.match_mode=IF(s.force_any,'ANY',m.match_mode),m.version=m.version+1
    WHERE m.application_id<>app.id OR NOT (m.parent_id <=> parent.id) OR NOT (m.path <=> s.new_path)
      OR NOT (m.view_path <=> s.new_key) OR NOT (m.route_name <=> s.new_key) OR m.name<>s.new_name OR (s.force_any AND m.match_mode<>'ANY');
  INSERT INTO iam_menu_action(application_id,menu_id,action_id)
    SELECT m.application_id,m.id,a.id FROM capability_links l JOIN iam_menu m ON m.id=l.menu_id JOIN iam_action a ON a.id=l.action_id
      WHERE m.application_id=a.application_id;
'''
    sql += '\n'.join(rest)
    sql += '''
  UPDATE iam_menu old SET old.enabled=FALSE,old.version=old.version+1
    WHERE old.application_id=source_app AND old.route_name='platform.iam.security' AND old.kind='DIRECTORY' AND old.enabled=TRUE
      AND NOT EXISTS (SELECT 1 FROM (SELECT parent_id FROM iam_menu) child WHERE child.parent_id=old.id);
  DROP TEMPORARY TABLE capability_resource_moves;
  DROP TEMPORARY TABLE capability_links;
  DROP TEMPORARY TABLE capability_selected;
  DROP TEMPORARY TABLE capability_page_map;
  IF COALESCE(@iam_capability_dry_run,FALSE) THEN ROLLBACK; ELSE COMMIT; END IF;
END$$
DELIMITER ;
CALL iam_complete_platform_capabilities();
DROP PROCEDURE iam_complete_platform_capabilities;
'''
    if check:
        if not MIGRATION.is_file() or MIGRATION.read_text()!=sql:
            raise SystemExit('020 migration differs; run tools/iam/generate_database.py')
    else: MIGRATION.write_text(sql)


if __name__ == '__main__':
    build()
