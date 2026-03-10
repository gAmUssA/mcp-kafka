## ADDED Requirements

### Requirement: Dual transport support
The project SHALL include both `quarkus-mcp-server-stdio` and `quarkus-mcp-server-sse` dependencies, enabling either transport from a single build artifact.

#### Scenario: Default transport is STDIO
- **WHEN** the application starts without specifying a profile
- **THEN** the MCP server communicates over standard input/output

#### Scenario: HTTP/SSE transport via profile
- **WHEN** the application starts with `quarkus.profile=http`
- **THEN** the MCP server exposes Streamable HTTP at `/mcp` and legacy SSE at `/mcp/sse`

### Requirement: HTTP binding defaults
The HTTP/SSE transport SHALL bind to `127.0.0.1:8080` by default. Users SHALL be able to override the host to `0.0.0.0` for remote access.

#### Scenario: Localhost-only by default
- **WHEN** the application starts with the `http` profile and no host override
- **THEN** the HTTP server binds to `127.0.0.1` and is not reachable from remote hosts

#### Scenario: Remote access enabled
- **WHEN** the application starts with `quarkus.http.host=0.0.0.0`
- **THEN** the HTTP server binds to all interfaces and is reachable from remote hosts

### Requirement: All tools available on both transports
The same set of MCP tools SHALL be available regardless of which transport is active. Tool registration and filtering SHALL work identically.

#### Scenario: Tools listed over HTTP
- **WHEN** a client sends `tools/list` via the `/mcp` HTTP endpoint
- **THEN** the response contains the same tools as when using STDIO transport
