package com.github.imcf.mcp.kafka.tools.kafka;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
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

    @Tool(name = "delete-topics", description = "Delete one or more Kafka topics. Provide topic names as a comma-separated list.")
    ToolResponse deleteTopics(
            @ToolArg(description = "Comma-separated list of topic names to delete") String topicNames) {
        log.infof("delete-topics called with topicNames='%s'", topicNames);
        try {
            List<String> topics = Arrays.stream(topicNames.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());

            if (topics.isEmpty()) {
                return error("No topic names provided");
            }

            // Verify topics exist before deleting
            Set<String> existing = kafkaClientManager.getAdminClient()
                .listTopics()
                .names()
                .get(30, TimeUnit.SECONDS);

            List<String> notFound = topics.stream()
                .filter(t -> !existing.contains(t))
                .toList();
            if (!notFound.isEmpty()) {
                return error("Topics not found: " + String.join(", ", notFound));
            }

            log.infof("Deleting topics: %s", topics);
            kafkaClientManager.getAdminClient()
                .deleteTopics(topics)
                .all()
                .get(30, TimeUnit.SECONDS);

            return success("Deleted topics: " + String.join(", ", topics));
        } catch (Exception e) {
            return error(e.getMessage());
        }
    }
}
