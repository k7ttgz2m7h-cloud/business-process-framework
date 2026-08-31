package com.businessprocess.engine.parser;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.businessprocess.core.model.base.BasePayload;
import com.businessprocess.core.model.task.Task;
import com.businessprocess.core.model.businessprocess.BusinessProcessStep;
import com.businessprocess.engine.model.BusinessProcessContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class PayloadPathResolver {
    private final ObjectMapper objectMapper;
    private static final Pattern TASK_BODY_PATTERN = Pattern.compile(
            "^\\$\\.steps\\[(\\d+)]\\.tasks\\[(\\d+)]\\.(requestBody|responseBody|requestPayload|responsePayload)(?:\\.(.+))?$"
    );
    private static final String INITIAL_PAYLOAD_PREFIX = "$.initialPayload";

    public PayloadPathResolver() {
        this(new ObjectMapper());
    }

    public PayloadPathResolver(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public JsonNode resolve(String path, BusinessProcessContext context) {
        if (path == null || path.trim().isEmpty()) {
            throw new IllegalArgumentException("Payload path is required");
        }
        if (context == null) {
            throw new IllegalArgumentException("Business process context is required");
        }

        if (path.equals(INITIAL_PAYLOAD_PREFIX)) {
            return toJsonNode(context.getInitialPayload());
        }
        if (path.startsWith(INITIAL_PAYLOAD_PREFIX + ".")) {
            return resolveChildPath(toJsonNode(context.getInitialPayload()), path.substring((INITIAL_PAYLOAD_PREFIX + ".").length()));
        }

        Matcher matcher = TASK_BODY_PATTERN.matcher(path);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Unsupported payload path: " + path);
        }

        Integer stepNumber = Integer.valueOf(matcher.group(1));
        Integer taskNumber = Integer.valueOf(matcher.group(2));
        String bodyType = matcher.group(3);
        String childPath = matcher.group(4);

        Task task = findTask(context, stepNumber, taskNumber);

        BasePayload payload = ("requestBody".equals(bodyType) || "requestPayload".equals(bodyType))
                ? task.getRequestPayload()
                : task.getResponsePayload();
        JsonNode body = toJsonNode(payload);
        if (childPath == null || childPath.isEmpty()) {
            return body;
        }
        return resolveChildPath(body, childPath);
    }

    public Object resolveValue(Object value, BusinessProcessContext context) {
        if (value instanceof String text && text.startsWith("$.")) {
            return objectMapper.convertValue(resolve(text, context), Object.class);
        }
        if (value instanceof Map<?, ?> map) {
            return map.entrySet().stream()
                    .collect(Collectors.toMap(
                            entry -> entry.getKey().toString(),
                            entry -> resolveValue(entry.getValue(), context)
                    ));
        }
        if (value instanceof Iterable<?> iterable) {
            List<Object> resolved = new ArrayList<>();
            iterable.forEach(item -> resolved.add(resolveValue(item, context)));
            return resolved;
        }
        return value;
    }

    private Task findTask(BusinessProcessContext context, Integer stepNumber, Integer taskNumber) {
        if (context.getBusinessProcess() == null || context.getBusinessProcess().getSteps() == null
                || stepNumber < 0 || stepNumber >= context.getBusinessProcess().getSteps().size()) {
            throw new IllegalArgumentException("No step found for step number: " + stepNumber);
        }

        BusinessProcessStep step = context.getBusinessProcess().getSteps().get(stepNumber);
        if (step.getTasks() == null || taskNumber < 0 || taskNumber >= step.getTasks().size()) {
            throw new IllegalArgumentException(
                    String.format("No task found for step number: %d and task number: %d", stepNumber, taskNumber));
        }
        return step.getTasks().get(taskNumber);
    }

    private JsonNode toJsonNode(BasePayload payload) {
        if (payload == null) {
            throw new IllegalArgumentException("Task payload is missing");
        }
        return objectMapper.valueToTree(payload.getPayload());
    }

    private JsonNode resolveChildPath(JsonNode root, String childPath) {
        JsonNode current = root;
        for (String segment : childPath.split("\\.")) {
            if (current == null || current.isMissingNode() || current.isNull()) {
                throw new IllegalArgumentException("Path segment not found: " + segment);
            }
            current = current.path(segment);
        }
        if (current == null || current.isMissingNode()) {
            throw new IllegalArgumentException("Payload path not found: " + childPath);
        }
        return current;
    }
}
