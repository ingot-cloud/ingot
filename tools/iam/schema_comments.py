#!/usr/bin/env python3
"""从权威DDL生成最新字典；026注释升级使用固定历史脚本及基线。"""
import argparse
import hashlib
import json
from pathlib import Path
import re
import subprocess
from database_sources import ROOT, sources, read_source
from schema_model import tables, COMMENT, comment, without_sql_comments, closing, parts

DICTIONARY = ROOT / 'docs/modules/iam/DATA-DICTIONARY.md'
MIGRATION = ROOT / 'databases/iam/migrations/026_schema_comments.sql'
SNAPSHOT = ROOT / 'databases/iam/schema_structure.json'
BASELINE = ROOT / 'databases/iam/migrations/026_schema_comments_baseline.json'


def normalized(ddl):
    """忽略说明与运行时自增高水位，其他结构必须与隔离 MySQL 快照一致。"""
    ddl = COMMENT.sub('', ddl)
    ddl = re.sub(r'\bAUTO_INCREMENT=\d+\s*', '', ddl, flags=re.I)
    # MySQL MODIFY会按ASCII列重写CHECK常量的字符集引导符；仅折叠同编码的ASCII字面量。
    # 不忽略列CHARACTER SET/COLLATE，也不忽略非ASCII文字、表达式或常量值变化。
    ddl = re.sub(r"_(?:utf8mb4|utf8mb3|utf8|ascii)('(?:''|[\x20-\x26\x28-\x7e])*')", r'\1', ddl, flags=re.I)
    if re.match(r'\s*CREATE TABLE\b', ddl, re.I):
        start = ddl.index('(')
        end = closing(ddl, start)
        definitions = parts(ddl[start + 1:end])
        is_constraint = lambda value: re.match(r'(?:PRIMARY KEY|UNIQUE KEY|KEY|CONSTRAINT|FOREIGN KEY|CHECK)\b', value, re.I)
        columns = [x for x in definitions if not is_constraint(x)]
        constraints = sorted(x for x in definitions if is_constraint(x))
        ddl = ddl[:start + 1] + ', '.join(columns + constraints) + ddl[end:]
    tokens = re.split(r"('(?:''|\\.|[^'\\])*'|`(?:``|[^`])*`)", ddl)
    return ''.join(token if index % 2 else re.sub(r'\s+', ' ', token)
                   for index, token in enumerate(tokens)).strip()


def source_digest():
    """防止已变更的权威结构使用过期预检快照。"""
    payload = [(str(path.relative_to(ROOT)), normalized(without_sql_comments(read_source(path))))
               for path in sources('schemas') + sources('frameworkSchemas')]
    return hashlib.sha256(json.dumps(payload, ensure_ascii=False).encode()).hexdigest()


def render_dictionary():
    definitions = tables()
    lines = ['# IAM 数据字典', '', '<!-- 自动生成：python3 tools/iam/generate_database.py；说明在权威 DDL 维护。 -->', '',
             f'manifest 管理 {len(definitions)} 张表、{sum(len(t.columns) for t in definitions)} 个字段。', '',
             '## 阅读约定', '',
             '- 数据库字段定义列保留默认值、可空性及生成表达式；时间点统一 UTC。',
             '- primary_member_id / primary_department_id 为自动约束键，不由接口填写；唯一索引保证至多一个主关系，业务完整性由事务服务校验。',
             '- tenant_key 将空租户转为0，以便平台/共享归属参与唯一索引或复合外键；0不是实际租户。',
             '- 角色 version、权限快照 revision、metadata_version 职责不同；并行生效模型按平台固定版本与租户当前角色分别解释。',
             '- 历史辅助表列名不自动代表当前成员ID；第三方 app_id 与 IAM 应用目录ID按各字段说明区分。',
             '- JSON 的数组/对象形态由列定义及 CHECK 约束，具体求值规则由对应模块负责；空集合不统一解释为全量或无权限。', '',
             '主键与索引评估见 [逐表评估](DATABASE-REVIEW.md)。', '']
    for table in definitions:
        source = str(table.source.relative_to(ROOT))
        lines += [f'## {table.name}', '', comment(table.options), '', f'权威来源：[{source}](../../../{source})', '', '| 字段 | 定义 | 说明 |', '|---|---|---|']
        for name, definition in table.columns.items():
            spec = COMMENT.sub('', definition).replace('\n', ' ').replace('|', '\\|')
            lines.append(f'| `{name}` | ``{spec}`` | {comment(definition).replace("|", "／")} |')
        lines += ['', '约束与索引：', '', '```sql', *table.constraints, '```', '']
    return '\n'.join(lines)


def build(check=False):
    """生成可重现工件；check 模式只读。"""
    for table in tables():
        if not comment(table.options) or any(not comment(c) for c in table.columns.values()):
            raise ValueError('缺少表或字段 COMMENT: ' + table.name)
    # 026是版本固定的历史迁移，不能随最新DDL增加列或修改索引。
    baseline = json.loads(BASELINE.read_text())
    if hashlib.sha256(MIGRATION.read_bytes()).hexdigest() != baseline['migration_sha256']:
        raise ValueError('026历史注释脚本摘要不匹配')
    for path, expected in [(DICTIONARY, render_dictionary())]:
        if check:
            if not path.exists() or path.read_text() != expected:
                raise ValueError(str(path.relative_to(ROOT)) + ' differs; regenerate database artifacts')
        else:
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_text(expected)
    if SNAPSHOT.exists() and json.loads(SNAPSHOT.read_text())['source_digest'] != source_digest():
        raise ValueError('预检结构快照过期；在隔离 MySQL 中重新运行 refresh_schema_snapshot.py')


def read_definitions(query, names):
    """在同一只读客户端批量读取定义，避免为每张表建立连接。"""
    output = query(';\n'.join(f'SHOW CREATE TABLE `{name}`' for name in names) + ';')
    matches = list(re.finditer(r'(?m)^([a-z_]+)\t(CREATE TABLE\s)', output))
    result = {}
    for index, match in enumerate(matches):
        end = matches[index + 1].start() if index + 1 < len(matches) else len(output)
        result[match.group(1)] = output[match.start(2):end].strip()
    return result


def preflight(query, snapshot, check_source=True):
    """只读核对所有目标表；调用方不得在核对完成前执行任何 ALTER。"""
    if check_source and snapshot['source_digest'] != source_digest():
        raise ValueError('权威结构与预检快照不一致')
    if set(snapshot['tables']) != {table.name for table in tables()}:
        raise ValueError('预检快照未覆盖全部 manifest 表')
    try:
        actual = read_definitions(query, snapshot['tables'])
    except (subprocess.CalledProcessError, RuntimeError) as error:
        raise ValueError('注释升级已停止，未执行任何 ALTER：无法读取全部目标表定义（缺表或权限不足）') from error
    failures = [name + ': 缺失定义或非注释结构与当前权威定义不同'
                for name, expected in snapshot['tables'].items()
                if name not in actual or normalized(actual[name]) != normalized(expected)]
    if failures:
        raise ValueError('注释升级已停止，未执行任何 ALTER：\n' + '\n'.join(failures))


def upgrade(query, apply=False):
    """先全量预检；apply 才执行注释DDL。只读检查绝不创建参考库。"""
    build(check=True)
    snapshot = json.loads(BASELINE.read_text())
    preflight(query, snapshot, check_source=False)
    if apply:
        query(MIGRATION.read_text())
    print('Schema comments ' + ('applied' if apply else 'preflight passed (read-only)'))


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--check', action='store_true', help='只读检查生成工件')
    parser.add_argument('--database', help='显式选择目标库')
    parser.add_argument('--defaults-file', type=Path, help='mysql连接配置文件，凭证不放命令行')
    parser.add_argument('--apply', action='store_true', help='全表结构预检通过后执行注释升级')
    args = parser.parse_args()
    if args.database:
        if not re.fullmatch(r'[A-Za-z0-9_]+', args.database) or not args.defaults_file:
            parser.error('数据库名仅允许字母数字下划线，并须指定 --defaults-file')
        if args.check:
            parser.error('--check 与目标库参数互斥')
        command = ['mysql', '--defaults-extra-file=' + str(args.defaults_file.resolve()), '--batch', '--raw',
                   '--skip-column-names', '--default-character-set=utf8mb4', '--database=' + args.database]
        def query(sql):
            return subprocess.run(command, input=sql, text=True, capture_output=True, check=True).stdout
        upgrade(query, args.apply)
    else:
        if args.apply or args.defaults_file:
            parser.error('升级必须显式选择 --database')
        build(args.check)


if __name__ == '__main__':
    main()
