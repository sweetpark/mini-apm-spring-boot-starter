# 🚀 mini-apm-spring-boot-starter

<p align="center">
  <img src="https://img.shields.io/badge/License-Apache_2.0-blue.svg" alt="License" />
  <img src="https://img.shields.io/badge/Java-17%2B-orange.svg" alt="Java 17+" />
  <img src="https://img.shields.io/badge/Spring%20Boot-3.x-brightgreen.svg" alt="Spring Boot 3.x" />
  <img src="https://img.shields.io/badge/Code%20Style-Google%20Java%20Format-brightgreen.svg" alt="Spotless" />
  <img src="https://img.shields.io/badge/Static%20Analysis-SpotBugs-yellow.svg" alt="SpotBugs" />
  <img src="https://img.shields.io/badge/Coverage-100%25%20(Core)-success.svg" alt="Coverage" />
  <img src="https://img.shields.io/badge/AI%20Review-CodeRabbit-purple.svg" alt="CodeRabbit" />
</p>

> **Lightweight Non-Invasive Observability & APM Starter for Spring Boot**  
> 별도의 복잡한 에이전트 설치 없이 의존성 추가(`@AutoConfiguration`)만으로 SQL 실행 시간 측정, 예외 지문 해싱, 민감정보 마스킹, 비동기 로깅 및 Grafana/Loki 연동을 일괄 제공하는 경량 APM 라이브러리입니다.

---

## 📌 1. 프로젝트 개요 & 오픈소스 비전 (Open Source Vision)

### 💡 왜 이 라이브러리가 필요한가? (Problem Statement)
* **무거운 APM 에이전트 부담**: Pinpoint, Datadog, Scouter 등 대형 APM 도구는 인프라 비용과 설정 복잡도가 큽니다.
* **비즈니스 코드 침투 없는 모니터링**: 코드 수정 없이 SQL 실행 시간(`0.001ms` 정밀도), 파라미터가 바인딩된 완성형 SQL, 슬로우 쿼리를 자동으로 수집해야 합니다.
* **로그 보안 및 성능**: 실시간 로그 수집 시 민감정보(카드번호, 주민번호) 마스킹과 비동기 큐(`AsyncLogEventQueue`) 처리를 통한 요청 스레드 블로킹 방지가 필수적입니다.
* **다중 런타임 & 다중 ORM 지원**: Spring MVC, Netty TCP, Spring Batch 런타임뿐만 아니라 **MyBatis와 JPA(Hibernate) 환경 모두에서 동일한 수준의 SQL 추적**을 지원해야 합니다.

### 🎯 오픈소스 비전 & 제공 형태
외부 개발자가 자신의 Spring Boot 애플리케이션에 손쉽게 도입할 수 있도록 표준 스타터 형태로 배포합니다.
1. **JitPack / Maven Central 배포**: `implementation 'io.github.sweetpark:mini-apm-spring-boot-starter:1.0.0'`
2. **Zero-Configuration 원칙**: `@ConditionalOnProperty(matchIfMissing = true)`로 설정 없이 즉시 동작하되, `application.yml`로 세부 튜닝 가능.
3. **ORM 자동 감지(MyBatis & JPA)**: 프로젝트에 포함된 의존성에 따라 MyBatis 인터셉터 또는 JDBC DataSource 프록시를 자동 활성화.
4. **Grafana 대시보드 템플릿 번들**: Grafana / Alloy / Loki 환경에서 즉시 시각화할 수 있는 JSON 대시보드 템플릿 기본 제공.

---

## 🧩 2. SQL 추적 아키텍처 (MyBatis & JPA 지원 설계)

MyBatis와 JPA(Hibernate)의 실행 라이프사이클 차이를 반영하여, 환경에 맞게 최적화된 비침투 인터셉터를 조건부로 활성화합니다.

```
mini-apm-spring-boot-starter
  ├── [MyBatis 환경 감지 시]
  │     └─ SqlTraceInterceptor (MyBatis StatementHandler 가로채기)
  │
  ├── [JPA / Hibernate / JDBC 감지 시]
  │     └─ JpaSqlTraceInterceptor (JDBC DataSource Proxy / PreparedStatement 가로채기)
  │
  └── [공통 로깅 파이프라인 (재사용)]
        ├─ ErrorFingerprinter (SHA-256 예외 스택 해싱)
        ├─ SensitiveDataMasker (정규식 기반 민감정보 마스킹)
        └─ AsyncLogEventQueue (비동기 큐 기반 Non-blocking 로깅)
```

| 구분 | MyBatis 모드 | JPA (Hibernate / QueryDSL) 모드 |
| :--- | :--- | :--- |
| **인터셉트 지점** | `org.apache.ibatis.plugin.Interceptor` | `DataSource` Proxy (`PreparedStatement.execute()`) |
| **추적 내용** | Mapper ID, 완성형 SQL, 파라미터, 실행 시간 | Repository/Entity, 실제 실행 SQL, 파라미터, 실행 시간 |
| **슬로우 쿼리 감지**| `[SLOW_SQL]` 임계치 초과 시 자동 마킹 | 동일하게 `[SLOW_SQL]` 자동 마킹 |

---

## 🛡 3. Sweetpark 오픈소스 표준 품질 게이트 & CI/CD 파이프라인

본 프로젝트는 `sweetpark` 오픈소스 표준 품질 관리 체계를 따릅니다.

| 도구 | 역할 | 검증 방식 |
| :--- | :--- | :--- |
| **Spotless** | Google Java Format 스타일 자동 포맷팅 | `./gradlew spotlessCheck` (적용: `spotlessApply`) |
| **SpotBugs** | Java 바이트코드 레벨 잠재적 버그/NPE 정적 분석 | `./gradlew spotbugsMain` |
| **JaCoCo** | 핵심 파서/인터셉터 모듈 **100% 라인 커버리지** 강제화 | `./gradlew jacocoTestCoverageVerification` |
| **CodeRabbit AI** | PR 등록 시 변경 diff 자동 AI 코드 리뷰 | GitHub PR Webhook 연동 |
| **GitHub Actions** | Push/PR 시 자동 빌드, 테스트, Step Summary 리포트 발행 | `.github/workflows/ci.yml` |

---

## 🔍 4. 원본 소스 및 이관 대상 (Source Reference)

| 구분 | 내용 |
| :--- | :--- |
| **원본 저장소** | `wiezonSRC/APM-LOGGING-STARTER` (Branch: `main`) |
| **핵심 모듈 1** | `logging-starter/` (AutoConfiguration, Interceptors, Filters, LogQueue, Masker) |
| **핵심 모듈 2** | `logging-starter-test/` (Servlet/Netty/Batch 통합 테스트 및 샘플 데모 앱) |
| **참고 문서/설정** | `GRAFANA.md`, `NETTY_CONFIG.md`, `SERVLET_CONFIG.md`, `BATCH_CONFIG.md` |

---

## 🛠 5. 이관 및 리팩토링 기준 (Refactoring & Sanitization Rules)

새로운 세션에서 소스코드를 옮겨올 때 **반드시 준수해야 하는 기준**입니다.

### ① 사내 규격 및 하드코딩 완전 제거 (Sanitization & Generalization)
* **특정 헤더명 강제 해소**:
  * 사내 헤더명(`IFID` 등)을 고정하지 않고, `apm.trace.header-name: X-Trace-Id`와 같이 `application.yml` 프로퍼티로 자유롭게 변경 가능하도록 개선 (기본값: `X-Request-Id` or `traceId`).
* **사내 에러 코드(`9999`) 하드코딩 제거**:
  * 특정 응답 코드 기반의 에러 판정 로직을 `ErrorEvaluator` 인터페이스로 추상화하고, 기본 구현체는 HTTP 4xx/5xx 및 일반적인 Exception을 기준으로 판정하도록 리팩토링.
* **사내 전용 로거/경로 일반화**:
  * `/LOG_PATH` 등 하드코딩된 파일 경로 대신 Spring Boot의 기본 `logging.file.path` 및 표준 SLF4J Marker 구조 준수.

### ② 패키지 네이밍 표준화
* 기존 `com.company.logging`을 표준 오픈소스 패키지로 리네이밍:
  * **Target Base Package**: `io.github.sweetpark.apm`
  * Core: `io.github.sweetpark.apm.core`
  * MyBatis Interceptor: `io.github.sweetpark.apm.interceptor.mybatis`
  * JPA/JDBC Proxy: `io.github.sweetpark.apm.interceptor.jpa`
  * Netty/Batch: `io.github.sweetpark.apm.support.{netty,batch}`

### ③ 확장성 및 조건부 자동 구성 (Conditional Configuration)
* `@ConditionalOnClass`를 세분화하여, 소비 프로젝트에 Netty, Spring Batch, MyBatis, JPA 중 일부만 존재해도 `NoClassDefFoundError` 없이 안전하게 구동되도록 설계.

---

## 📚 6. 오픈소스 필수 표준 6종 문서 구축 계획

이관 시 다음 6종 문서를 생성합니다:
1. `LICENSE` (Apache License 2.0)
2. `CODE_OF_CONDUCT.md` (Contributor Covenant 2.1)
3. `CONTRIBUTING.md` (기여 가이드, 브랜치 전략, 100% 커버리지 룰)
4. `docs/ARCHITECTURE.md` (Spring Boot AutoConfiguration 및 인터셉터 흐름 다이어그램)
5. `docs/CONVENTIONS.md` (Spotless, SpotBugs, Conventional Commits)
6. `README.md` (공식 뱃지, 퀵스타트, Grafana 연동 가이드)

---

## ⚙️ 7. GitHub 저장소 설정 (Repository Settings Checklist)

* [ ] **Visibility**: `Settings` ➔ Danger Zone ➔ **`Make public`**
* [ ] **PR 머지 시 브랜치 자동 삭제**: `Settings` ➔ `General` ➔ `Pull Requests` ➔ ✅ **`Automatically delete head branches`** 체크
* [ ] **`main` 브랜치 보호 룰**: `Settings` ➔ `Branches` ➔ `Add branch protection rule` (`main`)
  - ✅ `Require a pull request before merging` (Approvals: 1)
  - ✅ `Require status checks to pass before merging` (Status check: `Test & 100% Coverage Verification`)
  - ✅ `Require conversation resolution before merging`
* [ ] **CodeRabbit AI 연동**: [CodeRabbit.ai](https://coderabbit.ai/)에서 저장소 연동 및 `Chill` 프로필 설정

---

## 🗺 8. 단계별 로드맵 (Roadmap to Public Release)

```mermaid
graph LR
    P1["Phase 1<br/>레포 초기화 & 설계"] --> P2["Phase 2<br/>소스 이관 & 리팩토링"]
    P2 --> P3["Phase 3<br/>JPA/MyBatis 검증"]
    P3 --> P4["Phase 4<br/>Public 오픈소스 배포"]
    style P1 fill:#238636,stroke:#fff,stroke-width:2px,color:#fff
    style P2 fill:#1f6feb,stroke:#fff,stroke-width:2px,color:#fff
    style P3 fill:#8957e5,stroke:#fff,stroke-width:2px,color:#fff
    style P4 fill:#d29922,stroke:#fff,stroke-width:2px,color:#fff
```

### 📌 Phase 1: Private 레포 생성 및 청사진 수립 (✅ 완료)
- [x] 오픈소스 지향 저장소(`mini-apm-spring-boot-starter`) 생성 (Private)
- [x] 이관 가이드, 리팩토링 원칙, **MyBatis & JPA 동시 지원 아키텍처** 및 품질 플레이북이 담긴 README 작성

### 📌 Phase 2: 소스코드 이관 및 클렌징 (Next Session)
- [ ] `wiezonSRC/APM-LOGGING-STARTER`에서 `logging-starter` 및 `test` 모듈 이관
- [ ] 패키지명 변경 (`io.github.sweetpark.apm`)
- [ ] 하드코딩된 사내 규격(IFID, 9999 에러코드 등)을 `application.yml` 프로퍼티 및 전략 패턴 인터페이스로 리팩토링
- [ ] **JPA/Hibernate용 DataSource Proxy SQL 추적 모듈 추가** (`JpaSqlTraceInterceptor`)
- [ ] Spotless, SpotBugs, JaCoCo 100% 라인 커버리지 룰 구성

### 📌 Phase 3: 테스트 및 데모 검증
- [ ] Spring MVC, Netty TCP, Spring Batch 3개 런타임별 통합 테스트 수행
- [ ] **MyBatis vs JPA(Hibernate) 각각의 SQL 파라미터 바인딩 및 슬로우 쿼리 감지 검증**
- [ ] 비동기 큐(`AsyncLogEventQueue`) 부하 테스트 및 TPS 측정
- [ ] Grafana 대시보드 템플릿(`grafana-dashboard.json`) 작성 및 동작 검증

### 📌 Phase 4: Public 오픈소스 전환 및 배포
- [ ] `LICENSE` (Apache 2.0), `CONTRIBUTING.md`, `CODE_OF_CONDUCT.md`, `docs/` 추가
- [ ] GitHub Actions CI (`.github/workflows/ci.yml`, `release.yml`) 및 PR 템플릿 추가
- [ ] CodeRabbit AI 연동 및 Branch Protection 설정
- [ ] **저장소 Public 전환 및 Maven Central / JitPack 배포**

---

## 📋 다음 세션 작업자를 위한 체크리스트 (Action Items for Next Session)
1. `wiezonSRC/APM-LOGGING-STARTER` 소스 복사 및 패키지 리네이밍 (`io.github.sweetpark.apm`)
2. `LoggingProperties.java`에 커스텀 헤더/에러 판정 설정 추가
3. DataSource 프록시 기반의 JPA/JDBC SQL 인터셉터(`JpaSqlTraceInterceptor`) 추가
4. Spotless, SpotBugs, JaCoCo 100% 라인 커버리지 연동 (`./gradlew check`)
5. GitHub Actions CI (`.github/workflows/ci.yml`) 및 PR 템플릿 추가 후 PR 생성
