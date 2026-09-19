package com.businessprocess.businessprocess.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "business_process_task_attempt")
public class BusinessProcessTaskAttemptEntity {
    @Id
    @Column(name = "attempt_id", length = 80)
    private String attemptId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "execution_id", nullable = false)
    private BusinessProcessExecutionEntity execution;

    @Column(name = "execution_id", nullable = false, length = 80, insertable = false, updatable = false)
    private String executionId;

    @Column(name = "correlation_id", nullable = false, length = 160)
    private String correlationId;
    @Column(name = "step_id", nullable = false, length = 160)
    private String stepId;
    @Column(name = "task_id", nullable = false, length = 160)
    private String taskId;
    @Column(name = "attempt_number", nullable = false)
    private Integer attemptNumber;
    @Column(name = "attempt_type", nullable = false, length = 40)
    private String attemptType;
    @Column(name = "provider_name", nullable = false, length = 160)
    private String providerName;
    @Column(name = "operation_name", length = 200)
    private String operationName;
    @Column(name = "endpoint", length = 500)
    private String endpoint;
    @Column(name = "http_method", length = 12)
    private String httpMethod;
    @Column(name = "status", nullable = false, length = 40)
    private String status;
    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt = LocalDateTime.now();
    @Column(name = "completed_at")
    private LocalDateTime completedAt;
    @Column(name = "duration_ms")
    private Long durationMs;
    @Column(name = "failure_type", length = 40)
    private String failureType;
    @Column(name = "error_code", length = 160)
    private String errorCode;
    @Column(name = "error_message", length = 2000)
    private String errorMessage;
    @Column(name = "http_status")
    private Integer httpStatus;
    @Column(name = "retryable")
    private Boolean retryable;
    @Column(name = "retry_reason", length = 500)
    private String retryReason;
    @Lob @Column(name = "request_payload")
    private String requestPayload;
    @Lob @Column(name = "response_payload")
    private String responsePayload;
    @Column(name = "idempotency_key", length = 200)
    private String retryRequestId;
    @Column(name = "trace_id", length = 100)
    private String traceId;
    @Column(name = "span_id", length = 100)
    private String spanId;
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
