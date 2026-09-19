package com.businessprocess.businessprocess.persistence.repository;

import com.businessprocess.businessprocess.persistence.entity.BusinessProcessAuditEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BusinessProcessAuditEventRepository extends JpaRepository<BusinessProcessAuditEventEntity, String> {
}
