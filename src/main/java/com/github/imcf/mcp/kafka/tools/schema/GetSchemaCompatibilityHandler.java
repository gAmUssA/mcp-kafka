package com.github.imcf.mcp.kafka.tools.schema;

import com.github.imcf.mcp.kafka.client.SchemaRegistryClient;
import com.github.imcf.mcp.kafka.tools.BaseToolHandler;

import io.quarkiverse.mcp.server.Tool;
import io.quarkiverse.mcp.server.ToolArg;
import io.quarkiverse.mcp.server.ToolResponse;
import jakarta.inject.Inject;

public class GetSchemaCompatibilityHandler extends BaseToolHandler {

    @Inject
    SchemaRegistryClient schemaRegistryClient;

    @Tool(name = "get-schema-compatibility", description = "Get the compatibility level for a subject or the global default.")
    ToolResponse getSchemaCompatibility(
            @ToolArg(description = "Subject name (omit for global compatibility)") String subject) {
        try {
            String level = schemaRegistryClient.getCompatibility(
                    subject != null && !subject.isBlank() ? subject : null);
            if (subject != null && !subject.isBlank()) {
                return success(String.format("Compatibility for '%s': %s", subject, level));
            } else {
                return success(String.format("Global compatibility: %s", level));
            }
        } catch (Exception e) {
            return error(e.getMessage());
        }
    }
}
