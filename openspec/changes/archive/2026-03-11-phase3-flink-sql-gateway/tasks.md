## 1. Configuration & Dependencies

- [x] 1.1 Add `quarkus-rest-client-jackson` dependency to `build.gradle.kts`
- [x] 1.2 Create `FlinkSqlGatewayConfig` SmallRye ConfigMapping interface with `url`, `default-catalog`, `default-database`, `session-idle-timeout`, `statement-timeout`, `max-rows` properties
- [x] 1.3 Add Flink SQL Gateway config entries to `application.properties` (commented out defaults)

## 2. REST Client & Gateway Client

- [x] 2.1 Create `FlinkSqlGatewayApi` Quarkus REST Client interface with methods for sessions, statements, operations, and results endpoints
- [x] 2.2 Create request/response DTOs for Flink SQL Gateway API (session, statement, operation status, result set)
- [x] 2.3 Create `FlinkSqlGatewayClient` CDI bean wrapping the REST client with lazy session management, statement execution, polling, result fetching, and max-rows enforcement
- [x] 2.4 Add session auto-recovery (recreate session on 404/session-not-found and retry)
- [x] 2.5 Add `@PreDestroy` session cleanup in `FlinkSqlGatewayClient`

## 3. Tool Infrastructure

- [x] 3.1 Create `BaseFlinkToolHandler` extending `BaseToolHandler` with `FlinkSqlGatewayClient` injection and `requireFlinkGateway()` guard
- [x] 3.2 Add `FLINK_TOOLS` set to `ToolRegistrationFilter` to auto-disable Flink tools when not configured
- [x] 3.3 Update `StartupLogger` to log Flink SQL Gateway URL and include in enabled features

## 4. MCP Tool Handlers

- [x] 4.1 Create `ExecuteFlinkSqlHandler` — execute arbitrary Flink SQL with `statement` and optional `maxRows` parameters
- [x] 4.2 Create `ListFlinkCatalogsHandler` — execute `SHOW CATALOGS` and return catalog names
- [x] 4.3 Create `ListFlinkDatabasesHandler` — execute `SHOW DATABASES` with optional `catalog` parameter
- [x] 4.4 Create `ListFlinkTablesHandler` — execute `SHOW TABLES` with optional `catalog` and `database` parameters
- [x] 4.5 Create `DescribeFlinkTableHandler` — execute `DESCRIBE <table>` and return column metadata
- [x] 4.6 Create `GetFlinkJobStatusHandler` — get operation status by operation handle
- [x] 4.7 Create `CancelFlinkJobHandler` — cancel a running operation by operation handle

## 5. Integration Tests

- [x] 5.1 Create `FlinkSqlGatewayTestResource` using Testcontainers with Flink SQL Gateway (official `flink` image with SQL Gateway entrypoint)
- [x] 5.2 Create `FlinkSqlGatewayToolsTest` — test execute-flink-sql, list-catalogs, list-databases, list-tables, describe-table
- [x] 5.3 Create `FlinkSqlGatewayClientTest` — test session auto-recovery, timeout handling, max-rows enforcement

## 6. Documentation

- [x] 6.1 Update `README.md` with Flink SQL Gateway tools table, configuration reference, and Phase 3 status
