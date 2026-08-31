package com.businessprocess.core.delegate;

import com.businessprocess.core.model.task.Task;

public interface TaskDelegate {


    Task execute(Task task);

    Task compensateTask(Task task); //implementation pending - should be generic
}