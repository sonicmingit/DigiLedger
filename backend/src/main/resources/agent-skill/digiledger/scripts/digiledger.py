#!/usr/bin/env python3
"""Portable DigiLedger client. Python standard library only; credentials stay in env."""
import argparse
import json
import os
import sys
import urllib.error
import urllib.parse
import urllib.request


class NoRedirect(urllib.request.HTTPRedirectHandler):
    def redirect_request(self, req, fp, code, msg, headers, newurl):
        # Never forward a credential to another host or downgrade HTTPS.
        return None


def build_parser():
    parser = argparse.ArgumentParser(description="查询和维护 DigiLedger 物品")
    commands = parser.add_subparsers(dest="command", required=True)
    listing = commands.add_parser("list", help="分页查询物品")
    listing.add_argument("--keyword")
    listing.add_argument("--status")
    listing.add_argument("--category-id", type=int)
    listing.add_argument("--brand-id", type=int)
    listing.add_argument("--platform-id", type=int)
    listing.add_argument("--tag-ids", help="逗号分隔的标签 ID")
    listing.add_argument("--page", type=int, default=1)
    listing.add_argument("--page-size", type=int, default=20)
    for command in ("get", "patch", "delete"):
        sub = commands.add_parser(command)
        sub.add_argument("id", type=int)
        if command == "patch":
            sub.add_argument("--json-file", required=True)
    commands.add_parser("create").add_argument("--json-file", required=True)
    commands.add_parser("dict").add_argument("kind", choices=["categories", "brands", "platforms", "tags"])
    return parser


def call(args):
    base = os.environ.get("DIGILEDGER_API_URL", "").strip().rstrip("/")
    token = os.environ.get("DIGILEDGER_API_TOKEN", "").strip()
    parsed = urllib.parse.urlsplit(base)
    if (parsed.scheme not in ("http", "https") or not parsed.hostname or parsed.username
            or parsed.password or parsed.query or parsed.fragment or not parsed.path.endswith("/api/open/v1")):
        raise ValueError("请设置 DIGILEDGER_API_URL 为 http(s)://你的系统地址/api/open/v1")
    if not token:
        raise ValueError("请设置 DIGILEDGER_API_TOKEN")
    if hasattr(args, "id") and args.id < 1:
        raise ValueError("物品 ID 必须大于 0")
    method, path, payload = "GET", "/assets", None
    if args.command == "list":
        if not 1 <= args.page <= 1000000 or not 1 <= args.page_size <= 100:
            raise ValueError("page 需为 1–1000000，page-size 需为 1–100")
        query = {key.replace("-", "_"): value for key, value in {
            "keyword": args.keyword, "status": args.status, "category_id": args.category_id,
            "brand_id": args.brand_id, "platform_id": args.platform_id, "tag_ids": args.tag_ids,
            "page": args.page, "page_size": args.page_size}.items() if value is not None}
        path += "?" + urllib.parse.urlencode(query)
    elif args.command == "dict":
        path = "/dict/" + args.kind + ("/tree" if args.kind in ("categories", "tags") else "")
    elif args.command in ("get", "patch", "delete"):
        path += "/" + str(args.id)
        method = {"get": "GET", "patch": "PATCH", "delete": "DELETE"}[args.command]
    elif args.command == "create":
        method = "POST"
    if method in ("POST", "PATCH"):
        if args.json_file == "-":
            body = json.load(sys.stdin)
        else:
            with open(args.json_file, encoding="utf-8-sig") as file:
                body = json.load(file)
        if not isinstance(body, dict) or not body:
            raise ValueError("JSON 请求必须是非空对象")
        payload = json.dumps(body, ensure_ascii=False).encode("utf-8")
    request = urllib.request.Request(base + path, data=payload, method=method,
        headers={"Authorization": "Bearer " + token, "Content-Type": "application/json", "Accept": "application/json"})
    with urllib.request.build_opener(NoRedirect).open(request, timeout=30) as response:
        envelope = json.load(response)
    if envelope.get("code") != 200:
        raise ValueError("API 错误 {}：{}".format(envelope.get("code"), envelope.get("msg", "未知错误")))
    return envelope.get("data")


def main():
    for stream in (sys.stdout, sys.stderr):
        if hasattr(stream, "reconfigure"):
            stream.reconfigure(encoding="utf-8")
    args = build_parser().parse_args()
    try:
        print(json.dumps(call(args), ensure_ascii=False, indent=2))
        return 0
    except urllib.error.HTTPError as error:
        # Do not dump arbitrary proxy response bodies or request headers.
        hints = {401: "访问 Token 无效或缺失", 403: "开放 API 未启用或 Token 仅有只读权限"}
        print("HTTP {}：{}".format(error.code, hints.get(error.code, "调用失败；写请求请查询结果后再决定是否重试")), file=sys.stderr)
    except urllib.error.URLError:
        print("网络连接失败；写请求请查询结果后再决定是否重试", file=sys.stderr)
    except ValueError as error:
        secret = os.environ.get("DIGILEDGER_API_TOKEN", "").strip()
        message = str(error)
        if secret:
            message = message.replace(secret, "<Token 已隐藏>")
        print(message, file=sys.stderr)
    except (OSError, TimeoutError):
        print("调用失败：请检查连接、Token、JSON 字段及系统 API 文档；写请求请先查询结果再重试", file=sys.stderr)
        return 1
    return 1


if __name__ == "__main__":
    sys.exit(main())
