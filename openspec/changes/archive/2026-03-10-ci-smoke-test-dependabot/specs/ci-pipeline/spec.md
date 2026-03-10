## ADDED Requirements

### Requirement: CI workflow triggers
The CI workflow SHALL run on pull requests targeting `main` and on pushes to `main`.

#### Scenario: PR triggers CI
- **WHEN** a pull request is opened or updated against `main`
- **THEN** the CI workflow runs

#### Scenario: Push to main triggers CI
- **WHEN** code is pushed to `main`
- **THEN** the CI workflow runs

### Requirement: Build and unit test job
The workflow SHALL have a job that compiles the project and runs unit tests without requiring Docker.

#### Scenario: Compilation succeeds
- **WHEN** the build job runs
- **THEN** `./gradlew compileJava compileTestJava` completes successfully

#### Scenario: Unit tests pass
- **WHEN** the build job runs unit tests
- **THEN** tests that do not require Docker pass

### Requirement: Integration test job
The workflow SHALL have a job that runs the full test suite including Testcontainers-based integration tests.

#### Scenario: Integration tests pass
- **WHEN** the integration test job runs with Docker available
- **THEN** all tests including Testcontainers-based tests pass

### Requirement: Java and Gradle versions
The workflow SHALL use Java 21 and the Gradle wrapper version checked into the repository.

#### Scenario: Correct toolchain
- **WHEN** the CI jobs run
- **THEN** they use Java 21 (Temurin) and Gradle via `./gradlew`
