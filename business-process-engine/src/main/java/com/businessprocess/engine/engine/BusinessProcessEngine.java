package com.businessprocess.engine.engine;

import com.businessprocess.core.delegate.TaskDelegate;
import com.businessprocess.core.model.audit.Audit;
import com.businessprocess.core.model.base.BasePayload;
import com.businessprocess.core.model.task.Task;
import com.businessprocess.core.model.businessprocess.BusinessProcessStep;
import com.businessprocess.core.model.businessprocess.BusinessProcessStepResult;
import com.businessprocess.core.model.businessprocess.BusinessProcessStepStatus;
import com.businessprocess.core.policy.FailureAction;
import com.businessprocess.core.policy.FailurePolicy;
import com.businessprocess.core.policy.FailureType;
import com.businessprocess.core.policy.RetryPolicy;
import com.businessprocess.core.util.CompensationExecutor;
import com.businessprocess.engine.model.BusinessProcessContext;
import com.businessprocess.engine.model.BusinessProcessDefinition;
import com.businessprocess.engine.model.BusinessProcessStatus;
import com.businessprocess.engine.parser.PayloadPathResolver;
import com.businessprocess.businessprocess.discovery.BusinessProcessServiceDiscoveryClient;
import com.businessprocess.businessprocess.discovery.BusinessProcessServiceEndpoint;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class BusinessProcessEngine {
    private final CompensationExecutor compensationHandler;
    private final BusinessProcessMonitor monitor;
    private final RetryPolicy retryPolicy;
    private final BusinessProcessServiceDiscoveryClient serviceDiscoveryClient;
    private final PayloadPathResolver payloadPathResolver;

    public BusinessProcessEngine(
            CompensationExecutor compensationHandler,
            BusinessProcessMonitor monitor,
            RetryPolicy retryPolicy,
            BusinessProcessServiceDiscoveryClient serviceDiscoveryClient,
            PayloadPathResolver payloadPathResolver
    ) {
        this.compensationHandler = compensationHandler;
        this.monitor = monitor;
        this.retryPolicy = retryPolicy;
        this.serviceDiscoveryClient = serviceDiscoveryClient;
        this.payloadPathResolver = payloadPathResolver;
    }

    // Public API

    public BusinessProcessExecution execute(BusinessProcessDefinition businessProcess) {
        return execute(businessProcess, null);
    }

    public BusinessProcessExecution execute(BusinessProcessDefinition businessProcess, BasePayload initialPayload) {
        Audit businessProcessAudit = new Audit();
        BusinessProcessContext context = new BusinessProcessContext(businessProcess);
        context.setInitialPayload(initialPayload);
        List<BusinessProcessStep> completedSteps = new ArrayList<>();
        log.info("Starting businessProcess execution: executionId={}, processId={}",
                context.getExecutionId(), resolveBusinessProcessIdentity(businessProcess));

        context.setStatus(BusinessProcessStatus.IN_PROGRESS);

        try {
            BusinessProcessExecution execution = executeBusinessProcess(businessProcess.getSteps(), context, completedSteps);
            businessProcessAudit.complete();
            execution.setBusinessProcessAudit(businessProcessAudit);
            return execution;
        } catch (Exception e) {
            log.error("Business process execution failed: executionId={}", context.getExecutionId(), e);
            BusinessProcessExecution compensated = runCompensation(completedSteps, context, e.getMessage());
            businessProcessAudit.complete();
            if (compensated != null) {
                compensated.setBusinessProcessAudit(businessProcessAudit);
                return compensated;
            }
            BusinessProcessExecution failed = buildFailedExecution(context.getExecutionId(), e.getMessage(), context);
            failed.setBusinessProcessAudit(businessProcessAudit);
            return failed;
        }
    }

    // Main businessProcess control

    private BusinessProcessExecution executeBusinessProcess(List<BusinessProcessStep> steps, BusinessProcessContext context, List<BusinessProcessStep> completedSteps) {
        for (BusinessProcessStep step : steps) {
            BusinessProcessStepResult result = executeBusinessProcessStep(step, context);
            monitor.trackStep(result);

            if (!BusinessProcessStepStatus.COMPLETED.equals(result.getStatus())) {
                FailureType failureType = resolveFailureType(step);
                FailureAction action = resolveFailureAction(step, failureType);
                log.error("Step {} failed, failureType={}, action={}, executionId={}",
                        step.getStepName(), failureType, action, context.getExecutionId());

                if (FailureAction.STOP.equals(action)) {
                    return buildFailedExecution(context.getExecutionId(), result.getErrorMessage(), context);
                }

                if (FailureAction.CONTINUE.equals(action)) {
                    continue;
                }

                BusinessProcessExecution compensated = runCompensation(completedSteps, context, result.getErrorMessage());
                if (compensated != null && BusinessProcessStatus.FAILED_COMPENSATION_FAILED.equals(compensated.getStatus())) {
                    return compensated;
                }

                if (FailureAction.COMPENSATE_AND_CONTINUE.equals(action)) {
                    continue;
                }

                // default COMPENSATE_AND_STOP
                return compensated != null ? compensated : buildFailedExecution(context.getExecutionId(), result.getErrorMessage(), context);
            }
            completedSteps.add(step);
        }

        return BusinessProcessExecution.builder()
                .processId(context.getExecutionId())
                .status(BusinessProcessStatus.COMPLETED)
                .context(context)
                .build();
    }

    // Step execution

    private BusinessProcessStepResult executeBusinessProcessStep(BusinessProcessStep step, BusinessProcessContext context) {
        step.markStarted();
        step.setProcessId(context.getExecutionId());
        log.info("Executing businessProcess step: {} with {} tasks, executionId={}",
                step.getStepId(), step.getTasks().size(), context.getExecutionId());
        StringBuilder errorBuilder = new StringBuilder();

        try {
            List<Task> tasks = step.getTasks();
            for (int i = 0; i < tasks.size(); i++) {
                executeTask(tasks.get(i), i, step, context, errorBuilder);
            }

            if (!errorBuilder.isEmpty()) {
                step.complete();
                return buildStepFailureResult(step, errorBuilder.toString(), context);
            }

            step.complete();
            log.info("Successfully completed step: {}, executionId={}", step.getStepId(), context.getExecutionId());
            return buildStepSuccessResult(step, context);

        } catch (Exception e) {
            String errorMessage = String.format("Unexpected error in step %s: %s", step.getStepId(), e.getMessage());
            log.error(errorMessage, e);
            step.complete();
            return buildStepFailureResult(step, errorMessage, context);
        }
    }

    // Task execution

    private void executeTask(Task task, int taskIndex, BusinessProcessStep step, BusinessProcessContext context, StringBuilder errorBuilder) {
        task.setProcessId(context.getExecutionId());
        task.setStepId(step.getStepId());
        log.info("Executing task: {} of type: {}, executionId={}",
                task.getTaskId(), task.getTaskType(), context.getExecutionId());
        try {
            injectInput(task, taskIndex, step, context);
            resolveBusinessProcessServiceEndpoint(task, step);
            task.markStarted();
            TaskDelegate delegate = resolveDelegate(task, step);
            Task response = retryPolicy.executeWithRetry(delegate, task);
            task.setResponsePayload(response.getResponsePayload());
            captureOutputs(step, response, context);
            task.markCompleted();
        } catch (Exception e) {
            handleTaskError(task, e, errorBuilder);
        }
    }

    private TaskDelegate resolveDelegate(Task task, BusinessProcessStep step) {
        TaskDelegate delegate = step.getProvider();
        if (delegate == null) {
            throw new IllegalStateException(
                    String.format("No provider resolved for step '%s'. Ensure provider '%s' is registered.",
                            step.getStepId(), step.getProviderName()));
        }
        return delegate;
    }

    // Failure handling

    private FailureAction resolveFailureAction(BusinessProcessStep step, FailureType failureType) {
        if (step.getFailurePolicy() == null || step.getFailurePolicy().getAction() == null) {
            return FailureAction.COMPENSATE_AND_STOP;
        }
        FailurePolicy policy = step.getFailurePolicy();
        FailureAction typedAction = switch (failureType) {
            case TECHNICAL_FAILURE -> policy.getTechnicalFailureAction();
            case AUTH_FAILURE -> policy.getAuthFailureAction();
            case BUSINESS_FAILURE -> policy.getBusinessFailureAction();
            case CONFLICT -> policy.getConflictAction();
            case UNKNOWN -> null;
        };
        return typedAction != null ? typedAction : policy.getAction();
    }

    private FailureType resolveFailureType(BusinessProcessStep step) {
        if (step.getTasks() == null) {
            return FailureType.UNKNOWN;
        }
        return step.getTasks().stream()
                .map(Task::getFailureType)
                .filter(type -> type != null)
                .findFirst()
                .orElse(FailureType.UNKNOWN);
    }

    private void handleTaskError(Task task, Exception e, StringBuilder errorBuilder) {
        FailureType failureType = classifyFailure(e);
        String errorMessage = formatTaskError(task, failureType);
        task.markFailed(e.getMessage());
        task.setError(errorMessage);
        task.setFailureType(failureType);

        if (!errorBuilder.isEmpty()) {
            errorBuilder.append("\n");
        }
        errorBuilder.append(errorMessage);

        log.error(errorMessage, e);
    }

    static String formatTaskError(Task task, FailureType failureType) {
        String taskName = task.getTaskName() != null && !task.getTaskName().isBlank() ? task.getTaskName() : task.getTaskId();
        return switch (failureType) {
            case AUTH_FAILURE -> String.format(
                    "AUTH_ERROR at task %s: Authentication or authorization failed",
                    taskName
            );
            case BUSINESS_FAILURE -> String.format(
                    "BUSINESS_ERROR at task %s: Business validation failed",
                    taskName
            );
            case CONFLICT -> String.format(
                    "BUSINESS_ERROR at task %s: Business conflict occurred",
                    taskName
            );
            case TECHNICAL_FAILURE -> String.format(
                    "TECHNICAL_ERROR at task %s: Technical failure occurred",
                    taskName
            );
            case UNKNOWN -> String.format(
                    "UNKNOWN_ERROR at task %s: Unknown failure occurred",
                    taskName
            );
        };
    }

    //classify error based on response code and Response message
    private FailureType classifyFailure(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof HttpStatusCodeException httpException) {
                return FailureType.fromHttpStatus(httpException.getStatusCode().value());
            }
            if (current instanceof ResourceAccessException) {
                return FailureType.TECHNICAL_FAILURE;
            }
            current = current.getCause();
        }
        return FailureType.UNKNOWN;
    }

    // Compensation

    private BusinessProcessExecution runCompensation(
            List<BusinessProcessStep> completedSteps,
            BusinessProcessContext context,
            String originalError
    ) {
        List<BusinessProcessStep> compensationSteps = new ArrayList<>();

        for (int step = completedSteps.size() - 1; step >= 0; step--) {
            BusinessProcessStep comp = completedSteps.get(step).getCompensation();
            if (comp != null) {
                comp.setProcessId(context.getExecutionId());
                compensationSteps.add(comp);
            }
        }
        if (!compensationSteps.isEmpty()) {
            String compensationError = executeCompensation(compensationSteps, context);
            boolean compensationFailed = compensationError != null && !compensationError.isBlank();
            return BusinessProcessExecution.builder()
                    .processId(context.getExecutionId())
                    .status(compensationFailed
                            ? BusinessProcessStatus.FAILED_COMPENSATION_FAILED
                            : BusinessProcessStatus.FAILED_COMPENSATED)
                    .error(originalError)
                    .compensationStatus(compensationFailed ? "FAILED" : "COMPLETED")
                    .compensationError(compensationError)
                    .context(context)
                    .build();
        }
        return null;
    }

    private String executeCompensation(List<BusinessProcessStep> compensationSteps, BusinessProcessContext context) {
        log.info("Starting compensation execution for {} steps", compensationSteps.size());
        StringBuilder compensationErrors = new StringBuilder();
        for (BusinessProcessStep step : compensationSteps) {
            StringBuilder errorBuilder = new StringBuilder();
            step.setProcessId(context.getExecutionId());
            for (int i = 0; i < step.getTasks().size(); i++) {
                executeTask(step.getTasks().get(i), i, step, context, errorBuilder);
            }
            step.complete();
            if (!errorBuilder.isEmpty()) {
                log.error("Compensation step '{}' failed: {}", step.getStepId(), errorBuilder);
                if (!compensationErrors.isEmpty()) {
                    compensationErrors.append("\n");
                }
                compensationErrors.append(errorBuilder);
                break;
            }
        }
        return !compensationErrors.isEmpty() ? compensationErrors.toString() : null;
    }

    // Payload mapping

    private void resolveBusinessProcessServiceEndpoint(Task task, BusinessProcessStep step) {
        BusinessProcessServiceEndpoint endpoint = serviceDiscoveryClient.resolve(step.getProviderName());
        Map<String, Object> config = task.getTaskConfig() != null ? new HashMap<>(task.getTaskConfig()) : new HashMap<>();
        config.put("resolvedBaseUrl", endpoint.getUrl());
        task.setTaskConfig(config);
        log.debug("Resolved provider '{}' for task '{}' to '{}'", step.getProviderName(), task.getTaskId(), endpoint.getUrl());
    }

    private void injectInput(Task task, int taskIndex, BusinessProcessStep step, BusinessProcessContext context) {
        if (task.getRequestPayload() != null) {
            Object resolved = payloadPathResolver.resolveValue(task.getRequestPayload().getPayload(), context);
            task.getRequestPayload().setPayload(resolved);
            log.info("Resolved mapped request payload for task '{}'", task.getTaskId());
            return;
        }

        if (taskIndex > 0) {
            Task previousTask = step.getTasks().get(taskIndex - 1);
            if (context.hasTaskOutput(previousTask.getTaskId())) {
                task.setRequestPayload(context.getTaskOutput(previousTask.getTaskId()));
                log.info("Injected task '{}' output as input for task '{}'", previousTask.getTaskId(), task.getTaskId());
                return;
            }
        }

        int stepNumber = step.getStepNumber() != null ? step.getStepNumber() : 0;
        if (stepNumber > 1) {
            String previousStepId = context.getBusinessProcess().getSteps().get(stepNumber - 2).getStepId();
            if (context.hasStepOutput(previousStepId)) {
                task.setRequestPayload(context.getStepOutput(previousStepId));
                log.info("Injected step '{}' output as input for task '{}'", previousStepId, task.getTaskId());
            }
        } else if (context.getInitialPayload() != null && task.getRequestPayload() == null) {
            task.setRequestPayload(context.getInitialPayload());
            log.info("Injected initial payload as input for task '{}'", task.getTaskId());
        }
    }

    private void captureOutputs(BusinessProcessStep step, Task response, BusinessProcessContext context) {
        if (response.getResponsePayload() != null) {
            context.addTaskOutput(response.getTaskId(), response.getResponsePayload());
            context.addStepOutput(step.getStepId(), response.getResponsePayload());
            log.info("Captured output for task '{}' and step '{}'", response.getTaskId(), step.getStepId());
            log.info("Output payload: {}", response.getResponsePayload().getPayload());
        }
    }

    // Result builders

    private BusinessProcessStepResult buildStepSuccessResult(BusinessProcessStep step, BusinessProcessContext context) {
        return BusinessProcessStepResult.builder()
                .processId(context.getExecutionId())
                .stepId(step.getStepId())
                .stepName(step.getStepName())
                .status(BusinessProcessStepStatus.COMPLETED)
                .businessProcessStepResultAudit(step.getBusinessProcessStepAudit())
                .build();
    }

    private BusinessProcessStepResult buildStepFailureResult(BusinessProcessStep step, String errorMessage, BusinessProcessContext context) {
        return BusinessProcessStepResult.builder()
                .processId(context.getExecutionId())
                .stepId(step.getStepId())
                .stepName(step.getStepName())
                .status(BusinessProcessStepStatus.FAILED)
                .errorMessage(errorMessage)
                .businessProcessStepResultAudit(step.getBusinessProcessStepAudit())
                .build();
    }

    private BusinessProcessExecution buildFailedExecution(String processId, String error, BusinessProcessContext context) {
        return BusinessProcessExecution.builder()
                .processId(processId)
                .status(BusinessProcessStatus.FAILED)
                .error(error)
                .context(context)
                .build();
    }

    private String resolveBusinessProcessIdentity(BusinessProcessDefinition businessProcess) {
        if (businessProcess.getProcessId() != null && !businessProcess.getProcessId().isBlank()) {
            return businessProcess.getProcessId();
        }
        return businessProcess.getBusinessProcessName();
    }
}
