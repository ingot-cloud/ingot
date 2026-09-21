"""测试数据 D02：通过真实 IAM HTTP 接口构建基础身份与组织图，复用清单对象。"""

from __future__ import annotations

import json
import os
from pathlib import Path
from typing import Callable

from client import (
    HttpTransport,
    TransportError,
    created_resource,
    page_records,
    payload_data,
    resource_identity,
    resource_version,
)
from lib import ConfigError, credential_names, inventory_path, prepare, report_identities, write_inventory

CREATED_IDENTITIES = (
    "platform-reader",
    "owner-a",
    "owner-b",
    "dual",
    "reader-a",
    "editor-a",
    "grantor-a",
    "ordinary-a",
    "multi-dept-a",
    "no-dept-a",
    "no-access",
    "suspended",
    "disabled",
    "owner-transfer-src",
    "owner-transfer-dst",
)
TENANT_A_MEMBERS = (
    "owner-a",
    "dual",
    "reader-a",
    "editor-a",
    "grantor-a",
    "ordinary-a",
    "multi-dept-a",
    "no-dept-a",
    "no-access",
    "owner-transfer-src",
    "owner-transfer-dst",
)
TENANT_B_MEMBERS = ("owner-b", "dual")
TENANT_OWNERS = {"a": "owner-a", "b": "owner-b"}
PLATFORM_MEMBERS = ("platform-reader", "dual", "suspended")
ALL_SCOPE = [{"kind": "ALL"}]


class IamSession:
    """带会话令牌的 IAM 调用。"""

    def __init__(self, config: dict, transport, getenv: Callable[[str], str | None]):
        self.config = config
        self.transport = transport
        self.getenv = getenv
        self.gateway = str(config["gatewayBaseUrl"]).rstrip("/")
        self.auth = str(config["authBaseUrl"]).rstrip("/")

    def _oauth_client(self) -> tuple[str, str]:
        names = self.config.get("oauth") or {}
        client_id = self.getenv(str(names.get("clientIdEnv") or "IAM_TEST_OAUTH_CLIENT_ID")) or names.get(
            "clientId"
        )
        client_secret = self.getenv(str(names.get("clientSecretEnv") or "IAM_TEST_OAUTH_CLIENT_SECRET")) or names.get(
            "clientSecret"
        )
        if not client_id or not client_secret:
            raise ConfigError("缺少 OAuth 客户端配置：oauth.clientId/clientSecret 或对应环境变量")
        return str(client_id), str(client_secret)

    def login(self, domain: str, username: str, password: str, org: str | None = None) -> str:
        form = {
            "grant_type": "password",
            "username": username,
            "password": password,
            "user_type": "0",
            "domain": domain,
        }
        if org:
            form["org"] = org
        body = self.transport.request(
            "POST",
            f"{self.auth}/oauth2/token",
            form=form,
            basic=self._oauth_client(),
        )
        token = payload_data(body).get("accessToken") or body.get("access_token")
        if not token:
            raise ConfigError("登录未返回 accessToken")
        return str(token)

    def iam(
        self,
        method: str,
        path: str,
        token: str,
        *,
        json_body: dict | None = None,
        tenant: str | None = None,
    ) -> dict:
        headers = {"Authorization": f"Bearer {token}"}
        if tenant:
            headers["Tenant"] = tenant
        return self.transport.request(
            method,
            f"{self.gateway}/iam{path}",
            headers=headers,
            json_body=json_body,
        )


def username_of(identity: str) -> str:
    return f"iam-test-{identity}"


def secrets_path(inventory_file: Path) -> Path:
    return inventory_file.with_suffix(".secrets.json")


def _read_secrets(path: Path) -> dict[str, str]:
    if not path.is_file():
        return {}
    data = json.loads(path.read_text())
    if not isinstance(data, dict):
        raise ConfigError("密钥文件损坏")
    return {str(key): str(value) for key, value in data.items()}


def _write_secrets(path: Path, secrets: dict[str, str]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(secrets, ensure_ascii=False, indent=2) + "\n")
    try:
        os.chmod(path, 0o600)
    except OSError:
        pass


def _ensure_object(objects: dict, key: str, factory) -> str:
    current = objects.get(key)
    if isinstance(current, dict) and current.get("id"):
        return "reused"
    objects[key] = factory()
    return "created"


def _require(objects: dict, key: str) -> dict:
    item = objects.get(key)
    if not isinstance(item, dict) or not item.get("id"):
        raise ConfigError(f"清单缺少 {key}，无法继续")
    return item


def _password(getenv: Callable[[str], str | None], config: dict, identity: str, secrets: dict[str, str]) -> str:
    env_name = credential_names(config).get(identity)
    if env_name:
        value = getenv(env_name)
        if value:
            return value
    if identity in secrets:
        return secrets[identity]
    raise ConfigError(f"身份 {identity} 没有可用口令（环境变量或密钥文件）")


def _lookup_account(session: IamSession, token: str, username: str) -> dict | None:
    try:
        body = session.iam(
            "POST",
            "/v1/platform/accounts/lookup",
            token,
            json_body={"purpose": "ACCOUNT_MANAGE", "username": username},
        )
    except TransportError as exc:
        if exc.status in {403, 404}:
            return None
        raise ConfigError(f"查找账号 {username} 失败：{exc}") from exc
    found = resource_identity(body)
    if found:
        found["username"] = username
    return found


def _revision_conflict(exc: TransportError) -> bool:
    return exc.status == 409 or "RevisionConflict" in exc.body


def _account_detail(body: dict) -> dict | None:
    found = resource_identity(body)
    if not found:
        return None
    data = payload_data(body)
    record = data.get("record") if isinstance(data.get("record"), dict) else data
    if isinstance(record, dict):
        if record.get("username"):
            found["username"] = record["username"]
        if "enabled" in record:
            found["enabled"] = record["enabled"]
    return found


def _refresh_account(session: IamSession, token: str, account: dict) -> dict:
    try:
        body = session.iam("GET", f"/v1/platform/accounts/{account['id']}", token)
    except TransportError as exc:
        raise ConfigError(f"读取账号 {account.get('username') or account['id']} 失败：{exc}") from exc
    found = _account_detail(body)
    if not found:
        raise ConfigError(f"账号 {account.get('username') or account['id']} 详情无效")
    account["version"] = found["version"]
    if found.get("username"):
        account["username"] = found["username"]
    if "enabled" in found:
        account["enabled"] = found["enabled"]
    return account


def _refresh_member(
    session: IamSession,
    token: str,
    member: dict,
    *,
    domain: str = "platform",
    tenant_id: str | None = None,
) -> dict:
    path = (
        f"/v1/platform/members/{member['id']}"
        if domain == "platform"
        else f"/v1/tenant/members/{member['id']}"
    )
    try:
        body = session.iam("GET", path, token, tenant=tenant_id)
    except TransportError as exc:
        raise ConfigError(f"读取成员 {member['id']} 失败：{exc}") from exc
    data = payload_data(body)
    parsed = _unwrap_item(data) or _unwrap_item(data.get("record") if isinstance(data.get("record"), dict) else {})
    if not parsed:
        raise ConfigError(f"成员 {member['id']} 详情无效")
    member["version"] = parsed["version"]
    record = data.get("record") if isinstance(data.get("record"), dict) else data
    if isinstance(record, dict) and record.get("status"):
        member["status"] = record["status"]
    return member


def _submit_expected(
    session: IamSession,
    token: str,
    method: str,
    path: str,
    resource: dict,
    extra: dict,
    refresh,
    label: str,
    tenant: str | None = None,
) -> dict:
    refresh(resource)
    payload = {"expectedVersion": str(resource.get("version") or "0"), **extra}

    def send() -> dict:
        payload["expectedVersion"] = str(resource.get("version") or "0")
        return session.iam(method, path, token, json_body=payload, tenant=tenant)

    try:
        body = send()
    except TransportError as exc:
        if not _revision_conflict(exc):
            raise ConfigError(f"{label}失败：{exc}") from exc
        refresh(resource)
        try:
            body = send()
        except TransportError as retry_exc:
            raise ConfigError(f"{label}失败：{retry_exc}") from retry_exc
    try:
        resource["version"] = created_resource(body)["version"]
    except TransportError:
        refresh(resource)
    return body


def _reset_secret(
    session: IamSession,
    token: str,
    account: dict,
    identity: str,
    secrets: dict[str, str],
) -> None:
    _refresh_account(session, token, account)
    if identity in secrets:
        return

    def send():
        return session.iam(
            "POST",
            f"/v1/platform/accounts/{account['id']}/reset-password",
            token,
            json_body={"expectedVersion": str(account.get("version") or "0")},
        )

    try:
        reset = send()
    except TransportError as exc:
        if not _revision_conflict(exc):
            raise ConfigError(f"重置账号 {account.get('username') or identity} 口令失败：{exc}") from exc
        _refresh_account(session, token, account)
        try:
            reset = send()
        except TransportError as retry_exc:
            raise ConfigError(
                f"重置账号 {account.get('username') or identity} 口令失败：{retry_exc}"
            ) from retry_exc
    password = payload_data(reset).get("password")
    if not isinstance(password, str) or not password:
        raise ConfigError(f"重置 {identity} 口令未返回一次性密码")
    secrets[identity] = password
    _refresh_account(session, token, account)


def _ensure_account(
    session: IamSession,
    token: str,
    objects: dict,
    identity: str,
    secrets: dict[str, str],
    checkpoint,
) -> str:
    key = f"account:{identity}"
    current = objects.get(key)
    if isinstance(current, dict) and current.get("id"):
        _reset_secret(session, token, current, identity, secrets)
        checkpoint()
        return "reused"
    username = username_of(identity)
    existing = _lookup_account(session, token, username)
    if existing:
        objects[key] = existing
        _reset_secret(session, token, existing, identity, secrets)
        checkpoint()
        return "reused"
    try:
        created = created_resource(
            session.iam(
                "POST",
                "/v1/platform/accounts",
                token,
                json_body={"username": username, "phone": None, "email": None},
            )
        )
    except TransportError as exc:
        raise ConfigError(f"创建账号 {username} 失败：{exc}") from exc
    objects[key] = {"id": created["id"], "username": username, "version": created["version"]}
    checkpoint()
    _reset_secret(session, token, objects[key], identity, secrets)
    checkpoint()
    return "created"


def _list_or_empty(session: IamSession, path: str, token: str, tenant: str | None = None) -> list:
    try:
        return page_records(session.iam("GET", path, token, tenant=tenant))
    except TransportError:
        return []


def _unwrap_item(item: dict) -> dict | None:
    if not isinstance(item, dict):
        return None
    record = item.get("record") if isinstance(item.get("record"), dict) else item
    if not isinstance(record, dict) or not record.get("id"):
        return None
    result = {
        "id": str(record["id"]),
        "version": resource_version(item if item.get("version") is not None else record),
    }
    for key in ("name", "displayName", "code", "domain"):
        if record.get(key) is not None:
            result[key] = record[key]
    for key in ("ownerMemberId", "parentId"):
        if record.get(key) is not None and record.get(key) != "":
            result[key] = str(record[key])
    return result


def _page_details(
    session: IamSession, path: str, token: str, tenant: str | None = None
) -> list[dict]:
    try:
        body = session.iam("GET", path, token, tenant=tenant)
    except TransportError:
        return []
    data = payload_data(body)
    items = data.get("items") or data.get("records") or []
    if not isinstance(items, list):
        return []
    found = []
    for item in items:
        parsed = _unwrap_item(item)
        if parsed:
            found.append(parsed)
    return found


def _named(items: list[dict], name: str, field: str = "name") -> dict | None:
    for item in items:
        if item.get(field) == name:
            return item
    return None


def _tenant_detail(session: IamSession, token: str, tenant_id: str) -> dict:
    try:
        body = session.iam("GET", f"/v1/platform/tenants/{tenant_id}", token)
    except TransportError as exc:
        raise ConfigError(f"读取组织 {tenant_id} 失败：{exc}") from exc
    data = payload_data(body)
    record = data.get("record") if isinstance(data.get("record"), dict) else data
    parsed = _unwrap_item(record if isinstance(record, dict) else {})
    if not parsed:
        parsed = _unwrap_item(data)
    if not parsed:
        raise ConfigError(f"组织 {tenant_id} 详情无效")
    owner = parsed.get("ownerMemberId") or (
        str(record["ownerMemberId"]) if isinstance(record, dict) and record.get("ownerMemberId") else None
    )
    if not owner:
        raise ConfigError(f"组织 {tenant_id} 缺少所有者成员")
    parsed["ownerMemberId"] = owner
    return parsed


def _ensure_tenant(
    session: IamSession,
    token: str,
    objects: dict,
    tenant_key: str,
    owner_key: str,
    name: str,
    checkpoint,
) -> str:
    object_key = f"tenant:{tenant_key}"
    current = objects.get(object_key)
    if isinstance(current, dict) and current.get("id"):
        if not current.get("ownerMemberId"):
            detail = _tenant_detail(session, token, current["id"])
            current["ownerMemberId"] = detail["ownerMemberId"]
            if detail.get("name"):
                current["name"] = detail["name"]
            checkpoint()
        return "reused"
    existing = _named(_page_details(session, "/v1/platform/tenants?page=1&pageSize=100", token), name)
    if existing:
        owner_member_id = existing.get("ownerMemberId")
        if not owner_member_id:
            owner_member_id = _tenant_detail(session, token, existing["id"])["ownerMemberId"]
        objects[object_key] = {
            "id": existing["id"],
            "name": name,
            "ownerMemberId": owner_member_id,
        }
        checkpoint()
        return "reused"
    owner = _require(objects, f"account:{owner_key}")
    try:
        created_tenant = created_resource(
            session.iam(
                "POST",
                "/v1/platform/tenants",
                token,
                json_body={
                    "name": name,
                    "ownerAccountId": owner["id"],
                    "ownerDisplayName": owner_key,
                    "rootDepartmentName": "根部门",
                },
            )
        )
    except TransportError as exc:
        existing = _named(_page_details(session, "/v1/platform/tenants?page=1&pageSize=100", token), name)
        if existing:
            owner_member_id = existing.get("ownerMemberId") or _tenant_detail(
                session, token, existing["id"]
            )["ownerMemberId"]
            objects[object_key] = {
                "id": existing["id"],
                "name": name,
                "ownerMemberId": owner_member_id,
            }
            checkpoint()
            return "reused"
        raise ConfigError(f"创建组织 {name} 失败：{exc}") from exc
    detail = _tenant_detail(session, token, created_tenant["id"])
    objects[object_key] = {
        "id": created_tenant["id"],
        "name": name,
        "ownerMemberId": detail["ownerMemberId"],
    }
    checkpoint()
    return "created"


def _pick_actions(session: IamSession, token: str) -> tuple[str, str]:
    apps = _list_or_empty(session, "/v1/platform/applications?page=1&pageSize=100", token)
    app = next((item for item in apps if item.get("code") == "iam-tenant"), None)
    if app is None:
        app = next((item for item in apps if item.get("domain") == "TENANT"), None)
    if app is None:
        raise ConfigError("没有租户域应用 iam-tenant，无法创建共享角色")
    actions = _list_or_empty(
        session, f"/v1/platform/applications/{app['id']}/actions?page=1&pageSize=100", token
    )
    if not actions:
        raise ConfigError("租户域应用没有可用操作，无法创建共享角色")
    by_code = {str(item.get("code") or ""): item for item in actions}
    picked: list[str] = []
    for code in ("iam-tenant:member:read", "iam-tenant:member:update"):
        item = by_code.get(code)
        if item and item.get("id"):
            picked.append(str(item["id"]))
    for item in actions:
        action_id = str(item.get("id") or "")
        if action_id and action_id not in picked:
            picked.append(action_id)
        if len(picked) >= 2:
            break
    if not picked:
        raise ConfigError("租户域应用没有可用操作，无法创建共享角色")
    return picked[0], picked[1] if len(picked) > 1 else picked[0]


def _ensure_departments(
    session: IamSession,
    token: str,
    objects: dict,
    tenant_key: str,
    tenant_id: str,
) -> str:
    records = _page_details(
        session,
        "/v1/tenant/departments?purpose=MANAGED_DEPARTMENT&page=1&pageSize=100",
        token,
        tenant=tenant_id,
    )
    root_key = f"dept:{tenant_key}:root"
    if not (isinstance(objects.get(root_key), dict) and objects[root_key].get("id")):
        root = _named(records, "根部门") or (records[0] if records else None)
        if not root:
            raise ConfigError(f"组织 {tenant_key} 没有根部门")
        objects[root_key] = {"id": str(root["id"]), "name": root.get("name") or "根部门"}
        status = "created"
    else:
        status = "reused"
    parent = objects[root_key]["id"]
    children = (
        ("rd", "研发", parent),
        ("sales", "销售", parent),
        ("empty", "空部门", parent),
    )

    def _remember(item: dict) -> None:
        records.append(item)

    for suffix, name, parent_id in children:
        key = f"dept:{tenant_key}:{suffix}"
        if isinstance(objects.get(key), dict) and objects[key].get("id"):
            continue
        existing = _named(records, name)
        if existing:
            objects[key] = {"id": existing["id"], "name": name}
            continue
        created = created_resource(
            session.iam(
                "POST",
                "/v1/tenant/departments",
                token,
                json_body={"name": name, "parentId": parent_id, "sortOrder": 10},
                tenant=tenant_id,
            )
        )
        objects[key] = {"id": created["id"], "name": name}
        _remember({"id": created["id"], "name": name, "parentId": parent_id})
        status = "created"
    child_key = f"dept:{tenant_key}:rd-child"
    if not (isinstance(objects.get(child_key), dict) and objects[child_key].get("id")):
        existing_child = _named(records, "研发下级")
        if existing_child:
            objects[child_key] = {"id": existing_child["id"], "name": "研发下级"}
        else:
            created = created_resource(
                session.iam(
                    "POST",
                    "/v1/tenant/departments",
                    token,
                    json_body={
                        "name": "研发下级",
                        "parentId": objects[f"dept:{tenant_key}:rd"]["id"],
                        "sortOrder": 10,
                    },
                    tenant=tenant_id,
                )
            )
            objects[child_key] = {"id": created["id"], "name": "研发下级"}
            status = "created"
    return status


def _member_list_path(domain: str) -> str:
    return "/v1/platform/members" if domain == "platform" else "/v1/tenant/members"


def _lookup_member(
    session: IamSession,
    token: str,
    domain: str,
    tenant_id: str | None,
    display_name: str,
) -> dict | None:
    path = f"{_member_list_path(domain)}?page=1&pageSize=100"
    tenant = None if domain == "platform" else tenant_id
    found = _named(_page_details(session, path, token, tenant=tenant), display_name, "displayName")
    if not found:
        return None
    return {"id": found["id"], "version": found.get("version") or "0"}


def _ensure_member(
    session: IamSession,
    token: str,
    objects: dict,
    *,
    domain: str,
    identity: str,
    tenant_id: str | None,
    departments: list[dict],
    owner_identity: str | None = None,
) -> str:
    key = f"member:{domain}:{identity}"
    if isinstance(objects.get(key), dict) and objects[key].get("id"):
        return "reused"
    if owner_identity and identity == owner_identity:
        tenant = _require(objects, f"tenant:{domain}")
        owner_member_id = tenant.get("ownerMemberId")
        if not owner_member_id:
            raise ConfigError(f"组织 {domain} 缺少所有者成员，无法复用 {identity}")
        listed = _lookup_member(session, token, domain, tenant_id, identity)
        objects[key] = {
            "id": str(owner_member_id),
            "version": (listed or {}).get("version") or "0",
        }
        return "reused"
    existing = _lookup_member(session, token, domain, tenant_id, identity)
    if existing:
        objects[key] = existing
        return "reused"
    account = _require(objects, f"account:{identity}")
    path = _member_list_path(domain)
    try:
        created = created_resource(
            session.iam(
                "POST",
                path,
                token,
                json_body={
                    "accountId": account["id"],
                    "displayName": identity,
                    "departments": departments,
                },
                tenant=tenant_id,
            )
        )
    except TransportError as exc:
        if exc.status == 400:
            existing = _lookup_member(session, token, domain, tenant_id, identity)
            if existing:
                objects[key] = existing
                return "reused"
        raise ConfigError(f"创建成员 {identity} 失败：{exc}") from exc
    objects[key] = {"id": created["id"], "version": created["version"]}
    return "created"


def _ensure_group(
    session: IamSession,
    token: str,
    objects: dict,
    key: str,
    path: str,
    name: str,
    json_body: dict,
    tenant: str | None = None,
) -> str:
    if isinstance(objects.get(key), dict) and objects[key].get("id"):
        return "reused"
    existing = _named(_page_details(session, f"{path}?page=1&pageSize=100", token, tenant=tenant), name)
    if existing:
        objects[key] = {"id": existing["id"], "name": name}
        return "reused"
    try:
        created = created_resource(
            session.iam("POST", path, token, json_body=json_body, tenant=tenant)
        )
    except TransportError as exc:
        if exc.status == 400:
            existing = _named(
                _page_details(session, f"{path}?page=1&pageSize=100", token, tenant=tenant), name
            )
            if existing:
                objects[key] = {"id": existing["id"], "name": name}
                return "reused"
        raise ConfigError(f"创建组 {name} 失败：{exc}") from exc
    objects[key] = created | {"name": name}
    return "created"


def build(
    config: dict,
    run_id: str,
    *,
    state_override: Path | None = None,
    transport=None,
    getenv: Callable[[str], str | None] | None = None,
) -> dict:
    getenv = getenv or os.getenv
    inventory = prepare(config, run_id, state_override=state_override)
    inventory_file = inventory_path(config, run_id, state_override)
    secrets = _read_secrets(secrets_path(inventory_file))
    objects = inventory.setdefault("objects", {})
    created: list[str] = []
    reused: list[str] = []

    def mark(key: str, status: str) -> None:
        (reused if status == "reused" else created).append(key)

    def checkpoint() -> None:
        _write_secrets(secrets_path(inventory_file), secrets)
        write_inventory(inventory_file, inventory)

    session = IamSession(config, transport or HttpTransport(), getenv)
    governor_user = str(config.get("bootstrapUsername") or "platform")
    try:
        platform_token = session.login(
            "PLATFORM",
            governor_user,
            _password(getenv, config, "platform-governor", secrets),
        )
    except TransportError as exc:
        raise ConfigError(f"平台治理账号 {governor_user} 登录失败：{exc}") from exc

    for identity in CREATED_IDENTITIES:
        mark(
            f"account:{identity}",
            _ensure_account(session, platform_token, objects, identity, secrets, checkpoint),
        )

    for tenant_key, owner_key, name in (("a", "owner-a", "测试组织A"), ("b", "owner-b", "测试组织B")):
        mark(
            f"tenant:{tenant_key}",
            _ensure_tenant(session, platform_token, objects, tenant_key, owner_key, name, checkpoint),
        )

    tenant_a = _require(objects, "tenant:a")["id"]
    tenant_b = _require(objects, "tenant:b")["id"]
    try:
        owner_a_token = session.login(
            "TENANT",
            username_of("owner-a"),
            _password(getenv, config, "owner-a", secrets),
            org=tenant_a,
        )
        owner_b_token = session.login(
            "TENANT",
            username_of("owner-b"),
            _password(getenv, config, "owner-b", secrets),
            org=tenant_b,
        )
    except TransportError as exc:
        raise ConfigError(f"组织所有者登录失败：{exc}") from exc
    mark("dept:a", _ensure_departments(session, owner_a_token, objects, "a", tenant_a))
    mark("dept:b", _ensure_departments(session, owner_b_token, objects, "b", tenant_b))

    for identity in PLATFORM_MEMBERS:
        mark(
            f"member:platform:{identity}",
            _ensure_member(
                session,
                platform_token,
                objects,
                domain="platform",
                identity=identity,
                tenant_id=None,
                departments=[],
            ),
        )
    rd_a = _require(objects, "dept:a:rd")["id"]
    sales_a = _require(objects, "dept:a:sales")["id"]
    for identity in TENANT_A_MEMBERS:
        departments = []
        if identity == "multi-dept-a":
            departments = [{"id": rd_a, "primary": True}, {"id": sales_a, "primary": False}]
        elif identity not in {"no-dept-a", "no-access"}:
            departments = [{"id": rd_a, "primary": True}]
        mark(
            f"member:a:{identity}",
            _ensure_member(
                session,
                owner_a_token,
                objects,
                domain="a",
                identity=identity,
                tenant_id=tenant_a,
                departments=departments,
                owner_identity=TENANT_OWNERS["a"],
            ),
        )
    rd_b = _require(objects, "dept:b:rd")["id"]
    for identity in TENANT_B_MEMBERS:
        mark(
            f"member:b:{identity}",
            _ensure_member(
                session,
                owner_b_token,
                objects,
                domain="b",
                identity=identity,
                tenant_id=tenant_b,
                departments=[{"id": rd_b, "primary": True}],
                owner_identity=TENANT_OWNERS["b"],
            ),
        )

    suspended = objects.get("member:platform:suspended")
    if isinstance(suspended, dict) and suspended.get("id") and not suspended.get("statusPatched"):
        _refresh_member(session, platform_token, suspended)
        if suspended.get("status") != "SUSPENDED":
            _submit_expected(
                session,
                platform_token,
                "PATCH",
                f"/v1/platform/members/{suspended['id']}/status",
                suspended,
                {"status": "SUSPENDED"},
                lambda resource: _refresh_member(session, platform_token, resource),
                "暂停平台成员",
            )
        suspended["statusPatched"] = True
        suspended["status"] = "SUSPENDED"
        mark("member:platform:suspended:status", "created")
    disabled = objects.get("account:disabled")
    if isinstance(disabled, dict) and disabled.get("id") and not disabled.get("disabled"):
        _refresh_account(session, platform_token, disabled)
        if disabled.get("enabled") is not False:
            _submit_expected(
                session,
                platform_token,
                "POST",
                f"/v1/platform/accounts/{disabled['id']}/disable",
                disabled,
                {},
                lambda resource: _refresh_account(session, platform_token, resource),
                "停用账号",
            )
        disabled["disabled"] = True
        disabled["enabled"] = False
        mark("account:disabled:status", "created")

    mark(
        "group:platform",
        _ensure_group(
            session,
            platform_token,
            objects,
            "group:platform",
            "/v1/platform/groups",
            "平台测试组",
            {
                "name": "平台测试组",
                "selection": {
                    "members": [_require(objects, "member:platform:dual")["id"]],
                    "departments": [],
                },
            },
        ),
    )
    mark(
        "group:a",
        _ensure_group(
            session,
            owner_a_token,
            objects,
            "group:a",
            "/v1/tenant/groups",
            "研发组",
            {
                "name": "研发组",
                "selection": {
                    "members": [_require(objects, "member:a:ordinary-a")["id"]],
                    "departments": [{"id": rd_a, "includeDescendants": True}],
                },
            },
            tenant=tenant_a,
        ),
    )

    if not (isinstance(objects.get("role:shared"), dict) and objects["role:shared"].get("id")):
        role_code = f"iam-test-shared-{run_id}"
        existing_role = _named(
            _page_details(session, "/v1/platform/shared-roles?page=1&pageSize=100", platform_token),
            role_code,
            "code",
        )
        if existing_role:
            objects["role:shared"] = {
                "id": existing_role["id"],
                "revisionId": existing_role["id"],
                "kind": "SHARED",
            }
            revisions = page_records(
                session.iam(
                    "GET",
                    f"/v1/platform/shared-roles/{existing_role['id']}/revisions?page=1&pageSize=20",
                    platform_token,
                )
            )
            if revisions:
                objects["role:shared"]["revisionId"] = str(revisions[0]["id"])
            mark("role:shared", "reused")
        else:
            read_id, update_id = _pick_actions(session, platform_token)
            try:
                created_role = created_resource(
                    session.iam(
                        "POST",
                        "/v1/platform/shared-roles",
                        platform_token,
                        json_body={
                            "code": role_code,
                            "name": "测试共享成员读写",
                            "kind": "SHARED",
                            "definition": {
                                "grants": [
                                    {"actionId": read_id, "scopes": ALL_SCOPE},
                                    {"actionId": update_id, "scopes": ALL_SCOPE},
                                ],
                                "deltas": [],
                                "parameterDefinitions": [],
                            },
                        },
                    )
                )
            except TransportError as exc:
                existing_role = _named(
                    _page_details(session, "/v1/platform/shared-roles?page=1&pageSize=100", platform_token),
                    role_code,
                    "code",
                )
                if existing_role:
                    objects["role:shared"] = {
                        "id": existing_role["id"],
                        "revisionId": existing_role["id"],
                        "kind": "SHARED",
                    }
                    revisions = page_records(
                        session.iam(
                            "GET",
                            f"/v1/platform/shared-roles/{existing_role['id']}/revisions?page=1&pageSize=20",
                            platform_token,
                        )
                    )
                    if revisions:
                        objects["role:shared"]["revisionId"] = str(revisions[0]["id"])
                    mark("role:shared", "reused")
                else:
                    raise ConfigError(f"创建共享角色失败：{exc}") from exc
            else:
                revisions = page_records(
                    session.iam(
                        "GET",
                        f"/v1/platform/shared-roles/{created_role['id']}/revisions?page=1&pageSize=20",
                        platform_token,
                    )
                )
                revision_id = str(revisions[0]["id"]) if revisions else created_role["id"]
                objects["role:shared"] = {
                    "id": created_role["id"],
                    "revisionId": revision_id,
                    "kind": "SHARED",
                }
                mark("role:shared", "created")
    else:
        mark("role:shared", "reused")

    if not (isinstance(objects.get("assignment:a:ordinary-a"), dict) and objects["assignment:a:ordinary-a"].get("id")):
        shared = _require(objects, "role:shared")
        member = _require(objects, "member:a:ordinary-a")
        created_assignment = created_resource(
            session.iam(
                "POST",
                "/v1/tenant/assignments",
                owner_a_token,
                json_body={
                    "items": [
                        {
                            "subject": {"type": "MEMBER", "id": member["id"]},
                            "roleRevisionRef": {"kind": "SHARED", "id": shared.get("revisionId") or shared["id"]},
                            "scopeBindings": {},
                            "validFrom": None,
                            "validUntil": None,
                            "delegationGrantId": None,
                        }
                    ]
                },
                tenant=tenant_a,
            )
        )
        objects["assignment:a:ordinary-a"] = {"id": created_assignment["id"]}
        mark("assignment:a:ordinary-a", "created")
    else:
        mark("assignment:a:ordinary-a", "reused")

    policy = payload_data(
        session.iam("GET", "/v1/tenant/policies/directory", owner_a_token, tenant=tenant_a)
    )
    objects["policy:a:directory"] = {
        "id": str(policy.get("id") or "directory"),
        "version": str(policy.get("version") or "read"),
    }
    mark("policy:a:directory", "created")

    _write_secrets(secrets_path(inventory_file), secrets)
    write_inventory(inventory_file, inventory)
    dumped = json.dumps(inventory)
    if any(value and value in dumped for value in secrets.values()):
        raise ConfigError("口令不得写入运行清单")
    return {
        "ok": True,
        "runId": run_id,
        "environmentId": config["environmentId"],
        "created": sorted(set(created)),
        "reused": sorted(set(reused)),
        "objectCount": len(objects),
        "identities": report_identities(config),
        "secretsFile": str(secrets_path(inventory_file).name),
        "note": "一次性口令只写入同目录 secrets 文件，报告只含环境变量名；D03 才会补 250+ 成员与动态期限。",
    }
