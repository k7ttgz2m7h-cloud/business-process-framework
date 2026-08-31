package com.businessprocess.businessprocess.persistence.entity;

import lombok.Getter;
import lombok.Setter;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(
        name = "business_process",
        uniqueConstraints = @UniqueConstraint(name = "uk_business_process_version", columnNames = {"business_process_name", "version"})
)
public class BusinessProcessDefinitionEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "process_id")
    private Long processId;

    @Column(name = "business_process_name", nullable = false, length = 120)
    private String businessProcessName;

    @Column(name = "version", nullable = false, length = 40)
    private String version;

    @Lob
    @Column(name = "business_process_template", nullable = false)
    private String businessProcessTemplate;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
