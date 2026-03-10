package com.github.imcf.mcp.kafka.tools.kafka;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.github.imcf.mcp.kafka.client.KafkaClientManager;
import com.github.imcf.mcp.kafka.tools.BaseToolHandler;

import io.quarkiverse.mcp.server.Tool;
import io.quarkiverse.mcp.server.ToolArg;
import io.quarkiverse.mcp.server.ToolResponse;
import jakarta.inject.Inject;

public class DeleteTopicsHandler extends BaseToolHandler {

    @Inject
    KafkaClientManager kafkaClientManager;

    @Tool(name = "delete-topics", description = "Delete one or more Kafka topics.")
    ToolResponse deleteTopics(
            @ToolArg(description = "Comma-separated list of topic names to delete") String topicNames) {
        try {
            List<String> topics = Arrays.stream(topicNames.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());

            kafkaClientManager.getAdminClient()
                .deleteTopics(topics)
                .all()
                .get(30, TimeUnit.SECONDS);

            return success("Deleted topics: " + String.join(",", topics));
        } catch (Exception e) {
            return error(e.getMessage());
        }
    }
}
