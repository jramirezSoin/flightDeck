package com.soin.pipeline;

public class StepResult {

    private final StepStatus status;
    private final String message;

    public StepResult(StepStatus status, String message) {
        this.status = status;
        this.message = message;
    }

    public StepStatus getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    public boolean isSuccess() {
        return status == StepStatus.SUCCESS;
    }

    public boolean isSkipped() {
        return status == StepStatus.SKIPPED;
    }
}
