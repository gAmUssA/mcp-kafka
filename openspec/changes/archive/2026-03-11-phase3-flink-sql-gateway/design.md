## Context

mcp-kafka is a Quarkus-based MCP server providing Kafka and Schema Registry tools to AI assistants. Phase 3 adds Flink SQL Gateway support, enabling AI assistants to execute Flink SQL queries, inspect streaming tables, and manage Flink jobs through the same MCP server.

The Flink SQL Gateway exposes a REST API (v1) at `/v1/sessions/...` with a session-based interaction model: open a session, execute SQL statements (which return operation handles), poll for status, and fetch results. This is fundamentally different from the Kafka Admin API (single-call) and Schema Registry REST API (stateless CRUD) — it requires session lifecycle management and async operation handling.

The existing project patterns are well-established: `BaseToolHandler` → `Base*ToolHandler` → individual tool handlers, `*Config` interfaces via SmallRye ConfigMapping, `*Client` wrappers for external services, `ToolRegistrationFilter` for conditional tool enablement.

## Goals / Non-Goals

**Goals:**
- Provide MCP tools that let AI assistants execute arbitrary Flink SQL and get results
- Provide convenience tools for common operations (list catalogs, databases, tables, describe table)
- Follow existing project patterns (config, client, base handler, tool registration filter)
- Auto-disable Flink tools when `flink-sql-gateway.url` is not configured
- Support session reuse (one session per MCP server lifetime, lazily created)

**Non-Goals:**
- Flink job management (savepoints, checkpoints, restart strategies) — future phase
- Flink cluster management (TaskManagers, JobManager config) — out of scope
- Streaming result consumption (continuous queries that never terminate) — tools return bounded result sets
- Authentication to Flink SQL Gateway (no standard auth mechanism in OSS Flink SQL Gateway)
- Multi-session management (user-specified sessions) — keep it simple with one auto-managed session

## Decisions

### 1. HTTP client: Quarkus REST Client (Jackson) vs. raw java.net.HttpClient

**Decision**: Use `quarkus-rest-client-jackson` (MicroProfile REST Client).

**Rationale**: Quarkus REST client provides declarative interfaces, automatic JSON serialization, CDI integration, and dev mode support. This is idiomatic Quarkus and avoids the boilerplate of raw `HttpClient`. The Schema Registry client used Confluent's SDK because one exists; there is no equivalent SDK for Flink SQL Gateway, so a REST client is the right approach.

**Alternative considered**: Raw `java.net.HttpClient` — would work but requires manual JSON handling and doesn't integrate with Quarkus configuration.

### 2. Session lifecycle: One shared session vs. per-tool-call sessions

**Decision**: One lazily-created session per MCP server lifetime, stored in the client.

**Rationale**: Opening a session per tool call adds latency and creates unnecessary session churn on the Flink SQL Gateway. A single shared session is simpler and sufficient — the MCP server is typically used by one AI assistant at a time. The session is created on first Flink tool invocation and reused for all subsequent calls. If the session expires or becomes invalid, the client recreates it automatically.

**Alternative considered**: Per-call sessions — simpler state management but slower and creates session leak risk.

### 3. Result fetching: Synchronous polling vs. async

**Decision**: Synchronous polling with timeout.

**Rationale**: MCP tools are request/response. The tool handler submits the SQL, polls for completion (with a configurable timeout), fetches all result pages, and returns the combined result. This matches MCP's synchronous tool model. Long-running queries that exceed the timeout return a timeout error with the operation handle for manual follow-up.

### 4. Tool granularity: Fine-grained (one tool per REST endpoint) vs. coarse-grained (execute-sql + convenience tools)

**Decision**: One primary `execute-flink-sql` tool + convenience wrappers for common operations.

**Rationale**: The Flink SQL Gateway is SQL-based — any operation is just a SQL statement. A single `execute-flink-sql` tool covers all use cases. Convenience tools (`list-flink-catalogs`, `list-flink-databases`, `list-flink-tables`, `describe-flink-table`) simply wrap common SQL patterns (e.g., `SHOW CATALOGS`) to make discovery easier for AI assistants. Session management is hidden from the user (auto-managed).

### 5. Result format: Raw Flink JSON vs. formatted table

**Decision**: Format results as a readable JSON table with column names and row data.

**Rationale**: Raw Flink result format is deeply nested (`results.columns[].name`, `results.data[].fields[]`). The tool should return a clean JSON array of objects (column-name → value maps) that AI assistants can easily parse and present. Include metadata (column types, row count) alongside the data.

## Risks / Trade-offs

- **[Session expiry]** → The Flink SQL Gateway has configurable session idle timeout. Mitigation: catch session-not-found errors and auto-recreate the session, re-executing the failed statement.
- **[Long-running queries]** → Some Flink SQL queries (e.g., streaming SELECTs) never terminate. Mitigation: enforce a result-fetch timeout (default 30s, configurable). Return partial results or timeout error.
- **[Large result sets]** → Unbounded result pagination could overwhelm memory. Mitigation: cap result rows (default 100, configurable via tool parameter `maxRows`).
- **[Flink SQL Gateway availability]** → Not all Flink deployments run the SQL Gateway. Mitigation: tools are auto-disabled when URL not configured; clear error messages when configured but unreachable.
- **[REST API version compatibility]** → Using v1 API endpoints. v2/v3/v4 exist but v1 is the most widely deployed. Mitigation: v1 is stable and backward-compatible in Flink 1.17+.
