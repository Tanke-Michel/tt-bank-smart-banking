# CI/CD Pipeline — Stage Explanations

**Smart Banking System | Jenkins Pipeline**
**File:** `infrastructure/jenkins/Jenkinsfile`

---

## Pipeline Overview

The Jenkins pipeline implements a **9-stage automated delivery workflow** that takes every commit pushed to the `main` or `master` branch from source code to a live, verified deployment on the Contabo VPS. The pipeline is fully declarative and uses parallel execution where possible to minimise build time.

---

## Stage 1: Checkout

**What it does:**
Clones the GitHub repository at the exact commit SHA that triggered the build. Records the commit author and commit message as environment variables for use in later notifications.

**Why it exists:**
Jenkins needs a clean, reproducible copy of the source code for every build. Using `checkout scm` (rather than a hardcoded URL) means the Jenkinsfile works regardless of which repository or branch it runs from.

**Failure impact:**
If checkout fails (e.g. GitHub is unreachable or credentials are invalid), the entire pipeline aborts immediately. No code is built or deployed.

**Key output:**
- `GIT_COMMIT` — 40-character SHA of the current commit
- `GIT_BRANCH` — branch name (e.g. `main`)
- `GIT_AUTHOR` — name of the person who made the commit

---

## Stage 2: Build

**What it does:**
Runs `./mvnw clean package -DskipTests -q` in parallel across all 8 Spring Boot service directories. `-DskipTests` skips test execution at this stage because tests run separately in Stage 3, preventing redundant execution. Each service produces a fat JAR in `target/`.

**Why it exists:**
Separating the build from the test stage allows failures to be attributed precisely. A compilation error is a different problem from a test failure.

**Why parallel:**
All 8 services are fully independent Maven projects. Running them concurrently reduces the build stage from ~8 minutes (sequential) to ~2 minutes (parallel), assuming sufficient Jenkins executor resources.

**Failure impact:**
A compilation error in any one service fails that service's parallel branch. If a parallel branch fails, the stage fails after all branches complete (so all errors are visible in one run, not just the first one).

---

## Stage 3: Test

**What it does:**
Runs `./mvnw test` in parallel across all 8 services. The `-Dspring.profiles.active=test` flag activates the `application-test.properties` file, which configures H2 in-memory databases, mocked Redis, mocked RabbitMQ, and a fixed test JWT secret. After all parallel branches finish, `junit` collects all Surefire XML reports and publishes them to the Jenkins test results view.

**Why it exists:**
Tests validate that the code is correct before it is deployed. Running tests on every commit (not just before releases) means bugs are caught within minutes of being introduced.

**Test types executed:**
- Unit tests (`@ExtendWith(MockitoExtension.class)`) — pure logic, no Spring context
- Controller tests (`@WebMvcTest`) — HTTP layer with MockMvc, no database

**Failure impact:**
Any failing test fails the pipeline. The build does not proceed to Docker image creation if tests are red.

---

## Stage 4: Coverage Check

**What it does:**
Runs `./mvnw verify` which triggers the full JaCoCo lifecycle: `prepare-agent` (instrument bytecode) → `test` → `report` (generate HTML/XML to `target/site/jacoco/`) → `check` (enforce minimum thresholds). The configured thresholds are 80% LINE coverage and 70% BRANCH coverage per service. The auth-service JaCoCo HTML report is published as a Jenkins artifact.

**Why it exists:**
The project brief requires minimum 80% code coverage. Without enforced coverage thresholds, coverage degrades silently over time as new code is added without tests. JaCoCo with `jacoco-check` makes low coverage a build-breaking event, not a recommendation.

**Excluded from coverage:**
DTOs, entities, enums, and the Spring Boot `*Application.java` entry point are excluded. These are pure data containers or framework boilerplate with no testable business logic.

**Failure impact:**
If any service's coverage falls below 80% LINE or 70% BRANCH, the build fails here. No Docker images are built.

---

## Stage 5: Docker Build

**What it does:**
Builds Docker images for all 8 backend services and the frontend in parallel. Each image is tagged with two tags: a unique `{BUILD_NUMBER}-{GIT_COMMIT_SHORT}` tag for traceability, and a `latest` tag for the rolling deployment. Images are built using the multi-stage Dockerfiles in each service directory.

**Why it exists:**
Docker images are the deployment artefact. Building them in CI ensures that the exact code that passed tests is what gets deployed — not code that was modified locally.

**Multi-stage build rationale:**
Stage 1 (`maven:3.9.6-eclipse-temurin-21`) compiles and packages. Stage 2 (`eclipse-temurin:21-jre-jammy`) contains only the JRE and the fat JAR — no build tools. This reduces the final image size from ~600MB to ~250MB.

**Failure impact:**
A Docker build failure (e.g. Dockerfile syntax error, network failure pulling base image) fails the stage. No images are pushed.

---

## Stage 6: Docker Push

**What it does:**
Logs into Docker Hub using the `DOCKER_CREDENTIALS` Jenkins credential, pushes all 9 images (8 services + frontend) with both the unique and `latest` tags, then immediately logs out. This stage only runs on `main` or `master` branches (not on feature branches).

**Why it exists:**
Images must be stored in a registry accessible by the VPS. Docker Hub is the registry of record. The unique tag provides an immutable reference to any specific build; `latest` is what the deploy stage pulls.

**Security note:**
The Docker password is never echoed in logs because it is retrieved via `withCredentials` which masks it. `docker logout` is called immediately after the push.

**Failure impact:**
A push failure (e.g. Docker Hub rate limit, invalid credentials) fails the pipeline before deployment occurs. The VPS still runs the previous version.

---

## Stage 7: Deploy

**What it does:**
SSH's into the Contabo VPS as the `ubuntu` user using the `VPS_SSH_KEY` private key credential. On the server, it:
1. Changes to the deployment directory `/opt/tt-bank`
2. Runs `docker compose pull` to download the newly pushed `latest` images
3. Writes a fresh `.env` file from Jenkins secret credentials (no secrets ever stored on the server between deployments)
4. Runs `docker compose up -d --no-build` for a rolling restart

The `--no-build` flag is deliberate: the images were already built and pushed in Stages 5–6. The compose command only needs to restart containers with the new images.

**Why it exists:**
Automated deployment eliminates manual SSH sessions, reduces human error, and ensures the deployed code always matches the tested code.

**Rolling restart behaviour:**
`docker compose up -d` updates one container at a time. Containers with `restart: unless-stopped` are restarted with the new image. Health checks ensure the new container is healthy before the old one is terminated.

**Failure impact:**
If `docker compose up` fails (e.g. a service fails its health check), Docker Compose rolls back to the previous container. The VPS remains operational on the last working version.

---

## Stage 8: Smoke Test

**What it does:**
SSH's into the VPS again and runs a `check` function against every service's `/actuator/health` endpoint. Waits 30 seconds first to allow all containers to finish their startup sequence. The function uses `curl` to fetch the health URL and verifies the HTTP response code is 200. If any service returns a non-200 status, the function exits with code 1, failing the stage.

**Services checked:**
API Gateway (:8080), Auth (:8081), Wallet (:8082), Transaction (:8083), Merchant (:8084), Notification (:8085), Savings (:8086), Audit (:8087), Frontend (/health).

**Why it exists:**
A deployment is not complete until the system is verified to be running. The smoke test catches cases where deployment succeeded but a service failed to start (e.g. a missing environment variable, a database migration failure, an out-of-memory error).

**Failure impact:**
A smoke test failure does not automatically roll back the deployment (Docker Compose does not support automatic rollback in this configuration). The failure creates a Jenkins alert and the team must investigate and redeploy manually.

---

## Stage 9 (Post): Notify & Cleanup

**What it does:**
Runs in the `post` block regardless of pipeline outcome. Prints a success or failure summary. Calls `cleanWs()` to delete the workspace from the Jenkins agent's disk, freeing space for the next build.

**Why cleanup matters:**
Without cleanup, each build accumulates ~500MB of compiled classes, downloaded Maven dependencies, and Docker build context. On a Jenkins server with many projects, disk space exhaustion is a real operational risk.

---

---

# Monitoring — Key Metrics Explained

**Smart Banking System | Prometheus + Grafana**
**Config:** `infrastructure/monitoring/prometheus.yml`

---

## Infrastructure Metrics

### `up{job="<service-name>"}` — Service availability

**What it measures:** Whether Prometheus can successfully scrape the service's `/actuator/prometheus` endpoint. Returns `1` if the scrape succeeded, `0` if the service is unreachable.

**Why it matters:** This is the most critical metric. A value of `0` for any service means that service is down and all users attempting to use that service's features are getting errors. Alert rule `ServiceDown` fires after 1 minute of `up == 0`.

**Grafana panel:** "Service Health" row — coloured stat panels (green = UP, red = DOWN).

---

### `node_cpu_seconds_total{mode="idle"}` — VPS CPU usage

**What it measures:** The fraction of CPU time spent in idle state. CPU usage is calculated as `1 - avg(rate(node_cpu_seconds_total{mode="idle"}[5m]))`.

**Why it matters:** Sustained CPU above 85% indicates that services are starved for compute resources, which will manifest as slow response times and potential OOM kills. Alert rule `HighCPUUsage` fires after 5 minutes above 85%.

**Source:** Node Exporter on the VPS host.

---

### `node_memory_MemAvailable_bytes` — VPS memory

**What it measures:** Available memory (not just free — includes reclaimable cache). Memory usage = `(MemTotal - MemAvailable) / MemTotal`.

**Why it matters:** Spring Boot JVMs use significant memory. With 8 services each using 256–512MB, a VPS with 8GB RAM runs at 50–65% memory utilisation at rest. Alert rule `HighMemoryUsage` fires above 85%.

---

### `node_filesystem_avail_bytes` — Disk space

**What it measures:** Available bytes on the root filesystem.

**Why it matters:** Docker images, log files, and PostgreSQL data all consume disk. Running out of disk space causes PostgreSQL to crash (it cannot write WAL logs), which causes all services to lose database connectivity simultaneously. Alert `LowDiskSpace` fires when less than 15% remains.

---

## Application Metrics (Spring Boot / Micrometer)

### `http_server_requests_seconds_count` — Request rate

**What it measures:** A counter of HTTP requests, labelled by `job` (service name), `uri`, `method`, and `status` (HTTP status code).

**Useful queries:**
- Total requests/s: `sum(rate(http_server_requests_seconds_count{job="wallet-service"}[2m]))`
- Error rate: requests where `status=~"5.."` divided by total requests

**Why it matters:** A sudden spike in request rate may indicate a DDoS or a malfunctioning client making repeated calls. A sudden drop may indicate that the service is down or that the load balancer has removed it from rotation.

---

### `http_server_requests_seconds_bucket` — Response latency (histogram)

**What it measures:** A histogram of HTTP response times, allowing percentile calculations.

**Key queries:**
- P95 latency: `histogram_quantile(0.95, sum(rate(http_server_requests_seconds_bucket{job="transaction-service"}[5m])) by (le))`

**Why it matters:** Average latency is misleading — a service can have a good average while 5% of requests take 10 seconds. P95 and P99 latency reveal the tail behaviour that users actually experience. Alert `SlowAPIResponse` fires when P95 > 2 seconds for 3 minutes.

---

### `jvm_memory_used_bytes{area="heap"}` and `jvm_memory_max_bytes{area="heap"}` — JVM heap

**What it measures:** Bytes of JVM heap currently occupied vs the configured maximum.

**Key query:** `jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"}` gives heap usage as a ratio.

**Why it matters:** A JVM whose heap usage stays above 85% is constantly running garbage collection, which causes GC pauses that manifest as latency spikes. A heap above 95% will trigger `OutOfMemoryError`, crashing the container. Alert `HighJVMHeapUsage` fires above 85% for 5 minutes.

---

### `jvm_threads_live_threads` — Thread count

**What it measures:** Number of live JVM threads (platform threads for the servlet services, virtual threads if configured).

**Why it matters:** A continuously growing thread count indicates a thread leak. Spring Boot uses a thread-per-request model for the servlet services; a stuck thread pool exhaustion manifests as requests timing out with `HTTP 503`.

---

## Business & Security Metrics

### `http_server_requests_seconds_count{status="401"}` on auth-service — Authentication failures

**What it measures:** Rate of 401 Unauthorized responses from the auth service, which correspond to failed login attempts.

**Why it matters:** More than 10 failed auth attempts per second suggests a brute-force or credential-stuffing attack targeting the login endpoint. Alert `HighAuthFailureRate` fires above 10/s for 2 minutes. The API Gateway's rate limiting (5 requests/minute on `/api/v1/auth/login`) provides the first line of defence, but monitoring provides the second.

---

### `http_server_requests_seconds_count{status="429"}` on api-gateway — Rate limit hits

**What it measures:** Rate of 429 Too Many Requests responses from the API Gateway, indicating that the rate limiter is actively blocking requests.

**Why it matters:** Occasional 429s are expected during normal usage peaks. Sustained 429s above 5/s indicate either an attack, a misconfigured client making too many requests, or a legitimate traffic spike that requires scaling the gateway.

---

### Transfer and payment throughput (derived)

**What it measures:** Rate of successful (status=201) requests to the transfer endpoint of transaction-service and the pay endpoint of merchant-service.

**Why it matters:** These are the primary business value metrics — they measure how much financial activity the system is processing. A drop in this rate (without a corresponding drop in request volume) indicates that transfers are failing, which requires immediate investigation.

---

*Pipeline documentation prepared for the DevOps and Software Architecture course.*
*Monitoring documentation prepared in accordance with SRE observability practices.*
