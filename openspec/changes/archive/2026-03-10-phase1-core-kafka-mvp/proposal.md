## Why

There is no open-source MCP server for Apache Kafka. The existing `mcp-confluent` is tied to Confluent Cloud APIs and cannot be used with vanilla Apache Kafka clusters. This project creates a Quarkus-based MCP server that serves as a drop-in replacement, enabling AI assistants (Claude Desktop, Goose, Gemini CLI) to interact with open-source Kafka, Schema Registry, Flink SQL Gateway, and Kafka Connect.

Phase 1 delivers the MVP: project scaffolding, STDIO transport, core Kafka topic tools, and Docker support.

## What Changes

- Scaffold a new Quarkus 3.x project with Gradle (Kotlin DSL) and `quarkus-mcp-server` extension, Java 21+
- Implement STDIO MCP transport for subprocess-based MCP clients
- Implement 8 core Kafka tools via Apache Kafka Admin Client and Producer/Consumer APIs:
  - `list-topics` - List all topics (CSV format for mcp-confluent compat)
  - `create-topics` - Create one or more topics
  - `delete-topics` - Delete topics by name
  - `produce-message` - Produce records (without Schema Registry in Phase 1)
  - `consume-messages` - Consume records (without Schema Registry in Phase 1)
  - `get-topic-config` - Retrieve topic configuration
  - `alter-topic-config` - Modify topic configuration
  - `describe-cluster` (NEW) - Get cluster metadata (broker count, controller, cluster ID)
- Implement `KafkaClientManager` CDI bean for Admin/Producer/Consumer lifecycle
- Implement `BaseToolHandler` pattern matching mcp-confluent's tool interface
- Implement configuration via Quarkus SmallRye Config (env vars + application.properties)
- Implement conditional tool enablement (disable tools when backend not configured)
- Implement tool allow/block list filtering
- Add Dockerfile with multi-stage build (JVM + GraalVM native optional)
- Add docker-compose.yml for local development stack (Kafka + MCP server)
- Add Testcontainers-based integration tests

## Capabilities

### New Capabilities
- `project-scaffolding`: Quarkus project setup, build config, module structure, and base abstractions (ToolHandler, BaseToolHandler, KafkaClientManager)
- `stdio-transport`: STDIO MCP transport implementation for subprocess-based clients
- `kafka-topic-tools`: Core Kafka topic management tools (list, create, delete, produce, consume, config)
- `cluster-tools`: Kafka cluster metadata tool (describe-cluster)
- `configuration`: Quarkus SmallRye Config integration, env var mapping, conditional tool enablement, tool filtering
- `docker-deployment`: Dockerfile, docker-compose, and container-based deployment

### Modified Capabilities

_(none - greenfield project)_

## Impact

- **New project**: Entire codebase is new; no existing code affected
- **Build**: Gradle with Kotlin DSL (build.gradle.kts)
- **Dependencies**: Quarkus 3.x, quarkus-mcp-server, Apache Kafka Client 3.x, Jackson, Testcontainers
- **APIs**: Exposes MCP tools over STDIO transport; connects to Kafka brokers via Admin/Producer/Consumer APIs
- **Compatibility**: Tool names, parameter shapes, and response formats match mcp-confluent v1.1.0 for all shared tools
