## Why

Phase 1 delivered core Kafka tools but produce/consume only work with raw strings. Schema Registry integration is essential for teams using Avro, JSON Schema, or Protobuf - they need to register, browse, and manage schemas, and produce/consume messages with schema-aware serialization. This is also a key compatibility gap with mcp-confluent which supports `list-schemas` and SR-encoded produce/consume.

## What Changes

- Implement Schema Registry REST API client for Confluent Schema Registry OSS
- Add Schema Registry configuration (URL, basic auth)
- Add conditional enablement (disable SR tools when `schema-registry.url` is not set)
- Implement 6 Schema Registry tools:
  - `list-schemas` - List all schemas with optional prefix filter and deleted subjects
  - `register-schema` (NEW) - Register a new schema version for a subject
  - `get-schema` (NEW) - Get schema by subject and version
  - `delete-schema` (NEW) - Delete a schema subject or specific version (soft/hard)
  - `get-schema-compatibility` (NEW) - Get compatibility level for a subject or global
  - `set-schema-compatibility` (NEW) - Set compatibility level
- Enhance `produce-message` with optional Schema Registry serialization (Avro, JSON Schema, Protobuf)
- Enhance `consume-messages` with automatic Schema Registry deserialization
- Implement `search-topics-by-name` - client-side regex/glob filtering via Admin Client
- Add integration tests with Testcontainers (Schema Registry container)

## Capabilities

### New Capabilities
- `schema-registry-client`: REST client for Confluent Schema Registry OSS API
- `schema-registry-tools`: 6 MCP tools for schema management (list, register, get, delete, compatibility)
- `sr-serialization`: Schema Registry serialization/deserialization for produce/consume (Avro, JSON Schema, Protobuf)
- `search-topics`: Client-side topic search by name pattern

### Modified Capabilities

_(none - extending existing tools with optional SR parameters)_

## Impact

- **Dependencies**: Add `org.apache.avro:avro` for Avro support, `com.google.protobuf:protobuf-java` for Protobuf, Quarkus REST client or raw HTTP client for SR REST API
- **Configuration**: New `schema-registry.*` config properties
- **Existing tools**: `produce-message` and `consume-messages` get new optional parameters (backward compatible)
- **Tests**: New Testcontainers dependency for Schema Registry (`confluentinc/cp-schema-registry`)
- **Tool registration filter**: Extend with Schema Registry tool group
