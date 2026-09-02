package com.businessprocess.businessprocess.api.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class BusinessProcessSummaryResponse {
    private String correlationId;
    private String businessProcessName;
    private BusinessStatus overallStatus;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private List<StepSummary> steps;

    public enum BusinessStatus {
        NOT_STARTED,
        IN_PROGRESS,
        PASSED,
        FAILED
    }

    @Data
    @Builder
    public static class StepSummary {
        private Integer stepNumber;
        private String stepName;
        private BusinessStatus status;
        private List<TaskSummary> tasks;
    }

    @Data
    @Builder
    public static class TaskSummary {
        private Integer taskOrder;
        private String taskName;
        private BusinessStatus status;
    }
}
