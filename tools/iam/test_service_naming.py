#!/usr/bin/env python3
"""验证构建、RPC 与部署中的 IAM 命名一致；不启动或发布服务。"""
import re
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[2]


class ServiceNamingTest(unittest.TestCase):
    def test_gradle_and_rpc_do_not_depend_on_old_module(self):
        roots = ['ingot-service', 'ingot-framework', 'config', 'nacos', 'deploy']
        stale = re.compile(r'ingot-pms|in-service-pms|com\.ingot\.cloud\.pms|RemotePms|PMS_SERVICE|ingot\.pms_api')
        for root in roots:
            for path in (ROOT / root).rglob('*'):
                if not path.is_file() or any(part in {'build', 'bin', '.gradle'} for part in path.parts) or path.suffix not in {'.java', '.gradle', '.yml', '.xml', '.sh', '.env', '.template'}:
                    continue
                self.assertIsNone(stale.search(path.read_text()), str(path.relative_to(ROOT)))
        self.assertIsNone(stale.search((ROOT / 'settings.gradle').read_text()), 'settings.gradle')

    def test_feign_targets_use_iam_service(self):
        directory = ROOT / 'ingot-service/ingot-iam/ingot-iam-api/src/main/java/com/ingot/cloud/iam/api/rpc'
        clients = list(directory.glob('RemoteIam*.java'))
        self.assertEqual(7, len(clients))
        for client in clients:
            self.assertIn('value = ServiceNameConstants.IAM_SERVICE', client.read_text())

    def test_nacos_routes_match_proxy_stripped_api_prefix(self):
        for group in ['DEV_GROUP', 'TEST_GROUP', 'PROD_GROUP']:
            directory = ROOT / 'nacos' / group
            gateway = (directory / 'in-service-gateway.yml').read_text()
            self.assertIn('uri: lb://in-service-iam', gateway)
            self.assertIn('Path=/iam/**', gateway)
            self.assertTrue((directory / 'in-service-iam.yml').exists())
            self.assertFalse((directory / 'in-service-pms.yml').exists())

    def test_iam_database_is_explicit_and_utc(self):
        for group in ['DEV_GROUP', 'TEST_GROUP', 'PROD_GROUP']:
            content = (ROOT / 'nacos' / group / 'in-service-iam.yml').read_text()
            self.assertIn('${IAM_DATABASE}', content)
            self.assertNotIn('${MYSQL_DATABASE:ingot_core}', content)
            self.assertIn('connectionTimeZone=UTC&forceConnectionTimeZoneToSession=true', content)
        for filename in ['docker-compose.yml', 'docker-compose.standalone.yml']:
            content = (ROOT / 'deploy/services' / filename).read_text()
            self.assertIn('IAM_DATABASE=${IAM_DATABASE:?', content)

    def test_iam_gradle_image_and_launcher_target_the_same_module(self):
        module = ROOT / 'ingot-service/ingot-iam/ingot-iam-provider'
        assembly = (module / 'ingot-assemble.gradle').read_text()
        self.assertEqual(['dev', 'test', 'prod'], re.findall(r'env\("([^"]+)"\)', assembly))
        self.assertEqual(['ingot/iam'] * 3, re.findall(r'\bname\s*=\s*"([^"]+)"', assembly))
        launcher = (ROOT / 'deploy/services/run-iam.sh').read_text()
        self.assertIn('SERVICE_NAME=ingot-iam', launcher)
        self.assertNotIn('ingot-auth', launcher)

    def test_deployment_image_and_version_are_iam_owned(self):
        for filename in ['docker-compose.yml', 'docker-compose.standalone.yml']:
            content = (ROOT / 'deploy/services' / filename).read_text()
            self.assertIn('ingot/iam:${IAM_VERSION}', content)
            self.assertIn('services-env/iam.env', content)
        launcher = (ROOT / 'deploy/services/run-iam.sh').read_text()
        self.assertIn('${IAM_VERSION}', launcher)
        self.assertNotIn('${MEMBER_VERSION}', launcher)


if __name__ == '__main__':
    unittest.main()
