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
> 별도의 무거운 APM 에이전트(Bytecode Weaver/Agent) 설치 없이 의존성 추가(`@AutoConfiguration`)만으로  
> **SQL 실행 시간 측정, 파라미터 완성형 SQL, N+1 쿼리 감지, 예외 지문 해싱, 민감정보 마스킹, 비동기 로깅 및 Grafana/Loki 연동**을 일괄 제공하는 오픈소스 경량 APM 스타터입니다.

---

## 📌 주요 특징 (Key Features)

- **비침투성 (Non-Invasive)**: 비즈니스 코드에 애노테이션이나 수동 로깅 코드 수정 없이 100% 자동 계측.
- **다중 런타임 지원**:
  - **Spring Web (Servlet / MVC)**: ContentCaching 기반 요청/응답 본문, HTTP 상태, 레이턴시 기록.
  - **Netty TCP**: Netty `ChannelDuplexHandler` 기반 인바운드/아웃바운드 패킷 트레이싱.
  - **Spring Batch**: Job & Step Execution 리스너 및 멀티스레드 스텝용 `TaskDecorator` 컨텍스트 전파.
- **Dual ORM 완벽 지원 (MyBatis & JPA/Hibernate)**:
  - MyBatis: `SqlTraceInterceptor` (MappedStatement, 파라미터 바인딩, 슬로우 쿼리 감지).
  - JPA / Hibernate / JDBC: `ApmProxyDataSource` & `ApmProxyPreparedStatement` (실제 실행 SQL 및 파라미터 로깅).
  - **스마트 중복 방지 (De-duplication)**: 동일 트랜잭션/스레드에서 MyBatis와 JPA가 동시 실행될 때 MyBatis 인터셉터가 우선 실행되고 JDBC 프록시는 자동으로 추적을 양보하여 중복 로깅을 원천 차단.
- **N+1 쿼리 감지 (N+1 Query Detection)**: 동일 요청/트랜잭션 내에서 동일 SQL ID가 임계치(`apm.limit.n1-detection-threshold`, 기본 3회) 이상 반복 실행되면 즉시 `[N1_QUERY]` 경고 마킹.
- **스마트 에러 지문 해싱 (SHA-256 Error Fingerprinter)**:
  - 프레임워크 보일러플레이트 스택을 필터링하고 애플리케이션 핵심 스택 트레이스만 추출하여 고유한 12자리 SHA-256 해시 생성.
  - 동일한 원인의 에러를 Grafana Loki에서 단일 지문(`error_fingerprint`)으로 손쉽게 집계(Aggregation) 가능.
- **민감정보 자동 마스킹 (Sensitive Data Masking)**:
  - 신용카드 번호(15~16자리), 주민등록번호(RRN 13자리), 이메일 주소, 전화번호를 고속 정규식으로 안전하게 치환(`*`).
  - 요청/응답 Body 및 SQL 바인딩 파라미터에 선택적으로 적용 가능.
- **OOM 방지 메모리 가드 (OOM Prevention Limits)**:
  - SQL 개수 제한, 세부 쿼리 저장 개수 제한, Body 길이 제한, 스택 트레이스 깊이 제한을 통해 트래픽 폭주 시에도 메모리 안전성 보장.
- **Grafana Loki & logfmt 최적화**:
  - `key=value` 형태의 표준 logfmt 구조 및 SLF4J Marker로 출력되어 Loki 레이블 추출 및 LogQL 쿼리 성능 극대화.

---

## 🚀 빠른 시작 (Quick Start)

### 1. 의존성 추가 (Dependency: JitPack vs 로컬 Maven)

프로젝트 환경에 따라 **① JitPack 원격 저장소** 또는 **② 소스 빌드 후 로컬 Maven(`mavenLocal()`)** 중 선택하여 의존성을 구성할 수 있습니다.

#### 방법 A: JitPack 원격 저장소 사용 (외부 프로젝트 권장)
별도 빌드 과정 없이 `build.gradle` 또는 `pom.xml`에 JitPack 저장소와 의존성을 선언하여 즉시 사용합니다.

- 🌐 **JitPack 배포 주소**: [https://jitpack.io/#sweetpark/mini-apm-spring-boot-starter](https://jitpack.io/#sweetpark/mini-apm-spring-boot-starter)
- 📌 **GroupId**: `com.github.sweetpark`
- 📌 **ArtifactId**: `mini-apm-spring-boot-starter`
- 📌 **최신 버전**: `v1.0.0` (또는 `main-SNAPSHOT`)

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

#### 방법 B: 소스 코드 클론 후 로컬 Maven(`mavenLocal()`) 직접 빌드/설치 (사내망/오프라인 권장)
사내망, 오프라인 환경 또는 커스텀 패치를 직접 빌드하여 적용할 때는 소스를 클론하여 로컬 Maven 저장소(`~/.m2/repository`)에 배포 후 사용합니다.  
*(로컬 빌드 GroupId: `io.github.sweetpark`)*

##### 1) 스타터 저장소 클론 및 로컬 배포
```bash
git clone https://github.com/sweetpark/mini-apm-spring-boot-starter.git
cd mini-apm-spring-boot-starter
./gradlew publishToMavenLocal
```
> 빌드 완료 시 `~/.m2/repository/io/github/sweetpark/mini-apm-spring-boot-starter/1.0.0/` 경로에 `.jar` 및 `.pom` 파일이 자동 설치됩니다.

##### 2) 내 프로젝트 설정
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

### 2. 기본 설정 (`application.yml`)

Zero-Configuration을 지향하므로 별도 설정 없이도 즉시 동작합니다. 세부 동작을 조정하려면 아래 설정을 추가하세요:

```yaml
apm:
  enabled: true
  trace:
    level: PROD                 # PROD (경량 레이턴시/상태만), TRACE (상세 바디/SQL)
    header-name: X-Trace-Id     # 클라이언트 전파 헤더 (미입력 시 W3C traceparent 또는 UUID 자동 생성)
  slow:
    api-ms: 1000                # API 응답 지연 임계치 (1000ms 초과 시 경고)
    query:
      ms: 300                   # 단일 SQL 슬로우 쿼리 임계치 (ms)
      total-ms: 1000            # 요청당 누적 SQL 시간 임계치 (ms)
  capture:
    body: ERROR                 # ALWAYS, ERROR, SLOW, SAMPLE, OFF
    sql: SLOW                   # ALWAYS, ERROR, SLOW, SAMPLE, OFF
    sample-rate: 0.01           # SAMPLE 모드 시 샘플링 비율 (1%)
  security:
    masking-enabled: true       # 카드번호, 주민번호, 이메일, 전화번호 자동 마스킹
    mask-body: true
    mask-sql-param: true
  limit:
    max-sql-count: 100          # 요청당 최대 수집 SQL 수
    max-body-length: 2000       # 캡처할 최대 Body 길이
    n1-detection-threshold: 3   # N+1 쿼리 감지 임계치
```

---

## ⚙️ 상세 설정 참조 (Configuration Reference)

| 설정 프로퍼티 | 기본값 | 설명 |
| :--- | :---: | :--- |
| `apm.enabled` | `true` | APM 기능 전체 활성화 여부 |
| `apm.trace.level` | `PROD` | 로깅 레벨 (`PROD`, `TRACE`) |
| `apm.trace.header-name` | `X-Trace-Id` | 트레이스 ID HTTP 요청/응답 헤더명 |
| `apm.trace.interface-header-name` | `X-Interface-Id` | 인터페이스 ID 식별 헤더명 |
| `apm.slow.api-ms` | `1000` | API 슬로우 응답 판정 임계치 (ms) |
| `apm.slow.query.ms` | `300` | 개별 SQL 슬로우 쿼리 판정 임계치 (ms) |
| `apm.slow.query.total-ms` | `1000` | 요청 내 전체 SQL 누적 시간 초과 임계치 (ms) |
| `apm.capture.body` | `ERROR` | 요청/응답 본문 캡처 전략 (`ALWAYS`, `ERROR`, `SLOW`, `SAMPLE`, `OFF`) |
| `apm.capture.sql` | `SLOW` | SQL 쿼리 캡처 전략 (`ALWAYS`, `ERROR`, `SLOW`, `SAMPLE`, `OFF`) |
| `apm.capture.sample-rate` | `0.01` | SAMPLE 모드 시 샘플링 비율 (0.0 ~ 1.0) |
| `apm.security.masking-enabled` | `true` | 정규식 기반 개인/민감정보 마스킹 활성화 |
| `apm.security.mask-body` | `true` | 요청/응답 Body 마스킹 적용 여부 |
| `apm.security.mask-sql-param` | `true` | SQL 바인딩 파라미터 마스킹 적용 여부 |
| `apm.limit.max-sql-count` | `100` | 단일 요청 내 추적할 최대 SQL 개수 |
| `apm.limit.max-sql-detail-count`| `10` | 쿼리 본문 및 파라미터를 보관할 최대 SQL 수 |
| `apm.limit.max-sql-length` | `2000` | 기록할 SQL 최대 문자열 길이 |
| `apm.limit.max-sql-param-length`| `1000` | 기록할 파라미터 최대 문자열 길이 |
| `apm.limit.max-body-length` | `2000` | 기록할 요청/응답 Body 최대 문자열 길이 |
| `apm.limit.n1-detection-threshold` | `3` | N+1 쿼리 감지 임계 호출 횟수 |
| `apm.error.http-status-threshold` | `400` | 에러로 간주할 최소 HTTP 상태 코드 |
| `apm.error.error-code-keys` | `resCode, res_cd, code, errorCode, status` | JSON 응답 본문에서 탐색할 에러 필드 키 목록 |
| `apm.error.error-codes` | `9999, ERROR, FAIL, ERR` | 비즈니스 에러로 간주할 에러 코드 값 목록 |

---

## 🏷️ 로그 마커 규격 (Log Markers & Format)

모든 로그는 SLF4J Marker와 함께 구조화된 logfmt 포맷으로 `ApmLog` 로거에 기록됩니다.

| 마커 (Marker) | 설명 | 예시 로그 포맷 |
| :--- | :--- | :--- |
| `[HTTP]` | HTTP 요청 처리 완료 요약 | `trace_id=xxx span_id=yyy interface_id=- uri=/users method=GET status=200 elapsed=24ms` |
| `[HTTP_DETAIL]` | 상세 요청/응답 바디 포함 | `trace_id=xxx span_id=yyy req_body="{...}" res_body="{...}"` |
| `[SQL]` | 캡처된 실행 SQL 및 바인딩 파라미터 | `trace_id=xxx span_id=yyy sql_id=findUser elapsed=4ms sql="SELECT * FROM users WHERE id = 1" param="id=1"` |
| `[SLOW_SQL]` | 슬로우 쿼리 단건 감지 | `trace_id=xxx span_id=yyy sql_id=largeQuery elapsed=420ms sql="..." [SLOW_SQL]` |
| `[TOTAL_SQL_SLOW]` | 요청 누적 SQL 시간 초과 | `trace_id=xxx span_id=yyy total_sql_elapsed=1250ms limit=1000ms [TOTAL_SQL_SLOW]` |
| `[N1_QUERY]` | N+1 쿼리 감지 경고 | `trace_id=xxx sql_id=selectItem call_count=4 possible N+1 detected — consider fetch join or batch size` |
| `[EXCEPTION]` | 예외 발생 및 지문 해시 | `trace_id=xxx span_id=yyy error_fingerprint=a1b2c3d4e5f6 error_type=SYSTEM message="..." breadcrumbs=[...]` |
| `[NETTY]` | Netty TCP 트랜잭션 요약 | `trace_id=xxx span_id=yyy remote=/127.0.0.1:8080 elapsed=12ms` |
| `[NETTY_DETAIL]` | Netty 패킷 페이로드 상세 | `trace_id=xxx span_id=yyy request="{...}" response="{...}"` |
| `[BATCH]` | Spring Batch Job/Step 완료 | `trace_id=xxx span_id=yyy job_name=dailyBatch step_name=step1 status=COMPLETED elapsed=850ms` |

---

## 📊 Grafana Loki 대시보드 연동

본 프로젝트는 Grafana에서 즉시 임포트하여 사용할 수 있는 대시보드 템플릿을 번들로 제공합니다:
- **템플릿 파일**: [`grafana/mini-apm-dashboard.json`](grafana/mini-apm-dashboard.json)

### 주요 LogQL 예시
- **초당 HTTP 요청 수 (RPS)**:
  ```logql
  sum(rate({app=~".+"} |= "ApmLog" |~ "\\[HTTP\\]" [1m])) by (app)
  ```
- **p95 / p99 레이턴시 (ms)**:
  ```logql
  quantile_over_time(0.95, {app=~".+"} |= "ApmLog" |~ "\\[HTTP\\]" | unwrap elapsed [1m])
  ```
- **슬로우 쿼리 발생 빈도**:
  ```logql
  sum(count_over_time({app=~".+"} |= "ApmLog" |~ "\\[SLOW_SQL\\]|\\[TOTAL_SQL_SLOW\\]" [5m]))
  ```
- **에러 지문별 실시간 발생 순위**:
  ```logql
  topk(10, sum(count_over_time({app=~".+"} |= "ApmLog" |~ "\\[EXCEPTION\\]" | logfmt | __error__="" [10m])) by (error_fingerprint, error_type))
  ```

---

## 🧪 로컬 관측 스택 1분 체험 (Docker Compose)

별도의 인프라 없이 로컬에서 Loki + Grafana + mini-apm 조합을 바로 띄워볼 수 있습니다.

```bash
# 1. Loki + Promtail + Grafana 스택 기동
docker compose up -d

# 2. 샘플 앱 실행 (별도 터미널)
./gradlew :examples:sample-app:bootRun
```

샘플 앱이 뜨면 아래 엔드포인트를 호출해보세요. 각 호출이 어떤 APM 로그를 만들어내는지는
[`examples/sample-app/README.md`](examples/sample-app/README.md)에 정리되어 있습니다.

```bash
curl http://localhost:8080/api/authors                 # 정상 요청 (SQL 로깅)
curl http://localhost:8080/api/authors/n-plus-one       # N+1 쿼리 감지
curl http://localhost:8080/api/authors/slow             # 슬로우 API 감지
curl http://localhost:8080/api/authors/999/error        # 예외 지문 해싱
```

그 다음 [http://localhost:3000](http://localhost:3000) (별도 로그인 없이 접속 가능)으로 접속하면
`grafana/mini-apm-dashboard.json` 대시보드가 자동 프로비저닝되어 실시간으로 채워지는 것을 확인할 수 있습니다.
샘플 앱은 `logs/mini-apm-sample.log`에 logfmt 포맷으로 기록하며, Promtail이 이 파일을 tail 하여 Loki로 전송합니다.

스택을 종료하려면:

```bash
docker compose down
```

---

## 🏗️ 아키텍처 상세 & 개발자 가이드

- [상세 사용 및 런타임별 설정 가이드 (docs/USAGE_GUIDE.md)](docs/USAGE_GUIDE.md)
- [자동 릴리즈 및 배포 파이프라인 가이드 (docs/RELEASE_WORKFLOW.md)](docs/RELEASE_WORKFLOW.md)
- [아키텍처 및 내부 라이프사이클 설계 (docs/ARCHITECTURE.md)](docs/ARCHITECTURE.md)
- [품질 게이트 및 코드 컨벤션 가이드 (docs/CONVENTIONS.md)](docs/CONVENTIONS.md)
- [오픈소스 기여 가이드 (CONTRIBUTING.md)](CONTRIBUTING.md)
- [행동 강령 (CODE_OF_CONDUCT.md)](CODE_OF_CONDUCT.md)
- [보안 정책 (SECURITY.md)](SECURITY.md)

---

## 🛠️ 소스 빌드 및 로컬 테스트 (Build from Source)

```bash
# 1. 저장소 클론
git clone https://github.com/sweetpark/mini-apm-spring-boot-starter.git
cd mini-apm-spring-boot-starter

# 2. 코드 포맷팅 자동 적용 (Google Java Format)
./gradlew spotlessApply

# 3. 전체 단위/통합 테스트, 정적 분석(SpotBugs) 및 커버리지 게이트 검증
./gradlew check

# 4. 로컬 Maven 저장소(~/.m2/repository)에 배포
./gradlew publishToMavenLocal
```

---

## 📄 라이선스 (License)

본 프로젝트는 **[Apache License 2.0](LICENSE)**에 따라 자유롭게 사용, 수정, 배포할 수 있습니다.