"""IAM 建库的唯一清单及框架DDL投影，不执行数据库命令。"""

import json
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
MANIFEST = ROOT / 'databases/iam/manifest.json'
DATABASE_NAME = 'ingot_iam'
CREATE_TABLE = re.compile(r'^CREATE TABLE\s+(?:IF NOT EXISTS\s+)?`?([a-z_]+)', re.MULTILINE)
FRAMEWORK_CREATE = re.compile(
    r'^CREATE TABLE\s+(?:IF NOT EXISTS\s+)?[^\n]+\([\s\S]*?^\)\s*ENGINE[^;]*;',
    re.MULTILINE,
)


def sources(kind):
    """按manifest顺序返回路径；生成目标可以不存在，源文件必须存在。"""
    definition = json.loads(MANIFEST.read_text())
    value = definition[kind]
    names = value if isinstance(value, list) else [value]
    paths = [(ROOT / name).resolve() for name in names]
    if len(paths) != len(set(paths)):
        raise ValueError('重复建库来源: ' + kind)
    for path in paths:
        if not path.is_relative_to(ROOT) or (kind != 'bundle' and not path.is_file()):
            raise ValueError('非法或缺失的建库来源: ' + str(path))
    return paths


def schema_files():
    """只返回IAM权威结构，不包含种子或历史ALTER补丁。"""
    return sources('schemas')


def legacy_schema_files():
    """显式选择的历史元数据/记录结构，不进入首次部署的建表清单。"""
    return sources('legacySchemas')


def initialization_files(*, seed_platform_account=True):
    """结构、框架表、治理目录及平台账号的完整顺序。"""
    catalog = schema_files() + sources('frameworkSchemas') + sources('bootstrap')
    return catalog + sources('initialAccounts') if seed_platform_account else catalog


def read_source(path):
    """框架表只提取CREATE，避免固定USE和可选历史数据示例跨库。"""
    text = path.read_text()
    if path in sources('frameworkSchemas'):
        tables = FRAMEWORK_CREATE.findall(text)
        if not tables:
            raise ValueError('框架源未找到CREATE TABLE: ' + str(path))
        return '\n\n'.join(tables) + '\n'
    return text


def render_bundle():
    """输出自包含的首次部署SQL，重建固定IAM库及必要基础数据。"""
    contents = [(path, read_source(path)) for path in initialization_files()]
    names = set()
    for _, content in contents:
        for table in CREATE_TABLE.findall(content):
            if table in names:
                raise ValueError('重复CREATE TABLE: ' + table)
            names.add(table)
    lines = [
        '-- IAM 首次部署完整初始化：直接执行本文件即可建库、建表并写入默认数据。',
        '-- 每次执行都会删除并重建 ingot_iam 库，该库已有表及数据全部清空。',
        '-- 自动生成：python3 tools/iam/generate_database.py；不要手工编辑。',
        '-- 来源顺序：databases/iam/manifest.json；框架DDL仍由原模块维护。',
        '-- 固定作用于 ingot_iam，不需要预先建库或选择数据库；执行前停止使用该库的服务。',
        '-- 包含应用、资源、权限操作、菜单、角色及关联；不导入测试租户或运行记录。',
        '-- 默认平台账号 platform，初始密码 11111111，首次登录必须修改密码。',
        '-- MySQL 8.0.16+；仅删除阶段关闭外键，建表/种子阶段开启，最后恢复原设置；数据时间按UTC写入。',
        'SET NAMES utf8mb4;',
        "SET SESSION time_zone = '+00:00';",
        '',
        '-- 整库重建同时移除历史迁移表和其他遗留表；不处理其他数据库。',
        'SET @iam_init_previous_foreign_key_checks = @@SESSION.FOREIGN_KEY_CHECKS;',
        'SET SESSION FOREIGN_KEY_CHECKS = 0;',
        f'DROP DATABASE IF EXISTS `{DATABASE_NAME}`;',
        f'CREATE DATABASE `{DATABASE_NAME}` CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;',
        f'USE `{DATABASE_NAME}`;',
        'SET SESSION FOREIGN_KEY_CHECKS = 1;',
        '',
    ]
    for path, content in contents:
        lines.extend(['-- ====================================================================',
                      '-- Source: ' + str(path.relative_to(ROOT)),
                      '-- ====================================================================', content.rstrip(), ''])
    lines.extend(['-- 恢复调用者原有的会话外键设置。',
                  'SET SESSION FOREIGN_KEY_CHECKS = @iam_init_previous_foreign_key_checks;', ''])
    return '\n'.join(lines)
