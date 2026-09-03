# 🚀 Automated Release Workflow

When a PR is merged into the `main` branch, `mini-apm-spring-boot-starter` analyzes the commit messages and fully automates **semantic version (SemVer) calculation ➔ quality gate verification ➔ Git tag creation ➔ GitHub Release publishing ➔ JitPack build warm-up** with no manual steps.

---

## 📑 Table of Contents
1. [Pipeline Overview](#1-pipeline-overview)
2. [Commit Message Convention & Version Bump Rules](#2-commit-message-convention--version-bump-rules)
3. [PR Merge Guide (Squash and Merge)](#3-pr-merge-guide-squash-and-merge)
4. [GitHub Actions Step-by-Step Details](#4-github-actions-step-by-step-details)
5. [Commits That Skip a Release](#5-commits-that-skip-a-release)
6. [Manual Release (Manual Fallback)](#6-manual-release-manual-fallback)

---

## 1. Pipeline Overview

```
[Developer opens a PR] ──► [CI quality gate passes] ──► [Squash & Merge into main]
                                                       │
  ┌────────────────────────────────────────────────────┘
  ▼
[GitHub Actions: release.yml runs automatically]
  ├─ 1. Analyze commit messages (feat, fix, etc.) since the previous tag
  ├─ 2. Compute the next semantic version (e.g. v1.0.0 ➔ v1.1.0)
  ├─ 3. Re-verify the full quality gate (./gradlew check)
  ├─ 4. Create and push the Git tag automatically (v1.1.0)
  ├─ 5. Publish the GitHub Release automatically (changelog and dependency snippet attached)
  └─ 6. Call the JitPack API ➔ trigger a background pre-build (warm-up)
```

---

## 2. Commit Message Convention & Version Bump Rules

The repository automatically determines the version based on the [Conventional Commits](https://www.conventionalcommits.org/) standard:

| Commit Prefix (Type) | Meaning | Version Bump (SemVer) | Example |
| :--- | :--- | :---: | :--- |
| **`fix:`**, **`perf:`** | Bug fixes and performance improvements | **PATCH** (`+0.0.1`) | `fix: handle null pointer in Netty trace context (#15)` |
| **`feat:`** | New feature | **MINOR** (`+0.1.0`) | `feat: support Redis command latency tracing (#16)` |
| **`BREAKING CHANGE:`** or **`feat!:`** | Breaking change to an existing API | **MAJOR** (`+1.0.0`) | `feat!: change default apm properties hierarchy (#20)` |

---

## 3. PR Merge Guide (Squash and Merge)

We strongly recommend using **"Squash and merge"** when merging a PR.

1. Select **"Squash and merge"** from the merge button at the bottom of the PR page.
2. The generated commit message's **first line (title)** determines the version bump, so write it according to the convention:
   - ✨ New feature: `feat: add webflux reactive tracing support (#12)`
   - 🐛 Bug fix: `fix: prevent duplicate log in mixed orm (#13)`
   - 💥 Major change: `feat!: upgrade minimum spring boot baseline to 3.2 (#14)`
3. Clicking **Confirm squash and merge** immediately kicks off the automated release pipeline.

---

## 4. GitHub Actions Step-by-Step Details

Workflow file: [`.github/workflows/release.yml`](../.github/workflows/release.yml)

1. **Semantic version detection (`mathieudutour/github-tag-action`)**:
   - Scans the latest commits on `main` and computes the diff against the previous tag.
2. **Quality verification build (`./gradlew check --no-daemon`)**:
   - The release only proceeds once unit/integration tests, Google Java Format (`spotless`), SpotBugs static analysis, and the JaCoCo coverage gate have all passed.
3. **GitHub Release publishing (`softprops/action-gh-release`)**:
   - Automatically generates markdown release notes alongside the release tag, and attaches a ready-to-copy JitPack dependency snippet.
4. **JitPack build pre-caching (warm-up)**:
   - Runs `curl -s -X GET "https://jitpack.io/api/builds/sweetpark/mini-apm-spring-boot-starter/vX.Y.Z"` to trigger JitPack into starting the build immediately.
   - This means external users requesting the library get it right away, with no build wait time.

---

## 5. Commits That Skip a Release

To avoid unnecessarily bumping the version number, the following commit types are **merged silently without publishing a new release**:
- `docs:` (README or documentation changes)
- `chore:` (dependency bumps, build script changes, etc.)
- `style:` (code formatting)
- `test:` (additional test coverage)
- `ci:` (GitHub Actions workflow changes)

---

## 6. Manual Release (Manual Fallback)

If you need to force-publish a specific version outside of the automated release:

```bash
# 1. Update main to the latest
git checkout main
git pull origin main

# 2. Create and push the desired version tag
git tag -a v1.2.0 -m "Release v1.2.0: Manual deployment"
git push origin v1.2.0
```
After pushing the tag, click **`Get it`** on the [JitPack page](https://jitpack.io/#sweetpark/mini-apm-spring-boot-starter) to trigger an immediate build.
