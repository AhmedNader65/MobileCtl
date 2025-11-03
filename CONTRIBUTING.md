# Contributing to mobilectl

Thank you for considering contributing to mobilectl! This document provides guidelines and instructions for getting involved.

## Code of Conduct

Be respectful, inclusive, and professional. We're building a welcoming community.

## How to Contribute

### 1. Fork & Clone

git clone https://github.com/yourusername/mobilectl.git
cd mobilectl


### 2. Create a Feature Branch

git checkout -b feature/your-feature-name


### 3. Make Changes
- Write clean, readable code
- Add tests for new functionality
- Update documentation as needed
- Keep commits atomic and descriptive

### 4. Push & Create Pull Request

git push origin feature/your-feature-name

Then open a PR on GitHub with a clear description.

## Code Style

### Kotlin Style Guide

- **Naming:** Use `camelCase` for variables/functions, `PascalCase` for classes
- **Line Length:** Max 120 characters
- **Indentation:** 4 spaces (no tabs)
- **Formatting:** Use ktlint (auto-formatter)

./gradlew ktlint # Check style
./gradlew ktlintFormat # Auto-fix style


### Commit Messages

Follow Conventional Commits format:

feat: add Slack notifications
fix: handle empty config file gracefully
docs: update README with examples
test: add unit tests for version bumping
chore: upgrade Gradle to 8.5


Format: `<type>: <description>`

**Types:** `feat`, `fix`, `docs`, `test`, `chore`, `refactor`, `perf`

## PR Guidelines

- **Title:** Clear, descriptive (e.g., "Add Firebase artifact upload")
- **Description:** Include:
  - What problem does this solve?
  - How did you test it?
  - Any breaking changes?
  - Screenshots/demos (if applicable)
- **Tests:** Include tests for new features
- **Documentation:** Update docs if changing behavior
- **Keep it focused:** One feature per PR

## Running Tests Locally

./gradlew test # Run all tests
./gradlew test --watch # Watch mode (if supported)
./gradlew detekt # Code quality checks


## Reporting Issues

Use GitHub Issues for bugs & feature requests:

- **Bugs:** Include OS, version, exact steps to reproduce
- **Features:** Explain use case and expected behavior
- **Documentation:** Clarify what's confusing

## Questions?

Open a Discussion on GitHub or reach out to maintainers.

---

Happy contributing! 🚀



