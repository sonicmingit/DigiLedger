"""Test the distributed CLI against an isolated HTTP fixture, never a real ledger."""
import contextlib
import importlib.util
import io
import json
import os
from pathlib import Path
import sys
import tempfile
import threading
import unittest
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from unittest.mock import patch
import urllib.error
import urllib.parse
import zipfile

sys.dont_write_bytecode = True
SKILL = Path(__file__).resolve().parents[2] / 'main/resources/agent-skill/digiledger'
spec = importlib.util.spec_from_file_location('digiledger', SKILL / 'scripts/digiledger.py')
client = importlib.util.module_from_spec(spec)
spec.loader.exec_module(client)


class Fixture(BaseHTTPRequestHandler):
    state = {'enabled': False, 'allowWrite': False, 'tokenConfigured': False}
    calls = []
    mode = 'ok'

    def log_message(self, *args):
        pass

    def respond(self, data, status=200, code=200):
        raw = json.dumps({'code': code, 'data': data, 'msg': '字段校验失败' if code != 200 else 'OK'}).encode('utf-8')
        self.send_response(status)
        self.send_header('Content-Type', 'application/json; charset=utf-8')
        self.end_headers()
        self.wfile.write(raw)

    def do_GET(self):
        self.calls.append((self.command, self.path, self.headers.get('Authorization'), None))
        if self.path.startswith('/api/agent-resources/'):
            name = self.path.rsplit('/', 1)[1]
            if name == 'digiledger-skill.zip':
                output = io.BytesIO()
                with zipfile.ZipFile(output, 'w') as bundle:
                    for file in SKILL.rglob('*'):
                        if file.is_file() and '__pycache__' not in file.parts:
                            bundle.write(file, 'digiledger/' + file.relative_to(SKILL).as_posix())
                raw, mime = output.getvalue(), 'application/zip'
            elif name in ('api.md', 'openapi.json'):
                raw = (SKILL / 'references' / name).read_bytes()
                mime = 'text/plain;charset=UTF-8' if name == 'api.md' else 'application/json'
            else:
                self.respond(None, status=404, code=404)
                return
            self.send_response(200)
            self.send_header('Content-Type', mime)
            if name.endswith('.zip'):
                self.send_header('Content-Disposition', 'attachment; filename=digiledger-skill.zip')
            self.end_headers()
            self.wfile.write(raw)
        elif self.path == '/api/settings/open-api':
            self.respond(self.state)
        elif self.mode == 'redirect' and self.path.startswith('/api/open/'):
            self.send_response(302)
            self.send_header('Location', '/should-not-receive-token')
            self.end_headers()
        elif self.mode == 'forbidden' and self.path.startswith('/api/open/'):
            self.respond(None, status=403, code=403)
        elif self.path.startswith('/api/open/v1/assets'):
            self.respond({'records': [{'id': 42, 'name': '相机'}], 'total': 1, 'page': 1, 'pageSize': 20})
        else:
            self.respond([])

    def write(self):
        body = json.loads(self.rfile.read(int(self.headers.get('Content-Length', '0'))) or '{}')
        self.calls.append((self.command, self.path, self.headers.get('Authorization'), body))
        if self.path == '/api/settings/open-api/token':
            self.state.update(tokenConfigured=True, tokenPrefix='dl_demo')
            self.respond({'token': 'dl_demo_preview_only'})
        elif self.path == '/api/settings/open-api':
            self.state.update(body)
            self.respond(None)
        elif self.mode == 'business-error':
            self.respond(None, code=400)
        else:
            self.respond(42 if self.command == 'POST' else None)

    do_POST = write
    do_PATCH = write
    do_PUT = write
    do_DELETE = write


class SkillClientTest(unittest.TestCase):
    def setUp(self):
        Fixture.calls = []; Fixture.mode = 'ok'
        self.server = ThreadingHTTPServer(('127.0.0.1', 0), Fixture)
        threading.Thread(target=self.server.serve_forever, daemon=True).start()
        self.env = patch.dict(os.environ, {'DIGILEDGER_API_URL': 'http://127.0.0.1:{}/api/open/v1'.format(self.server.server_port), 'DIGILEDGER_API_TOKEN': 'dl_test_secret'})
        self.env.start()

    def tearDown(self):
        self.env.stop(); self.server.shutdown(); self.server.server_close()

    def call(self, *args):
        return client.call(client.build_parser().parse_args(args))

    def test_keyword_query_is_encoded_and_result_is_unwrapped(self):
        result = self.call('list', '--keyword', '相机 & 镜头', '--tag-ids', '1,2', '--page', '2')
        self.assertEqual(result['records'][0]['id'], 42)
        method, url, token, _ = Fixture.calls[0]
        self.assertEqual(method, 'GET')
        self.assertEqual(token, 'Bearer dl_test_secret')
        self.assertEqual(urllib.parse.parse_qs(urllib.parse.urlsplit(url).query)['keyword'], ['相机 & 镜头'])

    def test_create_and_patch_send_only_supplied_utf8_body(self):
        with tempfile.TemporaryDirectory() as directory:
            file = Path(directory) / 'payload.json'
            file.write_text(json.dumps({'name': '相机', 'categoryId': 3, 'status': '使用中'}), encoding='utf-8-sig')
            self.assertEqual(self.call('create', '--json-file', str(file)), 42)
            file.write_text('{"notes":"放在书房"}', encoding='utf-8')
            self.call('patch', '42', '--json-file', str(file))
        self.assertEqual(Fixture.calls[-1][0], 'PATCH')
        self.assertEqual(Fixture.calls[-1][-1], {'notes': '放在书房'})

    def test_business_error_is_not_retried_and_token_is_not_printed(self):
        Fixture.mode = 'business-error'
        with patch.object(sys, 'argv', ['digiledger', 'create', '--json-file', '-']), patch.object(sys, 'stdin', io.StringIO('{"name":"相机"}')):
            output, error = io.StringIO(), io.StringIO()
            with contextlib.redirect_stdout(output), contextlib.redirect_stderr(error):
                self.assertEqual(client.main(), 1)
        self.assertEqual(len(Fixture.calls), 1)
        self.assertIn('400', error.getvalue())
        self.assertNotIn('dl_test_secret', output.getvalue() + error.getvalue())

    def test_http_permissions_errors_fail(self):
        Fixture.mode = 'forbidden'
        with self.assertRaises(urllib.error.HTTPError) as error:
            self.call('get', '42')
        self.assertEqual(error.exception.code, 403)

    def test_does_not_follow_redirect_with_credentials(self):
        Fixture.mode = 'redirect'
        with self.assertRaises(urllib.error.HTTPError):
            self.call('list')
        self.assertEqual(len(Fixture.calls), 1)

    def test_rejects_invalid_url_or_pagination_without_network(self):
        with self.assertRaises(ValueError):
            self.call('list', '--page-size', '101')
        with patch.dict(os.environ, {'DIGILEDGER_API_URL': 'https://user:password@example.com/api/open/v1'}):
            with self.assertRaises(ValueError): self.call('list')
        self.assertEqual(len(Fixture.calls), 0)


if __name__ == '__main__':
    if len(sys.argv) == 3 and sys.argv[1] == '--serve-preview':
        server = ThreadingHTTPServer(('127.0.0.1', int(sys.argv[2])), Fixture)
        print('Isolated API preview fixture ready', flush=True)
        server.serve_forever()
    else:
        unittest.main()
