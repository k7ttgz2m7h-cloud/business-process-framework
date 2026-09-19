package com.businessprocess.businessprocess.api.dto;

import lombok.Builder;
import lombok.Data;

@Data @Builder
public class BusinessProcessRetryResponse {
    private String correlationId;
    private String originalExecutionId;
    private String executionId;
    private String executionMode;
    private String resumedFromStep;
    private String resumedFromTask;
    private String status;
}
