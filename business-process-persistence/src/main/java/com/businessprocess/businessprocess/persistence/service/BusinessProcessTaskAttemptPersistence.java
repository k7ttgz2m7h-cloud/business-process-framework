package com.businessprocess.businessprocess.persistence.service;

import com.businessprocess.businessprocess.persistence.entity.BusinessProcessTaskAttemptEntity;
import com.businessprocess.businessprocess.persistence.repository.BusinessProcessTaskAttemptRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BusinessProcessTaskAttemptPersistence {
    private final BusinessProcessTaskAttemptRepository repository;

    public BusinessProcessTaskAttemptPersistence(BusinessProcessTaskAttemptRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public BusinessProcessTaskAttemptEntity save(BusinessProcessTaskAttemptEntity attempt) {
        return repository.save(attempt);
    }
}
