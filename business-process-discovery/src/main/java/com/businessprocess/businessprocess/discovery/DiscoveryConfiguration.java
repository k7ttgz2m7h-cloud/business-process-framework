package com.businessprocess.businessprocess.discovery;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class DiscoveryConfiguration {
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
