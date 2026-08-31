package com.businessprocess.engine.provider;

import com.businessprocess.core.model.businessprocess.BusinessProcessStep;

import java.util.List;

public interface BusinessProcessProviderResolver {
    void resolveProviders(List<BusinessProcessStep> steps);
    void resolveCompensationProviders(List<BusinessProcessStep> steps);
}