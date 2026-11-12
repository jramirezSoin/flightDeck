package com.soin.pipeline.steps;

import java.nio.file.Paths;

import com.soin.pipeline.PipelineContext;

/**
 * Diferencias entre desplegar un OSB y un SOA: qué archivo local se sube,
 * con qué nombre queda en el servidor y qué script remoto lo importa.
 */
public enum DeployTarget {

    OSB("OSB", "installOSB.sh") {
        @Override
        String localFile(PipelineContext context) {
            return context.getLocalFile() + context.getArchivoWithExtension();
        }

        @Override
        String remoteName(PipelineContext context) {
            return context.getArchivoWithExtension();
        }

        @Override
        String scriptArgument(PipelineContext context) {
            return context.getArchivo();
        }
    },

    SOA("SOA", "installSOA.sh") {
        @Override
        String localFile(PipelineContext context) {
            return Paths.get(context.getSoaPath(), context.getArchivo(), "deploy", context.getArchivoSOA()).toString();
        }

        @Override
        String remoteName(PipelineContext context) {
            return context.getArchivoSOA();
        }

        @Override
        String scriptArgument(PipelineContext context) {
            return context.getArchivoSOA();
        }
    };

    private final String label;
    private final String script;

    DeployTarget(String label, String script) {
        this.label = label;
        this.script = script;
    }

    /** Nombre para mostrar en mensajes ("OSB" / "SOA"). */
    String label() {
        return label;
    }

    /** Nombre del script remoto que importa el archivo. */
    String script() {
        return script;
    }

    /** Ruta local del archivo a subir. */
    abstract String localFile(PipelineContext context);

    /** Nombre del archivo en el directorio remoto. */
    abstract String remoteName(PipelineContext context);

    /** Argumento que recibe el script remoto. */
    abstract String scriptArgument(PipelineContext context);
}
