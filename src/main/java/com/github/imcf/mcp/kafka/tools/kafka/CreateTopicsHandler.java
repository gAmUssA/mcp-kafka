package com.github.imcf.mcp.kafka.tools.kafka;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.kafka.clients.admin.NewTopic;

import com.github.imcf.mcp.kafka.client.KafkaClientManager;
import com.github.imcf.mcp.kafka.tools.BaseToolHandler;

import io.quarkiverse.mcp.server.Tool;
import io.quarkiverse.mcp.server.ToolArg;
import io.quarkiverse.mcp.server.ToolResponse;
import jakarta.inject.Inject;

public class CreateTopicsHandler extends BaseToolHandler {

    private static final int ADMIN_TIMEOUT_SECONDS = 30;

    @Inject
    KafkaClientManager kafkaClientManager;

    @Tool(name = "create-topics", description = "Create one or more Kafka topics.")
    ToolResponse createTopics(
            @ToolArg(description = "Comma-separated list of topic names to create") String topicNames,
            @ToolArg(description = "Number of partitions", defaultValue = "1") int numPartitions,
            @ToolArg(description = "Replication factor", defaultValue = "1") short replicationFactor) {

        if (isBlank(topicNames)) {
            return error("Topic names are required");
        }
        if (numPartitions <= 0) {
            return error("Number of partitions must be positive");
        }
        if (replicationFactor <= 0) {
            return error("Replication factor must be positive");
        }

        try {
            List<NewTopic> newTopics = List.of(topicNames.split(",")).stream()
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(name -> new NewTopic(name, numPartitions, replicationFactor))
                .collect(Collectors.toList());

            if (newTopics.isEmpty()) {
                return error("No valid topic names provided");
            }

            kafkaClientManager.getAdminClient()
                .createTopics(newTopics)
                .all()
                .get(ADMIN_TIMEOUT_SECONDS, TimeUnit.SECONDS);

            String created = newTopics.stream().map(NewTopic::name).collect(Collectors.joining(","));
            return success("Created topics: " + created);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return error("Operation interrupted");
        } catch (Exception e) {
            return error(e);
        }
    }
}
