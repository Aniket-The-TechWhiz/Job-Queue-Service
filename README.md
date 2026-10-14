# job-queue-service

A standalone async job queue microservice built with **Java 21**, **Spring Boot 3**, and **PostgreSQL**, designed to complement [banking-core-api](https://github.com/visurachan/banking-core-api). It accepts background jobs from any service via a lightweight SDK, processes them asynchronously with exponential backoff retries, and guarantees no job is processed more than once even under concurrent workers.

---

## How It Works

A calling service submits a job via the SDK. The job is saved to PostgreSQL with `PENDING` status. A scheduled worker polls every second, claims the next available job using `FOR UPDATE SKIP LOCKED`, and dispatches it to the appropriate handler.

```
Calling Service (e.g. Banking Core API)
        ↓  POST /jobs  { type: "SEND_EMAIL", payload: {...} }
┌─────────────────────────────────────────────────────┐
│                  Job Queue Service                  │
│                                                     │
│  1. Persist job with status PENDING                 │
│  2. Worker polls every 1s — claims next job         │
│  3. FOR UPDATE SKIP LOCKED prevents double-claim    │
│  4. Dispatch to handler (email / webhook / pdf)     │
│  5. Mark DONE — or retry with exponential backoff   │
└─────────────────────────────────────────────────────┘
        ↓                         ↓
   status: DONE              status: FAILED → retry
                             attempts >= 5 → DEAD
```

---

## Tech Stack

| Layer            | Technology         |
|------------------|--------------------|
| Language         | Java 21            |
| Framework        | Spring Boot 3.5    |
| Database         | PostgreSQL         |
| Migrations       | Flyway             |
| Observability    | Micrometer, Prometheus |
| Containerisation | Docker Compose     |
| Build Tool       | Maven              |
| Utilities        | Lombok             |
| Testing          | Testcontainers     |

---

## Job Types

| Type            | Description                                      |
|-----------------|--------------------------------------------------|
| `SEND_EMAIL`    | Sends an email using the configured mail server  |
| `SEND_WEBHOOK`  | Fires an HTTP POST to an external webhook URL    |
| `GENERATE_PDF`  | Generates and stores a PDF document              |

Adding a new job type requires only a new `JobHandler` implementation — no changes to the worker or controller.

---

## Job Lifecycle

```
PENDING → RUNNING → DONE
                  ↘ FAILED  (retryable — attempts < maxAttempts)
                  ↘ DEAD    (exhausted — attempts >= maxAttempts)

PENDING → CANCELLED  (cancelled before a worker claims it)
```

Every job starts with `maxAttempts = 5`. On failure, the worker schedules a retry using exponential backoff (`2^attempts` seconds). After 5 failed attempts the job moves to `DEAD` and is no longer retried.

---

## Architecture Decisions

**`FOR UPDATE SKIP LOCKED` for concurrent worker safety**
The worker claims jobs using a PostgreSQL `FOR UPDATE SKIP LOCKED` query. If multiple workers are running simultaneously, each worker locks the row it claims and skips any row already locked by another worker. This guarantees no job is processed more than once without needing application-level coordination or distributed locks.

**Exponential backoff on retry**
Failed jobs are not retried immediately. The next `run_at` is set to `now + 2^attempts` seconds. A job that fails once waits 2 seconds before the next attempt; after three failures it waits 8 seconds. This prevents a broken handler from hammering downstream systems.

**DEAD status after max attempts**
Jobs that exceed `maxAttempts` are moved to `DEAD` rather than being deleted. This preserves the full failure history — the `lastError` field records the most recent exception message — and allows manual resubmission via the admin endpoint once the underlying issue is resolved.

**Handler-per-job-type with a dispatch switch**
Each job type maps to a dedicated `JobHandler` implementation. The worker dispatches via a switch on `job.getType()`, keeping the worker itself free of business logic. New job types are added by implementing the `JobHandler` interface and registering it — the worker and controller need no changes.

**Testcontainers for concurrency correctness**
The concurrency test spins up a real PostgreSQL instance via Testcontainers, inserts 10 jobs, and fires 3 concurrent workers. It asserts that all 10 jobs reach `DONE` exactly once — proving the `SKIP LOCKED` claim strategy is correct under real database conditions, not a mock.

---

## API Reference

### Submit a job
```bash
POST /jobs
Content-Type: application/json

Body: { "type": "SEND_EMAIL", "payload": { "to": "user@example.com", "subject": "...", "body": "..." } }

Response 202: { "jobId": "uuid", "status": "PENDING", "createdAt": "..." }
```

### Get job status
```bash
GET /jobs/{id}

Response 200: { "jobId": "uuid", "type": "SEND_EMAIL", "status": "DONE", "attempts": 1, ... }
```

### Admin

```bash
GET  /admin/jobs?status=FAILED&type=SEND_EMAIL&limit=20   # list jobs with optional filters
POST /admin/jobs/{id}/resubmit                             # requeue a DEAD job
DELETE /admin/jobs/{id}                                    # cancel a PENDING job
```

---

## Running Locally

### Prerequisites
- Java 21
- Docker and Docker Compose
- Maven

### Run

```bash
git clone https://github.com/visurachan/job-queue.git
cd job-queue
docker compose up -d
./mvnw spring-boot:run
```

The service starts on port `8083`.

### Ports

| Service           | Port |
|-------------------|------|
| Job Queue API     | 8083 |
| PostgreSQL        | 5436 |

### Verify

```bash
curl http://localhost:8083/actuator/health
```

Expected: `{"status":"UP"}`

---

## Testing

### Run all tests

```bash
./mvnw test
```

---

### Concurrency Test — Proof of Correctness

3 concurrent workers process 10 jobs simultaneously against a real PostgreSQL instance (Testcontainers). Asserts that all 10 jobs reach `DONE` exactly once — proving `FOR UPDATE SKIP LOCKED` prevents double-processing under concurrent load.

```bash
./mvnw test -Dtest=JobWorkerConcurrencyTest
```

---

## SDK Integration

The job queue ships as a Java SDK making integration straightforward for any Java service. Add one Maven dependency and call one method.

```java
// 1. Add the dependency to pom.xml
<dependency>
    <groupId>com.Job_Queue</groupId>
    <artifactId>JQ_Service</artifactId>
    <version>0.0.1-SNAPSHOT</version>
</dependency>

// 2. Register the client bean
@Bean
public JobQueueClient jobQueueClient() {
    return new JobQueueClient("http://localhost:8083");
}

// 3. Submit a job
jobQueueClient.submit("SEND_EMAIL", Map.of(
    "to", "user@example.com",
    "subject", "Transfer Completed",
    "body", "Your transfer was successful."
));
```