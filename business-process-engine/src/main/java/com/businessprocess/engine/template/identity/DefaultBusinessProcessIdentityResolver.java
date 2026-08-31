package com.businessprocess.engine.template.identity;

import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class DefaultBusinessProcessIdentityResolver implements BusinessProcessIdentityResolver {
    @Override
    @SuppressWarnings("unchecked")
    public void resolveIdentity(Map<String, Object> businessProcessTemplate) {
        Object trigger = businessProcessTemplate.get("trigger");

        if (!businessProcessTemplate.containsKey("businessProcessName") && trigger instanceof Map<?, ?> triggerMap) {
            Object processKey = ((Map<String, Object>) triggerMap).get("processKey");
            if (processKey != null && !processKey.toString().isBlank()) {
                businessProcessTemplate.put("businessProcessName", processKey.toString());
            }
        }

        if (!businessProcessTemplate.containsKey("businessProcessName") && businessProcessTemplate.containsKey("name")) {
            businessProcessTemplate.put("businessProcessName", businessProcessTemplate.get("name"));
        }

        if (!businessProcessTemplate.containsKey("processId") && businessProcessTemplate.containsKey("id")) {
            businessProcessTemplate.put("processId", businessProcessTemplate.get("id"));
        }

        if (!businessProcessTemplate.containsKey("processId") && businessProcessTemplate.containsKey("businessProcessName")) {
            businessProcessTemplate.put("processId", businessProcessTemplate.get("businessProcessName"));
        }

        businessProcessTemplate.remove("id");
        businessProcessTemplate.remove("name");
        businessProcessTemplate.remove("stepName");
        businessProcessTemplate.remove("trigger");
    }
}
