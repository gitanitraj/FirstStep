"""IM-05 verification harness: built Vite UI + packaged backend, no production changes.
Usage from root: python3 .../serve-ui.py UI_PORT API_PORT
"""
from http.server import SimpleHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
import sys
import urllib.request
import urllib.error

ROOT = Path('frontend/dist').resolve()
API = 'http://127.0.0.1:' + sys.argv[2]
class Handler(SimpleHTTPRequestHandler):
    def do_GET(self):
        if self.path.startswith(('/api/', '/images/')):
            try:
                response = urllib.request.urlopen(API + self.path)
            except urllib.error.HTTPError as error:
                response = error
            self.send_response(response.status)
            self.send_header('Content-Type', response.headers.get('Content-Type', 'application/json'))
            self.end_headers()
            self.wfile.write(response.read())
            return
        path = self.path.split('?')[0].removeprefix('/app-next/')
        candidate = ROOT / path
        if not candidate.is_file(): path = 'index.html'
        self.path = '/' + path
        return super().do_GET()
    def __init__(self, *args, **kwargs): super().__init__(*args, directory=str(ROOT), **kwargs)
ThreadingHTTPServer(('127.0.0.1', int(sys.argv[1])), Handler).serve_forever()
