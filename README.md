# 🚀 mini-apm-spring-boot-starter

<p align="center">
  <a href="https://jitpack.io/#sweetpark/mini-apm-spring-boot-starter"><img src="https://jitpack.io/v/sweetpark/mini-apm-spring-boot-starter.svg" alt="JitPack" /></a>
  <img src="https://img.shields.io/badge/License-Apache_2.0-blue.svg" alt="License" />
  <img src="https://img.shields.io/badge/Java-17%2B-orange.svg" alt="Java 17+" />
  <img src="https://img.shields.io/badge/Spring%20Boot-3.x-brightgreen.svg" alt="Spring Boot 3.x" />
  <img src="https://img.shields.io/badge/Code%20Style-Google%20Java%20Format-brightgreen.svg" alt="Spotless" />
  <img src="https://img.shields.io/badge/Static%20Analysis-SpotBugs-yellow.svg" alt="SpotBugs" />
  <img src="https://img.shields.io/badge/Coverage-85%25%2B%20(Core%20%2B%20Interceptors)-success.svg" alt="Coverage" />
  <img src="https://img.shields.io/badge/Build-Passing-brightgreen.svg" alt="Build" />
</p>

> **Lightweight Non-Invasive Observability & APM Starter for Spring Boot**  
> No heavyweight APM agent (bytecode weaver/agent) installation required -- just add the dependency
> (`@AutoConfiguration`) and get **SQL execution timing, fully-bound SQL, N+1 query detection, error
> fingerprint hashing, sensitive data masking, async logging, and Grafana/Loki integration** as a
> lightweight open-source APM starter.

---

## 📌 Key Features

- **Non-Invasive**: 100% automatic instrumentation with no annotations or manual logging code in your business logic.
- **Multi-Runtime Support**:
  - **Spring Web (Servlet / MVC)**: Request/response body, HTTP status, and latency captured via ContentCaching.
  - **Netty TCP**: Inbound/outbound packet tracing via Netty's `ChannelDuplexHandler`.
  - **Spring Batch**: Job & Step execution listeners, plus `TaskDecorator` context propagation for multi-threaded steps.
- **Full Dual-ORM Support (MyBatis & JPA/Hibernate)**:
  - MyBatis: `SqlTraceInterceptor` (MappedStatement, parameter binding, slow query detection).
  - JPA / Hibernate / JDBC: `ApmProxyDataSource` & `ApmProxyPreparedStatement` (actual executed SQL and parameter logging).
  - **Smart De-duplication**: when MyBatis and JPA run in the same transaction/thread, the MyBatis interceptor logs first and the JDBC proxy automatically yields, eliminating duplicate logging at the source.
- **N+1 Query Detection**: as soon as the same SQL ID repeats past a threshold (`apm.limit.n1-detection-threshold`, default 3) within the same request/transaction, a `[N1_QUERY]` warning is emitted immediately.
- **Smart Error Fingerprinting (SHA-256)**:
  - Filters out framework boilerplate stack frames and hashes only the application's core stack trace into a unique 12-character SHA-256 fingerprint.
  - Lets Grafana Loki easily aggregate errors that share the same root cause under a single `error_fingerprint`.
- **Automatic Sensitive Data Masking**:
  - Credit card numbers (15-16 digits), Korean resident registration numbers (13 digits), email addresses, and phone numbers are safely redacted (`*`) via fast regex matching.
  - Optionally applied to request/response bodies and SQL binding parameters.
- **OOM Prevention Limits**:
  - Caps on SQL count, retained query detail count, body length, and stack trace depth keep memory usage bounded even under traffic spikes.
- **Grafana Loki & logfmt Optimized**:
  - Output as standard `key=value` logfmt with SLF4J Markers, maximizing Loki label extraction and LogQL query performance.

---

## 🚀 Quick Start

### 1. Add the Dependency (JitPack vs. Local Maven)

Depending on your project setup, you can either use **① the JitPack remote repository** or **② build from source and publish to local Maven (`mavenLocal()`)**.

#### Option A: JitPack Remote Repository (recommended for external projects)
Use it immediately by declaring the JitPack repository and dependency in `build.gradle` or `pom.xml` -- no separate build step required.

- 🌐 **JitPack page**: [https://jitpack.io/#sweetpark/mini-apm-spring-boot-starter](https://jitpack.io/#sweetpark/mini-apm-spring-boot-starter)
- 📌 **GroupId**: `com.github.sweetpark`
- 📌 **ArtifactId**: `mini-apm-spring-boot-starter`
- 📌 **Latest version**: `v1.0.0` (or `main-SNAPSHOT`)

##### Gradle (Groovy)
```groovy
repositories {
    mavenCentral()
    maven { url 'https://jitpack.io' }
}

dependencies {
    implementation 'com.github.sweetpark:mini-apm-spring-boot-starter:v1.0.0'
}
```

##### Gradle (Kotlin DSL)
```kotlin
repositories {
    mavenCentral()
    maven("https://jitpack.io")
}

dependencies {
    implementation("com.github.sweetpark:mini-apm-spring-boot-starter:v1.0.0")
}
```

##### Maven (`pom.xml`)
```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>

<dependencies>
    <dependency>
        <groupId>com.github.sweetpark</groupId>
        <artifactId>mini-apm-spring-boot-starter</artifactId>
        <version>v1.0.0</version>
    </dependency>
</dependencies>
```

---

#### Option B: Clone and Build Locally via Maven (`mavenLocal()`) (recommended for internal networks/offline use)
For internal networks, offline environments, or when applying a custom patch to the starter itself, clone the source and publish it to your local Maven repository (`~/.m2/repository`).  
*(The GroupId for local builds is `io.github.sweetpark`)*

##### 1) Clone the starter repository and publish locally
```bash
git clone https://github.com/sweetpark/mini-apm-spring-boot-starter.git
cd mini-apm-spring-boot-starter
./gradlew publishToMavenLocal
```
> Once the build completes, the `.jar` and `.pom` files are installed automatically at `~/.m2/repository/io/github/sweetpark/mini-apm-spring-boot-starter/1.0.0/`.

##### 2) Configure your own project
**Gradle (Groovy)**:
```groovy
repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    implementation 'io.github.sweetpark:mini-apm-spring-boot-starter:1.0.0'
}
```

**Gradle (Kotlin DSL)**:
```kotlin
repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    implementation("io.github.sweetpark:mini-apm-spring-boot-starter:1.0.0")
}
```

**Maven (`pom.xml`)**:
```xml
<dependencies>
    <dependency>
        <groupId>io.github.sweetpark</groupId>
        <artifactId>mini-apm-spring-boot-starter</artifactId>
        <version>1.0.0</version>
    </dependency>
</dependencies>
```

---

### 2. Basic Configuration (`application.yml`)

Designed for zero-configuration, so it works out of the box with no setup at all. To fine-tune behavior, add the following:

```yaml
apm:
  enabled: true
  trace:
    level: PROD                 # PROD (lightweight latency/status only), TRACE (detailed body/SQL)
    header-name: X-Trace-Id     # Client-propagated header (falls back to W3C traceparent or a generated UUID)
  slow:
    api-ms: 1000                # API response latency threshold (warns above 1000ms)
    query:
      ms: 300                   # Single-SQL slow query threshold (ms)
      total-ms: 1000            # Per-request cumulative SQL time threshold (ms)
  capture:
    body: ERROR                 # ALWAYS, ERROR, SLOW, SAMPLE, OFF
    sql: SLOW                   # ALWAYS, ERROR, SLOW, SAMPLE, OFF
    sample-rate: 0.01           # Sampling rate when using SAMPLE mode (1%)
  security:
    masking-enabled: true       # Automatically masks card numbers, RRNs, emails, and phone numbers
    mask-body: true
    mask-sql-param: true
  limit:
    max-sql-count: 100          # Max number of SQL statements collected per request
    max-body-length: 2000       # Max captured body length
    n1-detection-threshold: 3   # N+1 query detection threshold
```

---

## ⚙️ Configuration Reference

| Property | Default | Description |
| :--- | :---: | :--- |
| `apm.enabled` | `true` | Enables/disables APM entirely |
| `apm.trace.level` | `PROD` | Logging level (`PROD`, `TRACE`) |
| `apm.trace.header-name` | `X-Trace-Id` | HTTP request/response header name for the trace ID |
| `apm.trace.interface-header-name` | `X-Interface-Id` | Header name used to identify the calling interface |
| `apm.slow.api-ms` | `1000` | Threshold (ms) for classifying an API response as slow |
| `apm.slow.query.ms` | `300` | Threshold (ms) for classifying a single SQL query as slow |
| `apm.slow.query.total-ms` | `1000` | Threshold (ms) for cumulative SQL time exceeding the limit within a request |
| `apm.capture.body` | `ERROR` | Request/response body capture strategy (`ALWAYS`, `ERROR`, `SLOW`, `SAMPLE`, `OFF`) |
| `apm.capture.sql` | `SLOW` | SQL query capture strategy (`ALWAYS`, `ERROR`, `SLOW`, `SAMPLE`, `OFF`) |
| `apm.capture.sample-rate` | `0.01` | Sampling rate for SAMPLE mode (0.0 - 1.0) |
| `apm.security.masking-enabled` | `true` | Enables regex-based masking of personal/sensitive data |
| `apm.security.mask-body` | `true` | Whether to mask request/response bodies |
| `apm.security.mask-sql-param` | `true` | Whether to mask SQL binding parameters |
| `apm.limit.max-sql-count` | `100` | Max number of SQL statements tracked within a single request |
| `apm.limit.max-sql-detail-count`| `10` | Max number of SQL statements for which body/parameters are retained |
| `apm.limit.max-sql-length` | `2000` | Max recorded SQL string length |
| `apm.limit.max-sql-param-length`| `1000` | Max recorded parameter string length |
| `apm.limit.max-body-length` | `2000` | Max recorded request/response body string length |
| `apm.limit.n1-detection-threshold` | `3` | Call-count threshold for N+1 query detection |
| `apm.error.http-status-threshold` | `400` | Minimum HTTP status code considered an error |
| `apm.error.error-code-keys` | `resCode, res_cd, code, errorCode, status` | Field keys to search for an error code in a JSON response body |
| `apm.error.error-codes` | `9999, ERROR, FAIL, ERR` | Error code values considered a business error |

---

## 🏷️ Log Markers & Format

All logs are written to the `ApmLog` logger as structured logfmt, tagged with an SLF4J Marker.

| Marker | Description | Example log format |
| :--- | :--- | :--- |
| `[HTTP]` | HTTP request completion summary | `trace_id=xxx span_id=yyy interface_id=- uri=/users method=GET status=200 elapsed=24ms` |
| `[HTTP_DETAIL]` | Includes detailed request/response body | `trace_id=xxx span_id=yyy req_body="{...}" res_body="{...}"` |
| `[SQL]` | Captured executed SQL and bound parameters | `trace_id=xxx span_id=yyy sql_id=findUser elapsed=4ms sql="SELECT * FROM users WHERE id = 1" param="id=1"` |
| `[SLOW_SQL]` | Single slow query detected | `trace_id=xxx span_id=yyy sql_id=largeQuery elapsed=420ms sql="..." [SLOW_SQL]` |
| `[TOTAL_SQL_SLOW]` | Cumulative SQL time for a request exceeded the limit | `trace_id=xxx span_id=yyy total_sql_elapsed=1250ms limit=1000ms [TOTAL_SQL_SLOW]` |
| `[N1_QUERY]` | N+1 query detection warning | `trace_id=xxx sql_id=selectItem call_count=4 possible N+1 detected — consider fetch join or batch size` |
| `[EXCEPTION]` | Exception occurred, with fingerprint hash | `trace_id=xxx span_id=yyy error_fingerprint=a1b2c3d4e5f6 error_type=SYSTEM message="..." breadcrumbs=[...]` |
| `[NETTY]` | Netty TCP transaction summary | `trace_id=xxx span_id=yyy remote=/127.0.0.1:8080 elapsed=12ms` |
| `[NETTY_DETAIL]` | Detailed Netty packet payload | `trace_id=xxx span_id=yyy request="{...}" response="{...}"` |
| `[BATCH]` | Spring Batch Job/Step completion | `trace_id=xxx span_id=yyy job_name=dailyBatch step_name=step1 status=COMPLETED elapsed=850ms` |

---

## 📊 Grafana Loki Dashboard Integration

This project bundles a dashboard template you can import into Grafana right away:
- **Template file**: [`grafana/mini-apm-dashboard.json`](grafana/mini-apm-dashboard.json)

### Key LogQL Examples
- **HTTP requests per second (RPS)**:
  ```logql
  sum(rate({app=~".+"} |= "ApmLog" |~ "\\[HTTP\\]" [1m])) by (app)
  ```
- **p95 / p99 latency (ms)**:
  ```logql
  quantile_over_time(0.95, {app=~".+"} |= "ApmLog" |~ "\\[HTTP\\]" | unwrap elapsed [1m])
  ```
- **Slow query frequency**:
  ```logql
  sum(count_over_time({app=~".+"} |= "ApmLog" |~ "\\[SLOW_SQL\\]|\\[TOTAL_SQL_SLOW\\]" [5m]))
  ```
- **Real-time ranking by error fingerprint**:
  ```logql
  topk(10, sum(count_over_time({app=~".+"} |= "ApmLog" |~ "\\[EXCEPTION\\]" | logfmt | __error__="" [10m])) by (error_fingerprint, error_type))
  ```

---

## 🧪 Try the Local Observability Stack in 1 Minute (Docker Compose)

Spin up Loki + Grafana + mini-apm locally with no extra infrastructure.

```bash
# 1. Start the Loki + Promtail + Grafana stack
docker compose up -d

# 2. Run the sample app (in a separate terminal)
./gradlew :examples:sample-app:bootRun
```

Once the sample app is up, try the endpoints below. What APM log each call produces is documented in
[`examples/sample-app/README.md`](examples/sample-app/README.md).

```bash
curl http://localhost:8080/api/authors                 # Normal request (SQL logging)
curl http://localhost:8080/api/authors/n-plus-one       # N+1 query detection
curl http://localhost:8080/api/authors/slow             # Slow API detection
curl http://localhost:8080/api/authors/999/error        # Error fingerprint hashing
```

Then open [http://localhost:3000](http://localhost:3000) (no login required) to see the
`grafana/mini-apm-dashboard.json` dashboard, auto-provisioned and filling in with live data.
The sample app writes to `logs/mini-apm-sample.log` in logfmt format, and Promtail tails that file and ships it to Loki.

To stop the stack:

```bash
docker compose down
```

---

## 🏗️ Architecture Details & Developer Guide

- [Detailed usage and per-runtime configuration guide (docs/USAGE_GUIDE.md)](docs/USAGE_GUIDE.md)
- [Automated release and deployment pipeline guide (docs/RELEASE_WORKFLOW.md)](docs/RELEASE_WORKFLOW.md)
- [Architecture and internal lifecycle design (docs/ARCHITECTURE.md)](docs/ARCHITECTURE.md)
- [Quality gate and code convention guide (docs/CONVENTIONS.md)](docs/CONVENTIONS.md)
- [Open source contribution guide (CONTRIBUTING.md)](CONTRIBUTING.md)
- [Code of Conduct (CODE_OF_CONDUCT.md)](CODE_OF_CONDUCT.md)
- [Security policy (SECURITY.md)](SECURITY.md)

---

## 🛠️ Build from Source & Local Testing

```bash
# 1. Clone the repository
git clone https://github.com/sweetpark/mini-apm-spring-boot-starter.git
cd mini-apm-spring-boot-starter

# 2. Auto-apply code formatting (Google Java Format)
./gradlew spotlessApply

# 3. Run all unit/integration tests, static analysis (SpotBugs), and the coverage gate
./gradlew check

# 4. Publish to your local Maven repository (~/.m2/repository)
./gradlew publishToMavenLocal
```

---

## 📄 License

This project is freely usable, modifiable, and distributable under the **[Apache License 2.0](LICENSE)**.
