package com.businessprocess.businessprocess.config;

import com.businessprocess.core.config.CoreConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.annotation.EnableKafka;

@Configuration
@EnableKafka
@Import(CoreConfiguration.class)
@ComponentScan(basePackages = {"com.businessprocess.engine", "com.businessprocess.businessprocess", "com.businessprocess.businessprocess.discovery"})
public class AppConfiguration {
}
