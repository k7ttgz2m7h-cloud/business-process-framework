package com.businessprocess.engine.service;

import com.businessprocess.core.model.base.BasePayload;
import com.businessprocess.engine.engine.BusinessProcessEngine;
import com.businessprocess.engine.engine.BusinessProcessExecution;
import com.businessprocess.engine.model.BusinessProcessDefinition;
import com.businessprocess.engine.parser.BusinessProcessParser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class BusinessProcessOrchestrationService {
    private final BusinessProcessEngine businessProcessEngine;
    @Qualifier("configBusinessProcessParser")
    private final BusinessProcessParser parser;

    public BusinessProcessOrchestrationService(BusinessProcessEngine businessProcessEngine, BusinessProcessParser parser) {
        this.businessProcessEngine = businessProcessEngine;
        this.parser = parser;
    }

    public BusinessProcessExecution executeBusinessProcessflow(String businessProcessPath) {
        BusinessProcessDefinition businessProcess = parser.parseBusinessProcess(businessProcessPath);
        return businessProcessEngine.execute(businessProcess);
    }

    public BusinessProcessExecution executeBusinessProcessflow(String businessProcessPath, BasePayload initialPayload) {
        BusinessProcessDefinition businessProcess = parser.parseBusinessProcess(businessProcessPath);
        return businessProcessEngine.execute(businessProcess, initialPayload);
    }

    public BusinessProcessDefinition parseBusinessProcess(String businessProcessPath) {
        return parser.parseBusinessProcess(businessProcessPath);
    }

    //this should be used for businessProcess apis
    public BusinessProcessDefinition parseBusinessProcessTemplate(String businessProcessTemplate) {
        return parser.parseBusinessProcessTemplate(businessProcessTemplate);
    }

    public BusinessProcessExecution executeBusinessProcessflow(BusinessProcessDefinition businessProcess) {
        return businessProcessEngine.execute(businessProcess);
    }

    public BusinessProcessExecution executeBusinessProcessflow(BusinessProcessDefinition businessProcess, BasePayload initialPayload) {
        return businessProcessEngine.execute(businessProcess, initialPayload);
    }
}
