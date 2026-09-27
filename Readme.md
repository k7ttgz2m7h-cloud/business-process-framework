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

A business-safe process summary, including all steps and their tasks, can be fetched by correlation ID:

- `GET /api/businessprocesses/correlations/{correlationId}`

The summary maps completed work to `PASSED`, active work to `IN_PROGRESS`, and failed or compensated work to `FAILED`. It does not expose execution IDs or request and response payloads.

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
business_process
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

## Additional test business processes

`business-process-flows/` contains 10 YAML definitions. The startup loader discovers
all `*.yaml` files in this folder; it has no configured definition-count limit.
This does not establish a concurrent-execution capacity limit.

The six additional scenarios use existing registered sample services:

| Business process name | Steps | Scenario |
| --- | --- | --- |
| `customer-verification-bp` | 2 | Create an order, then fetch and verify its customer |
| `order-quotation-bp` | 3 | Create an order, verify the customer, fetch a price book and calculate pricing |
| `inventory-reservation-bp` | 3 | Create an order, verify the customer and reserve stock |
| `payment-capture-bp` | 4 | Create an order, calculate pricing, authorize and capture payment |
| `order-dispatch-bp` | 4 | Create an order, reserve stock, create fulfillment and arrange shipping |
| `compact-order-bp` | 10 | Order creation through customer verification, inventory, pricing, payment authorization, fraud screening, fulfillment, shipping, capture and completion |

These are independent test scenarios. Intermediate flows intentionally leave their
successful orders/reservations/payments in the resulting state; compensation runs
on applicable failures, not as cleanup after successful execution. They preserve
the corresponding compensation definitions from the end-to-end flow.

Start the registry, app, and each provider used by the selected scenario. Submit
this request to `POST /api/businessprocessflow/execute`, substituting any name above:

```json
{
  "businessProcessName": "compact-order-bp",
  "inputPayload": {
    "clientRequestId": "COMPACT-TEST-001",
    "customerId": "CUST-1",
    "priceBookId": "PB-1",
    "items": [{"productId": "PROD-1", "quantity": 2}]
  }
}
```

Use a fresh `clientRequestId` for each independent run; sample providers can reject
repeated authorizations, fulfillment requests, and shipments for the same order.
Use customer/product/price-book identifiers present in the sample service data.
For a failure-path check, run `inventory-reservation-bp` with a nonexistent
`customerId`: customer lookup should fail after order creation and exercise order
compensation. Inspect the existing execution-details API for the resulting state.

## ECS logging with Elasticsearch and Kibana

The framework, registry, and all nine sample services write one ECS (Elastic
Common Schema) JSON object per log line. Fields include `@timestamp`,
`service.name`, `log.level`, and `message`. Console logs remain readable.
One shared Logstash instance collects the files and sends them to daily
Elasticsearch indices named `stepflow-logs-YYYY.MM.dd`.

### Application log location

Before starting applications, source the environment script in Bash from the
`business-process-framework` directory:

```bash
source ./stepflow-env.sh
```

It derives `<stepflow>/temp/stepflow-logs` from the checkout location, without
hardcoding a machine path. An existing `STEPFLOW_LOG_DIR` override is preserved.
The sample-services `start-all.sh` sets this location automatically. For IDE
launches, set `STEPFLOW_LOG_DIR` to the same absolute directory in each run
configuration. Restart applications after changing their logging environment.

The default layout is:

```text
stepflow/
├── business-process-framework/
│   └── logstash.conf
└── temp/
    └── stepflow-logs/
        ├── business-process-manager.json
        ├── business-process-service-registry.json
        ├── payment-service.json
        └── ...
```

### Start Logstash with Docker

Start Docker Desktop, Elasticsearch, and Kibana first. Logstash runs separately
from the applications. The following command uses Logstash **9.5.4**, matching
the Elasticsearch version used in this setup, and assumes Elasticsearch is
exposed on the Mac at `http://localhost:9200`.

Configure Elasticsearch authentication in the `elasticsearch` output block of
[logstash.conf](logstash.conf). Use credentials with permission to write the log
indices; keep passwords out of Git. For environment-based credentials, use:

```text
user => "${ELASTICSEARCH_USERNAME}"
password => "${ELASTICSEARCH_PASSWORD}"
```

Export those variables locally and add `-e ELASTICSEARCH_USERNAME` and
`-e ELASTICSEARCH_PASSWORD` to the Docker command when using that configuration.
For HTTPS, also configure the trusted CA certificate in the pipeline, mount it
into the container, and change `ELASTICSEARCH_URL` to the HTTPS endpoint.

Run from **`business-process-framework`**:

```bash
docker run --rm --name stepflow-logstash \
  --mount "type=bind,source=$PWD/logstash.conf,target=/usr/share/logstash/pipeline/logstash.conf,readonly" \
  --mount "type=bind,source=$(cd .. && pwd)/temp/stepflow-logs,target=/stepflow-logs,readonly" \
  --mount "type=volume,source=stepflow-logstash-data,target=/usr/share/logstash/data" \
  -e STEPFLOW_LOG_DIR=/stepflow-logs \
  -e ELASTICSEARCH_URL=http://host.docker.internal:9200 \
  docker.elastic.co/logstash/logstash:9.5.4
```

The host source folder must exist and contain the JSON files directly. If you
use a custom application log directory, replace the source folder in the mount.
Mount `temp/stepflow-logs`, not its parent `temp`; otherwise the files appear one
level below the path Logstash watches. `temp` and `tmp` are different directories.
Inside Docker, `host.docker.internal` addresses the Mac; `localhost` addresses
the Logstash container itself.

Leave the command running. `--rm` removes the container when it stops, so rerun
the command for the next session. The named data volume preserves file read
positions: previously unseen files are read from the beginning; after restarts,
Logstash resumes from saved offsets and continues watching for new entries.

To run in the background and restart with Docker, replace
`docker run --rm --name stepflow-logstash` with
`docker run -d --restart unless-stopped --name stepflow-logstash` when creating
the container. Stop any existing instance first. A manually stopped persistent
container can be started with `docker start stepflow-logstash`.

### Verify ingestion and create a Kibana data view

In another terminal, check the collector and its mounted files:

```bash
docker logs --tail 100 stepflow-logstash
docker exec stepflow-logstash sh -c 'ls -lh /stepflow-logs/*.json'
```

Open Kibana at `http://127.0.0.1:5601`. In **Dev Tools → Console**, run:

```http
GET _cat/indices/stepflow-logs-*?v
```

Wait for an index with a nonzero `docs.count`. Container log output alone does
not confirm successful ingestion.

1. Find **Data Views** through Kibana's global search, or open the data view
   dropdown in **Discover**.
2. Select **Create data view** and enter:
   - **Name:** `Stepflow Logs`
   - **Index pattern:** `stepflow-logs-*` (without quotes or backticks)
   - **Timestamp field:** `@timestamp`
3. Save the data view, open **Discover**, and select **Stepflow Logs**.
4. Set the time range to include application startup. Add `@timestamp`,
   `service.name`, `log.level`, and `message` as columns.

### Search service and Tomcat logs

Use **KQL** mode in Discover. For Tomcat startup in one service:

```text
service.name: "payment-service" AND message: "Tomcat started"
```

For Tomcat startup across all services:

```text
message: "Tomcat started"
```

For all framework application logs:

```text
service.name: "business-process-manager"
```

For errors across every service:

```text
log.level: "ERROR"
```

### Troubleshooting
hard-refresh
  the browser or try a private window. If it persists, restart the Kibana
  container and reload after startup. This is a browser asset-loading failure,
  not an Elasticsearch search timeout.
