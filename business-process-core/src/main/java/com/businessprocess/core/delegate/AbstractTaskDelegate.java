package com.businessprocess.core.delegate;

import com.businessprocess.core.model.base.BasePayload;
import com.businessprocess.core.model.task.Task;
import com.businessprocess.core.model.task.TaskStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDateTime;

public abstract class AbstractTaskDelegate implements TaskDelegate {
    protected final Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    public Task execute(Task request) {
        LocalDateTime startTime = LocalDateTime.now();

        try {
            return runExecution(request, startTime);
        } catch (Exception e) {
            return handleTaskError(request, startTime, e);
        } finally {
            request.setExecutionDuration(Duration.between(startTime, LocalDateTime.now()));
        }
    }

    private Task runExecution(Task request, LocalDateTime startTime) throws Exception {
        validateContext(request);
        Object result = doExecute(request);

        updateTaskWithSuccess(request, result);
        return request;
    }

    private void updateTaskWithSuccess(Task request, Object result) {
        if (result instanceof BasePayload) {
            request.setResponsePayload((BasePayload) result);
        } else {
            logger.warn("Result is not instance of BasePayload: {}", result.getClass().getName());
        }

        request.setResult(result);
        request.setStatus(TaskStatus.COMPLETED);
    }

    private Task handleTaskError(Task request, LocalDateTime startTime, Exception e) {
        logger.error("Task execution failed for taskId: {}", request.getTaskId(), e);

        request.setResult("Error");
        request.setStatus(TaskStatus.FAILED);  // Changed from COMPLETED to FAILED for error cases
        request.setError(e.getMessage());

        return request;
    }

    private void validateContext(Task request) {
        if (request == null) {
            throw new IllegalArgumentException("Task request cannot be null");
        }

        if (StringUtils.isEmpty(request.getTaskId())) {
            throw new IllegalArgumentException("Task ID cannot be empty");
        }

        // Add any other validation rules specific to your context
    }

    protected abstract Object doExecute(Task request);

//    protected abstract void validateContext(Task request);
}
