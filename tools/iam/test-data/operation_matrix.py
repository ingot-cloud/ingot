"""Generate the IAM acceptance crosswalk from the current contracts."""

from __future__ import annotations

import argparse
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[3]
CHANGE = ROOT / "specs/changes/archive/2026/20260912-iam-identity-access-management"
CONTRACTS = CHANGE / "contracts"
OUTPUT = CHANGE / "TD-OPERATIONS.md"

# Endpoint family -> scenario and frontend acceptance work area.
FAMILIES = {
    "me": ("TD02 / TD16 / TD17", "双端会话与个人设置"),
    "assets/uploads": ("TD16", "成员与个人设置的图片上传"),
    "platform/accounts": ("TD02 / TD16", "平台全局账号"),
    "platform/actions": ("TD04 / TD13", "平台应用及授权操作选择器"),
    "platform/applications": ("TD03 / TD04 / TD11", "平台应用、资源、操作和菜单"),
    "platform/assignments": ("TD06 / TD09 / TD12", "平台角色与授权 / 角色分配"),
    "platform/authorization": ("TD13", "平台权限诊断和审计"),
    "platform/delegations": ("TD09 / TD12", "平台角色与授权 / 授权管理员"),
    "platform/dictionaries": ("TD16", "平台字典"),
    "platform/groups": ("TD09", "平台人员 / 用户组"),
    "platform/id-allocations": ("TD16", "平台业务 ID 发号"),
    "platform/members": ("TD02 / TD07 / TD09 / TD11", "平台人员 / 成员"),
    "platform/plans": ("TD03", "平台应用套餐"),
    "platform/roles": ("TD06 / TD09 / TD11", "平台角色与授权 / 固定版本"),
    "platform/shared-roles": ("TD05 / TD11", "平台共享角色"),
    "platform/social-configs": ("TD16", "平台社会化配置"),
    "platform/tenants": ("TD02 / TD03", "平台租户及应用开通"),
    "tenant/applications": ("TD03 / TD04", "组织应用与应用人群"),
    "tenant/assignments": ("TD05 / TD09 / TD12", "组织角色与授权 / 角色分配"),
    "tenant/authorization": ("TD13", "组织权限诊断和审计"),
    "tenant/delegations": ("TD09 / TD12", "组织角色与授权 / 授权管理员"),
    "tenant/departments": ("TD08", "组织部门"),
    "tenant/downloads": ("TD14", "组织下载中心"),
    "tenant/groups": ("TD09", "组织用户组"),
    "tenant/member-picker": ("TD07 / TD08 / TD09", "组织成员选择器"),
    "tenant/members": ("TD07 / TD08 / TD11", "组织成员"),
    "tenant/policies": ("TD11", "组织资源字段策略"),
    "tenant/roles": ("TD05 / TD09 / TD11", "组织角色与授权 / 当前权限"),
    "tenant/settings": ("TD15", "组织设置"),
    "tenant/workbench": ("TD04", "组织工作台"),
}


def operation_area(path: str) -> tuple[str, str]:
    """Resolve a published route to its current acceptance area."""
    if path.startswith("/v1/tenant/members/export"):
        return "TD14", "组织成员 JSON 导出"
    if path.startswith("/v1/tenant/members/workbook"):
        return "TD07 / TD11 / TD14", "组织成员工作簿"
    if path.startswith("/v1/tenant/departments/workbook"):
        return "TD08 / TD14", "组织部门工作簿"
    if "/version-applications" in path:
        return "TD06 / TD18", "平台角色版本应用"
    segments = path.removeprefix("/v1/").split("/")
    family = "me" if segments[0] == "me" else "/".join(segments[:2])
    return FAMILIES[family]


def render() -> str:
    """Require an explicit acceptance area for every public route."""
    public = json.loads((CONTRACTS / "routes.json").read_text())
    internal = json.loads((CONTRACTS / "internal-openapi.json").read_text())["paths"]
    lines = [
        "# IAM 现行接口与测试场景",
        "",
        "由 `tools/iam/test-data/operation_matrix.py` 从现行契约生成。",
        "各行登记操作归属、前端能力入口及场景。运行结果见 [验收矩阵](./ACCEPTANCE.md)。",
        "双端同源登录单列 TD17。独立的 Auth、Security、Member、OSS 服务按各自契约归入 TD16 或 TD17。",
        "",
        f"## 公开操作（{len(public)}）",
        "",
        "| 操作标识 | 方法与路径 | ACTION | TD | 页面或调用入口 |",
        "|---|---|---|---|---|",
    ]
    for route in sorted(public, key=lambda r: (r["path"], r["method"])):
        path = route["path"]
        scenarios, page = operation_area(path)
        action = f'`{route["action"]}`' if route["action"] else "按接口执行条件"
        lines.append(f'| `{route["operationId"]}` | `{route["method"].upper()} {path}` | '
                     f'{action} | {scenarios} | {page} |')

    lines += [
        "",
        "## 内部授权与资源操作",
        "",
        "| 操作标识 | 方法与路径 | TD | 调用入口 |",
        "|---|---|---|---|",
    ]
    for path, operations in sorted(internal.items()):
        for method, operation in operations.items():
            lines.append(f'| `{operation["operationId"]}` | `{method.upper()} {path}` | '
                         'TD16 / TD18 | 服务消费者 SDK |')
    return "\n".join(lines) + "\n"


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--check", action="store_true", help="check the committed crosswalk")
    args = parser.parse_args()
    content = render()
    if args.check:
        if not OUTPUT.is_file() or OUTPUT.read_text() != content:
            parser.exit(1, f"{OUTPUT.name} needs regeneration\n")
    else:
        OUTPUT.write_text(content)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
