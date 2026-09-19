package com.businessprocess.businessprocess.persistence.model;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Builder
@Data
public class BusinessProcessAuditEvent {
    private String eventId;
    private String correlationId;
    private String executionId;
    private String entityType;
    private String entityId;
    private String eventType;

    private LocalDateTime eventTime;
    private LocalDateTime createdAt;

    private Long eventSequence;

    private String previousStatus;
    private String currentStatus;
    private String message;
    private String metadata;
    private String traceId;
    private String spanId;
    private String actorType;
    private String actorId;
}
