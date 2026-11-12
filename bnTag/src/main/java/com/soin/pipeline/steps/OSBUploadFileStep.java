package com.soin.pipeline.steps;

import com.soin.pipeline.PipelineContext;
import com.soin.pipeline.PipelineStep;
import com.soin.pipeline.StepResult;
import com.soin.pipeline.StepStatus;
import com.soin.services.ssh.ScpSender;

public class OSBUploadFileStep implements PipelineStep {

    @Override
    public StepResult execute(PipelineContext context) {
        if (!context.isOSB() || !context.isImport()) {
            System.out.println("Carga de OSB no ejecutado porque no es un OSB o se indicó que no se Cargue");
            return new StepResult(
                    StepStatus.SKIPPED,
                    "Carga de OSB no ejecutado porque no es un OSB o se indicó que no se Cargue");
        }

        try {

            System.out.println("Subiendo archivo...");

            ScpSender.sendFile(
                    context.getHost(),
                    context.getPort(),
                    context.getUsername(),
                    context.getPrivateKey(),
                    context.getLocalFile() + context.getArchivoWithExtension(),
                    context.getRemoteFile() + context.getArchivoWithExtension());

            return new StepResult(
                    StepStatus.SUCCESS,
                    "Archivo enviado correctamente");

        } catch (Exception e) {

            return new StepResult(
                    StepStatus.FAILED,
                    "Error enviando archivo: " + e.getMessage());
        }
    }
}
