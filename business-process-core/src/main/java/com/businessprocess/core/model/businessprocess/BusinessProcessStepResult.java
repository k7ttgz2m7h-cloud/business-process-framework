package com.businessprocess.core.model.businessprocess;

import com.businessprocess.core.model.audit.Audit;
import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class BusinessProcessStepResult {
    private String processId;
    private String stepId;
    private String stepName;
    private BusinessProcessStepStatus status;
    private String errorMessage;
    private Map<String, Object> output;
    private Audit businessProcessStepResultAudit;

    public static BusinessProcessStepResult success(String processId, String stepId, Map<String, Object> output) {
        Audit audit = new Audit();
        audit.complete();

        return BusinessProcessStepResult.builder()
                .processId(processId)
                .stepId(stepId)
                .status(BusinessProcessStepStatus.COMPLETED)
                .output(output)
                .businessProcessStepResultAudit(audit)
                .build();
    }

    public static BusinessProcessStepResult failure(String processId, String stepId, String errorMessage) {
        Audit audit = new Audit();
        audit.complete();

        return BusinessProcessStepResult.builder()
                .processId(processId)
                .stepId(stepId)
                .status(BusinessProcessStepStatus.FAILED)
                .errorMessage(errorMessage)
                .businessProcessStepResultAudit(audit)
                .build();
    }
}
