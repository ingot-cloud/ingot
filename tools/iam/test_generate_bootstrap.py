"""校验冷启动种子在已有保留号被占用时不会硬撞主键。"""

import unittest
import json
import re

from generate_bootstrap import (ACTION_BASE, MENU_BASE, DEVELOPER_ACTIONS, DEVELOPER_APPLICATION,
                                ROOT, ROUTES, collect, reserved_id)


class ReservedIdTest(unittest.TestCase):
    def test_prefers_free_reserved_number(self):
        expression = reserved_id('iam_action', ACTION_BASE + 18, ACTION_BASE, MENU_BASE)
        self.assertIn(f"SELECT {ACTION_BASE + 18} FROM DUAL", expression)
        self.assertIn("taken.id = 120018", expression)
        self.assertIn(f"taken.id >= {ACTION_BASE} AND taken.id < {MENU_BASE}", expression)

    def test_generated_purge_action_falls_back_when_preferred_id_is_taken(self):
        from pathlib import Path
        import generate_bootstrap
        sql = Path(generate_bootstrap.TARGET).read_text()
        self.assertIn("iam-platform:application:purge", sql)
        block = sql.split("iam-platform:application:purge", 1)[0].rsplit("INSERT INTO iam_action", 1)[1]
        self.assertIn("COALESCE(", block)
        self.assertIn("taken.id = 120018", block)

    def test_client_operation_codes_match_the_existing_auth_guards(self):
        api = ROOT / 'ingot-service/ingot-auth/ingot-auth-provider/src/main/java/com/ingot/cloud/auth/web/DevClientAPI.java'
        guards = set(re.findall(r'@AdminOrHasAnyAuthority\(\{"([^"]+)"}', api.read_text()))
        self.assertEqual(6, len(guards))
        self.assertEqual(guards, set(DEVELOPER_ACTIONS['client']))

    def test_existing_social_and_allocation_codes_keep_their_execution_contract_and_explicit_owner(self):
        routes = json.loads(ROUTES.read_text())
        catalog = collect(routes)
        for resource in ('social-config', 'id-allocation'):
            expected = {route['action'] for route in routes
                        if (route.get('action') or '').startswith(f'iam-platform:{resource}:')}
            self.assertEqual(4, len(expected))
            self.assertEqual(expected, catalog[DEVELOPER_APPLICATION][resource])
            self.assertNotIn(resource, catalog['iam-platform'])
        all_codes = [code for app in catalog.values() for codes in app.values() for code in codes]
        self.assertEqual(len(all_codes), len(set(all_codes)))
        self.assertTrue(all('*' not in code for code in all_codes))


if __name__ == '__main__':
    unittest.main()
