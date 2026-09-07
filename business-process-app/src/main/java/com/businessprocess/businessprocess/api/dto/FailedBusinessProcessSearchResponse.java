package com.businessprocess.businessprocess.api.dto;

import com.businessprocess.core.policy.FailureType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class FailedBusinessProcessSearchResponse {
    private List<FailedExecution> items;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;

    @Data
    @Builder
    public static class FailedExecution {
        private String executionId;
        private String businessProcessName;
        private String correlationId;
        private String status;
        private FailedStep failedStep;
        private FailedTask failedTask;
        private FailureType failureType;
        private String failureReason;
        private boolean retryEligible;
        private String retryEligibilityReason;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private LocalDateTime failedAt;
    }

    @Data
    @Builder
    public static class FailedStep {
        private String stepId;
        private Integer stepNumber;
        private String stepName;
    }

    @Data
    @Builder
    public static class FailedTask {
        private String taskId;
        private Integer taskOrder;
        private String taskName;
    }
}
