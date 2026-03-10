## ADDED Requirements

### Requirement: Schema Registry REST client
The project SHALL include a `SchemaRegistryClient` CDI bean that communicates with Confluent Schema Registry OSS via its REST API.

#### Scenario: Client created when SR URL is configured
- **WHEN** `schema-registry.url` is set in configuration
- **THEN** a `SchemaRegistryClient` bean is available for injection

### Requirement: Schema Registry configuration
The project SHALL support configuring Schema Registry URL and optional basic auth credentials via SmallRye Config.

#### Scenario: Configure with URL only
- **WHEN** `schema-registry.url=http://localhost:8081` is set
- **THEN** the client connects to the Schema Registry without authentication

#### Scenario: Configure with basic auth
- **WHEN** `schema-registry.url`, `schema-registry.auth.username`, and `schema-registry.auth.password` are set
- **THEN** the client sends Basic auth headers with every request

### Requirement: Conditional tool enablement for SR tools
Schema Registry tools SHALL be automatically disabled when `schema-registry.url` is not configured.

#### Scenario: SR not configured
- **WHEN** `schema-registry.url` is not set
- **THEN** all Schema Registry tools are removed from the MCP tool list at startup
