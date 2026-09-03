# 📐 Code Conventions & Quality Standards

본 프로젝트는 sweetpark 오픈소스 표준 품질 체계를 엄격하게 준수합니다.

## 1. Code Formatting (Spotless & Google Java Format)
- 모든 Java 소스코드는 **Google Java Format (1.18.1)** 스타일에 맞춰 자동 포맷팅됩니다.
- 검사: ./gradlew spotlessCheck
- 자동 적용: ./gradlew spotlessApply

## 2. Static Analysis (SpotBugs)
- 잠재적 버그, NPE, 자원 누수 등을 사전에 감지하기 위해 SpotBugs가 적용됩니다.
- 검사: ./gradlew spotbugsMain

## 3. Test Line Coverage (JaCoCo)
- 아래 패키지/클래스는 라인 커버리지 85% 이상을 강제하는 품질 게이트(`jacocoTestCoverageVerification`) 대상입니다.
  - `io.github.sweetpark.apm.core.support.util.*`, `io.github.sweetpark.apm.core.error.*` (유틸리티 및 에러 처리)
  - `io.github.sweetpark.apm.core.context.*`, `io.github.sweetpark.apm.core.sql.*`, `io.github.sweetpark.apm.core.process.*` (트레이스 컨텍스트, SQL 추적/N+1 감지, 공통 로그 처리)
  - `io.github.sweetpark.apm.interceptor.mybatis.SqlTraceInterceptor` (MyBatis 인터셉터)
  - `io.github.sweetpark.apm.interceptor.jpa.*` 중 `ApmProxyConnection`, `ApmProxyStatement`, `ApmProxyPreparedStatement`, `ApmProxyDataSource`, `ApmDataSourceBeanPostProcessor` (JDBC 프록시 데이터소스 및 Smart De-duplication)
  - `io.github.sweetpark.apm.support.servlet.LoggingFilter` (트레이스 필터)
- 자동 구성(`@AutoConfiguration`) 클래스들은 Spring 컨텍스트 부트스트랩 배선이 대부분이라 게이트 대상에서 제외되어 있으며, `mini-apm-spring-boot-starter-test` 모듈의 통합 테스트로 별도 검증됩니다.
- 리포트 생성: ./gradlew jacocoTestReport
- 커버리지 검증: ./gradlew jacocoTestCoverageVerification

## 4. Conventional Commits
- feat: 새로운 기능 추가
- fix: 버그 수정
- refactor: 코드 리팩토링
- test: 테스트 코드 추가/수정
- docs: 문서 변경
- chore: 빌드, 패키지 설정 변경
