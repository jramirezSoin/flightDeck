package com.soin.pipeline.steps;

import com.soin.pipeline.PipelineContext;
import com.soin.pipeline.PipelineStep;
import com.soin.pipeline.StepResult;
import com.soin.services.ssh.SshCommand;

/** Ejecuta en el servidor el script que importa el OSB o SOA (según el DeployTarget). */
public class ImportStep implements PipelineStep {

    private final DeployTarget target;

    public ImportStep(DeployTarget target) {
        this.target = target;
    }

    @Override
    public StepResult execute(PipelineContext context) {
        if (!context.isImport()) {
            return StepResult.skipped("Import de " + target.label() + " no ejecutado porque se indicó --skipImport");
        }

        try {
            System.out.println("Ejecutando script SSH...");

            String command = "sh " + context.getScriptDir() + target.script() + " "
                    + SshCommand.shellQuote(target.scriptArgument(context));
            System.out.println("Comando SSH: " + command);

            SshCommand.execute(
                    context.getHost(),
                    context.getPort(),
                    context.getUsername(),
                    context.getPrivateKey(),
                    context.getKnownHosts(),
                    command);

            return StepResult.success("Script SSH ejecutado correctamente");

        } catch (Exception e) {
            return StepResult.failed("Error ejecutando script SSH: "
                    + (e.getMessage() != null ? e.getMessage() : e.toString()));
        }
    }

}
