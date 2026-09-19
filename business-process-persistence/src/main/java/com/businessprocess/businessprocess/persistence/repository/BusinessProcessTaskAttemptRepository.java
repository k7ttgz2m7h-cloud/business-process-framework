package com.businessprocess.businessprocess.persistence.repository;

import com.businessprocess.businessprocess.persistence.entity.BusinessProcessTaskAttemptEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BusinessProcessTaskAttemptRepository extends JpaRepository<BusinessProcessTaskAttemptEntity, String> {
}
