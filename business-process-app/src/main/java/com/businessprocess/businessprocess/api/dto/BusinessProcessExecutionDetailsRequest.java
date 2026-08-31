package com.businessprocess.businessprocess.api.dto;

import lombok.Data;

@Data
public class BusinessProcessExecutionDetailsRequest {
    private String businessProcessName;
    private String correlationId;
}
