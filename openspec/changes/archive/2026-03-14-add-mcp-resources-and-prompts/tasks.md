## 1. KafkaClientManager Extensions

- [x] 1.1 Add `getClusterOverview()` method to KafkaClientManager that returns broker count, controller ID, topic count, partition count, under-replicated partitions, offline partitions, and health status
- [x] 1.2 Add `getConsumerGroupLag(long threshold)` method to KafkaClientManager that returns consumer group summaries with lag metrics and high-lag details
- [x] 1.3 Add `getUnderReplicatedPartitions()` method to KafkaClientManager that returns per-partition replication details for partitions where ISR < replication factor

## 2. Resource Handlers

- [x] 2.1 Create `BaseResourceHandler` class in `resources/` package with common error handling and JSON serialization (reuse patterns from `BaseToolHandler`)
- [x] 2.2 Implement `ClusterOverviewResource` with `@Resource(uri = "kafka-mcp://overview")` returning cluster health summary JSON
- [x] 2.3 Implement `HealthCheckResource` with `@Resource(uri = "kafka-mcp://health-check")` returning detailed health assessment JSON
- [x] 2.4 Implement `UnderReplicatedPartitionsResource` with `@Resource(uri = "kafka-mcp://under-replicated-partitions")` returning under-replication analysis JSON
- [x] 2.5 Implement `ConsumerLagReportResource` with `@Resource(uri = "kafka-mcp://consumer-lag-report")` returning consumer lag metrics JSON

## 3. Prompt Handlers

- [x] 3.1 Create `BasePromptHandler` class in `prompts/` package with common markdown formatting utilities and error handling
- [x] 3.2 Implement `ClusterOverviewPrompt` with `@Prompt(name = "kafka_cluster_overview")` returning formatted markdown cluster summary
- [x] 3.3 Implement `HealthCheckPrompt` with `@Prompt(name = "kafka_health_check")` returning markdown health check report with status indicators
- [x] 3.4 Implement `UnderReplicatedPartitionsPrompt` with `@Prompt(name = "kafka_under_replicated_partitions")` returning markdown under-replication report with table and recommendations
- [x] 3.5 Implement `ConsumerLagReportPrompt` with `@Prompt(name = "kafka_consumer_lag_report")` accepting optional `@PromptArg threshold` (default 1000) and returning markdown lag analysis

## 4. Configuration & Filtering

- [x] 4.1 Extend startup filtering logic to disable resources and prompts when Kafka is not configured (similar to existing tool disabling in `ToolRegistrationFilter`)

## 5. Smoke Tests (TestContainers)

- [x] 5.1 Create `KafkaResourcesTest` (`@QuarkusTest` + `@QuarkusTestResource(KafkaTestResource.class)`) — inject all 4 resource handlers, verify each returns non-error `TextResourceContents` with valid JSON containing expected top-level keys (timestamp, health_status, etc.)
- [x] 5.2 Create `KafkaPromptsTest` (`@QuarkusTest` + `@QuarkusTestResource(KafkaTestResource.class)`) — inject all 4 prompt handlers, verify each returns a non-null `PromptMessage` with `TextContent` containing expected markdown headings
- [x] 5.3 Add `testConsumerLagWithProducedMessages` in `KafkaPromptsTest` — create topic, produce messages without consuming, invoke `kafka_consumer_lag_report` prompt, verify lag is reported
- [x] 5.4 Add `testConsumerLagReportCustomThreshold` in `KafkaPromptsTest` — invoke with threshold="5", verify threshold value appears in output
- [x] 5.5 Add `testOverviewReflectsCreatedTopics` in `KafkaResourcesTest` — create a topic, then read `kafka-mcp://overview`, verify topic_count includes the new topic
