package com.businessprocess.businessprocess.persistence.repository;

import com.businessprocess.businessprocess.persistence.entity.BusinessProcessDefinitionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BusinessProcessDefinitionRepository extends JpaRepository<BusinessProcessDefinitionEntity, Long> {
    Optional<BusinessProcessDefinitionEntity> findByBusinessProcessName(String businessProcessName);

    Optional<BusinessProcessDefinitionEntity> findByBusinessProcessNameAndVersion(String businessProcessName, String version);
}
