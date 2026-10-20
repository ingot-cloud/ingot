#!/usr/bin/env python3
"""用无网络临时MySQL对照密码历史索引三态；不接受实际库连接。"""
import argparse
import hashlib
import json
import os
import statistics
import time
from pathlib import Path
from isolated_mysql import IsolatedMysql
from schema_comments import BASELINE


def verify(mysql):
    results = []
    for users, history, hot_history in [(500, 5, 5), (10000, 10, 10), (1000, 5, 1000)]:
        database = 'password_index_check'
        mysql.sql('DROP DATABASE IF EXISTS ' + database + '; CREATE DATABASE ' + database)
        sql = lambda statement: mysql.sql(statement, database)
        sql(json.loads(BASELINE.read_text())['tables']['password_history'])
        digits = ' UNION ALL '.join('SELECT ' + str(i) + ' AS d' for i in range(10))
        numbers = 'SELECT ' + '+'.join(f'd{i}.d*{10**i}' for i in range(5)) + ' AS n FROM ' + ' CROSS JOIN '.join(f'({digits}) d{i}' for i in range(5))
        sql(f"INSERT INTO password_history(user_id,password_hash,sequence_number,version,created_at,updated_at) "
            f"SELECT FLOOR(n/{history})+1,'synthetic-hash',MOD(n,{history})+1,1,"
            f"TIMESTAMPADD(SECOND,MOD(n,{history}),'2026-01-01'), '2026-01-01' FROM ({numbers}) seq WHERE n<{users*history}")
        if hot_history > history:
            sql(f"INSERT INTO password_history(user_id,password_hash,sequence_number,version,created_at,updated_at) "
                f"SELECT 1,'synthetic-hash',n+1,1,TIMESTAMPADD(SECOND,n,'2026-01-01'),'2026-01-01' "
                f"FROM ({numbers}) seq WHERE n>={history} AND n<{hot_history}")
        queries = {
            'all': 'SELECT * FROM password_history WHERE user_id=1',
            'recent': 'SELECT * FROM password_history WHERE user_id=1 ORDER BY created_at DESC LIMIT 5',
            'ordinary': f'SELECT * FROM password_history WHERE user_id={users} ORDER BY created_at DESC LIMIT 5',
            'missing': f'SELECT * FROM password_history WHERE user_id={users+1}',
        }
        baseline = {}
        for stage in ['visible', 'invisible', 'removed']:
            if stage == 'invisible': sql('ALTER TABLE password_history ALTER INDEX idx_user_id INVISIBLE')
            if stage == 'removed': sql('ALTER TABLE password_history DROP INDEX idx_user_id')
            sql('ANALYZE TABLE password_history')
            evidence = {}
            for name, query in queries.items():
                rows = sql(query).splitlines()
                # 无ORDER BY的查询按集合比较，避免把不同索引顺序当作契约。
                digest = hashlib.sha256('\n'.join(sorted(rows)).encode()).hexdigest()
                if stage == 'visible': baseline[name] = digest
                assert baseline[name] == digest, (stage, name, 'results changed')
                plan = json.loads(sql('EXPLAIN FORMAT=JSON ' + query))
                plan_text = json.dumps(plan)
                assert '"access_type": "ALL"' not in plan_text, (stage, name, plan)
                samples = []
                sql(query)  # warm-up
                for _ in range(5):
                    start = time.perf_counter()
                    sql(';'.join([query] * 5))
                    samples.append(round((time.perf_counter()-start)*1000/5, 3))
                evidence[name] = {'plan': plan, 'analyze': sql('EXPLAIN ANALYZE ' + query),
                                  'median_ms': statistics.median(samples), 'samples_ms': samples, 'result_sha256': digest}
            before = sql('SELECT COUNT(*),SUM(version) FROM password_history')
            mutation = ("START TRANSACTION; UPDATE password_history SET version=version+1 WHERE user_id=1 AND sequence_number=1;"
                        f"INSERT INTO password_history(user_id,password_hash,sequence_number,version) VALUES({users+1},'synthetic-hash',1,1);"
                        f"DELETE FROM password_history WHERE user_id={users};ROLLBACK;")
            write_samples = []
            for _ in range(5):
                start = time.perf_counter(); sql(mutation)
                write_samples.append(round((time.perf_counter()-start)*1000, 3))
            assert before == sql('SELECT COUNT(*),SUM(version) FROM password_history')
            pages = sql("SELECT index_name,stat_value FROM mysql.innodb_index_stats WHERE database_name=DATABASE() "
                        "AND table_name='password_history' AND stat_name='size' ORDER BY index_name")
            results.append({'users': users, 'history': history, 'hot_history': hot_history, 'stage': stage,
                            'queries': evidence, 'write_samples_ms': write_samples, 'index_pages': pages})
    return {'image': mysql.image, 'scope': 'synthetic isolated MySQL; timings include local client overhead', 'results': results}


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--output', type=Path)
    args = parser.parse_args()
    with IsolatedMysql(os.environ.get('IAM_SCHEMA_MYSQL_IMAGE', 'mysql:8.4')) as mysql:
        result = verify(mysql)
    if args.output: args.output.write_text(json.dumps(result, ensure_ascii=False, indent=2) + '\n')
    for row in result['results']:
        print(row['users'], row['history'], row['hot_history'], row['stage'],
              {k: v['median_ms'] for k, v in row['queries'].items()}, row['index_pages'].strip())
