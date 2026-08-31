package com.businessprocess.businessprocess.discovery;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@ConfigurationProperties(prefix = "saga.service-discovery")
public class BusinessProcessServiceDiscoveryProperties {
    private String registryBaseUrl = "http://localhost:8090";
    private Duration cacheTtl = Duration.ofSeconds(30);

    public String getRegistryBaseUrl() {
        return registryBaseUrl;
    }

    public void setRegistryBaseUrl(String registryBaseUrl) {
        this.registryBaseUrl = registryBaseUrl;
    }

    public Duration getCacheTtl() {
        return cacheTtl;
    }

    public void setCacheTtl(Duration cacheTtl) {
        this.cacheTtl = cacheTtl;
    }
}
