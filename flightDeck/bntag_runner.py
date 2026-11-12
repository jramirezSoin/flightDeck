"""Ejecuta bnTag replicando la lógica de bnTag/installBN.sh (sin bash ni envsubst).

Asume que config/application.properties ya existe y que classpath.txt es fijo.
"""

import os
import queue
import re
import shlex
import shutil
import subprocess
import threading
import uuid
from pathlib import Path
from string import Template

JAR_NAME = "installbn-1.0.0.jar"
TMP_DIR = "C:/Temp/osbinstance"  # mismo valor que el rm -rf de installBN.sh
NAME_RE = re.compile(r"^[A-Za-z0-9_.-]+$")

STEPS_SOA = ["LoadContext", "UploadFileStep", "ImportStep"]
STEPS_OSB_CREATE = ["LoadContext", "CreateOSBStep"]
STEPS_OSB = ["LoadContext", "GitStatusStep", "OSBCompile", "UploadFileStep", "ImportStep"]

RE_RUNNING = re.compile(r"^Ejecutando: (\S+)\s*$")
RE_RESULT = re.compile(r"^(\S+) -> (SUCCESS|FAILED|SKIPPED)\s*:?\s*(.*)$")
RE_STOPPED = re.compile(r"^Pipeline detenido en: (\S+)\s*$")


class RunError(Exception):
    """Error de validación o de entorno antes de lanzar bnTag."""


def bntag_home() -> Path:
    env = os.environ.get("BNTAG_HOME")
    if env:
        return Path(env)
    return Path(__file__).resolve().parent.parent / "bnTag"


def _expand(text: str) -> str:
    # Equivale a envsubst: sustituye $VAR con el ambiente; lo no definido queda vacío.
    return Template(text).substitute(_EnvDefault())


class _EnvDefault(dict):
    def __missing__(self, key):
        return os.environ.get(key, "")


def environment_status() -> dict:
    home = bntag_home()
    return {
        "bntag_home": str(home),
        "properties": (home / "config" / "application.properties").is_file(),
        "jar": (home / "target" / JAR_NAME).is_file(),
        "java": shutil.which("java") is not None,
        "oracle_home": bool(os.environ.get("ORACLE_HOME")),
        "busy": current_job() is not None,
    }


def validate_request(req: dict) -> tuple[list[str], list[str]]:
    """Valida la petición y devuelve (args de bnTag, pasos esperados)."""
    target = req.get("target")
    name = req.get("name")
    if target not in ("osb", "soa"):
        raise RunError("target debe ser 'osb' o 'soa'")
    if not isinstance(name, str) or not NAME_RE.match(name):
        raise RunError("Nombre inválido: solo letras, números, '_', '-' y '.'")

    flags = {}
    for key in ("project", "skipCompile", "skipImport", "create", "rest"):
        value = req.get(key, False)
        if not isinstance(value, bool):
            raise RunError(f"'{key}' debe ser booleano")
        flags[key] = value

    if target == "soa":
        if flags["create"] or flags["rest"] or flags["skipCompile"]:
            raise RunError("--create, --rest y --skipCompile solo aplican a OSB")
        args = ["-soa", name]
        if flags["skipImport"]:
            args.append("--skipImport")
        return args, list(STEPS_SOA)

    if flags["rest"] and not flags["create"]:
        raise RunError("--rest requiere --create")
    if flags["create"]:
        if flags["project"] or flags["skipCompile"] or flags["skipImport"]:
            raise RunError("--create no se combina con --project, --skipCompile ni --skipImport")
        args = ["-osb", name, "--create"]
        if flags["rest"]:
            args.append("--rest")
        return args, list(STEPS_OSB_CREATE)

    args = ["-osb", name]
    for key in ("project", "skipCompile", "skipImport"):
        if flags[key]:
            args.append("--" + key)
    return args, list(STEPS_OSB)


def build_command(args: list[str]) -> tuple[list[str], Path, bool]:
    """Arma el comando java como installBN.sh. Devuelve (comando, cwd, limpiar_tmp)."""
    home = bntag_home()
    if not (home / "config" / "application.properties").is_file():
        raise RunError(f"No existe {home / 'config' / 'application.properties'}")
    jar = home / "target" / JAR_NAME
    if not jar.is_file():
        raise RunError(f"No existe {jar}. Compila bnTag con 'mvn package'.")
    java = shutil.which("java")
    if java is None:
        raise RunError("No se encontró 'java' en el PATH")

    needs_oracle = "-osb" in args and "--skipCompile" not in args
    # java_opts.txt usa $ORACLE_HOME siempre (igual que installBN.sh).
    if not os.environ.get("ORACLE_HOME"):
        raise RunError("Falta la variable de ambiente ORACLE_HOME")

    opts_raw = (home / "java_opts.txt").read_text(encoding="utf-8")
    java_opts = shlex.split(_expand(opts_raw).replace("\\", "/"))

    if needs_oracle:
        cp_raw = (home / "classpath.txt").read_text(encoding="utf-8").strip()
        # Se divide en crudo: cada entrada empieza con $ORACLE_HOME, así los ':' de 'C:' no estorban.
        entries = [_expand(e).replace("\\", "/") for e in cp_raw.split(":") if e]
        extra_cp = os.pathsep.join(entries)
    else:
        extra_cp = "."

    classpath = os.pathsep.join([f"target/{JAR_NAME}", extra_cp])
    cmd = [java, *java_opts, "-cp", classpath, "com.soin.App", *args]
    return cmd, home, needs_oracle


class Job:
    def __init__(self, args: list[str], steps: list[str]):
        self.id = uuid.uuid4().hex[:12]
        self.args = args
        self.steps = steps
        self.events: list[dict] = []
        self.cond = threading.Condition()
        self.proc: subprocess.Popen | None = None
        self.exit_code: int | None = None
        self.finished = False

    def emit(self, event: dict) -> None:
        with self.cond:
            self.events.append(event)
            self.cond.notify_all()

    def start(self, cmd: list[str], cwd: Path, clean_tmp: bool) -> None:
        if clean_tmp:
            shutil.rmtree(TMP_DIR, ignore_errors=True)
        self.proc = subprocess.Popen(
            cmd,
            cwd=str(cwd),
            stdout=subprocess.PIPE,
            stderr=subprocess.STDOUT,
            stdin=subprocess.DEVNULL,
        )
        threading.Thread(target=self._read, daemon=True).start()

    def _read(self) -> None:
        assert self.proc and self.proc.stdout
        for raw in iter(self.proc.stdout.readline, b""):
            line = raw.decode("utf-8", errors="replace").rstrip("\r\n")
            self.emit({"type": "log", "line": line})
            self._parse(line)
        self.exit_code = self.proc.wait()
        with self.cond:
            self.finished = True
            self.events.append({"type": "done", "exit_code": self.exit_code})
            self.cond.notify_all()

    def _parse(self, line: str) -> None:
        m = RE_RUNNING.match(line)
        if m:
            self.emit({"type": "step", "name": m.group(1), "status": "RUNNING", "msg": ""})
            return
        m = RE_RESULT.match(line)
        if m:
            self.emit({"type": "step", "name": m.group(1), "status": m.group(2), "msg": m.group(3)})
            return
        m = RE_STOPPED.match(line)
        if m:
            self.emit({"type": "stopped", "name": m.group(1)})

    def cancel(self) -> None:
        if self.proc and self.proc.poll() is None:
            # /T mata también los procesos hijos
            subprocess.run(
                ["taskkill", "/T", "/F", "/PID", str(self.proc.pid)],
                stdout=subprocess.DEVNULL,
                stderr=subprocess.DEVNULL,
            )
            self.emit({"type": "log", "line": "ERROR: ejecución cancelada por el usuario"})

    def wait_events(self, index: int, timeout: float = 15.0) -> tuple[list[dict], bool]:
        """Devuelve (eventos desde index, terminó). Bloquea hasta timeout si no hay nuevos."""
        with self.cond:
            if index >= len(self.events) and not self.finished:
                self.cond.wait(timeout)
            return self.events[index:], self.finished


_jobs: dict[str, Job] = {}
_lock = threading.Lock()


def current_job() -> Job | None:
    with _lock:
        for job in _jobs.values():
            if not job.finished:
                return job
    return None


def get_job(job_id: str) -> Job | None:
    with _lock:
        return _jobs.get(job_id)


def start_job(req: dict) -> Job:
    args, steps = validate_request(req)
    cmd, cwd, clean_tmp = build_command(args)
    with _lock:
        if any(not j.finished for j in _jobs.values()):
            raise RunError("busy")
        job = Job(args, steps)
        _jobs[job.id] = job
    try:
        job.start(cmd, cwd, clean_tmp)
    except OSError as exc:
        job.finished = True
        raise RunError(f"No se pudo lanzar java: {exc}") from exc
    return job
