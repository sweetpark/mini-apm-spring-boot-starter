# 📖 mini-apm-spring-boot-starter 상세 사용 및 빌드 가이드 (Usage & Build Guide)

본 문서는 `mini-apm-spring-boot-starter`의 설치 방법(JitPack vs 로컬 Maven), 소스 빌드, 런타임별 설정 및 고급 사용법을 다룹니다.

---

## 📑 목차
1. [스타터 설치 방법 (Installation - JitPack vs Local Maven)](#1-스타터-설치-방법-installation)
   - [방법 A: JitPack을 통한 의존성 추가 (외부 프로젝트 권장)](#방법-a-jitpack을-통한-의존성-추가-외부-프로젝트-도입-시-권장)
   - [방법 B: 소스 빌드 후 로컬 Maven(`mavenLocal()`) 설치 (사내망/오프라인 권장)](#방법-b-소스-코드-클론-후-로컬-mavenmavenlocal-직접-빌드설치-사내망오프라인커스텀-패치-시)
2. [소스 코드 빌드 및 품질 검증 (Build & Quality Gate)](#2-소스-코드-빌드-및-품질-검증-build--quality-gate)
3. [런타임별 사용 가이드](#3-런타임별-사용-가이드)
   - [Spring MVC (Servlet) 환경](#31-spring-mvc-servlet-환경)
   - [MyBatis 환경](#32-mybatis-환경)
   - [Spring Data JPA / Hibernate 환경](#33-spring-data-jpa--hibernate-환경)
   - [Netty TCP 소켓 환경](#34-netty-tcp-소켓-환경)
   - [Spring Batch 환경](#35-spring-batch-환경)
4. [전체 설정 프로퍼티 레퍼런스](#4-전체-설정-프로퍼티-레퍼런스)
5. [민감정보 마스킹 및 보안](#5-민감정보-마스킹-및-보안)
6. [에러 지문 해싱 및 사용자 정의 에러 평가](#6-에러-지문-해싱-및-사용자-정의-에러-평가)
7. [Grafana Loki 대시보드 연동](#7-grafana-loki-대시보드-연동)

---

## 1. 스타터 설치 방법 (Installation)

`mini-apm-spring-boot-starter`를 프로젝트에 도입하는 방법은 **① JitPack 원격 저장소 사용**과 **② 소스 빌드 후 로컬 Maven(`mavenLocal()`) 사용** 2가지 방식이 지원됩니다.

---

### 방법 A: JitPack을 통한 의존성 추가 (외부 프로젝트 도입 시 권장)
별도의 소스 다운로드나 로컬 빌드 없이 Gradle / Maven 저장소 설정만으로 즉시 사용할 수 있습니다.

> ⚠️ **주의 (GroupId)**: JitPack을 통해 의존성을 받을 때는 GroupId가 **`com.github.sweetpark`**입니다.

#### Gradle (Groovy)
`build.gradle`:
```groovy
repositories {
    mavenCentral()
    maven { url 'https://jitpack.io' }
}

dependencies {
    // 최신 릴리즈 태그 (예: v1.0.0) 또는 특정 커밋 해시
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

### 방법 B: 소스 코드 클론 후 로컬 Maven(`mavenLocal()`) 직접 빌드/설치 (사내망/오프라인/커스텀 패치 시)
사내망, 오프라인 환경 또는 스타터 소스를 직접 수정하여 사용할 때는 로컬 Maven 저장소(`~/.m2/repository`)에 직접 빌드 및 설치하여 사용할 수 있습니다.

> 💡 **안내 (GroupId)**: 소스에서 로컬 빌드 시 공식 GroupId인 **`io.github.sweetpark`**가 사용됩니다.

#### 1단계: 스타터 소스 클론 및 로컬 배포
```bash
# 1. 저장소 복제
git clone https://github.com/sweetpark/mini-apm-spring-boot-starter.git
cd mini-apm-spring-boot-starter

# 2. 로컬 Maven 저장소(~/.m2/repository)에 배포
./gradlew publishToMavenLocal
```
> 빌드 완료 시 `~/.m2/repository/io/github/sweetpark/mini-apm-spring-boot-starter/1.0.0/` 경로에 `.jar` 및 `.pom` 파일이 자동 설치됩니다.

#### 2단계: 내 애플리케이션 프로젝트 설정
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

## 2. 소스 코드 빌드 및 품질 검증 (Build & Quality Gate)

직접 소스 코드를 내려받아 빌드하거나 품질 게이트를 검증할 때 사용합니다.

### 사전 요구사항
- **JDK 17** 이상 (Java 17, 21 권장)
- 별도의 Gradle 설치 불필요 (프로젝트에 번들된 `./gradlew` Wrapper 사용)

### 빌드 및 테스트 실행
```bash
# 1. 코드 스타일 포맷팅 적용 (Google Java Format)
./gradlew spotlessApply

# 2. 전체 단위/통합 테스트, 정적 분석(SpotBugs) 및 커버리지 게이트 검증
./gradlew check

# 3. JaCoCo 테스트 커버리지 리포트 생성
./gradlew jacocoTestReport
# 리포트 위치: build/reports/jacoco/test/html/index.html
```

---

## 3. 런타임별 사용 가이드

### 3.1 Spring MVC (Servlet) 환경
Spring Boot Starter Web 의존성이 존재하면 별도 설정 없이 `ApmWebAutoConfiguration`이 자동 활성화됩니다.
- 요청마다 `LoggingFilter`가 동작하여 고유 `trace_id` 발급 (또는 수신된 `X-Trace-Id` / W3C `traceparent` 계승).
- 응답 헤더에 `X-Trace-Id`를 자동 주입하여 클라이언트-서버 간 분산 트레이싱 지원.
- JSON 요청 및 응답 본문을 `ContentCachingRequestWrapper`를 통해 안전하게 캡처 (파일 업로드/바이너리 다운로드는 자동 스킵).

#### 출력 로그 예시
```logfmt
2026-09-02 14:30:10.123 [http-nio-8080-exec-1] INFO ApmLog [HTTP] - trace_id=f47ac10b58cc4372a5670e02b2c3d479 span_id=a1b2c3d4e5f60708 interface_id=- uri=/api/v1/orders method=POST status=200 elapsed=45ms
```

---

### 3.2 MyBatis 환경
`mybatis-spring-boot-starter`가 클래스패스에 존재하면 `SqlTraceInterceptor`가 MyBatis의 `Executor`를 자동으로 가로챕니다.
- MappedStatement ID (예: `com.example.OrderMapper.selectById`).
- 바인딩된 파라미터가 치환된 **완성형 SQL**.
- 쿼리 수행 시간 및 슬로우 쿼리(`[SLOW_SQL]`) 마킹.

#### 출력 로그 예시
```logfmt
2026-09-02 14:30:10.124 [http-nio-8080-exec-1] INFO ApmLog [SQL] - trace_id=f47ac10b58cc4372a5670e02b2c3d479 span_id=a1b2c3d4e5f60708 sql_id=com.example.OrderMapper.selectById elapsed=3ms sql="SELECT * FROM orders WHERE id = 100" param="id=100"
```

---

### 3.3 Spring Data JPA / Hibernate 환경
Spring Data JPA 또는 순수 JDBC 환경에서는 `ApmDataSourceBeanPostProcessor`가 Spring의 `DataSource` 빈을 `ApmProxyDataSource`로 래핑합니다.
- `ApmProxyPreparedStatement`가 바인딩된 파라미터 및 실행 시간을 자동 수집.
- **MyBatis와 동시 사용 시 중복 방지**: MyBatis 쿼리가 실행 중일 때는 MyBatis 인터셉터가 우선 기록하고, DataSource 프록시는 자동으로 추적을 양보하여 동일 쿼리가 두 번 기록되지 않습니다.
- **N+1 쿼리 감지**: 동일 트랜잭션/요청 내에서 동일한 쿼리가 3회(설정 가능) 이상 반복 실행되면 `[N1_QUERY]` 경고 로그 출력.

#### N+1 감지 로그 예시
```logfmt
2026-09-02 14:30:10.128 [http-nio-8080-exec-1] WARN ApmLog [N1_QUERY] - trace_id=f47ac10b58cc4372a5670e02b2c3d479 sql_id=SELECT:USER call_count=4 possible N+1 detected — consider fetch join or batch size
```

---

### 3.4 Netty TCP 소켓 환경
Netty 기반 서버 파이프라인에서 `NettyTraceDuplexHandler`를 파이프라인에 추가하면 TCP 소켓 단위 트레이싱을 제공합니다.
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

#### 출력 로그 예시
```logfmt
2026-09-02 14:30:12.450 [nioEventLoopGroup-3-1] INFO ApmLog [NETTY] - trace_id=d8a4f10c59ba4182 span_id=b2c3d4e5 interface_id=TCP_ORDER client_ip=/192.168.1.100:54321 method=INBOUND status=SUCCESS elapsed=8ms sql_count=1 sql_total_elapsed=2ms
```

---

### 3.5 Spring Batch 환경
`spring-boot-starter-batch`가 포함된 환경에서는 `LoggingBatchListener`가 Job 및 Step 실행 전후를 자동 계측합니다.
- 멀티스레드 스텝 구성 시 `ApmTaskDecorator`를 `TaskExecutor`에 등록하면 메인 스레드의 트레이스 ID가 비동기 작업 스레드로 자동 전파됩니다.

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

## 4. 전체 설정 프로퍼티 레퍼런스

`application.yml` 또는 `application.properties`에서 설정 가능한 전체 옵션 목록입니다:

```yaml
apm:
  # APM 전체 활성화 여부 (기본값: true)
  enabled: true

  trace:
    # 로깅 레벨: PROD (경량 레이턴시/상태 위주), TRACE (상세 바디/SQL 포함)
    level: PROD
    # HTTP 요청/응답 헤더에 사용할 트레이스 ID 헤더명
    header-name: X-Trace-Id
    # 시스템 인터페이스 구분용 헤더명
    interface-header-name: X-Interface-Id

  slow:
    # API 응답 지연 경고 임계치 (ms)
    api-ms: 1000
    query:
      # 단일 SQL 슬로우 쿼리 경고 임계치 (ms)
      ms: 300
      # 단일 요청 내 전체 SQL 누적 시간 초과 임계치 (ms)
      total-ms: 1000

  capture:
    # Body 캡처 전략: ALWAYS, ERROR, SLOW, SAMPLE, OFF (기본값: ERROR)
    body: ERROR
    # SQL 쿼리 캡처 전략: ALWAYS, ERROR, SLOW, SAMPLE, OFF (기본값: SLOW)
    sql: SLOW
    # SAMPLE 모드 사용 시 샘플링 비율 (0.01 = 1%, 0.1 = 10%)
    sample-rate: 0.01

  security:
    # 개인/민감정보 마스킹 활성화 여부
    masking-enabled: true
    # Body 내 민감정보 마스킹 적용 여부
    mask-body: true
    # SQL 파라미터 내 민감정보 마스킹 적용 여부
    mask-sql-param: true

  limit:
    # 요청당 최대 수집 SQL 수 (OOM 방지)
    max-sql-count: 100
    # SQL 본문 및 파라미터를 보관할 최대 쿼리 수
    max-sql-detail-count: 10
    # SQL 최대 기록 문자열 길이
    max-sql-length: 2000
    # SQL 파라미터 최대 기록 문자열 길이
    max-sql-param-length: 1000
    # 요청/응답 Body 최대 기록 문자열 길이
    max-body-length: 2000
    # N+1 쿼리 감지 임계 호출 횟수
    n1-detection-threshold: 3
    # 스택 트레이스 최대 분석 깊이
    max-stack-depth: 5

  error:
    # 에러로 판정할 최소 HTTP 상태 코드 (기본값: 400)
    http-status-threshold: 400
    # JSON 응답 본문에서 에러 코드를 탐색할 키 목록
    error-code-keys:
      - resCode
      - res_cd
      - code
      - errorCode
      - status
    # 비즈니스 에러로 판정할 에러 코드 값 목록
    error-codes:
      - "9999"
      - ERROR
      - FAIL
      - ERR
    # 에러 지문 생성 시 포함할 패키지 프리픽스 (예: com.mycompany)
    app-package-prefixes:
      - com.mycompany
```

---

## 5. 민감정보 마스킹 및 보안

`SensitiveDataMasker`가 정규식 기반으로 고속 치환을 수행합니다:
- **신용카드 번호**: `1234-5678-1234-5678` ➔ `1234-****-****-5678`
- **주민등록번호 (RRN)**: `900101-1234567` ➔ `900101-1******`
- **이메일 주소**: `user@example.com` ➔ `u***@example.com`
- **전화번호**: `010-1234-5678` ➔ `010-****-5678`

---

## 6. 에러 지문 해싱 및 사용자 정의 에러 평가

### 에러 지문 해싱 (`error_fingerprint`)
예외 발생 시 Spring/Tomcat 프레임워크 내부 스택을 제외하고 애플리케이션 핵심 라인만 해싱하여 12자리 SHA-256 해시를 생성합니다.
```logfmt
2026-09-02 14:30:15.890 [http-nio-8080-exec-2] ERROR ApmLog [EXCEPTION] - trace_id=... error_fingerprint=a4f9b21c08d3 error_type=DATABASE message="Connection timed out" breadcrumbs=[{cat:"SQL",msg:"SELECT:USER 250ms"}, {cat:"SQL_ERROR",msg:"SELECT:ORDER 5000ms"}]
```
Grafana Loki에서 동일한 근본 원인의 예외를 `error_fingerprint`를 기준으로 실시간 그룹화 및 카운팅할 수 있습니다.

### 사용자 정의 `ErrorEvaluator` Bean 등록
기본 에러 판정 로직 외에 특별한 비즈니스 에러 판정이 필요한 경우, `ErrorEvaluator` 빈을 등록하면 기본 구현체를 자동 대체합니다:
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

## 7. Grafana Loki 대시보드 연동

본 프로젝트에 번들된 [`grafana/mini-apm-dashboard.json`](../grafana/mini-apm-dashboard.json)을 Grafana에 임포트하여 다음 패널을 즉시 사용할 수 있습니다:
1. **HTTP RPS (Request Per Second)**: 상태 코드별 실시간 요청 추이
2. **API Latency Percentiles**: p95, p99, 평균 응답 시간
3. **Slow SQL & Total SQL Slow**: 슬로우 쿼리 발생 빈도 및 누적 지연 쿼리
4. **N+1 Query Warnings**: N+1 쿼리 감지 경고 목록
5. **Error Breakdown by Fingerprint**: 상위 에러 지문별 발생 랭킹
6. **Unified Log Stream**: 로그 레벨 및 마커(`[HTTP]`, `[SQL]`, `[EXCEPTION]` 등) 기반의 실시간 통합 로그 스트림