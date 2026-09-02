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
- io.github.sweetpark.apm.core.support.util.* 및 io.github.sweetpark.apm.core.error.* 패키지는 엄격한 테스트 커버리지를 유지합니다.
- 리포트 생성: ./gradlew jacocoTestReport
- 커버리지 검증: ./gradlew jacocoTestCoverageVerification

## 4. Conventional Commits
- eat: 새로운 기능 추가
- ix: 버그 수정
- efactor: 코드 리팩토링
- 	est: 테스트 코드 추가/수정
- docs: 문서 변경
- chore: 빌드, 패키지 설정 변경