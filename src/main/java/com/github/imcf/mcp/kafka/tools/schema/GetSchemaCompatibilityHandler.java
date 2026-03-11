package com.github.imcf.mcp.kafka.tools.schema;

import io.quarkiverse.mcp.server.Tool;
import io.quarkiverse.mcp.server.ToolArg;
import io.quarkiverse.mcp.server.ToolResponse;

public class GetSchemaCompatibilityHandler extends BaseSchemaToolHandler {

    @Tool(name = "get-schema-compatibility", description = "Get the compatibility level for a subject or the global default.")
    ToolResponse getSchemaCompatibility(
            @ToolArg(description = "Subject name (omit for global compatibility)") String subject) {

        ToolResponse configCheck = requireSchemaRegistry();
        if (configCheck != null) return configCheck;

        try {
            String subjectOrNull = isNotBlank(subject) ? subject : null;
            String level = schemaRegistryClient.getCompatibility(subjectOrNull);
            if (subjectOrNull != null) {
                return success(String.format("Compatibility for '%s': %s", subject, level));
            } else {
                return success(String.format("Global compatibility: %s", level));
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return error("Operation interrupted");
        } catch (Exception e) {
            return error(e);
        }
    }
}
