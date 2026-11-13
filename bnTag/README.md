# bnTag

Herramienta para compilar, subir e importar recursos OSB y SOA al servidor remoto, y para crear proyectos OSB nuevos desde una plantilla.

## Variables de ambiente

Los datos personales no se versionan. `config/application.properties` se **genera** a partir de `config/application.properties.tpl` usando estas variables.

### Requeridas

| Variable | Descripción | Ejemplo |
|----------|-------------|---------|
| `BN_WORKSPACE_ROOT` | Carpeta raíz que contiene `BNCR_OSB_WORKSPACE` y `BNCR_SOA_WORKSPACE`. Usar `/` y sin `/` final. | `E:/Azure/TP_SOA.joseph.ramirez` |
| `SOA_HOSTNAME` | Host del servidor Linux al que se suben e importan los artefactos (`server.host`). | `soa-host.example.com` |
| `ORACLE_HOME` | Instalación de Oracle. `installBN.sh` ya no la define: `classpath.txt` y `java_opts.txt` la referencian y se resuelven con `envsubst`. Usar `/` en la ruta. | `/c/Oracle/Middleware/Oracle_Home` |

### Con valor por defecto

| Variable | Descripción | Por defecto |
|----------|-------------|-------------|
| `BN_WIN_USER` | Usuario de Windows, usado en `C:/Users/<usuario>/...` (carpeta de JARs y llave SSH). | Variable `USERNAME` de Windows |
| `BNTAG_HOME` | Directorio de `bnTag`. Solo hace falta si `installBN.sh` se copió fuera del proyecto. | Directorio donde vive `installBN.sh` |

### Opcional

| Variable | Descripción |
|----------|-------------|
| `MW_HOME` | Antes `installBN.sh` la exportaba con el mismo valor de `ORACLE_HOME`; ya no. Los archivos del proyecto no la referencian, pero si las herramientas de Oracle (ConfigJar/WebLogic) la necesitan hay que definirla. |

## Instalación

Requisitos: Java 8, Maven, Git Bash con `envsubst`, y la llave SSH al servidor (ruta definida en `server.key`, por defecto `C:/Users/<BN_WIN_USER>/Documents/installBN/id_rsa`).

1. Definir las variables de ambiente (ver arriba), por ejemplo en `~/.bash_profile`:

   ```bash
   export BN_WORKSPACE_ROOT=E:/Azure/TP_SOA.joseph.ramirez
   export SOA_HOSTNAME=<host del servidor>
   export ORACLE_HOME=/c/Oracle/Middleware/Oracle_Home
   # opcional: export BN_WIN_USER=<usuario de Windows>
   ```

2. Compilar el proyecto:

   ```bash
   mvn package
   ```

3. Ejecutar `installBN.sh`. Si `config/application.properties` no existe, el script lo genera con `generate-config.sh` y falla indicando qué variable falta.

Para regenerar el archivo manualmente: borrar `config/application.properties` y ejecutar `./generate-config.sh`.

## Configuración (`application.properties`)

| Propiedad | Descripción |
|-----------|-------------|
| `osb.dir`, `soa.dir` | Workspaces locales de OSB y SOA |
| `dest.jar.dir` | Carpeta local donde se generan los JARs de OSB |
| `server.key` | Llave privada SSH |
| `server.host`, `server.port`, `server.user` | Conexión SSH al servidor |
| `server.remote.path` | Carpeta remota donde se suben los JARs |
| `server.script.dir` | Carpeta remota de `installOSB.sh` e `installSOA.sh` |
| `log.file` | **Opcional.** Además de la consola, guarda el log en este archivo (se agrega al final en cada ejecución, con fecha y nivel, e incluye la salida del servidor remoto). Si falla al abrirlo, avisa y continúa solo con consola |
| `server.known.hosts` | **Opcional.** Archivo `known_hosts` para verificar la huella del servidor SSH. Si no se define, no se verifica (`StrictHostKeyChecking=no`) y se imprime un aviso |

`server.port` y `server.script.dir` son obligatorias: si faltan, `LoadContext` detiene el pipeline indicando cuál.

## Uso

```bash
installBN.sh -osb <nombre OSB> [--project] [--skipCompile] [--skipImport]
installBN.sh -osb <nombre OSB> --create [--rest]
installBN.sh -soa <nombre SOA> [--skipImport]
```

| Opción | Efecto |
|--------|--------|
| `-osb <nombre>` / `-soa <nombre>` | Proyecto a procesar |
| `--project` | Procesa el OSB completo, no solo los recursos con cambios en `git status` |
| `--skipCompile` | Omite la compilación |
| `--skipImport` | Omite subida e importación |
| `--create` | Crea el proyecto OSB desde la plantilla |
| `--rest` | Con `--create`, incluye el ProxyService REST |
| `--dvmArea <área>` | Con `--create` de un OSB, área de aplicación de la DVM (ej. `SINPE_MOVIL`); sin ella no se genera ni se sube el XML de DVM |
| `--dvmComplexity <valor>` | Con `--dvmArea`: `simple` (por defecto), `complex` o `ramificado` |

El proceso termina con código `1` si algún paso falla.
