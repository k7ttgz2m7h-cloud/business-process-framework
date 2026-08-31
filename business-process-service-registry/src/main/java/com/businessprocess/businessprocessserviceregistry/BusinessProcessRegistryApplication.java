package com.businessprocess.businessprocessserviceregistry;

import com.businessprocess.businessprocessserviceregistry.config.BusinessProcessRegistryProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties(BusinessProcessRegistryProperties.class)
public class BusinessProcessRegistryApplication {
    public static void main(String[] args) {
        SpringApplication.run(BusinessProcessRegistryApplication.class, args);
    }
}
