package com.businessprocess.core.policy;

import com.businessprocess.core.delegate.TaskDelegate;
import com.businessprocess.core.exception.TaskExecutionException;
import com.businessprocess.core.model.task.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;

@Component
public class RetryPolicy {
    private static final Logger logger = LoggerFactory.getLogger(RetryPolicy.class);

    public Task executeWithRetry(TaskDelegate delegate, Task task) throws TaskExecutionException {
        int maxRetries = task.getMaxRetries() != null ? task.getMaxRetries() : 2;
        int attempt = 0;

        while (true) {
            try {
                return delegate.execute(task);
            } catch (Exception e) {
                attempt++;
                task.setRetryCount(attempt);

                if (!isRetryable(e)) {
                    logger.warn("Task '{}' failed with non-retryable error", task.getTaskId());
                    throw new TaskExecutionException(e.getMessage(), e);
                }

                if (attempt > maxRetries) {
                    logger.error("Task '{}' failed after {} attempts", task.getTaskId(), attempt);
                    throw new TaskExecutionException(
                        String.format("Task '%s' failed after %d retries: %s",
                            task.getTaskId(), attempt, e.getMessage()), e);
                }

                logger.warn("Task '{}' failed on attempt {}/{}, retrying in 1s...",
                        task.getTaskId(), attempt, maxRetries);
                try {
                    Thread.sleep(1000L * attempt);  // incremental backoff
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new TaskExecutionException("Retry interrupted for task: " + task.getTaskId(), ie);
                }
            }
        }
    }

    private boolean isRetryable(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof ResourceAccessException) {
                return true;
            }
            if (current instanceof HttpStatusCodeException httpException) {
                return FailureType.fromHttpStatus(httpException.getStatusCode().value()) == FailureType.TECHNICAL_FAILURE;
            }
            current = current.getCause();
        }
        return false;
    }
}
