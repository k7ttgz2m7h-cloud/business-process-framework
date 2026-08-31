package com.businessprocess.businessprocessserviceregistry.config;

import com.businessprocess.businessprocessserviceregistry.model.BusinessProcessServiceEndpoint;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "registry")
public class BusinessProcessRegistryProperties {
    private List<BusinessProcessServiceEndpoint> services = new ArrayList<>();

    public List<BusinessProcessServiceEndpoint> getServices() {
        return services;
    }

    public void setServices(List<BusinessProcessServiceEndpoint> services) {
        this.services = services;
    }
}
