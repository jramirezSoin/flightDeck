package com.soin.pipeline.steps;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.soin.pipeline.PipelineContext;
import com.soin.pipeline.PipelineStep;
import com.soin.pipeline.StepResult;
import com.soin.pipeline.StepStatus;
import com.soin.services.osb.CreateOSB;

public class CreateOSBStep implements PipelineStep {

    @Override
    public StepResult execute(PipelineContext context) {
        try {

            Path carpeta = Paths.get(context.getOsbPath() + "/" + context.getArchivo());

            if (Files.isDirectory(carpeta)) {
                System.out.println("El OSB ya existe en " + context.getOsbPath() + ", no se creará nuevamente.");
                return new StepResult(
                        StepStatus.FAILED,
                        "OSB ya existe en " + context.getOsbPath());
            } else {
                System.out.println("Creando OSB en " + context.getOsbPath() + "...");
                CreateOSB.create(context.getArchivo(), carpeta, context.isRest());
                return new StepResult(
                        StepStatus.SUCCESS,
                        "OSB creado correctamente en " + context.getOsbPath());
            }

        } catch (Exception e) {
            return new StepResult(
                    StepStatus.FAILED,
                    "Error creando OSB en " + context.getOsbPath() + ": " + e.getMessage());
        }
    }

}
