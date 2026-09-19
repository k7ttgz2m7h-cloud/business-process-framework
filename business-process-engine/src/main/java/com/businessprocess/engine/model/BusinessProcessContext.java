package com.businessprocess.engine.model;

import com.businessprocess.core.model.base.BasePayload;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Getter
public class BusinessProcessContext {
    private final String executionId;
    private final BusinessProcessDefinition businessProcess;
    private final Map<String, BasePayload> stepResults = new HashMap<>();
    private final Map<String, BasePayload> taskResults = new HashMap<>();

    @Setter
    private BusinessProcessStatus status;
    @Setter
    private BasePayload initialPayload;

    public BusinessProcessContext(BusinessProcessDefinition businessProcess) {
        this.executionId = UUID.randomUUID().toString();
        this.businessProcess = businessProcess;
    }

    public void addStepOutput(String stepId, BasePayload output) {
        stepResults.put(stepId, output);
    }

    public BasePayload getStepOutput(String stepId) {
        return stepResults.get(stepId);
    }

    public boolean hasStepOutput(String stepId) {
        return stepResults.containsKey(stepId);
    }

    public void addTaskOutput(String taskId, BasePayload output) {
        taskResults.put(taskId, output);
    }

    public BasePayload getTaskOutput(String taskId) {
        return taskResults.get(taskId);
    }

    public boolean hasTaskOutput(String taskId) {
        return taskResults.containsKey(taskId);
    }

    public void restoreOutputs(RestoredExecutionState restoredState) {
        if (restoredState == null || restoredState.steps() == null) return;
        restoredState.steps().forEach((stepId, step) -> {
            if (step != null) {
                if (step.stepOutput() != null) stepResults.put(stepId, (BasePayload) step.stepOutput());
                if (step.taskOutputs() != null) step.taskOutputs().forEach((taskId, output) -> taskResults.put(taskId, (BasePayload) output));
            }
        });
    }
}
