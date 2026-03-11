## Why

AI assistants working with Kafka-based streaming architectures often need to query, inspect, and manage Flink SQL jobs. Apache Flink's SQL Gateway exposes a REST API (v1/v2) that allows clients to open sessions, execute SQL statements, fetch results, and manage operations. Adding Flink SQL Gateway tools to mcp-kafka lets AI assistants run Flink SQL queries, inspect tables/catalogs, and manage streaming jobs — completing the Kafka + Schema Registry + Flink trifecta that powers most real-time data platforms.

## What Changes

- Add a new `FlinkSqlGatewayClient` that wraps the Flink SQL Gateway REST API (v1 endpoints)
- Add a new `FlinkSqlGatewayConfig` configuration interface for connection settings (URL, optional session properties)
- Add MCP tools for Flink SQL Gateway operations:
  - **Session management**: open session, close session, get session info
  - **SQL execution**: execute SQL statement, fetch results, get operation status, cancel operation
  - **Convenience**: list catalogs, list databases, list tables, describe table (all via SQL statements under the hood)
- Add `BaseFlinkToolHandler` following the same pattern as `BaseSchemaToolHandler`
- Auto-disable Flink tools when `flink-sql-gateway.url` is not configured (same pattern as Schema Registry)
- Log Flink SQL Gateway connection details at startup
- Add integration tests using Testcontainers with Flink SQL Gateway

## Capabilities

### New Capabilities
- `flink-sql-gateway-client`: HTTP client wrapper for Flink SQL Gateway REST API v1 (sessions, statements, operations, results)
- `flink-sql-gateway-tools`: MCP tool handlers for executing Flink SQL, managing sessions, and inspecting Flink catalogs/tables

### Modified Capabilities
- (none)

## Impact

- **New dependencies**: `quarkus-rest-client-jackson` (Quarkus REST client for calling Flink SQL Gateway REST API)
- **New config**: `flink-sql-gateway.url`, `flink-sql-gateway.session-properties.*`
- **New packages**: `tools/flink/`, `client/FlinkSqlGatewayClient`, `config/FlinkSqlGatewayConfig`
- **Modified files**: `ToolRegistrationFilter` (add Flink tool set), `StartupLogger` (log Flink config), `README.md` (document new tools)
- **Test infra**: Flink SQL Gateway Testcontainer (official `flink:1.20` image with SQL Gateway enabled)
