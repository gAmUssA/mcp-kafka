## ADDED Requirements

### Requirement: Kafka connection configuration
The server SHALL support configuring Kafka connection via environment variables and Quarkus application.properties. Required properties: `kafka.bootstrap.servers` (default: `localhost:9092`). Optional: `kafka.security.protocol`, `kafka.sasl.mechanism`, `kafka.sasl.username`, `kafka.sasl.password`, SSL truststore/keystore settings, and `kafka.properties.file` for additional Kafka client properties.

#### Scenario: Connect with default settings
- **WHEN** the server starts with no Kafka-specific environment variables set
- **THEN** it connects to `localhost:9092` using PLAINTEXT protocol

#### Scenario: Connect with SASL_SSL
- **WHEN** `KAFKA_BOOTSTRAP_SERVERS=broker:9093`, `KAFKA_SECURITY_PROTOCOL=SASL_SSL`, `KAFKA_SASL_MECHANISM=PLAIN`, `KAFKA_SASL_USERNAME=user`, `KAFKA_SASL_PASSWORD=pass` are set
- **THEN** the server connects using SASL_SSL with PLAIN mechanism

### Requirement: Conditional tool enablement
Tools SHALL be automatically disabled when their required backend is not configured. Kafka tools are enabled when `kafka.bootstrap.servers` is set (which it is by default).

#### Scenario: Kafka tools enabled by default
- **WHEN** the server starts with default configuration
- **THEN** all Kafka and cluster tools are listed in `tools/list` response

### Requirement: Tool allow/block list filtering
The server SHALL support filtering enabled tools via `mcp.tools.allow` (comma-separated allow-list), `mcp.tools.block` (comma-separated block-list), `mcp.tools.allow-file`, and `mcp.tools.block-file` configuration properties.

#### Scenario: Block specific tools
- **WHEN** `MCP_TOOLS_BLOCK=delete-topics,alter-topic-config` is set
- **THEN** the `delete-topics` and `alter-topic-config` tools are not listed in `tools/list` and cannot be called

#### Scenario: Allow-list restricts to specific tools
- **WHEN** `MCP_TOOLS_ALLOW=list-topics,describe-cluster` is set
- **THEN** only `list-topics` and `describe-cluster` appear in `tools/list`
