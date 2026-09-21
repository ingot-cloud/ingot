"""stdlib HTTP 客户端。不把 Authorization、密码写入报告。"""

from __future__ import annotations

import json
from base64 import b64encode
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
    """基于 urllib 的 JSON/表单传输。"""

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
        if not isinstance(parsed, dict):
            raise TransportError(0, url, "响应不是 JSON 对象")
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
