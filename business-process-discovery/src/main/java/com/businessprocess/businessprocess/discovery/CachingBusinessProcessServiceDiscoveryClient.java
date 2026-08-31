package com.businessprocess.businessprocess.discovery;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Resolves provider names through the business-process-service-registry API and caches successful
 * lookups until TTL expiry. Registry-call failures are intentionally propagated;
 * stale cache entries are not used after expiry.
 */
@Service
public class CachingBusinessProcessServiceDiscoveryClient implements BusinessProcessServiceDiscoveryClient {
    private static final Logger log = LoggerFactory.getLogger(CachingBusinessProcessServiceDiscoveryClient.class);

    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();
    private final BusinessProcessServiceRegistryClient registryClient;
    private final BusinessProcessServiceDiscoveryProperties properties;
    private final Clock clock;

    public CachingBusinessProcessServiceDiscoveryClient(
            BusinessProcessServiceRegistryClient registryClient,
            BusinessProcessServiceDiscoveryProperties properties,
            Clock clock
    ) {
        this.registryClient = registryClient;
        this.properties = properties;
        this.clock = clock;
    }

    @Override
    public BusinessProcessServiceEndpoint resolve(String providerName) {
        validate(providerName);
        Instant now = clock.instant();
        CacheEntry cached = cache.get(providerName);
        if (cached != null && cached.expiresAt().isAfter(now)) {
            log.debug("Service discovery cache hit: providerName={}", providerName);
            return cached.endpoint();
        }

        if (cached == null) {
            log.debug("Service discovery cache miss: providerName={}", providerName);
        } else {
            log.debug("Service discovery cache expired: providerName={}", providerName);
        }

        return refresh(providerName, now);
    }

    private synchronized BusinessProcessServiceEndpoint refresh(String providerName, Instant now) {
        CacheEntry cached = cache.get(providerName);
        if (cached != null && cached.expiresAt().isAfter(now)) {
            log.debug("Service discovery cache hit after wait: providerName={}", providerName);
            return cached.endpoint();
        }

        log.debug("Calling service registry: providerName={}", providerName);
        BusinessProcessServiceEndpoint endpoint = registryClient.fetch(providerName);
        Instant expiresAt = clock.instant().plus(properties.getCacheTtl());
        cache.put(providerName, new CacheEntry(endpoint, expiresAt));
        return endpoint;
    }

    private void validate(String providerName) {
        if (providerName == null || providerName.isBlank()) {
            throw new IllegalArgumentException("providerName is required");
        }
    }

    private record CacheEntry(BusinessProcessServiceEndpoint endpoint, Instant expiresAt) {
    }
}
