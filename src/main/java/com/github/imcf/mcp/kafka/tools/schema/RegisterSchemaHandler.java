package com.github.imcf.mcp.kafka.tools.schema;

import io.quarkiverse.mcp.server.Tool;
import io.quarkiverse.mcp.server.ToolArg;
import io.quarkiverse.mcp.server.ToolResponse;

public class RegisterSchemaHandler extends BaseSchemaToolHandler {

    @Tool(name = "register-schema", description = "Register a new schema version for a subject in the Schema Registry.")
    ToolResponse registerSchema(
            @ToolArg(description = "Subject name") String subject,
            @ToolArg(description = "Schema definition (JSON string)") String schema,
            @ToolArg(description = "Schema type: AVRO, JSON, or PROTOBUF", defaultValue = "AVRO") String schemaType,
            @ToolArg(description = "Schema references as JSON array") String references) {

        ToolResponse configCheck = requireSchemaRegistry();
        if (configCheck != null) return configCheck;

        if (isBlank(subject)) {
            return error("Subject name is required");
        }
        if (isBlank(schema)) {
            return error("Schema definition is required");
        }

        try {
            int schemaId = schemaRegistryClient.registerSchema(subject, schema, schemaType, references);
            return success(String.format("Schema registered with ID %d for subject '%s'", schemaId, subject));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return error("Operation interrupted");
        } catch (Exception e) {
            return error(e);
        }
    }
}
