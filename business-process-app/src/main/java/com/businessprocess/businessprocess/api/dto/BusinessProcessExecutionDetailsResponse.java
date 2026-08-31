package com.businessprocess.businessprocess.api.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class BusinessProcessExecutionDetailsResponse {
    private String executionId;
    private String businessProcessName;
    private String correlationId;
    private String status;
    private Object inputPayload;
    private Object outputPayload;
    private String errorMessage;
    private String compensationStatus;
    private String compensationError;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private Long durationMs;
    private List<StepDetails> steps;

    @Data
    @Builder
    public static class StepDetails {
        private String stepId;
        private Integer stepNumber;
        private String stepName;
        private String status;
        private Object inputPayload;
        private Object outputPayload;
        private String errorMessage;
        private LocalDateTime startedAt;
        private LocalDateTime completedAt;
        private Long durationMs;
        private List<TaskDetails> tasks;
    }

    @Data
    @Builder
    public static class TaskDetails {
        private String taskId;
        private Integer taskOrder;
        private String taskName;
        private String status;
        private Object inputPayload;
        private Object outputPayload;
        private String errorMessage;
        private LocalDateTime startedAt;
        private LocalDateTime completedAt;
        private Long durationMs;
    }
}
