## 1. Project Scaffolding

- [x] 1.1 Generate Quarkus 3.x Gradle project with Kotlin DSL and Java 21, add quarkus-mcp-server extension and kafka-clients dependency to build.gradle.kts
- [x] 1.2 Create package structure: config/, client/, tools/kafka/, tools/cluster/ under src/main/java/com/github/imcf/mcp/kafka/
- [x] 1.3 Implement ToolHandler interface and BaseToolHandler abstract class with createResponse() helper
- [x] 1.4 Implement KafkaConfig class for Kafka connection properties (bootstrap.servers, security, SASL, SSL)
- [x] 1.5 Implement KafkaClientManager CDI bean with lazy AdminClient, shared Producer, per-call Consumer, and @PreDestroy shutdown

## 2. Configuration

- [x] 2.1 Create application.properties with default values for Kafka connection (localhost:9092), MCP transport (stdio), and logging
- [x] 2.2 Implement McpServerConfig class for MCP-specific settings (transport, tool filtering)
- [x] 2.3 Implement tool allow/block list filtering logic that reads from mcp.tools.allow, mcp.tools.block, and file-based equivalents
- [x] 2.4 Implement conditional tool enablement - disable tools when their backend config is not present

## 3. Kafka Topic Tools

- [x] 3.1 Implement ListTopicsHandler - list all topics, return CSV string
- [x] 3.2 Implement CreateTopicsHandler - create topics with configurable partitions and replication factor
- [x] 3.3 Implement DeleteTopicsHandler - delete topics by name
- [x] 3.4 Implement ProduceMessageHandler - produce records with key, value, headers, partition (no SR)
- [x] 3.5 Implement ConsumeMessagesHandler - consume from topics with maxMessages, timeoutMs, fromBeginning (no SR)
- [x] 3.6 Implement GetTopicConfigHandler - retrieve topic configuration with parameter metadata
- [x] 3.7 Implement AlterTopicConfigHandler - modify topic config with SET/DELETE operations and validateOnly mode

## 4. Cluster Tools

- [x] 4.1 Implement DescribeClusterHandler - return cluster ID, controller, broker list with host:port

## 5. Docker & Deployment

- [x] 5.1 Create Dockerfile with multi-stage build (Quarkus builder JDK 21 + minimal runtime)
- [x] 5.2 Create docker-compose.yml with Kafka (apache/kafka:latest) and MCP server services
- [x] 5.3 Create .env.example with all supported environment variables documented

## 6. Testing

- [x] 6.1 Add Testcontainers dependency and configure Quarkus Dev Services for Kafka
- [x] 6.2 Write integration tests for list-topics, create-topics, delete-topics
- [x] 6.3 Write integration tests for produce-message and consume-messages round-trip
- [x] 6.4 Write integration tests for get-topic-config and alter-topic-config
- [x] 6.5 Write integration test for describe-cluster
- [x] 6.6 Write unit tests for tool allow/block list filtering logic
