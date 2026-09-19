package com.businessprocess.businessprocess.persistence.service;

import com.businessprocess.businessprocess.persistence.model.BusinessProcessExecutionDetails;
import com.businessprocess.businessprocess.persistence.model.BusinessProcessStepDetails;
import com.businessprocess.businessprocess.persistence.model.BusinessProcessTaskDetails;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import com.businessprocess.core.model.audit.Audit;
import com.businessprocess.core.model.base.BasePayload;
import com.businessprocess.core.model.task.Task;
import com.businessprocess.core.model.task.TaskStatus;
import com.businessprocess.core.model.businessprocess.BusinessProcessStep;
import com.businessprocess.core.model.businessprocess.BusinessProcessStepStatus;
import com.businessprocess.core.policy.FailureType;
import com.businessprocess.engine.engine.BusinessProcessExecution;
import com.businessprocess.engine.model.BusinessProcessDefinition;
import com.businessprocess.businessprocess.persistence.entity.BusinessProcessDefinitionEntity;
import com.businessprocess.businessprocess.persistence.entity.BusinessProcessExecutionEntity;
import com.businessprocess.businessprocess.persistence.entity.BusinessProcessStepEntity;
import com.businessprocess.businessprocess.persistence.entity.BusinessProcessTaskEntity;
import com.businessprocess.businessprocess.persistence.repository.BusinessProcessDefinitionRepository;
import com.businessprocess.businessprocess.persistence.repository.BusinessProcessExecutionRepository;
import com.businessprocess.businessprocess.persistence.repository.BusinessProcessStepRepository;
import com.businessprocess.businessprocess.persistence.repository.BusinessProcessTaskRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
public class BusinessProcessPersistenceService {
    private final BusinessProcessDefinitionRepository businessProcessDefinitionRepository;
    private final BusinessProcessExecutionRepository businessProcessExecutionRepository;
    private final BusinessProcessStepRepository businessProcessStepRepository;
    private final BusinessProcessTaskRepository businessProcessTaskRepository;
    private final ObjectMapper objectMapper;

    public BusinessProcessPersistenceService(
            BusinessProcessDefinitionRepository businessProcessDefinitionRepository,
            BusinessProcessExecutionRepository businessProcessExecutionRepository,
            BusinessProcessStepRepository businessProcessStepRepository,
            BusinessProcessTaskRepository businessProcessTaskRepository,
            ObjectMapper objectMapper
    ) {
        this.businessProcessDefinitionRepository = businessProcessDefinitionRepository;
        this.businessProcessExecutionRepository = businessProcessExecutionRepository;
        this.businessProcessStepRepository = businessProcessStepRepository;
        this.businessProcessTaskRepository = businessProcessTaskRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public BusinessProcessDefinitionEntity saveBusinessProcessDefinition(
            BusinessProcessDefinition businessProcess,
            String processKey,
            String businessProcessPath
    ) {
        String version = businessProcess.getVersion() != null ? businessProcess.getVersion() : "1";
        Optional<BusinessProcessDefinitionEntity> existing =
                businessProcessDefinitionRepository.findByBusinessProcessNameAndVersion(processKey, version);

        if (existing.isPresent()) {
            return existing.get();
        }

        BusinessProcessDefinitionEntity entity = new BusinessProcessDefinitionEntity();
        entity.setBusinessProcessName(processKey);
        entity.setVersion(version);
        entity.setBusinessProcessTemplate(readBusinessProcessSource(businessProcessPath));
        return saveBusinessProcessDefinition(entity, processKey, version);
    }

    @Transactional
    public BusinessProcessDefinitionEntity saveBusinessProcessTemplate(
            BusinessProcessDefinition businessProcess,
            String businessProcessName,
            String businessProcessTemplate
    ) {
        String version = businessProcess.getVersion() != null ? businessProcess.getVersion() : "1";
        Optional<BusinessProcessDefinitionEntity> existing =
                businessProcessDefinitionRepository.findByBusinessProcessNameAndVersion(businessProcessName, version);

        if (existing.isPresent()) {
            BusinessProcessDefinitionEntity entity = existing.get();
            entity.setBusinessProcessTemplate(businessProcessTemplate);
            entity.setUpdatedAt(LocalDateTime.now());
            return businessProcessDefinitionRepository.save(entity);
        }

        BusinessProcessDefinitionEntity entity = new BusinessProcessDefinitionEntity();
        entity.setBusinessProcessName(businessProcessName);
        entity.setVersion(version);
        entity.setBusinessProcessTemplate(businessProcessTemplate);
        return saveBusinessProcessDefinition(entity, businessProcessName, version);
    }

    private BusinessProcessDefinitionEntity saveBusinessProcessDefinition(
            BusinessProcessDefinitionEntity entity,
            String businessProcessName,
            String version
    ) {
        BusinessProcessDefinitionEntity saved = businessProcessDefinitionRepository.save(entity);
        log.info("Persisted businessProcess definition: processName={}, version={}", businessProcessName, version);
        return saved;
    }

    @Transactional(readOnly = true)
    public BusinessProcessDefinitionEntity getBusinessProcessDefinition(String businessProcessName) {
        return businessProcessDefinitionRepository.findByBusinessProcessName(businessProcessName)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Business process template not found for process name: " + businessProcessName));
    }

    @Transactional
    public BusinessProcessExecutionEntity saveBusinessProcessExecution(
            BusinessProcessExecution execution,
            BusinessProcessDefinitionEntity businessProcessDefinition,
            String processKey,
            String correlationId
    ) {
        BusinessProcessDefinitionEntity managedBusinessProcessDefinition = businessProcessDefinitionRepository
                .findById(businessProcessDefinition.getProcessId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Business process not found: " + businessProcessDefinition.getProcessId()));

        BusinessProcessExecutionEntity entity = new BusinessProcessExecutionEntity();
        entity.setExecutionId(execution.getProcessId());
        entity.setBusinessProcess(managedBusinessProcessDefinition);
        entity.setBusinessProcessName(processKey);
        entity.setCorrelationId(correlationId != null ? correlationId : execution.getProcessId());
        entity.setStatus(execution.getStatus().name());
        entity.setFailureType(resolveExecutionFailureType(execution));
        entity.setErrorMessage(execution.getError());
        entity.setCompensationStatus(execution.getCompensationStatus());
        entity.setCompensationError(execution.getCompensationError());
        entity.setInputPayload(writePayload(execution.getContext().getInitialPayload()));
        entity.setOutputPayload(writePayload(resolveBusinessProcessOutput(execution)));
        entity.setStartedAt(resolveAuditStart(execution.getBusinessProcessAudit()));
        entity.setCompletedAt(resolveAuditEnd(execution.getBusinessProcessAudit()));
        entity.setDurationMs(resolveAuditDuration(execution.getBusinessProcessAudit()));
        BusinessProcessExecutionEntity saved = businessProcessExecutionRepository.save(entity);

        for (BusinessProcessStep step : execution.getContext().getBusinessProcess().getSteps()) {
            BusinessProcessStepEntity stepEntity = new BusinessProcessStepEntity();
            stepEntity.setExecution(saved);
            stepEntity.setCorrelationId(saved.getCorrelationId());
            stepEntity.setStepId(step.getStepId());
            stepEntity.setStepNumber(resolveStepNumber(step));
            stepEntity.setStepName(step.getStepName());
            stepEntity.setStatus(resolveStepStatus(step));
            stepEntity.setInputPayload(writePayload(resolveStepInput(step, execution)));
            stepEntity.setOutputPayload(writePayload(execution.getContext().getStepOutput(step.getStepId())));
            stepEntity.setErrorMessage(resolveStepError(step));
            stepEntity.setStartedAt(resolveAuditStart(step.getBusinessProcessStepAudit()));
            stepEntity.setCompletedAt(resolveAuditEnd(step.getBusinessProcessStepAudit()));
            stepEntity.setDurationMs(resolveAuditDuration(step.getBusinessProcessStepAudit()));
            businessProcessStepRepository.save(stepEntity);

            for (Task task : step.getTasks()) {
                BusinessProcessTaskEntity taskEntity = new BusinessProcessTaskEntity();
                taskEntity.setExecution(saved);
                taskEntity.setCorrelationId(saved.getCorrelationId());
                taskEntity.setStepId(step.getStepId());
                taskEntity.setTaskId(task.getTaskId());
                taskEntity.setTaskOrder(resolveTaskOrder(task));
                taskEntity.setTaskName(task.getTaskName());
                taskEntity.setStatus(resolveTaskStatus(task));
                taskEntity.setFailureType(task.getFailureType() != null ? task.getFailureType().name() : null);
                taskEntity.setInputPayload(writePayload(task.getRequestPayload()));
                taskEntity.setOutputPayload(writePayload(task.getResponsePayload()));
                taskEntity.setErrorMessage(task.getError());
                taskEntity.setStartedAt(resolveAuditStart(task.getTaskAudit(), task.getCreatedAt()));
                taskEntity.setCompletedAt(resolveAuditEnd(task.getTaskAudit(), task.getCompletedAt()));
                taskEntity.setDurationMs(resolveAuditDuration(task.getTaskAudit()));
                businessProcessTaskRepository.save(taskEntity);
            }
        }

        log.info("Persisted businessProcess execution: executionId={}, processKey={}, status={}",
                saved.getExecutionId(), processKey, saved.getStatus());
        return saved;
    }

    @Transactional(readOnly = true)
    public BusinessProcessExecutionDetails getBusinessProcessExecutionDetails(String businessProcessName, String correlationId) {
        BusinessProcessExecutionEntity execution = businessProcessExecutionRepository
                .findFirstByBusinessProcessNameAndCorrelationIdOrderByStartedAtDescExecutionIdDesc(businessProcessName, correlationId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Business process execution not found for process name '" + businessProcessName
                                + "' and correlation id '" + correlationId + "'"));

        return toExecutionDetails(execution);
    }

    @Transactional(readOnly = true)
    public BusinessProcessExecutionDetails getBusinessProcessExecutionDetails(String correlationId) {
        BusinessProcessExecutionEntity execution = businessProcessExecutionRepository
                .findFirstByCorrelationIdOrderByStartedAtDescExecutionIdDesc(correlationId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Business process execution not found for correlation id '" + correlationId + "'"));

        return toExecutionDetails(execution);
    }

    @Transactional(readOnly = true)
    public BusinessProcessExecutionDetails getBusinessProcessExecutionDetailsByExecutionId(String executionId) {
        BusinessProcessExecutionEntity execution = businessProcessExecutionRepository
                .findById(executionId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Business process execution not found for execution id '" + executionId + "'"));

        return toExecutionDetails(execution);
    }

    @Transactional(readOnly = true)
    public Page<BusinessProcessExecutionDetails> searchFailedBusinessProcessExecutions(
            String businessProcessName,
            List<FailureType> failureTypes,
            Pageable pageable
    ) {
        Specification<BusinessProcessExecutionEntity> specification =
                (root, query, criteriaBuilder) -> root.get("status").in(
                        "FAILED", "FAILED_COMPENSATED", "FAILED_COMPENSATION_FAILED");

        if (businessProcessName != null && !businessProcessName.isBlank()) {
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.equal(root.get("businessProcessName"), businessProcessName));
        }
        if (failureTypes != null && !failureTypes.isEmpty()) {
            List<String> names = failureTypes.stream().map(Enum::name).toList();
            specification = specification.and((root, query, criteriaBuilder) -> {
                var matchingTypes = root.get("failureType").in(names);
                return failureTypes.contains(FailureType.UNKNOWN)
                        ? criteriaBuilder.or(matchingTypes, criteriaBuilder.isNull(root.get("failureType")))
                        : matchingTypes;
            });
        }

        return businessProcessExecutionRepository.findAll(specification, pageable).map(this::toExecutionDetails);
    }

    private BusinessProcessExecutionDetails toExecutionDetails(BusinessProcessExecutionEntity execution) {

        List<BusinessProcessTaskEntity> tasks = businessProcessTaskRepository
                .findByExecutionExecutionIdOrderByStepIdAscTaskOrderAsc(execution.getExecutionId());
        Map<String, List<BusinessProcessTaskEntity>> tasksByStepId = tasks.stream()
                .collect(Collectors.groupingBy(BusinessProcessTaskEntity::getStepId));

        List<BusinessProcessStepDetails> steps = businessProcessStepRepository
                .findByExecutionExecutionIdOrderByStepNumberAsc(execution.getExecutionId())
                .stream()
                .map(step -> toStepDetails(step, tasksByStepId.getOrDefault(step.getStepId(), List.of())))
                .toList();

        return BusinessProcessExecutionDetails.builder()
                .executionId(execution.getExecutionId())
                .businessProcessName(execution.getBusinessProcessName())
                .correlationId(execution.getCorrelationId())
                .status(execution.getStatus())
                .failureType(execution.getFailureType())
                .inputPayload(readPayload(execution.getInputPayload()))
                .outputPayload(readPayload(execution.getOutputPayload()))
                .errorMessage(execution.getErrorMessage())
                .compensationStatus(execution.getCompensationStatus())
                .compensationError(execution.getCompensationError())
                .startedAt(execution.getStartedAt())
                .completedAt(execution.getCompletedAt())
                .durationMs(execution.getDurationMs())
                .steps(steps)
                .build();
    }

    private BusinessProcessStepDetails toStepDetails(BusinessProcessStepEntity step, List<BusinessProcessTaskEntity> tasks) {
        return BusinessProcessStepDetails.builder()
                .stepId(step.getStepId())
                .stepNumber(step.getStepNumber())
                .stepName(step.getStepName())
                .status(step.getStatus())
                .inputPayload(readPayload(step.getInputPayload()))
                .outputPayload(readPayload(step.getOutputPayload()))
                .errorMessage(step.getErrorMessage())
                .startedAt(step.getStartedAt())
                .completedAt(step.getCompletedAt())
                .durationMs(step.getDurationMs())
                .tasks(tasks.stream()
                        .sorted(Comparator.comparing(
                                BusinessProcessTaskEntity::getTaskOrder,
                                Comparator.nullsLast(Integer::compareTo)))
                        .map(this::toTaskDetails)
                        .toList())
                .build();
    }

    private BusinessProcessTaskDetails toTaskDetails(BusinessProcessTaskEntity task) {
        return BusinessProcessTaskDetails.builder()
                .taskId(task.getTaskId())
                .taskOrder(task.getTaskOrder())
                .taskName(task.getTaskName())
                .status(task.getStatus())
                .failureType(task.getFailureType())
                .inputPayload(readPayload(task.getInputPayload()))
                .outputPayload(readPayload(task.getOutputPayload()))
                .errorMessage(task.getErrorMessage())
                .startedAt(task.getStartedAt())
                .completedAt(task.getCompletedAt())
                .durationMs(task.getDurationMs())
                .build();
    }

    private LocalDateTime resolveAuditStart(Audit audit) {
        return resolveAuditStart(audit, LocalDateTime.now());
    }

    private LocalDateTime resolveAuditStart(Audit audit, LocalDateTime fallback) {
        return audit != null && audit.getStartTime() != null ? audit.getStartTime() : fallback;
    }

    private LocalDateTime resolveAuditEnd(Audit audit) {
        return resolveAuditEnd(audit, LocalDateTime.now());
    }

    private LocalDateTime resolveAuditEnd(Audit audit, LocalDateTime fallback) {
        return audit != null && audit.getEndTime() != null ? audit.getEndTime() : fallback;
    }

    private Long resolveAuditDuration(Audit audit) {
        return audit != null ? audit.getDurationInMillis() : null;
    }

    private Integer resolveStepNumber(BusinessProcessStep step) {
        if (step.getStepNumber() != null) {
            return step.getStepNumber();
        }
        try {
            return Integer.valueOf(step.getStepId());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    "Step number is required when stepId is not numeric: " + step.getStepId(), e);
        }
    }

    private Integer resolveTaskOrder(Task task) {
        if (task.getTaskOrder() != null) {
            return task.getTaskOrder();
        }
        try {
            return Integer.valueOf(task.getTaskId());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    "Task order is required when taskId is not numeric: " + task.getTaskId(), e);
        }
    }

    private BasePayload resolveBusinessProcessOutput(BusinessProcessExecution execution) {
        if (execution.getContext().getBusinessProcess().getSteps() == null || execution.getContext().getBusinessProcess().getSteps().isEmpty()) {
            return null;
        }
        for (int i = execution.getContext().getBusinessProcess().getSteps().size() - 1; i >= 0; i--) {
            BusinessProcessStep step = execution.getContext().getBusinessProcess().getSteps().get(i);
            BasePayload output = execution.getContext().getStepOutput(step.getStepId());
            if (output != null) {
                return output;
            }
        }
        return null;
    }

    private BasePayload resolveStepInput(BusinessProcessStep step, BusinessProcessExecution execution) {
        if (step.getTasks() != null && !step.getTasks().isEmpty()) {
            return step.getTasks().get(0).getRequestPayload();
        }
        return execution.getContext().getInitialPayload();
    }

    private String resolveStepError(BusinessProcessStep step) {
        if (step.getTasks() == null) {
            return null;
        }
        return step.getTasks().stream()
                .map(Task::getError)
                .filter(error -> error != null && !error.isBlank())
                .collect(Collectors.joining("\n"));
    }

    private String resolveStepStatus(BusinessProcessStep step) {
        return step.getStatus() != null ? step.getStatus().name() : BusinessProcessStepStatus.NOT_STARTED.name();
    }

    private String resolveTaskStatus(Task task) {
        return task.getStatus() != null ? task.getStatus().name() : TaskStatus.CREATED.name();
    }

    private String resolveExecutionFailureType(BusinessProcessExecution execution) {
        if (execution.getStatus() == null
                || !execution.getStatus().name().startsWith("FAILED")
                || execution.getContext() == null
                || execution.getContext().getBusinessProcess() == null
                || execution.getContext().getBusinessProcess().getSteps() == null) {
            return null;
        }
        return execution.getContext().getBusinessProcess().getSteps().stream()
                .filter(step -> step.getTasks() != null)
                .flatMap(step -> step.getTasks().stream())
                .filter(task -> task.getStatus() == TaskStatus.FAILED)
                .map(Task::getFailureType)
                .filter(java.util.Objects::nonNull)
                .findFirst()
                .orElse(FailureType.UNKNOWN)
                .name();
    }

    private String writePayload(BasePayload payload) {
        if (payload == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(payload.getPayload());
        } catch (JacksonException e) {
            throw new IllegalArgumentException("Failed to serialize payload", e);
        }
    }

    private Object readPayload(String payload) {
        if (payload == null || payload.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(payload, new TypeReference<>() {
            });
        } catch (JacksonException e) {
            throw new IllegalArgumentException("Failed to deserialize payload", e);
        }
    }



    private String readBusinessProcessSource(String businessProcessPath) {
        if (businessProcessPath == null || businessProcessPath.isBlank()) {
            return "";
        }
        try {
            String normalized = businessProcessPath.startsWith("/") ? businessProcessPath.substring(1) : businessProcessPath;
            ClassPathResource resource = new ClassPathResource(normalized);
            if (resource.exists()) {
                return new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            }
            return Files.readString(Path.of(businessProcessPath));
        } catch (IOException e) {
            log.warn("Could not read businessProcess source for path '{}', storing path only", businessProcessPath);
            return businessProcessPath;
        }
    }
}
