#!/usr/bin/env python3
"""从无网络隔离 MySQL 生成预检结构快照；不接受已有库连接。"""
import json
from database_sources import sources, read_source
from schema_model import tables
from schema_comments import SNAPSHOT, source_digest, normalized, read_definitions
from isolated_mysql import IsolatedMysql


def main():
    with IsolatedMysql() as mysql:
        database = 'iam_schema_reference'
        mysql.sql('CREATE DATABASE ' + database)
        for source in sources('schemas') + sources('frameworkSchemas'):
            mysql.sql(read_source(source), database)
        definitions = {name: normalized(ddl) for name, ddl in read_definitions(
            lambda sql: mysql.sql(sql,database), [t.name for t in tables()]).items()}
        SNAPSHOT.write_text(json.dumps({'source_digest': source_digest(), 'reference': 'mysql:8.4', 'tables': definitions},
                                       ensure_ascii=False, indent=2) + '\n')
        print(f'Refreshed {len(definitions)} table structures from isolated MySQL')


if __name__ == '__main__':
    main()
