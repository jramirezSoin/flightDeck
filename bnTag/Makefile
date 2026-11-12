# Genera config/application.properties a partir de config/application.properties.tpl
# usando variables de ambiente (los datos personales no se versionan).
#
# Variables requeridas:
#   BN_WORKSPACE_ROOT  p. ej. E:/Azure/TP_SOA.joseph.ramirez   (con "/", sin "/" final)
#   BN_WIN_USER        usuario de Windows; si no se define se usa USERNAME
#   SOA_HOSTNAME       host del servidor SOA
#
# Uso:  make config      (o simplemente: make)
#       make clean       (borra application.properties generado)

SHELL := /bin/bash

TPL := config/application.properties.tpl
OUT := config/application.properties

BN_WIN_USER ?= $(USERNAME)
export BN_WIN_USER

REQUIRED_VARS := BN_WORKSPACE_ROOT BN_WIN_USER SOA_HOSTNAME
SUBST_VARS    := $$BN_WORKSPACE_ROOT $$BN_WIN_USER $$SOA_HOSTNAME

.DELETE_ON_ERROR:
.PHONY: config clean

config: $(OUT)

$(OUT): $(TPL)
	@missing=0; \
	for v in $(REQUIRED_VARS); do \
		if [ -z "$${!v}" ]; then \
			echo "ERROR: falta la variable de ambiente $$v" >&2; \
			missing=1; \
		fi; \
	done; \
	if [ $$missing -ne 0 ]; then exit 1; fi
	envsubst '$(SUBST_VARS)' < $(TPL) > $(OUT)
	@echo "Generado $(OUT)"

clean:
	rm -f $(OUT)
