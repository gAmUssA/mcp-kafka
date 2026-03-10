package com.github.imcf.mcp.kafka.tools.schema;

import com.github.imcf.mcp.kafka.client.SchemaRegistryClient;
import com.github.imcf.mcp.kafka.tools.BaseToolHandler;

import io.quarkiverse.mcp.server.Tool;
import io.quarkiverse.mcp.server.ToolArg;
import io.quarkiverse.mcp.server.ToolResponse;
import jakarta.inject.Inject;

public class RegisterSchemaHandler extends BaseToolHandler {

    @Inject
    SchemaRegistryClient schemaRegistryClient;

    @Tool(name = "register-schema", description = "Register a new schema version for a subject in the Schema Registry.")
    ToolResponse registerSchema(
            @ToolArg(description = "Subject name") String subject,
            @ToolArg(description = "Schema definition (JSON string)") String schema,
            @ToolArg(description = "Schema type: AVRO, JSON, or PROTOBUF", defaultValue = "AVRO") String schemaType,
            @ToolArg(description = "Schema references as JSON array") String references) {
        try {
            int schemaId = schemaRegistryClient.registerSchema(subject, schema, schemaType, references);
            return success(String.format("Schema registered with ID %d for subject '%s'", schemaId, subject));
        } catch (Exception e) {
            return error(e.getMessage());
        }
    }
}
