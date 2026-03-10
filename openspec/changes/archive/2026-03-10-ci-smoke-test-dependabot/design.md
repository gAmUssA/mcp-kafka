## Context

The project uses Gradle 9.4.0, Java 21, Quarkus 3.27.2, and Testcontainers with `apache/kafka-native` for integration tests. Unit tests (e.g. ToolFilterTest) run without Docker. Integration tests require Docker.

## Goals / Non-Goals

**Goals:**
- Fast CI feedback on every PR and push to main
- Separate unit tests from integration tests so compilation feedback is fast even if Docker is slow
- Automated dependency updates via Dependabot

**Non-Goals:**
- Release/publish workflow (future)
- GraalVM native image build in CI (future, Phase 5)
- Code coverage reporting

## Decisions

### 1. Single workflow with matrix for test types

One workflow file with two jobs: `build` (compile + unit tests) and `integration-test` (full tests with Docker). The build job is fast and gives immediate feedback. Integration tests run in parallel but don't block the build result.

### 2. Gradle caching

Use `actions/setup-java` with `cache: gradle` for fast builds.

### 3. Dependabot schedule

Weekly checks for both Gradle dependencies and GitHub Actions versions. Group minor/patch updates to reduce PR noise.

## Risks / Trade-offs

- **[Risk] Integration tests flaky in CI** → Testcontainers with native Kafka image is fast and reliable. If issues arise, can add retry.
- **[Trade-off] No native build in CI** → Keeps CI fast. Native build is Phase 5 scope.
