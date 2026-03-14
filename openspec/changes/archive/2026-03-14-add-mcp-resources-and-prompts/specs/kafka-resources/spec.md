## ADDED Requirements

### Requirement: Cluster overview resource
The system SHALL expose a resource at URI `kafka-mcp://overview` that returns a JSON object containing Kafka cluster health summary data including broker count, controller ID, topic count, partition count, under-replicated partition count, offline partition count, offline broker IDs, health status, and a timestamp.

#### Scenario: Healthy cluster overview
- **WHEN** the MCP client reads the resource `kafka-mcp://overview`
- **THEN** the system returns a `TextResourceContents` with MIME type `application/json` containing broker_count, controller_id, topic_count, partition_count, under_replicated_partitions, offline_partitions, offline_broker_ids (empty array), health_status ("healthy"), and an ISO-8601 timestamp

#### Scenario: Cluster with offline brokers
- **WHEN** the MCP client reads the resource `kafka-mcp://overview` and one or more brokers are unreachable
- **THEN** the health_status SHALL be "unhealthy" and offline_broker_ids SHALL list the unreachable broker IDs

#### Scenario: Kafka not configured
- **WHEN** the MCP client reads the resource `kafka-mcp://overview` and Kafka bootstrap servers are not configured
- **THEN** the system SHALL return an error response indicating Kafka is not configured

### Requirement: Health check resource
The system SHALL expose a resource at URI `kafka-mcp://health-check` that returns a JSON object containing detailed health assessment covering broker status, controller status, partition health, and consumer group health, each with their own status field and an overall_status.

#### Scenario: All systems healthy
- **WHEN** the MCP client reads the resource `kafka-mcp://health-check`
- **THEN** the system returns JSON with broker_status, controller_status, partition_status, and consumer_status objects each having status "healthy", and overall_status "healthy"

#### Scenario: Under-replicated partitions detected
- **WHEN** the MCP client reads `kafka-mcp://health-check` and there are under-replicated partitions
- **THEN** partition_status.status SHALL be "warning" and partition_status.under_replicated_partitions SHALL contain the count

### Requirement: Under-replicated partitions resource
The system SHALL expose a resource at URI `kafka-mcp://under-replicated-partitions` that returns a JSON object listing all partitions where ISR count is less than the replication factor, including topic name, partition number, leader, replica count, ISR count, replica list, ISR list, missing replicas, and troubleshooting recommendations.

#### Scenario: No under-replicated partitions
- **WHEN** the MCP client reads `kafka-mcp://under-replicated-partitions` and all partitions are fully replicated
- **THEN** the system returns JSON with under_replicated_partition_count of 0 and an empty details array

#### Scenario: Under-replicated partitions exist
- **WHEN** the MCP client reads `kafka-mcp://under-replicated-partitions` and partitions have ISR count < replication factor
- **THEN** the system returns JSON with under_replicated_partition_count > 0, a details array with per-partition information (topic, partition, leader, replica_count, isr_count, replicas, isr, missing_replicas), and a recommendations array

### Requirement: Consumer lag report resource
The system SHALL expose a resource at URI `kafka-mcp://consumer-lag-report` that returns a JSON object containing consumer group lag analysis with group summaries (group_id, state, member_count, topic_count, total_lag, has_high_lag), high lag details (per-partition offsets), and recommendations. The default lag threshold SHALL be 1000 messages.

#### Scenario: All consumer groups within threshold
- **WHEN** the MCP client reads `kafka-mcp://consumer-lag-report` and no consumer group has lag exceeding 1000
- **THEN** all group_summary entries SHALL have has_high_lag as false and high_lag_details SHALL be empty

#### Scenario: Consumer groups with high lag
- **WHEN** the MCP client reads `kafka-mcp://consumer-lag-report` and a consumer group has total lag exceeding 1000
- **THEN** that group's has_high_lag SHALL be true and high_lag_details SHALL contain per-partition lag details for the offending group

#### Scenario: No consumer groups
- **WHEN** the MCP client reads `kafka-mcp://consumer-lag-report` and there are no consumer groups in the cluster
- **THEN** the system returns JSON with group_count of 0 and empty group_summary and high_lag_details arrays
