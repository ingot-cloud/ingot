"""IAM 建库的唯一清单及框架DDL投影，不执行数据库命令。"""

import json
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
MANIFEST = ROOT / 'databases/iam/manifest.json'
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


def initialization_files():
    """结构、框架表、正式种子的完整顺序；框架源需经过read_source投影。"""
    return schema_files() + sources('frameworkSchemas') + sources('bootstrap')


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
    """重建清单内的表，外键仅在删除阶段关闭，不携带开发快照数据。"""
    contents = [(path, read_source(path)) for path in initialization_files()]
    tables = []
    names = set()
    for _, content in contents:
        for table in CREATE_TABLE.findall(content):
            if table in names:
                raise ValueError('重复CREATE TABLE: ' + table)
            names.add(table)
            tables.append(table)
    lines = [
        '-- IAM 完整初始化（可重复执行；每次清空并重建清单内全部表，不是已有库升级脚本）。',
        '-- 自动生成：python3 tools/iam/generate_database.py；不要手工编辑。',
        '-- 来源顺序：databases/iam/manifest.json；框架DDL仍由原模块维护。',
        '-- 在调用者选定的数据库执行；先备份并停止服务，无CREATE/DROP DATABASE或固定USE。',
        '-- 不包含账号、凭证、业务组织、授权记录或登录运行状态。',
        '-- MySQL 8.0.16+；仅删除阶段关闭外键，建表/种子阶段开启，最后恢复原设置；数据时间按UTC写入。',
        'SET NAMES utf8mb4;',
        "SET SESSION time_zone = '+00:00';",
        '',
        '-- 重建：组织与所有者等循环外键要求先删除全部目标表，再开始建表。',
        'SET @iam_init_previous_foreign_key_checks = @@SESSION.FOREIGN_KEY_CHECKS;',
        'SET SESSION FOREIGN_KEY_CHECKS = 0;',
    ]
    lines.extend(f'DROP TABLE IF EXISTS `{table}`;' for table in reversed(tables))
    lines.extend(['SET SESSION FOREIGN_KEY_CHECKS = 1;', ''])
    for path, content in contents:
        lines.extend(['-- ====================================================================',
                      '-- Source: ' + str(path.relative_to(ROOT)),
                      '-- ====================================================================', content.rstrip(), ''])
    lines.extend(['-- 恢复调用者原有的会话外键设置。',
                  'SET SESSION FOREIGN_KEY_CHECKS = @iam_init_previous_foreign_key_checks;', ''])
    return '\n'.join(lines)
