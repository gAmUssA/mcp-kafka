package com.github.imcf.mcp.kafka.tools.schema;

import io.quarkiverse.mcp.server.Tool;
import io.quarkiverse.mcp.server.ToolArg;
import io.quarkiverse.mcp.server.ToolResponse;

public class SetSchemaCompatibilityHandler extends BaseSchemaToolHandler {

    @Tool(name = "set-schema-compatibility", description = "Set the compatibility level for a subject or globally.")
    ToolResponse setSchemaCompatibility(
            @ToolArg(description = "Subject name (omit to set global compatibility)") String subject,
            @ToolArg(description = "Compatibility level: BACKWARD, BACKWARD_TRANSITIVE, FORWARD, FORWARD_TRANSITIVE, FULL, FULL_TRANSITIVE, NONE") String compatibility) {

        ToolResponse configCheck = requireSchemaRegistry();
        if (configCheck != null) return configCheck;

        if (isBlank(compatibility)) {
            return error("Compatibility level is required");
        }

        try {
            String subjectOrNull = isNotBlank(subject) ? subject : null;
            String result = schemaRegistryClient.setCompatibility(subjectOrNull, compatibility);
            if (subjectOrNull != null) {
                return success(String.format("Compatibility for '%s' set to %s", subject, result));
            } else {
                return success(String.format("Global compatibility set to %s", result));
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return error("Operation interrupted");
        } catch (Exception e) {
            return error(e);
        }
    }
}
