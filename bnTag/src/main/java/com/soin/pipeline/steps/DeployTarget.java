package com.soin.pipeline.steps;

import java.nio.file.Path;
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
    },

    /** XML de DVM generado por CheckDVMStep: solo se sube (no hay script de importación). */
    DVM("XML de DVM", null) {
        @Override
        String skipReason(PipelineContext context) {
            return context.getDvmXmlFile() == null
                    ? "No se generó el XML de DVM; no hay nada que enviar"
                    : null;
        }

        @Override
        String localFile(PipelineContext context) {
            return context.getDvmXmlFile();
        }

        @Override
        String remoteName(PipelineContext context) {
            Path name = Paths.get(context.getDvmXmlFile()).getFileName();
            return name.toString();
        }

        @Override
        String scriptArgument(PipelineContext context) {
            throw new UnsupportedOperationException("El XML de DVM no se importa con un script");
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

    /** Motivo para omitir la carga (por ejemplo, no hay archivo); null si se debe subir. */
    String skipReason(PipelineContext context) {
        return null;
    }

    /** Ruta local del archivo a subir. */
    abstract String localFile(PipelineContext context);

    /** Nombre del archivo en el directorio remoto. */
    abstract String remoteName(PipelineContext context);

    /** Argumento que recibe el script remoto. */
    abstract String scriptArgument(PipelineContext context);
}
