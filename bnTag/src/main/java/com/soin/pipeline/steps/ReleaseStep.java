package com.soin.pipeline.steps;

import java.util.logging.Logger;

import com.soin.pipeline.PipelineContext;
import com.soin.pipeline.PipelineStep;
import com.soin.pipeline.StepResult;
import com.soin.services.release.*;

public class ReleaseStep implements PipelineStep {

    private static final Logger LOG = Logger.getLogger(ReleaseStep.class.getName());

    @Override
    public StepResult execute(PipelineContext context) {
        if ("".equals(context.getRelease())) {
            return StepResult.skipped("Inserción a release no ejecutado porque se no indicó --release");
        }


        try {
            LOG.info("Verificación de release "+context.getRelease());
            CreateRelease.create(context.getRelease(), context.getReleaseDir());
            return StepResult.success("Inserción a release ejecutado correctamente");

        } catch (Exception e) {
            return StepResult.failed(
                "Error ejecutando Inserción a release: "
                + (e.getMessage() != null ? e.getMessage() : e.toString()));
        }
    }

}
