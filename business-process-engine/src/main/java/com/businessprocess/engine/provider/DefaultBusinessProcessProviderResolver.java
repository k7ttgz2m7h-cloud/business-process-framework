package com.businessprocess.engine.provider;

import com.businessprocess.core.delegate.TaskDelegate;
import com.businessprocess.core.model.businessprocess.BusinessProcessStep;
import com.businessprocess.core.registry.ProviderRegistry;
import com.businessprocess.engine.exceptions.BusinessProcessParsingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class DefaultBusinessProcessProviderResolver implements BusinessProcessProviderResolver{
    private final ProviderRegistry providerRegistry;
    private final TaskDelegate defaultTaskDelegate;

    public DefaultBusinessProcessProviderResolver(ProviderRegistry providerRegistry, TaskDelegate defaultTaskDelegate) {
        this.providerRegistry = providerRegistry;
        this.defaultTaskDelegate = defaultTaskDelegate;
    }

    @Override
    public void resolveProviders(List<BusinessProcessStep> steps) {
        for (BusinessProcessStep step : steps) {
            String providerName = step.getProviderName();
            if (!providerRegistry.hasProvider(providerName)) {
                registerDefaultProvider(providerName);
            }
            step.setProvider(providerRegistry.getProvider(providerName));
            log.info("Resolved provider '{}' for step '{}'", providerName, step.getStepId());
        }
    }

    // Resolves providers on compensation steps so they can be executed by CompensationExecutor
    @Override
    public void resolveCompensationProviders(List<BusinessProcessStep> steps) {
        for (BusinessProcessStep step : steps) {
            BusinessProcessStep comp = step.getCompensation();
            if (comp != null && comp.getProviderName() != null) {
                if (!providerRegistry.hasProvider(comp.getProviderName())) {
                    registerDefaultProvider(comp.getProviderName());
                }
                comp.setProvider(providerRegistry.getProvider(comp.getProviderName()));
                log.info("Resolved compensation provider '{}' for step '{}'", comp.getProviderName(), step.getStepId());
            }
        }
    }

    private void registerDefaultProvider(String providerName) {
        if (defaultTaskDelegate == null) {
            throw new BusinessProcessParsingException("No provider registered for: " + providerName);
        }
        providerRegistry.register(providerName, defaultTaskDelegate);
        log.info("Registered default provider '{}' using generic delegate", providerName);
    }


}
