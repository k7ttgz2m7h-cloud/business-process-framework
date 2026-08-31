package com.businessprocess.businessprocess.kafka;

import tools.jackson.databind.ObjectMapper;
import com.businessprocess.core.model.base.BasePayload;
import com.businessprocess.engine.engine.BusinessProcessExecution;
import com.businessprocess.engine.model.BusinessProcessDefinition;
import com.businessprocess.engine.service.BusinessProcessOrchestrationService;
import com.businessprocess.businessprocess.api.dto.BusinessProcessTriggerMessage;
import com.businessprocess.businessprocess.persistence.entity.BusinessProcessDefinitionEntity;
import com.businessprocess.businessprocess.persistence.service.BusinessProcessPersistenceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@Slf4j
public class BusinessProcessTriggerListener {
    private final ObjectMapper objectMapper;
    private final BusinessProcessOrchestrationService orchestrationService;
    private final BusinessProcessPersistenceService businessProcessPersistenceService;

    public BusinessProcessTriggerListener(
            ObjectMapper objectMapper,
            BusinessProcessOrchestrationService orchestrationService,
            BusinessProcessPersistenceService businessProcessPersistenceService
    ) {
        this.objectMapper = objectMapper;
        this.orchestrationService = orchestrationService;
        this.businessProcessPersistenceService = businessProcessPersistenceService;
    }

    @KafkaListener(topics = "${saga.kafka.trigger-topic}", groupId = "${spring.kafka.consumer.group-id}")
    public void onMessage(String message) throws Exception {
        BusinessProcessTriggerMessage trigger = objectMapper.readValue(message, BusinessProcessTriggerMessage.class);
        if (trigger.getBusinessProcessName() == null || trigger.getBusinessProcessName().isBlank()) {
            throw new IllegalArgumentException("businessProcessName is required");
        }
        String correlationId = UUID.randomUUID().toString();
        log.info("Received businessProcess trigger: processName={}, correlationId={}",
                trigger.getBusinessProcessName(), correlationId);

        BusinessProcessDefinitionEntity businessProcessDefinition =
                businessProcessPersistenceService.getBusinessProcessDefinition(trigger.getBusinessProcessName());
        BusinessProcessDefinition businessProcess = orchestrationService.parseBusinessProcessTemplate(businessProcessDefinition.getBusinessProcessTemplate());
        String processKey = businessProcessDefinition.getBusinessProcessName();

        BusinessProcessExecution execution = orchestrationService.executeBusinessProcessflow(
                businessProcess,
                payload(trigger.getInputPayload())
        );
        businessProcessPersistenceService.saveBusinessProcessExecution(
                execution,
                businessProcessDefinition,
                processKey,
                correlationId
        );

        log.info("Business process trigger completed: executionId={}, correlationId={}, status={}",
                execution.getProcessId(), correlationId, execution.getStatus());
    }

    private BasePayload<Object> payload(Object value) {
        BasePayload<Object> payload = new BasePayload<>() {
        };
        payload.setPayload(value);
        return payload;
    }
}
