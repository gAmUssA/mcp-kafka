package com.github.imcf.mcp.kafka.prompts.kafka;

import java.util.List;
import java.util.Map;

import com.github.imcf.mcp.kafka.client.KafkaClientManager;
import com.github.imcf.mcp.kafka.config.KafkaConfig;
import com.github.imcf.mcp.kafka.model.ClusterOverview;
import com.github.imcf.mcp.kafka.model.ConsumerLagReport;
import com.github.imcf.mcp.kafka.model.UnderReplicatedPartitionReport;
import com.github.imcf.mcp.kafka.prompts.BasePromptHandler;

import io.quarkiverse.mcp.server.Prompt;
import io.quarkiverse.mcp.server.PromptMessage;
import jakarta.inject.Inject;

public class HealthCheckPrompt extends BasePromptHandler {

    @Inject
    KafkaClientManager kafkaClientManager;

    @Inject
    KafkaConfig kafkaConfig;

    @Prompt(name = "kafka-health-check",
            description = "Performs a comprehensive health assessment of the Kafka cluster with detailed analysis and actionable recommendations.")
    PromptMessage healthCheck() throws Exception {
        ClusterOverview overview = kafkaClientManager.getClusterOverview();
        UnderReplicatedPartitionReport underReplicated = kafkaClientManager.getUnderReplicatedPartitions();
        ConsumerLagReport consumerLag = kafkaClientManager.getConsumerGroupLag(kafkaConfig.defaultLagThreshold());

        StringBuilder sb = new StringBuilder();
        sb.append("# Kafka Cluster Health Check Report\n\n");
        sb.append("**Time**: ").append(overview.timestamp()).append("\n\n");

        // Broker Status
        int brokerCount = overview.brokerCount();
        List<Integer> offlineBrokers = overview.offlineBrokerIds();
        sb.append("## Broker Status\n\n");
        if (offlineBrokers.isEmpty()) {
            sb.append("- OK **All ").append(brokerCount).append(" brokers are online**\n\n");
        } else {
            sb.append("- WARNING **").append(offlineBrokers.size())
              .append(" brokers are offline**: ").append(offlineBrokers).append("\n\n");
        }

        // Controller Status
        sb.append("## Controller Status\n\n");
        sb.append("- OK **Active controller**: Broker ").append(overview.controllerId()).append("\n\n");

        // Partition Health
        int partitionCount = overview.partitionCount();
        int underReplicatedCount = underReplicated.underReplicatedPartitionCount();
        int offlinePartitions = overview.offlinePartitions();
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
        int groupCount = consumerLag.groupCount();
        List<ConsumerLagReport.HighLagDetail> highLagDetails = consumerLag.highLagDetails();
        sb.append("## Consumer Group Health\n\n");
        sb.append("- ").append("OK").append(" **")
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
