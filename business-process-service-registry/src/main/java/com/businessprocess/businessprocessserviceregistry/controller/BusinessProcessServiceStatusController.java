package com.businessprocess.businessprocessserviceregistry.controller;

import com.businessprocess.businessprocessserviceregistry.model.BusinessProcessServiceHealthStatus;
import com.businessprocess.businessprocessserviceregistry.service.BusinessProcessServiceHealthCheckerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/services/status")
public class BusinessProcessServiceStatusController {
    private final BusinessProcessServiceHealthCheckerService healthChecker;

    public BusinessProcessServiceStatusController(BusinessProcessServiceHealthCheckerService healthChecker) {
        this.healthChecker = healthChecker;
    }

    @GetMapping
    public List<BusinessProcessServiceHealthStatus> getAllStatuses(@RequestParam(defaultValue = "false") boolean refresh) {
        return healthChecker.getStatuses(refresh);
    }

    @GetMapping("/{name}")
    public ResponseEntity<BusinessProcessServiceHealthStatus> getStatus(
            @PathVariable String name,
            @RequestParam(defaultValue = "false") boolean refresh
    ) {
        return healthChecker.getStatus(name, refresh)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/summary")
    public Map<String, Long> getSummary(@RequestParam(defaultValue = "false") boolean refresh) {
        return healthChecker.getSummary(refresh);
    }
}
