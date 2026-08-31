package com.businessprocess.engine.config;

import tools.jackson.databind.ObjectMapper;
import com.businessprocess.core.registry.ProviderRegistry;
import com.businessprocess.engine.delegate.GenericHttpTaskDelegate;
import com.businessprocess.engine.parser.PayloadPathResolver;
import com.businessprocess.engine.parser.BusinessProcessParser;
import com.businessprocess.engine.provider.BusinessProcessProviderResolver;
import com.businessprocess.engine.template.preprocess.BusinessProcessTemplatePreprocessor;
import com.businessprocess.engine.validator.BusinessProcessValidator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BusinessProcessConfig {
    @Bean
    public BusinessProcessParser configBusinessProcessParser(
            ProviderRegistry providerRegistry,
            GenericHttpTaskDelegate genericHttpTaskDelegate,
            BusinessProcessTemplatePreprocessor businessProcessTemplatePreprocessor,
            BusinessProcessProviderResolver businessProcessProviderResolver,
            BusinessProcessValidator businessProcessValidator
    ) {
        return new BusinessProcessParser(providerRegistry, genericHttpTaskDelegate, businessProcessTemplatePreprocessor, businessProcessProviderResolver, businessProcessValidator);
    }

    @Bean
    public PayloadPathResolver payloadPathResolver(ObjectMapper objectMapper) {
        return new PayloadPathResolver(objectMapper);
    }
}
