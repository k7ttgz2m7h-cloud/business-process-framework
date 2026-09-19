package com.businessprocess.businessprocess.persistence.service;

import com.businessprocess.businessprocess.persistence.entity.BusinessProcessExecutionEntity;
import com.businessprocess.businessprocess.persistence.repository.BusinessProcessExecutionRepository;
import com.businessprocess.engine.engine.BusinessProcessExecution;
import com.businessprocess.businessprocess.persistence.entity.BusinessProcessDefinitionEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BusinessProcessExecutionPersistence {
    private final BusinessProcessExecutionRepository repository;
    private final BusinessProcessPersistenceService facade;

    public BusinessProcessExecutionPersistence(BusinessProcessExecutionRepository repository, BusinessProcessPersistenceService facade) {
        this.repository = repository;
        this.facade = facade;
    }

    @Transactional
    public BusinessProcessExecutionEntity save(BusinessProcessExecutionEntity execution) {
        return repository.save(execution);
    }

    public BusinessProcessExecutionEntity save(BusinessProcessExecution execution, BusinessProcessDefinitionEntity definition,
                                               String processKey, String correlationId) {
        return facade.saveBusinessProcessExecution(execution, definition, processKey, correlationId);
    }
}
