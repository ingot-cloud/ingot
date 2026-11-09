#!/usr/bin/env python3
"""将library权威DDL同步到消费服务的首次部署脚本；--check只读核对。"""
import argparse
import json
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
CATALOG = ROOT / 'databases/framework-tables.json'
CREATE = re.compile(r'^CREATE TABLE\s+(?:IF NOT EXISTS\s+)?`?([a-z_0-9]+)`?[^\n]*\([\s\S]*?^\)\s*ENGINE[^;]*;', re.M)


def catalog():
    """返回模块与消费服务清单，源路径必须在仓库内且存在。"""
    data = json.loads(CATALOG.read_text())
    for entry in data['modules']:
        path = (ROOT / entry['schema']).resolve()
        if not path.is_relative_to(ROOT) or not path.is_file():
            raise ValueError('Missing framework schema: ' + entry['schema'])
    return data


def definitions():
    """每张共享表只允许一个library维护。"""
    result = {}
    for entry in catalog()['modules']:
        found = {m.group(1): m.group() for m in CREATE.finditer((ROOT / entry['schema']).read_text())}
        if set(found) != set(entry['tables']) or result.keys() & found.keys():
            raise ValueError('Framework schema table ownership mismatch: ' + entry['module'])
        result.update(found)
    return result


def render_service(service, source):
    """仅同步共享表定义和移除共享表运行数据，保留服务自有种子。"""
    data = catalog()
    current = definitions()
    renames = {old: new for old, new in data['tableRenames'].items() if new in current}
    pattern = re.compile(r'(?<![A-Za-z0-9_])(' + '|'.join(map(re.escape, renames)) + r')(?![A-Za-z0-9_])')
    text = pattern.sub(lambda m: renames[m.group()], source)
    required = {name for entry in data['modules'] if service in entry['services'] for name in entry['tables']}
    text = CREATE.sub(lambda m: current[m.group(1)] if m.group(1) in required else m.group(), text)
    # Existing service snapshots use single-line INSERT statements, including all quoted values.
    text = re.sub(r'^INSERT INTO\s+`?(' + '|'.join(map(re.escape, required)) + r')`?\s[^\n]*;[ \t]*\n?', '', text, flags=re.M)
    present = {m.group(1) for m in CREATE.finditer(text)}
    for name in sorted(required - present):
        text += '\n\n-- Shared library schema\n' + current[name] + '\n'
    return text


def build(check=False):
    """生成或核对Member/Security的共享表投影。"""
    for service, relative in catalog()['serviceBundles'].items():
        path = ROOT / relative
        expected = render_service(service, path.read_text())
        if check:
            if path.read_text() != expected:
                raise ValueError(relative + ' shared schema differs; run generate_framework_databases.py')
        else:
            path.write_text(expected)


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--check', action='store_true')
    build(parser.parse_args().check)
