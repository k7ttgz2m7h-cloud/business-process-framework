package com.businessprocess.businessprocess.persistence.service;

import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import com.businessprocess.core.model.base.BasePayload;
import org.springframework.stereotype.Component;

@Component
public class PayloadSerializer {
    private final ObjectMapper objectMapper;

    public PayloadSerializer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String write(BasePayload<?> payload) {
        if (payload == null) return null;
        try {
            return objectMapper.writeValueAsString(payload.getPayload());
        } catch (JacksonException exception) {
            throw new IllegalArgumentException("Failed to serialize payload", exception);
        }
    }

    public Object read(String payload) {
        if (payload == null || payload.isBlank()) return null;
        try {
            return objectMapper.readValue(payload, new TypeReference<>() {});
        } catch (JacksonException exception) {
            throw new IllegalArgumentException("Failed to deserialize payload", exception);
        }
    }
}
