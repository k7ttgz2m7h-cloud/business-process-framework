package com.businessprocess.businessprocessserviceregistry.model;

import java.time.Instant;

public class BusinessProcessServiceHealthStatus {
    private String name;
    private String url;
    private Status status;
    private Long responseTimeMs;
    private Instant checkedAt;

    public enum Status {
        UP,
        DOWN
    }

    public BusinessProcessServiceHealthStatus() {
    }

    public BusinessProcessServiceHealthStatus(String name, String url, Status status, Long responseTimeMs, Instant checkedAt) {
        this.name = name;
        this.url = url;
        this.status = status;
        this.responseTimeMs = responseTimeMs;
        this.checkedAt = checkedAt;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public Long getResponseTimeMs() {
        return responseTimeMs;
    }

    public void setResponseTimeMs(Long responseTimeMs) {
        this.responseTimeMs = responseTimeMs;
    }

    public Instant getCheckedAt() {
        return checkedAt;
    }

    public void setCheckedAt(Instant checkedAt) {
        this.checkedAt = checkedAt;
    }
}
