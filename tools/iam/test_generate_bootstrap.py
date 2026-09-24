"""校验冷启动种子在已有保留号被占用时不会硬撞主键。"""

import unittest

from generate_bootstrap import ACTION_BASE, MENU_BASE, reserved_id


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


if __name__ == '__main__':
    unittest.main()
