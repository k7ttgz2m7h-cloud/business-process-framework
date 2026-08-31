package com.businessprocess.engine.validator;

import com.businessprocess.core.model.task.Task;
import com.businessprocess.core.model.businessprocess.BusinessProcessStep;
import com.businessprocess.engine.exceptions.BusinessProcessParsingException;
import com.businessprocess.engine.model.BusinessProcessDefinition;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.List;

@Slf4j
@Component
public class DefaultBusinessProcessValidator implements BusinessProcessValidator
{
    @Override
    public  void validateBusinessProcess(BusinessProcessDefinition businessProcess) {
        if (businessProcess == null) {
            throw new BusinessProcessParsingException("Business process definition cannot be null");
        }

        if (CollectionUtils.isEmpty(businessProcess.getSteps())) {
            throw new BusinessProcessParsingException("Business process must contain at least one step");
        }

        validateSteps(businessProcess.getSteps());
    }

    private void validateSteps(List<BusinessProcessStep> steps) {
        for (BusinessProcessStep step : steps) {
            if (StringUtils.isEmpty(step.getStepId())) {
                throw new BusinessProcessParsingException("Step ID is required");
            }

            if (StringUtils.isEmpty(step.getProviderName())) {
                throw new BusinessProcessParsingException("Step provider is required");
            }

            if (CollectionUtils.isEmpty(step.getTasks())) {
                throw new BusinessProcessParsingException(
                        String.format("Step %s must contain at least one task", step.getStepId())
                );
            }

            validateTasks(step.getTasks(), step.getStepId());
        }
    }

    private void validateTasks(List<Task> tasks, String stepId) {
        for (Task task : tasks) {
            if (StringUtils.isEmpty(task.getTaskId())) {
                throw new BusinessProcessParsingException(
                        String.format("Task ID is required in step %s", stepId)
                );
            }

            if (task.getTaskType() == null) {
                throw new BusinessProcessParsingException(
                        String.format("Task type is required for task %s in step %s",
                                task.getTaskId(), stepId)
                );
            }
        }
    }
}
