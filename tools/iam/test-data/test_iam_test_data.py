#!/usr/bin/env python3
"""测试数据 D01：缺配置失败、登记校验、重复 prepare 复用、漂移拒绝、reset 限制。"""

from __future__ import annotations

import json
import sys
import tempfile
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parent
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

import build
import client
import iam_test_data
import lib


def sample_config(state_dir: Path, **overrides) -> dict:
    data = {
        "environmentId": "iam-test-unit",
        "registered": True,
        "independent": True,
        "gatewayBaseUrl": "http://localhost:7980",
        "authBaseUrl": "http://localhost:5100",
        "bffBaseUrl": "http://localhost:5400",
        "database": {"host": "127.0.0.1", "port": 3306, "name": "iam_test", "user": "iam"},
        "redisNamespace": "iam-test",
        "sites": {
            "tenantAdmin": "http://tenant.localhost:5798",
            "tenantLogin": "http://tenant-login.localhost:1798",
            "platformAdmin": "http://platform.localhost:5799",
            "platformLogin": "http://platform-login.localhost:1799",
        },
        "bootstrapUsername": "platform",
        "oauth": {"clientId": "ingot", "clientSecret": "ingot"},
        "credentialEnv": {"platform-governor": "IAM_TEST_PASSWORD_PLATFORM_GOVERNOR"},
        "stateDir": str(state_dir),
    }
    data.update(overrides)
    return data


class TestDataToolTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.state = Path(self.temp.name) / "runs"
        self.config_path = Path(self.temp.name) / "env.json"
        self.config = sample_config(self.state)
        self.config_path.write_text(json.dumps(self.config))

    def tearDown(self):
        self.temp.cleanup()

    def test_missing_config_fails(self):
        with self.assertRaises(lib.ConfigError):
            lib.load_config(Path(self.temp.name) / "missing.json")

    def test_unregistered_environment_is_rejected(self):
        self.config["registered"] = False
        self.config_path.write_text(json.dumps(self.config))
        with self.assertRaises(lib.ConfigError):
            lib.load_config(self.config_path)

    def test_shared_dev_database_is_rejected(self):
        self.config["independent"] = False
        self.config_path.write_text(json.dumps(self.config))
        with self.assertRaises(lib.ConfigError):
            lib.load_config(self.config_path)

    def test_prepare_reuses_same_run_and_keeps_objects(self):
        first = lib.prepare(self.config, "run-a", state_override=self.state)
        path = lib.inventory_path(self.config, "run-a", self.state)
        first["objects"]["tenant-a"] = {"id": "keep-me"}
        lib.write_inventory(path, first)
        second = lib.prepare(self.config, "run-a", state_override=self.state)
        self.assertEqual(second["objects"]["tenant-a"]["id"], "keep-me")
        self.assertEqual(first["t0"], second["t0"])

    def test_fingerprint_drift_requires_reset(self):
        lib.prepare(self.config, "run-b", state_override=self.state)
        drifted = dict(self.config)
        drifted["redisNamespace"] = "other"
        with self.assertRaises(lib.ConfigError) as ctx:
            lib.prepare(drifted, "run-b", state_override=self.state)
        self.assertIn("漂移", str(ctx.exception))

    def test_reset_requires_confirm_and_only_clears_inventory(self):
        lib.prepare(self.config, "run-c", state_override=self.state)
        with self.assertRaises(lib.ConfigError):
            lib.reset(self.config, "run-c", confirm=False, state_override=self.state)
        result = lib.reset(self.config, "run-c", confirm=True, state_override=self.state)
        self.assertTrue(result["ok"])
        self.assertFalse(result["ddlResetApplied"])
        self.assertFalse(lib.inventory_path(self.config, "run-c", self.state).exists())

    def test_verify_and_cli_help(self):
        lib.prepare(self.config, "run-d", state_override=self.state)
        report = lib.verify(self.config, "run-d", state_override=self.state)
        self.assertTrue(report["ok"])
        self.assertIn("TD01", report["scenarios"])
        self.assertEqual(report["identities"]["platform-governor"], "IAM_TEST_PASSWORD_PLATFORM_GOVERNOR")
        self.assertEqual(iam_test_data.main(["help"]), 0)
        with self.assertRaises(SystemExit) as ctx:
            iam_test_data.main(["prepare"])
        self.assertEqual(ctx.exception.code, 2)

    def test_cli_prepare_verify(self):
        code = iam_test_data.main(
            ["prepare", "--config", str(self.config_path), "--run-id", "cli-1"]
        )
        self.assertEqual(code, 0)
        code = iam_test_data.main(
            ["verify", "--config", str(self.config_path), "--run-id", "cli-1"]
        )
        self.assertEqual(code, 0)

    def test_build_reuses_inventory_and_hides_passwords(self):
        lib.prepare(self.config, "run-e", state_override=self.state)
        transport = FakeGateway()
        env = {
            "IAM_TEST_PASSWORD_PLATFORM_GOVERNOR": "governor-secret",
        }
        first = build.build(
            self.config,
            "run-e",
            state_override=self.state,
            transport=transport,
            getenv=env.get,
        )
        self.assertTrue(first["ok"])
        self.assertIn("account:owner-a", first["created"])
        self.assertNotIn("governor-secret", json.dumps(first))
        inventory = lib.read_inventory(lib.inventory_path(self.config, "run-e", self.state))
        dumped = json.dumps(inventory)
        self.assertNotIn("governor-secret", dumped)
        self.assertNotIn("once-password", dumped)
        self.assertIn("tenant:a", inventory["objects"])
        account_posts = [
            item for item in transport.calls if item["method"] == "POST" and item["path"].endswith("/platform/accounts")
        ]
        first_count = len(account_posts)
        self.assertGreater(first_count, 0)
        self.assertEqual(account_posts[0]["json"]["username"], "iam-test-platform-reader")
        self.assertIn("phone", account_posts[0]["json"])
        second = build.build(
            self.config,
            "run-e",
            state_override=self.state,
            transport=transport,
            getenv=env.get,
        )
        self.assertIn("account:owner-a", second["reused"])
        after = [
            item for item in transport.calls if item["method"] == "POST" and item["path"].endswith("/platform/accounts")
        ]
        self.assertEqual(len(after), first_count)

    def test_build_reuses_existing_remote_username(self):
        lib.prepare(self.config, "run-f", state_override=self.state)
        transport = FakeGateway()
        transport.existing_usernames.add("iam-test-platform-reader")
        env = {"IAM_TEST_PASSWORD_PLATFORM_GOVERNOR": "governor-secret"}
        result = build.build(
            self.config,
            "run-f",
            state_override=self.state,
            transport=transport,
            getenv=env.get,
        )
        self.assertIn("account:platform-reader", result["reused"])
        created_names = [
            item["json"]["username"]
            for item in transport.calls
            if item["method"] == "POST"
            and item["path"].endswith("/platform/accounts")
            and item.get("json")
        ]
        self.assertNotIn("iam-test-platform-reader", created_names)

    def test_build_reuses_owner_member_instead_of_posting(self):
        lib.prepare(self.config, "run-g", state_override=self.state)
        transport = FakeGateway()
        env = {"IAM_TEST_PASSWORD_PLATFORM_GOVERNOR": "governor-secret"}
        result = build.build(
            self.config,
            "run-g",
            state_override=self.state,
            transport=transport,
            getenv=env.get,
        )
        self.assertTrue(result["ok"])
        self.assertIn("member:a:owner-a", result["reused"])
        self.assertIn("member:b:owner-b", result["reused"])
        owner_posts = [
            item
            for item in transport.calls
            if item["method"] == "POST"
            and str(item["path"]).endswith("/members")
            and (item.get("json") or {}).get("displayName") in {"owner-a", "owner-b"}
        ]
        self.assertEqual(owner_posts, [])
        inventory = lib.read_inventory(lib.inventory_path(self.config, "run-g", self.state))
        self.assertTrue(inventory["objects"]["tenant:a"].get("ownerMemberId"))
        self.assertEqual(
            inventory["objects"]["member:a:owner-a"]["id"],
            inventory["objects"]["tenant:a"]["ownerMemberId"],
        )

    def test_build_reuses_existing_remote_members_and_tenants(self):
        lib.prepare(self.config, "run-h", state_override=self.state)
        transport = FakeGateway()
        transport.seed_tenant("测试组织A", "owner-a")
        transport.seed_tenant("测试组织B", "owner-b")
        transport.seed_member("platform", None, "platform-reader")
        transport.seed_member("platform", None, "dual")
        transport.seed_member("platform", None, "suspended")
        env = {"IAM_TEST_PASSWORD_PLATFORM_GOVERNOR": "governor-secret"}
        result = build.build(
            self.config,
            "run-h",
            state_override=self.state,
            transport=transport,
            getenv=env.get,
        )
        self.assertIn("tenant:a", result["reused"])
        self.assertIn("tenant:b", result["reused"])
        self.assertIn("member:platform:platform-reader", result["reused"])
        tenant_posts = [
            item for item in transport.calls if item["method"] == "POST" and str(item["path"]).endswith("/tenants")
        ]
        self.assertEqual(tenant_posts, [])
        platform_member_posts = [
            item
            for item in transport.calls
            if item["method"] == "POST" and str(item["path"]).endswith("/platform/members")
        ]
        self.assertEqual(platform_member_posts, [])

    def test_reset_secret_refreshes_version_when_secrets_exist(self):
        transport = FakeGateway()
        account = transport.seed_account("iam-test-owner-a", version="3")
        session = build.IamSession(self.config, transport, lambda _: None)
        target = {"id": account["id"], "username": "iam-test-owner-a", "version": "0"}
        build._reset_secret(session, "token", target, "owner-a", {"owner-a": "keep"})
        self.assertEqual(target["version"], "3")
        self.assertEqual(
            [
                item
                for item in transport.calls
                if item["method"] == "POST" and str(item["path"]).endswith("/reset-password")
            ],
            [],
        )

    def test_disable_retries_revision_conflict(self):
        transport = FakeGateway()
        transport.force_conflicts = 1
        account = transport.seed_account("iam-test-disabled", version="1")
        session = build.IamSession(self.config, transport, lambda _: None)
        target = {"id": account["id"], "username": "iam-test-disabled", "version": "0"}
        build._submit_expected(
            session,
            "token",
            "POST",
            f"/v1/platform/accounts/{account['id']}/disable",
            target,
            {},
            lambda resource: build._refresh_account(session, "token", resource),
            "停用账号",
        )
        self.assertFalse(transport.accounts[account["id"]]["enabled"])
        self.assertEqual(target["version"], "2")

    def test_build_persists_disabled_account_version(self):
        lib.prepare(self.config, "run-i", state_override=self.state)
        transport = FakeGateway()
        result = build.build(
            self.config,
            "run-i",
            state_override=self.state,
            transport=transport,
            getenv={"IAM_TEST_PASSWORD_PLATFORM_GOVERNOR": "governor-secret"}.get,
        )
        self.assertTrue(result["ok"])
        inventory = lib.read_inventory(lib.inventory_path(self.config, "run-i", self.state))
        disabled = inventory["objects"]["account:disabled"]
        self.assertTrue(disabled.get("disabled"))
        self.assertNotEqual(str(disabled.get("version")), "0")

    def test_build_shared_role_uses_tenant_member_actions(self):
        lib.prepare(self.config, "run-j", state_override=self.state)
        transport = FakeGateway()
        result = build.build(
            self.config,
            "run-j",
            state_override=self.state,
            transport=transport,
            getenv={"IAM_TEST_PASSWORD_PLATFORM_GOVERNOR": "governor-secret"}.get,
        )
        self.assertTrue(result["ok"])
        posts = [
            item
            for item in transport.calls
            if item["method"] == "POST" and str(item["path"]).endswith("/shared-roles")
        ]
        self.assertEqual(len(posts), 1)
        grants = posts[0]["json"]["definition"]["grants"]
        self.assertEqual({item["actionId"] for item in grants}, {"act-read", "act-update"})
        for grant in grants:
            self.assertEqual(grant["scopes"], [{"kind": "ALL"}])


class FakeGateway:
    def __init__(self):
        self.calls = []
        self.seq = 100
        self.existing_usernames: set[str] = set()
        self.accounts: dict[str, dict] = {}
        self.force_conflicts = 0
        self.tenants: dict[str, dict] = {}
        self.members: dict[tuple[str, str | None], dict[str, dict]] = {}
        self.departments: dict[str, list[dict]] = {}
        self.groups: dict[tuple[str, str | None], dict[str, dict]] = {}
        self.roles: dict[str, dict] = {}

    def _next_id(self) -> str:
        self.seq += 1
        return str(self.seq)

    def _page(self, records: list[dict]) -> dict:
        return {
            "data": {
                "items": [{"record": item, "version": item.get("version") or "0"} for item in records],
                "total": len(records),
                "page": 1,
                "pageSize": 100,
            }
        }

    def _maybe_conflict(self, url: str) -> None:
        if self.force_conflicts > 0:
            self.force_conflicts -= 1
            raise client.TransportError(
                409, url, '{"code":"RevisionConflict","message":"配置已变化，请重新预览后提交"}'
            )

    def _bump(self, value: str) -> str:
        try:
            return str(int(value) + 1)
        except (TypeError, ValueError):
            return "1"

    def _put_account(self, username: str, version: str = "0") -> dict:
        account = {
            "id": self._next_id(),
            "username": username,
            "version": version,
            "enabled": True,
        }
        self.accounts[account["id"]] = account
        return account

    def _account_by_username(self, username: str | None) -> dict | None:
        if not username:
            return None
        for account in self.accounts.values():
            if account.get("username") == username:
                return account
        return None

    def _account_record(self, account: dict) -> dict:
        return {
            "id": account["id"],
            "username": account["username"],
            "enabled": account.get("enabled", True),
            "locked": False,
            "mustChangePassword": True,
        }

    def _find_member_by_id(self, member_id: str) -> dict | None:
        for bucket in self.members.values():
            for member in bucket.values():
                if member.get("id") == member_id:
                    return member
        return None

    def seed_account(self, username: str, version: str = "0") -> dict:
        existing = self._account_by_username(username)
        if existing:
            existing["version"] = version
            return existing
        return self._put_account(username, version)

    def seed_tenant(self, name: str, owner_display: str) -> dict:
        tenant_id = self._next_id()
        owner_member_id = self._next_id()
        tenant = {
            "id": tenant_id,
            "name": name,
            "ownerMemberId": owner_member_id,
            "version": "1",
        }
        self.tenants[tenant_id] = tenant
        self.members.setdefault(("tenant", tenant_id), {})[owner_display] = {
            "id": owner_member_id,
            "version": "0",
            "displayName": owner_display,
        }
        self.departments[tenant_id] = [{"id": "root-" + tenant_id, "name": "根部门"}]
        return tenant

    def seed_member(self, domain: str, tenant_id: str | None, display_name: str) -> dict:
        member = {"id": self._next_id(), "version": "0", "displayName": display_name}
        self.members.setdefault((domain, tenant_id), {})[display_name] = member
        return member

    def _member_bucket(self, path: str, headers: dict | None) -> dict[str, dict]:
        if "/platform/members" in path:
            return self.members.setdefault(("platform", None), {})
        tenant = (headers or {}).get("Tenant")
        return self.members.setdefault(("tenant", tenant), {})

    def _group_bucket(self, path: str, headers: dict | None) -> dict[str, dict]:
        if "/platform/groups" in path:
            return self.groups.setdefault(("platform", None), {})
        tenant = (headers or {}).get("Tenant")
        return self.groups.setdefault(("tenant", tenant), {})

    def request(self, method, url, *, headers=None, json_body=None, form=None, basic=None, timeout=30):
        path = url.split("?", 1)[0]
        path = path[path.find("/oauth2") :] if "/oauth2" in path else path[path.find("/iam") + 4 :]
        self.calls.append({"method": method, "path": path, "json": json_body, "form": form})
        if path.endswith("/oauth2/token"):
            return {"data": {"accessToken": "token"}}
        if method == "POST" and path.endswith("/accounts/lookup"):
            username = (json_body or {}).get("username")
            account = self._account_by_username(username)
            if account is None and username in self.existing_usernames:
                account = self._put_account(username, "1")
            if account is None:
                raise client.TransportError(
                    404, url, '{"code":"ObjectNotFound","message":"对象不存在或不可访问"}'
                )
            return {
                "data": {
                    "record": self._account_record(account),
                    "version": account["version"],
                }
            }
        if method == "POST" and path.endswith("/reset-password"):
            account_id = path.rsplit("/", 2)[-2]
            account = self.accounts.get(account_id)
            if account is None:
                raise client.TransportError(
                    404, url, '{"code":"ObjectNotFound","message":"对象不存在或不可访问"}'
                )
            expected = str((json_body or {}).get("expectedVersion") or "")
            self._maybe_conflict(url)
            if expected != str(account["version"]):
                raise client.TransportError(
                    409, url, '{"code":"RevisionConflict","message":"配置已变化，请重新预览后提交"}'
                )
            account["version"] = self._bump(account["version"])
            return {"data": {"password": "once-password"}}
        if method == "POST" and path.endswith("/disable"):
            account_id = path.rsplit("/", 2)[-2]
            account = self.accounts.get(account_id)
            if account is None:
                raise client.TransportError(
                    404, url, '{"code":"ObjectNotFound","message":"对象不存在或不可访问"}'
                )
            expected = str((json_body or {}).get("expectedVersion") or "")
            self._maybe_conflict(url)
            if expected != str(account["version"]):
                raise client.TransportError(
                    409, url, '{"code":"RevisionConflict","message":"配置已变化，请重新预览后提交"}'
                )
            account["version"] = self._bump(account["version"])
            account["enabled"] = False
            return {"data": {"id": account["id"], "version": account["version"]}}
        if method == "POST" and path.endswith("/accounts"):
            username = (json_body or {}).get("username")
            if self._account_by_username(username):
                raise client.TransportError(
                    400, url, '{"code":"InvalidArgument","message":"登录名已存在"}'
                )
            account = self._put_account(username, "0")
            return {"data": {"id": account["id"], "version": account["version"]}}
        if method == "GET" and "/platform/accounts/" in path:
            account_id = path.rsplit("/", 1)[-1]
            account = self.accounts.get(account_id)
            if account is None:
                raise client.TransportError(
                    404, url, '{"code":"ObjectNotFound","message":"对象不存在或不可访问"}'
                )
            return {
                "data": {
                    "record": self._account_record(account),
                    "version": account["version"],
                }
            }
        if method == "PATCH" and path.endswith("/status"):
            member_id = path.rsplit("/", 2)[-2]
            member = self._find_member_by_id(member_id)
            if member is None:
                raise client.TransportError(
                    404, url, '{"code":"ObjectNotFound","message":"对象不存在或不可访问"}'
                )
            expected = str((json_body or {}).get("expectedVersion") or "")
            self._maybe_conflict(url)
            if expected != str(member.get("version") or "0"):
                raise client.TransportError(
                    409, url, '{"code":"RevisionConflict","message":"配置已变化，请重新预览后提交"}'
                )
            member["version"] = self._bump(str(member.get("version") or "0"))
            member["status"] = (json_body or {}).get("status")
            return {"data": {"id": member["id"], "version": member["version"]}}
        if method == "GET" and path.endswith("/applications"):
            return self._page(
                [
                    {"id": "app-platform", "code": "iam-platform", "domain": "PLATFORM"},
                    {"id": "app-tenant", "code": "iam-tenant", "domain": "TENANT"},
                ]
            )
        if method == "GET" and path.endswith("/actions"):
            app_id = path.rsplit("/", 2)[-2]
            if app_id == "app-platform":
                return self._page(
                    [
                        {"id": "plat-read", "code": "iam-platform:account:read"},
                        {"id": "plat-update", "code": "iam-platform:account:update"},
                    ]
                )
            return self._page(
                [
                    {"id": "act-read", "code": "iam-tenant:member:read"},
                    {"id": "act-update", "code": "iam-tenant:member:update"},
                ]
            )
        if method == "GET" and path.endswith("/departments"):
            tenant = (headers or {}).get("Tenant") or ""
            items = self.departments.get(tenant) or [{"id": "root", "name": "根部门"}]
            if tenant and tenant not in self.departments:
                self.departments[tenant] = list(items)
            return self._page(items)
        if method == "POST" and path.endswith("/departments"):
            tenant = (headers or {}).get("Tenant") or ""
            name = (json_body or {}).get("name")
            bucket = self.departments.setdefault(tenant, [{"id": "root", "name": "根部门"}])
            existing = next((item for item in bucket if item.get("name") == name), None)
            if existing:
                raise client.TransportError(
                    400, url, '{"code":"InvalidArgument","message":"请求参数不合法"}'
                )
            created = {
                "id": self._next_id(),
                "name": name,
                "parentId": (json_body or {}).get("parentId"),
                "version": "1",
            }
            bucket.append(created)
            return {"data": {"id": created["id"], "version": "1"}}
        if method == "GET" and "revisions" in path:
            return self._page(
                [
                    {
                        "id": "rev1",
                        "revision": "1",
                        "kind": "SHARED",
                        "grants": [],
                        "deltas": [],
                        "parameterDefinitions": [],
                    }
                ]
            )
        if method == "GET" and path.endswith("/shared-roles"):
            return self._page(list(self.roles.values()))
        if method == "GET" and path.endswith("/groups"):
            return self._page(list(self._group_bucket(path, headers).values()))
        if method == "GET" and path.endswith("/members"):
            return self._page(list(self._member_bucket(path, headers).values()))
        if method == "GET" and "/members/" in path:
            member_id = path.rsplit("/", 1)[-1]
            member = self._find_member_by_id(member_id)
            if member is None:
                raise client.TransportError(
                    404, url, '{"code":"ObjectNotFound","message":"对象不存在或不可访问"}'
                )
            record = {
                **member,
                "status": member.get("status") or "ACTIVE",
                "departments": member.get("departments") or [],
            }
            return {"data": {"record": record, "version": member.get("version") or "0"}}
        if method == "GET" and path.endswith("/tenants"):
            return self._page(list(self.tenants.values()))
        if method == "GET" and "/platform/tenants/" in path:
            tenant_id = path.rsplit("/", 1)[-1]
            tenant = self.tenants.get(tenant_id)
            if not tenant:
                raise client.TransportError(
                    404, url, '{"code":"ObjectNotFound","message":"对象不存在或不可访问"}'
                )
            return {"data": {"record": tenant, "version": tenant.get("version") or "1"}}
        if method == "POST" and path.endswith("/tenants"):
            name = (json_body or {}).get("name")
            existing = next((item for item in self.tenants.values() if item.get("name") == name), None)
            if existing:
                raise client.TransportError(
                    400, url, '{"code":"InvalidArgument","message":"请求参数不合法"}'
                )
            tenant_id = self._next_id()
            owner_member_id = self._next_id()
            owner_display = (json_body or {}).get("ownerDisplayName") or "owner"
            tenant = {
                "id": tenant_id,
                "name": name,
                "ownerMemberId": owner_member_id,
                "version": "1",
            }
            self.tenants[tenant_id] = tenant
            self.members.setdefault(("tenant", tenant_id), {})[owner_display] = {
                "id": owner_member_id,
                "version": "0",
                "displayName": owner_display,
            }
            self.departments[tenant_id] = [{"id": "root-" + tenant_id, "name": "根部门"}]
            return {"data": {"id": tenant_id, "version": "1"}}
        if method == "POST" and path.endswith("/members"):
            display = (json_body or {}).get("displayName")
            bucket = self._member_bucket(path, headers)
            if display in bucket:
                raise client.TransportError(
                    400, url, '{"code":"InvalidArgument","message":"成员资格已存在"}'
                )
            member = {"id": self._next_id(), "version": "0", "displayName": display}
            bucket[display] = member
            return {"data": {"id": member["id"], "version": "0"}}
        if method == "POST" and path.endswith("/groups"):
            name = (json_body or {}).get("name")
            bucket = self._group_bucket(path, headers)
            if name in bucket:
                raise client.TransportError(
                    400, url, '{"code":"InvalidArgument","message":"请求参数不合法"}'
                )
            group = {"id": self._next_id(), "name": name, "version": "1"}
            bucket[name] = group
            return {"data": {"id": group["id"], "version": "1"}}
        if method == "POST" and path.endswith("/shared-roles"):
            code = (json_body or {}).get("code")
            if code in {item.get("code") for item in self.roles.values()}:
                raise client.TransportError(
                    400, url, '{"code":"InvalidArgument","message":"请求参数不合法"}'
                )
            role = {
                "id": self._next_id(),
                "code": code,
                "name": (json_body or {}).get("name"),
                "kind": "SHARED",
                "version": "1",
            }
            self.roles[role["id"]] = role
            return {"data": {"id": role["id"], "version": "1"}}
        if method == "GET":
            return {"data": {"id": "policy", "version": "1"}}
        return {"data": {"id": self._next_id(), "version": "1", "password": "once-password"}}


if __name__ == "__main__":
    unittest.main()
