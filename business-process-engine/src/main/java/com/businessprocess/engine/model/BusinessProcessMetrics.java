package com.businessprocess.engine.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BusinessProcessMetrics {
    private String processId;
    private int totalSteps;
    private int completedSteps;
    private int failedSteps;
    private int compensatedSteps;
    private Long totalExecutionTime;
    private String formattedTotalExecutionTime;
    private Double averageStepExecutionTime;
    private String formattedAverageStepExecutionTime;
}
