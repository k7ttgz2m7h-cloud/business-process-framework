package com.businessprocess.businessprocess.persistence.model;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class BusinessProcessExecutionDetails {
    private String executionId;
    private String businessProcessName;
    private String correlationId;
    private String status;
    private String failureType;
    private Object inputPayload;
    private Object outputPayload;
    private String errorMessage;
    private String compensationStatus;
    private String compensationError;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private Long durationMs;
    private List<BusinessProcessStepDetails> steps;
}
