package com.soin.pipeline.steps;


import com.soin.pipeline.PipelineContext;
import com.soin.pipeline.PipelineStep;
import com.soin.pipeline.StepResult;
import com.soin.pipeline.StepStatus;
import com.soin.services.osb.ImportService;
import com.soin.services.osb.ConfigJarXmlGenerator;

public class OSBCompile implements PipelineStep {

@Override
    public StepResult execute(PipelineContext context) {

        if(!context.isOSB() || !context.isCompile()) {
            System.out.println("Compilación de OSB no ejecutado porque no es un OSB o se indicó que no se compile");
            return new StepResult(
                StepStatus.SKIPPED,
                "Compilación de OSB no ejecutado porque no es un OSB o se indicó que no se compile"
            );
        }

        try {

            System.out.println("Compilando OSB...");

            ConfigJarXmlGenerator.generate(
                    context.getOsbPath() + context.getArchivo(),
                    context.getLocalFile() + context.getArchivoWithExtension(),
                    context.getGitFiles(),
                    context.isProject(),
                    "export-resource.xml");

            ImportService.importConfigJar("export-resource.xml");

            return new StepResult(
                StepStatus.SUCCESS,
                "OSB compilado correctamente"
            );

            

        } catch (Exception e) {

            return new StepResult(
                StepStatus.FAILED,
                "Error compilando OSB: " + e.getMessage()
            );
        }
    }
    
}
