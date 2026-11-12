"use strict";

const TOKEN = document.querySelector('meta[name="fd-token"]').content;
const $ = (id) => document.getElementById(id);
// Íconos de Bootstrap Icons por estado de paso.
const ICONS = {
  PENDING: '<i class="bi bi-circle"></i>',
  RUNNING: '<i class="bi bi-arrow-repeat spin"></i>',
  SUCCESS: '<i class="bi bi-check-circle-fill"></i>',
  FAILED: '<i class="bi bi-x-circle-fill"></i>',
  SKIPPED: '<i class="bi bi-dash-circle"></i>',
};

let currentJob = null;
let source = null;

// --- Tema claro / oscuro ----------------------------------------------------
function applyThemeLabel() {
  const dark = document.documentElement.getAttribute("data-bs-theme") === "dark";
  $("theme-label").textContent = dark ? "Tema claro" : "Tema oscuro";
}
$("theme-toggle").addEventListener("click", () => {
  const next = document.documentElement.getAttribute("data-bs-theme") === "dark" ? "light" : "dark";
  document.documentElement.setAttribute("data-bs-theme", next);
  try { localStorage.setItem("fd-theme", next); } catch (e) { /* almacenamiento no disponible */ }
  applyThemeLabel();
});
applyThemeLabel();

// --- Formulario: mostrar solo las opciones que aplican ---------------------
function refreshForm() {
  const target = $("target").value;
  const create = $("create").checked;
  document.querySelectorAll(".checks label").forEach((l) => {
    l.hidden = !l.dataset.for.split(" ").includes(target);
  });
  // --create no se combina con --project/--skipCompile/--skipImport; --rest requiere --create
  for (const id of ["project", "skipCompile", "skipImport"]) {
    $(id).disabled = target === "osb" && create;
    if ($(id).disabled) $(id).checked = false;
  }
  $("rest").disabled = !(target === "osb" && create);
  if ($("rest").disabled) $("rest").checked = false;
  if (target === "soa") {
    for (const id of ["create", "rest", "project", "skipCompile"]) $(id).checked = false;
  }
}
$("target").addEventListener("change", refreshForm);
$("create").addEventListener("change", refreshForm);
refreshForm();

// --- Estado del entorno ------------------------------------------------------
async function loadEnv() {
  try {
    const env = await (await fetch("/api/bntag/config")).json();
    const problems = [];
    if (!env.properties) problems.push("Falta config/application.properties en " + env.bntag_home);
    if (!env.jar) problems.push("Falta target/installbn-1.0.0.jar (mvn package)");
    if (!env.java) problems.push("No se encontró 'java' en el PATH");
    if (!env.oracle_home) problems.push("Variable de ambiente ORACLE_HOME no definida");
    const box = $("env-warnings");
    box.hidden = problems.length === 0;
    box.innerHTML = "";
    if (problems.length) {
      const ul = document.createElement("ul");
      problems.forEach((p) => { const li = document.createElement("li"); li.textContent = p; ul.appendChild(li); });
      box.append("Entorno incompleto:", ul);
    }
  } catch (e) {
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
    li.innerHTML = '<span class="icon"></span><span><span class="name"></span><span class="msg"></span></span>';
    li.querySelector(".name").textContent = n;
    li.querySelector(".icon").innerHTML = ICONS.PENDING;
    ol.appendChild(li);
  });
}

function setStep(name, status, msg) {
  const li = document.querySelector(`#steps li[data-step="${CSS.escape(name)}"]`);
  if (!li) return;
  li.dataset.status = status;
  li.querySelector(".icon").innerHTML = ICONS[status] || ICONS.PENDING;
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

function setStatus(text, cls) {
  const s = $("status");
  s.textContent = text;
  s.className = "status" + (cls ? " " + cls : "");
}

function setRunning(running) {
  $("run").disabled = running;
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

$("bntag-form").addEventListener("submit", async (ev) => {
  ev.preventDefault();
  const body = { target: $("target").value, name: $("name").value.trim() };
  for (const id of ["project", "skipCompile", "skipImport", "create", "rest"]) body[id] = $(id).checked;

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
});

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
