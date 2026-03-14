package com.github.imcf.mcp.kafka.prompts.kafka;

import java.util.List;
import java.util.Map;

import com.github.imcf.mcp.kafka.client.KafkaClientManager;
import com.github.imcf.mcp.kafka.prompts.BasePromptHandler;

import io.quarkiverse.mcp.server.Prompt;
import io.quarkiverse.mcp.server.PromptMessage;
import jakarta.inject.Inject;

public class HealthCheckPrompt extends BasePromptHandler {

    @Inject
    KafkaClientManager kafkaClientManager;

    @Prompt(name = "kafka_health_check",
            description = "Performs a comprehensive health assessment of the Kafka cluster with detailed analysis and actionable recommendations.")
    PromptMessage healthCheck() throws Exception {
        Map<String, Object> overview = kafkaClientManager.getClusterOverview();
        Map<String, Object> underReplicated = kafkaClientManager.getUnderReplicatedPartitions();
        Map<String, Object> consumerLag = kafkaClientManager.getConsumerGroupLag(1000);

        StringBuilder sb = new StringBuilder();
        sb.append("# Kafka Cluster Health Check Report\n\n");
        sb.append("**Time**: ").append(overview.get("timestamp")).append("\n\n");

        // Broker Status
        int brokerCount = (int) overview.get("broker_count");
        @SuppressWarnings("unchecked")
        List<Integer> offlineBrokers = (List<Integer>) overview.get("offline_broker_ids");
        sb.append("## Broker Status\n\n");
        if (offlineBrokers.isEmpty()) {
            sb.append("- OK **All ").append(brokerCount).append(" brokers are online**\n\n");
        } else {
            sb.append("- WARNING **").append(offlineBrokers.size())
              .append(" brokers are offline**: ").append(offlineBrokers).append("\n\n");
        }

        // Controller Status
        sb.append("## Controller Status\n\n");
        sb.append("- OK **Active controller**: Broker ").append(overview.get("controller_id")).append("\n\n");

        // Partition Health
        int partitionCount = (int) overview.get("partition_count");
        int underReplicatedCount = (int) underReplicated.get("under_replicated_partition_count");
        int offlinePartitions = (int) overview.get("offline_partitions");
        sb.append("## Partition Health\n\n");
        if (offlinePartitions == 0) {
            sb.append("- OK **All ").append(partitionCount).append(" partitions are online**\n");
        } else {
            sb.append("- WARNING **").append(offlinePartitions).append(" partitions are offline**\n");
        }
        if (underReplicatedCount == 0) {
            sb.append("- OK **No under-replicated partitions detected**\n\n");
        } else {
            sb.append("- WARNING **").append(underReplicatedCount).append(" under-replicated partitions detected**\n\n");
        }

        // Consumer Group Health
        int groupCount = (int) consumerLag.get("group_count");
        @SuppressWarnings("unchecked")
        List<?> highLagDetails = (List<?>) consumerLag.get("high_lag_details");
        sb.append("## Consumer Group Health\n\n");
        sb.append("- ").append(groupCount == 0 ? "OK" : "OK").append(" **")
          .append(groupCount).append(" consumer groups found**\n");
        if (highLagDetails.isEmpty()) {
            sb.append("- OK **No consumer groups with significant lag detected**\n\n");
        } else {
            sb.append("- WARNING **").append(highLagDetails.size())
              .append(" consumer groups with high lag detected**\n\n");
        }

        // Overall Assessment
        boolean allHealthy = offlineBrokers.isEmpty() && underReplicatedCount == 0
                && offlinePartitions == 0 && highLagDetails.isEmpty();
        sb.append("## Overall Health Assessment\n\n");
        if (allHealthy) {
            sb.append("OK **HEALTHY**: All systems are operating normally.\n");
        } else {
            sb.append("WARNING **UNHEALTHY**: Issues detected. Review sections above for details.\n");
        }

        return markdown(sb.toString());
    }
}
