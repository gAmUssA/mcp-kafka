## Context

This is a greenfield Quarkus project that implements an MCP server for open-source Apache Kafka. The reference implementation is `@confluentinc/mcp-confluent` v1.1.0 (Node.js/TypeScript). Phase 1 focuses on core Kafka tools over STDIO transport - the minimum needed for AI assistants like Claude Desktop to manage Kafka topics and messages.

The project follows the architecture defined in `docs/SPEC-OSS-MCP-SERVER.md`, specifically Section 4 (Architecture), Section 5 (Configuration), and Section 13 Phase 1 (Implementation Priority).

## Goals / Non-Goals

**Goals:**
- Scaffold a Quarkus 3.x project with quarkus-mcp-server extension
- Implement STDIO transport for subprocess-based MCP clients
- Deliver 8 working Kafka tools with mcp-confluent-compatible response formats
- Support configuration via environment variables and application.properties
- Provide Docker-based local development and deployment
- Establish patterns (ToolHandler, ClientManager) that later phases will follow

**Non-Goals:**
- HTTP/SSE transports (Phase 5)
- Schema Registry integration in produce/consume (Phase 2)
- Flink SQL Gateway tools (Phase 3)
- Kafka Connect tools (Phase 4)
- GraalVM native-image build (Phase 5)
- API key authentication and DNS rebinding protection (Phase 5)

## Decisions

### 1. Quarkus with quarkus-mcp-server extension

**Decision:** Use the `quarkus-mcp-server` Quarkus extension for MCP protocol handling.

**Rationale:** The extension provides built-in tool registration via `@Tool` annotations, STDIO transport support, and JSON-RPC message handling. This avoids reimplementing the MCP protocol from scratch.

**Alternative considered:** Manual MCP SDK port - rejected because the Quarkus extension is maintained and handles protocol details.

### 2. Gradle with Kotlin DSL build system

**Decision:** Use Gradle with Kotlin DSL (`build.gradle.kts`) as the build tool.

**Rationale:** Gradle offers faster incremental builds, better dependency management with version catalogs, and the Kotlin DSL provides type-safe build scripts with IDE autocompletion. Quarkus has full Gradle support via the `io.quarkus` plugin.

### 3. KafkaClientManager as CDI-managed singleton

**Decision:** Implement `KafkaClientManager` as an `@ApplicationScoped` CDI bean with lazy-initialized AdminClient and Producer, and per-call Consumer creation.

**Rationale:** Mirrors the mcp-confluent `ClientManager` pattern. AdminClient and Producer are thread-safe and expensive to create, so they are shared. Consumers are not thread-safe and need per-session isolation, so they are created per call.

**Alternative considered:** Creating clients per tool call - rejected due to connection overhead.

### 4. Tool response format matching mcp-confluent

**Decision:** All tool responses use the `CallToolResult` format with `TextContent` and `isError` flag. Specific formats: `list-topics` returns CSV, `consume-messages` returns JSON array, etc.

**Rationale:** Drop-in compatibility requires identical response shapes. MCP clients may parse these responses.

### 5. Package structure: `com.github.imcf.mcp.kafka`

**Decision:** Use `com.github.imcf.mcp.kafka` as the base package with sub-packages: `config/`, `client/`, `tools/kafka/`, `tools/cluster/`.

**Rationale:** Matches the module structure from the spec (Section 4.2). Keeps tool handlers organized by domain.

### 6. Testcontainers for integration testing

**Decision:** Use Testcontainers with `apache/kafka` Docker image for integration tests.

**Rationale:** Tests against real Kafka brokers. Quarkus has built-in Testcontainers support via Dev Services, making setup minimal.

## Risks / Trade-offs

- **[quarkus-mcp-server maturity]** The extension may not support all MCP features needed. → Mitigation: Verify STDIO transport works early in scaffolding. Fall back to manual implementation if needed.
- **[Response format drift]** mcp-confluent may change response formats in future versions. → Mitigation: Pin compatibility to v1.1.0. Add compatibility tests that verify response shapes.
- **[Consumer timeout in consume-messages]** Consuming with a timeout blocks the thread. → Mitigation: Use configurable timeout with sensible default (10s). Quarkus event loop is not blocked since tool handlers run on worker threads.
