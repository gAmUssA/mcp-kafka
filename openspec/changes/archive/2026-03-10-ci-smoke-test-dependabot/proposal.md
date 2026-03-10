## Why

The project has no CI pipeline. PRs can merge with broken builds, and dependencies can go stale without notice. A smoke test GitHub Action and Dependabot config are table stakes before adding more features.

## What Changes

- Create a GitHub Actions workflow that runs on PRs and pushes to main:
  - Compiles the project (Java 21, Gradle 9)
  - Runs unit tests (no Docker required)
  - Runs integration tests (Testcontainers, Docker required)
  - Validates the Quarkus build completes
- Create Dependabot configuration for Gradle and GitHub Actions dependency updates

## Capabilities

### New Capabilities
- `ci-pipeline`: GitHub Actions smoke test workflow for compilation, unit tests, integration tests
- `dependabot`: Automated dependency update configuration

### Modified Capabilities

_(none)_

## Impact

- **New files**: `.github/workflows/ci.yml`, `.github/dependabot.yml`
- **No code changes**: CI-only addition
