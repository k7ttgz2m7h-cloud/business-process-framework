package com.businessprocess.businessprocess.discovery;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class CachingBusinessProcessServiceDiscoveryClientTest {
    @Test
    void cacheMissCallsRegistry() {
        FakeRegistryClient registryClient = new FakeRegistryClient();
        CachingBusinessProcessServiceDiscoveryClient client = client(registryClient, Duration.ofSeconds(30), new MutableClock());

        BusinessProcessServiceEndpoint endpoint = client.resolve("order-service");

        assertThat(endpoint.getUrl()).isEqualTo("http://localhost:8081");
        assertThat(registryClient.calls()).isEqualTo(1);
    }

    @Test
    void cacheHitDoesNotCallRegistryAgain() {
        FakeRegistryClient registryClient = new FakeRegistryClient();
        CachingBusinessProcessServiceDiscoveryClient client = client(registryClient, Duration.ofSeconds(30), new MutableClock());

        client.resolve("order-service");
        client.resolve("order-service");

        assertThat(registryClient.calls()).isEqualTo(1);
    }

    @Test
    void expiredEntryRefreshesFromRegistry() {
        FakeRegistryClient registryClient = new FakeRegistryClient();
        MutableClock clock = new MutableClock();
        CachingBusinessProcessServiceDiscoveryClient client = client(registryClient, Duration.ofSeconds(30), clock);

        client.resolve("order-service");
        clock.advance(Duration.ofSeconds(31));
        client.resolve("order-service");

        assertThat(registryClient.calls()).isEqualTo(2);
    }

    @Test
    void concurrentResolveOnlyFetchesOnce() throws Exception {
        FakeRegistryClient registryClient = new FakeRegistryClient();
        CachingBusinessProcessServiceDiscoveryClient client = client(registryClient, Duration.ofSeconds(30), new MutableClock());
        var executor = Executors.newFixedThreadPool(8);

        List<Callable<BusinessProcessServiceEndpoint>> calls = java.util.stream.IntStream.range(0, 16)
                .mapToObj(i -> (Callable<BusinessProcessServiceEndpoint>) () -> client.resolve("order-service"))
                .toList();

        executor.invokeAll(calls);
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);

        assertThat(registryClient.calls()).isEqualTo(1);
    }

    private CachingBusinessProcessServiceDiscoveryClient client(
            BusinessProcessServiceRegistryClient registryClient,
            Duration ttl,
            Clock clock
    ) {
        BusinessProcessServiceDiscoveryProperties properties = new BusinessProcessServiceDiscoveryProperties();
        properties.setCacheTtl(ttl);
        return new CachingBusinessProcessServiceDiscoveryClient(registryClient, properties, clock);
    }

    private static class FakeRegistryClient implements BusinessProcessServiceRegistryClient {
        private final AtomicInteger calls = new AtomicInteger();

        @Override
        public BusinessProcessServiceEndpoint fetch(String providerName) {
            calls.incrementAndGet();
            return new BusinessProcessServiceEndpoint(providerName, "http://localhost:8081", "UP");
        }

        int calls() {
            return calls.get();
        }
    }

    private static class MutableClock extends Clock {
        private Instant instant = Instant.parse("2026-08-02T00:00:00Z");

        void advance(Duration duration) {
            instant = instant.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneId.of("UTC");
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
