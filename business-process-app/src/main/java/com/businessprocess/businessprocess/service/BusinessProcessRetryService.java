package com.businessprocess.businessprocess.service;

import com.businessprocess.businessprocess.api.dto.BusinessProcessRetryRequest;
import com.businessprocess.businessprocess.api.dto.BusinessProcessRetryResponse;
import com.businessprocess.businessprocess.persistence.entity.BusinessProcessDefinitionEntity;
import com.businessprocess.businessprocess.persistence.service.BusinessProcessDefinitionPersistence;
import com.businessprocess.businessprocess.persistence.service.BusinessProcessExecutionPersistence;
import com.businessprocess.businessprocess.persistence.service.BusinessProcessExecutionQuery;
import com.businessprocess.businessprocess.persistence.model.BusinessProcessExecutionDetails;
import com.businessprocess.core.model.base.BasePayload;
import com.businessprocess.core.model.task.TaskStatus;
import com.businessprocess.engine.engine.BusinessProcessEngine;
import com.businessprocess.engine.model.BusinessProcessDefinition;
import com.businessprocess.engine.model.RestoredExecutionState;
import com.businessprocess.engine.model.RestoredStepState;
import com.businessprocess.engine.service.BusinessProcessOrchestrationService;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class BusinessProcessRetryService {
    private final BusinessProcessExecutionQuery executionQuery;
    private final BusinessProcessDefinitionPersistence definitionPersistence;
    private final BusinessProcessExecutionPersistence executionPersistence;
    private final BusinessProcessOrchestrationService orchestrationService;
    private final BusinessProcessEngine engine;

    public BusinessProcessRetryService(BusinessProcessExecutionQuery executionQuery,
                                        BusinessProcessDefinitionPersistence definitionPersistence,
                                        BusinessProcessExecutionPersistence executionPersistence,
                                        BusinessProcessOrchestrationService orchestrationService,
                                        BusinessProcessEngine engine) {
        this.executionQuery = executionQuery;
        this.definitionPersistence = definitionPersistence;
        this.executionPersistence = executionPersistence;
        this.orchestrationService = orchestrationService;
        this.engine = engine;
    }

    public BusinessProcessRetryResponse retry(BusinessProcessRetryRequest request) {
        var original = executionQuery.getDetailsByExecutionId(request.getExecutionId());
        if (original == null) {
            throw new IllegalArgumentException("No execution found for executionId: " + request.getExecutionId());
        }
        if (!request.getExecutionId().equals(original.getExecutionId())) {
            throw new IllegalArgumentException("executionId does not belong to correlationId");
        }
        if (!request.getCorrelationId().equals(original.getCorrelationId())) {
            throw new IllegalArgumentException("correlationId does not belong to executionId");
        }
        if (!request.getBusinessProcessName().equals(original.getBusinessProcessName())) {
            throw new IllegalArgumentException("businessProcessName does not belong to executionId");
        }
        if (!original.getStatus().startsWith("FAILED")) {
            throw new IllegalStateException("Only failed executions can be retried, current status: " + original.getStatus());
        }
        if ("FAILED_COMPENSATED".equals(original.getStatus()) || "FAILED_COMPENSATION_FAILED".equals(original.getStatus())) {
            throw new IllegalStateException("Compensated executions cannot be resumed safely, current status: " + original.getStatus());
        }

        var failedStep = original.getSteps().stream()
                .filter(s -> "FAILED".equals(s.getStatus()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Failed step not found for execution: " + original.getExecutionId()));

        var failedTask = failedStep.getTasks().stream()
                .filter(t -> "FAILED".equals(t.getStatus()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Failed task not found in step: " + failedStep.getStepId()));

        var definitionEntity = definitionPersistence.getDefinition(original.getBusinessProcessName());
        var definition = orchestrationService.parseBusinessProcessTemplate(definitionEntity.getBusinessProcessTemplate());

        Map<String, RestoredStepState> restored = new LinkedHashMap<>();
        for (var step : original.getSteps()) {
            Map<String, BasePayload<?>> tasks = new LinkedHashMap<>();
            Map<String, TaskStatus> taskStatuses = new LinkedHashMap<>();
            for (var task : step.getTasks()) {
                if (task.getOutputPayload() != null) {
                    tasks.put(task.getTaskId(), payload(task.getOutputPayload()));
                }
                if (task.getStatus() != null) {
                    taskStatuses.put(task.getTaskId(), TaskStatus.valueOf(task.getStatus()));
                }
            }
            restored.put(step.getStepId(), new RestoredStepState(step.getStepId(), payload(step.getOutputPayload()), tasks, taskStatuses));
            if (step.getStepId().equals(failedStep.getStepId())) break;
        }

        var execution = engine.executeFrom(definition, payload(original.getInputPayload()), failedStep.getStepId(), failedTask.getTaskId(), new RestoredExecutionState(restored));

        executionPersistence.save(execution, definitionEntity, original.getBusinessProcessName(), original.getCorrelationId());

        return BusinessProcessRetryResponse.builder()
                .correlationId(original.getCorrelationId())
                .originalExecutionId(original.getExecutionId())
                .executionId(execution.getProcessId())
                .executionMode("MANUAL_RESUME")
                .resumedFromStep(failedStep.getStepId())
                .resumedFromTask(failedTask.getTaskId())
                .status(execution.getStatus().name())
                .build();
    }

    private BasePayload<Object> payload(Object value) { BasePayload<Object> payload = new BasePayload<>() {}; payload.setPayload(value); return payload; }
}
