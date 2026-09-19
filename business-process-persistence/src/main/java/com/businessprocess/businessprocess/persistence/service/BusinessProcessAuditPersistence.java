package com.businessprocess.businessprocess.persistence.service;

import com.businessprocess.businessprocess.persistence.entity.BusinessProcessAuditEventEntity;
import com.businessprocess.businessprocess.persistence.repository.BusinessProcessAuditEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BusinessProcessAuditPersistence {
    private final BusinessProcessAuditEventRepository repository;

    public BusinessProcessAuditPersistence(BusinessProcessAuditEventRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public BusinessProcessAuditEventEntity append(BusinessProcessAuditEventEntity event) {
        return repository.save(event);
    }
}
