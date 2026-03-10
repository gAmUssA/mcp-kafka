## Context

Phase 1 is complete with 8 core Kafka tools. The project uses Quarkus 3.27.2, Java 21, Gradle 9, and quarkus-mcp-server 1.10.2. Produce/consume currently handle raw byte[] with UTF-8 string encoding. Schema Registry integration requires a REST client, serialization libraries, and Confluent wire format support.

The Confluent Schema Registry OSS exposes a REST API (Appendix B of spec). All interactions are HTTP-based - no special client library needed.

## Goals / Non-Goals

**Goals:**
- Full Schema Registry CRUD via MCP tools
- Produce/consume with Avro, JSON Schema, and Protobuf via Schema Registry
- Confluent wire format compatibility (magic byte + 4-byte schema ID + payload)
- Drop-in compatible with mcp-confluent's `list-schemas` tool
- Client-side topic search via Admin Client

**Non-Goals:**
- Schema evolution validation (handled by SR server)
- Schema Registry mode management (IMPORT/READWRITE)
- Custom serializers/deserializers beyond SR standard formats

## Decisions

### 1. Schema Registry REST client: Quarkus REST Client

Use Quarkus REST Client (Reactive) to call the Schema Registry REST API. This integrates naturally with CDI, config, and supports basic auth out of the box.

**Alternative considered:** Raw `java.net.http.HttpClient`. Rejected because Quarkus REST Client handles JSON marshalling, error mapping, and config injection automatically.

### 2. Confluent wire format for serialization

When producing with Schema Registry, use the standard Confluent wire format:
```
[magic byte 0x00][4-byte schema ID (big-endian)][payload bytes]
```

For deserialization, detect the magic byte and extract schema ID to look up the schema from SR. This ensures interop with any Confluent-compatible consumer/producer.

Use `org.apache.avro:avro` for Avro serialization/deserialization and `com.google.protobuf:protobuf-java` for Protobuf. JSON Schema payloads are passed through as-is after schema validation by SR.

### 3. Schema Registry config via SmallRye ConfigMapping

```
schema-registry.url=http://localhost:8081
schema-registry.auth.username=
schema-registry.auth.password=
```

Tools are conditionally disabled when `schema-registry.url` is not set, following the same pattern as Kafka tools.

### 4. search-topics-by-name via Admin Client

Implement `search-topics-by-name` using `AdminClient.listTopics()` with client-side regex/glob matching. The spec notes this was originally a Confluent catalog API call but the OSS version uses client-side filtering.

### 5. SchemaRegistryClient as a CDI bean

Create a `SchemaRegistryClient` CDI bean (similar to `KafkaClientManager`) that wraps the Quarkus REST Client interface. This centralizes error handling and provides a clean API for tool handlers.

## Risks / Trade-offs

- **[Risk] Protobuf serialization complexity** → Protobuf requires dynamic message building from `.proto` schema text. Use `com.google.protobuf:protobuf-java-util` for JSON-to-Protobuf conversion via `JsonFormat`. If too complex, can defer Protobuf to a later iteration and start with Avro + JSON Schema only.
- **[Risk] Wire format compatibility** → Must match Confluent's exact byte layout. Well-documented format, low risk.
- **[Trade-off] Adding Avro + Protobuf dependencies increases JAR size** → Necessary for schema format support. Can be optional/profile-based in future if needed.
- **[Risk] Schema Registry container in tests** → Uses `confluentinc/cp-schema-registry` which requires a Kafka broker. Test setup is more complex but Testcontainers handles the orchestration.
