#!/usr/bin/env python3
"""只读导出IAM授权迁移所需数据；账号和成员只导出状态与身份ID，不导出凭证或联系资料。"""
import argparse
import json
import os
import re
import subprocess
from pathlib import Path
from migrate import TABLES, canonical

MINIMAL = {'iam_account': {'id','enabled','deleted_at','version'},
           'iam_tenant_member': {'id','tenant_id','account_id','status','version'},
           'iam_tenant': {'id','owner_member_id','enabled','deleted_at','version'}}


def export(command, database):
    if not re.fullmatch(r'[A-Za-z0-9_]+', database):
        raise ValueError('数据库名必须为显式合法标识')
    def query(sql):
        result=subprocess.run(command+['--default-character-set=utf8mb4','--database='+database,'--batch','--skip-column-names','--raw'],input="SET time_zone='+00:00';\n"+sql,text=True,capture_output=True,check=True)
        return result.stdout.strip()
    source={}
    for table in TABLES:
        columns=query(f"SELECT column_name FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='{table}' ORDER BY ordinal_position;").splitlines()
        if not columns:
            continue
        if table in MINIMAL:
            columns=[c for c in columns if c in MINIMAL[table]]
        if any(not re.fullmatch('[a-z_]+', c) for c in columns):
            raise ValueError('不支持的列名')
        pairs=','.join("'"+c+"',`"+c+'`' for c in columns)
        data=query(f'SELECT JSON_OBJECT({pairs}) FROM {table};')
        source[table]=sorted([json.loads(line) for line in data.splitlines()],key=canonical)
    return source


def main():
    os.umask(0o077)
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--database',required=True)
    parser.add_argument('--container',help='仅隔离测试容器；已有库使用mysql客户端配置文件')
    parser.add_argument('--defaults-extra-file',type=Path,help='mysql权限配置，密码不放命令行')
    parser.add_argument('--output',required=True,type=Path)
    args=parser.parse_args()
    command=['docker','exec','--interactive',args.container,'mysql','--user=root'] if args.container else ['mysql']+(['--defaults-extra-file='+str(args.defaults_extra_file)] if args.defaults_extra_file else [])
    source=export(command,args.database)
    args.output.write_text(json.dumps(source,ensure_ascii=False,indent=2)+'\n')
    print('已只读导出 '+str(len(source))+' 张授权表；未执行转换')


if __name__=='__main__':
    main()
