package com.businessprocess.engine.model;

import com.businessprocess.core.model.businessprocess.BusinessProcessStep;
import lombok.Data;

import java.util.List;

@Data
public class BusinessProcessDefinition {
    private String processId;
    private String businessProcessName;
    private String stepId;
    private String stepName;
    private String version;
    private List<BusinessProcessStep> steps;
}
