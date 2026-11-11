"""Read-only verification of the completed IAM archive and applied baselines."""
from collections import Counter
import hashlib
import json
from pathlib import Path
import re
import subprocess
from urllib.parse import unquote


def digest(data):
    return hashlib.sha256(data).hexdigest()


change = Path(__file__).resolve().parents[1]
backend = next(parent for parent in change.parents if (parent / '.git').exists())
frontend = backend.parent / 'ingot-admin'
front_change = frontend / 'specs/changes/archive/2026/20260913-base-iam-admin'
roots = [('backend', backend, change), ('frontend', frontend, front_change)]
errors = []
counts = Counter()
projected = {}


def check(condition, message):
    if not condition:
        errors.append(message)


for label, root, active in roots:
    plan = json.loads((active / 'closeout/baseline-plan.json').read_text())
    check(plan['currentNotApplied'] is False, f'{label}: candidates have not been applied')
    check(plan['status'] == 'applied-and-archived', f'{label}: archive transition incomplete')
    check(all(plan['userAcceptance'].get(key) == 'passed' for key in ['H01', 'H02', 'H03', 'H04']), f'{label}: missing user acceptance')
    check(not (root / 'specs/changes/active' / active.name).exists(), f'{label}: old active path still exists')
    for item in plan['files']:
        target = root / item['target']
        source = active / item['candidate']
        projected[target] = source
        check(source.is_file() and digest(source.read_bytes()) == item['afterSha256'], f'candidate drift: {source}')
        actual = digest(target.read_bytes()) if target.is_file() else None
        check(actual == item['appliedSha256'], f'applied baseline drift: {target}')
        counts['candidates'] += 1
    ledger = json.loads((active / 'closeout/unchecked-disposition.json').read_text())
    check(not ledger['unresolvedAgentTechnicalTasks'], f'{label}: unresolved technical work')
    check(not ledger['pendingUserReview'] and not ledger['remainingAgentWork'], f'{label}: unresolved gates')
    check(ledger['completedUserReview'] == ['H01', 'H02', 'H03', 'H04'], f'{label}: missing accepted gates')
    expected = dict(Counter(row['disposition'] for row in ledger['records']))
    check(expected == ledger['summary'], f'{label}: ledger summary mismatch')
    for row in ledger['records']:
        check(row['note'] and row['evidence'], f'{label}: missing disposition rationale')
        for ref in row['evidence']:
            check((change / ref.split('#', 1)[0]).exists(), f'ledger evidence missing: {ref}')
    counts['dispositionRecords'] += len(ledger['records'])
    manifest = json.loads((active / 'closeout/archive-implementation-manifest.json').read_text())
    for item in manifest['files']:
        path = root / item['path']
        actual = digest(path.read_bytes()) if path.is_file() else None
        check(actual == item['sha256'], f'worktree changed since manifest: {path}')
        counts['manifestFiles'] += 1
    result = subprocess.run(['git', 'diff', '--check'], cwd=root, capture_output=True, text=True)
    check(result.returncode == 0, f'{label}: diff check: {result.stdout}{result.stderr}')
    check('> 状态：completed' in (active / 'README.md').read_text(), f'{label}: status is not completed')


def resolve_archive(path):
    text = str(path)
    for _, root, active in roots:
        archive = root / 'specs/changes/archive/2026' / active.name
        if text == str(archive) or text.startswith(str(archive) + '/'):
            return Path(text.replace(str(archive), str(active), 1))
    return path


def exists(path):
    return path in projected or resolve_archive(path).exists()


def validate_links(virtual, content):
    for target in re.findall(r'\]\(([^)]+)\)', content):
        target = target.strip('<>')
        if target.startswith(('https://', 'http://', 'mailto:', '#', 'codex:')):
            continue
        relative = unquote(target.split('#', 1)[0])
        if not relative:
            continue
        path = (virtual.parent / relative).resolve()
        check(exists(path), f'link missing: {virtual}: {target}')
        counts['localLinks'] += 1


for virtual, source in projected.items():
    validate_links(virtual, source.read_text())
for _, _, active in roots:
    for path in active.rglob('*.md'):
        if any(part in ('sources', 'history', 'candidates') for part in path.relative_to(active).parts):
            continue
        validate_links(path, path.read_text())

section = (front_change / 'SOURCES.md').read_text().split('## 2026-11-10 现行接口同步', 1)[1]
for src, dest, expected in re.findall(r'^\| `([^`]+)` \| \[.*?\]\(\./(sources/[^)]+)\) \| `([a-f0-9]{64})` \|$', section, re.M):
    original, copy = (change / src).read_bytes(), (front_change / dest).read_bytes()
    check(original == copy and digest(copy) == expected, f'source mismatch: {src}')
    counts['sourceCopies'] += 1
check(counts['sourceCopies'] == 49, f'unexpected source count: {counts["sourceCopies"]}')

runtime = json.loads((change / 'REMOTE-UPGRADE-RUNTIME.json').read_text())
for name, expected in runtime['sourceSha256'].items():
    check(digest((backend / name).read_bytes()) == expected, f'remote evidence production source drift: {name}')
    counts['remoteSourceFingerprints'] += 1

result = {'counts': dict(counts), 'errors': errors, 'passed': not errors}
print(json.dumps(result, ensure_ascii=False, indent=2))
raise SystemExit(bool(errors))
