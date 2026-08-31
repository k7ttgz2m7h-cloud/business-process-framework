package com.businessprocess.core.policy;

import lombok.Data;

@Data
public class FailurePolicy {
    private FailureAction action;
    private Boolean retryable;
    private FailureAction technicalFailureAction;
    private FailureAction authFailureAction;
    private FailureAction businessFailureAction;
    private FailureAction conflictAction;
}
