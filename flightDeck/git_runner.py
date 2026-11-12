"""Operaciones git (status, add, restore, commit, push) para la vista Git de FlightDeck.

Usa el ejecutable `git` del PATH vía subprocess (sin shell). Todas las rutas de archivos se
pasan después de `--` y con `--literal-pathspecs`, así un nombre de archivo nunca se
interpreta como opción ni como patrón.
"""

import os
import shutil
import subprocess
import threading
from pathlib import Path

from . import bntag_runner

TIMEOUT = 120  # segundos; el push puede tardar
MAX_PATHS = 2000
MAX_MESSAGE = 10_000

# Un solo comando git a la vez: evita choques por .git/index.lock entre peticiones.
_lock = threading.Lock()


class GitError(Exception):
    """Error de validación o de ejecución de git (el mensaje es apto para mostrar)."""


def default_repo() -> str:
    """Ruta por defecto: `osb.dir` de config/application.properties (la que usa bnTag)."""
    props = bntag_runner.bntag_home() / "config" / "application.properties"
    try:
        for line in props.read_text(encoding="utf-8").splitlines():
            line = line.strip()
            if line.startswith("osb.dir") and "=" in line:
                key, value = line.split("=", 1)
                if key.strip() == "osb.dir":
                    return value.strip()
    except OSError:
        pass
    return ""


def _repo_dir(path: str) -> Path:
    path = (path or "").strip() or default_repo()
    if not path:
        raise GitError("Indica la ruta del repositorio.")
    repo = Path(path)
    if not repo.is_dir():
        raise GitError(f"La ruta no existe o no es una carpeta: {path}")
    return repo


def _run(repo: Path, args: list[str], *, check: bool = True, raw: bool = False) -> tuple[int, str]:
    """raw=True devuelve solo stdout sin recortar (para parsear); si no, stdout+stderr para mostrar."""
    git = shutil.which("git")
    if git is None:
        raise GitError("No se encontró 'git' en el PATH")
    env = dict(os.environ, GIT_TERMINAL_PROMPT="0")
    try:
        proc = subprocess.run(
            [git, "--literal-pathspecs", "-c", "core.quotepath=false", *args],
            cwd=str(repo),
            stdin=subprocess.DEVNULL,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            env=env,
            timeout=TIMEOUT,
        )
    except subprocess.TimeoutExpired as exc:
        raise GitError(f"git {args[0]} excedió {TIMEOUT}s (¿pide credenciales?)") from exc
    except OSError as exc:
        raise GitError(f"No se pudo ejecutar git: {exc}") from exc
    stdout = proc.stdout.decode("utf-8", errors="replace")
    stderr = proc.stderr.decode("utf-8", errors="replace")
    out = stdout if raw else "\n".join(t for t in (stdout.strip(), stderr.strip()) if t)
    if check and proc.returncode != 0:
        raise GitError(out or f"git {args[0]} terminó con código {proc.returncode}")
    return proc.returncode, out


def _clean_paths(paths) -> list[str]:
    if not isinstance(paths, list) or not paths:
        raise GitError("Selecciona al menos un archivo.")
    if len(paths) > MAX_PATHS:
        raise GitError("Demasiados archivos en una sola operación.")
    clean = []
    for p in paths:
        if not isinstance(p, str) or not p or "\0" in p:
            raise GitError("Ruta de archivo inválida.")
        norm = p.replace("\\", "/")
        if norm.startswith("/") or (len(norm) > 1 and norm[1] == ":") or ".." in norm.split("/"):
            raise GitError(f"Ruta fuera del repositorio: {p}")
        clean.append(norm)
    return clean


def _parse_branch(line: str) -> dict:
    # "## main...origin/main [ahead 1, behind 2]" | "## main" | "## HEAD (no branch)"
    # | "## No commits yet on main"
    info = {"branch": "", "upstream": "", "ahead": 0, "behind": 0}
    text = line[3:]
    if text.startswith("No commits yet on "):
        info["branch"] = text[len("No commits yet on "):]
        return info
    counts = ""
    if " [" in text and text.endswith("]"):
        text, counts = text[:-1].split(" [", 1)
    if "..." in text:
        info["branch"], info["upstream"] = text.split("...", 1)
    else:
        info["branch"] = text
    for part in counts.split(","):
        part = part.strip()
        if part.startswith("ahead "):
            info["ahead"] = int(part[6:])
        elif part.startswith("behind "):
            info["behind"] = int(part[7:])
    return info


def status(path: str) -> dict:
    repo = _repo_dir(path)
    with _lock:
        code, top = _run(repo, ["rev-parse", "--show-toplevel"], check=False)
        if code != 0:
            raise GitError(f"No es un repositorio git: {repo}")
        _, raw = _run(repo, ["status", "--porcelain=v1", "-z", "-b", "--untracked-files=all"], raw=True)
    entries = raw.split("\0")
    info = _parse_branch(entries[0]) if entries and entries[0].startswith("## ") else {
        "branch": "", "upstream": "", "ahead": 0, "behind": 0}
    files = []
    i = 1
    while i < len(entries):
        entry = entries[i]
        i += 1
        if len(entry) < 4:
            continue
        x, y, name = entry[0], entry[1], entry[3:]
        orig = ""
        if x in "RC" or y in "RC":  # la siguiente entrada es la ruta original
            orig = entries[i] if i < len(entries) else ""
            i += 1
        files.append({"path": name, "x": x, "y": y, "orig": orig})
    return {"path": str(repo), "root": top, **info, "files": files}


def _toplevel(repo: Path) -> Path:
    """Raíz del repo. `git status` da las rutas relativas a la raíz, así que add/restore
    deben ejecutarse desde ahí (la ruta indicada puede ser una subcarpeta del repo)."""
    code, top = _run(repo, ["rev-parse", "--show-toplevel"], check=False)
    if code != 0:
        raise GitError(f"No es un repositorio git: {repo}")
    return Path(top)


def add(path: str, paths) -> str:
    repo, files = _repo_dir(path), _clean_paths(paths)
    with _lock:
        _, out = _run(_toplevel(repo), ["add", "--", *files])
    return out


def restore(path: str, paths, staged: bool) -> str:
    """staged=True: saca del stage (conserva los cambios). staged=False: DESCARTA los cambios del disco."""
    repo, files = _repo_dir(path), _clean_paths(paths)
    args = ["restore", "--staged"] if staged else ["restore"]
    with _lock:
        _, out = _run(_toplevel(repo), [*args, "--", *files])
    return out


def commit(path: str, message) -> str:
    repo = _repo_dir(path)
    if not isinstance(message, str) or not message.strip():
        raise GitError("Escribe el mensaje del commit.")
    if len(message) > MAX_MESSAGE:
        raise GitError("El mensaje del commit es demasiado largo.")
    with _lock:
        _, out = _run(repo, ["commit", "-m", message])
    return out


def push(path: str) -> str:
    """`git push origin <rama actual>` (nunca --force). Siempre a 'origin', sin depender del
    upstream configurado (el repo también tiene el remoto 'upstream')."""
    repo = _repo_dir(path)
    with _lock:
        code, branch = _run(repo, ["symbolic-ref", "--short", "HEAD"], check=False)
        if code != 0 or not branch:
            raise GitError("No estás en una rama; no se puede hacer push.")
        _, out = _run(repo, ["push", "origin", branch])
    return out
