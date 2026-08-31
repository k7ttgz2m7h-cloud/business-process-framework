package com.businessprocess.engine.engine;


import com.businessprocess.core.model.audit.Audit;
import com.businessprocess.engine.model.BusinessProcessContext;
import com.businessprocess.engine.model.BusinessProcessStatus;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BusinessProcessExecution {
    private String processId;
    private BusinessProcessStatus status;
    private String error;
    private BusinessProcessContext context;
    private Audit businessProcessAudit;
    private String compensationStatus;
    private String compensationError;
}
