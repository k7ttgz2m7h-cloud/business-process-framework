package com.businessprocess.businessprocess.persistence.repository;

import com.businessprocess.businessprocess.persistence.entity.BusinessProcessTaskEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BusinessProcessTaskRepository extends JpaRepository<BusinessProcessTaskEntity, Long> {
    List<BusinessProcessTaskEntity> findByCorrelationIdOrderByStepIdAscTaskOrderAsc(String correlationId);

    List<BusinessProcessTaskEntity> findByExecutionExecutionIdOrderByStepIdAscTaskOrderAsc(String executionId);
}
