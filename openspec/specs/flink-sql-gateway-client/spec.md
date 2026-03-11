## ADDED Requirements

### Requirement: Flink SQL Gateway configuration
The system SHALL provide a `FlinkSqlGatewayConfig` configuration interface with the following properties:
- `flink-sql-gateway.url` (optional) — the base URL of the Flink SQL Gateway REST API
- `flink-sql-gateway.default-catalog` (optional) — default catalog to set on session creation
- `flink-sql-gateway.default-database` (optional) — default database to set on session creation
- `flink-sql-gateway.session-idle-timeout` (optional) — session idle timeout in milliseconds
- `flink-sql-gateway.statement-timeout` (optional, default 30000) — max time to wait for statement completion in milliseconds
- `flink-sql-gateway.max-rows` (optional, default 100) — default max rows to fetch per statement

#### Scenario: Configuration not set
- **WHEN** `flink-sql-gateway.url` is not configured
- **THEN** `isConfigured()` SHALL return `false`

#### Scenario: Configuration set
- **WHEN** `flink-sql-gateway.url` is set to a valid URL
- **THEN** `isConfigured()` SHALL return `true`

### Requirement: Flink SQL Gateway client initialization
The system SHALL provide a `FlinkSqlGatewayClient` that wraps the Flink SQL Gateway REST API v1 endpoints. The client SHALL be lazily initialized on first use.

#### Scenario: Client created on first use
- **WHEN** a Flink tool is invoked for the first time
- **THEN** the client SHALL create a REST client targeting the configured `flink-sql-gateway.url`

#### Scenario: Client reports not configured
- **WHEN** `flink-sql-gateway.url` is not set and a Flink tool is invoked
- **THEN** the client SHALL throw an `IllegalStateException`

### Requirement: Session lifecycle management
The client SHALL manage a single shared session that is lazily created on first use and reused for all subsequent operations. The session SHALL be auto-recreated if it expires or becomes invalid.

#### Scenario: Session created on first operation
- **WHEN** the first Flink SQL statement is executed
- **THEN** the client SHALL open a new session via `POST /v1/sessions` and store the session handle

#### Scenario: Session reused across operations
- **WHEN** multiple Flink SQL statements are executed sequentially
- **THEN** the client SHALL reuse the same session handle for all statements

#### Scenario: Session auto-recovery on expiry
- **WHEN** a statement execution fails because the session handle is invalid (404 or session-not-found error)
- **THEN** the client SHALL open a new session and retry the failed statement exactly once

### Requirement: SQL statement execution
The client SHALL execute SQL statements via `POST /v1/sessions/{sessionHandle}/statements` and return an operation handle.

#### Scenario: Execute a SQL statement
- **WHEN** `executeStatement(sql)` is called with a valid SQL string
- **THEN** the client SHALL submit the statement and return the operation handle

#### Scenario: Execute with timeout
- **WHEN** `executeStatement(sql, timeoutMs)` is called with a custom timeout
- **THEN** the client SHALL include `executionTimeout` in the request body

### Requirement: Operation status polling
The client SHALL poll `GET /v1/sessions/{sessionHandle}/operations/{operationHandle}/status` until the operation completes, fails, or times out.

#### Scenario: Operation completes successfully
- **WHEN** the operation status transitions to `FINISHED`
- **THEN** the client SHALL stop polling and proceed to fetch results

#### Scenario: Operation fails
- **WHEN** the operation status transitions to `ERROR`
- **THEN** the client SHALL throw an exception with the error details

#### Scenario: Polling timeout
- **WHEN** the operation does not complete within the configured `statement-timeout`
- **THEN** the client SHALL cancel the operation and throw a timeout exception

### Requirement: Result fetching
The client SHALL fetch results via `GET /v1/sessions/{sessionHandle}/operations/{operationHandle}/result/{token}` with pagination support.

#### Scenario: Fetch all result pages
- **WHEN** results span multiple pages (`nextResultUri` is not null)
- **THEN** the client SHALL follow pagination until `resultType` is `EOS` or `nextResultUri` is null

#### Scenario: Enforce max rows
- **WHEN** fetched rows exceed the configured `max-rows` limit
- **THEN** the client SHALL stop fetching and return the rows collected so far with a `truncated: true` flag

#### Scenario: Format results as column-value maps
- **WHEN** results are fetched successfully
- **THEN** the client SHALL transform the raw Flink result format into a list of maps (column name → value) along with column metadata

### Requirement: Operation cancellation
The client SHALL support cancelling an operation via `POST /v1/sessions/{sessionHandle}/operations/{operationHandle}/cancel`.

#### Scenario: Cancel a running operation
- **WHEN** `cancelOperation(operationHandle)` is called
- **THEN** the client SHALL send a cancel request and return the resulting status

### Requirement: Session closing
The client SHALL support closing the managed session via `DELETE /v1/sessions/{sessionHandle}`.

#### Scenario: Close session on shutdown
- **WHEN** the MCP server is shutting down
- **THEN** the client SHALL close the active session if one exists
