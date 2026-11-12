#!/bin/bash

# Directorio del proyecto: BNTAG_HOME si está definida; si no, el directorio donde
# vive este script (resolviendo symlinks), sin importar desde dónde se invoque.
BNTAG_HOME="${BNTAG_HOME:-$(dirname "$(readlink -f "${BASH_SOURCE[0]}")")}"
cd "$BNTAG_HOME" || { echo "ERROR: no se pudo entrar a $BNTAG_HOME" >&2; exit 1; }

if [ ! -f config/application.properties ]; then
    echo "config/application.properties no existe, generándolo con make..."
    make config/application.properties || exit 1
fi

export ORACLE_HOME="/c/Oracle/Middleware/Oracle_Home"
export MW_HOME="${ORACLE_HOME}"

JAVA_OPTS=$(envsubst < java_opts.txt | tr '\\' '/' | sed 's|C:|/c|g')
CLASSPATH="."
if [[ " $* " == *" -osb "* && " $* " != *" --skipCompile "* ]]; then
    rm -rf /c/Temp/osbinstance
    CLASSPATH=$(envsubst < classpath.txt | tr '\\' '/' | sed 's|C:|/c|g')
fi
echo "CLASSPATH: $CLASSPATH"
java $JAVA_OPTS -cp "target/installbn-1.0.0.jar:${CLASSPATH}" com.soin.App $@