"""版本固定的旧迁移验证使用旧表标识；不改变线上SQL或历史工件。"""
import json
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]


def historical_names(text):
    """将当前DDL中的新表标识还原为历史迁移发布时的名称。"""
    renames = json.loads((ROOT / 'databases/framework-tables.json').read_text())['tableRenames']
    reverse = {new: old for old, new in renames.items()}
    pattern = re.compile(r'(?<![A-Za-z0-9_])(' + '|'.join(map(re.escape, reverse)) + r')(?![A-Za-z0-9_])')
    return pattern.sub(lambda m: reverse[m.group()], text)


def historical_snapshot(snapshot):
    """返回用于固定旧迁移目标的结构投影，不修改最新快照文件。"""
    return {**snapshot, 'tables': {historical_names(name): historical_names(ddl)
                                for name, ddl in snapshot['tables'].items()}}


def restore_historical_tables(query):
    """在隔离历史测试库中改回旧表名，禁止用于当前业务初始化。"""
    renames = json.loads((ROOT / 'databases/framework-tables.json').read_text())['tableRenames']
    query('RENAME TABLE ' + ','.join(f'`{new}` TO `{old}`' for old, new in renames.items()))
