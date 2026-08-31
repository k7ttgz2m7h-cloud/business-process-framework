package com.businessprocess.engine.engine;

import com.businessprocess.core.model.businessprocess.BusinessProcessStepResult;
import com.businessprocess.core.model.businessprocess.BusinessProcessStepStatus;
import com.businessprocess.core.util.TimeDurationFormatterUtil;
import com.businessprocess.engine.model.BusinessProcessMetrics;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Component
public class BusinessProcessMonitor {
    private final Map<String, List<BusinessProcessStepResult>> businessProcessSteps = new ConcurrentHashMap<>();

    public void trackStep(BusinessProcessStepResult stepResult) {
        businessProcessSteps.computeIfAbsent(stepResult.getProcessId(), k -> new ArrayList<>())
                .add(stepResult);
        logStepExecution(stepResult);
    }

    public List<BusinessProcessStepResult> getBusinessProcessSteps(String processId) {
        return businessProcessSteps.getOrDefault(processId, Collections.emptyList());
    }

    public BusinessProcessMetrics getBusinessProcessMetrics(String processId) {
        List<BusinessProcessStepResult> steps = getBusinessProcessSteps(processId);

        if (steps.isEmpty()) {
            return null;
        }

        Long totalExecutionTime = calculateTotalExecutionTime(steps);
        Double averageStepExecutionTime = calculateAverageExecutionTime(steps);

        return BusinessProcessMetrics.builder()
                .processId(processId)
                .totalSteps(steps.size())
                .completedSteps(countStepsByStatus(steps, BusinessProcessStepStatus.COMPLETED))
                .failedSteps(countStepsByStatus(steps, BusinessProcessStepStatus.FAILED))
                .compensatedSteps(countStepsByStatus(steps, BusinessProcessStepStatus.COMPENSATED))
                .totalExecutionTime(totalExecutionTime)
                .formattedTotalExecutionTime(TimeDurationFormatterUtil.getFormattedDuration(totalExecutionTime))
                .averageStepExecutionTime(averageStepExecutionTime)
                .formattedAverageStepExecutionTime(TimeDurationFormatterUtil.getFormattedDuration(averageStepExecutionTime.longValue()))
                .build();

    }

    private int countStepsByStatus(List<BusinessProcessStepResult> steps, BusinessProcessStepStatus status) {
        return (int) steps.stream()
                .filter(step -> step.getStatus() == status)
                .count();
    }

    private Long calculateTotalExecutionTime(List<BusinessProcessStepResult> steps) {
        return steps.stream()
                .mapToLong(step -> step.getBusinessProcessStepResultAudit().getCurrentDuration().toMillis())
                .sum();
    }

    private Double calculateAverageExecutionTime(List<BusinessProcessStepResult> steps) {
        return steps.stream()
                .mapToLong(step -> step.getBusinessProcessStepResultAudit().getCurrentDuration().toMillis())
                .average()
                .orElse(0.0);
    }

    private void logStepExecution(BusinessProcessStepResult stepResult) {
        log.info("Business process: {}, Step: {}, Status: {}, Duration: {}",
                stepResult.getProcessId(),
                stepResult.getStepId(),
                stepResult.getStatus(),
                stepResult.getBusinessProcessStepResultAudit().getFormattedDuration());  // Using Audit's formatted duration

        if (stepResult.getStatus() == BusinessProcessStepStatus.FAILED) {
            log.error("Step failed with error: {}", stepResult.getErrorMessage());
        }
    }

    public Map<String, Object> getStepOutput(String processId, String stepId) {
        return getBusinessProcessSteps(processId).stream()
                .filter(step -> step.getStepId().equals(stepId))
                .findFirst()
                .map(BusinessProcessStepResult::getOutput)
                .orElse(Collections.emptyMap());
    }

    public List<BusinessProcessStepResult> getFailedSteps(String processId) {
        return getBusinessProcessSteps(processId).stream()
                .filter(step -> step.getStatus() == BusinessProcessStepStatus.FAILED)
                .collect(Collectors.toList());
    }

    public List<BusinessProcessStepResult> getCompensatedSteps(String processId) {
        return getBusinessProcessSteps(processId).stream()
                .filter(step -> step.getStatus() == BusinessProcessStepStatus.COMPENSATED)
                .collect(Collectors.toList());
    }
}

