#!/usr/bin/env python3
"""校验导入工具的幂等映射、源变化拒绝与未批准扩大。"""
import json
import tempfile
import unittest
from pathlib import Path
import subprocess
import sys

ROOT = Path(__file__).resolve().parent
MANIFEST = ROOT / 'fixtures' / 'metadata.json'
TOOL = ROOT / 'import_tool.py'


class ImportToolTest(unittest.TestCase):
    def run_tool(self, arguments):
        return subprocess.run([sys.executable, str(TOOL), *arguments], capture_output=True, text=True)

    def test_duplicate_import_reuses_mapping(self):
        with tempfile.TemporaryDirectory() as directory:
            mapping = Path(directory) / 'map.json'
            first_out = Path(directory) / 'first.json'
            second_out = Path(directory) / 'second.json'
            target = Path(directory) / 'target'
            self.assertEqual(0, self.run_tool(['dry-run', '--manifest', str(MANIFEST),
                                               '--mapping', str(mapping), '--output', str(first_out)]).returncode)
            self.assertEqual(0, self.run_tool(['import', '--manifest', str(MANIFEST), '--mapping', str(mapping),
                                               '--target-dir', str(target), '--output', str(first_out)]).returncode)
            self.assertEqual(0, self.run_tool(['import', '--manifest', str(MANIFEST), '--mapping', str(mapping),
                                               '--target-dir', str(target), '--output', str(second_out)]).returncode)
            first = json.loads((target / 'accounts.json').read_text())
            store = json.loads(mapping.read_text())
            self.assertTrue(store['imported'])
            self.assertFalse(store['verified'])
            self.assertEqual(len(first), len(json.loads(MANIFEST.read_text())['accounts']))

    def test_fingerprint_change_cannot_reuse_batch(self):
        with tempfile.TemporaryDirectory() as directory:
            mapping = Path(directory) / 'map.json'
            output = Path(directory) / 'out.json'
            self.assertEqual(0, self.run_tool(['dry-run', '--manifest', str(MANIFEST),
                                               '--mapping', str(mapping), '--output', str(output)]).returncode)
            changed = json.loads(MANIFEST.read_text())
            changed['sourceFingerprint'] = 'b' * 64
            other = Path(directory) / 'changed.json'
            other.write_text(json.dumps(changed))
            result = self.run_tool(['dry-run', '--manifest', str(other), '--mapping', str(mapping),
                                    '--output', str(output)])
            self.assertEqual(2, result.returncode)

    def test_expanded_without_disposition_fails_verify(self):
        with tempfile.TemporaryDirectory() as directory:
            mapping = Path(directory) / 'map.json'
            output = Path(directory) / 'out.json'
            target = Path(directory) / 'target'
            self.assertEqual(0, self.run_tool(['import', '--manifest', str(MANIFEST), '--mapping', str(mapping),
                                               '--target-dir', str(target), '--output', str(output)]).returncode)
            auth = Path(directory) / 'auth.json'
            auth.write_text(json.dumps([{'subjectId': 'm1', 'actionId': 'a1', 'result': 'EXPANDED'}]))
            result = self.run_tool(['verify', '--manifest', str(MANIFEST), '--mapping', str(mapping),
                                    '--authorization', str(auth), '--output', str(output)])
            self.assertEqual(1, result.returncode)
            document = json.loads(output.read_text())
            self.assertFalse(document['verified'])
            self.assertFalse(document['readyForCutover'])
            self.assertEqual('AUTHORIZATION_EXPANDED', document['issues'][0]['code'])


if __name__ == '__main__':
    unittest.main()
