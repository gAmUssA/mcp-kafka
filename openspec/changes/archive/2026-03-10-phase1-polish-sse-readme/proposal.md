## Why

The Phase 1 MVP only supports STDIO transport, limiting deployment to local subprocess mode. Remote MCP server deployments (containers, VMs, shared team servers) require HTTP/SSE transport. Additionally, the project has no README, making it difficult for users to get started or configure their MCP clients.

## What Changes

- Add `quarkus-mcp-server-sse` dependency alongside `quarkus-mcp-server-stdio` for dual transport support
- Use Quarkus config profiles to switch transports at runtime: default `stdio`, `%http` profile for SSE/Streamable HTTP
- Configure HTTP/SSE endpoints (`/mcp` for Streamable HTTP, `/mcp/sse` for legacy SSE)
- Bind HTTP to `127.0.0.1` by default with configurable host for remote deployments
- Create a comprehensive README.md covering:
  - Project overview (project name: `mcp-kafka`)
  - Quick start with Docker Compose
  - MCP client configuration for Claude Desktop, Claude Code CLI, Goose, and other clients
  - STDIO vs HTTP/SSE transport selection
  - Configuration reference (Kafka, transport, tool filtering)
  - Available tools table
  - Development guide (build, test)
  - Roadmap (phases 2-5)

## Capabilities

### New Capabilities
- `sse-transport`: HTTP/SSE transport support via Quarkus profiles, enabling remote MCP server deployments
- `project-readme`: Comprehensive README with quick start, client configuration, and reference documentation

### Modified Capabilities

_(none)_

## Impact

- **Dependencies**: Adds `quarkus-mcp-server-sse` (already in BOM, no version change)
- **Configuration**: New `%http` profile properties in `application.properties`
- **Documentation**: New `README.md` at project root
- **No breaking changes**: Default behavior remains STDIO
