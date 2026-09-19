package com.businessprocess.businessprocess.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "business_process_audit_event")
public class BusinessProcessAuditEventEntity {
    @Id
    @Column(name = "event_id", length = 80)
    private String eventId;
    @Column(name = "correlation_id", nullable = false, length = 160)
    private String correlationId;
    @Column(name = "execution_id", nullable = false, length = 80)
    private String executionId;
    @Column(name = "entity_type", nullable = false, length = 40)
    private String entityType;
    @Column(name = "entity_id", nullable = false, length = 160)
    private String entityId;
    @Column(name = "event_type", nullable = false, length = 80)
    private String eventType;
    @Column(name = "event_time", nullable = false)
    private LocalDateTime eventTime;
    @Column(name = "event_sequence", nullable = false)
    private Long eventSequence;
    @Column(name = "previous_status", length = 40)
    private String previousStatus;
    @Column(name = "current_status", length = 40)
    private String currentStatus;
    @Column(name = "message", length = 2000)
    private String message;
    @Lob @Column(name = "metadata")
    private String metadata;
    @Column(name = "trace_id", length = 100)
    private String traceId;
    @Column(name = "span_id", length = 100)
    private String spanId;
    @Column(name = "actor_type", length = 40)
    private String actorType;
    @Column(name = "actor_id", length = 160)
    private String actorId;
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
