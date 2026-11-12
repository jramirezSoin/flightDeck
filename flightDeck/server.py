"""Servidor HTTP local de FlightDeck (solo biblioteca estándar)."""

import json
import mimetypes
import re
import secrets
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
from urllib.parse import parse_qs, urlsplit

from . import bntag_runner as bntag
from . import git_runner as git

STATIC_DIR = Path(__file__).resolve().parent / "static"
TOKEN = secrets.token_urlsafe(24)
MAX_BODY = 16 * 1024

RE_JOB = re.compile(r"^/api/jobs/([0-9a-f]+)/(events|cancel)$")
GIT_OPS = ("add", "restore", "commit", "push")


class Handler(BaseHTTPRequestHandler):
    protocol_version = "HTTP/1.1"
    server_version = "FlightDeck"

    # --- utilidades -------------------------------------------------------
    def log_message(self, fmt, *args):  # silencia el log por petición
        pass

    def _host_ok(self) -> bool:
        port = self.server.server_address[1]
        allowed = {f"127.0.0.1:{port}", f"localhost:{port}"}
        if self.headers.get("Host", "") not in allowed:
            return False
        origin = self.headers.get("Origin")
        return origin is None or origin in {f"http://{h}" for h in allowed}

    def _send(self, status: int, body: bytes, ctype: str = "application/json") -> None:
        self.send_response(status)
        self.send_header("Content-Type", ctype)
        self.send_header("Content-Length", str(len(body)))
        self.send_header("Cache-Control", "no-store")
        self.end_headers()
        self.wfile.write(body)

    def _json(self, status: int, data) -> None:
        self._send(status, json.dumps(data).encode("utf-8"))

    def _guard(self, post: bool) -> bool:
        if not self._host_ok():
            self._json(403, {"error": "Host u Origin no permitido"})
            return False
        if post and self.headers.get("X-FlightDeck-Token") != TOKEN:
            self._json(403, {"error": "Token inválido"})
            return False
        return True

    # --- GET ---------------------------------------------------------------
    def do_GET(self):
        if not self._guard(post=False):
            return
        url = urlsplit(self.path)
        path = url.path
        if path in ("/", "/index.html"):
            html = (STATIC_DIR / "index.html").read_text(encoding="utf-8")
            self._send(200, html.replace("__TOKEN__", TOKEN).encode("utf-8"), "text/html; charset=utf-8")
        elif path.startswith("/static/"):
            self._static(path[len("/static/"):])
        elif path == "/api/bntag/config":
            self._json(200, bntag.environment_status())
        elif path == "/api/git/status":
            repo = parse_qs(url.query).get("path", [""])[0]
            self._git(lambda: git.status(repo))
        else:
            m = RE_JOB.match(path)
            if m and m.group(2) == "events":
                self._events(m.group(1))
            else:
                self._json(404, {"error": "No encontrado"})

    def _static(self, rel: str) -> None:
        target = (STATIC_DIR / rel).resolve()
        if STATIC_DIR not in target.parents or not target.is_file():
            self._json(404, {"error": "No encontrado"})
            return
        ctype = mimetypes.guess_type(target.name)[0] or "application/octet-stream"
        if ctype.startswith("text/") or ctype.endswith("javascript"):
            ctype += "; charset=utf-8"
        self._send(200, target.read_bytes(), ctype)

    def _events(self, job_id: str) -> None:
        job = bntag.get_job(job_id)
        if job is None:
            self._json(404, {"error": "Job no existe"})
            return
        # Sin Content-Length: la respuesta se cierra al terminar (SSE).
        self.send_response(200)
        self.send_header("Content-Type", "text/event-stream; charset=utf-8")
        self.send_header("Cache-Control", "no-store")
        self.send_header("Connection", "close")
        self.end_headers()
        self.close_connection = True
        index = 0
        try:
            while True:
                events, finished = job.wait_events(index)
                for ev in events:
                    self.wfile.write(b"data: " + json.dumps(ev).encode("utf-8") + b"\n\n")
                index += len(events)
                if not events:
                    self.wfile.write(b": keepalive\n\n")
                self.wfile.flush()
                if finished and not events:
                    break
        except (BrokenPipeError, ConnectionResetError, ConnectionAbortedError):
            pass

    # --- POST --------------------------------------------------------------
    def do_POST(self):
        if not self._guard(post=True):
            return
        path = self.path.split("?", 1)[0]
        if path == "/api/bntag/run":
            self._run()
            return
        if path.startswith("/api/git/") and path[len("/api/git/"):] in GIT_OPS:
            self._git_op(path[len("/api/git/"):])
            return
        m = RE_JOB.match(path)
        if m and m.group(2) == "cancel":
            job = bntag.get_job(m.group(1))
            if job is None:
                self._json(404, {"error": "Job no existe"})
            else:
                job.cancel()
                self._json(200, {"ok": True})
            return
        self._json(404, {"error": "No encontrado"})

    def _body(self) -> dict | None:
        """Lee el body JSON (objeto). Responde 400 y devuelve None si es inválido."""
        try:
            length = int(self.headers.get("Content-Length", "0"))
            if length > MAX_BODY:
                raise ValueError
            req = json.loads(self.rfile.read(length) or b"{}")
            if not isinstance(req, dict):
                raise ValueError
            return req
        except ValueError:
            self._json(400, {"error": "JSON inválido"})
            return None

    def _git(self, call) -> None:
        try:
            self._json(200, call())
        except git.GitError as exc:
            self._json(400, {"error": str(exc)})

    def _git_op(self, op: str) -> None:
        req = self._body()
        if req is None:
            return
        repo = req.get("path", "")
        if not isinstance(repo, str):
            self._json(400, {"error": "'path' debe ser texto"})
            return
        if op == "add":
            call = lambda: git.add(repo, req.get("files"))
        elif op == "restore":
            staged = req.get("staged", False)
            if not isinstance(staged, bool):
                self._json(400, {"error": "'staged' debe ser booleano"})
                return
            call = lambda: git.restore(repo, req.get("files"), staged)
        elif op == "commit":
            call = lambda: git.commit(repo, req.get("message"))
        else:
            call = lambda: git.push(repo)
        self._git(lambda: {"output": call()})

    def _run(self) -> None:
        req = self._body()
        if req is None:
            return
        try:
            job = bntag.start_job(req)
        except bntag.RunError as exc:
            if str(exc) == "busy":
                self._json(409, {"error": "Ya hay una ejecución en curso"})
            else:
                self._json(400, {"error": str(exc)})
            return
        self._json(200, {"job_id": job.id, "steps": job.steps, "command": " ".join(job.args)})


def make_server(port: int) -> ThreadingHTTPServer:
    server = ThreadingHTTPServer(("127.0.0.1", port), Handler)
    server.daemon_threads = True
    return server
