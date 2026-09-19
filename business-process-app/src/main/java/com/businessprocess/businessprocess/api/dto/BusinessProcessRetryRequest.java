package com.businessprocess.businessprocess.api.dto;

import lombok.Data;

@Data
public class BusinessProcessRetryRequest {
    private String executionId;
    private String correlationId;
    private String businessProcessName;
}
