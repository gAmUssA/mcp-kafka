## Context

The MCP server is a Quarkus 3.27.2 Java application using the Quarkiverse MCP Server SDK 1.10.2. It currently implements 22 tools across Kafka, Schema Registry, and Flink SQL Gateway domains. The SDK already supports `@Resource`, `@ResourceTemplate`, `@Prompt`, and `@PromptArg` annotations — no new dependencies are needed.

Resources and prompts are complementary to tools:
- **Resources** expose read-only cluster state data (JSON) that clients can poll or subscribe to
- **Prompts** provide pre-built diagnostic report templates that generate human-readable markdown

## Goals / Non-Goals

**Goals:**
- Add 4 MCP resources for Kafka cluster observability (overview, health check, under-replicated partitions, consumer lag)
- Add 4 MCP prompts for Kafka diagnostic reports (same 4 domains, human-readable markdown output)
- Follow existing project patterns (base handler classes, CDI injection, error handling)
- Support filtering via configuration consistent with existing `mcp.tools-allow`/`mcp.tools-block` pattern

**Non-Goals:**
- Schema Registry or Flink resources/prompts (future work)
- Resource subscriptions or change notifications (MCP spec feature, not needed for v1)
- ResourceTemplate-based dynamic URIs (static resources are sufficient for cluster-wide data)

## Decisions

### 1. Package structure: parallel to tools

**Decision**: Create `src/main/java/.../resources/kafka/` and `src/main/java/.../prompts/kafka/` packages, mirroring the existing `tools/kafka/` structure.

**Rationale**: Follows the established project convention of domain-based sub-packages. Keeps resources and prompts separate from tools for clarity.

**Alternatives considered**:
- Putting resources/prompts inside `tools/kafka/` — rejected: conflates different MCP primitives
- Single flat package for all resources — rejected: won't scale when SR/Flink resources are added later

### 2. Use declarative `@Resource` and `@Prompt` annotations (not programmatic API)

**Decision**: Use method-level `@Resource` and `@Prompt` annotations on CDI bean methods, matching how tools use `@Tool`.

**Rationale**: The Quarkiverse SDK supports both declarative and programmatic registration. Declarative is consistent with the existing tool pattern and requires less boilerplate. The SDK auto-discovers annotated methods.

**Alternatives considered**:
- Programmatic registration via `ResourceManager`/`PromptManager` at `@Startup` — rejected: more code, inconsistent with tool pattern

### 3. Resources return `TextResourceContents` with JSON

**Decision**: All resource handlers return `TextResourceContents` with `application/json` MIME type, using Jackson `ObjectMapper` for serialization.

**Rationale**: JSON is machine-readable and matches the reference implementation's format. `TextResourceContents` is the appropriate return type for text-based content. The project already uses Jackson extensively.

### 4. Prompts return `PromptMessage` with markdown `TextContent`

**Decision**: Prompt handlers return `PromptMessage.withUserRole(new TextContent(...))` containing formatted markdown reports.

**Rationale**: Prompts should produce human-readable diagnostic reports. Markdown renders well in LLM clients. The `withUserRole` factory method follows the SDK convention from the docs.

### 5. Reuse `KafkaClientManager` for cluster metadata

**Decision**: Add helper methods to `KafkaClientManager` for fetching cluster metadata, topic descriptions, consumer group listings, and consumer group offsets. Resources and prompts will inject `KafkaClientManager` like existing tool handlers do.

**Rationale**: Centralizes Kafka client access. The AdminClient already has APIs for `describeCluster()`, `describeTopics()`, `listConsumerGroups()`, `listConsumerGroupOffsets()`, and `describeConsumerGroups()`.

### 6. Filtering via ResourceFilter/PromptFilter (future consideration)

**Decision**: For v1, rely on the Quarkiverse SDK's built-in registration. Filtering can be added later using `ResourceFilter` and `PromptFilter` interfaces if needed.

**Rationale**: Tool filtering required custom `ToolRegistrationFilter` because tools needed conditional disabling based on missing backends. Resources and prompts all depend on Kafka only, so they should be disabled when Kafka config is missing (same check). We can extend `ToolRegistrationFilter` or create a similar startup bean.

## Risks / Trade-offs

- **[Performance]** Resources that query cluster state on every request could be slow for large clusters → **Mitigation**: Keep queries lightweight (metadata only, no message consumption). Consider adding a short TTL cache in future if needed.
- **[Consumer lag overhead]** Listing all consumer groups and their offsets can be expensive → **Mitigation**: The consumer lag resource/prompt only queries on-demand (not polled). Document that it may take a few seconds on clusters with many consumer groups.
- **[SDK annotation limitations]** The `@Resource` annotation requires a static `uri` string → **Mitigation**: Our 4 resources use fixed URIs, so this is not a problem. If dynamic URIs are needed later, use `@ResourceTemplate`.
