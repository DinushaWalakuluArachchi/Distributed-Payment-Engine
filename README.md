# Saga-Orchestrated Distributed Microservice Payment Engine

A production-grade microservices payment backend built with Java 17 and Spring Boot 3.3.4,
implementing distributed saga orchestration, idempotent payment processing, and
adaptive fraud detection.

## System Architecture

The project is designed as a highly scalable, resilient, and distributed payment processing engine. It follows a microservices architecture coordinated via the Saga Pattern to ensure data consistency across distributed transactions.

The detailed structural layout of the engine is illustrated below based on `payment_engine_architecture.jpg`:

![Payment Engine Architecture](payment_engine_architecture.jpg)

### Core Architectural Components

#### 1. API Gateway
* **Responsibilities:** Acts as the single entry point for all incoming client requests. It handles Authentication (Auth), Idempotency checks (to prevent double-charging), and dynamic request routing to downstream services.

#### 2. Payment Orchestrator (Saga Coordinator)
* **Responsibilities:** Drives the core workflow of the payment processing lifecycle. It implements the Saga Pattern to coordinate distributed transactions across multiple microservices, handling both successful commit flows and rollback/compensation logic if a failure occurs.

#### 3. Core Microservices
* **Account Service:** Manages balance checks, ledger balances, and processes financial debit and credit transactions.
* **Payment Service:** Governs the absolute state of a payment transaction using a defined State Machine.
* **Fraud Service:** Performs real-time risk scoring and compliance checks to detect and flag fraudulent transactions.
* **Notification Service:** Dispatches transactional updates and receipts to users via Email and Webhooks.

#### 4. Event Bus & Messaging Layer (Apache Kafka)
* **Responsibilities:** Facilitates asynchronous, decoupled communication between microservices using event-driven streams.
* **Key Events Transmitted:** `payment.initiated` $\rightarrow$ `payment.completed` $\rightarrow$ `payment.failed`

#### 5. Data & Observability Layer
* **PostgreSQL:** Serves as the primary relational database storing core transactional data and account state records.
* **Redis:** A high-performance in-memory key-value store utilized for storing and validating Idempotency keys.
* **Prometheus + Grafana:** Provides comprehensive system observability, capturing core application metrics and exposing real-time monitoring dashboards.

### Services

| Service | Port | Responsibility |
|---|---|---|
| api-gateway | 8080 | Auth, idempotency, routing |
| payment-service | 8081 | State machine, saga orchestration |
| account-service | 8082 | Debit, credit, compensation |
| fraud-service | 8083 | Pluggable signal scoring |
| notification-service | 8084 | Email + webhook delivery |


## Key Engineering Decisions

**Idempotency** — Every POST request requires a client-generated `Idempotency-Key`
header. The gateway checks this key against Redis before forwarding. Duplicate requests
return the cached response without touching downstream services. Only successful
responses are cached — errors are retryable.

**Saga pattern** — Payment processing uses orchestration-based sagas. The
`PaymentSagaOrchestrator` drives each step sequentially and runs compensating
transactions in reverse on any failure. Every step is persisted to `saga_steps`
for full auditability.

**Optimistic locking** — Account debits use JPA `@Version` fields to prevent
concurrent overdrafts. A `@Retryable` annotation retries on version conflicts with
100ms exponential backoff.

**Payment state machine** — `PaymentStatus` enum defines legal transitions via
abstract `nextStates()` methods. The entity exposes only `transitionTo()` — there
is no setter — making illegal state changes structurally impossible.

**Pluggable fraud signals** — Each fraud check implements `FraudSignalCheck`
(Strategy pattern). Spring auto-collects all implementations into the scorer.
Adding a new signal requires one new class with zero changes to existing code.

**`NUMERIC(19,4)` for money** — Never `double` or `float`. Floating-point
arithmetic loses cents. This is enforced at both the Java (`BigDecimal`) and
PostgreSQL (`NUMERIC`) layers.

## Tech Stack

Java 17 · Spring Boot 3.3.4 · Apache Kafka · Redis (Redisson) ·
PostgreSQL 16 · Docker · Prometheus · Grafana · Mailhog


## Testing a payment

```bash
curl -X POST http://localhost:8080/payments \
  -H "X-API-Key: test-key-merchant-001" \
  -H "Idempotency-Key: pay-$(date +%s)" \
  -H "Content-Type: application/json" \
  -d '{
    "senderId":   "11111111-1111-1111-1111-111111111111",
    "receiverId": "22222222-2222-2222-2222-222222222222",
    "amount": 50.00,
    "currency": "USD"
  }'
```

## Observability

- Prometheus: http://localhost:9090
- Grafana: http://localhost:3000 (admin/admin)
- Mailhog: http://localhost:8025

## What I learned building this

- Distributed sagas solve partial failures that two-phase commit cannot handle
  at scale — but they require careful compensating transaction design
- Idempotency is not just a database unique constraint — it requires careful
  decisions about what to cache, when, and for how long
- Optimistic locking is almost always preferable to pessimistic for concurrent
  writes, except for high-value operations where retry cost is too high
