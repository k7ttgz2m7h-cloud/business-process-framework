package com.businessprocess.businessprocess.discovery;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class BusinessProcessServiceDiscoveryPropertiesTest {
    @Test
    void defaultCacheTtlIsThirtySeconds() {
        BusinessProcessServiceDiscoveryProperties properties = new BusinessProcessServiceDiscoveryProperties();

        assertThat(properties.getCacheTtl()).isEqualTo(Duration.ofSeconds(30));
    }
}
