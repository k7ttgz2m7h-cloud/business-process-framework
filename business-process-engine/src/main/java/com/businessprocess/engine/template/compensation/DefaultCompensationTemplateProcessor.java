package com.businessprocess.engine.template.compensation;

import com.businessprocess.engine.template.payload.request.RequestPayloadTransformer;
import com.businessprocess.engine.template.payload.response.ResponsePayloadMetadataExtractor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class DefaultCompensationTemplateProcessor implements CompensationTemplateProcessor {
    private final RequestPayloadTransformer requestPayloadTransformer;
    private final ResponsePayloadMetadataExtractor responsePayloadMetadataExtractor;

    public DefaultCompensationTemplateProcessor(
            RequestPayloadTransformer requestPayloadTransformer,
            ResponsePayloadMetadataExtractor responsePayloadMetadataExtractor
    ) {
        this.requestPayloadTransformer = requestPayloadTransformer;
        this.responsePayloadMetadataExtractor = responsePayloadMetadataExtractor;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void processCompensation(Map<String, Object> businessProcessTemplate) {
        Object steps = businessProcessTemplate.get("steps");
        if (!(steps instanceof List<?> stepList)) {
            return;
        }

        for (Object step : stepList) {
            if (!(step instanceof Map<?, ?> stepMap)) {
                continue;
            }

            Object compensation = ((Map<String, Object>) stepMap).get("compensation");
            if (compensation instanceof Map<?, ?> compensationMap) {
                requestPayloadTransformer.transformRequestPayloads((Map<String, Object>) compensationMap);
                responsePayloadMetadataExtractor.extractResponsePayloadMetadata((Map<String, Object>) compensationMap);
            }
        }
    }
}
