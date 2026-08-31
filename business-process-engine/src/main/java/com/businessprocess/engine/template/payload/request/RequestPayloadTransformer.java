package com.businessprocess.engine.template.payload.request;

import java.util.Map;

public interface RequestPayloadTransformer {
    void transformRequestPayloads(Map<String, Object> businessProcessTemplate);
}
