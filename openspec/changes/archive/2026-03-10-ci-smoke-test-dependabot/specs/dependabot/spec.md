## ADDED Requirements

### Requirement: Gradle dependency updates
Dependabot SHALL check for Gradle dependency updates on a weekly schedule.

#### Scenario: New dependency version available
- **WHEN** a newer version of a Gradle dependency is published
- **THEN** Dependabot creates a PR with the update within a week

### Requirement: GitHub Actions version updates
Dependabot SHALL check for GitHub Actions version updates on a weekly schedule.

#### Scenario: New action version available
- **WHEN** a newer version of a GitHub Actions action is published
- **THEN** Dependabot creates a PR with the update within a week

### Requirement: Grouped minor/patch updates
Dependabot SHALL group minor and patch version updates into single PRs to reduce noise.

#### Scenario: Multiple patch updates available
- **WHEN** multiple dependencies have patch updates
- **THEN** Dependabot creates a single grouped PR for all patch updates
