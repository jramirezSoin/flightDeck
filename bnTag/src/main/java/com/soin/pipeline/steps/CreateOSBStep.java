package com.soin.pipeline.steps;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.soin.pipeline.PipelineContext;
import com.soin.pipeline.PipelineStep;
import com.soin.pipeline.StepResult;
import com.soin.services.osb.CreateOSB;

public class CreateOSBStep implements PipelineStep {

    @Override
    public StepResult execute(PipelineContext context) {
        try {

            Path carpeta = Paths.get(context.getOsbPath() + "/" + context.getArchivo());

            if (Files.isDirectory(carpeta)) {
                return StepResult.failed("OSB ya existe en " + context.getOsbPath() + ", no se creará nuevamente");
            }

            System.out.println("Creando OSB en " + context.getOsbPath() + "...");
            CreateOSB.create(context.getArchivo(), carpeta, context.isRest());

            return StepResult.success("OSB creado correctamente en " + context.getOsbPath());

        } catch (Exception e) {
            return StepResult.failed(
                "Error creando OSB en " + context.getOsbPath() + ": "
                + (e.getMessage() != null ? e.getMessage() : e.toString()));
        }
    }

}
