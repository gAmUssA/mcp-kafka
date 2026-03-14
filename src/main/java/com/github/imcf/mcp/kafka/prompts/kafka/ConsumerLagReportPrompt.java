package com.github.imcf.mcp.kafka.prompts.kafka;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.github.imcf.mcp.kafka.client.KafkaClientManager;
import com.github.imcf.mcp.kafka.prompts.BasePromptHandler;

import io.quarkiverse.mcp.server.Prompt;
import io.quarkiverse.mcp.server.PromptArg;
import io.quarkiverse.mcp.server.PromptMessage;
import jakarta.inject.Inject;

public class ConsumerLagReportPrompt extends BasePromptHandler {

    @Inject
    KafkaClientManager kafkaClientManager;

    @Prompt(name = "kafka_consumer_lag_report",
            description = "Generates a comprehensive consumer lag analysis report covering all consumer groups with performance recommendations.")
    PromptMessage consumerLagReport(
            @PromptArg(name = "threshold", description = "Message lag threshold for highlighting groups with performance issues",
                       defaultValue = "1000") String threshold) throws Exception {

        long lagThreshold = Long.parseLong(threshold);
        Map<String, Object> data = kafkaClientManager.getConsumerGroupLag(lagThreshold);

        StringBuilder sb = new StringBuilder();
        sb.append("# Kafka Consumer Lag Report\n\n");
        sb.append("**Time**: ").append(data.get("timestamp")).append("\n\n");
        sb.append("**Lag Threshold**: ").append(lagThreshold).append(" messages\n\n");

        int groupCount = (int) data.get("group_count");

        if (groupCount == 0) {
            sb.append("No consumer groups found in the cluster.\n");
            return markdown(sb.toString());
        }

        sb.append("Found ").append(groupCount).append(" consumer group(s)\n\n");

        // Consumer Group Summary Table
        sb.append("## Consumer Group Summary\n\n");
        List<String> headers = List.of("Group ID", "State", "Members", "Topics", "Total Lag", "High Lag");
        List<List<String>> rows = new ArrayList<>();

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> summaries = (List<Map<String, Object>>) data.get("group_summary");
        for (Map<String, Object> summary : summaries) {
            boolean hasHighLag = (boolean) summary.get("has_high_lag");
            rows.add(List.of(
                    String.valueOf(summary.get("group_id")),
                    String.valueOf(summary.get("state")),
                    String.valueOf(summary.get("member_count")),
                    String.valueOf(summary.get("topic_count")),
                    String.valueOf(summary.get("total_lag")),
                    hasHighLag ? "WARNING Yes" : "No"
            ));
        }
        sb.append(formatTable(headers, rows));

        // High Lag Details
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> highLagDetails = (List<Map<String, Object>>) data.get("high_lag_details");
        if (!highLagDetails.isEmpty()) {
            sb.append("\n## High Lag Details\n\n");
            List<String> detailHeaders = List.of("Group", "Topic", "Partition", "Current Offset", "Log End Offset", "Lag");
            List<List<String>> detailRows = new ArrayList<>();
            for (Map<String, Object> detail : highLagDetails) {
                detailRows.add(List.of(
                        String.valueOf(detail.get("group_id")),
                        String.valueOf(detail.get("topic")),
                        String.valueOf(detail.get("partition")),
                        String.valueOf(detail.get("current_offset")),
                        String.valueOf(detail.get("log_end_offset")),
                        String.valueOf(detail.get("lag"))
                ));
            }
            sb.append(formatTable(detailHeaders, detailRows));
        }

        // Recommendations
        sb.append("\n## Recommendations\n\n");
        @SuppressWarnings("unchecked")
        List<String> recommendations = (List<String>) data.get("recommendations");
        for (int i = 0; i < recommendations.size(); i++) {
            sb.append(i + 1).append(". **").append(recommendations.get(i)).append("**\n");
        }

        return markdown(sb.toString());
    }
}
