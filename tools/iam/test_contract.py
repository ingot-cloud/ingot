#!/usr/bin/env python3
"""校验 IAM 阶段契约引用、隔离路径及发布快照；不替代完整 OpenAPI 规范验证器。"""
import copy
import json
import re
import unittest

from build_contract import CONTRACT, ROOT, build, validate_references

CONTROLLERS = ROOT / 'ingot-service/ingot-iam/ingot-iam-provider/src/main/java/com/ingot/cloud/iam/web/v1'
EXCLUDED_CONTROLLERS = {'OSSCommonAPI.java'}
CLASS_MAPPING = re.compile(
    r'@RequestMapping\s*\(\s*(?:value\s*=\s*)?["\']([^"\']+)["\']')
METHOD_MAPPING = re.compile(
    r'@(Get|Post|Put|Patch|Delete)Mapping(?:\s*\(\s*(?:value\s*=\s*)?["\']([^"\']+)["\'])?')
UNTYPED_ENVELOPES = {'RJson', 'RVoid'}


def normalize_path(path):
    return re.sub(r'\{[^}]+\}', '{}', path)


def join_path(prefix, suffix):
    if not suffix:
        return prefix
    if prefix.endswith('/') and suffix.startswith('/'):
        return prefix[:-1] + suffix
    if not suffix.startswith('/'):
        return prefix.rstrip('/') + '/' + suffix
    return prefix + suffix


def controller_operations():
    operations = set()
    for path in sorted(CONTROLLERS.rglob('*.java')):
        if path.name in EXCLUDED_CONTROLLERS:
            continue
        text = path.read_text()
        class_match = CLASS_MAPPING.search(text)
        if class_match is None:
            raise AssertionError(f'missing class RequestMapping: {path}')
        prefix = class_match.group(1)
        for method, suffix in METHOD_MAPPING.findall(text):
            operations.add((normalize_path(join_path(prefix, suffix)), method.lower()))
    return operations


class ContractTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.schemas = json.loads((CONTRACT / 'schemas.json').read_text())['components']['schemas']
        cls.document = build(cls.schemas)
        cls.route_items = json.loads((CONTRACT / 'routes.json').read_text())
        cls.routes = {route['operationId']: bool(route.get('implemented'))
                      for route in cls.route_items}
        cls.by_id = {route['operationId']: route for route in cls.route_items}

    def test_published_snapshot_is_reproducible(self):
        self.assertEqual(self.document, json.loads((CONTRACT / 'openapi.json').read_text()))

    def test_all_references_resolve_and_dangling_refs_fail(self):
        validate_references(self.document)
        broken = copy.deepcopy(self.document)
        del broken['components']['schemas']['PolicyPreviewInput']
        with self.assertRaises(KeyError):
            validate_references(broken)

    def test_domain_is_bound_to_route_not_request_query(self):
        implemented = 0
        for path, methods in self.document['paths'].items():
            self.assertNotIn('{domain}', path)
            for operation in methods.values():
                self.assertIn('x-runtime-implemented', operation)
                self.assertIsInstance(operation['x-runtime-implemented'], bool)
                self.assertEqual(operation['x-runtime-implemented'], self.routes[operation['operationId']])
                if operation['x-runtime-implemented']:
                    implemented += 1
                self.assertEqual([{'bearerAuth': []}], operation['security'])
                for parameter in operation.get('parameters', []):
                    self.assertNotEqual('tenantId', parameter['name'])
                if '/tenant/' in path or path.startswith('/v1/directory/'):
                    self.assertEqual('TENANT', operation['x-iam-domain'])
                    self.assertTrue(operation['x-iam-action'].startswith('iam-tenant:'))
                elif '/platform/' in path:
                    self.assertEqual('PLATFORM', operation['x-iam-domain'])
                    self.assertTrue(operation['x-iam-action'].startswith('iam-platform:'))
                else:
                    self.assertEqual('CURRENT', operation['x-iam-domain'])
                    self.assertTrue(path.startswith('/v1/me/'))
        self.assertEqual(sum(1 for flag in self.routes.values() if flag), implemented)

    def test_policy_updates_require_version_but_preview_has_no_write_command(self):
        for prefix in ['Directory', 'Field']:
            self.assertIn('expectedVersion', self.schemas[prefix + 'PolicyInput']['required'])
            self.assertIn('policy', self.schemas[prefix + 'PolicyInput']['required'])
            self.assertNotIn('expectedVersion', self.schemas[prefix + 'PolicyDraft']['properties'])
        self.assertNotIn('allowed', self.schemas['PolicyPreviewInput']['properties'])
        self.assertNotIn('tenantId', self.schemas['PolicyPreviewInput']['properties'])

    def test_operations_use_concrete_response_envelope_and_explicit_errors(self):
        envelopes = self.document['components']['schemas']
        for methods in self.document['paths'].values():
            for operation in methods.values():
                responses = operation['responses']
                for status in ['400', '401', '403', '404', '409', '503']:
                    self.assertIn(status, responses)
                schema = responses['200']['content']['application/json']['schema']
                name = schema['$ref'].split('/')[-1]
                envelope = envelopes[name]
                data = envelope['properties']['data']
                if name not in UNTYPED_ENVELOPES:
                    self.assertIn('$ref', data)
        error = self.document['components']['schemas']['IamErrorEnvelope']
        self.assertEqual([None], error['properties']['data']['enum'])
        self.assertEqual([False], error['properties']['success']['enum'])
        self.assertIn('AuthorizationUnavailable', error['properties']['code']['enum'])

    def test_candidate_purpose_filters_and_export_status_are_explicit(self):
        for operation_id, expected in [
            ('listDirectoryMembers', 'DIRECTORY'),
            ('listDirectoryDepartments', 'DIRECTORY'),
            ('tenantListDepartments', 'MANAGED_DEPARTMENT'),
        ]:
            purpose = next(item for item in self.by_id[operation_id]['query'] if item['name'] == 'purpose')
            self.assertTrue(purpose['required'])
            self.assertEqual([expected], purpose['schema']['enum'])
        tenant_filters = {item['name']: item for item in self.by_id['tenantListMembers']['query']}
        self.assertEqual(200, tenant_filters['pageSize']['schema']['maximum'])
        self.assertEqual(20, tenant_filters['pageSize']['schema']['default'])
        self.assertIn('phone', tenant_filters)
        self.assertIn('email', tenant_filters)
        platform_filters = {item['name'] for item in self.by_id['platformListMembers']['query']}
        self.assertEqual({'page', 'pageSize'}, platform_filters)
        tenant_list = {item['name']: item for item in self.by_id['platformListTenants']['query']}
        self.assertEqual({'page', 'pageSize', 'name', 'status'}, set(tenant_list))
        self.assertEqual(['ENABLED', 'DISABLED'], tenant_list['status']['schema']['enum'])
        export_status = self.by_id['tenantGetMemberExportStatus']
        self.assertEqual('/v1/tenant/members/export/{id}/status', export_status['path'])
        self.assertEqual('RExportTask', export_status['response'])
        self.assertEqual('get', export_status['method'])
        self.assertIn('purpose', self.schemas['AccountLookupInput']['required'])
        self.assertIn('/v1/me/profile', self.document['paths'])
        self.assertIn('/v1/me/password', self.document['paths'])
        self.assertIn('/v1/platform/accounts', self.document['paths'])
        self.assertIn('/v1/platform/dictionaries', self.document['paths'])
        dict_query = {item['name']: item for item in self.by_id['platformListDictionaries']['query']}
        self.assertEqual(['tree', 'page', 'items'], dict_query['view']['schema']['enum'])
        self.assertEqual(['current', 'size'], [item['name'] for item in self.by_id['platformListIdAllocations']['query']])

    def test_management_controllers_match_published_routes_excluding_oss_and_inner(self):
        published = {(normalize_path(route['path']), route['method']) for route in self.route_items}
        controllers = controller_operations()
        self.assertEqual(set(), published - controllers)
        self.assertEqual(set(), controllers - published)
        self.assertFalse(any(path.startswith('/v1/oss') or path.startswith('/inner/')
                             for path, _ in published))
        self.assertTrue(CONTROLLERS.joinpath('common/OSSCommonAPI.java').exists())


if __name__ == '__main__':
    unittest.main()
