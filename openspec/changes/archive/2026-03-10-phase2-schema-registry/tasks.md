## 1. Schema Registry Client

- [x] 1.1 Add dependencies to build.gradle.kts: Quarkus REST Client, avro, protobuf-java, protobuf-java-util
- [x] 1.2 Create SchemaRegistryConfig (SmallRye ConfigMapping: url, auth.username, auth.password)
- [x] 1.3 Create SchemaRegistryClient CDI bean with REST calls for subjects, versions, schemas, config endpoints
- [x] 1.4 Update ToolRegistrationFilter to disable SR tools when schema-registry.url is not configured
- [x] 1.5 Add schema-registry properties to application.properties and .env.example

## 2. Schema Registry Tools

- [x] 2.1 Implement ListSchemasHandler (list-schemas) - list subjects with optional prefix filter and deleted flag
- [x] 2.2 Implement RegisterSchemaHandler (register-schema) - register schema with subject, schemaType, optional references
- [x] 2.3 Implement GetSchemaHandler (get-schema) - get schema by subject and optional version
- [x] 2.4 Implement DeleteSchemaHandler (delete-schema) - soft/hard delete subject or version
- [x] 2.5 Implement GetSchemaCompatibilityHandler (get-schema-compatibility) - get subject or global compatibility
- [x] 2.6 Implement SetSchemaCompatibilityHandler (set-schema-compatibility) - set compatibility level

## 3. Schema Registry Serialization

- [x] 3.1 Implement SchemaRegistrySerializer - serialize with Confluent wire format (Avro, JSON Schema, Protobuf)
- [x] 3.2 Implement SchemaRegistryDeserializer - deserialize from Confluent wire format
- [x] 3.3 Enhance ProduceMessageHandler with optional SR serialization (useSchemaRegistry, schemaType, schema, subject)
- [x] 3.4 Enhance ConsumeMessagesHandler with optional SR deserialization (useSchemaRegistry, subject)

## 4. Search Topics

- [x] 4.1 Implement SearchTopicsByNameHandler (search-topics-by-name) - client-side regex matching via Admin Client

## 5. Testing

- [x] 5.1 Add Schema Registry Testcontainers setup (cp-schema-registry container depending on Kafka)
- [x] 5.2 Write integration tests for list-schemas, register-schema, get-schema, delete-schema
- [x] 5.3 Write integration tests for get-schema-compatibility, set-schema-compatibility
- [x] 5.4 Write integration tests for produce-message with SR serialization and consume with SR deserialization round-trip
- [x] 5.5 Write integration test for search-topics-by-name
