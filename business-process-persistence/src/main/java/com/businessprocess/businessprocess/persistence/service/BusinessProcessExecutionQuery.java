package com.businessprocess.businessprocess.persistence.service;

import com.businessprocess.businessprocess.persistence.entity.BusinessProcessExecutionEntity;
import com.businessprocess.businessprocess.persistence.repository.BusinessProcessExecutionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.businessprocess.businessprocess.persistence.model.BusinessProcessExecutionDetails;
import com.businessprocess.core.policy.FailureType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;

@Service
public class BusinessProcessExecutionQuery {
    private final BusinessProcessExecutionRepository repository;
    private final BusinessProcessPersistenceService facade;

    public BusinessProcessExecutionQuery(BusinessProcessExecutionRepository repository, BusinessProcessPersistenceService facade) {
        this.repository = repository;
        this.facade = facade;
    }

    @Transactional(readOnly = true)
    public BusinessProcessExecutionEntity findByCorrelationId(String correlationId) {
        return repository.findFirstByCorrelationIdOrderByStartedAtDescExecutionIdDesc(correlationId).orElseThrow(() ->
                new IllegalArgumentException("Business process execution not found for correlation id '" + correlationId + "'"));
    }

    public BusinessProcessExecutionDetails getDetails(String processName, String correlationId) {
        return facade.getBusinessProcessExecutionDetails(processName, correlationId);
    }

    public BusinessProcessExecutionDetails getDetails(String correlationId) {
        return facade.getBusinessProcessExecutionDetails(correlationId);
    }

    public BusinessProcessExecutionDetails getDetailsByExecutionId(String executionId) {
        return facade.getBusinessProcessExecutionDetailsByExecutionId(executionId);
    }

    public Page<BusinessProcessExecutionDetails> searchFailed(String processName, List<FailureType> failureTypes, Pageable pageable) {
        return facade.searchFailedBusinessProcessExecutions(processName, failureTypes, pageable);
    }
}
