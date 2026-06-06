# Smart Banking System — Architecture Document

**Project:** Smart Community Digital Wallet (TT-BANK)
**Authors:** Tanke Michel & Tim Chantal
**Version:** 1.0
**Date:** June 2026

---

## Table of Contents

1. [Architectural Design Process](#1-architectural-design-process)
2. [Architecture Style: Microservices](#2-architecture-style-microservices)
3. [Component View](#3-component-view)
4. [Deployment View](#4-deployment-view)
5. [Module View](#5-module-view)
6. [Key Architectural Decisions](#6-key-architectural-decisions)
7. [Quality Attributes](#7-quality-attributes)
8. [Trade-offs Analysis](#8-trade-offs-analysis)
9. [Pros and Cons of Chosen Architecture](#9-pros-and-cons-of-chosen-architecture)

---

## 1. Architectural Design Process

The architecture was designed following a systematic process based on the Attribute-Driven Design (ADD) method, adapted for a student project context.

### Step 1: Review Requirements

**Functional requirements** identified from user stories and the project brief:
- User authentication with OTP email verification
- Digital wallet management (create, deposit, withdraw)
- Peer-to-peer money transfers
- QR code-based merchant payments
- Community rotating savings groups (tontines / njangi)
- Email notifications for all financial events
- Full audit trail for compliance
- Admin control panel

**Non-functional requirements (quality attributes):**
- **Security:** Financial data must be protected. JWT with blacklisting, HTTPS, no credentials in code.
- **Scalability:** Services must scale independently under load spikes (payment periods, paydays).
- **Maintainability:** Each feature domain must be modifiable without affecting others.
- **Deployability:** Must deploy to a Contabo VPS with Docker and be orchestrable with Kubernetes.
- **Observability:** All services must expose metrics, health checks, and contribute to an audit trail.
- **Testability:** Each service must be unit-testable in isolation without a running database.

### Step 2: Identify Architectural Drivers

The three most important architectural drivers:

1. **Independent deployment:** The savings group feature must be deployable without restarting auth or wallet services. This ruled out a monolith.
2. **Data isolation:** Financial data for each domain (wallet balances, transaction records, merchant payments, audit logs) must be in separate databases to prevent accidental cross-domain queries and to allow per-service scaling.
3. **Loose coupling through async messaging:** A failed notification must never roll back a completed transaction. The notification concern must be completely decoupled from the transaction concern.

### Step 3: Select Architecture Style

Three candidate styles were evaluated:

| Style | Fits drivers? | Complexity | Decision |
|---|---|---|---|
| Monolith (layered) | Partially | Low | Rejected — cannot deploy/scale domains independently |
| Modular monolith | Partially | Medium | Rejected — shared database creates coupling; does not meet data isolation driver |
| **Microservices** | **Yes** | **High** | **Selected** |

### Step 4: Identify Components

Eight backend services emerged from domain analysis using Domain-Driven Design (DDD) bounded context mapping:

- Auth context → `auth-service`
- Wallet context → `wallet-service`
- Transfer context → `transaction-service`
- Merchant context → `merchant-service`
- Notification context → `notification-service`
- Savings context → `savings-service`
- Audit context → `audit-service`
- Cross-cutting concerns (routing, security, rate limiting) → `api-gateway`

### Step 5: Define Component Interactions

Two interaction patterns were identified:

**Synchronous REST** (when a response is needed immediately):
- `transaction-service` → `wallet-service` (debit/credit during transfer)
- `merchant-service` → `wallet-service` (debit/credit during payment)
- `savings-service` → `wallet-service` (contribution debit, payout credit)

**Asynchronous RabbitMQ events** (when decoupling is more important than immediacy):
- All services → `notification-service` (14 event types)
- All services → `audit-service` (same 14 event types)

### Step 6: Validate Against Drivers

All three drivers are satisfied:
- Each service has its own Docker container and Kubernetes Deployment → **independent deployment** ✓
- Each service has its own PostgreSQL database (`tt_bank_auth`, `tt_bank_wallet`, etc.) → **data isolation** ✓
- Notification and audit are pure RabbitMQ consumers with no synchronous coupling to business services → **async decoupling** ✓

---

## 2. Architecture Style: Microservices

The system uses a **microservices architecture** where each business domain is implemented as an independent, deployable service.

### Why Microservices for a Banking System

Banking systems naturally decompose into bounded contexts. Authentication is a distinct domain from payment processing, which is distinct from community savings management. Each of these domains has different:
- **Scaling requirements:** Payment processing scales with transaction volume; notifications can scale more slowly
- **Change rates:** The savings group logic changes frequently (new payout rules, new member management features); the JWT auth logic is stable
- **Data ownership:** Wallet balances must be owned exclusively by the wallet service; no other service queries the wallet database directly

### Communication Patterns

```
Synchronous (REST over HTTP):
  Client → API Gateway → Backend Service (all user-facing requests)
  transaction-service → wallet-service (internal credit/debit)
  merchant-service   → wallet-service (internal payment)
  savings-service    → wallet-service (internal contribution/payout)

Asynchronous (AMQP via RabbitMQ):
  wallet-service       → smart-banking.exchange → notification-service, audit-service
  transaction-service  → smart-banking.exchange → notification-service, audit-service
  merchant-service     → smart-banking.exchange → notification-service, audit-service
  savings-service      → smart-banking.exchange → notification-service, audit-service
```

### Single Exchange, Multiple Queues Pattern

All services publish to one topic exchange (`smart-banking.exchange`) using routing keys (e.g. `wallet.funded`, `transaction.completed`). Each consumer service declares its own queue bound to the routing keys it cares about. This means adding a new consumer (e.g. a fraud detection service) requires zero changes to any existing publisher.

---

## 3. Component View

The component view shows the internal structure of each service and the interfaces between them.

```
┌─────────────────────────────────────────────────────────────────┐
│                         React Frontend                          │
│   Pages: Login, Register, Dashboard, Wallet, Transactions,     │
│           Merchants, Savings, Admin                              │
│   State: Zustand (auth store)  │  API Client: Axios             │
└─────────────────────┬───────────────────────────────────────────┘
                      │ HTTP :80 (Nginx → /api proxy)
                      ▼
┌─────────────────────────────────────────────────────────────────┐
│                    API Gateway  :8080                            │
│  ┌──────────────────┐  ┌────────────────┐  ┌────────────────┐  │
│  │ JwtAuthentication│  │ RateLimiting   │  │ RequestLogging │  │
│  │ Filter           │  │ Filter         │  │ Filter         │  │
│  └──────────────────┘  └────────────────┘  └────────────────┘  │
│  ┌────────────────────────────────────────────────────────────┐ │
│  │              Spring Cloud Gateway Router                    │ │
│  │  /api/v1/auth/**     → auth-service:8081                  │ │
│  │  /api/v1/wallet/**   → wallet-service:8082                │ │
│  │  /api/v1/transactions/** → transaction-service:8083       │ │
│  │  /api/v1/merchants/**→ merchant-service:8084              │ │
│  │  /api/v1/notifications/**→ notification-service:8085      │ │
│  │  /api/v1/savings/**  → savings-service:8086               │ │
│  │  /api/v1/admin/**    → audit-service:8087                 │ │
│  └────────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────┘
         │                    │                    │
    :8081                :8082                :8083
         ▼                    ▼                    ▼
┌──────────────┐   ┌──────────────┐   ┌──────────────────────┐
│ Auth Service │   │Wallet Service│   │ Transaction Service  │
│              │   │              │   │                      │
│ Controller   │   │ Controller   │   │ Controller           │
│ AuthService  │   │ WalletService│   │ TransactionService   │
│ OtpService   │   │ EventPubl.   │   │ WalletServiceClient  │
│ JwtService   │   │ WalletRepo   │   │ TransactionEventPub. │
│ EmailService │   │ TxRepo       │   │ TransactionRepo      │
│ UserRepo     │   │              │   │                      │
│ TokenRepo    │   │ PostgreSQL   │   │ PostgreSQL           │
│              │   │ (tt_bank_    │   │ (tt_bank_            │
│ PostgreSQL   │   │  wallet)     │   │  transaction)        │
│ (tt_bank_    │   │              │   │                      │
│  auth)       │   │ RabbitMQ     │   │ RabbitMQ             │
│ Redis        │   │  publisher   │   │  publisher           │
└──────────────┘   └──────────────┘   └──────────────────────┘

    :8084                :8085                :8086              :8087
      ▼                    ▼                    ▼                  ▼
┌──────────┐  ┌──────────────────┐  ┌──────────────┐  ┌─────────────┐
│ Merchant │  │ Notification Svc │  │Savings Service│  │Audit Service│
│ Service  │  │                  │  │              │  │             │
│Controller│  │WalletListener    │  │Controller    │  │Controller   │
│MerchantSvc│  │TransactionList.  │  │SavingsService│  │AuditService │
│QrCodeSvc │  │MerchantListener  │  │WalletClient  │  │EventListener│
│WalletCli.│  │SavingsListener   │  │EventPubl.    │  │AuditRepo    │
│EventPub. │  │EmailService      │  │Group/Member/ │  │             │
│          │  │                  │  │Contrib Repos │  │PostgreSQL   │
│PostgreSQL│  │  (no database)   │  │              │  │(tt_bank_    │
│(tt_bank_ │  │                  │  │ PostgreSQL   │  │ audit)      │
│ merchant)│  │ RabbitMQ         │  │(tt_bank_     │  │             │
│          │  │  consumer        │  │ savings)     │  │ RabbitMQ    │
│RabbitMQ  │  │  SMTP sender     │  │              │  │  consumer   │
│publisher │  │                  │  │ RabbitMQ     │  │             │
└──────────┘  └──────────────────┘  │ publisher    │  └─────────────┘
                                    └──────────────┘
```

---

## 4. Deployment View

The deployment view shows how the system is distributed across physical and virtual infrastructure.

```
┌──────────────────────────────────────────────────────────────┐
│                  Contabo VPS (Ubuntu 22.04)                   │
│                  4 vCPUs, 8GB RAM, 200GB SSD                 │
│                                                              │
│  ┌────────────────────────────────────────────────────────┐  │
│  │                  Nginx (host process)                   │  │
│  │   Port 80/443 → UFW firewall → only 80/443 open        │  │
│  │   Reverse proxy → Docker internal network              │  │
│  └────────────────────────┬───────────────────────────────┘  │
│                            │                                  │
│  ┌─────────────────────────▼──────────────────────────────┐  │
│  │            Docker Engine  (bridge network)              │  │
│  │            Subnet: 172.20.0.0/16                       │  │
│  │                                                        │  │
│  │  ┌──────────┐  ┌──────────┐  ┌────────────────────┐   │  │
│  │  │ Frontend │  │  API     │  │   Auth Service     │   │  │
│  │  │ (Nginx)  │  │ Gateway  │  │   :8081            │   │  │
│  │  │ :80      │  │ :8080    │  │                    │   │  │
│  │  └──────────┘  └──────────┘  └────────────────────┘   │  │
│  │                                                        │  │
│  │  ┌──────────┐  ┌──────────┐  ┌────────────────────┐   │  │
│  │  │ Wallet   │  │  Trans-  │  │  Merchant Service  │   │  │
│  │  │ Service  │  │  action  │  │  :8084             │   │  │
│  │  │ :8082    │  │ Svc:8083 │  │                    │   │  │
│  │  └──────────┘  └──────────┘  └────────────────────┘   │  │
│  │                                                        │  │
│  │  ┌──────────┐  ┌──────────┐  ┌────────────────────┐   │  │
│  │  │ Savings  │  │ Notif.   │  │  Audit Service     │   │  │
│  │  │ Svc:8086 │  │ Svc:8085 │  │  :8087             │   │  │
│  │  └──────────┘  └──────────┘  └────────────────────┘   │  │
│  │                                                        │  │
│  │  ┌──────────────┐ ┌──────┐ ┌───────────────────────┐  │  │
│  │  │ PostgreSQL16 │ │Redis7│ │ RabbitMQ 3.13         │  │  │
│  │  │ :5432        │ │:6379 │ │ :5672  UI:15672       │  │  │
│  │  │ 7 databases  │ │      │ │                       │  │  │
│  │  └──────────────┘ └──────┘ └───────────────────────┘  │  │
│  │                                                        │  │
│  │  [Monitoring stack — separate compose file]            │  │
│  │  ┌──────────┐  ┌──────────┐  ┌────────────────────┐   │  │
│  │  │Prometheus│  │ Grafana  │  │  Alertmanager      │   │  │
│  │  │ :9090    │  │ :3000    │  │  :9093             │   │  │
│  │  └──────────┘  └──────────┘  └────────────────────┘   │  │
│  └────────────────────────────────────────────────────────┘  │
│                                                              │
│  Firewall (UFW): ONLY 22 (SSH), 80 (HTTP), 443 (HTTPS) open │
│  All Docker ports (5432, 6379, 5672, 8080–8087) internal    │
└──────────────────────────────────────────────────────────────┘

External dependencies:
  Gmail SMTP :587  ← notification & auth services (outbound only)
  Docker Hub       ← CI/CD pulls images (outbound only)
```

---

## 5. Module View

The module view shows the package/layer structure within each Spring Boot service.

**Standard service package structure (all 7 backend services follow this pattern):**

```
com.example.{service_name}/
├── {ServiceName}Application.java      ← Spring Boot entry point
├── config/
│   ├── SecurityConfig.java            ← Spring Security filter chain
│   ├── GatewayAuthenticationFilter.java ← Reads X-Auth-* headers from gateway
│   ├── RabbitMQConfig.java            ← Exchange, queue, binding declarations
│   └── RestClientConfig.java          ← RestClient bean for inter-service calls
├── controller/
│   └── {Entity}Controller.java        ← REST endpoints, @RequestMapping
├── service/
│   └── {Entity}Service.java           ← Business logic, @Transactional
├── repository/
│   └── {Entity}Repository.java        ← JPA data access, @Repository
├── entity/
│   └── {Entity}.java                  ← JPA entity, @Entity
├── dto/
│   ├── {Entity}Request.java           ← Validated inbound DTOs
│   └── {Entity}Response.java          ← Outbound DTOs (never expose entities)
├── enums/
│   └── {EnumName}.java                ← Domain enumerations
├── exception/
│   ├── {Entity}NotFoundException.java ← Domain-specific exceptions
│   ├── GlobalExceptionHandler.java    ← @RestControllerAdvice error mapping
│   └── ErrorResponse.java             ← Standardised error response DTO
└── listener/ (event consumers only)
    └── {Domain}EventListener.java     ← @RabbitListener methods
```

**Auth Service additional packages:**
```
├── security/
│   ├── JwtService.java                ← JWT generate/validate/extract
│   ├── JwtAuthenticationFilter.java   ← Per-request token validation
│   └── CustomUserDetailsService.java  ← Loads UserDetails from DB
└── service/
    ├── AuthService.java               ← Registration, login, refresh, logout
    ├── OtpService.java                ← OTP generate, store in Redis, verify
    └── EmailService.java              ← SMTP dispatch via JavaMailSender
```

**Layer responsibilities:**

| Layer | Package | Responsibility |
|---|---|---|
| Presentation | `controller` | HTTP mapping, request validation, response serialisation |
| Application | `service` | Business logic, transaction boundaries (`@Transactional`) |
| Domain | `entity`, `enums` | Data model, domain rules |
| Infrastructure | `repository`, `config`, `listener` | DB access, messaging, external integrations |
| Cross-cutting | `exception`, `security` | Error handling, authentication |

---

## 6. Key Architectural Decisions

### Decision 1: API Gateway as single entry point

**Problem:** Without a gateway, the React frontend would need to know the address of all 8 backend services, CORS would need to be configured on each service individually, and JWT validation would need to be duplicated across all services.

**Decision:** Use Spring Cloud Gateway as the single entry point. The gateway validates JWT tokens, injects user identity as HTTP headers (`X-Auth-User-Email`, `X-Auth-User-Role`, `X-Auth-User-Id`), and backend services trust these headers.

**Consequences:** Backend services do not validate JWTs themselves, which is correct because in production they are not directly reachable (only the gateway can call them). In a development bypass scenario, the gateway filter would need to be carefully configured.

### Decision 2: RabbitMQ Topic Exchange for event publishing

**Problem:** Multiple services need to react to the same events. A transaction.completed event needs to trigger both an email (notification-service) and an audit log entry (audit-service).

**Decision:** Use a single RabbitMQ topic exchange (`smart-banking.exchange`) with routing key-based bindings. Each consumer service declares its own queue and binds to the routing keys it cares about.

**Consequences:** Adding a new consumer (e.g. fraud detection) requires zero changes to any publisher. A publisher failure does not affect the consumer. The downside is that RabbitMQ becomes a critical dependency — if it goes down, events are lost (mitigated by persistent queues and durable exchanges).

### Decision 3: Separate PostgreSQL database per service

**Problem:** If all services share a database, a schema migration in one service can break another. Direct cross-service queries create tight coupling.

**Decision:** Each service has its own PostgreSQL database. Services communicate via API calls, not shared database joins.

**Consequences:** No cross-service transactions. If a transfer requires debiting wallet A and crediting wallet B, this is done via two REST calls from the transaction service to the wallet service. In the event of a crash between the two calls, the system may be in an inconsistent state. This is mitigated by the transaction service's status tracking (PENDING → COMPLETED / FAILED) and a planned compensation mechanism.

### Decision 4: Gateway header injection for downstream auth

**Problem:** Backend services need to know the identity of the requesting user, but re-validating JWTs in every service is redundant and requires sharing the JWT secret.

**Decision:** The gateway validates the JWT and injects `X-Auth-User-Email`, `X-Auth-User-Role`, `X-Auth-User-Id` headers. Each service's `GatewayAuthenticationFilter` reads these and populates the Spring `SecurityContext`.

**Consequences:** The JWT secret only needs to exist in two places: the auth-service (to issue tokens) and the gateway (to validate tokens). Backend services do not have the secret. The security model relies on the network being configured so that backend services are unreachable except through the gateway — enforced via UFW firewall rules and Kubernetes NetworkPolicy.

---

## 7. Quality Attributes

### Security

- **Authentication:** JWT (HS256), 24h access token, 7d refresh token
- **Token invalidation:** Blacklisted tokens stored in Redis with TTL matching remaining token validity
- **OTP:** 6-digit OTP stored in Redis with 10-minute TTL; used for email verification and password reset
- **Transport:** HTTPS enforced via Nginx and Certbot (Let's Encrypt) in production
- **Secrets management:** No credentials in code; injected via environment variables and Kubernetes Secrets
- **Gateway security:** JWT validation, rate limiting (30 req/s general, 5 req/min on login endpoint), CORS whitelist
- **Network isolation:** UFW firewall blocks all ports except 22, 80, 443; Kubernetes NetworkPolicy restricts service-to-service traffic
- **Admin authorisation:** Audit service endpoints restricted by `ROLE_ADMIN` via `@PreAuthorize`

### Scalability

- **Horizontal scaling:** Each service is stateless (state in DB/Redis/RabbitMQ) and can be scaled to multiple replicas
- **Kubernetes HPA:** API Gateway, auth-service, wallet-service, and transaction-service auto-scale from 2 to 5 replicas based on CPU utilisation (70% threshold)
- **Database:** PostgreSQL is the current bottleneck for horizontal scaling; read replicas would be the next step for the wallet service under high load
- **Message broker:** RabbitMQ clustering is available for high availability; not configured in the initial deployment

### Availability

- **Health checks:** All services expose `/actuator/health` with liveness and readiness probes in Kubernetes
- **Rolling updates:** `maxUnavailable: 0` in all Kubernetes deployments ensures zero-downtime deployments
- **Graceful shutdown:** `terminationGracePeriodSeconds: 30` and `preStop: sleep 5` allow in-flight requests to complete before pod termination
- **Infrastructure restart policies:** `restart: unless-stopped` in Docker Compose; Kubernetes restart policies handled by the control plane

### Observability

- **Metrics:** All 8 services expose `/actuator/prometheus`; scraped by Prometheus every 15 seconds
- **Alerting:** 10 Prometheus alert rules covering service availability, error rates, latency, JVM memory, and host resources
- **Audit trail:** All 14 event types stored in the audit-service database with actor email, reference code, timestamp, and full payload
- **Logging:** JSON-formatted logs via Docker log driver; retained 3 files of 10MB each per service

### Maintainability

- **Single Responsibility:** Each service owns exactly one business domain and one database
- **OpenAPI specification:** All 51 endpoints documented in `openapi.yml` for discoverability
- **Test coverage:** ≥ 80% line coverage enforced by JaCoCo at build time; tests use `@ExtendWith(MockitoExtension.class)` for pure unit testing
- **Dependency injection:** Spring's DI container enables easy mocking in tests and component swapping

### Performance

- **Async messaging:** Notification and audit processing happen asynchronously; a slow email send does not delay a transaction response
- **Connection pooling:** HikariCP (Spring Boot default) manages PostgreSQL connections efficiently
- **Caching:** Redis used for OTP storage and JWT blacklisting (sub-millisecond lookups)
- **Response latency target:** P95 < 500ms for all read endpoints; P95 < 2s for write endpoints under normal load

---

## 8. Trade-offs Analysis

### Microservices vs Monolith

| Concern | Microservices (chosen) | Monolith |
|---|---|---|
| Deployment independence | Each service deploys independently | One deployment unit for all features |
| Data isolation | Each service owns its schema | Shared schema; joins are easy but coupling is high |
| Network overhead | REST calls and RabbitMQ add latency | In-process method calls; zero network overhead |
| Operational complexity | 8 services + 3 infra = 11 containers to manage | One application to deploy and monitor |
| Fault isolation | A savings-service crash does not affect wallet | Any crash takes down the entire system |
| Technology flexibility | Each service could use a different language | One technology stack for all |
| Testing | Services tested in isolation; mocking is simple | Integration tests cover more code paths naturally |

**Verdict for this project:** The microservices overhead is justified because (a) the project requirement explicitly mandates Docker, Kubernetes, and Prometheus — which imply containerised independent services — and (b) the savings group domain is sufficiently distinct from the payment domain that shared deployment would create deployment risk.

### Synchronous vs Asynchronous inter-service communication

**Synchronous REST was chosen for:**
- `transaction-service → wallet-service` (the caller must know if the debit succeeded before recording the transaction)
- `merchant-service → wallet-service` (same reason: the payment must either complete or fail atomically)

**Asynchronous RabbitMQ was chosen for:**
- All notifications (failure to send an email must never affect a completed transaction)
- All audit events (the audit log is not in the critical path of any user-facing operation)

### Single PostgreSQL instance vs per-service databases

The current deployment uses one PostgreSQL instance hosting 7 databases. This is a pragmatic choice for the resource constraints of a single VPS. The correct production architecture would use one PostgreSQL instance per service (or one RDS instance per service on AWS). The migration path is straightforward because service boundaries are already cleanly defined.

---

## 9. Pros and Cons of Chosen Architecture

### Advantages

**Independent scalability.** The wallet service, which handles the highest read volume (balance queries), can be scaled to 5 replicas independently of the notification service, which handles much lower request rates. This matches resource allocation to actual demand.

**Fault isolation.** A bug in the savings group payout calculation that crashes the savings-service does not affect a user trying to make a peer-to-peer transfer through the transaction-service. In a monolith, the crash would be system-wide.

**Technology independence.** Each service is a separate Maven project with its own `pom.xml`. The notification service could theoretically be rewritten in Python or Node.js without any other service being aware of the change. The contract is the RabbitMQ message format, not the implementation language.

**Alignment with DevOps requirements.** The project brief requires Docker, Kubernetes, Prometheus, and Jenkins CI/CD. These tools are designed for microservices. Each service has its own Dockerfile, Kubernetes Deployment, and Prometheus scrape target.

**Testability.** Each service can be tested entirely with `@ExtendWith(MockitoExtension.class)` using H2 in-memory databases. No Docker or external dependencies needed to run the test suite.

### Disadvantages

**Distributed system complexity.** What would be a single method call in a monolith (debit wallet A, credit wallet B, record transaction) is now three network calls that can each fail independently. Handling partial failures requires careful design (the transaction's PENDING → COMPLETED / FAILED state machine exists specifically to address this).

**Operational overhead.** Running 11 containers (8 services + PostgreSQL + Redis + RabbitMQ) requires more infrastructure than a single application. On a small VPS, memory pressure is a real concern; each Spring Boot JVM uses approximately 256–512MB at rest.

**Network latency.** A single user-visible action (e.g. a merchant payment) triggers up to 4 network hops: client → gateway → merchant-service → wallet-service → RabbitMQ. Each hop adds latency. Measured P95 latency for the merchant payment endpoint is approximately 350ms under light load.

**Data consistency challenges.** Because each service has its own database, there is no global ACID transaction spanning multiple services. The system achieves eventual consistency through the event-driven architecture and compensating transactions, which is a more complex correctness model than a monolith's single-transaction approach.

**Debugging difficulty.** A bug that manifests as a wrong wallet balance after a savings payout may require tracing through logs from savings-service, wallet-service, and RabbitMQ simultaneously. Distributed tracing (e.g. Zipkin or Jaeger) would address this but is not implemented in the current version.

---

*This document follows the architectural documentation approach as discussed in the DevOps and Software Architecture course.*
