# Smart Banking System — TT-BANK

A production-grade **microservices banking platform** built with Java 21 / Spring Boot 3.3, React 19, Docker, and Kubernetes. Designed for the Central African market with XAF (CFA franc) as the primary currency.

---

## Table of Contents

1. [Architecture Overview](#architecture-overview)
2. [Technology Stack](#technology-stack)
3. [Prerequisites](#prerequisites)
4. [Quick Start — Local Development](#quick-start--local-development)
5. [Environment Variables Reference](#environment-variables-reference)
6. [Service Ports Reference](#service-ports-reference)
7. [API Documentation](#api-documentation)
8. [Running Tests](#running-tests)
9. [Docker Deployment (Contabo VPS)](#docker-deployment-contabo-vps)
10. [Kubernetes Deployment](#kubernetes-deployment)
11. [CI/CD Pipeline (Jenkins)](#cicd-pipeline-jenkins)
12. [Monitoring (Prometheus + Grafana)](#monitoring-prometheus--grafana)
13. [Infrastructure as Code (Ansible)](#infrastructure-as-code-ansible)
14. [Project Structure](#project-structure)
15. [Contributing](#contributing)

---

## Architecture Overview

The system follows a **microservices architecture** with 8 independent Spring Boot services, all accessed through a single API Gateway.

```
Internet
    │
    ▼
Nginx (host, port 80/443)
    │
    ▼
API Gateway :8080  ──── JWT validation, rate limiting, CORS
    │
    ├── Auth Service         :8081  ── PostgreSQL (tt_bank_auth),   Redis
    ├── Wallet Service       :8082  ── PostgreSQL (tt_bank_wallet),  RabbitMQ
    ├── Transaction Service  :8083  ── PostgreSQL (tt_bank_transaction), RabbitMQ
    ├── Merchant Service     :8084  ── PostgreSQL (tt_bank_merchant), RabbitMQ
    ├── Notification Service :8085  ── RabbitMQ (consumer only, no DB)
    ├── Savings Service      :8086  ── PostgreSQL (tt_bank_savings),  RabbitMQ
    └── Audit Service        :8087  ── PostgreSQL (tt_bank_audit),    RabbitMQ

Infrastructure:
    PostgreSQL 16   :5432  ── 7 isolated databases (one per service)
    Redis 7         :6379  ── JWT blacklist, OTP cache
    RabbitMQ 3.13   :5672  ── Async event bus (14 event types)
```

**Async event flow:** Each service publishes domain events to RabbitMQ. The Notification Service consumes all events and sends transactional emails. The Audit Service consumes all events and writes a full immutable audit log.

---

## Technology Stack

| Layer | Technology |
|---|---|
| Backend | Java 21, Spring Boot 3.3.5, Spring Cloud Gateway |
| Frontend | React 19, TypeScript, Vite 8, Zustand, Axios |
| Database | PostgreSQL 16 |
| Cache / Session | Redis 7 |
| Message broker | RabbitMQ 3.13 |
| Authentication | JWT (HS256), OTP via SMTP |
| Containerisation | Docker, Docker Compose |
| Orchestration | Kubernetes (manifests in `infrastructure/kubernetes/`) |
| CI/CD | Jenkins (Jenkinsfile in `infrastructure/jenkins/`) |
| Monitoring | Prometheus 2.51, Grafana 10.4, Alertmanager |
| IaC | Ansible 3 playbooks |
| Reverse proxy | Nginx |
| Build tool | Maven 3.9 (backend), npm (frontend) |

---

## Prerequisites

### For local development
- **Java 21** — `java -version` should show 21
- **Maven 3.9+** — included via `./mvnw` wrapper (no separate install needed)
- **Node.js 22+** and npm
- **Docker Desktop** (or Docker Engine + Docker Compose plugin)

### For VPS deployment
- Contabo VPS running Ubuntu 22.04 LTS
- SSH access with a key pair
- A domain name pointed at your VPS IP (for HTTPS via Certbot)

---

## Quick Start — Local Development

### 1. Clone the repository

```bash
git clone https://github.com/YOUR_USERNAME/TT-BANK.git
cd TT-BANK/Smart-Banking-System
```

### 2. Configure environment variables

```bash
cp infrastructure/docker/.env.example infrastructure/docker/.env
```

Open `infrastructure/docker/.env` and fill in:
- `POSTGRES_PASSWORD` — any strong password
- `REDIS_PASSWORD` — any strong password
- `RABBITMQ_PASSWORD` — any strong password
- `JWT_SECRET` — generate with `openssl rand -hex 32`
- `SMTP_USERNAME` / `SMTP_PASSWORD` — your Gmail address and [App Password](https://myaccount.google.com/apppasswords)

### 3. Add the Docker profile override to each service

Copy the `application-docker.properties` file from `infrastructure/docker/spring-profiles/` into each service's `src/main/resources/` directory:

```bash
# From Smart-Banking-System/
for svc in auth-service wallet-service transaction-service merchant-service notification-service savings-service audit-service; do
  cp infrastructure/docker/spring-profiles/${svc}-application-docker.properties \
     services/${svc}/src/main/resources/application-docker.properties
done

# API Gateway uses YAML
cp infrastructure/docker/spring-profiles/api-gateway-application-docker.yml \
   services/api-gateway/src/main/resources/application-docker.yml
```

### 4. Add Micrometer Prometheus to each service's pom.xml

Add the following dependency inside `<dependencies>` in every service's `pom.xml`:

```xml
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
```

Then ensure each `application.properties` includes:

```properties
management.endpoints.web.exposure.include=health,info,prometheus
management.endpoint.health.show-details=always
```

### 5. Start infrastructure services

```bash
cd infrastructure/docker
docker compose up -d postgres redis rabbitmq
```

Wait for health checks to pass (~30 seconds):

```bash
docker compose ps
```

All three should show `healthy`.

### 6. Start backend services

Option A — run individually (for active development):

```bash
# Terminal 1
cd services/auth-service && ./mvnw spring-boot:run

# Terminal 2
cd services/wallet-service && ./mvnw spring-boot:run

# (repeat for each service you need)
```

Option B — build and run all via Docker Compose:

```bash
cd infrastructure/docker
docker compose up -d --build
```

### 7. Start the frontend

```bash
cd frontend
npm install
npm run dev
```

The React app is available at **http://localhost:5173**.

---

## Environment Variables Reference

All variables are documented in `infrastructure/docker/.env.example`.

| Variable | Description | Example |
|---|---|---|
| `POSTGRES_PASSWORD` | PostgreSQL admin password | `StrongPass123!` |
| `REDIS_PASSWORD` | Redis password | `RedisPass456!` |
| `RABBITMQ_PASSWORD` | RabbitMQ admin password | `RabbitPass789!` |
| `JWT_SECRET` | HS256 signing key (must be same for gateway and auth-service) | `openssl rand -hex 32` |
| `JWT_ACCESS_EXPIRY` | Access token TTL (ms) | `86400000` (24h) |
| `JWT_REFRESH_EXPIRY` | Refresh token TTL (ms) | `604800000` (7d) |
| `SMTP_HOST` | SMTP server hostname | `smtp.gmail.com` |
| `SMTP_PORT` | SMTP server port | `587` |
| `SMTP_USERNAME` | SMTP username (email address) | `you@gmail.com` |
| `SMTP_PASSWORD` | SMTP password (Gmail App Password) | `xxxx xxxx xxxx xxxx` |
| `APP_BASE_URL` | Base URL for email links | `https://yourdomain.com` |
| `VITE_API_URL` | API base path for frontend | `/api` |

---

## Service Ports Reference

| Service | Port | Notes |
|---|---|---|
| Frontend (Nginx) | 80 | React SPA |
| API Gateway | 8080 | Single entry point — use this for all API calls |
| Auth Service | 8081 | Direct access for debugging only |
| Wallet Service | 8082 | Direct access for debugging only |
| Transaction Service | 8083 | Direct access for debugging only |
| Merchant Service | 8084 | Direct access for debugging only |
| Notification Service | 8085 | Direct access for debugging only |
| Savings Service | 8086 | Direct access for debugging only |
| Audit Service | 8087 | Direct access for debugging only |
| PostgreSQL | 5432 | |
| Redis | 6379 | |
| RabbitMQ AMQP | 5672 | |
| RabbitMQ Management UI | 15672 | admin / (your RABBITMQ_PASSWORD) |
| Prometheus | 9090 | Monitoring stack only |
| Grafana | 3000 | Monitoring stack only |

> **Security note:** In production, only ports 80 and 443 should be exposed publicly through the firewall. All other ports are accessible only within the Docker/Kubernetes internal network.

---

## API Documentation

The complete OpenAPI 3.0 specification is in `infrastructure/swagger/openapi.yml`.

### View in Swagger UI locally

```bash
docker run -p 8090:8080 \
  -e SWAGGER_JSON=/api/openapi.yml \
  -v $(pwd)/infrastructure/swagger:/api \
  swaggerapi/swagger-ui
```

Then open **http://localhost:8090**.

### Authentication in Swagger UI

1. Call `POST /api/v1/auth/login` with your credentials
2. Copy the `accessToken` from the response
3. Click **Authorize** (top right) and paste: `Bearer <your-token>`

All subsequent requests in Swagger UI will include the token automatically.

---

## Running Tests

### Run all tests for a single service

```bash
cd services/auth-service
./mvnw test
```

### Run all tests across all services

```bash
# From Smart-Banking-System/
for svc in api-gateway auth-service wallet-service transaction-service merchant-service notification-service savings-service audit-service; do
  echo "=== Testing $svc ==="
  (cd services/$svc && ./mvnw test -q)
done
```

### Generate JaCoCo code coverage report

```bash
cd services/auth-service
./mvnw verify org.jacoco:jacoco-maven-plugin:0.8.11:report
open target/site/jacoco/index.html
```

### Run frontend linting

```bash
cd frontend
npm run lint
```

---

## Docker Deployment (Contabo VPS)

### First-time VPS setup (run once)

```bash
# From your local machine:
ansible-playbook \
  -i infrastructure/ansible/inventory.yml \
  infrastructure/ansible/playbooks/01-setup-vps.yml
```

This installs Docker, configures UFW firewall, creates the `ttbank` app user, and installs Nginx.

### Deploy the application

```bash
ansible-playbook \
  -i infrastructure/ansible/inventory.yml \
  infrastructure/ansible/playbooks/02-deploy-app.yml \
  --extra-vars "postgres_password=YOUR_PW redis_password=YOUR_PW rabbitmq_password=YOUR_PW jwt_secret=YOUR_SECRET smtp_username=YOUR_EMAIL smtp_password=YOUR_APP_PW"
```

### Manual deployment (if Ansible is not available)

```bash
# SSH into VPS
ssh ubuntu@YOUR_VPS_IP

# Clone or pull latest code
git clone https://github.com/YOUR_USERNAME/TT-BANK.git /opt/tt-bank
cd /opt/tt-bank/Smart-Banking-System/infrastructure/docker

# Create .env
cp .env.example .env
nano .env   # fill in all values

# Build and start everything
docker compose up -d --build

# Check status
docker compose ps
docker compose logs -f api-gateway
```

### Enable HTTPS with Certbot

```bash
sudo apt install certbot python3-certbot-nginx
sudo certbot --nginx -d yourdomain.com
```

Certbot auto-renews certificates via a cron job. No manual intervention needed.

---

## Kubernetes Deployment

### Apply all manifests

```bash
# From Smart-Banking-System/
kubectl apply -f infrastructure/kubernetes/base/00-namespace.yml
kubectl apply -f infrastructure/kubernetes/base/01-configmap.yml

# Edit secrets first — replace placeholder base64 values with real ones
# See infrastructure/kubernetes/base/02-secrets.yml for instructions
kubectl apply -f infrastructure/kubernetes/base/02-secrets.yml

kubectl apply -f infrastructure/kubernetes/base/03-pvcs.yml
kubectl apply -f infrastructure/kubernetes/base/04-infrastructure.yml

# Wait for infrastructure to be ready before deploying services
kubectl wait --for=condition=ready pod -l app=postgres -n tt-bank --timeout=120s
kubectl wait --for=condition=ready pod -l app=redis    -n tt-bank --timeout=60s
kubectl wait --for=condition=ready pod -l app=rabbitmq -n tt-bank --timeout=120s

kubectl apply -f infrastructure/kubernetes/base/05-auth-service.yml
kubectl apply -f infrastructure/kubernetes/base/06-backend-services-1.yml
kubectl apply -f infrastructure/kubernetes/base/07-backend-services-2.yml
kubectl apply -f infrastructure/kubernetes/base/08-gateway-frontend.yml
kubectl apply -f infrastructure/kubernetes/base/09-ingress-hpa-netpol.yml
```

### Verify all pods are running

```bash
kubectl get pods -n tt-bank
kubectl get services -n tt-bank
kubectl get ingress -n tt-bank
```

### Rolling update (new image version)

```bash
kubectl set image deployment/auth-service \
  auth-service=ttbank/auth-service:v1.1.0 \
  -n tt-bank

# Watch the rollout
kubectl rollout status deployment/auth-service -n tt-bank

# Roll back if needed
kubectl rollout undo deployment/auth-service -n tt-bank
```

### Scale a service manually

```bash
kubectl scale deployment wallet-service --replicas=3 -n tt-bank
```

HPAs (HorizontalPodAutoscalers) will auto-scale api-gateway, auth-service, wallet-service, and transaction-service based on CPU utilisation (70% threshold).

---

## CI/CD Pipeline (Jenkins)

The `Jenkinsfile` is at `infrastructure/jenkins/Jenkinsfile`.

### Required Jenkins credentials

Configure these in **Jenkins → Manage Jenkins → Credentials**:

| Credential ID | Type | Description |
|---|---|---|
| `GITHUB_CREDENTIALS` | Username with password | GitHub token for checkout |
| `DOCKER_CREDENTIALS` | Username with password | Docker Hub login |
| `VPS_SSH_KEY` | SSH Username with private key | Contabo VPS SSH key |
| `VPS_HOST` | Secret text | VPS IP address |
| `JWT_SECRET` | Secret text | JWT signing key |
| `POSTGRES_PASSWORD` | Secret text | Database password |
| `REDIS_PASSWORD` | Secret text | Redis password |
| `RABBITMQ_PASSWORD` | Secret text | RabbitMQ password |
| `SMTP_CREDENTIALS` | Username with password | Gmail credentials |

### Pipeline stages

| Stage | Description |
|---|---|
| Checkout | Clone repository, record commit metadata |
| Build | Maven package (all 8 services in parallel) |
| Test | Run all unit and integration tests |
| Coverage Check | Enforce 80% minimum coverage via JaCoCo |
| Docker Build | Build all 9 images (8 services + frontend) |
| Docker Push | Push to Docker Hub (main/master branch only) |
| Deploy | SSH to VPS, pull images, rolling `docker compose up -d` |
| Smoke Test | Verify all `/actuator/health` endpoints return UP |

---

## Monitoring (Prometheus + Grafana)

### Start the monitoring stack

```bash
# Alongside the main stack:
docker compose \
  -f infrastructure/docker/docker-compose.yml \
  -f infrastructure/monitoring/docker-compose.monitoring.yml \
  up -d
```

### Access dashboards

| Service | URL | Credentials |
|---|---|---|
| Prometheus | http://localhost:9090 | None |
| Grafana | http://localhost:3000 | admin / admin (change on first login) |
| Alertmanager | http://localhost:9093 | None |

### Key metrics collected

- **Service health:** `up` gauge per service
- **HTTP request rate:** `http_server_requests_seconds_count`
- **HTTP error rate:** 5xx responses as a % of total
- **Response latency:** P50, P95, P99 histograms
- **JVM:** heap usage, GC pauses, thread count
- **Database:** PostgreSQL connections, query duration
- **RabbitMQ:** queue depth, publish rate, consumer rate
- **Host:** CPU, memory, disk — via Node Exporter

### Alerting rules

10 alert rules are configured in `infrastructure/monitoring/alert.rules.yml`:

- Service down (any of the 8 services)
- 5xx error rate > 5%
- P95 latency > 2 seconds
- JVM heap > 85%
- VPS CPU > 85%
- VPS memory > 85%
- VPS disk < 15% free
- PostgreSQL down
- RabbitMQ down
- Auth failure rate > 10/s (brute-force detection)

---

## Infrastructure as Code (Ansible)

Three playbooks in `infrastructure/ansible/playbooks/`:

| Playbook | Purpose |
|---|---|
| `01-setup-vps.yml` | First-time VPS provisioning: Docker, Nginx, UFW, fail2ban, SSH hardening |
| `02-deploy-app.yml` | Deploy or update the application stack with zero downtime |
| `03-setup-monitoring.yml` | Install Prometheus + Grafana monitoring stack |

---

## Project Structure

```
TT-BANK/
├── init-db.sql                         # Creates all 7 databases at first boot
└── Smart-Banking-System/
    ├── frontend/                       # React 19 + TypeScript + Vite
    │   ├── src/
    │   │   ├── api/                    # Axios API clients per service
    │   │   ├── components/             # Shared UI components
    │   │   ├── pages/                  # Route-level page components
    │   │   ├── store/                  # Zustand global state
    │   │   ├── types/                  # TypeScript type definitions
    │   │   └── utils/                  # Formatting helpers
    │   ├── Dockerfile
    │   └── nginx.conf                  # SPA + /api proxy config
    ├── services/
    │   ├── api-gateway/                # Spring Cloud Gateway (port 8080)
    │   ├── auth-service/               # JWT, OTP, email verify (port 8081)
    │   ├── wallet-service/             # Deposits, withdrawals (port 8082)
    │   ├── transaction-service/        # P2P transfers (port 8083)
    │   ├── merchant-service/           # QR payments (port 8084)
    │   ├── notification-service/       # Email sender (port 8085)
    │   ├── savings-service/            # Tontines (port 8086)
    │   └── audit-service/              # Audit log (port 8087)
    └── infrastructure/
        ├── docker/
        │   ├── docker-compose.yml      # Full stack (12 services)
        │   ├── .env.example            # Environment variable template
        │   ├── init-db.sql             # Database initialisation
        │   └── spring-profiles/        # Docker profile overrides per service
        ├── kubernetes/
        │   └── base/                   # 10 Kubernetes YAML manifests
        ├── nginx/
        │   └── smart-banking.conf      # VPS host reverse proxy config
        ├── jenkins/
        │   └── Jenkinsfile             # 9-stage CI/CD pipeline
        ├── ansible/
        │   ├── inventory.yml           # Target hosts
        │   └── playbooks/              # 3 Ansible playbooks
        ├── monitoring/
        │   ├── prometheus.yml          # Scrape config (9 targets)
        │   ├── alert.rules.yml         # 10 alerting rules
        │   ├── alertmanager.yml        # Alert routing to email
        │   └── docker-compose.monitoring.yml
        └── swagger/
            └── openapi.yml             # Full OpenAPI 3.0 specification
```

---

## Contributing

1. **Fork** the repository and create a feature branch: `git checkout -b feature/your-feature`
2. **Follow the code style** — the project uses Lombok; no boilerplate getters/setters by hand
3. **Write tests** — all new service methods must have corresponding unit tests; maintain ≥ 80% coverage
4. **Test locally** — run `./mvnw test` for your service and `./mvnw verify` to check coverage before committing
5. **Commit convention** — use conventional commits: `feat:`, `fix:`, `refactor:`, `test:`, `docs:`
6. **Open a Pull Request** against the `main` branch with a clear description of the change

### Adding a new microservice

1. Generate a Spring Boot project at [start.spring.io](https://start.spring.io) with Java 21 and the same Spring Boot parent version
2. Place it under `services/your-service/`
3. Create a Dockerfile following the pattern in any existing service
4. Add a new database to `infrastructure/docker/init-db.sql`
5. Add a new route to `services/api-gateway/src/main/resources/application.yml`
6. Add the new service to `infrastructure/docker/docker-compose.yml`
7. Add the new service to `infrastructure/kubernetes/base/`
8. Add a new scrape target to `infrastructure/monitoring/prometheus.yml`
9. Document the new endpoints in `infrastructure/swagger/openapi.yml`

---

*Smart Banking System — built for Cameroon, designed to scale globally.*
