## ADDED Requirements

### Requirement: Flink tools auto-disabled when not configured
The system SHALL disable all Flink SQL Gateway tools when `flink-sql-gateway.url` is not configured. The `ToolRegistrationFilter` SHALL include a `FLINK_TOOLS` set of tool names and skip registration when the Flink SQL Gateway client reports not configured.

#### Scenario: Flink tools hidden when URL not set
- **WHEN** `flink-sql-gateway.url` is not configured
- **THEN** no Flink tools SHALL appear in the MCP tool list

#### Scenario: Flink tools visible when URL set
- **WHEN** `flink-sql-gateway.url` is set to a valid URL
- **THEN** all Flink tools SHALL appear in the MCP tool list

### Requirement: Base Flink tool handler
The system SHALL provide a `BaseFlinkToolHandler` extending `BaseToolHandler` that injects `FlinkSqlGatewayClient` and provides a `requireFlinkGateway()` guard method (same pattern as `BaseSchemaToolHandler.requireSchemaRegistry()`).

#### Scenario: Guard rejects when not configured
- **WHEN** a Flink tool handler calls `requireFlinkGateway()` and the client is not configured
- **THEN** it SHALL return an error `ToolResponse` with a message indicating Flink SQL Gateway is not configured

#### Scenario: Guard passes when configured
- **WHEN** a Flink tool handler calls `requireFlinkGateway()` and the client is configured
- **THEN** it SHALL return `null` (no error)

### Requirement: execute-flink-sql tool
The system SHALL provide an `execute-flink-sql` MCP tool that executes arbitrary Flink SQL statements and returns formatted results.

Parameters:
- `statement` (String, required) — the Flink SQL statement to execute
- `maxRows` (int, optional, default from config) — maximum number of result rows to return

#### Scenario: Execute a SELECT statement
- **WHEN** `execute-flink-sql` is called with `statement="SELECT 1 AS num"`
- **THEN** the tool SHALL return a JSON response containing column metadata and result rows

#### Scenario: Execute a DDL statement
- **WHEN** `execute-flink-sql` is called with `statement="CREATE TABLE ..."`
- **THEN** the tool SHALL return a success message (DDL statements return no result rows)

#### Scenario: Execute with invalid SQL
- **WHEN** `execute-flink-sql` is called with an invalid SQL statement
- **THEN** the tool SHALL return an error response with the Flink error message

#### Scenario: Execute with custom maxRows
- **WHEN** `execute-flink-sql` is called with `maxRows=10`
- **THEN** the tool SHALL return at most 10 result rows

### Requirement: list-flink-catalogs tool
The system SHALL provide a `list-flink-catalogs` MCP tool that lists all available Flink catalogs.

Parameters: (none)

#### Scenario: List catalogs
- **WHEN** `list-flink-catalogs` is called
- **THEN** the tool SHALL execute `SHOW CATALOGS` and return the list of catalog names

### Requirement: list-flink-databases tool
The system SHALL provide a `list-flink-databases` MCP tool that lists databases in a catalog.

Parameters:
- `catalog` (String, optional) — catalog name; if omitted, uses the current catalog

#### Scenario: List databases in current catalog
- **WHEN** `list-flink-databases` is called without a catalog parameter
- **THEN** the tool SHALL execute `SHOW DATABASES` and return the list of database names

#### Scenario: List databases in specific catalog
- **WHEN** `list-flink-databases` is called with `catalog="my_catalog"`
- **THEN** the tool SHALL execute `USE CATALOG my_catalog` followed by `SHOW DATABASES` and return the list

### Requirement: list-flink-tables tool
The system SHALL provide a `list-flink-tables` MCP tool that lists tables in a database.

Parameters:
- `catalog` (String, optional) — catalog name
- `database` (String, optional) — database name; if omitted, uses the current database

#### Scenario: List tables in current database
- **WHEN** `list-flink-tables` is called without parameters
- **THEN** the tool SHALL execute `SHOW TABLES` and return the list of table names

#### Scenario: List tables in specific database
- **WHEN** `list-flink-tables` is called with `database="my_db"`
- **THEN** the tool SHALL execute `USE my_db` followed by `SHOW TABLES` and return the list

### Requirement: describe-flink-table tool
The system SHALL provide a `describe-flink-table` MCP tool that describes the schema of a Flink table.

Parameters:
- `tableName` (String, required) — fully-qualified or simple table name

#### Scenario: Describe an existing table
- **WHEN** `describe-flink-table` is called with `tableName="my_table"`
- **THEN** the tool SHALL execute `DESCRIBE my_table` and return the column names, types, nullability, and other metadata

#### Scenario: Describe a non-existent table
- **WHEN** `describe-flink-table` is called with a table name that does not exist
- **THEN** the tool SHALL return an error response with the Flink error message

### Requirement: get-flink-job-status tool
The system SHALL provide a `get-flink-job-status` MCP tool that shows the status of a Flink SQL operation.

Parameters:
- `operationHandle` (String, required) — the operation handle from a previous `execute-flink-sql` call

#### Scenario: Get status of a completed operation
- **WHEN** `get-flink-job-status` is called with a valid, completed operation handle
- **THEN** the tool SHALL return the operation status (e.g., `FINISHED`)

#### Scenario: Get status of an invalid operation
- **WHEN** `get-flink-job-status` is called with an invalid operation handle
- **THEN** the tool SHALL return an error response

### Requirement: cancel-flink-job tool
The system SHALL provide a `cancel-flink-job` MCP tool that cancels a running Flink SQL operation.

Parameters:
- `operationHandle` (String, required) — the operation handle to cancel

#### Scenario: Cancel a running operation
- **WHEN** `cancel-flink-job` is called with a valid, running operation handle
- **THEN** the tool SHALL cancel the operation and return the resulting status

#### Scenario: Cancel an already-completed operation
- **WHEN** `cancel-flink-job` is called with an operation handle that is already finished
- **THEN** the tool SHALL return the current status without error

### Requirement: Startup logging for Flink SQL Gateway
The `StartupLogger` SHALL log Flink SQL Gateway connection details at server startup when configured.

#### Scenario: Flink configured
- **WHEN** `flink-sql-gateway.url` is set
- **THEN** the startup log SHALL include the Flink SQL Gateway URL and "Flink SQL Gateway tools" in the enabled features list

#### Scenario: Flink not configured
- **WHEN** `flink-sql-gateway.url` is not set
- **THEN** the startup log SHALL show "Flink SQL Gateway: not configured"
