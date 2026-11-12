package com.soin.pipeline.steps;

import com.soin.pipeline.PipelineContext;
import com.soin.pipeline.PipelineStep;
import com.soin.pipeline.StepResult;
import com.soin.pipeline.StepStatus;
import com.soin.services.git.GitService;

public class GitStatusStep implements PipelineStep {

    @Override
    public StepResult execute(PipelineContext context) {
        if(!context.isOSB() || !context.isCompile()) {
            System.out.println("Revisión de cambios en Git no ejecutado porque no es un OSB o se indicó que no se compile");
            return new StepResult(
                StepStatus.SKIPPED,
                "Revisión de cambios en Git no ejecutado porque no es un OSB o se indicó que no se compile"
            );
        }

        try {
            

            if (context.isOSB() && !context.isProject()) {
                System.out.println("Ejecutando en modo Resource");
                System.out.println("Revisando cambios en Git...");
                context.setGitFiles(GitService.getStatus(context.getOsbPath(), context.getArchivo()));

                if (context.getGitFiles().isEmpty()) {
                    System.out.println("No hay cambios en el proyecto " + context.getOsbPath() + " para el OSB " + context.getArchivo());
                    return new StepResult(
                        StepStatus.FAILED,
                        "No hay cambios en el proyecto " + context.getOsbPath() + " para el OSB " + context.getArchivo()
                    );
                }

            } else{
                System.out.println("Ejecutando en modo Project");
                    return new StepResult(
                    StepStatus.SKIPPED,
                    "Revisión de cambios en Git no ejecutado porque se indicó modo --project (se incluye el proyecto completo)"
                );
            }

            return new StepResult(
                StepStatus.SUCCESS,
                "Revisión de cambios en Git ejecutado correctamente"
            );

        } catch (Exception e) {
            return new StepResult(
                StepStatus.FAILED,
                "Error ejecutando revisión de cambios en Git: " + e.getMessage()
            );
        }
    }
    
}
