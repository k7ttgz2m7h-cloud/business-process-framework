package com.businessprocess.businessprocess.api.controller;

import com.businessprocess.businessprocess.api.dto.FailedBusinessProcessSearchRequest;
import com.businessprocess.businessprocess.api.dto.FailedBusinessProcessSearchResponse;
import com.businessprocess.businessprocess.api.mapper.FailedBusinessProcessMapper;
import com.businessprocess.businessprocess.persistence.service.BusinessProcessExecutionQuery;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/v1/operations/business-process-executions")
public class FailedBusinessProcessController {
    private final BusinessProcessExecutionQuery executionQuery;
    private final FailedBusinessProcessMapper mapper;
    private static final Map<String, String> SORT_FIELDS = Map.of(
            "failedAt", "completedAt",
            "createdAt", "startedAt",
            "updatedAt", "completedAt",
            "businessProcessName", "businessProcessName",
            "failureType", "failureType"
    );

    public FailedBusinessProcessController(
            BusinessProcessExecutionQuery executionQuery,
            FailedBusinessProcessMapper mapper
    ) {
        this.executionQuery = executionQuery;
        this.mapper = mapper;
    }

    @PostMapping("/search")
    public ResponseEntity<FailedBusinessProcessSearchResponse> search(
            @RequestBody(required = false) FailedBusinessProcessSearchRequest request
    ) {
        FailedBusinessProcessSearchRequest search = request != null ? request : new FailedBusinessProcessSearchRequest();
        validate(search);
        Sort.Direction direction = Sort.Direction.fromString(search.getSortDirection());
        PageRequest pageable = PageRequest.of(search.getPage(), search.getSize(),
                Sort.by(direction, SORT_FIELDS.get(search.getSortBy())));

        return ResponseEntity.ok(mapper.toResponse(
                executionQuery.searchFailed(
                        search.getBusinessProcessName(),
                        search.getFailureTypes(),
                        pageable)));
    }

    private void validate(FailedBusinessProcessSearchRequest request) {
        if (request.getPage() == null || request.getPage() < 0) {
            throw badRequest("page must be zero or greater");
        }
        if (request.getSize() == null || request.getSize() < 1 || request.getSize() > 100) {
            throw badRequest("size must be between 1 and 100");
        }
        if (!SORT_FIELDS.containsKey(request.getSortBy())) {
            throw badRequest("sortBy must be one of " + Set.copyOf(SORT_FIELDS.keySet()));
        }
        if (request.getSortDirection() == null
                || !(request.getSortDirection().equalsIgnoreCase("ASC")
                || request.getSortDirection().equalsIgnoreCase("DESC"))) {
            throw badRequest("sortDirection must be ASC or DESC");
        }
    }

    private ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
