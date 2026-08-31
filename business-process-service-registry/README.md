# Service Registry

A small Spring Boot service that monitors configured service health endpoints.

## Add A Service

Edit `src/main/resources/services-registry.yml`:

```yaml
registry:
  services:
    - name: order-service
      host: localhost
      port: 8081
      healthPath: /actuator/health
      protocol: http
      timeoutMs: 2000
```

No code change is needed.

## Run

```bash
mvn spring-boot:run
```

The service runs on port `8090`.

## APIs

```text
GET /api/services/status
GET /api/services/status?refresh=true
GET /api/services/status/{name}
GET /api/services/status/summary
```
