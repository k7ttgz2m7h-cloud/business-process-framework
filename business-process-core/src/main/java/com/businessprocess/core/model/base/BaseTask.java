package com.businessprocess.core.model.base;

import com.businessprocess.core.model.task.TaskStatus;
import com.businessprocess.core.model.task.TaskType;
import lombok.Data;

@Data
public abstract class BaseTask {
    private String taskId;
    private String taskName;
    private TaskType taskType;
    private TaskStatus status;
    private Integer taskOrder;
}