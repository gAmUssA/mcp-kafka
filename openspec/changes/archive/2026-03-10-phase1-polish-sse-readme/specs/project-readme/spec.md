## ADDED Requirements

### Requirement: Project README
The project SHALL have a `README.md` at the repository root that covers project overview, quick start, client configuration, and reference documentation.

#### Scenario: User reads README
- **WHEN** a user visits the repository
- **THEN** the README explains what mcp-kafka is, how to build it, and how to configure MCP clients

### Requirement: Claude Desktop configuration
The README SHALL include a complete `claude_desktop_config.json` example for STDIO transport showing Kafka connection via environment variables.

#### Scenario: User configures Claude Desktop
- **WHEN** a user copies the Claude Desktop config example into their config file
- **THEN** Claude Desktop can start the MCP server as a subprocess and use Kafka tools

### Requirement: Claude Code CLI configuration
The README SHALL include configuration for Claude Code CLI using the `.mcp.json` project-level config format.

#### Scenario: User configures Claude Code
- **WHEN** a user adds the `.mcp.json` config to their project
- **THEN** Claude Code CLI detects and connects to the MCP server

### Requirement: HTTP/SSE remote configuration
The README SHALL document how to run the server in HTTP/SSE mode and how to configure MCP clients to connect to a remote server URL.

#### Scenario: User sets up remote MCP server
- **WHEN** a user follows the HTTP/SSE documentation
- **THEN** they can start the server on a remote host and connect MCP clients to it

### Requirement: Available tools reference
The README SHALL include a table listing all available MCP tools with their names and descriptions.

#### Scenario: User checks available tools
- **WHEN** a user reads the tools table
- **THEN** they can see all tool names, descriptions, and which phase they belong to

### Requirement: Configuration reference
The README SHALL include a table of all configuration properties (Kafka connection, transport, tool filtering) with environment variable names, defaults, and descriptions.

#### Scenario: User looks up configuration
- **WHEN** a user needs to configure a specific property
- **THEN** they can find the property name, env var, default value, and description in the reference table
