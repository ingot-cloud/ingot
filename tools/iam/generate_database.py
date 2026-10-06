#!/usr/bin/env python3
"""同步006正式种子及ingot_iam.sql；--check只读检查生成漂移。"""

import argparse

from database_sources import render_bundle, sources
from generate_bootstrap import build


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--check', action='store_true', help='只读核对种子和完整初始化文件')
    args = parser.parse_args()
    build(check=args.check)
    expected = render_bundle()
    target = sources('bundle')[0]
    if args.check:
        if not target.is_file() or target.read_text() != expected:
            raise SystemExit('ingot_iam.sql differs; run python3 tools/iam/generate_database.py')
    else:
        target.write_text(expected)
    print('IAM initialization bundle verified')


if __name__ == '__main__':
    main()
