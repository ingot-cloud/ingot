#!/usr/bin/env python3
"""从同一Java公共模型生成内部v2契约；不混入浏览器管理面或冷启动权限目录。"""
import argparse
import json
from pathlib import Path

from build_contract import validate_references

ROOT = Path(__file__).resolve().parents[2]
CONTRACT = ROOT / 'specs/changes/active/20260912-iam-identity-access-management/contracts'


def build():
    schemas = json.loads((CONTRACT / 'schemas.json').read_text())['components']['schemas']
    schemas['IamErrorEnvelope'] = json.loads((CONTRACT / 'openapi.json').read_text())['components']['schemas']['IamErrorEnvelope']
    paths = {}
    for path, operation, request, response, description in [
        ('/inner/authorization/v2/evaluate', 'iamEvaluateV2', 'AuthorizationRequest', 'RAuthorizationDecision',
         'INNER受信任调用及在线原始身份；请求不能替代用户；注册操作模式决定fresh或读热缓存'),
        ('/inner/iam/resource-objects/query', 'resourceObjectQuery', 'SignedResourceObjectRequest', 'RResourceObjectResult',
         '固定资源服务白名单；HMAC覆盖payload及短时间窗；payload.context与原认证身份完全一致；只返回最小候选或存在性'),
    ]:
        responses = {'200': {'description': 'R信封；精确错误码区分拒绝和不可用',
                             'content': {'application/json': {'schema': {'$ref': '#/components/schemas/' + response}}}}}
        for status in ('400', '401', '403', '404', '409', '503'):
            responses[status] = {'description': '稳定业务错误码；失败时data为空',
                                 'content': {'application/json': {'schema': {'$ref': '#/components/schemas/IamErrorEnvelope'}}}}
        paths[path] = {'post': {
            'operationId': operation, 'description': description, 'security': [{'bearerAuth': []}],
            'requestBody': {'required': True, 'content': {
                'application/json': {'schema': {'$ref': '#/components/schemas/' + request}}}},
            'responses': responses,
        }}
    # Use only transitively referenced types to keep this separate snapshot small.
    needed = set()

    def add(name):
        if name in needed:
            return
        needed.add(name)

        def visit(value):
            if isinstance(value, dict):
                if '$ref' in value:
                    add(value['$ref'].split('/')[-1])
                for child in value.values():
                    visit(child)
            elif isinstance(value, list):
                for child in value:
                    visit(child)
        visit(schemas[name])

    for name in ['AuthorizationRequest', 'RAuthorizationDecision', 'SignedResourceObjectRequest',
                 'ResourceObjectInvocation', 'RResourceObjectResult', 'IamErrorEnvelope']:
        add(name)
    document = {
        'openapi': '3.0.3', 'info': {'title': 'IAM资源内部v2契约', 'version': '2.0.0'}, 'paths': paths,
        'components': {'schemas': {name: schemas[name] for name in sorted(needed)},
                       'securitySchemes': {'bearerAuth': {'type': 'http', 'scheme': 'bearer'}}},
    }
    validate_references(document)
    return document


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('--check', action='store_true')
    args = parser.parse_args()
    output = json.dumps(build(), ensure_ascii=False, indent=2) + '\n'
    path = CONTRACT / 'internal-openapi.json'
    if args.check:
        if not path.exists() or path.read_text() != output:
            raise SystemExit('internal OpenAPI needs regeneration')
    else:
        path.write_text(output)
    print('2 internal operations verified')
