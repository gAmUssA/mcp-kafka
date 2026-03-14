## ADDED Requirements

### Requirement: Cluster overview prompt
The system SHALL expose an MCP prompt named `kafka_cluster_overview` that generates a human-readable markdown summary of Kafka cluster health. The prompt SHALL accept no arguments and SHALL return a PromptMessage with user role containing a TextContent with formatted markdown including broker count, controller ID, topic count, partition count, under-replicated partitions, offline partitions, and an overall health status indicator.

#### Scenario: Generate cluster overview
- **WHEN** the MCP client invokes the prompt `kafka_cluster_overview`
- **THEN** the system returns a PromptMessage containing markdown with a "Kafka Cluster Overview" heading, a timestamp, bullet points for each metric, and a health status line

#### Scenario: Kafka not configured
- **WHEN** the MCP client invokes `kafka_cluster_overview` and Kafka is not configured
- **THEN** the system SHALL return a PromptMessage indicating Kafka is not configured

### Requirement: Health check prompt
The system SHALL expose an MCP prompt named `kafka_health_check` that performs a comprehensive health assessment and returns a formatted markdown report. The prompt SHALL accept no arguments and SHALL return sections for broker status, controller status, partition health, consumer group health, and an overall health assessment with status indicators.

#### Scenario: Generate healthy cluster report
- **WHEN** the MCP client invokes the prompt `kafka_health_check` and all systems are healthy
- **THEN** the system returns markdown with check-mark indicators for all sections and "HEALTHY" overall assessment

#### Scenario: Generate unhealthy cluster report
- **WHEN** the MCP client invokes `kafka_health_check` and there are issues (offline brokers, under-replicated partitions, or high consumer lag)
- **THEN** the system returns markdown with warning indicators for affected sections, details of the issues, and recommendations

### Requirement: Under-replicated partitions prompt
The system SHALL expose an MCP prompt named `kafka_under_replicated_partitions` that identifies and reports partitions with insufficient replication. The prompt SHALL accept no arguments and SHALL return a formatted markdown report with a table of affected partitions, possible causes, and step-by-step recommendations.

#### Scenario: No under-replicated partitions
- **WHEN** the MCP client invokes `kafka_under_replicated_partitions` and all partitions are fully replicated
- **THEN** the system returns markdown indicating no under-replicated partitions were found

#### Scenario: Under-replicated partitions found
- **WHEN** the MCP client invokes `kafka_under_replicated_partitions` and under-replicated partitions exist
- **THEN** the system returns markdown with a count, a table (Topic, Partition, Leader, Replica Count, ISR Count, Missing Replicas), a "Possible Causes" section, and a "Recommendations" section

### Requirement: Consumer lag report prompt
The system SHALL expose an MCP prompt named `kafka_consumer_lag_report` that generates a consumer lag analysis report. The prompt SHALL accept an optional `threshold` argument (default: 1000) specifying the message lag threshold for flagging consumer groups. The prompt SHALL return formatted markdown with a consumer group summary table, high lag details, and recommendations.

#### Scenario: Generate lag report with default threshold
- **WHEN** the MCP client invokes `kafka_consumer_lag_report` without a threshold argument
- **THEN** the system uses a default threshold of 1000 and returns markdown with a summary table, details for groups exceeding 1000 messages lag, and recommendations

#### Scenario: Generate lag report with custom threshold
- **WHEN** the MCP client invokes `kafka_consumer_lag_report` with threshold "500"
- **THEN** the system uses 500 as the threshold and flags consumer groups with lag exceeding 500

#### Scenario: No consumer groups
- **WHEN** the MCP client invokes `kafka_consumer_lag_report` and no consumer groups exist
- **THEN** the system returns markdown indicating no consumer groups were found in the cluster
