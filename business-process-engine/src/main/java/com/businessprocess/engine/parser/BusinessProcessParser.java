package com.businessprocess.engine.parser;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;
import com.businessprocess.core.delegate.TaskDelegate;
import com.businessprocess.core.model.base.BasePayload;
import com.businessprocess.core.model.impl.JsonPayload;
import com.businessprocess.core.model.task.Task;
import com.businessprocess.core.model.businessprocess.BusinessProcessStep;
import com.businessprocess.core.registry.ProviderRegistry;
import com.businessprocess.engine.exceptions.BusinessProcessParsingException;
import com.businessprocess.engine.model.BusinessProcessDefinition;
import com.businessprocess.engine.provider.BusinessProcessProviderResolver;
import com.businessprocess.engine.template.preprocess.BusinessProcessTemplatePreprocessor;
import com.businessprocess.engine.validator.BusinessProcessValidator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.UUID;

//@Component
@Slf4j
public class BusinessProcessParser {
    private final Yaml yaml;
    private final ObjectMapper objectMapper;
    private final ProviderRegistry providerRegistry;
    private final TaskDelegate defaultTaskDelegate;
    private final BusinessProcessTemplatePreprocessor businessProcessTemplatePreprocessor;
    private final BusinessProcessProviderResolver businessProcessProviderResolver;
    private final BusinessProcessValidator businessProcessValidator;
    public BusinessProcessParser(ProviderRegistry providerRegistry,
                          BusinessProcessTemplatePreprocessor businessProcessTemplatePreprocessor,
                          BusinessProcessProviderResolver businessProcessProviderResolver,
                          BusinessProcessValidator businessProcessValidator) {
        this(providerRegistry, null, businessProcessTemplatePreprocessor, businessProcessProviderResolver, businessProcessValidator);
    }

    public BusinessProcessParser(ProviderRegistry providerRegistry,
                          TaskDelegate defaultTaskDelegate,
                          BusinessProcessTemplatePreprocessor businessProcessTemplatePreprocessor,
                          BusinessProcessProviderResolver businessProcessProviderResolver,
                          BusinessProcessValidator businessProcessValidator) {
        this.providerRegistry = providerRegistry;
        this.defaultTaskDelegate = defaultTaskDelegate;
        this.businessProcessTemplatePreprocessor = businessProcessTemplatePreprocessor;
        this.businessProcessProviderResolver = businessProcessProviderResolver;
        this.businessProcessValidator = businessProcessValidator;
        this.objectMapper = buildObjectMapper();
        this.yaml = new Yaml();
    }

    private static ObjectMapper buildObjectMapper() {
        SimpleModule payloadModule = new SimpleModule();
        payloadModule.addAbstractTypeMapping(BasePayload.class, JsonPayload.class);
        return JsonMapper.builder()
                .addModule(payloadModule)
                .build();
    }

    public BusinessProcessDefinition parseBusinessProcess(String businessProcessPath) {
        try (InputStream inputStream = getBusinessProcessInputStream(businessProcessPath)) {
            return prepareBusinessProcess(yaml.load(inputStream));
        } catch (IOException e) {
            throw new BusinessProcessParsingException("Failed to read businessProcess file: " + businessProcessPath, e);
        }
    }

    public BusinessProcessDefinition parseBusinessProcessTemplate(String businessProcessTemplate) {
        if (!StringUtils.hasText(businessProcessTemplate)) {
            throw new BusinessProcessParsingException("Business process template cannot be empty");
        }
        return prepareBusinessProcess(yaml.load(new StringReader(businessProcessTemplate)));
    }

    private BusinessProcessDefinition prepareBusinessProcess(Object rawBusinessProcess) {
        BusinessProcessDefinition businessProcess = toBusinessProcessDefinition(rawBusinessProcess);
        assignBusinessProcessIdentities(businessProcess);
        businessProcessValidator.validateBusinessProcess(businessProcess);
        businessProcessProviderResolver.resolveProviders(businessProcess.getSteps());
        businessProcessProviderResolver.resolveCompensationProviders(businessProcess.getSteps());
        return businessProcess;
    }

    @SuppressWarnings("unchecked")
    private BusinessProcessDefinition toBusinessProcessDefinition(Object rawBusinessProcess) {
        if (rawBusinessProcess instanceof Map<?, ?> businessProcessMap) {
            Map<String, Object> typedBusinessProcessMap = (Map<String, Object>) businessProcessMap;
            // Normalise via strategy pattern before mapping to model
            businessProcessTemplatePreprocessor.preprocess(typedBusinessProcessMap);
        }

        return objectMapper.convertValue(rawBusinessProcess, BusinessProcessDefinition.class);
    }

    private InputStream getBusinessProcessInputStream(String businessProcessPath) throws IOException {
        // Try classpath resource first (try with and without leading slash)
        String normalized = businessProcessPath.startsWith("/") ? businessProcessPath.substring(1) : businessProcessPath;

        InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(normalized);
        if (inputStream == null) {
            inputStream = getClass().getResourceAsStream('/' + normalized);
        }

        // If still not found, try the file system path
        if (inputStream == null) {
            Path p = Paths.get(businessProcessPath);
            if (Files.exists(p)) {
                return Files.newInputStream(p);
            }
            // last resort: try FileInputStream to preserve previous behaviour and original exceptions
            return new FileInputStream(businessProcessPath);
        }

        return inputStream;
    }

    private void assignBusinessProcessIdentities(BusinessProcessDefinition businessProcess) {
        if (businessProcess == null || CollectionUtils.isEmpty(businessProcess.getSteps())) {
            return;
        }

        for (int i = 0; i < businessProcess.getSteps().size(); i++) {
            BusinessProcessStep step = businessProcess.getSteps().get(i);
            assignStepIdentity(step, i + 1);
            assignTaskIdentities(step.getTasks());

            if (step.getCompensation() != null) {
                assignStepIdentity(step.getCompensation(), i + 1);
                assignTaskIdentities(step.getCompensation().getTasks());
            }
        }
    }

    //step number and step id  are assigned
    private void assignStepIdentity(BusinessProcessStep step, int stepNumber) {
        if (step == null) {
            return;
        }

        if (step.getStepNumber() == null) {
            step.setStepNumber(stepNumber);
        }

        if (!StringUtils.hasText(step.getStepId())) {
            step.setStepId(UUID.randomUUID().toString());
        }
    }

    //Task number and task id  are assigned
    private void assignTaskIdentities(List<Task> tasks) {
        if (CollectionUtils.isEmpty(tasks)) {
            return;
        }

        for (int i = 0; i < tasks.size(); i++) {
            Task task = tasks.get(i);

            if (task.getTaskOrder() == null) {
                task.setTaskOrder(i + 1);
            }

            if (!StringUtils.hasText(task.getTaskId())) {
                task.setTaskId(UUID.randomUUID().toString());
            }
        }
    }

    private Integer resolveStepNumber(BusinessProcessStep step, int defaultStepNumber) {
        if (step == null) {
            return defaultStepNumber;
        }

        String id = step.getStepId();
        return parseIntOrDefault(id, defaultStepNumber);
    }

    private Integer resolveTaskOrder(Task task, int defaultTaskOrder) {
        if (task == null) {
            return defaultTaskOrder;
        }

        return parseIntOrDefault(task.getTaskId(), defaultTaskOrder);
    }

    private int parseIntOrDefault(String value, int defaultValue) {
        if (!StringUtils.hasText(value)) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
