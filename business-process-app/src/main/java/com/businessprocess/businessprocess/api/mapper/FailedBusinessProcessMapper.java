package com.businessprocess.businessprocess.api.mapper;

import com.businessprocess.businessprocess.api.dto.FailedBusinessProcessSearchResponse;
import com.businessprocess.businessprocess.persistence.service.BusinessProcessPersistenceService;
import com.businessprocess.core.policy.FailureType;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

@Component
public class FailedBusinessProcessMapper {

    public FailedBusinessProcessSearchResponse toResponse(
            Page<BusinessProcessPersistenceService.BusinessProcessExecutionDetails> executions
    ) {
        return FailedBusinessProcessSearchResponse.builder()
                .items(executions.getContent().stream().map(this::toFailedExecution).toList())
                .page(executions.getNumber())
                .size(executions.getSize())
                .totalElements(executions.getTotalElements())
                .totalPages(executions.getTotalPages())
                .build();
    }

    private FailedBusinessProcessSearchResponse.FailedExecution toFailedExecution(
            BusinessProcessPersistenceService.BusinessProcessExecutionDetails execution
    ) {
        var failedStep = execution.getSteps().stream()
                .filter(step -> "FAILED".equals(step.getStatus()))
                .findFirst()
                .orElse(null);
        var failedTask = failedStep == null ? null : failedStep.getTasks().stream()
                .filter(task -> "FAILED".equals(task.getStatus()))
                .findFirst()
                .orElse(null);
        FailureType failureType = resolveFailureType(execution.getFailureType(), failedTask);
        boolean retryEligible = failureType == FailureType.TECHNICAL_FAILURE
                || failureType == FailureType.UNKNOWN;

        return FailedBusinessProcessSearchResponse.FailedExecution.builder()
                .executionId(execution.getExecutionId())
                .businessProcessName(execution.getBusinessProcessName())
                .correlationId(execution.getCorrelationId())
                .status(execution.getStatus())
                .failedStep(failedStep == null ? null : FailedBusinessProcessSearchResponse.FailedStep.builder()
                        .stepId(failedStep.getStepId())
                        .stepNumber(failedStep.getStepNumber())
                        .stepName(failedStep.getStepName())
                        .build())
                .failedTask(failedTask == null ? null : FailedBusinessProcessSearchResponse.FailedTask.builder()
                        .taskId(failedTask.getTaskId())
                        .taskOrder(failedTask.getTaskOrder())
                        .taskName(failedTask.getTaskName())
                        .build())
                .failureType(failureType)
                .failureReason(resolveFailureReason(execution, failedStep, failedTask))
                .retryEligible(retryEligible)
                .retryEligibilityReason(retryEligible
                        ? "Retryable failure type"
                        : "Only TECHNICAL_FAILURE and UNKNOWN failures are retryable")
                .createdAt(execution.getStartedAt())
                .updatedAt(execution.getCompletedAt())
                .failedAt(execution.getCompletedAt())
                .build();
    }

    private FailureType resolveFailureType(
            String executionFailureType,
            BusinessProcessPersistenceService.BusinessProcessTaskDetails failedTask
    ) {
        String value = failedTask != null && failedTask.getFailureType() != null
                ? failedTask.getFailureType()
                : executionFailureType;
        return value == null ? FailureType.UNKNOWN : FailureType.valueOf(value);
    }

    private String resolveFailureReason(
            BusinessProcessPersistenceService.BusinessProcessExecutionDetails execution,
            BusinessProcessPersistenceService.BusinessProcessStepDetails failedStep,
            BusinessProcessPersistenceService.BusinessProcessTaskDetails failedTask
    ) {
        if (failedTask != null && failedTask.getErrorMessage() != null) {
            return failedTask.getErrorMessage();
        }
        if (failedStep != null && failedStep.getErrorMessage() != null) {
            return failedStep.getErrorMessage();
        }
        return execution.getErrorMessage();
    }
}
