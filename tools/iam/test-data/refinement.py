"""平台角色分配增量：独立环境的三类管理员夹具与真实 HTTP 回归。"""
from __future__ import annotations

import copy
import hashlib
import os
from datetime import datetime, timedelta, timezone
from urllib.parse import urlencode

from build import (IamSession, _ensure_account, _ensure_group, _ensure_member, _password,
                   _read_secrets, _write_secrets, secrets_path, username_of)
from client import HttpTransport, TransportError, created_resource, payload_data
from lib import ConfigError, inventory_path, prepare, write_inventory

ACTORS = ("refine-governor", "refine-limited", "refine-both", "refine-recipient-a",
          "refine-recipient-b", "refine-outsider", "refine-overlap", "refine-partial", "refine-disjoint")
PREFIX = "/v1/platform"
SCOPE = [{"kind": "OBJECT_SET", "parameterKey": "members"}]
ENTRY_CODES = {"iam-platform:assignment:" + operation for operation in ("read", "create", "update", "delete")}


def candidates(session, token, kind, **query):
    """完整分页，候选与已选回显始终使用已批准的专用接口。"""
    page = 1
    while True:
        data = payload_data(session.iam("GET", PREFIX + "/assignments/candidates?" +
                                       urlencode({"kind": kind, "page": page, "pageSize": 20, **query}), token))
        yield from data["items"]
        if page * data["pageSize"] >= data["total"]:
            return
        page += 1


def assignment(member, revision, objects, start, end, source=None):
    """角色版本和范围对象均使用 API 返回的实际标识。"""
    return {"subject": {"type": "MEMBER", "id": member}, "roleRevisionRef": revision,
            "scopeBindings": {"members": {"kind": "OBJECTS", "ids": objects}} if objects is not None else {},
            "validFrom": start, "validUntil": end, "delegationGrantId": source}


def refinement_build(config, run_id, *, state_override=None, transport=None, getenv=None):
    getenv = getenv or os.getenv
    inventory = prepare(config, run_id, state_override=state_override)
    file = inventory_path(config, run_id, state_override)
    secrets = _read_secrets(secrets_path(file))
    objects = inventory.setdefault("objects", {})
    session = IamSession(config, transport or HttpTransport(), getenv)
    token = session.login("PLATFORM", config.get("bootstrapUsername") or "platform",
                          _password(getenv, config, "platform-governor", secrets))
    # Fail before creating data when the server has not been refreshed to the new contract.
    context = payload_data(session.iam("GET", PREFIX + "/assignments/context", token))
    if not context.get("directCreate") or not context.get("directRevoke"):
        raise ConfigError("测试治理身份缺少直接分配与撤销资格")

    def checkpoint():
        _write_secrets(secrets_path(file), secrets)
        write_inventory(file, inventory)

    def create_once(key, path, body):
        if objects.get(key, {}).get("id"):
            return objects[key]
        objects[key] = created_resource(session.iam("POST", path, token, json_body=body))
        checkpoint()
        return objects[key]

    for identity in ACTORS:
        _ensure_account(session, token, objects, identity, secrets, checkpoint)
        _ensure_member(session, token, objects, domain="PLATFORM", identity=identity,
                       tenant_id=None, departments=[])
        checkpoint()
    member = lambda identity: objects["member:PLATFORM:refine-" + identity]["id"]
    versions = list(candidates(session, token, "ROLE_REVISION"))
    governance = next((option for option in versions if option["roleRevisionRef"]["kind"] == "SYSTEM"
                       and ENTRY_CODES <= {action["code"] for action in option["actions"]}), None)
    if governance is None:
        raise ConfigError("正式初始化目录缺少平台 SYSTEM 治理角色固定版本")
    read = next((action for action in governance["actions"] if action["code"] == "iam-platform:member:read"), None)
    if read is None:
        raise ConfigError("平台成员读取操作未接入正式目录")
    definition = {"grants": [{"actionId": read["id"], "scopes": SCOPE}], "deltas": [],
                  "parameterDefinitions": [{"key": "members", "kind": "OBJECTS"}], "metadataOverrides": None}
    role = create_once("refinement:role", PREFIX + "/roles", {
        "code": "refine-platform-member-reader-" + hashlib.sha256(run_id.encode()).hexdigest()[:8], "name": "测试-受限平台成员读取", "kind": "PLATFORM_CUSTOM",
        "definition": definition})
    if not objects.get("refinement:revision"):
        detail = payload_data(session.iam("GET", PREFIX + "/roles/" + role["id"], token))
        revision = create_once("refinement:revision", PREFIX + "/roles/" + role["id"] + "/revisions", {
            "expectedVersion": detail["version"], "definition": definition})
    else:
        revision = objects["refinement:revision"]
    ref = {"kind": "PLATFORM_CUSTOM", "id": revision["id"]}
    now = datetime.now(timezone.utc).replace(microsecond=0)
    instant = lambda value: value.isoformat().replace("+00:00", "Z")
    bounds = inventory.setdefault("refinementBounds", {"from": instant(now - timedelta(minutes=1)),
        "until": instant(now + timedelta(days=30)), "assignmentUntil": instant(now + timedelta(days=6))})
    start, end = bounds["from"], bounds["assignmentUntil"]
    a, b = member("recipient-a"), member("recipient-b")
    for identity in ("governor", "both"):
        create_once("refinement:direct:" + identity, PREFIX + "/assignments", {"items": [
            assignment(member(identity), governance["roleRevisionRef"], None, start, None)]})
    for identity, allowed in (("limited", [a, b]), ("both", [a]), ("overlap", [a]),
                              ("partial", [a, b]), ("disjoint", [b])):
        create_once("refinement:delegation:" + identity, PREFIX + "/delegations", {
            "administratorMemberId": member(identity), "allowedRoleRevisionRefs": [ref],
            "recipientSelection": {"members": [a, b], "departments": []},
            "actionScopeCeilings": [{"actionId": read["id"], "scopes": SCOPE,
                "scopeBindings": {"members": {"kind": "OBJECTS", "ids": allowed}}}],
            "validFrom": start, "validUntil": bounds["until"], "maxAssignmentDuration": "P7D"})
        if identity in ("overlap", "partial", "disjoint"):
            create_once("refinement:business:" + identity, PREFIX + "/assignments", {"items": [
                assignment(member(identity), ref, [a], start, None)]})
    _ensure_group(session, token, objects, "refinement:group:allowed", PREFIX + "/groups", "测试-允许接收组",
                  {"name": "测试-允许接收组", "selection": {"members": [a, b], "departments": []}})
    _ensure_group(session, token, objects, "refinement:group:outside", PREFIX + "/groups", "测试-越界接收组",
                  {"name": "测试-越界接收组", "selection": {"members": [a, member("outsider")], "departments": []}})
    limited = session.login("PLATFORM", username_of("refine-limited"), secrets["refine-limited"])
    create_payload = {"items": [assignment(a, ref, [a], start, end, objects["refinement:delegation:limited"]["id"])]}
    if not objects.get("refinement:derived"):
        objects["refinement:derived"] = created_resource(session.iam("POST", PREFIX + "/assignments", limited,
                                                                    json_body=create_payload))
    inventory["refinement"] = {"roleRevisionRef": ref, "action": read, "assignment": create_payload["items"][0]}
    checkpoint()
    return {"environmentId": config["environmentId"], "runId": run_id, "fixture": "built",
            "identities": list(ACTORS), "inventory": str(file), "acceptance": "not-run"}


def refinement_verify(config, run_id, *, state_override=None, transport=None, getenv=None):
    """真实 HTTP 断言不修改已有范围；拒绝用预览成功替代写入拒绝检查。"""
    getenv = getenv or os.getenv
    file = inventory_path(config, run_id, state_override)
    inventory = prepare(config, run_id, state_override=state_override)
    if not inventory.get("refinement"):
        raise ConfigError("请先执行 refinement-build")
    objects = inventory["objects"]
    secrets = _read_secrets(secrets_path(file))
    session = IamSession(config, transport or HttpTransport(), getenv)
    checked = []

    def check(name, condition):
        if not condition:
            raise ConfigError("HTTP 验收失败：" + name)
        checked.append(name)

    def denied(name, method, path, token, body=None, statuses=(400, 403, 404)):
        try:
            session.iam(method, path, token, json_body=body)
        except TransportError as error:
            check(name, error.status in statuses)
        else:
            check(name, False)

    tokens = {name: session.login("PLATFORM", username_of("refine-" + name), secrets["refine-" + name])
              for name in ("governor", "limited", "both", "overlap", "partial", "disjoint")}
    for name in ("governor", "limited", "both"):
        context = payload_data(session.iam("GET", PREFIX + "/assignments/context", tokens[name]))
        check(name + " 分配资格独立", bool(context["directCreate"]) == (name != "limited"))
    limited = tokens["limited"]
    bootstrap = payload_data(session.iam("GET", "/v1/me/bootstrap", limited))
    check("纯委派具备入口，未授予业务操作", ENTRY_CODES <= set(bootstrap["actionCodes"])
          and "iam-platform:member:read" not in bootstrap["actionCodes"]
          and "iam-platform:delegation:create" not in bootstrap["actionCodes"])
    allowed = {objects["member:PLATFORM:refine-recipient-" + name]["id"] for name in ("a", "b")}
    check("成员候选边界", {item["id"] for item in candidates(session, limited, "MEMBER",
          delegationGrantId=objects["refinement:delegation:limited"]["id"])} == allowed)
    check("组候选边界", {item["id"] for item in candidates(session, limited, "GROUP",
          delegationGrantId=objects["refinement:delegation:limited"]["id"])} == {objects["refinement:group:allowed"]["id"]})
    current = payload_data(session.iam("GET", PREFIX + "/assignments/" + objects["refinement:derived"]["id"], limited))
    check("真实创建审计与授权时间", bool(current["record"].get("createdAt"))
          and current["record"]["grantedBy"]["memberId"] == objects["member:PLATFORM:refine-limited"]["id"])
    for name in ("overlap", "partial", "disjoint"):
        before = payload_data(session.iam("GET", "/v1/me/bootstrap", tokens[name]))
        check(name + " 委派不改变本人业务权限", "iam-platform:member:read" in before["actionCodes"])
        visible = payload_data(session.iam("GET", PREFIX + "/members?page=1&pageSize=20", tokens[name]))
        check(name + " 本人业务范围保持直接分配", {item["record"]["id"] for item in visible["items"]}
              == {objects["member:PLATFORM:refine-recipient-a"]["id"]})
    base = inventory["refinement"]["assignment"]
    variants = {"省略来源": {"delegationGrantId": None}, "借用他人来源": {
        "delegationGrantId": objects["refinement:delegation:both"]["id"]},
        "接收人越界": {"subject": {"type": "MEMBER", "id": objects["member:PLATFORM:refine-outsider"]["id"]}},
        "对象越界": {"scopeBindings": {"members": {"kind": "OBJECTS", "ids": [objects["member:PLATFORM:refine-outsider"]["id"]]}}},
        "长期分配越界": {"validUntil": None}}
    for name, changed in variants.items():
        item = copy.deepcopy(base); item.update(changed)
        denied(name, "POST", PREFIX + "/assignments", limited, {"items": [item]})
    forged = copy.deepcopy(base); forged["subject"] = {"type": "GROUP", "id": objects["refinement:group:outside"]["id"]}
    denied("用户组越界", "POST", PREFIX + "/assignments", limited, {"items": [forged]})
    forged = copy.deepcopy(base); forged["delegationGrantId"] = objects["refinement:delegation:both"]["id"]
    denied("多委派拼接", "POST", PREFIX + "/assignments", limited, {"items": [base, forged]})
    source_path = PREFIX + "/delegations/" + objects["refinement:delegation:limited"]["id"]
    source = payload_data(session.iam("GET", source_path, tokens["governor"]))
    narrowed = copy.deepcopy(source["record"]["delegation"])
    narrowed["recipientSelection"]["members"] = [objects["member:PLATFORM:refine-recipient-b"]["id"]]
    update = {"expectedVersion": source["version"], "delegation": narrowed}
    preview = payload_data(session.iam("POST", source_path + "/preview", tokens["governor"], json_body=update))
    check("委派收窄预览拒绝", preview["valid"] is False)
    denied("委派收窄提交拒绝", "PUT", source_path, tokens["governor"], update)
    unchanged = payload_data(session.iam("GET", source_path, tokens["governor"]))
    check("委派收窄无部分保存", unchanged["version"] == source["version"])
    foreign = objects["refinement:direct:both"]["id"]
    denied("其他管理员记录不可读", "GET", PREFIX + "/assignments/" + foreign, limited)
    denied("其他管理员记录不可撤销", "DELETE", PREFIX + "/assignments/" + foreign, limited)
    recipient = objects["member:PLATFORM:refine-recipient-a"]["id"]
    denied("人员角色移除不可绕过", "PUT", PREFIX + "/members/" + recipient + "/roles", limited, {"roleIds": []})
    diagnosis = payload_data(session.iam("POST", PREFIX + "/authorization/diagnose", tokens["governor"], json_body={
        "memberId": recipient, "applicationId": inventory["refinement"]["action"]["applicationId"],
        "actionId": inventory["refinement"]["action"]["id"], "targetId": recipient}))
    check("诊断真实分配来源", any(item.get("assignmentId") == objects["refinement:derived"]["id"]
         and item.get("delegationId") == objects["refinement:delegation:limited"]["id"] for item in diagnosis["sources"]))
    report = {"environmentId": config["environmentId"], "runId": run_id, "checks": checked,
              "status": "passed", "remaining": "浏览器交互、多时区、并发锁竞争及到期场景仍按验收卡执行"}
    inventory.setdefault("reports", []).append(report); write_inventory(file, inventory)
    return report
