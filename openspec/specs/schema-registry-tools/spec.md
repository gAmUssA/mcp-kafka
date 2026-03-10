## ADDED Requirements

### Requirement: list-schemas tool
The `list-schemas` tool SHALL list all schemas in the Schema Registry, returning a JSON object mapping subject names to schema details.

#### Scenario: List all schemas
- **WHEN** the tool is called with no parameters
- **THEN** it returns the latest version of each schema subject with id, version, schemaType, and schema text

#### Scenario: Filter by subject prefix
- **WHEN** the tool is called with `subjectPrefix=my-topic`
- **THEN** only subjects starting with `my-topic` are returned

#### Scenario: Include deleted schemas
- **WHEN** the tool is called with `deleted=true`
- **THEN** soft-deleted schemas are included in the results

### Requirement: register-schema tool
The `register-schema` tool SHALL register a new schema version for a subject via the Schema Registry REST API.

#### Scenario: Register an Avro schema
- **WHEN** the tool is called with subject, schema JSON, and schemaType=AVRO
- **THEN** the schema is registered and the assigned schema ID is returned

#### Scenario: Register with references
- **WHEN** the tool is called with a Protobuf schema that has references
- **THEN** the schema and its references are registered

### Requirement: get-schema tool
The `get-schema` tool SHALL retrieve a schema by subject and optional version.

#### Scenario: Get latest schema
- **WHEN** the tool is called with subject only
- **THEN** the latest version of the schema is returned with id, version, schemaType, and schema text

#### Scenario: Get specific version
- **WHEN** the tool is called with subject and version=2
- **THEN** version 2 of the schema is returned

### Requirement: delete-schema tool
The `delete-schema` tool SHALL delete a schema subject or specific version.

#### Scenario: Soft delete entire subject
- **WHEN** the tool is called with subject and no version
- **THEN** the entire subject is soft-deleted

#### Scenario: Hard delete a version
- **WHEN** the tool is called with subject, version, and permanent=true
- **THEN** the specific version is permanently deleted

### Requirement: get-schema-compatibility tool
The `get-schema-compatibility` tool SHALL return the compatibility level for a subject or the global default.

#### Scenario: Get subject compatibility
- **WHEN** the tool is called with a subject name
- **THEN** the subject-level compatibility setting is returned

#### Scenario: Get global compatibility
- **WHEN** the tool is called with no subject
- **THEN** the global compatibility setting is returned

### Requirement: set-schema-compatibility tool
The `set-schema-compatibility` tool SHALL set the compatibility level for a subject or globally.

#### Scenario: Set subject compatibility
- **WHEN** the tool is called with subject and compatibility=BACKWARD
- **THEN** the subject's compatibility level is set to BACKWARD

#### Scenario: Set global compatibility
- **WHEN** the tool is called with no subject and compatibility=FULL
- **THEN** the global compatibility level is set to FULL
