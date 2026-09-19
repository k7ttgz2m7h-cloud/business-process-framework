package com.businessprocess.businessprocess.persistence.entity;

import lombok.Getter;
import lombok.Setter;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(
        name = "business_process_execution",
        indexes = {
                @Index(name = "idx_business_process_execution_name", columnList = "business_process_name"),
                @Index(name = "idx_business_process_execution_correlation", columnList = "business_process_name, correlation_id"),
                @Index(name = "idx_business_process_execution_failure", columnList = "status, failure_type, completed_at")
        }
)
public class BusinessProcessExecutionEntity {
    @Id
    @Column(name = "execution_id", length = 80)
    private String executionId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "process_id", nullable = false)
    private BusinessProcessDefinitionEntity businessProcess;

    @Column(name = "business_process_name", nullable = false, length = 120)
    private String businessProcessName;

    @Column(name = "correlation_id", nullable = false, length = 160)
    private String correlationId;

    @Column(name = "status", nullable = false, length = 40)
    private String status;

    @Column(name = "failure_type", length = 40)
    private String failureType;

    @Lob
    @Column(name = "input_payload")
    private String inputPayload;

    @Lob
    @Column(name = "output_payload")
    private String outputPayload;

    @Column(name = "error_message", length = 2000)
    private String errorMessage;

    @Column(name = "compensation_status", length = 40)
    private String compensationStatus;

    @Column(name = "compensation_error", length = 2000)
    private String compensationError;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt = LocalDateTime.now();

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "duration_ms")
    private Long durationMs;
}
