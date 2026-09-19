package com.businessprocess.businessprocess.api.controller;

import com.businessprocess.businessprocess.api.dto.BusinessProcessRetryRequest;
import com.businessprocess.businessprocess.api.dto.BusinessProcessRetryResponse;
import com.businessprocess.businessprocess.service.BusinessProcessRetryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/operations/business-process-executions")
public class BusinessProcessRetryController {
    private final BusinessProcessRetryService retryService;

    public BusinessProcessRetryController(BusinessProcessRetryService retryService) { this.retryService = retryService; }

    @PostMapping("/retry")
    public ResponseEntity<BusinessProcessRetryResponse> retry(@RequestBody BusinessProcessRetryRequest request) {
        return ResponseEntity.ok(retryService.retry(request));
    }
}
