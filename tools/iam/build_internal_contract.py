#!/usr/bin/env python3
"""从同一Java公共模型生成内部授权契约；不混入浏览器管理面或冷启动权限目录。"""
import argparse
import json
from pathlib import Path

from build_contract import validate_references

ROOT = Path(__file__).resolve().parents[2]
CONTRACT = ROOT / 'specs/changes/active/20260912-iam-identity-access-management/contracts'


def build():
    schemas = json.loads((CONTRACT / 'schemas.json').read_text())['components']['schemas']
    schemas['IamErrorEnvelope'] = json.loads((CONTRACT / 'openapi.json').read_text())['components']['schemas']['IamErrorEnvelope']
    # The online annotation snapshot is an iam-api DTO rather than a commons v2 type.
    # Match the framework's InModule (Long identifiers/version serialize as strings).
    identifier = {'type': 'string', 'pattern': r'^\d+$'}
    instant = {'type': 'string', 'format': 'date-time'}
    schemas['AuthorizationSnapshotRequest'] = {'type': 'object', 'properties': {
        'tenantId': {**identifier, 'nullable': True}, 'userId': {**identifier, 'nullable': True}}}
    schemas['AuthorizationRoleBindingDTO'] = {'type': 'object', 'properties': {
        'roleId': identifier, 'platformRole': {'type': 'boolean'}, 'roleCode': {'type': 'string'},
        'deptId': {**identifier, 'nullable': True}, 'filterDept': {'type': 'boolean'}}}
    schemas['AuthorizationResourceRuleDTO'] = {'type': 'object', 'properties': {
        'resourceCode': {'type': 'string'}, 'permissionCode': {'type': 'string'},
        'scopeType': {'type': 'integer', 'enum': [0, 1, 2, 3, 9]},
        'deptIds': {'type': 'array', 'items': identifier}, 'self': {'type': 'boolean'}}}
    schemas['AuthorizationSnapshotDTO'] = {'type': 'object', 'required': ['platformAdministrator'], 'properties': {
        'tenantId': identifier, 'userId': identifier,
        'roleBindings': {'type': 'array', 'items': {'$ref': '#/components/schemas/AuthorizationRoleBindingDTO'}},
        'permissionCodes': {'type': 'array', 'uniqueItems': True, 'items': {'type': 'string'}},
        'platformAdministrator': {'type': 'boolean', 'description': '仅服务器实时有效平台系统直接分配产生；不是业务操作码，租户恒为false'},
        'resourceRules': {'type': 'array', 'items': {'$ref': '#/components/schemas/AuthorizationResourceRuleDTO'}},
        'source': {'type': 'string'}, 'version': identifier, 'generatedAt': instant, 'expiresAt': instant,
        'emptyAuthorization': {'type': 'boolean', 'readOnly': True}}}
    schemas['RAuthorizationSnapshotDTO'] = {'type': 'object', 'properties': {
        'code': {'type': 'string'}, 'message': {'type': 'string'}, 'success': {'type': 'boolean'},
        'data': {'$ref': '#/components/schemas/AuthorizationSnapshotDTO'}}}
    paths = {}
    for path, operation, request, response, description in [
        ('/inner/authorization/snapshot', 'iamAuthorizationSnapshot', 'AuthorizationSnapshotRequest', 'RAuthorizationSnapshotDTO',
         'INNER受信任调用；在线重验原始身份和有效授权；请求身份仅可核对不能替代；platformAdministrator独立于permissionCodes，注解不能复用旧JWT或数据范围热缓存'),
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
            'requestBody': {'required': path != '/inner/authorization/snapshot', 'content': {
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
                 'ResourceObjectInvocation', 'RResourceObjectResult', 'IamErrorEnvelope',
                 'AuthorizationSnapshotRequest', 'RAuthorizationSnapshotDTO']:
        add(name)
    document = {
        'openapi': '3.0.3', 'info': {'title': 'IAM资源内部授权契约', 'version': '2.0.0'}, 'paths': paths,
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
    print('3 internal operations verified')
