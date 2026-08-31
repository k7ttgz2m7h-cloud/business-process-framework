package com.businessprocess.engine.template.compensation;

import java.util.Map;

public interface CompensationTemplateProcessor {
    void processCompensation(Map<String, Object> businessProcessTemplate);
}
