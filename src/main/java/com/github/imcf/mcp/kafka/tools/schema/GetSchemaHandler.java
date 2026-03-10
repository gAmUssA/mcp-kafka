package com.github.imcf.mcp.kafka.tools.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.imcf.mcp.kafka.client.SchemaRegistryClient;
import com.github.imcf.mcp.kafka.tools.BaseToolHandler;

import io.quarkiverse.mcp.server.Tool;
import io.quarkiverse.mcp.server.ToolArg;
import io.quarkiverse.mcp.server.ToolResponse;
import jakarta.inject.Inject;

public class GetSchemaHandler extends BaseToolHandler {

    @Inject
    SchemaRegistryClient schemaRegistryClient;

    @Inject
    ObjectMapper objectMapper;

    @Tool(name = "get-schema", description = "Get a schema by subject and optional version from the Schema Registry.")
    ToolResponse getSchema(
            @ToolArg(description = "Subject name") String subject,
            @ToolArg(description = "Schema version (omit for latest)") String version) {
        try {
            JsonNode schema = schemaRegistryClient.getSchemaBySubjectVersion(subject, version);
            return success(objectMapper.writeValueAsString(schema));
        } catch (Exception e) {
            return error(e.getMessage());
        }
    }
}
