# Business Process Framework

A lightweight saga orchestration framework for backend process flows.

StepFlow executes a business process definition as ordered steps and tasks. Each task calls a registered provider, stores request/response payloads on the task, and allows later tasks to use previous outputs. Business logic stays inside provider services; the framework only controls flow, retry, compensation, and execution visibility.

## Architecture

```text
business-process-app
  REST API / Kafka trigger
        |
        v
business-process-engine
  BusinessProcessParser -> BusinessProcessEngine -> BusinessProcessMonitor
        |                         |
        v                         v
business-process-core
  Task, BusinessProcessStep, TaskDelegate, ProviderRegistry,
  RetryPolicy, CompensationExecutor
business-process-service-registry
  Provider host/port lookup and health status
```

## Modules

- `business-process-core`: shared models, task delegate contracts, provider registry, retry policy, and compensation executor.
- `business-process-engine`: business process parsing, execution, monitoring, and payload path resolution.
- `business-process-app`: Spring Boot application exposing REST APIs and a minimal Kafka trigger listener.
- `business-process-service-registry`: Spring Boot service that tracks provider service locations and health.

## Business Process Model

A business process contains ordered steps. Each step contains one or more tasks.

```text
BusinessProcess
  -> Step
    -> Task
      -> requestPayload
      -> responsePayload
```

The orchestrator is domain-neutral. Payment, inventory, shipping, email, and other business actions should be implemented by provider services and exposed through APIs.

## Triggers

Business processes can currently be started through:

- REST: `POST /api/businessprocessflow/execute`
- Kafka topic: `business-process-trigger`

REST execution request example:

```json
{
  "businessProcessName": "order-flow-bp",
  "inputPayload": {
    "clientRequestId": "REQ-1001",
    "customerId": "CUST-1",
    "priceBookId": "PB-1",
    "items": [
      {
        "productId": "PROD-1",
        "quantity": 2
      }
    ]
  }
}
```

`order-service` generates the internal `orderId`. The Business process passes `clientRequestId` to make create-order retry-safe and uses the generated `orderId` from the create-order response in later steps.

The end-to-end order process does not execute notification steps. Customer contact lookup and conditional notifications can be added as a future enhancement.

Execution details can be fetched with:

- `POST /api/businessprocessflow/execution`
- `GET /api/businessprocessflow/{executionId}`
- `GET /api/businessprocessflow/{executionId}/metrics`

Kafka trigger message example:

```json
{
  "businessProcessName": "order-flow-bp",
  "inputPayload": {
    "clientRequestId": "REQ-1001",
    "customerId": "CUST-1",
    "priceBookId": "PB-1",
    "items": [
      {
        "productId": "PROD-1",
        "quantity": 2
      }
    ]
  }
}
```

## Build

```bash
./mvnw clean package
```

## Install

```bash
./mvnw clean install
./mvnw verify
```

## Run

Start the service registry first:

```bash
./mvnw -pl business-process-service-registry spring-boot:run
```

The service registry runs on port `8090` by default and exposes:

```text
GET /api/services/status
GET /api/services/status?refresh=true
GET /api/services/status/{name}
GET /api/services/status/summary
```

Then start `business-process-app`:

```bash
./mvnw -pl business-process-app -am spring-boot:run
```

The app runs on port `8080` by default.

Provider services are expected at the ports configured in `business-process-service-registry/src/main/resources/services-registry.yml`:

```text
business-process-app 8080
order-service        8081
payment-service      8082
customer-service     8083
inventory-service    8084
pricing-service      8085
fraud-service        8086
fulfillment-service  8087
shipping-service     8088
notification-service 8089
```

`business-process-app` is also registered as a provider so one business process can delegate work to another business process through `POST /api/businessprocessflow/execute`.

The included `order-flow-bp` process currently completes without delegating to `notification-flow-bp`.

## Database Details

`business-process-app` uses an in-memory H2 database for business process definitions and execution history:

```text
Service:  business-process-app
Port:     8080
H2 URL:   jdbc:h2:mem:saga;MODE=PostgreSQL;DB_CLOSE_DELAY=-1
Console:  http://localhost:8080/h2-console
User:     sa
Password:
DDL:      update
```

Business process templates are stored in the `business_process` table. Executions, steps, and tasks are stored in:

```text
business_process_execution
business_process_step
business_process_task
```

Most sample provider services also use in-memory H2 databases:

```text
Service               Port  H2 URL                         DDL
order-service         8081  jdbc:h2:mem:orderdb            create-drop
payment-service       8082  jdbc:h2:mem:paymentdb          create-drop
customer-service      8083  jdbc:h2:mem:customerdb         create-drop
inventory-service     8084  jdbc:h2:mem:inventorydb        create-drop
pricing-service       8085  jdbc:h2:mem:pricingdb          create-drop
fulfillment-service   8087  jdbc:h2:mem:fulfillmentdb      create-drop
shipping-service      8088  jdbc:h2:mem:shippingdb         create-drop
notification-service  8089  jdbc:h2:mem:notificationdb     create-drop
```

The sample services use the default H2 credentials unless explicitly configured:

```text
User:     sa
Password:
```

`fraud-service` does not configure a datasource. `business-process-service-registry` also does not use a database; it reads provider configuration from `services-registry.yml`.

## Notes

- Business process definitions are loaded from YAML resources under `business-process-flows/` at startup and saved to the application database.
- `business-process-app` resolves provider base URLs through `business-process-service-registry` at `http://localhost:8090`.
- DB-backed business process storage, durable execution state, encrypted payload storage, and reprocessing are future runtime concerns.
- Kafka is used as a business process trigger, not as a topic-per-step choreography model.
