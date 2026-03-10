# Requirements & Specification: Open-Source Kafka MCP Server (Quarkus)

## Drop-in Replacement for `mcp-confluent`

**Version:** 1.0-DRAFT
**Date:** 2026-03-10

---

## 1. Executive Summary

This document specifies a new MCP (Model Context Protocol) server built with **Quarkus (Java/Kotlin)** that provides AI assistants with tools to interact with **open-source Apache Kafka, Confluent Schema Registry, and Apache Flink SQL Gateway**. It is designed as a **drop-in replacement** for `@confluentinc/mcp-confluent` (v1.1.0), maintaining identical MCP tool names, parameter shapes, and response formats so that any MCP client (Claude Desktop, Goose, Gemini CLI, etc.) can switch without configuration changes beyond updating the server command.

### Key Differences from mcp-confluent

| Aspect           | mcp-confluent (Current)                                           | mcp-kafka-oss (New)                          |
|------------------|-------------------------------------------------------------------|----------------------------------------------|
| Runtime          | Node.js 22 / TypeScript                                           | Quarkus / Java 21+ (GraalVM native optional) |
| Kafka Client     | @confluentinc/kafka-javascript                                    | Apache Kafka Java Client                     |
| Schema Registry  | @confluentinc/schemaregistry                                      | Confluent OSS Schema Registry REST API       |
| Flink            | Confluent Cloud Flink REST API                                    | Apache Flink SQL Gateway REST API            |
| Cloud Management | Confluent Cloud APIs (environments, clusters, billing, tableflow) | **Not applicable** - dropped                 |
| Kafka Connect    | Confluent Cloud Connect API                                       | Apache Kafka Connect REST API (open-source)  |
| Authentication   | Confluent Cloud API keys (Basic Auth)                             | SASL/PLAIN, SASL/SCRAM, mTLS, or no auth     |

---

## 2. Scope: Tool Mapping

### 2.1 Tools to Implement (Direct Equivalents)

These 20 tools have direct open-source equivalents and MUST be implemented with **identical MCP tool names**:

#### Kafka Topic Management (7 tools)

| #   | Tool Name            | Description                                                                      | OSS Backend                                    |
|-----|----------------------|----------------------------------------------------------------------------------|------------------------------------------------|
| 1   | `list-topics`        | List all topics in the Kafka cluster                                             | Kafka Admin Client `listTopics()`              |
| 2   | `create-topics`      | Create one or more Kafka topics                                                  | Kafka Admin Client `createTopics()`            |
| 3   | `delete-topics`      | Delete topics by name                                                            | Kafka Admin Client `deleteTopics()`            |
| 4   | `produce-message`    | Produce records with optional Schema Registry serialization (AVRO/JSON/PROTOBUF) | Kafka Producer + SR REST API                   |
| 5   | `consume-messages`   | Consume messages with optional Schema Registry deserialization                   | Kafka Consumer + SR REST API                   |
| 6   | `alter-topic-config` | Modify topic configuration parameters                                            | Kafka Admin Client `incrementalAlterConfigs()` |
| 7   | `get-topic-config`   | Retrieve topic configuration                                                     | Kafka Admin Client `describeConfigs()`         |

#### Schema Registry (1 tool)

| #   | Tool Name      | Description                                                         | OSS Backend                                      |
|-----|----------------|---------------------------------------------------------------------|--------------------------------------------------|
| 8   | `list-schemas` | List all schemas (latest or all versions, filter by subject prefix) | Schema Registry REST API `/subjects`, `/schemas` |

#### Flink SQL Management (5 tools)

| #   | Tool Name                        | Description                              | OSS Backend                                                     |
|-----|----------------------------------|------------------------------------------|-----------------------------------------------------------------|
| 9   | `create-flink-statement`         | Submit and execute a Flink SQL statement | Flink SQL Gateway REST API `POST /sessions/{id}/statements`     |
| 10  | `list-flink-statements`          | List all statements in session           | Flink SQL Gateway REST API `GET /sessions/{id}/operations`      |
| 11  | `read-flink-statement`           | Read statement status and results        | Flink SQL Gateway `GET /sessions/{id}/operations/{id}/result`   |
| 12  | `delete-flink-statements`        | Cancel/delete a statement                | Flink SQL Gateway `DELETE /sessions/{id}/operations/{id}/close` |
| 13  | `get-flink-statement-exceptions` | Get recent exceptions for a statement    | Flink SQL Gateway error/status endpoints                        |

#### Kafka Connect Management (4 tools)

| #   | Tool Name          | Description                      | OSS Backend                                           |
|-----|--------------------|----------------------------------|-------------------------------------------------------|
| 14  | `list-connectors`  | List active connectors           | Connect REST API `GET /connectors`                    |
| 15  | `read-connector`   | Get connector details and status | Connect REST API `GET /connectors/{name}` + `/status` |
| 16  | `create-connector` | Create a new connector           | Connect REST API `POST /connectors`                   |
| 17  | `delete-connector` | Delete a connector               | Connect REST API `DELETE /connectors/{name}`          |

#### Flink Catalog (via SQL Gateway) (3 tools)

| #   | Tool Name              | Description                 | OSS Backend                              |
|-----|------------------------|-----------------------------|------------------------------------------|
| 18  | `list-flink-catalogs`  | List available catalogs     | Execute `SHOW CATALOGS` via SQL Gateway  |
| 19  | `list-flink-databases` | List databases in a catalog | Execute `SHOW DATABASES` via SQL Gateway |
| 20  | `list-flink-tables`    | List tables in a database   | Execute `SHOW TABLES` via SQL Gateway    |

### 2.2 Tools to Implement (Adapted)

These tools exist in mcp-confluent but need adaptation for open-source:

| #   | Tool Name               | Adaptation Notes                                                                        |
|-----|-------------------------|-----------------------------------------------------------------------------------------|
| 21  | `describe-flink-table`  | Execute `DESCRIBE <table>` via SQL Gateway instead of INFORMATION_SCHEMA query          |
| 22  | `get-flink-table-info`  | Execute `SHOW CREATE TABLE <table>` via SQL Gateway                                     |
| 23  | `search-topics-by-name` | Implement via Kafka Admin Client with client-side regex/glob filtering (no catalog API) |

### 2.3 Tools to Drop (Confluent Cloud Only)

These tools are Confluent Cloud specific with NO open-source equivalent:

| Tool Name                              | Reason for Dropping                      |
|----------------------------------------|------------------------------------------|
| `list-environments`                    | Confluent Cloud organizational concept   |
| `read-environment`                     | Confluent Cloud organizational concept   |
| `list-clusters`                        | Confluent Cloud multi-cluster management |
| `list-billing-costs`                   | Confluent Cloud billing API              |
| `create-topic-tags`                    | Confluent Stream Catalog (proprietary)   |
| `add-tags-to-topic`                    | Confluent Stream Catalog (proprietary)   |
| `delete-tag`                           | Confluent Stream Catalog (proprietary)   |
| `remove-tag-from-entity`               | Confluent Stream Catalog (proprietary)   |
| `list-tags`                            | Confluent Stream Catalog (proprietary)   |
| `search-topics-by-tag`                 | Confluent Stream Catalog (proprietary)   |
| `create-tableflow-topic`               | Confluent Tableflow (proprietary)        |
| `list-tableflow-topics`                | Confluent Tableflow (proprietary)        |
| `read-tableflow-topic`                 | Confluent Tableflow (proprietary)        |
| `update-tableflow-topic`               | Confluent Tableflow (proprietary)        |
| `delete-tableflow-topic`               | Confluent Tableflow (proprietary)        |
| `list-tableflow-regions`               | Confluent Tableflow (proprietary)        |
| `create-tableflow-catalog-integration` | Confluent Tableflow (proprietary)        |
| `list-tableflow-catalog-integrations`  | Confluent Tableflow (proprietary)        |
| `read-tableflow-catalog-integration`   | Confluent Tableflow (proprietary)        |
| `update-tableflow-catalog-integration` | Confluent Tableflow (proprietary)        |
| `delete-tableflow-catalog-integration` | Confluent Tableflow (proprietary)        |
| `check-flink-statement-health`         | Requires Confluent Cloud metrics API     |
| `detect-flink-statement-issues`        | Requires Confluent Cloud metrics API     |
| `get-flink-statement-profile`          | Requires Confluent Cloud telemetry API   |

### 2.4 New Tools (OSS-Specific Additions)

These tools have no mcp-confluent equivalent but add value for open-source Kafka users:

| #   | Tool Name                  | Description                                                 | OSS Backend                                                                  |
|-----|----------------------------|-------------------------------------------------------------|------------------------------------------------------------------------------|
| 24  | `describe-cluster`         | Get cluster metadata (broker count, controller, cluster ID) | Kafka Admin Client `describeCluster()`                                       |
| 25  | `list-consumer-groups`     | List consumer groups                                        | Kafka Admin Client `listConsumerGroups()`                                    |
| 26  | `describe-consumer-group`  | Get consumer group details (members, offsets, lag)          | Kafka Admin Client `describeConsumerGroups()` + `listConsumerGroupOffsets()` |
| 27  | `get-connector-status`     | Get connector and task status (running/failed/paused)       | Connect REST API `GET /connectors/{name}/status`                             |
| 28  | `restart-connector`        | Restart a failed connector or task                          | Connect REST API `POST /connectors/{name}/restart`                           |
| 29  | `pause-connector`          | Pause a running connector                                   | Connect REST API `PUT /connectors/{name}/pause`                              |
| 30  | `resume-connector`         | Resume a paused connector                                   | Connect REST API `PUT /connectors/{name}/resume`                             |
| 31  | `list-connector-plugins`   | List available connector plugins                            | Connect REST API `GET /connector-plugins`                                    |
| 32  | `register-schema`          | Register a new schema for a subject                         | Schema Registry REST API `POST /subjects/{subject}/versions`                 |
| 33  | `get-schema`               | Get schema by subject and version                           | Schema Registry REST API `GET /subjects/{subject}/versions/{version}`        |
| 34  | `delete-schema`            | Delete a schema subject or version                          | Schema Registry REST API `DELETE /subjects/{subject}`                        |
| 35  | `get-schema-compatibility` | Get compatibility level for a subject                       | Schema Registry REST API `GET /config/{subject}`                             |
| 36  | `set-schema-compatibility` | Set compatibility level for a subject                       | Schema Registry REST API `PUT /config/{subject}`                             |

---

## 3. Tool Specifications (Detailed)

### 3.1 Kafka Topic Tools

#### `list-topics`

```
Name: list-topics
Description: List all topics in the Kafka cluster.
Input Parameters: (none)
Returns: CSV string of topic names (matching mcp-confluent format)
```

#### `create-topics`

```
Name: create-topics
Description: Create one or more Kafka topics.
Input Parameters:
  - topicNames: string[] (required) - Names of kafka topics to create
  - numPartitions: number (optional, default: 1) - Number of partitions
  - replicationFactor: number (optional, default: cluster default) - Replication factor
Returns: Success/failure message with topic names
```

#### `delete-topics`

```
Name: delete-topics
Description: Delete the topic with the given names.
Input Parameters:
  - topicNames: string[] (required) - Names of kafka topics to delete
Returns: Confirmation message with deleted topic names
```

#### `produce-message`

```
Name: produce-message
Description: Produce records to a Kafka topic. Supports Schema Registry
             serialization (AVRO, JSON, PROTOBUF) for both key and value.
Input Parameters:
  - topicName: string (required) - Name of the kafka topic
  - value: object (required)
    - message: string | object (required) - The payload to produce
    - useSchemaRegistry: boolean (optional) - Enable schema registry serialization
    - schemaType: enum [AVRO, JSON, PROTOBUF] (optional) - Schema type
    - schema: string (optional) - Schema definition to register
    - subject: string (optional) - Schema Registry subject
  - key: object (optional) - Same structure as value, for message key
  - headers: object (optional) - Key-value pairs for Kafka headers
  - partition: number (optional) - Target partition
Returns: Delivery report with topic, partition, offset
```

#### `consume-messages`

```
Name: consume-messages
Description: Consumes messages from one or more Kafka topics. Supports automatic
             deserialization of Schema Registry encoded messages.
Input Parameters:
  - topicNames: string[] (required) - Names of topics to consume from
  - maxMessages: number (optional, default: 10) - Max messages to consume
  - timeoutMs: number (optional, default: 10000) - Max wait time in ms
  - value: object (required)
    - useSchemaRegistry: boolean (optional) - Enable deserialization
    - subject: string (optional) - Subject name override
  - key: object (optional) - Key deserialization options (same structure)
  - fromBeginning: boolean (optional, default: true) - Start from earliest
Returns: Array of messages with deserialized key/value and metadata
         (partition, offset, timestamp, headers)
```

#### `alter-topic-config`

```
Name: alter-topic-config
Description: Alter topic configuration.
Input Parameters:
  - topicName: string (required) - Name of the topic
  - topicConfigs: array of objects (required)
    - name: string (required) - Config parameter name (e.g., retention.ms)
    - value: string (required) - Config parameter value
    - operation: enum [SET, DELETE] (required) - Operation type
  - validateOnly: boolean (optional, default: false)
Returns: Configuration alteration response
```

**Note:** The mcp-confluent version includes `baseUrl` and `clusterId` parameters because it uses Confluent Cloud's Kafka REST Proxy. The OSS version uses the native Kafka Admin Client directly, so these parameters are dropped.

#### `get-topic-config`

```
Name: get-topic-config
Description: Retrieve configuration details for a specific Kafka topic.
Input Parameters:
  - topicName: string (required) - Name of the topic
Returns: Topic configuration with all parameters, their values,
         sources (DEFAULT_CONFIG, DYNAMIC_TOPIC_CONFIG, etc.), and
         whether they are read-only or sensitive
```

### 3.2 Schema Registry Tools

#### `list-schemas`

```
Name: list-schemas
Description: List all schemas in the Schema Registry.
Input Parameters:
  - latestOnly: boolean (optional, default: true) - Return only latest versions
  - subjectPrefix: string (optional) - Filter subjects by prefix
  - deleted: boolean (optional, default: false) - Include soft-deleted schemas
Returns: JSON object mapping subject names to schema details
         (id, version, schemaType, schema)
```

#### `register-schema` (NEW)

```
Name: register-schema
Description: Register a new schema version for a subject.
Input Parameters:
  - subject: string (required) - Subject name (e.g., "my-topic-value")
  - schema: string (required) - Schema definition (JSON string)
  - schemaType: enum [AVRO, JSON, PROTOBUF] (required)
  - references: array (optional) - Schema references for Protobuf/JSON
  - normalize: boolean (optional, default: false)
Returns: Registered schema ID
```

#### `get-schema` (NEW)

```
Name: get-schema
Description: Get a schema by subject and version.
Input Parameters:
  - subject: string (required) - Subject name
  - version: string (optional, default: "latest") - Version number or "latest"
Returns: Schema details (id, version, schemaType, schema, subject)
```

#### `delete-schema` (NEW)

```
Name: delete-schema
Description: Delete a schema subject or specific version.
Input Parameters:
  - subject: string (required) - Subject name
  - version: string (optional) - If omitted, deletes entire subject
  - permanent: boolean (optional, default: false) - Hard delete
Returns: Deletion confirmation
```

#### `get-schema-compatibility` (NEW)

```
Name: get-schema-compatibility
Description: Get the compatibility level for a subject.
Input Parameters:
  - subject: string (optional) - Subject name (global if omitted)
Returns: Compatibility level (BACKWARD, FORWARD, FULL, NONE, etc.)
```

#### `set-schema-compatibility` (NEW)

```
Name: set-schema-compatibility
Description: Set the compatibility level for a subject.
Input Parameters:
  - subject: string (optional) - Subject name (global if omitted)
  - compatibility: enum [BACKWARD, BACKWARD_TRANSITIVE, FORWARD,
                         FORWARD_TRANSITIVE, FULL, FULL_TRANSITIVE, NONE] (required)
Returns: Updated compatibility level
```

### 3.3 Flink SQL Gateway Tools

**Architecture Note:** Apache Flink SQL Gateway uses a session-based model. The server MUST manage a session pool, creating sessions on first use and reusing them for subsequent requests within the same MCP session.

#### Session Management (Internal)

```
Flink SQL Gateway Session Lifecycle:
1. On first Flink tool call per MCP session: POST /sessions → sessionHandle
2. Cache sessionHandle keyed by MCP sessionId
3. Reuse sessionHandle for all subsequent Flink operations in that MCP session
4. On MCP session close: DELETE /sessions/{sessionHandle}
```

#### `create-flink-statement`

```
Name: create-flink-statement
Description: Execute a Flink SQL statement.
Input Parameters:
  - statement: string (required, max 131072 chars) - The raw Flink SQL text
  - statementName: string (optional) - User-provided label (stored in metadata)
  - catalogName: string (optional) - Catalog to use (default from config)
  - databaseName: string (optional) - Database to use (default from config)
  - executionConfig: object (optional) - Key-value execution properties
Returns: Operation handle, statement status (RUNNING/FINISHED/ERROR)
```

**Mapping:** The mcp-confluent version POSTs to Confluent Cloud's `/sql/v1/.../statements`. The OSS version POSTs to Flink SQL Gateway `POST /v2/sessions/{sessionHandle}/statements`.

#### `list-flink-statements`

```
Name: list-flink-statements
Description: List all statements/operations in the current Flink SQL session.
Input Parameters:
  - statusPhase: string (optional) - Filter by status (RUNNING, FINISHED, ERROR)
Returns: List of operations with handles, status, and SQL text
```

**Mapping:** Confluent Cloud has `GET /sql/v1/.../statements` with server-side filtering. The OSS version queries `GET /v2/sessions/{sessionHandle}/operations` and applies client-side filtering.

#### `read-flink-statement`

```
Name: read-flink-statement
Description: Read a statement's results.
Input Parameters:
  - statementName: string (required) - The operation handle or statement label
  - timeoutInMilliseconds: number (optional, default: 60000) - Pagination timeout
  - maxRows: number (optional, default: 1000) - Max rows to fetch
Returns: Result set with column names, data types, and row data
```

**Mapping:** Uses `GET /v2/sessions/{sessionHandle}/operations/{operationHandle}/result/{token}` with pagination via result tokens.

#### `delete-flink-statements`

```
Name: delete-flink-statements
Description: Cancel and close a Flink SQL statement.
Input Parameters:
  - statementName: string (required) - The operation handle or statement label
Returns: HTTP status code of deletion
```

**Mapping:** Uses `DELETE /v2/sessions/{sessionHandle}/operations/{operationHandle}/close`.

#### `get-flink-statement-exceptions`

```
Name: get-flink-statement-exceptions
Description: Get exceptions for a Flink SQL statement.
Input Parameters:
  - statementName: string (required) - The operation handle or statement label
Returns: Array of exception objects with message, timestamp, and root cause
```

**Mapping:** Check operation status via `GET /v2/sessions/{sessionHandle}/operations/{operationHandle}/status`; if ERROR, extract exception details from the response.

#### `list-flink-catalogs`

```
Name: list-flink-catalogs
Description: List all catalogs available in the Flink environment.
Input Parameters: (none)
Returns: List of catalog names
```

**Implementation:** Execute `SHOW CATALOGS` via SQL Gateway.

#### `list-flink-databases`

```
Name: list-flink-databases
Description: List all databases in a Flink catalog.
Input Parameters:
  - catalogName: string (optional) - Catalog to query (default: current)
Returns: List of database names
```

**Implementation:** Execute `USE CATALOG <name>; SHOW DATABASES;` via SQL Gateway.

#### `list-flink-tables`

```
Name: list-flink-tables
Description: List all tables in a Flink database.
Input Parameters:
  - catalogName: string (optional) - Catalog to query
  - databaseName: string (optional) - Database to query
Returns: List of table names with types
```

**Implementation:** Execute `USE CATALOG <c>; USE <db>; SHOW TABLES;` via SQL Gateway.

#### `describe-flink-table`

```
Name: describe-flink-table
Description: Get full schema details for a Flink table.
Input Parameters:
  - tableName: string (required) - Fully qualified or simple table name
  - catalogName: string (optional)
  - databaseName: string (optional)
Returns: Column names, data types, nullability, keys, watermarks
```

**Implementation:** Execute `DESCRIBE <qualified_table>` via SQL Gateway.

#### `get-flink-table-info`

```
Name: get-flink-table-info
Description: Get extended table metadata.
Input Parameters:
  - tableName: string (required) - Table name
  - catalogName: string (optional)
  - databaseName: string (optional)
Returns: Table DDL including watermarks, distribution, connectors, options
```

**Implementation:** Execute `SHOW CREATE TABLE <qualified_table>` via SQL Gateway.

### 3.4 Kafka Connect Tools

**Backend:** Open-source Kafka Connect REST API (default: `http://localhost:8083`).

#### `list-connectors`

```
Name: list-connectors
Description: List all active connectors.
Input Parameters:
  - expand: string (optional) - Include "status" or "info" in response
Returns: List of connector names (or expanded objects)
```

**Mapping:** `GET /connectors` or `GET /connectors?expand=status`.

#### `read-connector`

```
Name: read-connector
Description: Get connector details including configuration and status.
Input Parameters:
  - connectorName: string (required) - Connector name
Returns: Connector info (name, config, tasks, type) and status
```

**Mapping:** `GET /connectors/{name}` merged with `GET /connectors/{name}/status`.

#### `create-connector`

```
Name: create-connector
Description: Create a new connector.
Input Parameters:
  - connectorName: string (required) - Connector name
  - connectorConfig: object (required)
    - connector.class: string (required) - The connector class
    - topics / topics.regex: string (optional) - Topics to connect
    - Additional key-value config pairs
Returns: Created connector details
```

**Mapping:** `POST /connectors` with body `{"name": "...", "config": {...}}`.

#### `delete-connector`

```
Name: delete-connector
Description: Delete an existing connector.
Input Parameters:
  - connectorName: string (required) - Connector name
Returns: Success confirmation
```

**Mapping:** `DELETE /connectors/{name}`.

#### `get-connector-status` (NEW)

```
Name: get-connector-status
Description: Get detailed connector and task status.
Input Parameters:
  - connectorName: string (required) - Connector name
Returns: Connector state, worker ID, per-task status with error traces
```

#### `restart-connector` (NEW)

```
Name: restart-connector
Description: Restart a connector or specific task.
Input Parameters:
  - connectorName: string (required) - Connector name
  - taskId: number (optional) - Restart specific task (if omitted, restarts connector)
  - includeTasks: boolean (optional, default: false) - Restart all tasks too
  - onlyFailed: boolean (optional, default: false) - Only restart failed tasks
Returns: Success confirmation
```

#### `pause-connector` (NEW)

```
Name: pause-connector
Description: Pause a running connector.
Input Parameters:
  - connectorName: string (required) - Connector name
Returns: Success confirmation
```

#### `resume-connector` (NEW)

```
Name: resume-connector
Description: Resume a paused connector.
Input Parameters:
  - connectorName: string (required) - Connector name
Returns: Success confirmation
```

#### `list-connector-plugins` (NEW)

```
Name: list-connector-plugins
Description: List available connector plugins.
Input Parameters: (none)
Returns: List of plugin classes with type (source/sink) and version
```

### 3.5 Cluster Tools (NEW)

#### `describe-cluster` (NEW)

```
Name: describe-cluster
Description: Get Kafka cluster metadata.
Input Parameters: (none)
Returns: Cluster ID, controller broker, broker list with host:port,
         Kafka version (if available)
```

#### `list-consumer-groups` (NEW)

```
Name: list-consumer-groups
Description: List all consumer groups.
Input Parameters:
  - states: string[] (optional) - Filter by state (STABLE, EMPTY, DEAD, etc.)
Returns: List of consumer groups with state and protocol type
```

#### `describe-consumer-group` (NEW)

```
Name: describe-consumer-group
Description: Get detailed consumer group information.
Input Parameters:
  - groupId: string (required) - Consumer group ID
  - includeOffsets: boolean (optional, default: true) - Include partition offsets and lag
Returns: Group state, coordinator, members (with assignments), partition offsets,
         end offsets, and computed lag per partition
```

---

## 4. Architecture

### 4.1 Technology Stack

| Component         | Technology                                                           |
|-------------------|----------------------------------------------------------------------|
| Framework         | Quarkus 3.x (Java 21+)                                               |
| Build             | Maven or Gradle                                                      |
| MCP Protocol      | quarkus-mcp-server extension (or manual implementation over MCP SDK) |
| Kafka Client      | Apache Kafka Client 3.x (`org.apache.kafka:kafka-clients`)           |
| Schema Registry   | REST client (Confluent Schema Registry OSS REST API)                 |
| Flink SQL Gateway | REST client (Apache Flink SQL Gateway REST API v2)                   |
| Kafka Connect     | REST client (Kafka Connect REST API)                                 |
| HTTP Server       | Quarkus Vert.x (built-in)                                            |
| Serialization     | Jackson JSON + Avro (apache-avro), Protobuf                          |
| Config            | Quarkus SmallRye Config (`application.properties` + env vars)        |
| Native            | GraalVM native-image support (optional, fast startup)                |
| Testing           | Quarkus Test + Testcontainers                                        |

### 4.2 Module Structure

```
mcp-kafka-oss/
├── pom.xml (or build.gradle.kts)
├── src/main/java/com/example/mcp/kafka/
│   ├── McpKafkaApplication.java              # Quarkus entry point
│   │
│   ├── config/
│   │   ├── KafkaConfig.java                  # Kafka connection configuration
│   │   ├── SchemaRegistryConfig.java         # Schema Registry config
│   │   ├── FlinkSqlGatewayConfig.java        # Flink SQL Gateway config
│   │   ├── KafkaConnectConfig.java           # Kafka Connect config
│   │   └── McpServerConfig.java              # MCP transport/auth config
│   │
│   ├── client/
│   │   ├── KafkaClientManager.java           # Kafka Admin/Producer/Consumer lifecycle
│   │   ├── SchemaRegistryClient.java         # SR REST API client
│   │   ├── FlinkSqlGatewayClient.java        # Flink SQL Gateway REST client
│   │   └── KafkaConnectClient.java           # Connect REST API client
│   │
│   ├── tools/
│   │   ├── ToolHandler.java                  # Interface (mirrors mcp-confluent)
│   │   ├── BaseToolHandler.java              # Abstract base with createResponse()
│   │   │
│   │   ├── kafka/
│   │   │   ├── ListTopicsHandler.java
│   │   │   ├── CreateTopicsHandler.java
│   │   │   ├── DeleteTopicsHandler.java
│   │   │   ├── ProduceMessageHandler.java
│   │   │   ├── ConsumeMessagesHandler.java
│   │   │   ├── AlterTopicConfigHandler.java
│   │   │   └── GetTopicConfigHandler.java
│   │   │
│   │   ├── schema/
│   │   │   ├── ListSchemasHandler.java
│   │   │   ├── RegisterSchemaHandler.java
│   │   │   ├── GetSchemaHandler.java
│   │   │   ├── DeleteSchemaHandler.java
│   │   │   ├── GetSchemaCompatibilityHandler.java
│   │   │   └── SetSchemaCompatibilityHandler.java
│   │   │
│   │   ├── flink/
│   │   │   ├── FlinkSessionManager.java      # Session pool management
│   │   │   ├── CreateFlinkStatementHandler.java
│   │   │   ├── ListFlinkStatementsHandler.java
│   │   │   ├── ReadFlinkStatementHandler.java
│   │   │   ├── DeleteFlinkStatementHandler.java
│   │   │   ├── GetFlinkExceptionsHandler.java
│   │   │   ├── ListFlinkCatalogsHandler.java
│   │   │   ├── ListFlinkDatabasesHandler.java
│   │   │   ├── ListFlinkTablesHandler.java
│   │   │   ├── DescribeFlinkTableHandler.java
│   │   │   └── GetFlinkTableInfoHandler.java
│   │   │
│   │   ├── connect/
│   │   │   ├── ListConnectorsHandler.java
│   │   │   ├── ReadConnectorHandler.java
│   │   │   ├── CreateConnectorHandler.java
│   │   │   ├── DeleteConnectorHandler.java
│   │   │   ├── GetConnectorStatusHandler.java
│   │   │   ├── RestartConnectorHandler.java
│   │   │   ├── PauseConnectorHandler.java
│   │   │   ├── ResumeConnectorHandler.java
│   │   │   └── ListConnectorPluginsHandler.java
│   │   │
│   │   ├── cluster/
│   │   │   ├── DescribeClusterHandler.java
│   │   │   ├── ListConsumerGroupsHandler.java
│   │   │   └── DescribeConsumerGroupHandler.java
│   │   │
│   │   └── search/
│   │       └── SearchTopicsByNameHandler.java
│   │
│   ├── transport/
│   │   ├── StdioTransport.java               # STDIO transport
│   │   ├── HttpTransport.java                # Streamable HTTP transport
│   │   ├── SseTransport.java                 # SSE transport
│   │   └── AuthHandler.java                  # API key auth + DNS rebinding protection
│   │
│   └── serialization/
│       ├── SchemaRegistrySerializer.java     # Serialize with SR (AVRO/JSON/PROTOBUF)
│       ├── SchemaRegistryDeserializer.java   # Deserialize with SR
│       └── SerializationFormat.java          # Enum: AVRO, JSON, PROTOBUF
│
├── src/main/resources/
│   ├── application.properties                # Default configuration
│   └── META-INF/
│       └── native-image/                     # GraalVM native-image hints
│
├── src/test/java/                            # Tests
├── Dockerfile                                # Multi-stage Docker build
├── docker-compose.yml                        # Full stack: Kafka + SR + Flink + Connect + MCP
└── .env.example                              # Environment variable template
```

### 4.3 Client Manager Pattern

Mirror the mcp-confluent `ClientManager` interface pattern using CDI:

```java
@ApplicationScoped
public class KafkaClientManager {

    @Inject KafkaConfig kafkaConfig;

    // Lazy-initialized, CDI-managed lifecycle
    private volatile AdminClient adminClient;
    private volatile KafkaProducer<byte[], byte[]> producer;

    public AdminClient getAdminClient() { /* lazy init */ }
    public KafkaProducer<byte[], byte[]> getProducer() { /* lazy init */ }
    public KafkaConsumer<byte[], byte[]> createConsumer(String sessionId) { /* new per call */ }

    @PreDestroy
    void shutdown() { /* close all clients */ }
}
```

### 4.4 Tool Handler Pattern

Mirror the mcp-confluent `BaseToolHandler` pattern:

```java
public interface ToolHandler {
    CallToolResult handle(Map<String, Object> arguments, String sessionId);
    ToolConfig getToolConfig();
    List<String> getRequiredConfigKeys();  // replaces getRequiredEnvVars()
}

public abstract class BaseToolHandler implements ToolHandler {
    protected CallToolResult createResponse(String message, boolean isError) {
        return new CallToolResult(
            List.of(new TextContent(message)),
            isError
        );
    }

    @Override
    public List<String> getRequiredConfigKeys() {
        return List.of();
    }
}
```

### 4.5 Quarkus MCP Server Integration

Use the `quarkus-mcp-server` extension for automatic tool registration:

```java
@MCPTool(name = "list-topics", description = "List all topics in the Kafka cluster.")
public class ListTopicsHandler extends BaseToolHandler {

    @Inject KafkaClientManager kafkaClientManager;

    @Override
    public CallToolResult handle(Map<String, Object> arguments, String sessionId) {
        Set<String> topics = kafkaClientManager.getAdminClient()
            .listTopics().names().get(30, SECONDS);
        return createResponse(String.join(",", topics));
    }
}
```

---

## 5. Configuration

### 5.1 Environment Variables / Properties

All config maps to Quarkus SmallRye Config (`application.properties` or env vars):

#### Kafka Connection

| Property                        | Env Var                         | Default          | Description                                      |
|---------------------------------|---------------------------------|------------------|--------------------------------------------------|
| `kafka.bootstrap.servers`       | `KAFKA_BOOTSTRAP_SERVERS`       | `localhost:9092` | Kafka broker addresses                           |
| `kafka.security.protocol`       | `KAFKA_SECURITY_PROTOCOL`       | `PLAINTEXT`      | `PLAINTEXT`, `SASL_PLAINTEXT`, `SSL`, `SASL_SSL` |
| `kafka.sasl.mechanism`          | `KAFKA_SASL_MECHANISM`          | _(none)_         | `PLAIN`, `SCRAM-SHA-256`, `SCRAM-SHA-512`        |
| `kafka.sasl.username`           | `KAFKA_SASL_USERNAME`           | _(none)_         | SASL username                                    |
| `kafka.sasl.password`           | `KAFKA_SASL_PASSWORD`           | _(none)_         | SASL password                                    |
| `kafka.ssl.truststore.location` | `KAFKA_SSL_TRUSTSTORE_LOCATION` | _(none)_         | Path to truststore                               |
| `kafka.ssl.truststore.password` | `KAFKA_SSL_TRUSTSTORE_PASSWORD` | _(none)_         | Truststore password                              |
| `kafka.ssl.keystore.location`   | `KAFKA_SSL_KEYSTORE_LOCATION`   | _(none)_         | Path to keystore (mTLS)                          |
| `kafka.ssl.keystore.password`   | `KAFKA_SSL_KEYSTORE_PASSWORD`   | _(none)_         | Keystore password                                |
| `kafka.properties.file`         | `KAFKA_PROPERTIES_FILE`         | _(none)_         | Path to .properties file with extra Kafka config |

#### Schema Registry

| Property                        | Env Var                    | Default                 | Description         |
|---------------------------------|----------------------------|-------------------------|---------------------|
| `schema-registry.url`           | `SCHEMA_REGISTRY_URL`      | `http://localhost:8081` | Schema Registry URL |
| `schema-registry.auth.username` | `SCHEMA_REGISTRY_USERNAME` | _(none)_                | Basic auth username |
| `schema-registry.auth.password` | `SCHEMA_REGISTRY_PASSWORD` | _(none)_                | Basic auth password |

#### Flink SQL Gateway

| Property                                 | Env Var                      | Default                 | Description           |
|------------------------------------------|------------------------------|-------------------------|-----------------------|
| `flink.sql-gateway.url`                  | `FLINK_SQL_GATEWAY_URL`      | `http://localhost:8083` | Flink SQL Gateway URL |
| `flink.sql-gateway.default-catalog`      | `FLINK_DEFAULT_CATALOG`      | `default_catalog`       | Default catalog       |
| `flink.sql-gateway.default-database`     | `FLINK_DEFAULT_DATABASE`     | `default_database`      | Default database      |
| `flink.sql-gateway.session-idle-timeout` | `FLINK_SESSION_IDLE_TIMEOUT` | `30m`                   | Session idle timeout  |

#### Kafka Connect

| Property                      | Env Var                  | Default                 | Description            |
|-------------------------------|--------------------------|-------------------------|------------------------|
| `kafka-connect.url`           | `KAFKA_CONNECT_URL`      | `http://localhost:8083` | Kafka Connect REST URL |
| `kafka-connect.auth.username` | `KAFKA_CONNECT_USERNAME` | _(none)_                | Basic auth username    |
| `kafka-connect.auth.password` | `KAFKA_CONNECT_PASSWORD` | _(none)_                | Basic auth password    |

#### MCP Server

| Property                | Env Var                 | Default               | Description                                |
|-------------------------|-------------------------|-----------------------|--------------------------------------------|
| `mcp.transport`         | `MCP_TRANSPORT`         | `stdio`               | `stdio`, `http`, `sse`, or comma-separated |
| `quarkus.http.port`     | `HTTP_PORT`             | `8080`                | HTTP server port                           |
| `quarkus.http.host`     | `HTTP_HOST`             | `127.0.0.1`           | HTTP bind address                          |
| `mcp.http.path`         | `MCP_HTTP_PATH`         | `/mcp`                | HTTP transport endpoint                    |
| `mcp.sse.path`          | `MCP_SSE_PATH`          | `/sse`                | SSE endpoint                               |
| `mcp.sse.messages-path` | `MCP_SSE_MESSAGES_PATH` | `/messages`           | SSE messages endpoint                      |
| `mcp.api-key`           | `MCP_API_KEY`           | _(none)_              | API key for HTTP/SSE auth (64+ hex chars)  |
| `mcp.auth.disabled`     | `MCP_AUTH_DISABLED`     | `false`               | Disable API key auth (dev only)            |
| `mcp.allowed-hosts`     | `MCP_ALLOWED_HOSTS`     | `localhost,127.0.0.1` | DNS rebinding allowed hosts                |
| `mcp.log-level`         | `LOG_LEVEL`             | `INFO`                | Logging level                              |

#### Tool Filtering

| Property               | Env Var                | Default  | Description                         |
|------------------------|------------------------|----------|-------------------------------------|
| `mcp.tools.allow`      | `MCP_TOOLS_ALLOW`      | _(none)_ | Comma-separated allow-list          |
| `mcp.tools.block`      | `MCP_TOOLS_BLOCK`      | _(none)_ | Comma-separated block-list          |
| `mcp.tools.allow-file` | `MCP_TOOLS_ALLOW_FILE` | _(none)_ | File with allow-list (one per line) |
| `mcp.tools.block-file` | `MCP_TOOLS_BLOCK_FILE` | _(none)_ | File with block-list (one per line) |

### 5.2 Conditional Tool Enablement

Tools MUST be automatically disabled when their required backend is not configured:

| Tool Group            | Enabled When                     |
|-----------------------|----------------------------------|
| Kafka tools           | `kafka.bootstrap.servers` is set |
| Schema Registry tools | `schema-registry.url` is set     |
| Flink tools           | `flink.sql-gateway.url` is set   |
| Connect tools         | `kafka-connect.url` is set       |

This mirrors mcp-confluent's `getRequiredEnvVars()` pattern.

---

## 6. Transport Layer

### 6.1 STDIO Transport (Default)

- Read JSON-RPC messages from stdin, write to stdout
- No authentication needed
- Used by Claude Desktop, Goose CLI, etc. as subprocess
- Must handle graceful shutdown on SIGINT/SIGTERM

### 6.2 HTTP Streamable Transport

- POST `{mcp.http.path}` - Send MCP messages
- GET `{mcp.http.path}` - Receive MCP messages (long-poll)
- DELETE `{mcp.http.path}` - Close session
- Session tracking via `mcp-session-id` header
- API key authentication via `cflt-mcp-api-Key` header (maintain header name for compatibility)
- DNS rebinding protection on all requests

### 6.3 SSE Transport

- GET `{mcp.sse.path}` - Establish SSE connection
- POST `{mcp.sse.messages-path}` - Send messages
- Same authentication as HTTP transport

### 6.4 Security (Identical to mcp-confluent)

1. **DNS Rebinding Protection** - Always enforced, validate Host header against allowlist
2. **API Key Authentication** - 64-char hex key, timing-safe comparison
3. **Localhost binding** - Default to 127.0.0.1, not 0.0.0.0
4. **Key generation** - CLI flag `--generate-key` outputs a secure random key

---

## 7. Response Format Compatibility

All tools MUST return responses in the same format as mcp-confluent to ensure drop-in compatibility:

```json
{
  "content": [
    {
      "type": "text",
      "text": "<response string>"
    }
  ],
  "isError": false
}
```

Error responses:
```json
{
  "content": [
    {
      "type": "text",
      "text": "Error: <error message>"
    }
  ],
  "isError": true
}
```

### Key Format Details

- `list-topics` returns CSV: `"topic1,topic2,topic3"`
- `list-connectors` returns CSV: `"connector1,connector2"`
- `consume-messages` returns JSON array of message objects
- `produce-message` returns delivery report text
- Flink tools return JSON objects/arrays
- All Schema Registry tools return JSON

---

## 8. Confluent-to-OSS Concept Mapping

### 8.1 Eliminated Concepts

| Confluent Cloud Concept            | Status     | Notes                                   |
|------------------------------------|------------|-----------------------------------------|
| Environment ID (`env-xxx`)         | Dropped    | No multi-env concept in OSS             |
| Organization ID                    | Dropped    | No org concept in OSS                   |
| Cluster ID (`lkc-xxx`)             | Dropped    | Direct connection via bootstrap servers |
| Compute Pool ID (`lfcp-xxx`)       | Dropped    | Flink resources managed by YARN/K8s     |
| CKU (Confluent Kafka Units)        | Dropped    | No billing units in OSS                 |
| API Key Pairs per service          | Simplified | Single auth config per backend          |
| Regional endpoints                 | Dropped    | Direct endpoint configuration           |
| Qualified Names (`lsrc:lkc:topic`) | Dropped    | Simple topic names                      |
| Stream Governance                  | Dropped    | Use external governance tools           |
| Tableflow                          | Dropped    | Confluent proprietary                   |

### 8.2 Parameter Translation

When a mcp-confluent tool has parameters like `baseUrl`, `environmentId`, `clusterId`, `organizationId` that represent Confluent Cloud concepts, the OSS version:

1. **Drops these parameters** from the tool's input schema
2. **Uses server-side configuration** for connection endpoints
3. Maintains identical tool names and remaining parameter shapes

Example:
```
mcp-confluent create-flink-statement params:
  - baseUrl (dropped - use config)
  - organizationId (dropped - no concept)
  - environmentId (dropped - no concept)
  - computePoolId (dropped - no concept)
  - statement (KEPT)
  - statementName (KEPT)
  - catalogName (KEPT)
  - databaseName (KEPT)

mcp-kafka-oss create-flink-statement params:
  - statement (KEPT)
  - statementName (KEPT)
  - catalogName (KEPT)
  - databaseName (KEPT)
  - executionConfig (NEW - Flink SQL Gateway config properties)
```

---

## 9. Docker Deployment

### 9.1 Dockerfile

```dockerfile
# Stage 1: Build
FROM quay.io/quarkus/ubi-quarkus-mandrel-builder-image:jdk-21 AS build
COPY . /project
WORKDIR /project
RUN ./mvnw package -Pnative -DskipTests

# Stage 2: Runtime
FROM quay.io/quarkus/quarkus-micro-image:2.0
COPY --from=build /project/target/*-runner /application
EXPOSE 8080
CMD ["./application"]
```

### 9.2 docker-compose.yml (Full Stack)

```yaml
services:
  kafka:
    image: apache/kafka:latest
    ports: ["9092:9092"]

  schema-registry:
    image: confluentinc/cp-schema-registry:latest
    ports: ["8081:8081"]
    depends_on: [kafka]

  flink-sql-gateway:
    image: apache/flink:latest
    command: sql-gateway.sh start-foreground
    ports: ["8083:8083"]
    depends_on: [kafka]

  kafka-connect:
    image: apache/kafka-connect:latest
    ports: ["8084:8084"]
    depends_on: [kafka, schema-registry]

  mcp-server:
    build: .
    env_file: .env
    ports: ["${HTTP_PORT:-8080}:${HTTP_PORT:-8080}"]
    depends_on: [kafka, schema-registry, flink-sql-gateway, kafka-connect]
    stdin_open: true
    tty: true
```

---

## 10. CLI Interface

Match mcp-confluent's CLI interface:

```
mcp-kafka-oss [options]

Options:
  -V, --version                    Show version
  -t, --transport <types>          Transport types: http, sse, stdio (default: stdio)
  --allow-tools <tools>            Comma-separated tool allow-list
  --block-tools <tools>            Comma-separated tool block-list
  --allow-tools-file <file>        Load tool allow-list from file
  --block-tools-file <file>        Load tool block-list from file
  --list-tools                     Print enabled tools and exit
  --disable-auth                   Disable HTTP/SSE authentication (dev only)
  --allowed-hosts <hosts>          Comma-separated DNS rebinding whitelist
  --generate-key                   Generate secure API key and exit
  -h, --help                       Show help
```

For Quarkus, implement via Picocli extension or Quarkus CLI.

---

## 11. MCP Client Configuration Examples

### Claude Desktop (`claude_desktop_config.json`)

```json
{
  "mcpServers": {
    "kafka": {
      "command": "mcp-kafka-oss",
      "args": ["--transport", "stdio"],
      "env": {
        "KAFKA_BOOTSTRAP_SERVERS": "localhost:9092",
        "SCHEMA_REGISTRY_URL": "http://localhost:8081",
        "FLINK_SQL_GATEWAY_URL": "http://localhost:8083",
        "KAFKA_CONNECT_URL": "http://localhost:8084"
      }
    }
  }
}
```

### Native binary (GraalVM)

```json
{
  "mcpServers": {
    "kafka": {
      "command": "/usr/local/bin/mcp-kafka-oss",
      "args": ["--transport", "stdio"],
      "env": {
        "KAFKA_BOOTSTRAP_SERVERS": "broker1:9092,broker2:9092",
        "KAFKA_SECURITY_PROTOCOL": "SASL_SSL",
        "KAFKA_SASL_MECHANISM": "SCRAM-SHA-256",
        "KAFKA_SASL_USERNAME": "admin",
        "KAFKA_SASL_PASSWORD": "secret"
      }
    }
  }
}
```

---

## 12. Testing Strategy

### 12.1 Unit Tests

- Test each tool handler with mocked client managers
- Verify input validation and error handling
- Verify response format matches mcp-confluent output

### 12.2 Integration Tests (Testcontainers)

```java
@QuarkusTest
@Testcontainers
class KafkaToolsIntegrationTest {

    @Container
    static KafkaContainer kafka = new KafkaContainer(
        DockerImageName.parse("apache/kafka:latest")
    );

    @Container
    static GenericContainer<?> schemaRegistry = new GenericContainer<>(
        "confluentinc/cp-schema-registry:latest"
    );

    @Test
    void testListTopics() { /* ... */ }

    @Test
    void testProduceAndConsume() { /* ... */ }
}
```

### 12.3 Compatibility Tests

- For each tool that exists in mcp-confluent, verify:
  1. Tool name matches exactly
  2. Required parameters are a subset of mcp-confluent params (no extra required params)
  3. Response format (content/text structure) is compatible
  4. Error response format matches

### 12.4 MCP Protocol Tests

- Test each transport (stdio, HTTP, SSE)
- Test tool listing (`tools/list`)
- Test tool calling (`tools/call`)
- Test session management
- Test authentication and DNS rebinding protection

---

## 13. Implementation Priority

### Phase 1: Core Kafka (MVP)

1. Project scaffolding (Quarkus + MCP server extension)
2. STDIO transport
3. `list-topics`, `create-topics`, `delete-topics`
4. `produce-message` (without Schema Registry)
5. `consume-messages` (without Schema Registry)
6. `get-topic-config`, `alter-topic-config`
7. `describe-cluster` (NEW)
8. Docker support

### Phase 2: Schema Registry

9. `list-schemas`
10. `register-schema`, `get-schema`, `delete-schema` (NEW)
11. `get-schema-compatibility`, `set-schema-compatibility` (NEW)
12. Schema Registry integration in `produce-message` and `consume-messages`
13. `search-topics-by-name`

### Phase 3: Flink SQL

14. Flink SQL Gateway session management
15. `create-flink-statement`, `list-flink-statements`
16. `read-flink-statement`, `delete-flink-statements`
17. `get-flink-statement-exceptions`
18. `list-flink-catalogs`, `list-flink-databases`, `list-flink-tables`
19. `describe-flink-table`, `get-flink-table-info`

### Phase 4: Kafka Connect

20. `list-connectors`, `read-connector`
21. `create-connector`, `delete-connector`
22. `get-connector-status`, `restart-connector` (NEW)
23. `pause-connector`, `resume-connector` (NEW)
24. `list-connector-plugins` (NEW)

### Phase 5: Advanced Features

25. HTTP and SSE transports
26. API key authentication + DNS rebinding protection
27. `list-consumer-groups`, `describe-consumer-group` (NEW)
28. GraalVM native-image build
29. Tool allow/block list filtering
30. Comprehensive Testcontainers test suite

---

## 14. Open Questions

1. **Quarkus MCP Extension Maturity:** Verify `quarkus-mcp-server` extension supports all three transports (stdio, HTTP, SSE) and tool/resource registration. If not, implement MCP protocol manually using `@modelcontextprotocol/sdk` concepts ported to Java.

2. **Flink SQL Gateway API Version:** Confirm the target Flink version (1.18+/1.19+/2.0) and corresponding SQL Gateway REST API version (v1 vs v2). The spec assumes v2.

3. **Schema Registry Wire Format Compatibility:** When producing messages with Schema Registry, must use the Confluent wire format (magic byte + 4-byte schema ID + payload) for interop with existing consumers. Verify if there's a Java library for this or if custom serialization is needed.

4. **Kafka Connect Multi-Cluster:** Should the server support configuring multiple Kafka Connect clusters? mcp-confluent supports this via per-request `baseUrl` parameter. The OSS version could support a `connectCluster` parameter or multiple named endpoints.

5. **MCP Resources & Prompts:** mcp-confluent implements zero MCP Resources and zero MCP Prompts. Should the OSS version add these? For example, a `kafka://topics` resource that lists topics, or a `debug-consumer-lag` prompt template.

6. **Backward Compatibility Header:** The API key header is named `cflt-mcp-api-Key` in mcp-confluent. Should the OSS version keep this vendor-specific name for compatibility, or use a generic name like `X-MCP-API-Key` with backward-compat support for the old name?

---

## Appendix A: Flink SQL Gateway REST API Reference

Key endpoints used by Flink tools:

```
POST   /v2/sessions                                          # Create session
DELETE /v2/sessions/{sessionHandle}                           # Close session
POST   /v2/sessions/{sessionHandle}/statements                # Execute statement
GET    /v2/sessions/{sessionHandle}/operations/{operationHandle}/status   # Get status
GET    /v2/sessions/{sessionHandle}/operations/{operationHandle}/result/{token}  # Get results
DELETE /v2/sessions/{sessionHandle}/operations/{operationHandle}/close    # Cancel operation
```

## Appendix B: Schema Registry REST API Reference

Key endpoints used by Schema Registry tools:

```
GET    /subjects                                              # List all subjects
GET    /subjects/{subject}/versions                           # List versions
GET    /subjects/{subject}/versions/{version}                 # Get schema
GET    /subjects/{subject}/versions/{version}/schema          # Get raw schema
POST   /subjects/{subject}/versions                           # Register schema
DELETE /subjects/{subject}                                    # Soft-delete subject
DELETE /subjects/{subject}?permanent=true                     # Hard-delete subject
DELETE /subjects/{subject}/versions/{version}                 # Delete version
GET    /config                                                # Global compatibility
GET    /config/{subject}                                      # Subject compatibility
PUT    /config/{subject}                                      # Set compatibility
GET    /schemas/ids/{id}                                      # Get schema by global ID
```

## Appendix C: Kafka Connect REST API Reference

Key endpoints used by Connect tools:

```
GET    /connectors                                            # List connectors
POST   /connectors                                            # Create connector
GET    /connectors/{name}                                     # Get connector info
GET    /connectors/{name}/config                              # Get connector config
PUT    /connectors/{name}/config                              # Update connector config
DELETE /connectors/{name}                                     # Delete connector
GET    /connectors/{name}/status                              # Get connector status
POST   /connectors/{name}/restart                             # Restart connector
PUT    /connectors/{name}/pause                               # Pause connector
PUT    /connectors/{name}/resume                              # Resume connector
POST   /connectors/{name}/tasks/{taskId}/restart              # Restart task
GET    /connector-plugins                                     # List plugins
```

## Appendix D: mcp-confluent Tool Name Compatibility Matrix

| mcp-confluent Tool               | OSS Tool                         | Status                                   |
|----------------------------------|----------------------------------|------------------------------------------|
| `list-topics`                    | `list-topics`                    | Identical                                |
| `create-topics`                  | `create-topics`                  | Identical                                |
| `delete-topics`                  | `delete-topics`                  | Identical                                |
| `produce-message`                | `produce-message`                | Identical (SR wire format compat)        |
| `consume-messages`               | `consume-messages`               | Identical (SR wire format compat)        |
| `alter-topic-config`             | `alter-topic-config`             | Params simplified (no baseUrl/clusterId) |
| `get-topic-config`               | `get-topic-config`               | Params simplified (no baseUrl/clusterId) |
| `list-schemas`                   | `list-schemas`                   | Identical                                |
| `create-flink-statement`         | `create-flink-statement`         | Params adapted (no org/env/pool IDs)     |
| `list-flink-statements`          | `list-flink-statements`          | Params adapted                           |
| `read-flink-statement`           | `read-flink-statement`           | Params adapted                           |
| `delete-flink-statements`        | `delete-flink-statements`        | Params adapted                           |
| `get-flink-statement-exceptions` | `get-flink-statement-exceptions` | Params adapted                           |
| `list-flink-catalogs`            | `list-flink-catalogs`            | Params simplified                        |
| `list-flink-databases`           | `list-flink-databases`           | Params simplified                        |
| `list-flink-tables`              | `list-flink-tables`              | Params simplified                        |
| `describe-flink-table`           | `describe-flink-table`           | Params simplified                        |
| `get-flink-table-info`           | `get-flink-table-info`           | Params simplified                        |
| `list-connectors`                | `list-connectors`                | Params simplified (no env/cluster IDs)   |
| `read-connector`                 | `read-connector`                 | Params simplified                        |
| `create-connector`               | `create-connector`               | Params simplified                        |
| `delete-connector`               | `delete-connector`               | Params simplified                        |
| `search-topics-by-name`          | `search-topics-by-name`          | Reimplemented (Admin Client)             |
| `list-environments`              | _(dropped)_                      | Cloud-only                               |
| `read-environment`               | _(dropped)_                      | Cloud-only                               |
| `list-clusters`                  | _(dropped)_                      | Cloud-only                               |
| `list-billing-costs`             | _(dropped)_                      | Cloud-only                               |
| `create-topic-tags`              | _(dropped)_                      | Cloud-only                               |
| `add-tags-to-topic`              | _(dropped)_                      | Cloud-only                               |
| `delete-tag`                     | _(dropped)_                      | Cloud-only                               |
| `remove-tag-from-entity`         | _(dropped)_                      | Cloud-only                               |
| `list-tags`                      | _(dropped)_                      | Cloud-only                               |
| `search-topics-by-tag`           | _(dropped)_                      | Cloud-only                               |
| `check-flink-statement-health`   | _(dropped)_                      | Cloud-only                               |
| `detect-flink-statement-issues`  | _(dropped)_                      | Cloud-only                               |
| `get-flink-statement-profile`    | _(dropped)_                      | Cloud-only                               |
| All Tableflow tools (11)         | _(dropped)_                      | Cloud-only                               |
