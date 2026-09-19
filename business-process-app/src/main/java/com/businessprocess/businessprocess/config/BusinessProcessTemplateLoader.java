package com.businessprocess.businessprocess.config;

import com.businessprocess.engine.model.BusinessProcessDefinition;
import com.businessprocess.engine.service.BusinessProcessOrchestrationService;
import com.businessprocess.businessprocess.persistence.service.BusinessProcessDefinitionPersistence;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;

import java.nio.charset.StandardCharsets;
import java.util.Map;

@Component
@Slf4j
public class BusinessProcessTemplateLoader implements ApplicationRunner {
    private static final String BUSINESS_PROCESS_FLOW_PATTERN = "classpath*:/business-process-flows/*.yaml";

    private final BusinessProcessOrchestrationService orchestrationService;
    private final BusinessProcessDefinitionPersistence businessProcessDefinitionPersistence;

    public BusinessProcessTemplateLoader(
            BusinessProcessOrchestrationService orchestrationService,
            BusinessProcessDefinitionPersistence businessProcessDefinitionPersistence
    ) {
        this.orchestrationService = orchestrationService;
        this.businessProcessDefinitionPersistence = businessProcessDefinitionPersistence;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        Resource[] resources = new PathMatchingResourcePatternResolver().getResources(BUSINESS_PROCESS_FLOW_PATTERN);
        for (Resource resource : resources) {
            String template = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            BusinessProcessDefinition businessProcess = orchestrationService.parseBusinessProcessTemplate(template);
            String businessProcessName = resolveBusinessProcessName(template, businessProcess);
            businessProcessDefinitionPersistence.saveBusinessProcessTemplate(businessProcess, businessProcessName, template);
            log.info("Loaded businessProcess template: processName={}, resource={}", businessProcessName, resource.getFilename());
        }
    }

    @SuppressWarnings("unchecked")
    private String resolveBusinessProcessName(String template, BusinessProcessDefinition businessProcess) {
        Object raw = new Yaml().load(template);
        if (raw instanceof Map<?, ?> businessProcessMap) {
            Object trigger = ((Map<String, Object>) businessProcessMap).get("trigger");
            if (trigger instanceof Map<?, ?> triggerMap) {
                Object processKey = ((Map<String, Object>) triggerMap).get("processKey");
                if (processKey != null && !processKey.toString().isBlank()) {
                    return processKey.toString();
                }
            }

            Object id = ((Map<String, Object>) businessProcessMap).get("id");
            if (id != null && !id.toString().isBlank()) {
                return id.toString();
            }
        }
        return businessProcess.getBusinessProcessName();
    }
}
