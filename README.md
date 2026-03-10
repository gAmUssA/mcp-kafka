# mcp-kafka

An open-source MCP (Model Context Protocol) server for Apache Kafka, built with Quarkus. Enables AI assistants like Claude, Goose, and Gemini to interact with Kafka clusters, Schema Registry, Flink SQL Gateway, and Kafka Connect.

Designed as a drop-in replacement for `@confluentinc/mcp-confluent`, targeting open-source Apache Kafka instead of Confluent Cloud.

## Features

- **8 Kafka tools** - list, create, delete topics; produce and consume messages; get/alter topic config; describe cluster
- **Dual transport** - STDIO (subprocess) and HTTP/SSE (remote server)
- **Drop-in compatible** - Same MCP tool names and response formats as mcp-confluent
- **Quarkus-powered** - Fast startup, low memory, GraalVM native-image ready
- **Configurable** - Environment variables, properties files, tool allow/block lists
- **Testcontainers** - Integration tests with real Kafka (native image for fast startup)

## Quick Start

### Prerequisites

- Java 21+
- Docker (for Kafka or running tests)

### Build from Source

```bash
git clone https://github.com/gamussa/mcp-kafka.git
cd mcp-kafka
./gradlew quarkusBuild
```

The built application is in `build/quarkus-app/`.

### Run with Docker Compose

Start Kafka and the MCP server together:

```bash
docker compose up
```

This starts:
- **Kafka** (native image) on `localhost:9092`
- **MCP server** connected to Kafka

## MCP Client Configuration

### Claude Desktop (STDIO)

Add to your `claude_desktop_config.json`:

```json
{
  "mcpServers": {
    "kafka": {
      "command": "java",
      "args": ["-Dquarkus.profile=stdio", "-jar", "/path/to/mcp-kafka/build/quarkus-app/quarkus-run.jar"],
      "env": {
        "KAFKA_BOOTSTRAP_SERVERS": "localhost:9092"
      }
    }
  }
}
```

### Claude Code CLI

Add a `.mcp.json` file to your project root:

```json
{
  "mcpServers": {
    "kafka": {
      "command": "java",
      "args": ["-Dquarkus.profile=stdio", "-jar", "/path/to/mcp-kafka/build/quarkus-app/quarkus-run.jar"],
      "env": {
        "KAFKA_BOOTSTRAP_SERVERS": "localhost:9092"
      }
    }
  }
}
```

Or configure globally via Claude Code settings.

### Goose / Other MCP Clients (STDIO)

Any MCP client that supports STDIO transport can use:

```
command: java -Dquarkus.profile=stdio -jar /path/to/mcp-kafka/build/quarkus-app/quarkus-run.jar
env:
  KAFKA_BOOTSTRAP_SERVERS: localhost:9092
```

## HTTP/SSE Transport (Remote Server)

For remote deployments, run the server directly (HTTP is the default transport):

```bash
java -Dquarkus.http.host=0.0.0.0 \
     -jar build/quarkus-app/quarkus-run.jar
```

This exposes:
- **Streamable HTTP** at `http://host:8080/mcp`
- **Legacy SSE** at `http://host:8080/mcp/sse`

Configure MCP clients to connect to the remote URL:

```json
{
  "mcpServers": {
    "kafka": {
      "url": "http://your-server:8080/mcp/sse"
    }
  }
}
```

> **Note:** HTTP/SSE mode does not yet include authentication. Bind to `127.0.0.1` (default) or use a reverse proxy for remote access.

## Configuration Reference

### Kafka Connection

| Property                        | Env Var                         | Default          | Description                                      |
|---------------------------------|---------------------------------|------------------|--------------------------------------------------|
| `kafka.bootstrap-servers`       | `KAFKA_BOOTSTRAP_SERVERS`       | `localhost:9092` | Kafka broker addresses                           |
| `kafka.security-protocol`       | `KAFKA_SECURITY_PROTOCOL`       | `PLAINTEXT`      | `PLAINTEXT`, `SASL_PLAINTEXT`, `SSL`, `SASL_SSL` |
| `kafka.sasl-mechanism`          | `KAFKA_SASL_MECHANISM`          | _(none)_         | `PLAIN`, `SCRAM-SHA-256`, `SCRAM-SHA-512`        |
| `kafka.sasl-username`           | `KAFKA_SASL_USERNAME`           | _(none)_         | SASL username                                    |
| `kafka.sasl-password`           | `KAFKA_SASL_PASSWORD`           | _(none)_         | SASL password                                    |
| `kafka.ssl-truststore-location` | `KAFKA_SSL_TRUSTSTORE_LOCATION` | _(none)_         | Path to truststore                               |
| `kafka.ssl-truststore-password` | `KAFKA_SSL_TRUSTSTORE_PASSWORD` | _(none)_         | Truststore password                              |
| `kafka.ssl-keystore-location`   | `KAFKA_SSL_KEYSTORE_LOCATION`   | _(none)_         | Path to keystore (mTLS)                          |
| `kafka.ssl-keystore-password`   | `KAFKA_SSL_KEYSTORE_PASSWORD`   | _(none)_         | Keystore password                                |
| `kafka.properties-file`         | `KAFKA_PROPERTIES_FILE`         | _(none)_         | Path to extra Kafka properties                   |

### Transport

| Property            | Env Var             | Default     | Description                                  |
|---------------------|---------------------|-------------|----------------------------------------------|
| `quarkus.profile`   | `QUARKUS_PROFILE`   | _(none)_    | Set to `stdio` for STDIO transport (MCP clients) |
| `quarkus.http.host` | `QUARKUS_HTTP_HOST` | `127.0.0.1` | HTTP bind address (use `0.0.0.0` for remote) |
| `quarkus.http.port` | `QUARKUS_HTTP_PORT` | `8080`      | HTTP server port                             |

### Tool Filtering

| Property               | Env Var                | Default  | Description                         |
|------------------------|------------------------|----------|-------------------------------------|
| `mcp.tools-allow`      | `MCP_TOOLS_ALLOW`      | _(none)_ | Comma-separated tool allow-list     |
| `mcp.tools-block`      | `MCP_TOOLS_BLOCK`      | _(none)_ | Comma-separated tool block-list     |
| `mcp.tools-allow-file` | `MCP_TOOLS_ALLOW_FILE` | _(none)_ | File with allow-list (one per line) |
| `mcp.tools-block-file` | `MCP_TOOLS_BLOCK_FILE` | _(none)_ | File with block-list (one per line) |

## Available Tools

| Tool                      | Description                               | Status  |
|---------------------------|-------------------------------------------|---------|
| `list-topics`             | List all topics in the Kafka cluster      | Phase 1 |
| `create-topics`           | Create one or more Kafka topics           | Phase 1 |
| `delete-topics`           | Delete one or more Kafka topics           | Phase 1 |
| `produce-message`         | Produce a message to a Kafka topic        | Phase 1 |
| `consume-messages`        | Consume messages from a Kafka topic       | Phase 1 |
| `get-topic-config`        | Get the configuration for a Kafka topic   | Phase 1 |
| `alter-topic-config`      | Alter the configuration for a Kafka topic | Phase 1 |
| `describe-cluster`        | Get Kafka cluster metadata                | Phase 1 |
| `list-schemas`            | List all schemas in Schema Registry       | Phase 2 |
| `register-schema`         | Register a new schema                     | Phase 2 |
| `get-schema`              | Get a schema by subject                   | Phase 2 |
| `delete-schema`           | Delete a schema                           | Phase 2 |
| `search-topics-by-name`   | Search topics by name pattern             | Phase 2 |
| `create-flink-statement`  | Execute a Flink SQL statement             | Phase 3 |
| `list-flink-statements`   | List Flink SQL statements                 | Phase 3 |
| `list-connectors`         | List Kafka Connect connectors             | Phase 4 |
| `create-connector`        | Create a Kafka Connect connector          | Phase 4 |
| `describe-consumer-group` | Get consumer group details and lag        | Phase 5 |

> Tools from Phase 2-5 are planned. See [Roadmap](#roadmap) below.

## Development

### Build

```bash
./gradlew quarkusBuild
```

### Run in Dev Mode

```bash
./gradlew quarkusDev
```

### Run Tests

Tests use Testcontainers with the `apache/kafka-native` image for fast startup. Docker must be running.

```bash
./gradlew test
```

### Project Structure

```
src/main/java/com/github/imcf/mcp/kafka/
  config/          # KafkaConfig, McpServerConfig, ToolFilter
  client/          # KafkaClientManager (Admin, Producer, Consumer lifecycle)
  tools/
    BaseToolHandler.java
    kafka/         # Topic tools (list, create, delete, produce, consume, config)
    cluster/       # Cluster tools (describe-cluster)
```

## Roadmap

| Phase       | Scope                                                    | Status  |
|-------------|----------------------------------------------------------|---------|
| **Phase 1** | Core Kafka tools, STDIO + HTTP/SSE transport, Docker     | Done    |
| **Phase 2** | Schema Registry tools, SR integration in produce/consume | Planned |
| **Phase 3** | Flink SQL Gateway tools                                  | Planned |
| **Phase 4** | Kafka Connect tools                                      | Planned |
| **Phase 5** | Consumer groups, API key auth, GraalVM native build      | Planned |

## License

See [LICENSE](LICENSE) for details.
