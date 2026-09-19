package com.businessprocess.businessprocess.persistence.model;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Builder
@Data
public class BusinessProcessTaskAttempt {
    private String attemptId;
    private String executionId;
    private String correlationId;
    private String stepId;
    private String taskId;

    private Integer attemptNumber;
    private Integer httpStatus;

    private String attemptType;
    private String providerName;
    private String operationName;
    private String endpoint;
    private String httpMethod;
    private String status;

    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private LocalDateTime createdAt;

    private Long durationMs;

    private String failureType;
    private String errorCode;
    private String errorMessage;
    private String retryReason;
    private String requestPayload;
    private String responsePayload;
    private String retryRequestId;
    private String traceId;
    private String spanId;

    private Boolean retryable;
}