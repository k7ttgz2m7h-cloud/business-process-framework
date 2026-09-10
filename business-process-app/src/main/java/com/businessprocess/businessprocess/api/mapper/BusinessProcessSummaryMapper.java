package com.businessprocess.businessprocess.api.mapper;

import com.businessprocess.businessprocess.persistence.model.BusinessProcessExecutionDetails;
import com.businessprocess.businessprocess.api.dto.BusinessProcessSummaryResponse;
import com.businessprocess.businessprocess.api.dto.BusinessProcessSummaryResponse.BusinessStatus;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class BusinessProcessSummaryMapper {

    public BusinessProcessSummaryResponse toResponse(
            BusinessProcessExecutionDetails details
    ) {
        return BusinessProcessSummaryResponse.builder()
                .correlationId(details.getCorrelationId())
                .businessProcessName(details.getBusinessProcessName())
                .overallStatus(toBusinessStatus(details.getStatus()))
                .startedAt(details.getStartedAt())
                .completedAt(details.getCompletedAt())
                .steps(details.getSteps().stream()
                        .map(step -> BusinessProcessSummaryResponse.StepSummary.builder()
                                .stepNumber(step.getStepNumber())
                                .stepName(step.getStepName())
                                .status(toBusinessStatus(step.getStatus()))
                                .tasks(step.getTasks().stream()
                                        .map(task -> BusinessProcessSummaryResponse.TaskSummary.builder()
                                                .taskOrder(task.getTaskOrder())
                                                .taskName(task.getTaskName())
                                                .status(toBusinessStatus(task.getStatus()))
                                                .build())
                                        .toList())
                                .build())
                        .toList())
                .build();
    }

    BusinessStatus toBusinessStatus(String status) {
        if (status == null || status.isBlank()) {
            return BusinessStatus.NOT_STARTED;
        }

        return switch (status.toUpperCase(Locale.ROOT)) {
            case "COMPLETED" -> BusinessStatus.PASSED;
            case "IN_PROGRESS", "STARTED", "COMPENSATING" -> BusinessStatus.IN_PROGRESS;
            case "NOT_STARTED", "PENDING", "CREATED" -> BusinessStatus.NOT_STARTED;
            default -> BusinessStatus.FAILED;
        };
    }
}
