package com.businessprocess.core.util;

import com.businessprocess.core.delegate.TaskDelegate;
import com.businessprocess.core.model.task.Task;
import com.businessprocess.core.model.businessprocess.BusinessProcessStep;
import com.businessprocess.core.model.task.TaskStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CompensationExecutor {
    private static final Logger logger = LoggerFactory.getLogger(CompensationExecutor.class);

    // Receives already-defined compensation steps from YAML (in reverse order) and executes them
    public void executeCompensation(List<BusinessProcessStep> compensationSteps) {
        logger.info("Starting compensation execution for {} steps", compensationSteps.size());

        for (BusinessProcessStep step : compensationSteps) {
            try {
                executeCompensationStep(step);
            } catch (Exception e) {
                logger.error("Failed to execute compensation step {}", step.getStepId(), e);
                step.setStatus(TaskStatus.FAILED);
            }
        }
    }

    private void executeCompensationStep(BusinessProcessStep step) {
        logger.info("Executing compensation step: {}", step.getStepId());

        TaskDelegate delegate = step.getProvider();
        if (delegate == null) {
            throw new IllegalStateException("No compensation handler found for step: " + step.getStepId());
        }

        try {
            for (Task compensationTask : step.getTasks()) {
                delegate.execute(compensationTask);  // taskType already set per original task
                compensationTask.setStatus(TaskStatus.COMPENSATED);
                logger.info("Compensated task '{}' of type '{}'", compensationTask.getTaskId(), compensationTask.getTaskType());
            }
            step.setStatus(TaskStatus.COMPENSATED);
            logger.info("Compensation step '{}' completed", step.getStepId());

        } catch (Exception e) {
            logger.error("Compensation execution failed for step: {}", step.getStepId(), e);
            step.setStatus(TaskStatus.FAILED);
            throw new RuntimeException("Compensation execution failed", e);
        }
    }
}
