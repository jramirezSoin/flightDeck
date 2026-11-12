package com.soin.pipeline.steps;

import java.util.logging.Logger;


import com.soin.pipeline.PipelineContext;
import com.soin.pipeline.PipelineStep;
import com.soin.pipeline.StepResult;
import com.soin.services.osb.ImportService;
import com.soin.services.osb.ConfigJarXmlGenerator;

public class OSBCompile implements PipelineStep {

    private static final Logger LOG = Logger.getLogger(OSBCompile.class.getName());

    @Override
    public StepResult execute(PipelineContext context) {

        if (!context.isCompile()) {
            return StepResult.skipped("Compilación de OSB no ejecutada porque se indicó --skipCompile");
        }

        try {

            LOG.info("Compilando OSB...");

            ConfigJarXmlGenerator.generate(
                    context.getOsbPath() + context.getArchivo(),
                    context.getLocalFile() + context.getArchivoWithExtension(),
                    context.getGitFiles(),
                    context.isProject(),
                    "export-resource.xml");

            ImportService.importConfigJar("export-resource.xml");

            return StepResult.success("OSB compilado correctamente");

        } catch (Exception e) {
            return StepResult.failed(
                "Error compilando OSB: " + (e.getMessage() != null ? e.getMessage() : e.toString()));
        }
    }

}
