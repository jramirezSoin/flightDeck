package com.soin.pipeline.steps;

import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.soin.pipeline.PipelineContext;
import com.soin.pipeline.PipelineStep;
import com.soin.pipeline.StepResult;
import com.soin.services.osb.DVMXmlGenerator;
import com.soin.services.ssh.SshCommand;

/**
 * Paso opcional de Create OSB: consulta en el servidor el siguiente número de DVM del proyecto
 * y, si lo encuentra, genera el XML correspondiente. Ante cualquier problema devuelve SKIPPED
 * (no detiene la creación del OSB).
 */
public class CheckDVMStep implements PipelineStep {

    private static final Logger LOG = Logger.getLogger(CheckDVMStep.class.getName());

    private static final String OUTPUT_FILE = "templatesAtomicos.xml";
    private static final Pattern DVM_PATTERN = Pattern.compile("SIGUIENTE NUMERO DE DVM:\\s*(\\d+)");

    @Override
    public StepResult execute(PipelineContext context) {

        try {
            LOG.info("Consultando el siguiente número de DVM...");

            String command = "sh " + context.getScriptDir() + "checkNextProjectDVM.sh "
                    + SshCommand.shellQuote(context.getArchivo());
            LOG.info("Comando SSH: " + command);

            String resultado = SshCommand.execute(
                    context.getHost(),
                    context.getPort(),
                    context.getUsername(),
                    context.getPrivateKey(),
                    context.getKnownHosts(),
                    command);

            Matcher matcher = DVM_PATTERN.matcher(resultado);
            if (!matcher.find()) {
                return StepResult.skipped(
                        "No se encontró el número de DVM en la salida del servidor; no se genera el XML");
            }

            int dvmNumber = Integer.parseInt(matcher.group(1));
            context.setDvmNumber(dvmNumber);

            DVMXmlGenerator.generate(context.getArchivo(), String.valueOf(dvmNumber), OUTPUT_FILE);

            return StepResult.success("Siguiente número de DVM: " + dvmNumber + ". XML generado: " + OUTPUT_FILE);

        } catch (Exception e) {
            return StepResult.skipped("No se pudo revisar la DVM (paso opcional): "
                    + (e.getMessage() != null ? e.getMessage() : e.toString()));
        }
    }

}
