#!/usr/bin/env python3
"""Read-only login migration inventory. Secrets and Redis keys are never printed."""
import argparse
import collections
import json
import subprocess

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('--host', default='127.0.0.1')
parser.add_argument('--port', type=int, default=6379)
parser.add_argument('--db', type=int, default=0)
parser.add_argument('--redis-cli', default='redis-cli')
parser.add_argument('--platform-client', default='in-bff-platform')
parser.add_argument('--tenant-client', default='in-bff-tenant')
args = parser.parse_args()
cli = [args.redis_cli, '-h', args.host, '-p', str(args.port), '-n', str(args.db)]
clients = {args.platform_client: 'platform-admin', args.tenant_client: 'tenant-admin'}
counts = collections.Counter()

def run(*command):
    return subprocess.check_output([*cli, '--raw', *command], text=True).strip()

patterns = ['in:bff_session:*', 'in:bff_login_tx:*', 'in:bff_auth_binding:*',
            'in:bff:v2:{platform-admin}:*', 'in:bff:v2:{tenant-admin}:*']
for pattern in patterns:
    keys = subprocess.check_output([*cli, '--scan', '--pattern', pattern], text=True).splitlines()
    for key in keys:
        kind = run('TYPE', key)
        if kind != 'string':
            counts['v2-index-or-queue'] += 1
            continue
        raw = run('GET', key)
        try:
            value = json.loads(raw)
            if isinstance(value, str):
                value = json.loads(value)
        except (ValueError, TypeError):
            counts['opaque-or-unrecognized'] += 1
            continue
        if not isinstance(value, dict):
            counts['opaque-or-unrecognized'] += 1
            continue
        app = value.get('appId')
        client = value.get('clientId')
        if client is not None and (client not in clients or (app is not None and app != clients[client])):
            counts["out-of-scope-or-unrecognized"] += 1
            continue
        if app not in clients.values() and client not in clients:
            counts['out-of-scope-or-unrecognized'] += 1
            continue
        app = app if app in clients.values() else clients[client]
        counts[app + ':records'] += 1
        if value.get('sid'):
            counts[app + ':known-sid-records'] += 1
print(json.dumps({'mode': 'dry-run-only', 'counts': counts,
                  'mutations': 0, 'revocations': 0,
                  'note': 'Count records, not unique sessions; this does not revoke Auth sessions.'}, ensure_ascii=False, indent=2))
