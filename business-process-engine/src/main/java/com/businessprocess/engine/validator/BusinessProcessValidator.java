package com.businessprocess.engine.validator;

import com.businessprocess.engine.model.BusinessProcessDefinition;

public interface BusinessProcessValidator {
    void validateBusinessProcess(BusinessProcessDefinition businessProcess);
}