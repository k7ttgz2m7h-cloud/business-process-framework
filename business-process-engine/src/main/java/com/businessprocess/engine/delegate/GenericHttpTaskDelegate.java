package com.businessprocess.engine.delegate;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.businessprocess.core.delegate.TaskDelegate;
import com.businessprocess.core.model.base.BasePayload;
import com.businessprocess.core.model.task.Task;
import com.businessprocess.core.model.task.TaskStatus;
import com.businessprocess.core.util.CompensationExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Component
public class GenericHttpTaskDelegate implements TaskDelegate {
    private static final Logger logger = LoggerFactory.getLogger(GenericHttpTaskDelegate.class);
    private static final String RESOLVED_BASE_URL = "resolvedBaseUrl";

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public GenericHttpTaskDelegate(ObjectMapper objectMapper) {
        this.restTemplate = new RestTemplate();
        this.objectMapper = objectMapper;
    }

    @Override
    public Task execute(Task task) {
        String url = buildUrl(task);
        HttpMethod method = HttpMethod.valueOf(configValue(task, "method", "POST"));
        Object requestBody = task.getRequestPayload() != null ? task.getRequestPayload().getPayload() : null;

        //these heders are set to accept and send JSON, you can modify them as per project requirement
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        //

        ResponseEntity<JsonNode> response = restTemplate.exchange(
                url,
                method,
                new HttpEntity<>(requestBody, headers),
                JsonNode.class
        );

        task.setResponsePayload(payload(response.getBody()));
        task.setStatus(TaskStatus.COMPLETED);
        return task;
    }

    @Override
    public Task compensateTask(Task task) {
        return execute(task);
    }

    private String buildUrl(Task task) {
        String baseUrl = configValue(task, RESOLVED_BASE_URL, null);
        logger.info("Building URL for task: {}, step: {}, compensation: {}, base URL: {}", task.getTaskOrder(), task.getStepId(), task.getIsCompensationStep(), baseUrl);
        String endpoint = configValue(task, "endpoint", null);
        if (baseUrl == null || endpoint == null) {
            throw new IllegalArgumentException("resolvedBaseUrl and endpoint are required for HTTP task: " + task.getTaskId());
        }
        String resolvedEndpoint = resolvePathVariables(endpoint, task);
        return baseUrl + (resolvedEndpoint.startsWith("/") ? resolvedEndpoint : "/" + resolvedEndpoint);
    }

    @SuppressWarnings("unchecked")
    private String resolvePathVariables(String endpoint, Task task) {
        if (task.getRequestPayload() == null || !(task.getRequestPayload().getPayload() instanceof Map<?, ?> payload)) {
            return endpoint;
        }
        String resolvedEndpoint = endpoint;
        for (Map.Entry<?, ?> entry : payload.entrySet()) {
            Object value = entry.getValue();
            if (value != null) {
                resolvedEndpoint = resolvedEndpoint.replace("{" + entry.getKey() + "}", value.toString());
            }
        }
        return resolvedEndpoint;
    }

    private String configValue(Task task, String key, String defaultValue) {
        Map<String, Object> config = task.getTaskConfig();
        Object value = config != null ? config.get(key) : null;
        return value != null ? value.toString() : defaultValue;
    }

    private BasePayload<Object> payload(Object value) {
        BasePayload<Object> payload = new BasePayload<>() {
        };
        payload.setPayload(value != null ? value : objectMapper.createObjectNode());
        return payload;
    }
}
