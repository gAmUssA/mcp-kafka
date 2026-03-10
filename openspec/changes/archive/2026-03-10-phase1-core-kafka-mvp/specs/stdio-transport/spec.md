## ADDED Requirements

### Requirement: STDIO MCP transport
The server SHALL support STDIO transport as the default MCP transport mode. It SHALL read JSON-RPC messages from stdin and write responses to stdout.

#### Scenario: MCP client connects via STDIO
- **WHEN** an MCP client spawns the server as a subprocess and sends a `tools/list` JSON-RPC request via stdin
- **THEN** the server responds with a JSON-RPC response listing all enabled tools on stdout

#### Scenario: Tool invocation via STDIO
- **WHEN** an MCP client sends a `tools/call` JSON-RPC request for `list-topics` via stdin
- **THEN** the server executes the tool and returns the result as a JSON-RPC response on stdout

### Requirement: Graceful shutdown on signals
The server SHALL handle SIGINT and SIGTERM signals and shut down gracefully, closing all Kafka clients.

#### Scenario: Server receives SIGTERM
- **WHEN** the server process receives a SIGTERM signal
- **THEN** the server closes all Kafka client connections and exits with code 0
