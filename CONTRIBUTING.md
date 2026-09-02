# 🤝 Contributing to mini-apm-spring-boot-starter

First off, thank you for considering contributing to mini-apm-spring-boot-starter! 🎉

## 📜 Code of Conduct

Please read and follow our [Code of Conduct](CODE_OF_CONDUCT.md) to foster an inclusive and respectful community.

## 🛠 Development Workflow

1. **Fork & Clone** the repository.
2. **Create a branch**: git checkout -b feature/my-new-feature or git checkout -b fix/issue-123.
3. **Make changes**: Follow the project's code conventions.
4. **Format & Verify**:
   - Run ./gradlew spotlessApply to format your code using Google Java Format.
   - Run ./gradlew check to verify tests, spotless formatting, SpotBugs analysis, and JaCoCo coverage.
5. **Commit**: Use Conventional Commits (eat: ..., ix: ..., docs: ..., 	est: ..., efactor: ...).
6. **Push & Create PR**: Submit a Pull Request against the main branch.

## 🛡 Quality Gates

Every contribution must satisfy:
- **Build & Tests**: ./gradlew test passes 100%.
- **Style**: ./gradlew spotlessCheck passes.
- **Static Analysis**: ./gradlew spotbugsMain reports 0 high-severity bugs.
- **Coverage**: Core parser and utility packages must maintain high test line coverage.

Thank you for helping make mini-apm-spring-boot-starter better!