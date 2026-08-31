package com.businessprocess.core.model.base;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
public abstract class BasePayload<T> {
    private Map<String, Object> metadata;
    private T payload;
    private LocalDateTime timestamp = LocalDateTime.now();

    // Optional: Protected constructor
    protected BasePayload(Map<String, Object> metadata, T payload) {
        this.metadata = metadata;
        this.payload = payload;
        this.timestamp = LocalDateTime.now();
    }
}