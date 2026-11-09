"""独立library脚本在无网络临时MySQL的重放和隔离验证。"""
import unittest
from generate_framework_databases import ROOT, CREATE, catalog
from isolated_mysql import IsolatedMysql


class FrameworkSchemasMysqlTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.mysql = IsolatedMysql().__enter__()
        cls.addClassCleanup(lambda: cls.mysql.__exit__(None, None, None))

    def test_each_library_initializes_only_the_selected_database_and_replays(self):
        self.mysql.sql('CREATE DATABASE unrelated;CREATE TABLE unrelated.sentinel(id INT PRIMARY KEY);INSERT INTO unrelated.sentinel VALUES(1);')
        for index, entry in enumerate(catalog()['modules']):
            with self.subTest(module=entry['module']):
                database = 'framework_module_' + str(index)
                self.mysql.sql('CREATE DATABASE ' + database)
                sql = (ROOT / entry['schema']).read_text()
                self.mysql.sql(sql, database)
                first = self.mysql.sql('SHOW TABLES', database).strip().splitlines()
                self.assertEqual(sorted(entry['tables']), first)
                definitions = self.mysql.sql(';'.join('SHOW CREATE TABLE `' + name + '`' for name in first), database)
                self.mysql.sql(sql, database)
                self.assertEqual(definitions, self.mysql.sql(';'.join('SHOW CREATE TABLE `' + name + '`' for name in first), database))
                for table in first:
                    self.assertEqual('0', self.mysql.sql('SELECT COUNT(*) FROM `' + table + '`', database).strip())
                self.assertEqual('1', self.mysql.sql('SELECT COUNT(*) FROM unrelated.sentinel', database).strip())
                self.assertEqual('1', self.mysql.sql('SELECT @@FOREIGN_KEY_CHECKS', database).strip())

    def test_member_and_security_consume_the_exact_library_table_definitions(self):
        for service, path in catalog()['serviceBundles'].items():
            with self.subTest(service=service):
                database = 'framework_consumer_' + service
                self.mysql.sql('CREATE DATABASE ' + database)
                self.mysql.sql((ROOT / path).read_text(), database)
                required = [entry for entry in catalog()['modules'] if service in entry['services']]
                for entry in required:
                    for table in entry['tables']:
                        self.assertEqual('0', self.mysql.sql('SELECT COUNT(*) FROM `' + table + '`', database).strip())
                        self.assertTrue(self.mysql.sql('SHOW CREATE TABLE `' + table + '`', database))


if __name__ == '__main__':
    unittest.main()
