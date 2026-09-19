package com.businessprocess.businessprocess.api.controller;

import com.businessprocess.businessprocess.api.dto.BusinessProcessSummaryResponse;
import com.businessprocess.businessprocess.api.mapper.BusinessProcessSummaryMapper;
import com.businessprocess.businessprocess.persistence.service.BusinessProcessExecutionQuery;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/businessprocesses")
@Slf4j
public class BusinessProcessSummaryController {

    private final BusinessProcessExecutionQuery executionQuery;
    private final BusinessProcessSummaryMapper summaryMapper;

    public BusinessProcessSummaryController(
            BusinessProcessExecutionQuery executionQuery,
            BusinessProcessSummaryMapper summaryMapper
    ) {
        this.executionQuery = executionQuery;
        this.summaryMapper = summaryMapper;
    }

    @GetMapping("/correlations/{correlationId}")
    public ResponseEntity<BusinessProcessSummaryResponse> getByCorrelationId(
            @PathVariable String correlationId
    ) {
        log.info("Fetching business process summary for correlationId: {}", correlationId);
        try {
            return ResponseEntity.ok(summaryMapper.toResponse(
                    executionQuery.getDetails(correlationId)));
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.notFound().build();
        }
    }

}
