package com.github.imcf.mcp.kafka.tools.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkiverse.mcp.server.Tool;
import io.quarkiverse.mcp.server.ToolArg;
import io.quarkiverse.mcp.server.ToolResponse;
import jakarta.inject.Inject;

public class GetSchemaHandler extends BaseSchemaToolHandler {

    @Inject
    ObjectMapper objectMapper;

    @Tool(name = "get-schema", description = "Get a schema by subject and optional version from the Schema Registry.")
    ToolResponse getSchema(
            @ToolArg(description = "Subject name") String subject,
            @ToolArg(description = "Schema version (omit for latest)") String version) {

        ToolResponse configCheck = requireSchemaRegistry();
        if (configCheck != null) return configCheck;

        if (isBlank(subject)) {
            return error("Subject name is required");
        }

        try {
            JsonNode schema = schemaRegistryClient.getSchemaBySubjectVersion(subject, version);
            return success(objectMapper.writeValueAsString(schema));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return error("Operation interrupted");
        } catch (Exception e) {
            return error(e);
        }
    }
}
