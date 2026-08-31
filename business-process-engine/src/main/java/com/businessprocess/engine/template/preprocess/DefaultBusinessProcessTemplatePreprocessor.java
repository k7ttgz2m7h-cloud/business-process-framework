package com.businessprocess.engine.template.preprocess;

import com.businessprocess.engine.template.compensation.CompensationTemplateProcessor;
import com.businessprocess.engine.template.identity.BusinessProcessIdentityResolver;
import com.businessprocess.engine.template.payload.request.RequestPayloadTransformer;
import com.businessprocess.engine.template.payload.response.ResponsePayloadMetadataExtractor;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class DefaultBusinessProcessTemplatePreprocessor implements BusinessProcessTemplatePreprocessor {
    private final BusinessProcessIdentityResolver businessProcessIdentityResolver;
    private final RequestPayloadTransformer requestPayloadTransformer;
    private final ResponsePayloadMetadataExtractor responsePayloadMetadataExtractor;
    private final CompensationTemplateProcessor compensationTemplateProcessor;

    public DefaultBusinessProcessTemplatePreprocessor(
            BusinessProcessIdentityResolver businessProcessIdentityResolver,
            RequestPayloadTransformer requestPayloadTransformer,
            ResponsePayloadMetadataExtractor responsePayloadMetadataExtractor,
            CompensationTemplateProcessor compensationTemplateProcessor
    ) {
        this.businessProcessIdentityResolver = businessProcessIdentityResolver;
        this.requestPayloadTransformer = requestPayloadTransformer;
        this.responsePayloadMetadataExtractor = responsePayloadMetadataExtractor;
        this.compensationTemplateProcessor = compensationTemplateProcessor;
    }

    @Override
    public void preprocess(Map<String, Object> businessProcessTemplate) {
        businessProcessIdentityResolver.resolveIdentity(businessProcessTemplate);
        requestPayloadTransformer.transformRequestPayloads(businessProcessTemplate);
        responsePayloadMetadataExtractor.extractResponsePayloadMetadata(businessProcessTemplate);
        compensationTemplateProcessor.processCompensation(businessProcessTemplate);
    }
}
