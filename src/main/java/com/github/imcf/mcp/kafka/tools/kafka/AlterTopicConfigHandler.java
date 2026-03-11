package com.github.imcf.mcp.kafka.tools.kafka;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.kafka.clients.admin.AlterConfigOp;
import org.apache.kafka.clients.admin.AlterConfigsOptions;
import org.apache.kafka.clients.admin.ConfigEntry;
import org.apache.kafka.common.config.ConfigResource;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.imcf.mcp.kafka.client.KafkaClientManager;
import com.github.imcf.mcp.kafka.tools.BaseToolHandler;

import io.quarkiverse.mcp.server.Tool;
import io.quarkiverse.mcp.server.ToolArg;
import io.quarkiverse.mcp.server.ToolResponse;
import jakarta.inject.Inject;

public class AlterTopicConfigHandler extends BaseToolHandler {

    private static final int ADMIN_TIMEOUT_SECONDS = 30;

    @Inject
    KafkaClientManager kafkaClientManager;

    @Inject
    ObjectMapper objectMapper;

    @Tool(name = "alter-topic-config", description = "Alter the configuration for a Kafka topic.")
    ToolResponse alterTopicConfig(
            @ToolArg(description = "Topic name") String topicName,
            @ToolArg(description = "Configuration changes as JSON array of objects with 'name', 'value', and optional 'operation' (SET or DELETE). Example: [{\"name\":\"retention.ms\",\"value\":\"86400000\"}]") String configChanges,
            @ToolArg(description = "Validate the configuration without applying", defaultValue = "false") boolean validateOnly) {

        if (isBlank(topicName)) {
            return error("Topic name is required");
        }
        if (isBlank(configChanges)) {
            return error("Configuration changes are required");
        }

        try {
            var changes = objectMapper.readTree(configChanges);
            List<AlterConfigOp> ops = new ArrayList<>();

            for (var change : changes) {
                String name = change.get("name").asText();
                String value = change.has("value") ? change.get("value").asText() : null;
                String operation = change.has("operation") ? change.get("operation").asText() : "SET";

                AlterConfigOp.OpType opType = "DELETE".equalsIgnoreCase(operation)
                    ? AlterConfigOp.OpType.DELETE
                    : AlterConfigOp.OpType.SET;

                ops.add(new AlterConfigOp(new ConfigEntry(name, value), opType));
            }

            if (ops.isEmpty()) {
                return error("No configuration changes provided");
            }

            ConfigResource resource = new ConfigResource(ConfigResource.Type.TOPIC, topicName);
            AlterConfigsOptions options = new AlterConfigsOptions().validateOnly(validateOnly);

            kafkaClientManager.getAdminClient()
                .incrementalAlterConfigs(Map.of(resource, ops), options)
                .all()
                .get(ADMIN_TIMEOUT_SECONDS, TimeUnit.SECONDS);

            String action = validateOnly ? "Validated" : "Applied";
            return success(String.format("%s %d config change(s) to topic '%s'", action, ops.size(), topicName));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return error("Operation interrupted");
        } catch (Exception e) {
            return error(e);
        }
    }
}
