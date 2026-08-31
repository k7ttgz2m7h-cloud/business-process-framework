package com.businessprocess.businessprocess.api.dto;

import com.businessprocess.core.model.businessprocess.BusinessProcessStepResult;
import com.businessprocess.engine.model.BusinessProcessMetrics;
import com.businessprocess.engine.model.BusinessProcessStatus;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class BusinessProcessExecutionResponse {
    private String executionId;
    private String correlationId;
    private BusinessProcessStatus status;
    private String error;
    private String compensationStatus;
    private String compensationError;
    private List<BusinessProcessStepResult> stepResults;
    private BusinessProcessMetrics metrics;
}
