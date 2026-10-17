"""Local evidence server only: built SPA at4178, packaged API/static media proxy at8108."""
from http.server import ThreadingHTTPServer, SimpleHTTPRequestHandler
from pathlib import Path
from urllib.request import urlopen
from urllib.error import HTTPError

ROOT = Path(__file__).resolve().parents[4]
DIST = ROOT / 'frontend/dist'
class Handler(SimpleHTTPRequestHandler):
    def do_GET(self):
        if self.path.startswith(('/api/', '/images/')):
            try:
                with urlopen('http://127.0.0.1:8108' + self.path) as response:
                    data = response.read(); self.send_response(response.status)
                    self.send_header('Content-Type', response.headers.get('Content-Type', 'application/octet-stream'))
                    self.end_headers(); self.wfile.write(data)
            except HTTPError as error:
                self.send_response(error.code); self.send_header('Content-Type', 'application/json'); self.end_headers(); self.wfile.write(error.read())
            return
        super().do_GET()
    def translate_path(self, path):
        relative = path.split('?', 1)[0].removeprefix('/app-next/').lstrip('/')
        file = (DIST / relative).resolve()
        return str(file if file.is_relative_to(DIST.resolve()) and file.is_file() else DIST / 'index.html')
ThreadingHTTPServer(('127.0.0.1', 4178), Handler).serve_forever()
