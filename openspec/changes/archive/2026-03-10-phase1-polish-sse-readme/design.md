## Context

Phase 1 MVP is complete with STDIO transport only. The quarkus-mcp-server extension supports multiple transports via separate modules that can coexist in the same build. The `quarkus-mcp-server-sse` module provides both Streamable HTTP (`/mcp`) and legacy SSE (`/mcp/sse`) endpoints.

The project currently has no README. Users need clear guidance on configuring MCP clients (Claude Desktop, Claude Code CLI, Goose) for both local (STDIO) and remote (HTTP/SSE) deployments.

## Goals / Non-Goals

**Goals:**
- Support both STDIO and HTTP/SSE transports in a single build artifact
- Use Quarkus config profiles to switch transport at runtime
- Provide a comprehensive README for the `mcp-kafka` project
- Document MCP client configuration for Claude Desktop and Claude Code CLI

**Non-Goals:**
- WebSocket transport (future consideration)
- API key authentication for HTTP/SSE (Phase 5 scope)
- DNS rebinding protection (Phase 5 scope)

## Decisions

### 1. Both transport dependencies in one build

Include both `quarkus-mcp-server-stdio` and `quarkus-mcp-server-sse` dependencies. The Quarkus extension architecture handles transport activation at build/runtime.

**Alternative considered:** Separate builds per transport. Rejected because it doubles build artifacts and complicates distribution.

### 2. Quarkus profiles for transport selection

- Default profile: STDIO transport (most common use case - Claude Desktop subprocess)
- `%http` profile: HTTP/SSE transport for remote deployments

Users activate via: `java -Dquarkus.profile=http -jar mcp-kafka.jar` or `QUARKUS_PROFILE=http`.

**Alternative considered:** Custom `mcp.transport` config property. Rejected because Quarkus profiles are the idiomatic approach and allow overriding any property per transport mode.

### 3. HTTP binding defaults

- Default host: `127.0.0.1` (localhost only, secure by default)
- Default port: `8080`
- Endpoints: `/mcp` (Streamable HTTP), `/mcp/sse` (legacy SSE)
- For remote access, users set `quarkus.http.host=0.0.0.0`

### 4. README structure

Prioritize "time to first tool call" - get users running within 2 minutes. Configuration examples for the two most common MCP clients (Claude Desktop, Claude Code) come first, followed by reference material.

## Risks / Trade-offs

- **[Risk] STDIO + HTTP in same build may conflict** → The quarkus-mcp-server extension handles this via profile-based activation. If stdio is active, it hijacks stdout; HTTP mode uses the Vert.x HTTP server. Only one should be active at runtime.
- **[Risk] No auth on HTTP/SSE in this change** → Acceptable for Phase 1 polish. Document as localhost-only. Phase 5 adds API key auth.
- **[Trade-off] Single artifact is larger** → Includes both transport modules. Marginal size increase (~100KB) is worth the deployment flexibility.
