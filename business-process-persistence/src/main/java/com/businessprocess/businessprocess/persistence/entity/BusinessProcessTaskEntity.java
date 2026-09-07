package com.businessprocess.businessprocess.persistence.entity;

import lombok.Getter;
import lombok.Setter;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(
        name = "business_process_task",
        uniqueConstraints = @UniqueConstraint(name = "uk_business_process_task_execution_step_task", columnNames = {"execution_id", "step_id", "task_id"}),
        indexes = {
                @Index(name = "idx_business_process_task_correlation", columnList = "correlation_id"),
                @Index(name = "idx_business_process_task_execution", columnList = "execution_id")
        }
)
public class BusinessProcessTaskEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "business_process_task_id")
    private Long businessProcessTaskId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "execution_id", nullable = false)
    private BusinessProcessExecutionEntity execution;

    @Column(name = "execution_id", nullable = false, length = 160, insertable = false, updatable = false)
    private String executionId;

    @Column(name = "correlation_id", nullable = false, length = 160)
    private String correlationId;

    @Column(name = "step_id", nullable = false, length = 160)
    private String stepId;

    @Column(name = "task_id", nullable = false, length = 160)
    private String taskId;

    @Column(name = "task_order")
    private Integer taskOrder;

    @Column(name = "task_name", length = 200)
    private String taskName;

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

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt = LocalDateTime.now();

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "duration_ms")
    private Long durationMs;
}
