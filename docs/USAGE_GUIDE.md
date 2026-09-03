# 📖 mini-apm-spring-boot-starter Usage & Build Guide

This document covers how to install `mini-apm-spring-boot-starter` (JitPack vs. local Maven), how to build it from source, per-runtime configuration, and advanced usage.

---

## 📑 Table of Contents
1. [Installing the Starter (JitPack vs. Local Maven)](#1-installing-the-starter)
   - [Option A: Add the dependency via JitPack (recommended for external projects)](#option-a-add-the-dependency-via-jitpack-recommended-for-external-projects)
   - [Option B: Clone and build locally via Maven (`mavenLocal()`)](#option-b-clone-and-build-locally-via-maven-mavenlocal-recommended-for-internal-networksoffline-use)
2. [Building & Verifying Quality Gates](#2-building--verifying-quality-gates)
3. [Per-Runtime Usage Guide](#3-per-runtime-usage-guide)
   - [Spring MVC (Servlet) Environment](#31-spring-mvc-servlet-environment)
   - [MyBatis Environment](#32-mybatis-environment)
   - [Spring Data JPA / Hibernate Environment](#33-spring-data-jpa--hibernate-environment)
   - [Netty TCP Socket Environment](#34-netty-tcp-socket-environment)
   - [Spring Batch Environment](#35-spring-batch-environment)
4. [Full Configuration Property Reference](#4-full-configuration-property-reference)
5. [Sensitive Data Masking & Security](#5-sensitive-data-masking--security)
6. [Error Fingerprint Hashing & Custom Error Evaluation](#6-error-fingerprint-hashing--custom-error-evaluation)
7. [Grafana Loki Dashboard Integration](#7-grafana-loki-dashboard-integration)

---

## 1. Installing the Starter

There are two supported ways to add `mini-apm-spring-boot-starter` to your project: **① use the JitPack remote repository**, or **② build from source and use it via local Maven (`mavenLocal()`)**.

---

### Option A: Add the dependency via JitPack (recommended for external projects)
Use it immediately with just a Gradle/Maven repository declaration -- no source download or local build required.

- 🌐 **JitPack page**: [https://jitpack.io/#sweetpark/mini-apm-spring-boot-starter](https://jitpack.io/#sweetpark/mini-apm-spring-boot-starter)
- 📌 **GroupId**: `com.github.sweetpark`
- 📌 **ArtifactId**: `mini-apm-spring-boot-starter`
- 📌 **Latest version**: `v1.0.0` (or `main-SNAPSHOT`)

#### Gradle (Groovy)
`build.gradle`:
```groovy
repositories {
    mavenCentral()
    maven { url 'https://jitpack.io' }
}

dependencies {
    // Latest release tag (e.g. v1.0.0) or a specific commit hash
    implementation 'com.github.sweetpark:mini-apm-spring-boot-starter:v1.0.0'
}
```

#### Gradle (Kotlin DSL)
`build.gradle.kts`:
```kotlin
repositories {
    mavenCentral()
    maven("https://jitpack.io")
}

dependencies {
    implementation("com.github.sweetpark:mini-apm-spring-boot-starter:v1.0.0")
}
```

#### Maven (`pom.xml`)
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

### Option B: Clone and build locally via Maven (`mavenLocal()`) (recommended for internal networks/offline use)
For internal networks, offline environments, or when you need to modify the starter's own source, you can build and install it directly to your local Maven repository (`~/.m2/repository`).

> 💡 **Note (GroupId)**: local builds from source use the official GroupId **`io.github.sweetpark`**.

#### Step 1: Clone the starter source and publish it locally
```bash
# 1. Clone the repository
git clone https://github.com/sweetpark/mini-apm-spring-boot-starter.git
cd mini-apm-spring-boot-starter

# 2. Publish to your local Maven repository (~/.m2/repository)
./gradlew publishToMavenLocal
```
> Once the build completes, the `.jar` and `.pom` files are installed automatically at `~/.m2/repository/io/github/sweetpark/mini-apm-spring-boot-starter/1.0.0/`.

#### Step 2: Configure your own application project
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

## 2. Building & Verifying Quality Gates

Use this section when you're downloading and building the source yourself, or verifying the quality gates.

### Prerequisites
- **JDK 17** or later (Java 17 or 21 recommended)
- No separate Gradle installation needed -- the project bundles a `./gradlew` wrapper

### Build & Run Tests
```bash
# 1. Apply code formatting (Google Java Format)
./gradlew spotlessApply

# 2. Run all unit/integration tests, static analysis (SpotBugs), and the coverage gate
./gradlew check

# 3. Generate the JaCoCo test coverage report
./gradlew jacocoTestReport
# Report location: build/reports/jacoco/test/html/index.html
```

---

## 3. Per-Runtime Usage Guide

### 3.1 Spring MVC (Servlet) Environment
When the Spring Boot Starter Web dependency is present, `ApmWebAutoConfiguration` activates automatically with no extra configuration.
- `LoggingFilter` runs on every request, issuing a unique `trace_id` (or inheriting an incoming `X-Trace-Id` / W3C `traceparent`).
- `X-Trace-Id` is automatically injected into the response header, enabling distributed tracing between client and server.
- JSON request and response bodies are safely captured via `ContentCachingRequestWrapper` (file uploads/binary downloads are automatically skipped).

#### Example log output
```logfmt
2026-09-02 14:30:10.123 [http-nio-8080-exec-1] INFO ApmLog [HTTP] - trace_id=f47ac10b58cc4372a5670e02b2c3d479 span_id=a1b2c3d4e5f60708 interface_id=- uri=/api/v1/orders method=POST status=200 elapsed=45ms
```

---

### 3.2 MyBatis Environment
When `mybatis-spring-boot-starter` is on the classpath, `SqlTraceInterceptor` automatically intercepts MyBatis's `Executor`.
- The MappedStatement ID (e.g. `com.example.OrderMapper.selectById`).
- The **fully-bound SQL**, with bound parameters substituted in.
- Query execution time, and slow queries tagged with `[SLOW_SQL]`.

#### Example log output
```logfmt
2026-09-02 14:30:10.124 [http-nio-8080-exec-1] INFO ApmLog [SQL] - trace_id=f47ac10b58cc4372a5670e02b2c3d479 span_id=a1b2c3d4e5f60708 sql_id=com.example.OrderMapper.selectById elapsed=3ms sql="SELECT * FROM orders WHERE id = 100" param="id=100"
```

---

### 3.3 Spring Data JPA / Hibernate Environment
In a Spring Data JPA or plain JDBC environment, `ApmDataSourceBeanPostProcessor` wraps Spring's `DataSource` bean in an `ApmProxyDataSource`.
- `ApmProxyPreparedStatement` automatically collects bound parameters and execution time.
- **De-duplication when used alongside MyBatis**: while a MyBatis query is executing, the MyBatis interceptor logs it first and the DataSource proxy automatically yields tracking, so the same query is never logged twice.
- **N+1 query detection**: if the same query executes 3 or more times (configurable) within the same transaction/request, a `[N1_QUERY]` warning is logged.

#### Example N+1 detection log
```logfmt
2026-09-02 14:30:10.128 [http-nio-8080-exec-1] WARN ApmLog [N1_QUERY] - trace_id=f47ac10b58cc4372a5670e02b2c3d479 sql_id=SELECT:USER call_count=4 possible N+1 detected — consider fetch join or batch size
```

---

### 3.4 Netty TCP Socket Environment
Add `NettyTraceDuplexHandler` to a Netty-based server pipeline to get per-socket TCP tracing.
```java
@Component
public class TcpServerChannelInitializer extends ChannelInitializer<SocketChannel> {

    @Autowired
    private ApmProperties apmProperties;

    @Override
    protected void initChannel(SocketChannel ch) {
        ChannelPipeline p = ch.pipeline();
        p.addLast("apmTraceHandler", new NettyTraceDuplexHandler(apmProperties));
        p.addLast(new StringDecoder(StandardCharsets.UTF_8));
        p.addLast(new StringEncoder(StandardCharsets.UTF_8));
        p.addLast(new BusinessHandler());
    }
}
```

#### Example log output
```logfmt
2026-09-02 14:30:12.450 [nioEventLoopGroup-3-1] INFO ApmLog [NETTY] - trace_id=d8a4f10c59ba4182 span_id=b2c3d4e5 interface_id=TCP_ORDER client_ip=/192.168.1.100:54321 method=INBOUND status=SUCCESS elapsed=8ms sql_count=1 sql_total_elapsed=2ms
```

---

### 3.5 Spring Batch Environment
When `spring-boot-starter-batch` is present, `LoggingBatchListener` automatically instruments Job and Step execution before and after.
- For multi-threaded step configurations, registering `ApmTaskDecorator` on your `TaskExecutor` automatically propagates the main thread's trace ID to asynchronous worker threads.

```java
@Bean
public TaskExecutor batchTaskExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(4);
    executor.setMaxPoolSize(8);
    executor.setTaskDecorator(new ApmTaskDecorator());
    executor.initialize();
    return executor;
}
```

---

## 4. Full Configuration Property Reference

The full list of options configurable in `application.yml` or `application.properties`:

```yaml
apm:
  # Enables/disables APM entirely (default: true)
  enabled: true

  trace:
    # Logging level: PROD (lightweight latency/status only), TRACE (includes detailed body/SQL)
    level: PROD
    # Header name used to carry the trace ID in HTTP requests/responses
    header-name: X-Trace-Id
    # Header name used to identify the calling system interface
    interface-header-name: X-Interface-Id

  slow:
    # API response latency warning threshold (ms)
    api-ms: 1000
    query:
      # Single SQL slow query warning threshold (ms)
      ms: 300
      # Threshold for cumulative SQL time across a single request (ms)
      total-ms: 1000

  capture:
    # Body capture strategy: ALWAYS, ERROR, SLOW, SAMPLE, OFF (default: ERROR)
    body: ERROR
    # SQL query capture strategy: ALWAYS, ERROR, SLOW, SAMPLE, OFF (default: SLOW)
    sql: SLOW
    # Sampling rate when using SAMPLE mode (0.01 = 1%, 0.1 = 10%)
    sample-rate: 0.01

  security:
    # Enables masking of personal/sensitive data
    masking-enabled: true
    # Whether to mask sensitive data in the body
    mask-body: true
    # Whether to mask sensitive data in SQL parameters
    mask-sql-param: true

  limit:
    # Max SQL statements collected per request (OOM prevention)
    max-sql-count: 100
    # Max number of queries for which body and parameters are retained
    max-sql-detail-count: 10
    # Max recorded SQL string length
    max-sql-length: 2000
    # Max recorded SQL parameter string length
    max-sql-param-length: 1000
    # Max recorded request/response body string length
    max-body-length: 2000
    # Call-count threshold for N+1 query detection
    n1-detection-threshold: 3
    # Max stack trace depth to analyze
    max-stack-depth: 5

  error:
    # Minimum HTTP status code considered an error (default: 400)
    http-status-threshold: 400
    # Keys to search for an error code within a JSON response body
    error-code-keys:
      - resCode
      - res_cd
      - code
      - errorCode
      - status
    # Error code values considered a business error
    error-codes:
      - "9999"
      - ERROR
      - FAIL
      - ERR
    # Package prefixes to include when generating the error fingerprint (e.g. com.mycompany)
    app-package-prefixes:
      - com.mycompany
```

---

## 5. Sensitive Data Masking & Security

`SensitiveDataMasker` performs fast, regex-based substitution:
- **Credit card numbers**: `1234-5678-1234-5678` -> `1234-****-****-5678`
- **Korean Resident Registration Numbers (RRN)**: `900101-1234567` -> `900101-1******`
- **Email addresses**: `user@example.com` -> `u***@example.com`
- **Phone numbers**: `010-1234-5678` -> `010-****-5678`

---

## 6. Error Fingerprint Hashing & Custom Error Evaluation

### Error Fingerprint Hashing (`error_fingerprint`)
When an exception occurs, framework-internal Spring/Tomcat stack frames are excluded and only the application's core lines are hashed into a 12-character SHA-256 fingerprint.
```logfmt
2026-09-02 14:30:15.890 [http-nio-8080-exec-2] ERROR ApmLog [EXCEPTION] - trace_id=... error_fingerprint=a4f9b21c08d3 error_type=DATABASE message="Connection timed out" breadcrumbs=[{cat:"SQL",msg:"SELECT:USER 250ms"}, {cat:"SQL_ERROR",msg:"SELECT:ORDER 5000ms"}]
```
This lets Grafana Loki group and count exceptions with the same root cause in real time, keyed by `error_fingerprint`.

### Registering a Custom `ErrorEvaluator` Bean
If you need custom business error evaluation logic beyond the default, registering an `ErrorEvaluator` bean automatically replaces the default implementation:
```java
@Configuration
public class CustomApmConfig {

    @Bean
    public ErrorEvaluator customErrorEvaluator(ApmProperties properties) {
        return new ErrorEvaluator() {
            @Override
            public boolean isError(int httpStatus, String responseBody, Exception ex) {
                if (ex != null) return true;
                if (httpStatus >= 500) return true;
                return responseBody != null && responseBody.contains("\"result\":\"FATAL\"");
            }

            @Override
            public String extractErrorCode(int httpStatus, String responseBody, Exception ex) {
                return ex != null ? ex.getClass().getSimpleName() : "HTTP_" + httpStatus;
            }
        };
    }
}
```

---

## 7. Grafana Loki Dashboard Integration

Import the bundled [`grafana/mini-apm-dashboard.json`](../grafana/mini-apm-dashboard.json) into Grafana to get these panels immediately:
1. **HTTP RPS (Requests Per Second)**: real-time request trends by status code
2. **API Latency Percentiles**: p95, p99, and average response time
3. **Slow SQL & Total SQL Slow**: frequency of slow queries and cumulative slow-query time
4. **N+1 Query Warnings**: list of N+1 query detection warnings
5. **Error Breakdown by Fingerprint**: ranking of the most frequent error fingerprints
6. **Unified Log Stream**: a real-time unified log stream keyed by log level and marker (`[HTTP]`, `[SQL]`, `[EXCEPTION]`, etc.)
