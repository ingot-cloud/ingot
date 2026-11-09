#!/usr/bin/env python3
"""Aggregate sanitized IAM access records; never emit raw requests or revision tokens."""
import argparse
import csv
import json
import math
import re
from collections import Counter, defaultdict
from datetime import datetime, timezone
from pathlib import Path


BOOTSTRAP = "/v1/me/bootstrap"
PLATFORM = "/v1/platform/applications/{id}/action-catalog"
TENANT = "/v1/tenant/applications/{id}/action-catalog"
ROUTES = (BOOTSTRAP, PLATFORM, TENANT)
CATALOG_PATH = re.compile(r"/v1/(platform|tenant)/applications/([1-9][0-9]*|\{id\})/action-catalog")


def instant(value):
    parsed = datetime.fromisoformat(value.replace("Z", "+00:00"))
    if parsed.utcoffset() is None:
        raise ValueError("timestamp must include a time zone")
    return parsed.astimezone(timezone.utc)


def route_for(path):
    if path == BOOTSTRAP:
        return BOOTSTRAP, None
    match = CATALOG_PATH.fullmatch(path)
    if not match:
        return None, None
    view, app_id = match.groups()
    return (PLATFORM if view == "platform" else TENANT), (None if app_id == "{id}" else app_id)


def route_metrics(seconds, minutes, statuses, window_minutes):
    nonzero = sorted(minutes.values())

    def percentile(q):
        rank = math.ceil(q * window_minutes) - (window_minutes - len(nonzero))
        return nonzero[rank - 1] / 60 if rank > 0 else 0

    return {
        "requests": sum(statuses.values()),
        "http_status_class": dict(sorted(statuses.items())),
        "active_minutes": len(nonzero),
        "minute_qps_p50": percentile(0.50),
        "minute_qps_p95": percentile(0.95),
        "peak_second_qps": max(seconds.values(), default=0),
        "peak_minute_qps": max(nonzero, default=0) / 60,
    }


def load_inventory(path):
    if path is None:
        return None
    required = {"observed_at", "application_id", "application_code", "domain",
                "application_enabled", "platform_actions", "tenant_actions"}
    apps = {}
    sampled_at = None
    with path.open(encoding="utf-8", newline="") as source:
        reader = csv.DictReader(source, delimiter="\t")
        if not required.issubset(reader.fieldnames or ()):
            raise ValueError("inventory must use the column names from inventory.sql")
        for row in reader:
            try:
                observed_at = instant(row["observed_at"])
                app_id = row["application_id"]
                platform, tenant = int(row["platform_actions"]), int(row["tenant_actions"])
                if (not re.fullmatch(r"[1-9][0-9]*", app_id) or app_id in apps
                        or not 0 <= tenant <= platform or row["application_enabled"] not in {"0", "1"}
                        or (sampled_at is not None and observed_at != sampled_at)):
                    raise ValueError
            except (ValueError, TypeError):
                raise ValueError(f"invalid inventory row at line {reader.line_num}") from None
            sampled_at = observed_at
            apps[app_id] = {
                "application_code": row["application_code"],
                "domain": row["domain"],
                "enabled": row["application_enabled"] == "1",
                "platform_actions": platform,
                "tenant_actions": tenant,
            }
    return {"observed_at": sampled_at.isoformat() if sampled_at else None, "applications": apps}


def summarize(path, environment, start, end, inventory):
    window_minutes = int((end - start).total_seconds()) // 60
    if (window_minutes <= 0 or any(t.second or t.microsecond for t in (start, end))):
        raise ValueError("start and end must be increasing, aligned to UTC minute boundaries")
    seconds, minutes, statuses = (defaultdict(Counter) for _ in range(3))
    catalog_requests, catalog_success, revisions = Counter(), Counter(), defaultdict(Counter)
    with path.open(encoding="utf-8", newline="") as source:
        reader = csv.DictReader(source)
        if not {"timestamp", "method", "path", "status"}.issubset(reader.fieldnames or ()):
            raise ValueError("access CSV must include timestamp, method, path and status")
        for row in reader:
            route, app_id = route_for((row["path"] or "").partition("?")[0])
            if row["method"] != "GET" or route is None:
                continue
            try:
                time = instant(row["timestamp"])
                status = int(row["status"])
                if not 100 <= status <= 599:
                    raise ValueError
            except (ValueError, TypeError, AttributeError):
                raise ValueError(f"invalid IAM access row at line {reader.line_num}") from None
            if not start <= time < end:
                continue
            epoch = int(time.timestamp())
            seconds[route][epoch] += 1
            minutes[route][epoch // 60] += 1
            statuses[route][f"{status // 100}xx"] += 1
            if route == BOOTSTRAP:
                continue
            key = (route, app_id)
            catalog_requests[key] += 1
            if status == 200:
                catalog_success[key] += 1
                revision = (row.get("catalog_revision") or "").strip()
                if app_id is not None and revision:
                    revisions[key][revision] += 1

    catalog = {route: {} for route in (PLATFORM, TENANT)}
    for key, requests in sorted(catalog_requests.items(), key=lambda item: (item[0][0], item[0][1] or "")):
        route, app_id = key
        versioned = sum(revisions[key].values())
        successful = catalog_success[key]
        catalog[route][app_id or "unknown_application"] = {
            "requests": requests,
            "successful_reads": successful,
            "mean_successful_qps": successful / (window_minutes * 60),
            "versioned_successful_reads": versioned,
            "unversioned_successful_reads": successful - versioned,
            "version_coverage": versioned / successful if successful else None,
            "observed_versions": len(revisions[key]) if versioned else None,
            "confirmed_repeated_reads": versioned - len(revisions[key]) if versioned else None,
        }
    return {
        "environment": environment,
        "window": {"start_inclusive": start.isoformat(), "end_exclusive": end.isoformat(),
                   "complete_minutes": window_minutes},
        "routes": {route: route_metrics(seconds[route], minutes[route], statuses[route], window_minutes)
                   for route in ROUTES},
        "action_catalog_by_application": catalog,
        "inventory": load_inventory(inventory),
    }


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--access", type=Path, required=True, help="sanitized IAM access CSV")
    parser.add_argument("--inventory", type=Path, help="TSV result of inventory.sql")
    parser.add_argument("--environment", required=True)
    parser.add_argument("--start", required=True, help="inclusive timestamp with time zone")
    parser.add_argument("--end", required=True, help="exclusive timestamp with time zone")
    args = parser.parse_args()
    try:
        report = summarize(args.access, args.environment, instant(args.start), instant(args.end), args.inventory)
    except (OSError, ValueError) as exc:
        parser.error(str(exc))
    print(json.dumps(report, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
