#!/usr/bin/env python3
"""IAM 独立测试环境 CLI：prepare / verify / reset。"""

from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path

from build import build
from client import TransportError
from lib import ConfigError, help_text, load_config, prepare, reset, verify


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(
        description="IAM 独立测试数据工具",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog=help_text(),
    )
    parser.add_argument("command", choices=("prepare", "build", "verify", "reset", "help"))
    parser.add_argument("--config", type=Path, help="测试环境 JSON 配置")
    parser.add_argument("--run-id", dest="run_id", help="本次运行标识")
    parser.add_argument(
        "--confirm-reset",
        action="store_true",
        help="确认仅重建已登记独立测试环境的运行清单",
    )
    return parser


def main(argv: list[str] | None = None) -> int:
    parser = build_parser()
    args = parser.parse_args(argv)
    if args.command == "help":
        sys.stdout.write(help_text())
        return 0
    if args.config is None or not args.run_id:
        parser.error("prepare/build/verify/reset 需要 --config 与 --run-id")
    try:
        config = load_config(args.config)
        if args.command == "prepare":
            result = prepare(config, args.run_id)
        elif args.command == "build":
            result = build(config, args.run_id)
        elif args.command == "verify":
            result = verify(config, args.run_id)
        else:
            result = reset(config, args.run_id, confirm=args.confirm_reset)
    except (ConfigError, TransportError) as exc:
        sys.stderr.write(str(exc) + "\n")
        return 2
    sys.stdout.write(json.dumps(result, ensure_ascii=False, indent=2) + "\n")
    return 0


if __name__ == "__main__":
    sys.exit(main())
