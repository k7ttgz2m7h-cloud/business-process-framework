package com.businessprocess.core.model.businessprocess;

import com.businessprocess.core.delegate.TaskDelegate;
import com.businessprocess.core.model.audit.Audit;
import com.businessprocess.core.model.task.Task;
import com.businessprocess.core.model.task.TaskStatus;
import com.businessprocess.core.policy.FailurePolicy;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class BusinessProcessStep {
    private String processId;
    private String stepId;
    private Integer stepNumber;
    private String stepName;
    private List<Task> tasks;
    private String providerName;     // set from YAML e.g. "order-service"
    private TaskDelegate provider;   // resolved at runtime via ProviderRegistry
    private TaskStatus status;
    private Audit businessProcessStepAudit;
    private BusinessProcessStep compensation; // per-step compensation defined in YAML
    private FailurePolicy failurePolicy; // failure policy and decision based on step and compensation tasks
    // For saga pattern

    public BusinessProcessStep() {
        // Default for deserialization
        this.businessProcessStepAudit = new Audit();
    }

    public BusinessProcessStep(String stepName) {
        this.stepName = stepName;
        this.tasks = new ArrayList<>();
        this.businessProcessStepAudit = new Audit(); // Creates audit with start time
    }

    public BusinessProcessStep(String stepName, TaskDelegate provider, List<Task> taskList) {
        this.stepName = stepName;
        this.provider = provider;
        this.tasks = taskList;
        this.businessProcessStepAudit = new Audit();
        this.status = TaskStatus.STARTED; // Initialize with STARTED status
    }

    public void markStarted() {
        this.businessProcessStepAudit = new Audit();
        this.status = TaskStatus.STARTED;
    }

    public void complete() {
        switch (determineStepStatus()) {
            case COMPLETED:
                this.status = TaskStatus.COMPLETED;
                this.businessProcessStepAudit.complete();
                break;
            case FAILED:
                this.status = TaskStatus.FAILED;
                this.businessProcessStepAudit.complete();
                break;
            case COMPENSATING:
                this.status = TaskStatus.COMPENSATING;
                break;
            case STARTED:
                this.status = TaskStatus.STARTED;
                break;
        }
    }

    private TaskStatus determineStepStatus() {
        if (tasks.stream().anyMatch(task -> task.getStatus() == TaskStatus.FAILED)) {
            return TaskStatus.FAILED;
        }
        if (tasks.stream().anyMatch(task -> task.getStatus() == TaskStatus.COMPENSATING)) {
            return TaskStatus.COMPENSATING;
        }
        if (tasks.stream().allMatch(task -> task.getStatus() == TaskStatus.COMPLETED)) {
            return TaskStatus.COMPLETED;
        }
        return TaskStatus.STARTED;
    }

    private boolean allTasksCompleted() {
        return tasks.stream()
                .allMatch(task -> task.getStatus() == TaskStatus.COMPLETED);
    }

    private boolean anyTaskFailed() {
        return tasks.stream()
                .anyMatch(task -> task.getStatus() == TaskStatus.FAILED);
    }

}
