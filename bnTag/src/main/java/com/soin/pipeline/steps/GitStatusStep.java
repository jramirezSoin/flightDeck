package com.soin.pipeline.steps;

import com.soin.pipeline.PipelineContext;
import com.soin.pipeline.PipelineStep;
import com.soin.pipeline.StepResult;
import com.soin.services.git.GitService;

public class GitStatusStep implements PipelineStep {

    @Override
    public StepResult execute(PipelineContext context) {
        if (!context.isCompile()) {
            return StepResult.skipped("Revisión de cambios en Git no ejecutada porque se indicó --skipCompile");
        }

        if (context.isProject()) {
            return StepResult.skipped("Revisión de cambios en Git no ejecutada porque se indicó modo --project (se incluye el proyecto completo)");
        }

        try {
            System.out.println("Ejecutando en modo Resource");
            System.out.println("Revisando cambios en Git...");

            context.setGitFiles(GitService.getStatus(context.getOsbPath(), context.getArchivo()));

            if (context.getGitFiles().isEmpty()) {
                return StepResult.failed(
                    "No hay cambios en el proyecto " + context.getOsbPath() + " para el OSB " + context.getArchivo());
            }

            return StepResult.success("Revisión de cambios en Git ejecutada correctamente");

        } catch (Exception e) {
            return StepResult.failed(
                "Error ejecutando revisión de cambios en Git: "
                + (e.getMessage() != null ? e.getMessage() : e.toString()));
        }
    }

}
