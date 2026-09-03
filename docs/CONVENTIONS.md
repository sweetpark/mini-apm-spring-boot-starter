# 📐 Code Conventions & Quality Standards

This project strictly follows sweetpark's open-source quality standards.

## 1. Code Formatting (Spotless & Google Java Format)
- All Java source code is auto-formatted to the **Google Java Format (1.18.1)** style.
- Check: ./gradlew spotlessCheck
- Auto-apply: ./gradlew spotlessApply

## 2. Static Analysis (SpotBugs)
- SpotBugs is applied to catch potential bugs, NPEs, resource leaks, and similar issues before they ship.
- Check: ./gradlew spotbugsMain

## 3. Test Line Coverage (JaCoCo)
- The following packages/classes are subject to the line-coverage quality gate (`jacocoTestCoverageVerification`), which enforces at least 85% coverage.
  - `io.github.sweetpark.apm.core.support.util.*`, `io.github.sweetpark.apm.core.error.*` (utilities and error handling)
  - `io.github.sweetpark.apm.core.context.*`, `io.github.sweetpark.apm.core.sql.*`, `io.github.sweetpark.apm.core.process.*` (trace context, SQL tracing/N+1 detection, common log processing)
  - `io.github.sweetpark.apm.interceptor.mybatis.SqlTraceInterceptor` (MyBatis interceptor)
  - `io.github.sweetpark.apm.interceptor.jpa.*`: `ApmProxyConnection`, `ApmProxyStatement`, `ApmProxyPreparedStatement`, `ApmProxyDataSource`, `ApmDataSourceBeanPostProcessor` (JDBC proxy datasource and Smart De-duplication)
  - `io.github.sweetpark.apm.support.servlet.LoggingFilter` (the trace filter)
- `@AutoConfiguration` classes are excluded from the gate, since they are mostly Spring context bootstrap wiring; they are instead verified separately via the integration tests in the `mini-apm-spring-boot-starter-test` module.
- Generate the report: ./gradlew jacocoTestReport
- Verify coverage: ./gradlew jacocoTestCoverageVerification

## 4. Conventional Commits
- feat: adding a new feature
- fix: bug fix
- refactor: code refactoring
- test: adding/updating test code
- docs: documentation changes
- chore: build/tooling/dependency changes
