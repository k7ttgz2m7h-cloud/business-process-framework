package com.businessprocess.engine.template.payload.request;

import com.businessprocess.core.model.impl.JsonPayload;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class DefaultRequestPayloadTransformer implements RequestPayloadTransformer {
    @Override
    @SuppressWarnings("unchecked")
    public void transformRequestPayloads(Map<String, Object> businessProcessTemplate) {
        transformBusinessProcessRequestPayloads(businessProcessTemplate);
    }

    @SuppressWarnings("unchecked")
    private void transformBusinessProcessRequestPayloads(Map<String, Object> businessProcessMap) {
        Object steps = businessProcessMap.get("steps");
        if (!(steps instanceof List<?> stepList)) {
            return;
        }

        for (Object step : stepList) {
            if (!(step instanceof Map<?, ?> stepMap)) {
                continue;
            }

            Map<String, Object> typedStepMap = (Map<String, Object>) stepMap;
            transformTaskRequestPayloads(typedStepMap);

            Object compensation = typedStepMap.get("compensation");
            if (compensation instanceof Map<?, ?> compensationMap) {
                transformTaskRequestPayloads((Map<String, Object>) compensationMap);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void transformTaskRequestPayloads(Map<String, Object> stepMap) {
        Object tasks = stepMap.get("tasks");
        if (!(tasks instanceof List<?> taskList)) {
            return;
        }

        for (Object task : taskList) {
            if (!(task instanceof Map<?, ?> taskMap)) {
                continue;
            }

            Map<String, Object> typedTask = (Map<String, Object>) taskMap;
            Object requestPayload = typedTask.get("requestPayload");

            if (requestPayload instanceof Map<?, ?> payloadMap) {
                Map<String, Object> typedPayload = (Map<String, Object>) payloadMap;

                if (!typedPayload.containsKey("payload") && !typedPayload.containsKey("metadata")) {
                    typedTask.put("requestPayload", new JsonPayload(typedPayload));
                }
            }
        }
    }
}
