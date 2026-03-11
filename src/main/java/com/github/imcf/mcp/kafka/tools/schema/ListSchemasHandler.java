package com.github.imcf.mcp.kafka.tools.schema;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkiverse.mcp.server.Tool;
import io.quarkiverse.mcp.server.ToolArg;
import io.quarkiverse.mcp.server.ToolResponse;
import jakarta.inject.Inject;

public class ListSchemasHandler extends BaseSchemaToolHandler {

    @Inject
    ObjectMapper objectMapper;

    @Tool(name = "list-schemas", description = "List all schemas in the Schema Registry.")
    ToolResponse listSchemas(
            @ToolArg(description = "Filter subjects by prefix") String subjectPrefix,
            @ToolArg(description = "Include deleted subjects", defaultValue = "false") boolean deleted) {

        ToolResponse configCheck = requireSchemaRegistry();
        if (configCheck != null) return configCheck;

        try {
            List<String> subjects = schemaRegistryClient.getSubjects(deleted);

            if (isNotBlank(subjectPrefix)) {
                subjects = subjects.stream()
                        .filter(s -> s.startsWith(subjectPrefix))
                        .toList();
            }

            Map<String, Object> result = new LinkedHashMap<>();
            for (String subject : subjects) {
                try {
                    JsonNode schema = schemaRegistryClient.getSchemaBySubjectVersion(subject, "latest");
                    Map<String, Object> entry = new LinkedHashMap<>();
                    entry.put("id", schema.get("id").asInt());
                    entry.put("version", schema.get("version").asInt());
                    entry.put("schemaType", schema.has("schemaType") ? schema.get("schemaType").asText() : "AVRO");
                    entry.put("schema", schema.get("schema").asText());
                    result.put(subject, entry);
                } catch (Exception e) {
                    result.put(subject, Map.of("error", e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()));
                }
            }

            return success(objectMapper.writeValueAsString(result));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return error("Operation interrupted");
        } catch (Exception e) {
            return error(e);
        }
    }
}
