package com.github.imcf.mcp.kafka.prompts.kafka;

import java.util.Map;

import com.github.imcf.mcp.kafka.client.KafkaClientManager;
import com.github.imcf.mcp.kafka.prompts.BasePromptHandler;

import io.quarkiverse.mcp.server.Prompt;
import io.quarkiverse.mcp.server.PromptMessage;
import jakarta.inject.Inject;

public class ClusterOverviewPrompt extends BasePromptHandler {

    @Inject
    KafkaClientManager kafkaClientManager;

    @Prompt(name = "kafka_cluster_overview",
            description = "Generates a comprehensive, human-readable summary of Kafka cluster health.")
    PromptMessage clusterOverview() throws Exception {
        Map<String, Object> overview = kafkaClientManager.getClusterOverview();

        String healthy = "healthy".equals(overview.get("health_status")) ? "Healthy" : "Unhealthy";
        String statusIcon = "healthy".equals(overview.get("health_status")) ? "OK" : "WARNING";

        String content = """
                # Kafka Cluster Overview

                **Time**: %s

                - **Broker Count**: %s
                - **Active Controller ID**: %s
                - **Total Topics**: %s
                - **Total Partitions**: %s
                - **Under-Replicated Partitions**: %s
                - **Offline Partitions**: %s

                **Overall Status**: %s %s
                """.formatted(
                overview.get("timestamp"),
                overview.get("broker_count"),
                overview.get("controller_id"),
                overview.get("topic_count"),
                overview.get("partition_count"),
                overview.get("under_replicated_partitions"),
                overview.get("offline_partitions"),
                statusIcon,
                healthy);

        return markdown(content);
    }
}
