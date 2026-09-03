# mini-apm-spring-boot-starter sample app

A minimal Spring Boot + Spring Data JPA (H2) app that pulls in the starter via
`implementation project(':')`, so it always exercises whatever is currently in
`src/main` -- no `publishToMavenLocal` step needed.

## Run it

Requires Java 17 (the Gradle wrapper build fails with `Unsupported class file major
version` on newer JDKs). Point `JAVA_HOME` at a JDK 17 install if that's not your
default.

From the repository root:

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 17) ./gradlew :examples:sample-app:bootRun
```

The app starts on `http://localhost:8080` and seeds 5 authors with 3 books
each on startup. APM output goes to the console and, in parallel, to
`logs/mini-apm-sample.log` at the repository root (see the root
[README](../../README.md) for wiring that file into the bundled Grafana
dashboard via `docker compose up -d`).

Config lives in [`src/main/resources/application.yml`](src/main/resources/application.yml)
and intentionally uses low thresholds (`apm.slow.api-ms: 1000`,
`apm.slow.query.ms: 50`, `apm.capture.body: ALWAYS`, `apm.capture.sql: ALWAYS`)
so every feature is visible on the first request instead of waiting for real
production traffic.

## Endpoints and what they demonstrate

| Endpoint | What it does | What to look for in the log |
| :--- | :--- | :--- |
| `GET /api/authors` | Loads all authors with a single query | `[HTTP]` + `[SQL]` lines, fast |
| `GET /api/authors/n-plus-one` | Loads all authors, then lazily touches each author's `books` in a loop | Five near-identical `[SQL]` lines followed by a `[N1_QUERY]` warning once the same `sql_id` repeats past `apm.limit.n1-detection-threshold` (default 3) |
| `GET /api/authors/slow` | Sleeps 1.2s, past the 1s `apm.slow.api-ms` threshold | `[HTTP]` line with `elapsed=12xxms` |
| `GET /api/authors/{id}/error` | Throws `IllegalArgumentException` for an unknown id | `[ERROR_BIZ]` line with a 12-character `error_fingerprint` (SHA-256-derived) and a breadcrumb trail |
| `POST /api/authors` (body: `name`, `email`, `phone`) | Creates an author | `[HTTP_DETAIL]` line with `email`/`phone` masked (`apm.security.masking-enabled: true`), and an `[SQL]` line with the fully bound `INSERT` |

Example:

```bash
curl http://localhost:8080/api/authors
curl http://localhost:8080/api/authors/n-plus-one
curl http://localhost:8080/api/authors/slow
curl http://localhost:8080/api/authors/999/error
curl -X POST http://localhost:8080/api/authors \
  -H "Content-Type: application/json" \
  -d '{"name":"New Author","email":"secret@example.com","phone":"010-1234-5678"}'
```
