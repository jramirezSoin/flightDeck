package com.soin.pipeline;

public interface PipelineStep {

    StepResult execute(PipelineContext context);

}
