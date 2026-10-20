#!/usr/bin/env python3
"""027/028已有库升级：默认只读，已知结构才可执行；不连接默认库。"""
import argparse
import json
import re
import subprocess
from pathlib import Path
from database_sources import ROOT
from schema_comments import BASELINE, SNAPSHOT, normalized, read_definitions, source_digest
from schema_model import closing, parts

RECIPIENT = 'iam_delegation_recipient_member'
PASSWORD = 'password_history'
ALLOCATOR = 'biz_leaf_alloc'
LONG_MAX = 9223372036854775807
MIGRATIONS = ROOT / 'databases/iam/migrations'
PRIMARY_SCRIPT = '027_delegation_recipient_member_primary_key'
INDEX_SCRIPT = '028_password_history_user_index'


def intermediate_definition(ddl):
    start = ddl.index('('); end = closing(ddl, start)
    definitions = parts(ddl[start+1:end])
    definitions = [d for d in definitions if not d.startswith('PRIMARY KEY')]
    definitions = [re.sub(r'(`id` bigint unsigned) NOT NULL', r'\1 DEFAULT NULL', d) for d in definitions]
    return normalized(ddl[:start+1] + ','.join(definitions) + ddl[end:])


def preflight(query, reserving=True):
    """核对两业务表及发号器，不执行DDL或预留ID。"""
    old = json.loads(BASELINE.read_text())['tables']
    latest = json.loads(SNAPSHOT.read_text())
    if latest['source_digest'] != source_digest():
        raise ValueError('最新结构快照过期，未执行任何DDL')
    expected = latest['tables']
    actual = read_definitions(query, [RECIPIENT, PASSWORD, ALLOCATOR])
    choices = {RECIPIENT: {'old': normalized(old[RECIPIENT]), 'nullable': intermediate_definition(expected[RECIPIENT]),
                            'new': normalized(expected[RECIPIENT])},
               PASSWORD: {'old': normalized(old[PASSWORD]), 'new': normalized(expected[PASSWORD])},
               ALLOCATOR: {'same': normalized(expected[ALLOCATOR])}}
    state = {}
    for name, variants in choices.items():
        matches = [stage for stage, ddl in variants.items() if normalized(actual.get(name, '')) == ddl]
        if not matches: raise ValueError(name + ': 未知结构或约束漂移，未执行任何DDL')
        state[name] = matches[-1]
    incoming = query("SELECT COUNT(*) FROM information_schema.key_column_usage WHERE referenced_table_schema=DATABASE() "
                     "AND referenced_table_name='iam_delegation_recipient_member' AND referenced_column_name='id'").strip()
    if int(incoming): raise ValueError('新增id已有入向外键引用，未执行任何DDL')
    allocated = query("SELECT max_id,step FROM biz_leaf_alloc WHERE biz_tag='iam'").strip().split('\t')
    if len(allocated) != 2 or not (0 < int(allocated[0]) <= LONG_MAX) or int(allocated[1]) <= 0:
        raise ValueError('iam Leaf标签缺失或高水位/步长非法，未执行任何DDL')
    high = int(allocated[0])
    if state[RECIPIENT] == 'old':
        pending = int(query('SELECT COUNT(*) FROM ' + RECIPIENT).strip())
    else:
        pending, count, unique, smallest, largest = map(int, query(
            f'SELECT SUM(id IS NULL),COUNT(id),COUNT(DISTINCT id),COALESCE(MIN(id),1),COALESCE(MAX(id),0) FROM {RECIPIENT}').strip().replace('NULL','0').split('\t'))
        if count != unique or smallest <= 0 or largest >= high:
            raise ValueError('已填ID重复/非正/超出Leaf预留范围，未执行任何DDL')
    if reserving and pending > LONG_MAX-high: raise ValueError('Leaf号段不足，未执行任何DDL')
    return state


def upgrade(query, apply=False, rollback=False):
    """先核对全部受影响结构，再逐步执行；中断后可按已知状态重跑。"""
    state = preflight(query, reserving=not rollback)
    if rollback:
        if state[PASSWORD] == 'new': query((MIGRATIONS / (INDEX_SCRIPT + '.rollback.sql')).read_text())
        if state[RECIPIENT] == 'new': query((MIGRATIONS / (PRIMARY_SCRIPT + '.rollback.sql')).read_text())
        elif state[RECIPIENT] == 'nullable': query(f'ALTER TABLE {RECIPIENT} DROP COLUMN id')
    elif apply:
        if state[RECIPIENT] != 'new': query((MIGRATIONS / (PRIMARY_SCRIPT + '.sql')).read_text())
        if state[PASSWORD] != 'new': query((MIGRATIONS / (INDEX_SCRIPT + '.sql')).read_text())
    return state


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--database', required=True)
    parser.add_argument('--defaults-file', required=True, type=Path)
    action = parser.add_mutually_exclusive_group()
    action.add_argument('--apply', action='store_true')
    action.add_argument('--rollback', action='store_true')
    parser.add_argument('--writers-stopped', action='store_true', help='确认全部写入节点已停止且已备份')
    args = parser.parse_args()
    if not re.fullmatch(r'[A-Za-z0-9_]+', args.database): parser.error('数据库名仅允许字母数字下划线')
    if (args.apply or args.rollback) and not args.writers_stopped: parser.error('执行DDL必须同时确认 --writers-stopped')
    command = ['mysql', '--defaults-extra-file='+str(args.defaults_file.resolve()), '--skip-force', '--skip-reconnect',
               '--batch', '--raw', '--skip-column-names', '--default-character-set=utf8mb4', '--database='+args.database]
    def query(sql):
        return subprocess.run(command, input=sql, text=True, capture_output=True, check=True).stdout
    state = upgrade(query, args.apply, args.rollback)
    if args.apply or args.rollback:
        after = preflight(query, reserving=not args.rollback)
        target = 'old' if args.rollback else 'new'
        if after[RECIPIENT] != target or after[PASSWORD] != target: raise ValueError('迁移后结构核对失败，保持停写')
        print('Completed:', after)
    else: print('Read-only preflight passed:', state)


if __name__ == '__main__':
    main()
