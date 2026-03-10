## ADDED Requirements

### Requirement: Multi-stage Dockerfile
The project SHALL include a Dockerfile with a multi-stage build. The build stage SHALL use a Quarkus builder image with JDK 21 and Gradle. The runtime stage SHALL use a minimal base image.

#### Scenario: Build Docker image
- **WHEN** the user runs `docker build -t mcp-kafka-oss .`
- **THEN** a Docker image is produced that contains the runnable application

#### Scenario: Run Docker container
- **WHEN** the user runs the built Docker image with `KAFKA_BOOTSTRAP_SERVERS` set
- **THEN** the MCP server starts and is ready to accept STDIO input

### Requirement: docker-compose for local development
The project SHALL include a `docker-compose.yml` that starts Kafka (using `apache/kafka:latest`) and the MCP server. The MCP server container SHALL depend on the Kafka container.

#### Scenario: Start local dev stack
- **WHEN** the user runs `docker compose up`
- **THEN** Kafka starts on port 9092 and the MCP server starts connected to it

### Requirement: Environment variable template
The project SHALL include a `.env.example` file documenting all supported environment variables with example values and comments.

#### Scenario: User copies env template
- **WHEN** the user copies `.env.example` to `.env` and fills in values
- **THEN** the MCP server can be configured by running `docker compose --env-file .env up`
