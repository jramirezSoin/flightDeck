package com.soin.pipeline;

public class StepResult {

    private final StepStatus status;
    private final String message;

    public StepResult(StepStatus status, String message) {
        this.status = status;
        this.message = message;
    }

    public static StepResult success(String message) {
        return new StepResult(StepStatus.SUCCESS, message);
    }

    public static StepResult failed(String message) {
        return new StepResult(StepStatus.FAILED, message);
    }

    public static StepResult skipped(String message) {
        return new StepResult(StepStatus.SKIPPED, message);
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
