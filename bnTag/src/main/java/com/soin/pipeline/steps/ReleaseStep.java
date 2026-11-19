package com.soin.pipeline.steps;

import java.util.logging.Logger;

import java.io.IOException;
import java.nio.file.*;

import com.soin.pipeline.PipelineContext;
import com.soin.pipeline.PipelineStep;
import com.soin.pipeline.StepResult;
import com.soin.services.release.*;

public class ReleaseStep implements PipelineStep {

    private static final Logger LOG = Logger.getLogger(ReleaseStep.class.getName());

    private DeployTarget target = null;

    public ReleaseStep(DeployTarget target) {
        this.target = target;
    }

    public ReleaseStep() {
    }

    @Override
    public StepResult execute(PipelineContext context) {
        if ("".equals(context.getRelease())) {
            return StepResult.skipped("Inserción a release no ejecutado porque se no indicó --release");
        }


        try {
            LOG.info("Verificación de release "+context.getRelease());
            Path carpeta = Paths.get(context.getReleaseDir() + "/" + context.getRelease());

            //if (Files.isDirectory(carpeta)) {
             //   return StepResult.failed("Release ya existe en " + context.getReleaseDir() + ", no se creará nuevamente");
            //}
            String tipo = (context.isOSB())?"OSB":((context.isSOA())?"SOA":"release");
            CreateRelease.create(context.getRelease(), carpeta,tipo);
            if(target!=null){
                LOG.info("Archivo a copiar "+target.localFile(context));
                Path copiado = copiarArchivo(target.localFile(context), context.getReleaseDir() + "/" + context.getRelease() + "/JARs/" + tipo);
                context.setReleasedJar(copiado.toString());
            }
                
            return StepResult.success("Inserción a release ejecutado correctamente");

        } catch (Exception e) {
            return StepResult.failed(
                "Error ejecutando Inserción a release: "
                + (e.getMessage() != null ? e.getMessage() : e.toString()));
        }
    }

    public static Path copiarArchivo(String origen, String carpetaDestino) throws IOException {
        Path archivoOrigen = Paths.get(origen);
        Path carpeta = Paths.get(carpetaDestino);

        // Crea la carpeta de destino si no existe
        Files.createDirectories(carpeta);

        // Mantiene el mismo nombre del archivo dentro de la carpeta destino
        Path archivoDestino = carpeta.resolve(archivoOrigen.getFileName());

        Files.copy(archivoOrigen, archivoDestino, StandardCopyOption.REPLACE_EXISTING);
        return archivoDestino;
    }

}
