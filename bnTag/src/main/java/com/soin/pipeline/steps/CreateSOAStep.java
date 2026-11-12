package com.soin.pipeline.steps;

import java.util.logging.Logger;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.soin.pipeline.PipelineContext;
import com.soin.pipeline.PipelineStep;
import com.soin.pipeline.StepResult;
import com.soin.services.soa.CreateSOA;

public class CreateSOAStep implements PipelineStep {

    private static final Logger LOG = Logger.getLogger(CreateSOAStep.class.getName());

    @Override
    public StepResult execute(PipelineContext context) {
        try {

            Path carpeta = Paths.get(context.getSoaPath() + "/" + context.getArchivo());

            if (Files.isDirectory(carpeta)) {
                return StepResult.failed("SOA ya existe en " + context.getSoaPath() + ", no se creará nuevamente");
            }

            LOG.info("Creando SOA en " + context.getSoaPath() + "...");
            CreateSOA.create(context.getArchivo(), carpeta, context.isRest());

            return StepResult.success("SOA creado correctamente en " + context.getSoaPath());

        } catch (Exception e) {
            return StepResult.failed(
                "Error creando SOA en " + context.getSoaPath() + ": "
                + (e.getMessage() != null ? e.getMessage() : e.toString()));
        }
    }

}
