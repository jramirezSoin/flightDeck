# DESIGN.md — FlightDeck

Dirección de diseño del frontend (`flightDeck/static/`). Estado: **propuesta, pendiente de aprobación**.
Fuentes: `design-system/flightdeck/MASTER.md` (ui-ux-pro-max, `--persist`) + criterio de interface-design.

## 1. Intención

- **Quién:** Joseph, desarrollador de integración en BNCR, dentro de una VDI. Repite el mismo ciclo muchas veces al día: instalar un OSB/SOA, mirar si pasó, volver a intentar. Rara vez crea proyectos nuevos.
- **Qué debe lograr:** lanzar una operación con las opciones correctas, ver de un vistazo en qué paso va y si falló, y leer el log sin pelearse con la UI.
- **Cómo debe sentirse:** una cabina de instrumentos, no un panel de administración. Sobrio, denso, silencioso; el color solo aparece cuando algo cambia de estado. Calma de cockpit de noche en el modo oscuro, y la misma disciplina en claro.

## 2. Exploración del dominio

- **Dominio (FlightDeck / BNCR):** lista de verificación pre-vuelo, plan de vuelo, despegue, bitácora, torre de control, luces anunciadoras (annunciator), instrumentos, pista, servicios OSB/SOA, dominio WebLogic, "pipeline" de pasos.
- **Mundo de color:** panel de cabina gris pizarra de noche · ámbar de instrumento · verde fósforo de radar · rojo de alarma maestra · blanco de balizas de pista · gris de placas serigrafiadas.
- **Firma (solo existe en este producto):** **la tira de lámparas anunciadoras.** Cada paso del pipeline y cada requisito del entorno es una lámpara: apagada (pendiente), ámbar (en curso), verde (listo), roja (falló), tenue con raya (omitido). El panel "Pasos" se lee como una checklist pre-vuelo, y una tira "Pre-vuelo" reemplaza la alerta amarilla actual con las lámparas de Java, ORACLE_HOME, `application.properties` y el JAR.
- **Defaults que se rechazan:**
  1. Tarjetas blancas con sombra sobre fondo gris claro (plantilla de admin) → superficies por tono y bordes de 1 px, sin sombras dramáticas.
  2. Botón primario azul Bootstrap + checkboxes de marca → un único acento verde "go", con lámparas para el estado.
  3. Un `alert-warning` grande para el entorno → tira de lámparas compacta con detalle al pasar el cursor/foco.
- **Prueba sin nombre:** quitado el nombre, la tira de lámparas y la checklist siguen diciendo "operación por pasos con verificación previa".

## 3. Estructura y jerarquía

- **Foco por vista:** Instalar → el botón **Instalar** y el campo Nombre; durante la ejecución → el paso en curso. Crear OSB → el campo Nombre.
- **Layout (escritorio ≥ 1100 px):** barra lateral 216 px (mismo fondo que el lienzo, solo borde derecho) + zona de trabajo en dos columnas:
  - Izquierda **360 px**: formulario (configuración). Ancho fijo, declara "controles al servicio del resultado".
  - Derecha fluida: **Pasos** (checklist, alto por contenido) sobre **Bitácora** (log, ocupa el resto).
- **Barra de comando:** línea monoespaciada de solo lectura bajo el formulario con el comando exacto que se lanzará (`-osb Nombre --project`). Es útil para quien conoce la consola y desmitifica las opciones.
- **Ritmo:** control denso en el formulario (gaps de 8–12 px), aire real (24 px) entre formulario y resultados.
- **Menú:** grupo **InstallBN** → **Instalar**, **Crear OSB** (ya existente). Las nuevas herramientas se agregan como grupos.
- **< 1100 px:** una columna (formulario → Pasos → Bitácora). **< 760 px:** la barra lateral pasa a barra superior.

## 4. Tokens

Prefijo `--fd-`. Nombres del dominio, no genéricos.

### Color (un solo matiz azul-pizarra; solo cambia la luminosidad entre superficies)

| Token | Oscuro | Claro | Uso |
|---|---|---|---|
| `--fd-panel` (lienzo) | `#0C121D` | `#F3F5F8` | fondo de la app y de la barra lateral |
| `--fd-deck` (nivel 1) | `#121A28` | `#FFFFFF` | tarjetas, formulario |
| `--fd-deck-2` (nivel 2) | `#182234` | `#F7F9FB` | inputs (inset), cabeceras de panel |
| `--fd-deck-3` (nivel 3) | `#1F2B40` | `#EDF1F5` | hover, popovers |
| `--fd-hairline` | `rgba(255,255,255,.08)` | `rgba(15,23,42,.10)` | bordes estándar |
| `--fd-hairline-strong` | `rgba(255,255,255,.16)` | `rgba(15,23,42,.20)` | bordes de control, énfasis |
| `--fd-ink` | `#E8EEF6` | `#0F172A` | texto primario |
| `--fd-ink-2` | `#B4C0D0` | `#334155` | secundario |
| `--fd-ink-3` | `#8494A9` | `#566579` | metadatos |
| `--fd-ink-off` | `#5B6B80` | `#8895A7` | deshabilitado |
| `--fd-go` (acento) | `#22C55E` | `#15803D` | acción primaria, foco, selección |
| `--fd-on-go` | `#04130A` | `#FFFFFF` | texto sobre el acento |
| `--fd-amber` | `#F5B83D` | `#A15C00` | en curso, aviso |
| `--fd-alarm` | `#F2635B` | `#C62828` | falla, error |
| `--fd-ok` | `#22C55E` | `#15803D` | paso terminado (misma familia que `--fd-go`: verde = "listo/avanza") |
| `--fd-log-bg` | `#080D15` | `#0C121D` | la bitácora es oscura en ambos temas |

Proporción 60/30/10: ~60 % lienzo, ~30 % superficies, ≤ 10 % color con significado. Contraste de texto ≥ 4,5:1 en ambos temas (a validar en la VDI). Nunca violeta.

### Tipografía

- **IBM Plex Sans** (interfaz) + **JetBrains Mono** (nombres de servicio, flags, comando, log, cifras). Par recomendado por ui-ux-pro-max para herramientas de desarrollo; sustituye a Inter.
- **Escala 1,2 (terciopelo, denso)** sobre base de 13 px: `11 · 13 · 15 · 18 · 22 · 26`.

| Rol | Tamaño / peso / color |
|---|---|
| Título de vista | 22 / 600 / `ink`, tracking −0,01em |
| Cabecera de panel | 11 / 600 / `ink-3`, MAYÚSCULAS, tracking +0,06em |
| Etiqueta de campo | 12 / 500 / `ink-2` |
| Cuerpo y controles | 13 / 400–500 / `ink` |
| Ayuda | 12 / 400 / `ink-3` |
| Dato y log | mono 12,5 / 400 / `ink` (cifras con `tabular-nums`) |

La jerarquía descansa en peso + color, no solo en tamaño. `text-wrap: balance` en títulos, `pretty` en párrafos; `-webkit-font-smoothing: antialiased`.

### Espaciado, radios, profundidad

- **Unidad base 4 px**; escala: `4 · 8 · 12 · 16 · 24 · 32`. Densidad **alta** (dial 8/10): padding de tarjeta 16, gap de formulario 12.
- **Control: 32 px de alto** (36 el botón primario). Es una app de escritorio con puntero; se acepta por debajo de 44 px siempre que el área de clic efectiva sea ≥ 40 px (padding/pseudo-elemento) y no haya áreas superpuestas. *Ajuste explícito frente a la regla de 44 px de ui-ux-pro-max, pensado para móvil.*
- **Radios:** `--fd-r-sm: 4px` (inputs, botones, chips), `--fd-r-md: 8px` (tarjetas/paneles), `--fd-r-lg: 12px` (diálogos). Radio concéntrico: exterior = interior + padding.
- **Profundidad — una sola estrategia: tono de superficie + borde hairline.** Sin sombras en oscuro; en claro, un único anillo de 1 px. Los inputs son más oscuros que su tarjeta (inset).
- **Movimiento:** solo `transform` y `opacity`/color; 120–160 ms, `cubic-bezier(0.23,1,0.32,1)`; press `scale(.97)`; el paso en curso respira con un pulso de la lámpara ámbar (2 s) y se detiene con `prefers-reduced-motion`. Sin animaciones al hacer scroll. Nada de `transition: all`.

## 5. Componentes y estados

Cada componente define: reposo, hover, activo (press), foco, deshabilitado. Los datos, además: cargando, vacío, error.

- **Botón primario** (`--fd-go`), **secundario** (borde hairline-strong), **peligro/cancelar** (borde `alarm`). Icono + verbo+objeto ("Instalar", "Crear proyecto", "Cancelar").
- **Campo de texto / select:** fondo `deck-2`, borde hairline; foco = anillo 2 px `--fd-go`; error = borde `alarm` + mensaje bajo el campo (patrón de nombre inválido, mensaje en español).
- **Interruptor y tarjeta de alcance:** selección = borde + tinte `go` al 12 %; nunca solo por color (el control incluye marca de check).
- **Lámpara (firma):** círculo de 8 px + texto; estados `off · running(ámbar, pulso) · ok · alarm · skipped`. Siempre acompañada de icono/texto para no depender solo del color.
- **Tira Pre-vuelo:** 4 lámparas (Java, ORACLE_HOME, properties, JAR). Todo en verde = una línea tenue; algo en rojo = la tira se expande con la causa (equivale al aviso actual).
- **Paso:** lámpara + nombre en mono + mensaje corto; el paso en curso sobre `deck-3`.
- **Bitácora:** fondo `log-bg`, ámbar para AVISO, rojo para ERROR, separadores atenuados; botones Copiar y Limpiar; auto-scroll solo si estaba al fondo (comportamiento actual).
- **Estados de datos:**
  - *Vacío* — Pasos: "Aún no hay ejecución"; Bitácora: "Aquí aparecerá la salida de la ejecución".
  - *Cargando* — Iniciando…: botón primario deshabilitado con indicador; el entorno consulta con lámparas en `running`.
  - *Error* — falla de red/servidor en la barra de estado (rojo, con icono) y, si ocurre en la verificación del entorno, lámpara roja con la causa.
- **Foco visible** en todo elemento interactivo, **navegación por teclado completa** (menú, subitems, tarjetas de alcance como radios nativos, interruptores como checkbox nativos), enlace "Saltar al contenido", `aria-live` en el estado y en el log, `aria-current="page"` en el ítem activo.

## 6. Tema claro / oscuro

Mismo matiz, solo cambia la luminosidad; se respeta `prefers-color-scheme` en la primera carga y la elección se guarda (`localStorage`). La bitácora se mantiene oscura en ambos temas. Dark: bordes en vez de sombras, semánticos ligeramente desaturados.

## 7. Decisiones por confirmar

1. **Bootstrap:** recomiendo **quitar Bootstrap** y escribir CSS propio sobre estos tokens. Con tokens propios, Bootstrap solo aporta reset y pelea con la identidad (es el origen del aspecto "plantilla"). *Alternativa:* conservarlo solo para reset/rejilla y sobrescribir todo.
2. **CDN vs. local:** hoy fuentes (Google Fonts) e iconos (Bootstrap Icons) vienen de CDN. El CLAUDE.md pide no asumir internet en la VDI. Recomiendo **dejarlos por CDN solo si confirmas acceso**, y de lo contrario copiarlos a `static/` y que `server.py` los sirva (ya sirve `/static/*` con los tipos correctos; `.woff2`/`.svg` los resuelve `mimetypes`).
3. **Iconos:** mantener Bootstrap Icons (ya cargados) o pasar a Lucide (recomendado por ui-ux-pro-max). Recomiendo mantener Bootstrap Icons para no sumar dependencias.
4. **Verbo del botón principal:** "Instalar" (claro) frente a algo temático como "Despegar". Recomiendo **"Instalar"**; lo temático queda solo en la lámpara y la checklist.

## 8. Notas sobre la herramienta de diseño

El `--design-system` devolvió el patrón "FAQ/Documentation Landing", que **no aplica** (es una app, no una landing); se descarta. Se adoptan de MASTER.md solo el par tipográfico (JetBrains Mono + IBM Plex Sans), la base oscura slate y el acento verde "run". La paleta y la firma son decisión propia de este documento.

## 9. Qué no se puede probar aquí

Todo lo visual (contraste real, densidad, carga de fuentes) se valida en la VDI; ver el checklist al final de la fase 2.
