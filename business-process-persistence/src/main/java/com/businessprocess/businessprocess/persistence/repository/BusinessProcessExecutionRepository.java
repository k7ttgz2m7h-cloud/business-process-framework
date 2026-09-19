package com.businessprocess.businessprocess.persistence.repository;

import com.businessprocess.businessprocess.persistence.entity.BusinessProcessExecutionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface BusinessProcessExecutionRepository extends JpaRepository<BusinessProcessExecutionEntity, String>,
        JpaSpecificationExecutor<BusinessProcessExecutionEntity> {
    Optional<BusinessProcessExecutionEntity> findFirstByCorrelationIdOrderByStartedAtDescExecutionIdDesc(String correlationId);

    Optional<BusinessProcessExecutionEntity> findFirstByBusinessProcessNameAndCorrelationIdOrderByStartedAtDescExecutionIdDesc(String businessProcessName, String correlationId);
}
