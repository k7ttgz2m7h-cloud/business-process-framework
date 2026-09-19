package com.businessprocess.engine.model;

import java.util.Map;

public record RestoredExecutionState(
        Map<String, RestoredStepState> steps
) {
}
