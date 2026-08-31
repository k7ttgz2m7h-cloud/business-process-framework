package com.businessprocess.engine.template.payload.response;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class DefaultResponsePayloadMetadataExtractor implements ResponsePayloadMetadataExtractor {
    @Override
    @SuppressWarnings("unchecked")
    public void extractResponsePayloadMetadata(Map<String, Object> businessProcessTemplate) {
        processStepList(businessProcessTemplate);
    }

    @SuppressWarnings("unchecked")
    private void processStepList(Map<String, Object> template) {
        Object steps = template.get("steps");
        if (!(steps instanceof List<?> stepList)) {
            return;
        }

        for (Object step : stepList) {
            if (step instanceof Map<?, ?> stepMap) {
                processTasks((Map<String, Object>) stepMap);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void processTasks(Map<String, Object> step) {
        Object tasks = step.get("tasks");
        if (!(tasks instanceof List<?> taskList)) {
            return;
        }

        Object stepNumber = step.get("stepNumber");

        for (Object task : taskList) {
            if (!(task instanceof Map<?, ?> taskMap)) {
                continue;
            }
            Map<String, Object> typedTask = (Map<String, Object>) taskMap;
            Object responsePayload = typedTask.get("responsePayload");

            if (responsePayload instanceof Map<?, ?> responseMap) {
                Map<String, Object> typedResponse = new HashMap<>((Map<String, Object>) responseMap);
                Object taskOrder = typedTask.get("taskOrder");
                typedResponse.putIfAbsent("saveAs", defaultResponseSaveAs(stepNumber, taskOrder));
                typedResponse.putIfAbsent("store", "FULL");
                typedTask.put("responsePayload", typedResponse);
            }
        }
    }

    private String defaultResponseSaveAs(Object stepNumber, Object taskOrder) {
        return "step-" + stepNumber + "_task-" + taskOrder + "_response";
    }
}
