package com.businessprocess.core.policy;

public enum FailureAction {
    STOP,
    CONTINUE,
    COMPENSATE_AND_STOP,
    COMPENSATE_AND_CONTINUE
}
