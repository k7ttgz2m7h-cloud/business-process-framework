package com.businessprocess.businessprocess.persistence.repository;

import com.businessprocess.businessprocess.persistence.entity.BusinessProcessStepEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BusinessProcessStepRepository extends JpaRepository<BusinessProcessStepEntity, Long> {
    List<BusinessProcessStepEntity> findByCorrelationIdOrderByStepNumberAsc(String correlationId);

    List<BusinessProcessStepEntity> findByExecutionExecutionIdOrderByStepNumberAsc(String executionId);
}
