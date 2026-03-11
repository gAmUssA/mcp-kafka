# mcp-kafka

[![CI](https://github.com/gamussa/mcp-kafka/actions/workflows/ci.yml/badge.svg)](https://github.com/gamussa/mcp-kafka/actions/workflows/ci.yml)
[![Java 21+](https://img.shields.io/badge/Java-21%2B-blue)](https://adoptium.net/)
[![Quarkus](https://img.shields.io/badge/Quarkus-3.27.2-4695EB?logo=quarkus&logoColor=white)](https://quarkus.io/)
[![Kafka](https://img.shields.io/badge/Apache%20Kafka-4.2-231F20?logo=apachekafka&logoColor=white)](https://kafka.apache.org/)
[![Flink](https://img.shields.io/badge/Apache%20Flink-2.2-E6526F?logo=apacheflink&logoColor=white)](https://flink.apache.org/)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![MCP](https://img.shields.io/badge/MCP-compatible-green)](https://modelcontextprotocol.io/)

An open-source MCP (Model Context Protocol) server for Apache Kafka, built with Quarkus. Enables AI assistants like Claude, Goose, and Gemini to interact with Kafka clusters and Schema Registry.

Designed as a drop-in replacement for `@confluentinc/mcp-confluent`, targeting open-source Apache Kafka instead of Confluent Cloud.

## Features

- **22 tools** - Kafka topic management, produce/consume, Schema Registry CRUD, Flink SQL Gateway, topic search, cluster info
- **Schema Registry** - Register, list, get, delete schemas; produce/consume with Avro serialization (Confluent wire format)
- **Dual transport** - STDIO (subprocess) and HTTP/SSE (remote server)
- **Drop-in compatible** - Same MCP tool names and response formats as mcp-confluent
- **Quarkus-powered** - Fast startup, low memory, GraalVM native-image ready
- **Configurable** - Environment variables, properties files, tool allow/block lists
- **Testcontainers** - Integration tests with real Kafka and Schema Registry

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

Start the full stack with one command:

```bash
docker compose up
```

This starts:
- **Kafka** on `localhost:9092`
- **Schema Registry** on `localhost:8081`
- **Flink SQL Gateway** on `localhost:8083`
- **MCP server** on `localhost:8080` (HTTP/SSE transport)

All services are health-checked — the MCP server waits for Kafka, Schema Registry, and Flink SQL Gateway to be ready before starting. All 22 tools are enabled.

Connect an MCP client to `http://localhost:8080/mcp/sse` or use the configuration below for STDIO mode.

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

### With Schema Registry

Add the Schema Registry URL to the environment:

```json
{
  "mcpServers": {
    "kafka": {
      "command": "java",
      "args": ["-Dquarkus.profile=stdio", "-jar", "/path/to/mcp-kafka/build/quarkus-app/quarkus-run.jar"],
      "env": {
        "KAFKA_BOOTSTRAP_SERVERS": "localhost:9092",
        "SCHEMA_REGISTRY_URL": "http://localhost:8081"
      }
    }
  }
}
```

Schema Registry tools are automatically enabled when `SCHEMA_REGISTRY_URL` is set, and disabled otherwise.

### With Flink SQL Gateway

Add the Flink SQL Gateway URL to the environment:

```json
{
  "mcpServers": {
    "kafka": {
      "command": "java",
      "args": ["-Dquarkus.profile=stdio", "-jar", "/path/to/mcp-kafka/build/quarkus-app/quarkus-run.jar"],
      "env": {
        "KAFKA_BOOTSTRAP_SERVERS": "localhost:9092",
        "FLINK_SQL_GATEWAY_URL": "http://localhost:8083"
      }
    }
  }
}
```

Flink SQL Gateway tools are automatically enabled when `FLINK_SQL_GATEWAY_URL` is set, and disabled otherwise.

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

### Schema Registry

| Property                        | Env Var                         | Default    | Description                                |
|---------------------------------|---------------------------------|------------|--------------------------------------------|
| `schema-registry.url`           | `SCHEMA_REGISTRY_URL`           | _(none)_   | Schema Registry URL (enables SR tools)     |
| `schema-registry.auth.username` | `SCHEMA_REGISTRY_AUTH_USERNAME` | _(none)_   | Basic auth username                        |
| `schema-registry.auth.password` | `SCHEMA_REGISTRY_AUTH_PASSWORD` | _(none)_   | Basic auth password                        |

### Flink SQL Gateway

| Property                              | Env Var                               | Default  | Description                                 |
|---------------------------------------|---------------------------------------|----------|---------------------------------------------|
| `flink-sql-gateway.url`               | `FLINK_SQL_GATEWAY_URL`               | _(none)_ | Flink SQL Gateway URL (enables Flink tools) |
| `flink-sql-gateway.default-catalog`   | `FLINK_SQL_GATEWAY_DEFAULT_CATALOG`   | _(none)_ | Default catalog for new sessions            |
| `flink-sql-gateway.default-database`  | `FLINK_SQL_GATEWAY_DEFAULT_DATABASE`  | _(none)_ | Default database for new sessions           |
| `flink-sql-gateway.statement-timeout` | `FLINK_SQL_GATEWAY_STATEMENT_TIMEOUT` | `30000`  | Max wait time for statement completion (ms) |
| `flink-sql-gateway.max-rows`          | `FLINK_SQL_GATEWAY_MAX_ROWS`          | `100`    | Default max result rows per statement       |

### Transport

| Property            | Env Var             | Default     | Description                                      |
|---------------------|---------------------|-------------|--------------------------------------------------|
| `quarkus.profile`   | `QUARKUS_PROFILE`   | _(none)_    | Set to `stdio` for STDIO transport (MCP clients) |
| `quarkus.http.host` | `QUARKUS_HTTP_HOST` | `127.0.0.1` | HTTP bind address (use `0.0.0.0` for remote)     |
| `quarkus.http.port` | `QUARKUS_HTTP_PORT` | `8080`      | HTTP server port                                 |

### Tool Filtering

| Property               | Env Var                | Default  | Description                         |
|------------------------|------------------------|----------|-------------------------------------|
| `mcp.tools-allow`      | `MCP_TOOLS_ALLOW`      | _(none)_ | Comma-separated tool allow-list     |
| `mcp.tools-block`      | `MCP_TOOLS_BLOCK`      | _(none)_ | Comma-separated tool block-list     |
| `mcp.tools-allow-file` | `MCP_TOOLS_ALLOW_FILE` | _(none)_ | File with allow-list (one per line) |
| `mcp.tools-block-file` | `MCP_TOOLS_BLOCK_FILE` | _(none)_ | File with block-list (one per line) |

## Available Tools

### Kafka Tools

| Tool                    | Description                                            |
|-------------------------|--------------------------------------------------------|
| `list-topics`           | List all topics in the Kafka cluster                   |
| `create-topics`         | Create one or more Kafka topics                        |
| `delete-topics`         | Delete one or more Kafka topics                        |
| `produce-message`       | Produce a message (raw or Schema Registry serialized)  |
| `consume-messages`      | Consume messages (raw or Schema Registry deserialized) |
| `get-topic-config`      | Get the configuration for a Kafka topic                |
| `alter-topic-config`    | Alter the configuration for a Kafka topic              |
| `search-topics-by-name` | Search topics by name using regex or substring         |
| `describe-cluster`      | Get Kafka cluster metadata                             |

### Schema Registry Tools

Automatically enabled when `schema-registry.url` is configured.

| Tool                       | Description                                            |
|----------------------------|--------------------------------------------------------|
| `list-schemas`             | List all schemas with optional prefix filter           |
| `register-schema`          | Register a new schema (Avro, JSON Schema, Protobuf)    |
| `get-schema`               | Get a schema by subject and optional version           |
| `delete-schema`            | Soft/hard delete a subject or specific version         |
| `get-schema-compatibility` | Get compatibility level for a subject or global        |
| `set-schema-compatibility` | Set compatibility level (BACKWARD, FORWARD, FULL, etc) |

### Flink SQL Gateway Tools

Automatically enabled when `flink-sql-gateway.url` is configured.

| Tool                    | Description                                            |
|-------------------------|--------------------------------------------------------|
| `execute-flink-sql`     | Execute arbitrary Flink SQL and return results         |
| `list-flink-catalogs`   | List all available Flink catalogs                      |
| `list-flink-databases`  | List databases in a Flink catalog                      |
| `list-flink-tables`     | List tables in a Flink database                        |
| `describe-flink-table`  | Describe the schema of a Flink table                   |
| `get-flink-job-status`  | Get the status of a Flink SQL operation                |
| `cancel-flink-job`      | Cancel a running Flink SQL operation                   |

### Schema Registry Serialization

The `produce-message` and `consume-messages` tools support optional Schema Registry integration:

```
produce-message with useSchemaRegistry=true, schemaType=AVRO, schema=<avro-json>
```

Messages are serialized/deserialized using the Confluent wire format (magic byte + 4-byte schema ID + payload), compatible with any Confluent-ecosystem consumer/producer.

## Development

### Build

```bash
./gradlew quarkusBuild
```

### Run in Dev Mode

```bash
./gradlew quarkusDev
```

Dev mode features:
- Hot reload on source changes
- Dev UI at `http://localhost:8080/q/dev-ui`
- Verbose tool and MCP framework logging

### Run Tests

Tests use Testcontainers with `apache/kafka-native` and `cp-schema-registry` images. Docker must be running.

```bash
./gradlew test
```

### Project Structure

```
src/main/java/com/github/imcf/mcp/kafka/
  config/          # KafkaConfig, SchemaRegistryConfig, ToolFilter, StartupLogger
  client/          # KafkaClientManager, SchemaRegistryClient
  serde/           # SchemaRegistrySerializer, SchemaRegistryDeserializer
  tools/
    BaseToolHandler.java
    kafka/         # Topic tools, produce/consume, search, topic config
    cluster/       # Cluster tools (describe-cluster)
    schema/        # Schema Registry tools (list, register, get, delete, compatibility)
    flink/         # Flink SQL Gateway tools (execute-sql, catalogs, databases, tables)
```

## Roadmap

| Phase       | Scope                                                    | Status  |
|-------------|----------------------------------------------------------|---------|
| **Phase 1** | Core Kafka tools, STDIO + HTTP/SSE transport, Docker     | Done    |
| **Phase 2** | Schema Registry tools, SR serialization, topic search    | Done    |
| **Phase 3** | Flink SQL Gateway tools                                  | Done    |
| **Phase 4** | Kafka Connect tools                                      | Planned |
| **Phase 5** | Consumer groups, API key auth, GraalVM native build      | Planned |

## License

See [LICENSE](LICENSE) for details.
