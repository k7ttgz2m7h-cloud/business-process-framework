package com.businessprocess.businessprocess.persistence.service;

import com.businessprocess.businessprocess.persistence.entity.BusinessProcessDefinitionEntity;
import com.businessprocess.engine.model.BusinessProcessDefinition;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import com.businessprocess.businessprocess.persistence.repository.BusinessProcessDefinitionRepository;

@Service
@Slf4j
public class BusinessProcessDefinitionPersistence {
    private final BusinessProcessDefinitionRepository repository;

    public BusinessProcessDefinitionPersistence(BusinessProcessDefinitionRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public BusinessProcessDefinitionEntity saveDefinition(BusinessProcessDefinition definition, String processKey, String sourcePath) {
        String version = definition.getVersion() == null ? "1" : definition.getVersion();
        var existing = repository.findByBusinessProcessNameAndVersion(processKey, version);
        if (existing.isPresent()) return existing.get();
        BusinessProcessDefinitionEntity entity = new BusinessProcessDefinitionEntity();
        entity.setBusinessProcessName(processKey); entity.setVersion(version);
        entity.setBusinessProcessTemplate(readSource(sourcePath));
        return repository.save(entity);
    }

    @Transactional
    public BusinessProcessDefinitionEntity saveBusinessProcessTemplate(BusinessProcessDefinition definition, String name, String template) {
        String version = definition.getVersion() == null ? "1" : definition.getVersion();
        var existing = repository.findByBusinessProcessNameAndVersion(name, version);
        if (existing.isPresent()) { var entity = existing.get(); entity.setBusinessProcessTemplate(template); entity.setUpdatedAt(LocalDateTime.now()); return repository.save(entity); }
        BusinessProcessDefinitionEntity entity = new BusinessProcessDefinitionEntity();
        entity.setBusinessProcessName(name); entity.setVersion(version); entity.setBusinessProcessTemplate(template);
        return repository.save(entity);
    }

    @Transactional(readOnly = true)
    public BusinessProcessDefinitionEntity getDefinition(String name) {
        return repository.findByBusinessProcessName(name).orElseThrow(() -> new IllegalArgumentException("Business process template not found for process name: " + name));
    }

    private String readSource(String sourcePath) {
        if (sourcePath == null || sourcePath.isBlank()) return "";
        try {
            String normalized = sourcePath.startsWith("/") ? sourcePath.substring(1) : sourcePath;
            ClassPathResource resource = new ClassPathResource(normalized);
            if (resource.exists()) return new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            return Files.readString(Path.of(sourcePath));
        } catch (IOException exception) {
            log.warn("Could not read businessProcess source for path '{}', storing path only", sourcePath);
            return sourcePath;
        }
    }
}
