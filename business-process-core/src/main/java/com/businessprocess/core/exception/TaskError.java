package com.businessprocess.core.exception;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TaskError {
    private String message;
    private String errorCode;
    private String details;

    public TaskError(String message) {
        this.message = message;
    }
}