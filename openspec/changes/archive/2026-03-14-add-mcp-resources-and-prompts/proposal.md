## Why

The MCP server currently only exposes **tools** (22 total for Kafka, Schema Registry, and Flink). The MCP protocol also defines **resources** and **prompts** as first-class primitives. Resources provide read-only data endpoints that LLM clients can subscribe to for cluster state, while prompts offer pre-built diagnostic templates that guide LLMs through common Kafka operational tasks. Adding these capabilities makes the server more useful for observability and troubleshooting workflows.

## What Changes

- **Add 4 MCP Resources** exposing Kafka cluster state as read-only data:
  - `kafka-mcp://overview` — cluster health summary (brokers, topics, partitions, replication)
  - `kafka-mcp://health-check` — detailed health assessment with actionable insights
  - `kafka-mcp://under-replicated-partitions` — under-replicated partition analysis
  - `kafka-mcp://consumer-lag-report` — consumer group lag metrics with threshold alerting
- **Add 4 MCP Prompts** providing pre-built diagnostic templates:
  - `kafka_cluster_overview` — human-readable cluster summary
  - `kafka_health_check` — comprehensive health check report
  - `kafka_under_replicated_partitions` — under-replication diagnostics
  - `kafka_consumer_lag_report` — consumer lag analysis with configurable threshold
- **Add filtering support** for resources and prompts via configuration, consistent with existing tool filtering

## Capabilities

### New Capabilities
- `kafka-resources`: MCP resource handlers exposing Kafka cluster state as structured JSON data via `@Resource` annotations
- `kafka-prompts`: MCP prompt handlers providing pre-built Kafka diagnostic report templates via `@Prompt` annotations

### Modified Capabilities
<!-- No existing spec-level requirements are changing -->

## Impact

- **New code**: ~8 new handler classes under `src/main/java/.../resources/` and `src/main/java/.../prompts/`
- **Dependencies**: No new dependencies — Quarkiverse MCP SDK 1.10.2 already supports `@Resource`, `@Prompt`, `@PromptArg` annotations
- **Configuration**: New optional config properties for resource/prompt filtering
- **Existing code**: `KafkaClientManager` may need new accessor methods for consumer group and partition metadata
- **Tests**: New integration tests for resource and prompt handlers
