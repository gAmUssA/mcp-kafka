## ADDED Requirements

### Requirement: Produce with Schema Registry serialization
The `produce-message` tool SHALL support optional Schema Registry serialization for both key and value using the Confluent wire format.

#### Scenario: Produce an Avro-encoded message
- **WHEN** the tool is called with `useSchemaRegistry=true`, `schemaType=AVRO`, and a schema definition
- **THEN** the message is serialized using Avro, registered in SR if needed, and written in Confluent wire format (magic byte + schema ID + payload)

#### Scenario: Produce without Schema Registry
- **WHEN** the tool is called without `useSchemaRegistry`
- **THEN** the message is produced as raw UTF-8 bytes (existing Phase 1 behavior)

### Requirement: Consume with Schema Registry deserialization
The `consume-messages` tool SHALL support automatic deserialization of Schema Registry encoded messages.

#### Scenario: Consume Avro-encoded messages
- **WHEN** consuming messages that use Confluent wire format and `useSchemaRegistry=true`
- **THEN** the magic byte and schema ID are extracted, the schema is fetched from SR, and the payload is deserialized to JSON

#### Scenario: Consume raw messages
- **WHEN** consuming messages without `useSchemaRegistry`
- **THEN** messages are returned as UTF-8 strings (existing Phase 1 behavior)

### Requirement: Confluent wire format compatibility
Serialized messages SHALL use the standard Confluent wire format: 1 magic byte (0x00) + 4-byte big-endian schema ID + serialized payload.

#### Scenario: Wire format interop
- **WHEN** a message is produced with Schema Registry serialization
- **THEN** any Confluent-compatible consumer can deserialize the message
