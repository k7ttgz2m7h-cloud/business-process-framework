package com.businessprocess.businessprocess.persistence.repository;

import com.businessprocess.businessprocess.persistence.entity.BusinessProcessExecutionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BusinessProcessExecutionRepository extends JpaRepository<BusinessProcessExecutionEntity, String> {
    Optional<BusinessProcessExecutionEntity> findByCorrelationId(String correlationId);

    Optional<BusinessProcessExecutionEntity> findByBusinessProcessNameAndCorrelationId(String businessProcessName, String correlationId);
}
