package com.soin.pipeline;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Pipeline {

    private static final Logger LOG = Logger.getLogger(Pipeline.class.getName());

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
            LOG.info("------------------------------------------------");
            LOG.info("Ejecutando: " + stepName);

            StepResult result = step.execute(context);

            Level level = result.isSuccess() || result.isSkipped() ? Level.INFO : Level.SEVERE;
            LOG.log(level, stepName + " -> " + result.getStatus() + " : " + result.getMessage());

            if (!result.isSuccess() && !result.isSkipped()) {

                LOG.severe("Pipeline detenido en: " + stepName);

                return false;
            }
        }

        return true;
    }
}
