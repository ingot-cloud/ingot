#!/usr/bin/env python3
"""守住 IAM 的源码分层、公开模型及 Mapper 装配边界；不访问数据库。"""
from pathlib import Path
import re
import unittest
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[2]
IAM = ROOT / 'ingot-service/ingot-iam'
PROVIDER = IAM / 'ingot-iam-provider/src/main/java/com/ingot/cloud/iam'
API = IAM / 'ingot-iam-api/src/main/java'
COMMONS = ROOT / 'ingot-framework/ingot-commons/src/main/java'
LAYERS = {'application', 'domain', 'infrastructure', 'interfaces', 'configuration'}
DEPENDENCIES = {
    'domain': {'domain', 'api'},
    'infrastructure': {'infrastructure', 'domain', 'api'},
    'application': {'application', 'domain', 'infrastructure', 'api'},
    'interfaces': {'interfaces', 'application', 'domain', 'api'},
    'configuration': LAYERS | {'api'},
}
REFERENCE = re.compile(r'\bcom\.ingot\.cloud\.iam\.([A-Za-z_]\w*)\.')


def code(path):
    """忽略历史注释，保留包括限定类型名与类名配置在内的实际源码。"""
    return re.sub(r'/\*[\s\S]*?\*/|//[^\n]*', '', path.read_text())


class PackageBoundariesTest(unittest.TestCase):
    def test_provider_has_only_explicit_layers(self):
        self.assertEqual({'InIamApplication.java'}, {path.name for path in PROVIDER.glob('*.java')})
        for path in PROVIDER.rglob('*.java'):
            relative = path.relative_to(PROVIDER)
            if len(relative.parts) > 1:
                self.assertIn(relative.parts[0], LAYERS, str(relative))

    def test_boot_application_is_unique_and_in_module_root(self):
        applications = [path for path in PROVIDER.rglob('*.java')
                        if re.search(r'@SpringBootApplication\b', code(path))]
        self.assertEqual([PROVIDER / 'InIamApplication.java'], applications)
        self.assertRegex(code(applications[0]), r'^package com\.ingot\.cloud\.iam;',
                         '启动类必须位于模块根包，保留全部 IAM 子包的默认扫描范围')

    def test_dependencies_follow_layer_direction(self):
        for path in PROVIDER.rglob('*.java'):
            layer = path.relative_to(PROVIDER).parts[0]
            if layer not in DEPENDENCIES:
                continue
            for target in REFERENCE.findall(code(path)):
                self.assertIn(target, DEPENDENCIES[layer], str(path.relative_to(ROOT)))

    def test_repository_beans_live_in_persistence(self):
        for path in PROVIDER.rglob('*.java'):
            if re.search(r'^@Repository\b', code(path), re.M):
                self.assertIn(PROVIDER / 'infrastructure/persistence', path.parents, str(path.relative_to(ROOT)))

    def test_member_query_types_expose_only_read_operations(self):
        for relative in ['application/organization/MemberQueryService.java',
                         'infrastructure/persistence/MemberQueryRepository.java']:
            text = code(PROVIDER / relative)
            self.assertNotRegex(text,
                                r'\bpublic\s+[^;{}]+\b(?:create|patch|replace|insert|update|delete|remove|lock)\w*\s*\(',
                                relative)
            self.assertNotRegex(text, r'\.(?:insert|update|delete)\w*\s*\(', relative)
            self.assertNotIn('TransactionTemplate', text, relative)

    def test_aggregate_repositories_delegate_without_mapper_dependencies(self):
        for relative in ['infrastructure/persistence/MemberRepository.java',
                         'infrastructure/persistence/CatalogRepository.java']:
            text = code(PROVIDER / relative)
            self.assertNotIn('infrastructure.persistence.mapper.', text, relative)
            self.assertNotIn('Wrappers', text, relative)
            self.assertNotIn('TransactionTemplate', text, relative)

    def test_eager_constructor_collaborators_have_no_cycles(self):
        # ObjectProvider and framework-owned dependencies remain outside this static graph.
        # Existing lazy cache/evaluator wiring is verified by configuration tests separately.
        beans = {}
        implementations = {}
        for path in PROVIDER.rglob('*.java'):
            text = code(path)
            if not re.search(r'^@(?:Service|Component|Repository)\b', text, re.M):
                continue
            package = re.search(r'^package\s+([\w.]+);', text, re.M).group(1)
            name = package + '.' + path.stem
            imports = {item.rsplit('.', 1)[-1]: item
                       for item in re.findall(r'^import ([\w.]+);', text, re.M)}
            beans[name] = (text, package, imports)
            declaration = re.search(r'\bclass\s+\w+[^{}]*\bimplements\s+([^\{]+)', text)
            if declaration:
                for interface in re.findall(r'\b[A-Z]\w+\b', declaration.group(1)):
                    target = imports.get(interface, package + '.' + interface)
                    implementations.setdefault(target, []).append(name)
        graph = {name: set() for name in beans}
        for name, (text, package, imports) in beans.items():
            for field_type in re.findall(r'private final\s+([\w.$]+)(?:<[^;]+>)?\s+\w+\s*;', text):
                target = field_type if '.' in field_type else imports.get(field_type, package + '.' + field_type)
                if target in beans:
                    graph[name].add(target)
                elif len(implementations.get(target, [])) == 1:
                    graph[name].add(implementations[target][0])
        visited = set()
        pending = []

        def visit(name):
            self.assertNotIn(name, pending, ' -> '.join(pending + [name]))
            if name in visited:
                return
            pending.append(name)
            for dependency in graph[name]:
                visit(dependency)
            pending.pop()
            visited.add(name)

        for name in graph:
            visit(name)

    def test_public_api_has_no_orm_entities_or_converters(self):
        forbidden = re.compile(r'@Table(?:Name|Id|Field)\b|\bBaseMapper\b|\borg\.mapstruct\b')
        for path in API.rglob('*.java'):
            self.assertIsNone(forbidden.search(code(path)), str(path.relative_to(ROOT)))
            self.assertNotIn('/model/domain/', str(path))
        for path in COMMONS.rglob('*.java'):
            self.assertNotIn('com.ingot.cloud.iam.', code(path), str(path.relative_to(ROOT)))

    def test_consumers_do_not_import_provider_implementation(self):
        for directory in ['ingot-framework', 'ingot-service', 'ingot-examples']:
            for path in (ROOT / directory).rglob('*.java'):
                if '/src/main/' not in str(path) or '/build/' in str(path) or PROVIDER in path.parents:
                    continue
                self.assertFalse(set(REFERENCE.findall(code(path))) - {'api'}, str(path.relative_to(ROOT)))

    def test_mapper_namespaces_and_qualified_types_resolve(self):
        classes = {}
        for path in PROVIDER.rglob('*.java'):
            package = re.search(r'^package\s+([\w.]+);', path.read_text(), re.M).group(1)
            classes[package + '.' + path.stem] = path
        for path in (IAM / 'ingot-iam-provider/src/main/resources/mapper').glob('*.xml'):
            document = ET.fromstring(path.read_text())
            namespace = document.attrib['namespace']
            self.assertIn(namespace, classes, str(path))
            self.assertIn('@Mapper', classes[namespace].read_text(), namespace)
            for item in document.iter():
                for value in item.attrib.values():
                    if value.startswith('com.ingot.cloud.iam.'):
                        self.assertIn(value.split('$')[0], classes, str(path))


if __name__ == '__main__':
    unittest.main()
