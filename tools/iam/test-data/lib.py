"""IAM 独立测试环境：配置、清单、指纹与命令。不连接未登记目标，不把凭证写入报告。"""

from __future__ import annotations

import hashlib
import json
from datetime import datetime, timezone
from pathlib import Path
from urllib.parse import urlparse

ROOT = Path(__file__).resolve().parents[3]
DDL_DIRECTORY = ROOT / "databases" / "iam"
FRAMEWORK_LOCK_DDL = (
    ROOT / "ingot-framework/ingot-security/ingot-security-account"
    / "ingot-security-account-adapter/src/main/resources/sql/account_lock_state.sql"
)
FRAMEWORK_PASSWORD_DDL = (
    ROOT / "ingot-framework/ingot-security/ingot-security-credential-data"
    / "src/main/resources/sql/add_password_history.sql"
)
REQUIRED_CONFIG = (
    "environmentId",
    "registered",
    "independent",
    "gatewayBaseUrl",
    "authBaseUrl",
    "bffBaseUrl",
    "database",
    "redisNamespace",
    "sites",
)
REQUIRED_SITES = ("tenantAdmin", "tenantLogin", "platformAdmin", "platformLogin")
REQUIRED_DATABASE = ("host", "port", "name", "user")
SCENARIO_CATALOG = Path(__file__).resolve().parent / "scenarios.json"
SCENE_VERSION = json.loads(SCENARIO_CATALOG.read_text())["sceneVersion"]


class ConfigError(ValueError):
    """配置缺失、非法或不允许对当前目标操作。"""


def ddl_files():
    """权威初始化顺序：001–005 → 007 → 框架 DDL → 006。不含人工种子。"""
    numbered = sorted(DDL_DIRECTORY.glob("[0-9][0-9][0-9]_*.sql"))
    identity = [item for item in numbered if item.name != "006_bootstrap.sql"]
    bootstrap = DDL_DIRECTORY / "006_bootstrap.sql"
    return identity + [FRAMEWORK_LOCK_DDL, FRAMEWORK_PASSWORD_DDL, bootstrap]


def load_config(path: Path) -> dict:
    if not path.is_file():
        raise ConfigError(f"配置文件不存在: {path}")
    try:
        data = json.loads(path.read_text())
    except json.JSONDecodeError as exc:
        raise ConfigError(f"配置不是合法 JSON: {exc}") from exc
    if not isinstance(data, dict):
        raise ConfigError("配置根必须是对象")
    missing = [key for key in REQUIRED_CONFIG if key not in data]
    if missing:
        raise ConfigError("配置缺少: " + ", ".join(missing))
    if not data["environmentId"] or not isinstance(data["environmentId"], str):
        raise ConfigError("environmentId 必须是非空字符串")
    if data.get("registered") is not True:
        raise ConfigError("environmentId 未登记，拒绝操作")
    if data.get("independent") is not True:
        raise ConfigError("目标不是独立测试环境，拒绝操作")
    database = data["database"]
    if not isinstance(database, dict):
        raise ConfigError("database 必须是对象")
    db_missing = [key for key in REQUIRED_DATABASE if key not in database]
    if db_missing:
        raise ConfigError("database 缺少: " + ", ".join(db_missing))
    sites = data["sites"]
    if not isinstance(sites, dict):
        raise ConfigError("sites 必须是对象")
    site_missing = [key for key in REQUIRED_SITES if not sites.get(key)]
    if site_missing:
        raise ConfigError("sites 缺少: " + ", ".join(site_missing))
    for name, url in [
        ("gatewayBaseUrl", data["gatewayBaseUrl"]),
        ("authBaseUrl", data["authBaseUrl"]),
        ("bffBaseUrl", data["bffBaseUrl"]),
        *sites.items(),
    ]:
        parsed = urlparse(str(url))
        if parsed.scheme not in {"http", "https"} or not parsed.netloc:
            raise ConfigError(f"{name} 不是合法 URL")
    if not str(data["redisNamespace"]).strip():
        raise ConfigError("redisNamespace 不能为空")
    return data


def fingerprint(config: dict) -> str:
    payload = {
        "environmentId": config["environmentId"],
        "gatewayBaseUrl": config["gatewayBaseUrl"],
        "authBaseUrl": config["authBaseUrl"],
        "bffBaseUrl": config["bffBaseUrl"],
        "database": {
            "host": config["database"]["host"],
            "port": config["database"]["port"],
            "name": config["database"]["name"],
        },
        "redisNamespace": config["redisNamespace"],
        "sites": config["sites"],
        "ddl": [str(path.relative_to(ROOT)) for path in ddl_files()],
    }
    encoded = json.dumps(payload, sort_keys=True, separators=(",", ":")).encode()
    return hashlib.sha256(encoded).hexdigest()


def state_dir(config: dict, override: Path | None = None) -> Path:
    if override is not None:
        return override
    raw = config.get("stateDir") or "./runs"
    path = Path(raw)
    if not path.is_absolute():
        path = Path(__file__).resolve().parent / path
    return path


def inventory_path(config: dict, run_id: str, override: Path | None = None) -> Path:
    safe = "".join(ch if ch.isalnum() or ch in "-_" else "_" for ch in run_id)
    return state_dir(config, override) / config["environmentId"] / f"{safe}.json"


def utc_now() -> str:
    return datetime.now(timezone.utc).replace(microsecond=0).isoformat().replace("+00:00", "Z")


def new_inventory(config: dict, run_id: str) -> dict:
    return {
        "sceneVersion": SCENE_VERSION,
        "runId": run_id,
        "environmentId": config["environmentId"],
        "t0": utc_now(),
        "fingerprint": fingerprint(config),
        "ddl": [str(path.relative_to(ROOT)) for path in ddl_files()],
        "objects": {},
        "reports": [],
    }


def read_inventory(path: Path) -> dict:
    if not path.is_file():
        raise ConfigError(f"运行清单不存在: {path}")
    data = json.loads(path.read_text())
    if not isinstance(data, dict) or "runId" not in data:
        raise ConfigError("运行清单损坏")
    return data


def write_inventory(path: Path, inventory: dict) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(inventory, ensure_ascii=False, indent=2) + "\n")


def credential_names(config: dict) -> dict[str, str]:
    mapping = config.get("credentialEnv") or {}
    if not isinstance(mapping, dict):
        raise ConfigError("credentialEnv 必须是对象")
    names = {}
    for identity, env_name in mapping.items():
        if not isinstance(env_name, str) or not env_name:
            raise ConfigError(f"身份 {identity} 的环境变量名非法")
        names[str(identity)] = env_name
    return names


def report_identities(config: dict) -> dict[str, str]:
    """只报告用途与环境变量名，不读取、不打印密码。"""
    return credential_names(config)


def prepare(config: dict, run_id: str, *, state_override: Path | None = None) -> dict:
    path = inventory_path(config, run_id, state_override)
    current = fingerprint(config)
    if path.is_file():
        inventory = read_inventory(path)
        if inventory.get("fingerprint") != current:
            raise ConfigError(
                "配置或 DDL 指纹已漂移，拒绝覆盖现有清单；请带 --confirm-reset 执行 reset 后重新 prepare"
            )
        if inventory.get("runId") != run_id:
            raise ConfigError("清单 runId 与参数不一致")
        return inventory
    inventory = new_inventory(config, run_id)
    write_inventory(path, inventory)
    return inventory


def verify(config: dict, run_id: str, *, state_override: Path | None = None) -> dict:
    path = inventory_path(config, run_id, state_override)
    inventory = read_inventory(path)
    current = fingerprint(config)
    if inventory.get("fingerprint") != current:
        raise ConfigError("清单指纹与当前配置不一致，需要 rebuild")
    missing = [item for item in inventory.get("ddl", []) if not (ROOT / item).is_file()]
    if missing:
        raise ConfigError("DDL 文件缺失: " + ", ".join(missing))
    catalog = json.loads(SCENARIO_CATALOG.read_text())["scenarios"]
    return {
        "ok": True,
        "runId": run_id,
        "environmentId": config["environmentId"],
        "objectCount": len(inventory.get("objects") or {}),
        "scenarios": catalog,
        "identities": report_identities(config),
    }


def reset(config: dict, run_id: str, *, confirm: bool, state_override: Path | None = None) -> dict:
    if not confirm:
        raise ConfigError("reset 必须显式 --confirm-reset")
    path = inventory_path(config, run_id, state_override)
    existed = path.is_file()
    if existed:
        path.unlink()
    return {
        "ok": True,
        "clearedInventory": existed,
        "environmentId": config["environmentId"],
        "runId": run_id,
        "ddlResetApplied": False,
        "ddlOrder": [str(item.relative_to(ROOT)) for item in ddl_files()],
        "note": "D01 只清除已登记环境的运行清单；不对任意开发库执行 DROP。DDL 重建由运维按 ddlOrder 显式执行。",
    }


def help_text() -> str:
    return """IAM 独立测试数据工具（测试数据 D01/D02）

用法:
  python3 tools/iam/test-data/iam_test_data.py prepare --config <file> --run-id <id>
  python3 tools/iam/test-data/iam_test_data.py build   --config <file> --run-id <id>
  python3 tools/iam/test-data/iam_test_data.py verify  --config <file> --run-id <id>
  python3 tools/iam/test-data/iam_test_data.py reset   --config <file> --run-id <id> --confirm-reset

配置必须指向已登记的独立测试环境。缺失字段立即失败，不猜测开发库。
同一 runId 的 prepare 复用清单，不覆盖测试人员已写入的 objects。
build 通过真实 /iam/v1 接口创建账号/组织/部门/成员/组/共享角色/分配，已有清单对象会跳过。
一次性口令只写入同目录 .secrets.json，stdout 报告不含密码。
指纹漂移时拒绝静默改回，需 reset 后重建。
凭证只通过环境变量名引用，报告不打印密码。
"""
