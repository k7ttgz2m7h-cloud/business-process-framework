package com.businessprocess.businessprocess.api.controller;

import com.businessprocess.businessprocess.persistence.model.BusinessProcessExecutionDetails;
import com.businessprocess.businessprocess.persistence.model.BusinessProcessStepDetails;
import com.businessprocess.businessprocess.persistence.model.BusinessProcessTaskDetails;
import com.businessprocess.businessprocess.api.dto.BusinessProcessExecutionRequest;
import com.businessprocess.businessprocess.api.dto.BusinessProcessExecutionDetailsResponse;
import com.businessprocess.businessprocess.api.dto.BusinessProcessExecutionDetailsRequest;
import com.businessprocess.businessprocess.api.dto.BusinessProcessExecutionResponse;
import com.businessprocess.core.model.base.BasePayload;
import com.businessprocess.engine.engine.BusinessProcessExecution;
import com.businessprocess.engine.engine.BusinessProcessMonitor;
import com.businessprocess.engine.model.BusinessProcessDefinition;
import com.businessprocess.engine.service.BusinessProcessOrchestrationService;
import com.businessprocess.businessprocess.persistence.entity.BusinessProcessDefinitionEntity;
import com.businessprocess.businessprocess.persistence.service.BusinessProcessPersistenceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/businessprocessflow")
@Slf4j
public class BusinessProcessController {

    private final BusinessProcessOrchestrationService orchestrationService;
    private final BusinessProcessMonitor businessProcessMonitor;
    private final BusinessProcessPersistenceService businessProcessPersistenceService;

    public BusinessProcessController(
            BusinessProcessOrchestrationService orchestrationService,
            BusinessProcessMonitor businessProcessMonitor,
            BusinessProcessPersistenceService businessProcessPersistenceService
    ) {
        this.orchestrationService = orchestrationService;
        this.businessProcessMonitor = businessProcessMonitor;
        this.businessProcessPersistenceService = businessProcessPersistenceService;
    }

    @PostMapping("/execute")
    public ResponseEntity<BusinessProcessExecutionResponse> execute(@RequestBody BusinessProcessExecutionRequest request) {
        if (request.getBusinessProcessName() == null || request.getBusinessProcessName().isBlank()) {
            throw new IllegalArgumentException("businessProcessName is required");
        }
        String correlationId = UUID.randomUUID().toString();
        log.info("Received businessProcess execution request for process: {}, correlationId: {}",
                request.getBusinessProcessName(), correlationId);

        BusinessProcessDefinitionEntity businessProcessDefinition =
                businessProcessPersistenceService.getBusinessProcessDefinition(request.getBusinessProcessName());
        BusinessProcessDefinition businessProcess = orchestrationService.parseBusinessProcessTemplate(businessProcessDefinition.getBusinessProcessTemplate());
        String processKey = businessProcessDefinition.getBusinessProcessName();

        BusinessProcessExecution execution = orchestrationService.executeBusinessProcessflow(businessProcess, payload(request.getInputPayload()));
        businessProcessPersistenceService.saveBusinessProcessExecution(
                execution,
                businessProcessDefinition,
                processKey,
                correlationId
        );

        BusinessProcessExecutionResponse response = buildResponse(execution, correlationId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{executionId}")
    public ResponseEntity<BusinessProcessExecutionResponse> getExecution(@PathVariable String executionId) {
        log.info("Fetching execution details for executionId: {}", executionId);

        var steps = businessProcessMonitor.getBusinessProcessSteps(executionId);
        if (steps.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        BusinessProcessExecutionResponse response = BusinessProcessExecutionResponse.builder()
                .executionId(executionId)
                .stepResults(steps)
                .metrics(businessProcessMonitor.getBusinessProcessMetrics(executionId))
                .build();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{executionId}/metrics")
    public ResponseEntity<?> getMetrics(@PathVariable String executionId) {
        log.info("Fetching metrics for executionId: {}", executionId);

        var metrics = businessProcessMonitor.getBusinessProcessMetrics(executionId);
        if (metrics == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(metrics);
    }

    @PostMapping("/execution")
    public ResponseEntity<BusinessProcessExecutionDetailsResponse> getExecutionDetails(
            @RequestBody BusinessProcessExecutionDetailsRequest request
    ) {
        var details = businessProcessPersistenceService.getBusinessProcessExecutionDetails(
                request.getBusinessProcessName(),
                request.getCorrelationId()
        );
        return ResponseEntity.ok(toResponse(details));
    }

    private BusinessProcessExecutionResponse buildResponse(BusinessProcessExecution execution, String correlationId) {
        return BusinessProcessExecutionResponse.builder()
                .executionId(execution.getProcessId())
                .correlationId(correlationId)
                .status(execution.getStatus())
                .error(execution.getError())
                .compensationStatus(execution.getCompensationStatus())
                .compensationError(execution.getCompensationError())
                .stepResults(businessProcessMonitor.getBusinessProcessSteps(execution.getProcessId()))
                .metrics(businessProcessMonitor.getBusinessProcessMetrics(execution.getProcessId()))
                .build();
    }

    private BasePayload<Object> payload(Object value) {
        if (value == null) {
            return null;
        }
        BasePayload<Object> payload = new BasePayload<>() {};
        payload.setPayload(value);
        return payload;
    }

    private BusinessProcessExecutionDetailsResponse toResponse(
            BusinessProcessExecutionDetails details
    ) {
        return BusinessProcessExecutionDetailsResponse.builder()
                .executionId(details.getExecutionId())
                .businessProcessName(details.getBusinessProcessName())
                .correlationId(details.getCorrelationId())
                .status(details.getStatus())
                .inputPayload(details.getInputPayload())
                .outputPayload(details.getOutputPayload())
                .errorMessage(details.getErrorMessage())
                .compensationStatus(details.getCompensationStatus())
                .compensationError(details.getCompensationError())
                .startedAt(details.getStartedAt())
                .completedAt(details.getCompletedAt())
                .durationMs(details.getDurationMs())
                .steps(details.getSteps().stream()
                        .map(this::toStepResponse)
                        .toList())
                .build();
    }

    private BusinessProcessExecutionDetailsResponse.StepDetails toStepResponse(
            BusinessProcessStepDetails step
    ) {
        return BusinessProcessExecutionDetailsResponse.StepDetails.builder()
                .stepId(step.getStepId())
                .stepNumber(step.getStepNumber())
                .stepName(step.getStepName())
                .status(step.getStatus())
                .inputPayload(step.getInputPayload())
                .outputPayload(step.getOutputPayload())
                .errorMessage(step.getErrorMessage())
                .startedAt(step.getStartedAt())
                .completedAt(step.getCompletedAt())
                .durationMs(step.getDurationMs())
                .tasks(step.getTasks().stream()
                        .map(this::toTaskResponse)
                        .toList())
                .build();
    }

    private BusinessProcessExecutionDetailsResponse.TaskDetails toTaskResponse(
            BusinessProcessTaskDetails task
    ) {
        return BusinessProcessExecutionDetailsResponse.TaskDetails.builder()
                .taskId(task.getTaskId())
                .taskOrder(task.getTaskOrder())
                .taskName(task.getTaskName())
                .status(task.getStatus())
                .inputPayload(task.getInputPayload())
                .outputPayload(task.getOutputPayload())
                .errorMessage(task.getErrorMessage())
                .startedAt(task.getStartedAt())
                .completedAt(task.getCompletedAt())
                .durationMs(task.getDurationMs())
                .build();
    }
}
