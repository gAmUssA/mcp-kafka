package com.github.imcf.mcp.kafka.prompts.kafka;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.github.imcf.mcp.kafka.client.KafkaClientManager;
import com.github.imcf.mcp.kafka.model.ConsumerLagReport;
import com.github.imcf.mcp.kafka.prompts.BasePromptHandler;

import io.quarkiverse.mcp.server.Prompt;
import io.quarkiverse.mcp.server.PromptArg;
import io.quarkiverse.mcp.server.PromptMessage;
import jakarta.inject.Inject;

public class ConsumerLagReportPrompt extends BasePromptHandler {

    @Inject
    KafkaClientManager kafkaClientManager;

    @Prompt(name = "kafka-consumer-lag-report",
            description = "Generates a comprehensive consumer lag analysis report covering all consumer groups with performance recommendations.")
    PromptMessage consumerLagReport(
            @PromptArg(name = "threshold", description = "Message lag threshold for highlighting groups with performance issues",
                       defaultValue = "1000") String threshold) throws Exception {

        long lagThreshold;
        try {
            lagThreshold = Long.parseLong(threshold);
        } catch (NumberFormatException e) {
            return markdown("Error: Invalid threshold value '" + threshold + "'. Please provide a numeric value.");
        }
        
        ConsumerLagReport data = kafkaClientManager.getConsumerGroupLag(lagThreshold);

        StringBuilder sb = new StringBuilder();
        sb.append("# Kafka Consumer Lag Report\n\n");
        sb.append("**Time**: ").append(data.timestamp()).append("\n\n");
        sb.append("**Lag Threshold**: ").append(lagThreshold).append(" messages\n\n");

        int groupCount = data.groupCount();

        if (groupCount == 0) {
            sb.append("No consumer groups found in the cluster.\n");
            return markdown(sb.toString());
        }

        sb.append("Found ").append(groupCount).append(" consumer group(s)\n\n");

        // Consumer Group Summary Table
        sb.append("## Consumer Group Summary\n\n");
        List<String> headers = List.of("Group ID", "State", "Members", "Topics", "Total Lag", "High Lag");
        List<List<String>> rows = new ArrayList<>();

        for (ConsumerLagReport.GroupSummary summary : data.groupSummary()) {
            rows.add(List.of(
                    String.valueOf(summary.groupId()),
                    String.valueOf(summary.state()),
                    String.valueOf(summary.memberCount()),
                    String.valueOf(summary.topicCount()),
                    String.valueOf(summary.totalLag()),
                    summary.hasHighLag() ? "WARNING Yes" : "No"
            ));
        }
        sb.append(formatTable(headers, rows));

        // High Lag Details
        List<ConsumerLagReport.HighLagDetail> highLagDetails = data.highLagDetails();
        if (!highLagDetails.isEmpty()) {
            sb.append("\n## High Lag Details\n\n");
            List<String> detailHeaders = List.of("Group", "Topic", "Partition", "Current Offset", "Log End Offset", "Lag");
            List<List<String>> detailRows = new ArrayList<>();
            for (ConsumerLagReport.HighLagDetail detail : highLagDetails) {
                detailRows.add(List.of(
                        String.valueOf(detail.groupId()),
                        String.valueOf(detail.topic()),
                        String.valueOf(detail.partition()),
                        String.valueOf(detail.currentOffset()),
                        String.valueOf(detail.logEndOffset()),
                        String.valueOf(detail.lag())
                ));
            }
            sb.append(formatTable(detailHeaders, detailRows));
        }

        // Recommendations
        sb.append("\n## Recommendations\n\n");
        List<String> recommendations = data.recommendations();
        for (int i = 0; i < recommendations.size(); i++) {
            sb.append(i + 1).append(". **").append(recommendations.get(i)).append("**\n");
        }

        return markdown(sb.toString());
    }
}
