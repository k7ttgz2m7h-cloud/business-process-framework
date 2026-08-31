package com.businessprocess.businessprocess.api.dto;

import lombok.Data;

@Data
public class BusinessProcessExecutionRequest {
    private String businessProcessName;
    private Object inputPayload;
}
