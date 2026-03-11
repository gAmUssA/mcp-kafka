package com.github.imcf.mcp.kafka.tools.schema;

import java.util.List;

import io.quarkiverse.mcp.server.Tool;
import io.quarkiverse.mcp.server.ToolArg;
import io.quarkiverse.mcp.server.ToolResponse;

public class DeleteSchemaHandler extends BaseSchemaToolHandler {

    @Tool(name = "delete-schema", description = "Delete a schema subject or specific version from the Schema Registry. Soft-deletes by default. To permanently delete, soft-delete first then call again with permanent=true.")
    ToolResponse deleteSchema(
            @ToolArg(description = "Subject name to delete") String subject,
            @ToolArg(description = "Specific version number to delete, or omit/leave empty to delete entire subject") String version,
            @ToolArg(description = "Set to true for permanent (hard) delete. Must soft-delete first.", defaultValue = "false") boolean permanent) {

        ToolResponse configCheck = requireSchemaRegistry();
        if (configCheck != null) return configCheck;

        if (isBlank(subject)) {
            return error("Subject name is required");
        }

        log.infof("delete-schema called with subject='%s', version='%s', permanent=%s", subject, version, permanent);
        try {
            boolean hasVersion = isNotBlank(version) && !"null".equalsIgnoreCase(version);

            if (hasVersion) {
                log.infof("Deleting version %s of subject '%s' (permanent=%s)", version, subject, permanent);
                int deleted = schemaRegistryClient.deleteSchemaVersion(subject, version, permanent);
                return success(String.format("Deleted version %d of subject '%s'%s",
                        deleted, subject, permanent ? " (permanent)" : " (soft delete)"));
            } else {
                log.infof("Deleting entire subject '%s' (permanent=%s)", subject, permanent);
                List<Integer> deleted = schemaRegistryClient.deleteSubject(subject, permanent);
                return success(String.format("Deleted subject '%s', removed versions: %s%s",
                        subject, deleted, permanent ? " (permanent)" : " (soft delete)"));
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return error("Operation interrupted");
        } catch (Exception e) {
            return error(e);
        }
    }
}
