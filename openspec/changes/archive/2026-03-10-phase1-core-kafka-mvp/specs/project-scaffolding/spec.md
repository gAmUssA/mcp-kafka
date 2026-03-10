## ADDED Requirements

### Requirement: Quarkus project structure
The project SHALL be a Quarkus 3.x application using Java 21+ with Gradle (Kotlin DSL) as the build tool. The project SHALL include the `quarkus-mcp-server` extension as a dependency in `build.gradle.kts`.

#### Scenario: Project compiles and starts
- **WHEN** the user runs `./gradlew quarkusDev`
- **THEN** the application starts successfully and logs the Quarkus banner

#### Scenario: MCP server extension is active
- **WHEN** the application starts
- **THEN** the quarkus-mcp-server extension is loaded and ready to register tools

### Requirement: ToolHandler interface
The project SHALL define a `ToolHandler` interface with a `handle(Map<String, Object> arguments, String sessionId)` method returning `CallToolResult`, and a `BaseToolHandler` abstract class providing a `createResponse(String message, boolean isError)` helper method.

#### Scenario: Tool handler returns success response
- **WHEN** a tool handler calls `createResponse("result", false)`
- **THEN** the returned `CallToolResult` contains a single `TextContent` with the message and `isError` set to false

#### Scenario: Tool handler returns error response
- **WHEN** a tool handler calls `createResponse("Error: something failed", true)`
- **THEN** the returned `CallToolResult` contains a single `TextContent` with the error message and `isError` set to true

### Requirement: KafkaClientManager CDI bean
The project SHALL provide a `KafkaClientManager` as an `@ApplicationScoped` CDI bean that lazily initializes and manages the lifecycle of Kafka AdminClient, KafkaProducer, and KafkaConsumer instances.

#### Scenario: AdminClient is lazily created
- **WHEN** `getAdminClient()` is called for the first time
- **THEN** a new AdminClient is created using the configured bootstrap servers and security settings

#### Scenario: Producer is shared across calls
- **WHEN** `getProducer()` is called multiple times
- **THEN** the same KafkaProducer instance is returned each time

#### Scenario: Consumer is created per call
- **WHEN** `createConsumer(sessionId)` is called
- **THEN** a new KafkaConsumer instance is created with a unique group ID

#### Scenario: Clients are closed on shutdown
- **WHEN** the application shuts down
- **THEN** all managed Kafka clients (AdminClient, Producer) are closed gracefully
