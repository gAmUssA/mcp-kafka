package com.github.imcf.mcp.kafka.tools.kafka;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.kafka.clients.admin.Config;
import org.apache.kafka.clients.admin.ConfigEntry;
import org.apache.kafka.common.config.ConfigResource;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.imcf.mcp.kafka.client.KafkaClientManager;
import com.github.imcf.mcp.kafka.tools.BaseToolHandler;

import io.quarkiverse.mcp.server.Tool;
import io.quarkiverse.mcp.server.ToolArg;
import io.quarkiverse.mcp.server.ToolResponse;
import jakarta.inject.Inject;

public class GetTopicConfigHandler extends BaseToolHandler {

    private static final int ADMIN_TIMEOUT_SECONDS = 30;

    @Inject
    KafkaClientManager kafkaClientManager;

    @Inject
    ObjectMapper objectMapper;

    @Tool(name = "get-topic-config", description = "Get the configuration for a Kafka topic.")
    ToolResponse getTopicConfig(
            @ToolArg(description = "Topic name") String topicName) {

        if (isBlank(topicName)) {
            return error("Topic name is required");
        }

        try {
            ConfigResource resource = new ConfigResource(ConfigResource.Type.TOPIC, topicName);
            Map<ConfigResource, Config> configs = kafkaClientManager.getAdminClient()
                .describeConfigs(List.of(resource))
                .all()
                .get(ADMIN_TIMEOUT_SECONDS, TimeUnit.SECONDS);

            Config config = configs.get(resource);
            Collection<ConfigEntry> entries = config.entries();

            List<Map<String, Object>> configList = entries.stream()
                .map(entry -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("name", entry.name());
                    map.put("value", entry.value());
                    map.put("source", entry.source().name());
                    map.put("isDefault", entry.isDefault());
                    map.put("isReadOnly", entry.isReadOnly());
                    map.put("isSensitive", entry.isSensitive());
                    return map;
                })
                .toList();

            return success(objectMapper.writeValueAsString(configList));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return error("Operation interrupted");
        } catch (Exception e) {
            return error(e);
        }
    }
}
