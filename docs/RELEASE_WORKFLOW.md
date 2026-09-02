# 🚀 자동 릴리즈 및 JitPack 배포 워크플로우 (Automated Release Workflow)

`mini-apm-spring-boot-starter`는 PR이 `main` 브랜치에 머지될 때 커밋 메시지를 분석하여 **자동으로 시맨틱 버전(SemVer) 계산 ➔ 품질 게이트 검증 ➔ Git Tag 생성 ➔ GitHub Release 발행 ➔ JitPack 빌드 웜업**까지 100% 무인 자동화로 처리합니다.

---

## 📑 목차
1. [전체 파이프라인 개요](#1-전체-파이프라인-개요)
2. [커밋 메시지 컨벤션 및 버전 승격 규칙](#2-커밋-메시지-컨벤션-및-버전-승격-규칙)
3. [PR 머지 가이드 (Squash and Merge)](#3-pr-머지-가이드-squash-and-merge)
4. [GitHub Actions 동작 단계 상세](#4-github-actions-동작-단계-상세)
5. [릴리즈 스킵 대상 커밋](#5-릴리즈-스킵-대상-커밋)
6. [수동 릴리즈 (Manual Fallback)](#6-수동-릴리즈-manual-fallback)

---

## 1. 전체 파이프라인 개요

```
[개발자 PR 생성] ──► [CI 품질 게이트 통과] ──► [main 브랜치에 Squash & Merge]
                                                       │
  ┌────────────────────────────────────────────────────┘
  ▼
[GitHub Actions: release.yml 자동 실행]
  ├─ 1. 이전 태그 이후의 커밋 메시지(feat, fix 등) 분석
  ├─ 2. 다음 시맨틱 버전 계산 (예: v1.0.0 ➔ v1.1.0)
  ├─ 3. 전체 품질 게이트 재검증 (./gradlew check)
  ├─ 4. Git Tag 자동 생성 및 Push (v1.1.0)
  ├─ 5. GitHub Release 자동 생성 (Changelog 및 의존성 가이드 자동 첨부)
  └─ 6. JitPack API 호출 ➔ 백그라운드 사전 빌드(Warm-up) 완료
```

---

## 2. 커밋 메시지 컨벤션 및 버전 승격 규칙

저장소는 [Conventional Commits](https://www.conventionalcommits.org/) 표준을 기반으로 버전을 자동으로 판별합니다:

| 커밋 접두사 (Type) | 의미 | 승격되는 버전 (SemVer) | 예시 |
| :--- | :--- | :---: | :--- |
| **`fix:`**, **`perf:`** | 버그 수정 및 성능 개선 | **PATCH** (`+0.0.1`) | `fix: handle null pointer in Netty trace context (#15)` |
| **`feat:`** | 새로운 기능 추가 | **MINOR** (`+0.1.0`) | `feat: support Redis command latency tracing (#16)` |
| **`BREAKING CHANGE:`** 또는 **`feat!:`** | 기존 API 파괴적 변경 | **MAJOR** (`+1.0.0`) | `feat!: change default apm properties hierarchy (#20)` |

---

## 3. PR 머지 가이드 (Squash and Merge)

PR을 머지할 때는 **"Squash and merge"** 방식을 적극 권장합니다.

1. PR 페이지 하단 머지 버튼에서 **"Squash and merge"** 선택.
2. 생성되는 커밋 메시지의 **첫 줄(Title)**이 버전 결정의 기준이 되므로 컨벤션에 맞게 작성합니다:
   - ✨ 기능 추가: `feat: add webflux reactive tracing support (#12)`
   - 🐛 버그 수정: `fix: prevent duplicate log in mixed orm (#13)`
   - 💥 메이저 변경: `feat!: upgrade minimum spring boot baseline to 3.2 (#14)`
3. **Confirm squash and merge**를 클릭하면 즉시 자동 배포 파이프라인이 작동합니다.

---

## 4. GitHub Actions 동작 단계 상세

워크플로우 파일: [`.github/workflows/release.yml`](../.github/workflows/release.yml)

1. **시맨틱 버전 판별 (`mathieudutour/github-tag-action`)**:
   - `main` 브랜치의 최신 커밋들을 스캔하여 이전 태그와의 차이를 계산합니다.
2. **품질 검증 빌드 (`./gradlew check --no-daemon`)**:
   - 단위/통합 테스트, Google Java Format(`spotless`), SpotBugs 정적 분석, JaCoCo 커버리지 게이트가 모두 통과해야만 릴리즈가 진행됩니다.
3. **GitHub Release 발행 (`softprops/action-gh-release`)**:
   - 자동으로 릴리즈 태그와 함께 마크다운 릴리즈 노트를 생성하고, 사용자가 복사할 수 있는 JitPack 의존성 스니펫을 첨부합니다.
4. **JitPack 빌드 사전 캐싱 (Warm-up)**:
   - `curl -s -X GET "https://jitpack.io/api/builds/sweetpark/mini-apm-spring-boot-starter/vX.Y.Z"`를 실행하여 JitPack이 즉시 빌드를 시작하도록 트리거합니다.
   - 외부 사용자가 라이브러리를 요청할 때 빌드 대기 시간 없이 즉시 다운로드됩니다.

---

## 5. 릴리즈 스킵 대상 커밋

버전 번호가 불필요하게 증가하는 것을 방지하기 위해 다음 커밋들은 **새 릴리즈를 발행하지 않고 조용히 머지만 수행**됩니다:
- `docs:` (README 또는 문서 수정)
- `chore:` (의존성 번들 갱신, 빌드 스크립트 수정 등)
- `style:` (코드 포맷팅)
- `test:` (테스트 코드 보강)
- `ci:` (GitHub Actions 워크플로우 수정)

---

## 6. 수동 릴리즈 (Manual Fallback)

자동 릴리즈 외에 특정 버전을 강제로 직접 배포하고 싶을 경우:

```bash
# 1. main 최신화
git checkout main
git pull origin main

# 2. 원하는 버전 태그 생성 및 푸시
git tag -a v1.2.0 -m "Release v1.2.0: Manual deployment"
git push origin v1.2.0
```
태그를 푸시한 후 [JitPack 웹페이지](https://jitpack.io/#sweetpark/mini-apm-spring-boot-starter)에서 **`Get it`**을 클릭하면 즉시 빌드됩니다.