package com.github.imcf.mcp.kafka.prompts.kafka;

import java.util.Map;

import com.github.imcf.mcp.kafka.client.KafkaClientManager;
import com.github.imcf.mcp.kafka.model.ClusterOverview;
import com.github.imcf.mcp.kafka.prompts.BasePromptHandler;

import io.quarkiverse.mcp.server.Prompt;
import io.quarkiverse.mcp.server.PromptMessage;
import jakarta.inject.Inject;

public class ClusterOverviewPrompt extends BasePromptHandler {

    @Inject
    KafkaClientManager kafkaClientManager;

    @Prompt(name = "kafka-cluster-overview",
            description = "Generates a comprehensive, human-readable summary of Kafka cluster health.")
    PromptMessage clusterOverview() throws Exception {
        ClusterOverview overview = kafkaClientManager.getClusterOverview();

        String healthy = "healthy".equals(overview.healthStatus()) ? "Healthy" : "Unhealthy";
        String statusIcon = "healthy".equals(overview.healthStatus()) ? "OK" : "WARNING";

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
                overview.timestamp(),
                overview.brokerCount(),
                overview.controllerId(),
                overview.topicCount(),
                overview.partitionCount(),
                overview.underReplicatedPartitions(),
                overview.offlinePartitions(),
                statusIcon,
                healthy);

        return markdown(content);
    }
}
