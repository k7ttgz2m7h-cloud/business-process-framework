package com.businessprocess.businessprocess.api.dto;

import com.businessprocess.core.policy.FailureType;
import lombok.Data;

import java.util.List;

@Data
public class FailedBusinessProcessSearchRequest {
    private List<FailureType> failureTypes;
    private String businessProcessName;
    private Integer page = 0;
    private Integer size = 20;
    private String sortBy = "failedAt";
    private String sortDirection = "DESC";
}
