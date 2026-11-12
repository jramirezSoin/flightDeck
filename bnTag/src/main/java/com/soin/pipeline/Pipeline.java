package com.soin.pipeline;

import java.util.ArrayList;
import java.util.List;

public class Pipeline {

    private final List<PipelineStep> steps = new ArrayList<>();

    public Pipeline addStep(PipelineStep step) {
        steps.add(step);
        return this;
    }

    /**
     * Ejecuta los steps en orden.
     *
     * @return true si todos los steps terminaron en SUCCESS o SKIPPED;
     *         false si alguno falló y el pipeline se detuvo.
     */
    public boolean execute(PipelineContext context) {

        for (PipelineStep step : steps) {

            String stepName = step.getClass().getSimpleName();
            System.out.println(
                "------------------------------------------------"
            );
            System.out.println(
                "Ejecutando: " + stepName
            );

            StepResult result = step.execute(context);

            System.out.println(
                stepName + " -> "
                + result.getStatus()
                + " : "
                + result.getMessage()
            );

            if (!result.isSuccess() && !result.isSkipped()) {

                System.out.println(
                    "Pipeline detenido en: " + stepName
                );

                return false;
            }
        }

        return true;
    }
}
