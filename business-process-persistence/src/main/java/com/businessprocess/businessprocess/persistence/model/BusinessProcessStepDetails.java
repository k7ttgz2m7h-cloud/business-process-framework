package com.businessprocess.businessprocess.persistence.model;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class BusinessProcessStepDetails {
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
    private List<BusinessProcessTaskDetails> tasks;
}
