package com.businessprocess.engine.template.payload.response;

import java.util.Map;

public interface ResponsePayloadMetadataExtractor {
    void extractResponsePayloadMetadata(Map<String, Object> businessProcessTemplate);
}
