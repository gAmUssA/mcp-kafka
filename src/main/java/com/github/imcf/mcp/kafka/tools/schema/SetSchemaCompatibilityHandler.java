package com.github.imcf.mcp.kafka.tools.schema;

import com.github.imcf.mcp.kafka.client.SchemaRegistryClient;
import com.github.imcf.mcp.kafka.tools.BaseToolHandler;

import io.quarkiverse.mcp.server.Tool;
import io.quarkiverse.mcp.server.ToolArg;
import io.quarkiverse.mcp.server.ToolResponse;
import jakarta.inject.Inject;

public class SetSchemaCompatibilityHandler extends BaseToolHandler {

    @Inject
    SchemaRegistryClient schemaRegistryClient;

    @Tool(name = "set-schema-compatibility", description = "Set the compatibility level for a subject or globally.")
    ToolResponse setSchemaCompatibility(
            @ToolArg(description = "Subject name (omit to set global compatibility)") String subject,
            @ToolArg(description = "Compatibility level: BACKWARD, BACKWARD_TRANSITIVE, FORWARD, FORWARD_TRANSITIVE, FULL, FULL_TRANSITIVE, NONE") String compatibility) {
        try {
            String result = schemaRegistryClient.setCompatibility(
                    subject != null && !subject.isBlank() ? subject : null,
                    compatibility);
            if (subject != null && !subject.isBlank()) {
                return success(String.format("Compatibility for '%s' set to %s", subject, result));
            } else {
                return success(String.format("Global compatibility set to %s", result));
            }
        } catch (Exception e) {
            return error(e.getMessage());
        }
    }
}
