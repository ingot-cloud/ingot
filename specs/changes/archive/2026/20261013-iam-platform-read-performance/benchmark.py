#!/usr/bin/env python3
"""Read-only benchmark through the original frontend proxy; never records credentials or response bodies."""
import argparse
import concurrent.futures
import hashlib
import http.client
import json
import math
import os
import threading
import time
from pathlib import Path
from urllib.parse import urlsplit


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--base-url", default="http://platform.localhost:5799")
    parser.add_argument("--connect-host", default="127.0.0.1")
    parser.add_argument("--role-id", default="1000214")
    parser.add_argument("--members-role-id", default="140001")
    parser.add_argument("--headers-file", type=Path, help="Private JSON object of existing authenticated request headers")
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--label", required=True)
    parser.add_argument("--require-target", action="store_true")
    args = parser.parse_args()
    base = urlsplit(args.base_url)
    if base.scheme != "http" or base.hostname not in {"platform.localhost", "localhost", "127.0.0.1"}:
        parser.error("Only the explicitly authorized local HTTP frontend is supported")
    if args.connect_host not in {"localhost", "127.0.0.1", "::1"}:
        parser.error("Connection host must be loopback")
    headers = json.loads(args.headers_file.read_text()) if args.headers_file else {}
    if "IAM_BENCH_COOKIE" in os.environ:
        headers["Cookie"] = os.environ["IAM_BENCH_COOKIE"]
    headers["Host"] = base.netloc
    prefix = "/api/iam/v1/platform"
    page = "?page=1&pageSize=20"
    paths = ["/members" + page, "/roles" + page, f"/roles/{args.members_role_id}/members" + page,
             "/assignments" + page, "/delegations" + page, f"/roles/{args.role_id}",
             f"/roles/{args.role_id}/grants", f"/roles/{args.role_id}/version-applications/distribution",
             f"/roles/{args.role_id}/revisions" + page, f"/roles/{args.role_id}/version-applications" + page]
    local = threading.local()
    connections = []
    lock = threading.Lock()

    def request(path):
        if not hasattr(local, "connection"):
            local.connection = http.client.HTTPConnection(args.connect_host, base.port or 80, timeout=15)
            with lock:
                connections.append(local.connection)
        connection = local.connection
        started = time.perf_counter()
        connection.request("GET", prefix + path, headers=headers)
        response = connection.getresponse()
        ttfb = (time.perf_counter() - started) * 1000
        body = response.read()
        if response.status != 200:
            raise RuntimeError(f"{path}: HTTP {response.status}; cannot benchmark an unauthenticated/error response")
        payload = json.loads(body)
        if str(payload.get("code")) != "0":
            raise RuntimeError(f"{path}: business error code {payload.get('code')}; rejected as a performance sample")
        data = payload.get("data")
        rows = len(data) if isinstance(data, list) else len(data.get("items", [])) if isinstance(data, dict) and "items" in data else None
        digest = hashlib.sha256(json.dumps(data, ensure_ascii=False, sort_keys=True).encode()).hexdigest()
        return {"ttfb_ms": ttfb, "response_sha256": digest, "rows": rows}

    report = {"label": args.label, "entry": args.base_url, "warmup": 20, "samples": 100, "rounds": 3,
              "target_p95_ms": 200, "ttfb_definition": "GET send through response headers, excludes body download", "results": []}
    try:
        for iteration in range(1, 4):
            for concurrency in (1, 3):
                with concurrent.futures.ThreadPoolExecutor(max_workers=concurrency) as pool:
                    for path in paths:
                        cold = request(path) if iteration == 1 and concurrency == 1 else None
                        list(pool.map(request, [path] * 20))
                        samples = list(pool.map(request, [path] * 100))
                        values = sorted(sample["ttfb_ms"] for sample in samples)
                        row = {"path": path, "round": iteration, "concurrency": concurrency,
                               "p50_ms": round(values[math.ceil(len(values) * .5) - 1], 3),
                               "p95_ms": round(values[math.ceil(len(values) * .95) - 1], 3),
                               "max_ms": round(values[-1], 3), "samples": samples}
                        if cold:
                            row["first_unwarmed_request_ms"] = round(cold["ttfb_ms"], 3)
                        report["results"].append(row)
                        args.output.write_text(json.dumps(report, ensure_ascii=False, indent=2))
                        print(json.dumps({key: value for key, value in row.items() if key != "samples"}, ensure_ascii=False), flush=True)
        report["all_p95_below_target"] = all(row["p95_ms"] < 200 for row in report["results"])
        args.output.write_text(json.dumps(report, ensure_ascii=False, indent=2))
        return 1 if args.require_target and not report["all_p95_below_target"] else 0
    finally:
        for connection in connections:
            connection.close()


if __name__ == "__main__":
    raise SystemExit(main())
