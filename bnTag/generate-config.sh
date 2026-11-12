#!/bin/bash
# Genera config/application.properties a partir de config/application.properties.tpl
# usando variables de ambiente (los datos personales no se versionan).
#
# Variables requeridas:
#   BN_WORKSPACE_ROOT  p. ej. E:/Azure/TP_SOA.joseph.ramirez   (con "/", sin "/" final)
#   BN_WIN_USER        usuario de Windows; si no se define se usa USERNAME
#   SOA_HOSTNAME       host del servidor SOA
#
# Uso: ./generate-config.sh
# Para regenerar: borrar config/application.properties y volver a ejecutar.

cd "$(dirname "$(readlink -f "${BASH_SOURCE[0]}")")" || exit 1

TPL="config/application.properties.tpl"
OUT="config/application.properties"

export BN_WIN_USER="${BN_WIN_USER:-$USERNAME}"

missing=0
for v in BN_WORKSPACE_ROOT BN_WIN_USER SOA_HOSTNAME; do
    if [ -z "${!v}" ]; then
        echo "ERROR: falta la variable de ambiente $v" >&2
        missing=1
    fi
done
[ "$missing" -eq 0 ] || exit 1

if [ ! -f "$TPL" ]; then
    echo "ERROR: no existe la plantilla $TPL" >&2
    exit 1
fi

# Se escribe a un temporal para no dejar un archivo a medias si algo falla.
envsubst '$BN_WORKSPACE_ROOT $BN_WIN_USER $SOA_HOSTNAME' < "$TPL" > "$OUT.tmp" \
    && mv "$OUT.tmp" "$OUT" \
    || { rm -f "$OUT.tmp"; echo "ERROR: no se pudo generar $OUT" >&2; exit 1; }

echo "Generado $OUT"
