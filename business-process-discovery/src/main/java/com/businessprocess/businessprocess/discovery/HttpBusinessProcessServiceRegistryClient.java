package com.businessprocess.businessprocess.discovery;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.net.URI;

@Component
public class HttpBusinessProcessServiceRegistryClient implements BusinessProcessServiceRegistryClient {
    private final RestTemplate restTemplate;
    private final BusinessProcessServiceDiscoveryProperties properties;

    public HttpBusinessProcessServiceRegistryClient(BusinessProcessServiceDiscoveryProperties properties) {
        this.restTemplate = new RestTemplate();
        this.properties = properties;
    }

    @Override
    public BusinessProcessServiceEndpoint fetch(String providerName) {
        String url = properties.getRegistryBaseUrl() + "/api/services/status/" + providerName + "?refresh=true";
        RegistryServiceStatus status = restTemplate.getForObject(url, RegistryServiceStatus.class);
        if (status == null) {
            throw new IllegalStateException("Service registry returned empty response for provider: " + providerName);
        }
        return new BusinessProcessServiceEndpoint(status.getName(), baseUrl(status.getUrl()), status.getStatus());
    }

    private String baseUrl(String healthUrl) {
        URI uri = URI.create(healthUrl);
        int port = uri.getPort();
        return uri.getScheme() + "://" + uri.getHost() + (port >= 0 ? ":" + port : "");
    }

    private static class RegistryServiceStatus {
        private String name;
        private String url;
        private String status;

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

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }
    }
}
