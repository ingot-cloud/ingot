#!/usr/bin/env python3
"""从 Java 导出的模型与显式路由清单生成阶段性 IAM OpenAPI；不表示运行时已实现。"""
import argparse
import json
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
CONTRACT = ROOT / 'specs/changes/active/20260912-iam-identity-access-management/contracts'
GENERATED = ROOT / 'ingot-framework/ingot-commons/build/iam-contract/schemas.json'
REF_PREFIX = '#/components/schemas/'
MEDIA_TYPE = 'application/json'


def ref(name):
    return {'$ref': REF_PREFIX + name}


def retain_envelopes(schemas):
    """为字典/发号/社交等既有领域包装补齐无 IAM DTO 的 R 信封，不臆造其内部字段。"""
    schemas.setdefault('RJson', {
        'type': 'object',
        'required': ['code', 'message', 'data', 'success'],
        'properties': {
            'code': {'type': 'string'},
            'message': {'type': 'string'},
            'data': {},
            'success': {'type': 'boolean'},
        },
    })
    schemas.setdefault('RVoid', {
        'type': 'object',
        'required': ['code', 'message', 'data', 'success'],
        'properties': {
            'code': {'type': 'string'},
            'message': {'type': 'string'},
            'data': {'nullable': True, 'enum': [None]},
            'success': {'type': 'boolean'},
        },
    })
    return schemas


def build(schemas):
    schemas = retain_envelopes(dict(schemas))
    routes = json.loads((CONTRACT / 'routes.json').read_text())
    paths = {}
    operation_ids = set()
    for route in routes:
        operation_id = route['operationId']
        if operation_id in operation_ids:
            raise ValueError(f'duplicate operationId: {operation_id}')
        operation_ids.add(operation_id)
        operation = {
            'operationId': operation_id, 'summary': route['summary'],
            'tags': [route['domain']], 'security': [{'bearerAuth': []}],
            'x-iam-domain': route['domain'], 'x-iam-action': route['action'],
            'x-iam-execution': route['execution'], 'x-runtime-implemented': bool(route.get('implemented')),
            'responses': {'200': {'description': '成功；使用既有 R 信封',
                                  'content': {MEDIA_TYPE: {'schema': ref(route['response'])}}}},
        }
        if route.get('request'):
            operation['requestBody'] = {'required': True, 'content': {
                MEDIA_TYPE: {'schema': ref(route['request'])}}}
        if route.get('example'):
            operation['requestBody']['content'][MEDIA_TYPE]['example'] = json.loads(
                (CONTRACT / 'examples' / (route['example'] + '.json')).read_text())
        for status in ('400', '401', '403', '404', '409', '503'):
            operation['responses'][status] = {
                'description': '稳定业务错误码；失败时 data 为 null',
                'content': {MEDIA_TYPE: {'schema': ref('IamErrorEnvelope')}}}
        parameters = [{'name': name, 'in': 'path', 'required': True,
                       'schema': {'type': 'string', 'minLength': 1}}
                      for name in re.findall(r'\{([A-Za-z][A-Za-z0-9]*)\}', route['path'])]
        for query in route.get('query') or []:
            parameter = {'name': query['name'], 'in': 'query', 'required': bool(query.get('required')),
                         'schema': query['schema']}
            parameters.append(parameter)
        if parameters:
            operation['parameters'] = parameters
        methods = paths.setdefault(route['path'], {})
        if route['method'] in methods:
            raise ValueError('duplicate route: ' + route['path'])
        methods[route['method']] = operation
    return {
        'openapi': '3.0.3',
        'info': {'title': 'IAM 目标接口契约', 'version': '0.3.0',
                 'description': '覆盖 routes.json 列出的管理面与保留能力接口；查询参数、purpose 与导出任务状态以控制器为准。x-runtime-implemented 仅表示控制器已接入。'},
        'servers': [{'url': '/api/iam'}], 'paths': paths,
        'components': {'securitySchemes': {'bearerAuth': {'type': 'http', 'scheme': 'bearer'}},
                       'schemas': {**schemas, 'IamErrorEnvelope': {
                           'type': 'object', 'required': ['code', 'message', 'data', 'success'],
                           'properties': {
                               'code': {'type': 'string', 'enum': schemas['Decision']['properties']['reasonCode']['enum']},
                               'message': {'type': 'string'},
                               'data': {'type': 'object', 'nullable': True, 'enum': [None]},
                               'success': {'type': 'boolean', 'enum': [False]}}}}}}


def validate_references(document):
    """拒绝悬空组件引用，避免发布无法解析的模型快照。"""
    def visit(value):
        if isinstance(value, dict):
            if '$ref' in value:
                current = document
                if not value['$ref'].startswith('#/'):
                    raise ValueError('external reference: ' + value['$ref'])
                for segment in value['$ref'][2:].split('/'):
                    current = current[segment]
            for item in value.values():
                visit(item)
        elif isinstance(value, list):
            for item in value:
                visit(item)
    visit(document)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--check', action='store_true', help='检查已提交快照与生成结果一致，不写文件')
    args = parser.parse_args()
    source = GENERATED if not args.check else CONTRACT / 'schemas.json'
    schemas = json.loads(source.read_text())['components']['schemas']
    document = build(schemas)
    validate_references(document)
    content = json.dumps(document, ensure_ascii=False, indent=2) + '\n'
    target = CONTRACT / 'openapi.json'
    if args.check:
        if target.read_text() != content:
            raise SystemExit('OpenAPI snapshot differs; regenerate after Java contract tests pass')
    else:
        (CONTRACT / 'schemas.json').write_text(source.read_text())
        target.write_text(content)
    print(f'{len(document["paths"])} paths, {sum(len(v) for v in document["paths"].values())} operations verified')


if __name__ == '__main__':
    main()
