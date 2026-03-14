package com.github.imcf.mcp.kafka.prompts.kafka;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.github.imcf.mcp.kafka.client.KafkaClientManager;
import com.github.imcf.mcp.kafka.prompts.BasePromptHandler;

import io.quarkiverse.mcp.server.Prompt;
import io.quarkiverse.mcp.server.PromptMessage;
import jakarta.inject.Inject;

public class UnderReplicatedPartitionsPrompt extends BasePromptHandler {

    @Inject
    KafkaClientManager kafkaClientManager;

    @Prompt(name = "kafka_under_replicated_partitions",
            description = "Identifies and analyzes partitions with insufficient replication, providing detailed reporting and troubleshooting recommendations.")
    PromptMessage underReplicatedPartitions() throws Exception {
        Map<String, Object> data = kafkaClientManager.getUnderReplicatedPartitions();

        StringBuilder sb = new StringBuilder();
        sb.append("# Under-Replicated Partitions Report\n\n");
        sb.append("**Time**: ").append(data.get("timestamp")).append("\n\n");

        int count = (int) data.get("under_replicated_partition_count");

        if (count == 0) {
            sb.append("OK **No under-replicated partitions found.** All partitions are fully replicated.\n");
            return markdown(sb.toString());
        }

        sb.append("WARNING **Found ").append(count).append(" under-replicated partitions**\n\n");

        // Table
        List<String> headers = List.of("Topic", "Partition", "Leader", "Replica Count", "ISR Count", "Missing Replicas");
        List<List<String>> rows = new ArrayList<>();

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> details = (List<Map<String, Object>>) data.get("details");
        for (Map<String, Object> detail : details) {
            rows.add(List.of(
                    String.valueOf(detail.get("topic")),
                    String.valueOf(detail.get("partition")),
                    String.valueOf(detail.get("leader")),
                    String.valueOf(detail.get("replica_count")),
                    String.valueOf(detail.get("isr_count")),
                    String.valueOf(detail.get("missing_replicas"))
            ));
        }
        sb.append(formatTable(headers, rows));

        // Causes and recommendations
        sb.append("\n## Possible Causes\n\n");
        sb.append("""
                Under-replicated partitions occur when one or more replicas are not in sync with the leader. Common causes include:

                - **Broker failure or network partition**
                - **High load on brokers**
                - **Insufficient disk space**
                - **Network bandwidth limitations**
                - **Misconfigured topic replication factor**
                """);

        sb.append("\n## Recommendations\n\n");
        @SuppressWarnings("unchecked")
        List<String> recommendations = (List<String>) data.get("recommendations");
        for (int i = 0; i < recommendations.size(); i++) {
            sb.append(i + 1).append(". **").append(recommendations.get(i)).append("**\n");
        }

        return markdown(sb.toString());
    }
}
