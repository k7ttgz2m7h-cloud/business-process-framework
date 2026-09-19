package com.businessprocess.engine.model;

import com.businessprocess.core.model.base.BasePayload;
import com.businessprocess.core.model.task.TaskStatus;

import java.util.Map;

public record RestoredStepState(
        String stepId,
        BasePayload<?> stepOutput,
        Map<String, BasePayload<?>> taskOutputs,
        Map<String, TaskStatus> taskStatuses
) {
}
