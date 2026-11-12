package com.soin.pipeline.steps;

import java.util.logging.Logger;

import com.soin.pipeline.PipelineContext;
import com.soin.pipeline.PipelineStep;
import com.soin.pipeline.StepResult;
import com.soin.services.ssh.ScpSender;

/** Sube al servidor el archivo del OSB o SOA (según el DeployTarget). */
public class UploadFileStep implements PipelineStep {

    private static final Logger LOG = Logger.getLogger(UploadFileStep.class.getName());

    private final DeployTarget target;

    public UploadFileStep(DeployTarget target) {
        this.target = target;
    }

    @Override
    public StepResult execute(PipelineContext context) {
        if (!context.isImport()) {
            return StepResult.skipped("Carga de " + target.label() + " no ejecutada porque se indicó --skipImport");
        }

        try {

            LOG.info("Subiendo archivo...");

            ScpSender.sendFile(
                    context.getHost(),
                    context.getPort(),
                    context.getUsername(),
                    context.getPrivateKey(),
                    context.getKnownHosts(),
                    target.localFile(context),
                    context.getRemoteFile() + target.remoteName(context));

            return StepResult.success("Archivo enviado correctamente");

        } catch (Exception e) {
            return StepResult.failed("Error enviando archivo: " + describe(e));
        }
    }

    private static String describe(Exception e) {
        return e.getMessage() != null ? e.getMessage() : e.toString();
    }
}
