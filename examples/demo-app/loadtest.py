#!/usr/bin/env python3
"""demo-app HTTP 压测脚本（宿主机或容器均可对准目标）。

用法：python3 loadtest.py --host 127.0.0.1 --port 8080 --scenario success --duration 20 --threads 16
场景：success=GET /user/1；failure=GET /stock?remain=0（业务异常路径）；mixed=8:2 混合。
输出：QPS、P50/P95/P99 客户端延迟（ms）、错误率。keep-alive 长连接压测。
"""
import argparse
import http.client
import json
import threading
import time
from concurrent.futures import ThreadPoolExecutor


class Runner(threading.Thread):
    def __init__(self, host, port, scenario, stop_event, bucket):
        super().__init__(daemon=True)
        self.host, self.port, self.scenario = host, port, scenario
        self.stop_event, self.bucket = stop_event, bucket

    def pick_path(self, seq):
        if self.scenario == "success":
            return "/user/1"
        if self.scenario == "failure":
            return "/stock?remain=0"
        return "/user/1" if seq % 10 < 8 else "/stock?remain=0"  # 读写约 8:2

    def run(self):
        conn = http.client.HTTPConnection(self.host, self.port, timeout=10)
        seq = 0
        while not self.stop_event.is_set():
            seq += 1
            path = self.pick_path(seq)
            # mixed 场景的失败路径是刻意打出来的业务失败包络，属正常响应，不计错误
            failure_expected = path.startswith("/stock")
            start = time.perf_counter()
            ok = False
            try:
                conn.request("GET", path)
                resp = conn.getresponse()
                body = resp.read()
                payload = json.loads(body)
                ok = resp.status == 200 and (payload.get("code") == 0 if not failure_expected
                                             else "code" in payload and "message" in payload)
            except Exception:
                try:
                    conn.close()
                except Exception:
                    pass
                conn = http.client.HTTPConnection(self.host, self.port, timeout=10)
            cost = (time.perf_counter() - start) * 1000
            bucket = self.bucket
            bucket["latencies"].append(cost)
            bucket["total"] += 1
            if ok:
                bucket["ok"] += 1
            else:
                bucket["errors"] += 1


def percentile(values, p):
    if not values:
        return 0.0
    values = sorted(values)
    return values[min(len(values) - 1, int(len(values) * p / 100))]


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--host", default="127.0.0.1")
    ap.add_argument("--port", type=int, default=8080)
    ap.add_argument("--scenario", choices=["success", "failure", "mixed"], default="success")
    ap.add_argument("--duration", type=int, default=20, help="秒，不含预热")
    ap.add_argument("--warmup", type=int, default=5, help="预热秒数，不计入统计")
    ap.add_argument("--threads", type=int, default=16, help="keep-alive 连接数")
    args = ap.parse_args()

    stop_event = threading.Event()
    bucket = {"latencies": [], "total": 0, "ok": 0, "errors": 0}
    with ThreadPoolExecutor(max_workers=args.threads) as pool:
        for _ in range(args.threads):
            pool.submit(Runner(args.host, args.port, args.scenario, stop_event, bucket).start)
        time.sleep(args.warmup)  # 预热 JIT 与连接池，不计入统计
        bucket["latencies"].clear()
        bucket["total"] = bucket["ok"] = bucket["errors"] = 0
        start = time.perf_counter()
        time.sleep(args.duration)
        stop_event.set()
        elapsed = time.perf_counter() - start
    lat = bucket["latencies"]
    print(json.dumps({
        "scenario": args.scenario,
        "threads": args.threads,
        "duration_s": round(elapsed, 1),
        "qps": round(bucket["total"] / elapsed, 1),
        "p50_ms": round(percentile(lat, 50), 2),
        "p95_ms": round(percentile(lat, 95), 2),
        "p99_ms": round(percentile(lat, 99), 2),
        "error_rate": round(bucket["errors"] / max(bucket["total"], 1) * 100, 3),
        "total": bucket["total"],
    }, ensure_ascii=False))


if __name__ == "__main__":
    main()
