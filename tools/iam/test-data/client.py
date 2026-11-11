"""stdlib HTTP 客户端。不把 Authorization、密码写入报告。"""

from __future__ import annotations

import json
import re
import secrets
from base64 import b64decode, b64encode
from time import time_ns
from urllib.error import HTTPError, URLError
from urllib.parse import urlencode, urlparse
from urllib.request import Request, urlopen


class TransportError(RuntimeError):
    """远端 HTTP 调用失败。"""

    def __init__(self, status: int, url: str, body: str):
        self.status = status
        self.url = url
        self.body = body
        super().__init__(f"HTTP {status} {urlparse(url).path}: {body[:300]}")


class HttpTransport:
    """基于 urllib 的 JSON/表单传输，敏感 IAM 接口使用当前 HYBRID 信封。"""

    def __init__(self, gateway: str):
        self.gateway = gateway
        self.key_url = f"{gateway}/crypto/public-keys"

    def request(
        self,
        method: str,
        url: str,
        *,
        headers: dict | None = None,
        json_body: dict | None = None,
        form: dict | None = None,
        basic: tuple[str, str] | None = None,
        timeout: int = 30,
    ) -> dict:
        path = urlparse(url.removeprefix(self.gateway)).path
        encrypted_response = method == "POST" and bool(re.fullmatch(
            r"/iam/v1/(?:platform/accounts(?:/[^/]+/reset-password)?|tenant/members)", path
        ))
        encrypted_request = method == "PUT" and path == "/iam/v1/me/password"
        if encrypted_response or encrypted_request:
            parsed = self._hybrid(method, url, headers, json_body, timeout, encrypted_request)
        else:
            parsed = self._send(method, url, headers, json_body, form, basic, timeout)
        if not isinstance(parsed, dict):
            raise TransportError(0, url, "响应不是 JSON 对象")
        return parsed

    def _hybrid(self, method, url, headers, json_body, timeout, encrypted_request):
        try:
            from cryptography.exceptions import InvalidTag
            from cryptography.hazmat.primitives import hashes, serialization
            from cryptography.hazmat.primitives.asymmetric import padding
            from cryptography.hazmat.primitives.ciphers.aead import AESGCM
        except ImportError as exc:
            raise TransportError(0, url, "IAM 凭证接口需要 Python cryptography 依赖") from exc

        keys = self.request("GET", self.key_url, timeout=timeout).get("data")
        key = next((item for item in keys if item.get("active")), None) if isinstance(keys, list) else None
        if not key or key.get("alg") != "RSA-OAEP-256":
            raise TransportError(0, self.key_url, "未返回可用的 HYBRID 活跃公钥")
        cek = secrets.token_bytes(32)
        nonce = b64encode(secrets.token_bytes(16)).decode()
        timestamp = str(time_ns() // 1_000_000)
        kid = key["kid"]
        aad = f"h1|{kid}|{nonce}|{timestamp}".encode()
        public_key = serialization.load_der_public_key(b64decode(key["publicKey"], validate=True))
        wrapped = public_key.encrypt(cek, padding.OAEP(
            mgf=padding.MGF1(hashes.SHA256()), algorithm=hashes.SHA256(), label=None
        ))
        crypto_headers = dict(headers or {}) | {
            "In-Crypto-Md": "h1", "In-Crypto-Kv": kid, "In-Crypto-Sk": b64encode(wrapped).decode(),
            "In-Crypto-No": nonce, "In-Crypto-Ts": timestamp,
        }
        aes = AESGCM(cek)
        if encrypted_request:
            iv = secrets.token_bytes(12)
            encoded = iv + aes.encrypt(iv, json.dumps(json_body).encode(), aad)
            json_body = {"data": b64encode(encoded).decode()}
        result = self._send(method, url, crypto_headers, json_body, None, None, timeout)
        if encrypted_request or (isinstance(result, dict) and result.get("success") is False):
            return result
        try:
            ciphertext = result["data"] if isinstance(result, dict) else result
            encoded = b64decode(ciphertext, validate=True)
            decoded = json.loads(aes.decrypt(encoded[:12], encoded[12:], aad))
        except (InvalidTag, KeyError, TypeError, ValueError) as exc:
            raise TransportError(0, url, "HYBRID 响应解密或解析失败") from exc
        return result | {"data": decoded} if isinstance(result, dict) else decoded

    def _send(self, method, url, headers, json_body, form, basic, timeout):
        payload = None
        request_headers = dict(headers or {})
        if form is not None:
            payload = urlencode(form).encode()
            request_headers["Content-Type"] = "application/x-www-form-urlencoded"
        elif json_body is not None:
            payload = json.dumps(json_body).encode()
            request_headers["Content-Type"] = "application/json"
        if basic:
            token = b64encode(f"{basic[0]}:{basic[1]}".encode()).decode()
            request_headers["Authorization"] = f"Basic {token}"
        request = Request(url, data=payload, headers=request_headers, method=method)
        try:
            with urlopen(request, timeout=timeout) as response:
                raw = response.read().decode()
        except HTTPError as exc:
            raw = exc.read().decode() if exc.fp else ""
            raise TransportError(exc.code, url, raw) from exc
        except URLError as exc:
            raise TransportError(0, url, str(exc.reason)) from exc
        if not raw:
            return {}
        try:
            parsed = json.loads(raw)
        except json.JSONDecodeError as exc:
            raise TransportError(0, url, raw) from exc
        return parsed


def payload_data(body: dict) -> dict:
    data = body.get("data")
    return data if isinstance(data, dict) else {}


def resource_version(data: dict) -> str:
    version = data.get("version")
    if version is None or version == "":
        return "0"
    return str(version)


def created_resource(body: dict) -> dict:
    data = payload_data(body)
    resource_id = data.get("id")
    if not resource_id:
        raise TransportError(0, "", "响应缺少 data.id")
    return {
        "id": str(resource_id),
        "version": resource_version(data),
    }


def resource_identity(body: dict) -> dict | None:
    data = payload_data(body)
    record = data.get("record") if isinstance(data.get("record"), dict) else data
    if not isinstance(record, dict) or not record.get("id"):
        return None
    return {
        "id": str(record["id"]),
        "username": record.get("username"),
        "version": resource_version(data if data.get("version") is not None else record),
    }


def page_records(body: dict) -> list:
    data = payload_data(body)
    items = data.get("items") or data.get("records") or []
    if not isinstance(items, list):
        return []
    records = []
    for item in items:
        if isinstance(item, dict) and isinstance(item.get("record"), dict):
            records.append(item["record"])
        elif isinstance(item, dict):
            records.append(item)
    return records
