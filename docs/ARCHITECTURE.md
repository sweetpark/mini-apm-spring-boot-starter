# 🏛 mini-apm-spring-boot-starter Architecture

## 1. High-Level Overview

mini-apm-spring-boot-starter is an ultra-lightweight, non-invasive APM (Application Performance Monitoring) and structured observability starter for Spring Boot applications.

### Core Goals
- **Zero-Invasive Instrumentation**: No custom byte-code manipulation, no proprietary agent runtime installation.
- **Multi-Runtime Support**: Spring MVC (Servlet), Netty TCP, and Spring Batch.
- **Dual ORM Support**: Seamless SQL tracing for both **MyBatis** and **JPA/Hibernate (JDBC)**.
- **Production-Grade Resilience**: Built-in OOM protection (SQL truncation, capacity limits) and Fail-safe error handlers.
- **Privacy & Security**: Built-in PCI-DSS compliant sensitive data masking (Card numbers, RRN, email, phone).

---

## 2. Component Diagram

`mermaid
graph TD
    subgraph Client Requests
        HTTP["HTTP (Servlet MVC)"]
        TCP["TCP (Netty)"]
        BATCH["Job / Step (Spring Batch)"]
    end

    subgraph Observability Pipeline
        LF["LoggingFilter / DuplexHandler / BatchListener"]
        TCH["TraceContextHolder (traceId, spanId, Breadcrumbs)"]
        EE["ErrorEvaluator (Status / Code / Exception)"]
        SDM["SensitiveDataMasker (Card / RRN / Custom)"]
    end

    subgraph SQL Observability Layer
        STC["SqlTraceContextHolder"]
        MYB["SqlTraceInterceptor (MyBatis Executor)"]
        JPA["ApmProxyDataSource (JPA / Hibernate / JDBC)"]
    end

    subgraph Structured Log Output
        ALP["AbstractLogProcessor"]
        LOKI["Grafana / Loki / Alloy (logfmt Format)"]
    end

    HTTP --> LF
    TCP --> LF
    BATCH --> LF

    LF --> TCH
    LF --> STC

    MYB --> STC
    JPA --> STC

    STC --> ALP
    EE --> ALP
    SDM --> ALP
    TCH --> ALP

    ALP --> LOKI
`

---

## 3. SQL Interception Architecture (MyBatis vs JPA)

### Dual Detection & De-duplication Mechanism
Applications may use either MyBatis, Spring Data JPA (Hibernate), or both simultaneously.
To prevent duplicate logging when both are active:
1. SqlTraceInterceptor marks SqlTraceContextHolder.setMyBatisActive(true).
2. ApmProxyPreparedStatement checks SqlTraceContextHolder.isMyBatisActive(). If 	rue, the lower-level JDBC proxy bypasses duplicate tracking.
3. If MyBatis is not present or not handling the statement (e.g. Pure JPA/Hibernate query), ApmProxyPreparedStatement captures the parameters, formats the SQL, records execution time, and detects N+1 queries.

| Feature | MyBatis Mode | JPA Mode |
| :--- | :--- | :--- |
| **Intercept Point** | MyBatis Executor (update, query) | JDBC DataSource Proxy (PreparedStatement) |
| **SQL Extraction** | MappedStatement & BoundSql | Intercepted SQL String & Parameter Index Map |
| **N+1 Detection** | Mapper ID threshold tracking | SQL signature threshold tracking |
| **Slow SQL** | Tagged with [SLOW_SQL] marker | Tagged with [SLOW_SQL] marker |

---

## 4. Error Classification & Bug Fingerprinting

ErrorFingerprinter computes a deterministic 12-character SHA-256 hash:
1. Top-level exception class name.
2. First application code stack trace frame (filtering out Spring, Hibernate, Netty, JVM internal frames).
3. Root cause exception class name.

This allows Grafana Loki to aggregate and alert on error_fingerprint across distributed nodes.