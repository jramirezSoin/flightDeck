package com.soin.pipeline.steps;

import com.soin.pipeline.PipelineContext;
import com.soin.pipeline.PipelineStep;
import com.soin.pipeline.StepResult;
import com.soin.pipeline.StepStatus;
import com.soin.services.ssh.SshCommand;

public class OSBImport implements PipelineStep {

    @Override
    public StepResult execute(PipelineContext context) {
        if (!context.isOSB() || !context.isImport()) {
            System.out.println("Import de OSB no ejecutado porque no es un OSB o se indicó que no se Importe");
            return new StepResult(
                    StepStatus.SKIPPED,
                    "Import de OSB no ejecutado porque no es un OSB o se indicó que no se Importe");
        }
        try {
            System.out.println("Ejecutando script SSH...");
            String command = "sh " + context.getScriptDir() + "installOSB.sh "
                    + SshCommand.shellQuote(context.getArchivo());
            System.out.println("Comando SSH: " + command);
            SshCommand.execute(
                    context.getHost(),
                    context.getPort(),
                    context.getUsername(),
                    context.getPrivateKey(),
                    command);

            // System.out.println("Resultado del script SSH: " + result);

            return new StepResult(
                    StepStatus.SUCCESS,
                    "script SSH ejecutado correctamente");

        } catch (Exception e) {
            return new StepResult(
                    StepStatus.FAILED,
                    "Error ejecutando script SSH: " + e.getMessage());
        }
    }

}
