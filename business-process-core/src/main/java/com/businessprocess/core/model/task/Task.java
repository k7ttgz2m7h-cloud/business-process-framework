package com.businessprocess.core.model.task;

import com.businessprocess.core.model.audit.Audit;
import com.businessprocess.core.model.base.BasePayload;
import com.businessprocess.core.model.base.BaseTask;
import com.businessprocess.core.policy.FailureType;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;


@Data
@EqualsAndHashCode(callSuper = true)
public class Task extends BaseTask {
    // Task type and configuration
    private Map<String, Object> taskConfig;
    private String stepId;
    private String processId;
    private String isCompensationStep;
    private Boolean compensationTask;
    private String originalTaskId;
    private String originalTaskName;
    private String error;
    private FailureType failureType;
    private Object result; //set result of the task execution

    // Payload handling
    private BasePayload requestPayload;
    private BasePayload responsePayload; // prepare the response that needs to be passed from this Step to other

   /* // Task execution details
    private TaskDelegate taskDelegate;
    private String executorId;
    private Map<String, Object> executionContext;
    private Duration timeout;*/

    //Execution Tracking
    private Integer retryCount;
    private Integer maxRetries = 3;
    private Duration executionDuration;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;

    private Audit taskAudit;

    // Constructor
    public Task() {
        this.setCreatedAt(LocalDateTime.now());
        this.setRetryCount(0);
        this.setStatus(TaskStatus.CREATED);
        this.taskConfig = new HashMap<>();
        //this.executionContext = new HashMap<>();
    }

    // Helper methods
    public void markStarted() {
        this.taskAudit = new Audit();
        this.setCreatedAt(this.taskAudit.getStartTime());
        this.setStatus(TaskStatus.IN_PROGRESS);
    }

    public void markCompleted() {
        this.setStatus(TaskStatus.COMPLETED);
        this.taskAudit.complete();
        this.setCompletedAt(this.taskAudit.getEndTime());
        this.setExecutionDuration(this.taskAudit.getExecutionDuration());
    }

    public void markFailed(String errorMessage) {
        //Audit runtime fix - dirty fix
        if (this.taskAudit == null) {
            this.taskAudit = new Audit();
        }
        //
        this.setStatus(TaskStatus.FAILED);
        this.taskAudit.complete();
        this.setCompletedAt(this.taskAudit.getEndTime());
        this.setExecutionDuration(this.taskAudit.getExecutionDuration());
    }


    public boolean canRetry() {
        return this.getRetryCount() < this.getMaxRetries();
    }
}
