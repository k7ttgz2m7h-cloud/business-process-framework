package com.businessprocess.businessprocess.persistence.model;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class BusinessProcessTaskDetails {
    private String taskId;
    private Integer taskOrder;
    private String taskName;
    private String status;
    private String failureType;
    private Object inputPayload;
    private Object outputPayload;
    private String errorMessage;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private Long durationMs;
}
