package com.businessprocess.businessprocessserviceregistry.service;

import com.businessprocess.businessprocessserviceregistry.config.BusinessProcessRegistryProperties;
import com.businessprocess.businessprocessserviceregistry.model.BusinessProcessServiceEndpoint;
import com.businessprocess.businessprocessserviceregistry.model.BusinessProcessServiceHealthStatus;
import com.businessprocess.businessprocessserviceregistry.model.BusinessProcessServiceHealthStatus.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class BusinessProcessServiceHealthCheckerService {
    private static final Logger log = LoggerFactory.getLogger(BusinessProcessServiceHealthCheckerService.class);

    private final BusinessProcessRegistryProperties properties;
    private final WebClient webClient;
    private final Map<String, BusinessProcessServiceHealthStatus> latestStatuses = new ConcurrentHashMap<>();

    public BusinessProcessServiceHealthCheckerService(BusinessProcessRegistryProperties properties, WebClient.Builder webClientBuilder) {
        this.properties = properties;
        this.webClient = webClientBuilder.build();
    }

    @Scheduled(fixedDelayString = "${business-process-service-registry.check-interval-ms:10000}", initialDelay = 1000)
    public void refreshStatuses() {
        checkAllLive();
    }

    public List<BusinessProcessServiceHealthStatus> getStatuses(boolean refresh) {
        if (refresh || latestStatuses.isEmpty()) {
            return checkAllLive();
        }
        return sorted(new ArrayList<>(latestStatuses.values()));
    }

    public Optional<BusinessProcessServiceHealthStatus> getStatus(String name, boolean refresh) {
        if (refresh || latestStatuses.isEmpty()) {
            checkAllLive();
        }
        return Optional.ofNullable(latestStatuses.get(name));
    }

    public Map<String, Long> getSummary(boolean refresh) {
        List<BusinessProcessServiceHealthStatus> statuses = getStatuses(refresh);
        long up = statuses.stream().filter(status -> status.getStatus() == Status.UP).count();
        long down = statuses.stream().filter(status -> status.getStatus() == Status.DOWN).count();
        return Map.of("total", (long) statuses.size(), "up", up, "down", down);
    }

    private List<BusinessProcessServiceHealthStatus> checkAllLive() {
        List<BusinessProcessServiceHealthStatus> statuses = Flux.fromIterable(properties.getServices())
                .flatMap(this::check)
                .collectList()
                .block(Duration.ofMillis(totalTimeoutMs()));

        if (statuses == null) {
            return List.of();
        }

        statuses.forEach(this::updateStatus);
        return sorted(statuses);
    }

    private Mono<BusinessProcessServiceHealthStatus> check(BusinessProcessServiceEndpoint endpoint) {
        String url = endpoint.url();
        long started = System.nanoTime();

        return webClient.get()
                .uri(url)
                .exchangeToMono(response -> {
                    long responseTimeMs = elapsedMs(started);
                    Status status = response.statusCode().is2xxSuccessful() ? Status.UP : Status.DOWN;
                    return Mono.just(new BusinessProcessServiceHealthStatus(endpoint.getName(), url, status, responseTimeMs, Instant.now()));
                })
                .timeout(Duration.ofMillis(endpoint.getTimeoutMs()))
                .onErrorResume(error -> Mono.just(new BusinessProcessServiceHealthStatus(endpoint.getName(), url, Status.DOWN, null, Instant.now())))
                .doOnNext(status -> log.debug("Checked service: name={}, url={}, status={}, responseTimeMs={}",
                        status.getName(), status.getUrl(), status.getStatus(), status.getResponseTimeMs()));
    }

    private void updateStatus(BusinessProcessServiceHealthStatus status) {
        BusinessProcessServiceHealthStatus previous = latestStatuses.put(status.getName(), status);
        if (previous == null) {
            log.info("Service {} is {}", status.getName(), status.getStatus());
            return;
        }
        if (previous.getStatus() != status.getStatus()) {
            if (status.getStatus() == Status.UP) {
                log.info("Service status changed: {} {} -> {}", status.getName(), previous.getStatus(), status.getStatus());
            } else {
                log.warn("Service status changed: {} {} -> {}", status.getName(), previous.getStatus(), status.getStatus());
            }
        }
    }

    private List<BusinessProcessServiceHealthStatus> sorted(List<BusinessProcessServiceHealthStatus> statuses) {
        return statuses.stream()
                .sorted(Comparator.comparing(BusinessProcessServiceHealthStatus::getName))
                .toList();
    }

    private long elapsedMs(long started) {
        return Duration.ofNanos(System.nanoTime() - started).toMillis();
    }

    private long totalTimeoutMs() {
        long maxTimeout = properties.getServices().stream()
                .mapToLong(BusinessProcessServiceEndpoint::getTimeoutMs)
                .max()
                .orElse(2000);
        return Math.max(maxTimeout + 1000, 3000);
    }
}
