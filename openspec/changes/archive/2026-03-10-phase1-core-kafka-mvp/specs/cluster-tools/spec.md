## ADDED Requirements

### Requirement: describe-cluster tool
The server SHALL provide a `describe-cluster` MCP tool that returns Kafka cluster metadata. It SHALL take no input parameters and return the cluster ID, controller broker info, and a list of all brokers with host and port.

#### Scenario: Describe a running cluster
- **WHEN** `describe-cluster` is called on a healthy cluster with 3 brokers
- **THEN** the tool returns the cluster ID, the controller broker identifier, and a list of 3 brokers each with host and port

#### Scenario: Describe a single-node cluster
- **WHEN** `describe-cluster` is called on a single-broker cluster
- **THEN** the tool returns the cluster ID, the controller (same as the single broker), and a list with 1 broker entry
