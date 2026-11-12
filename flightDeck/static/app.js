"use strict";

const TOKEN = document.querySelector('meta[name="fd-token"]').content;
const $ = (id) => document.getElementById(id);
const NAME_RE = /^[A-Za-z0-9_.-]+$/;

const STATE_LABEL = {
  PENDING: "Pendiente", RUNNING: "En curso", SUCCESS: "Listo", FAILED: "Falló", SKIPPED: "Omitido",
};

let currentJob = null;
let source = null;

// --- Tema claro / oscuro ----------------------------------------------------
function applyThemeLabel() {
  const dark = document.documentElement.getAttribute("data-theme") === "dark";
  $("theme-label").textContent = dark ? "Tema claro" : "Tema oscuro";
}
$("theme-toggle").addEventListener("click", () => {
  const next = document.documentElement.getAttribute("data-theme") === "dark" ? "light" : "dark";
  document.documentElement.setAttribute("data-theme", next);
  try { localStorage.setItem("fd-theme", next); } catch (e) { /* almacenamiento no disponible */ }
  applyThemeLabel();
});
applyThemeLabel();

// --- Navegación entre vistas -------------------------------------------------
function showView(name) {
  document.querySelectorAll("section[data-view]").forEach((s) => { s.hidden = s.dataset.view !== name; });
  document.querySelectorAll(".nav-item").forEach((b) => {
    const active = b.dataset.view === name;
    b.classList.toggle("active", active);
    if (active) b.setAttribute("aria-current", "page"); else b.removeAttribute("aria-current");
  });
}
document.querySelectorAll(".nav-item").forEach((b) => b.addEventListener("click", () => showView(b.dataset.view)));

// --- Formulario Instalar -----------------------------------------------------
// Alcance y "Compilar" solo aplican a OSB; para SOA solo queda "Importar al servidor".
function refreshForm() {
  const target = $("target").value;
  document.querySelectorAll("#install-form [data-for]").forEach((el) => {
    el.hidden = !el.dataset.for.split(" ").includes(target);
  });
  if (target === "soa") {
    $("scopeGit").checked = true;
    $("doCompile").checked = true;
  }
  refreshCommands();
}

// Comando equivalente (los controles están en positivo; el comando usa los flags de consola).
function installArgs() {
  const osb = $("target").value === "osb";
  const args = [osb ? "-osb" : "-soa", $("name").value.trim() || "<nombre>"];
  if (osb && $("scopeProject").checked) args.push("--project");
  if (osb && !$("doCompile").checked) args.push("--skipCompile");
  if (!$("doImport").checked) args.push("--skipImport");
  return args;
}
function createArgs() {
  const args = ["-osb", $("createName").value.trim() || "<nombre>", "--create"];
  if ($("rest").checked) args.push("--rest");
  return args;
}
function refreshCommands() {
  $("cmdInstall").textContent = installArgs().join(" ");
  $("cmdCreate").textContent = createArgs().join(" ");
}

["target"].forEach((id) => $(id).addEventListener("change", refreshForm));
["name", "createName"].forEach((id) => $(id).addEventListener("input", () => { clearError(id); refreshCommands(); }));
["scopeGit", "scopeProject", "doCompile", "doImport", "rest"].forEach((id) => $(id).addEventListener("change", refreshCommands));

// Validación en línea: el error va bajo su propio campo.
function showError(inputId, errorId, text) {
  const input = $(inputId), err = $(errorId);
  input.setAttribute("aria-invalid", "true");
  err.textContent = text;
  err.hidden = false;
  input.focus();
}
function clearError(inputId) {
  const input = $(inputId);
  input.removeAttribute("aria-invalid");
  const err = $(inputId === "name" ? "nameError" : "createNameError");
  err.hidden = true;
}
function nameValid(inputId, errorId) {
  const v = $(inputId).value.trim();
  if (!v) { showError(inputId, errorId, "Escribe el nombre del servicio."); return false; }
  if (!NAME_RE.test(v)) { showError(inputId, errorId, "Solo letras, números, '_', '-' y '.'."); return false; }
  return true;
}

refreshForm();

// --- Entorno (Pre-vuelo) -----------------------------------------------------
function setChip(key, ok) {
  const chip = document.querySelector(`.chip[data-check="${key}"]`);
  if (chip) chip.dataset.state = ok === null ? "running" : ok ? "ok" : "alarm";
}

async function loadEnv() {
  const bar = $("preflight"), state = $("preflight-state"), detail = $("preflight-detail");
  try {
    const resp = await fetch("/api/bntag/config");
    if (!resp.ok) throw new Error("HTTP " + resp.status);
    const env = await resp.json();
    const checks = {
      java: [env.java, "No se encontró 'java' en el PATH"],
      oracle_home: [env.oracle_home, "Variable de ambiente ORACLE_HOME no definida"],
      properties: [env.properties, "Falta config/application.properties en " + env.bntag_home],
      jar: [env.jar, "Falta target/installbn-1.0.0.jar (mvn package)"],
    };
    const problems = [];
    for (const [key, [ok, msg]] of Object.entries(checks)) {
      setChip(key, !!ok);
      if (!ok) problems.push(msg);
    }
    bar.classList.toggle("has-alarm", problems.length > 0);
    state.textContent = problems.length ? `${problems.length} sin resolver` : "Listo para despegar";
    detail.hidden = problems.length === 0;
    detail.innerHTML = "";
    problems.forEach((p) => { const li = document.createElement("li"); li.textContent = p; detail.appendChild(li); });
  } catch (e) {
    ["java", "oracle_home", "properties", "jar"].forEach((k) => setChip(k, null));
    bar.classList.add("has-alarm");
    state.textContent = "No se pudo consultar el entorno";
    setStatus("No se pudo consultar el entorno", "fail");
  }
}
loadEnv();

// --- Pasos y log -----------------------------------------------------------
function renderSteps(names) {
  const ol = $("steps");
  ol.innerHTML = "";
  names.forEach((n) => {
    const li = document.createElement("li");
    li.dataset.step = n;
    li.dataset.status = "PENDING";
    li.innerHTML = '<i class="lamp" aria-hidden="true"></i><span class="name"></span><span class="state"></span><span class="msg"></span>';
    li.querySelector(".name").textContent = n;
    li.querySelector(".state").textContent = STATE_LABEL.PENDING;
    ol.appendChild(li);
  });
}

function setStep(name, status, msg) {
  const li = document.querySelector(`#steps li[data-step="${CSS.escape(name)}"]`);
  if (!li) return;
  li.dataset.status = status;
  li.querySelector(".state").textContent = STATE_LABEL[status] || status;
  li.querySelector(".msg").textContent = msg || "";
}

function appendLog(line) {
  const log = $("log");
  const atBottom = log.scrollTop + log.clientHeight >= log.scrollHeight - 20;
  const div = document.createElement("div");
  if (line.startsWith("ERROR:")) div.className = "err";
  else if (line.startsWith("AVISO:")) div.className = "warn";
  else if (line.startsWith("-----")) div.className = "sep";
  div.textContent = line || " ";
  log.appendChild(div);
  if (atBottom) log.scrollTop = log.scrollHeight;
}

$("logClear").addEventListener("click", () => { $("log").innerHTML = ""; });
$("logCopy").addEventListener("click", async () => {
  const btn = $("logCopy"), label = btn.querySelector("span");
  try {
    await navigator.clipboard.writeText($("log").innerText);
    label.textContent = "Copiado";
  } catch (e) {
    label.textContent = "No se pudo copiar";
  }
  setTimeout(() => { label.textContent = "Copiar"; }, 1500);
});

function setStatus(text, cls) {
  const s = $("status");
  s.className = "status" + (cls ? " " + cls : "");
  s.textContent = "";
  if (cls) {
    const i = document.createElement("i");
    i.className = "bi " + (cls === "ok" ? "bi-check-circle-fill" : "bi-exclamation-triangle-fill");
    i.setAttribute("aria-hidden", "true");
    s.append(i, " ");
  }
  s.append(text);
}

function setRunning(running) {
  for (const [id, idle, busy] of [["run", "Instalar", "Ejecutando…"], ["runCreate", "Crear proyecto", "Ejecutando…"]]) {
    $(id).disabled = running;
    $(id).querySelector("span").textContent = running ? busy : idle;
  }
  $("cancel").disabled = !running;
}

// --- Ejecución ---------------------------------------------------------------
function post(url, body) {
  return fetch(url, {
    method: "POST",
    headers: { "Content-Type": "application/json", "X-FlightDeck-Token": TOKEN },
    body: JSON.stringify(body || {}),
  });
}

$("install-form").addEventListener("submit", (ev) => {
  ev.preventDefault();
  if (!nameValid("name", "nameError")) return;
  const osb = $("target").value === "osb";
  // Los controles están en positivo (Compilar/Importar); el backend espera flags "skip".
  runJob({
    target: $("target").value,
    name: $("name").value.trim(),
    project: osb && $("scopeProject").checked,
    skipCompile: osb && !$("doCompile").checked,
    skipImport: !$("doImport").checked,
  });
});

$("create-form").addEventListener("submit", (ev) => {
  ev.preventDefault();
  if (!nameValid("createName", "createNameError")) return;
  runJob({ target: "osb", name: $("createName").value.trim(), create: true, rest: $("rest").checked });
});

async function runJob(body) {
  setRunning(true);
  setStatus("Iniciando…");
  $("log").innerHTML = "";
  let data;
  try {
    const resp = await post("/api/bntag/run", body);
    data = await resp.json();
    if (!resp.ok) throw new Error(data.error || "Error " + resp.status);
  } catch (e) {
    setStatus(e.message, "fail");
    setRunning(false);
    loadEnv();
    return;
  }
  currentJob = data.job_id;
  renderSteps(data.steps);
  setStatus("En ejecución: " + data.command);
  listen(currentJob);
}

function listen(jobId) {
  source = new EventSource(`/api/jobs/${jobId}/events`);
  source.onmessage = (m) => {
    const ev = JSON.parse(m.data);
    if (ev.type === "log") appendLog(ev.line);
    else if (ev.type === "step") setStep(ev.name, ev.status, ev.msg);
    else if (ev.type === "done") finish(ev.exit_code);
  };
  source.onerror = () => {
    // El servidor cierra el stream al terminar; si no llegó "done", es una desconexión real.
    if (source && currentJob) {
      source.close();
      source = null;
      setStatus("Conexión con FlightDeck perdida", "fail");
      setRunning(false);
    }
  };
}

function finish(exitCode) {
  if (source) { source.close(); source = null; }
  currentJob = null;
  setRunning(false);
  if (exitCode === 0) setStatus("Terminó correctamente (código 0)", "ok");
  else setStatus("Terminó con error (código " + exitCode + ")", "fail");
  loadEnv();
}

$("cancel").addEventListener("click", async () => {
  if (!currentJob) return;
  $("cancel").disabled = true;
  await post(`/api/jobs/${currentJob}/cancel`);
});
