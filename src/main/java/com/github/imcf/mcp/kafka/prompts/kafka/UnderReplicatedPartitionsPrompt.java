package com.github.imcf.mcp.kafka.prompts.kafka;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.github.imcf.mcp.kafka.client.KafkaClientManager;
import com.github.imcf.mcp.kafka.model.UnderReplicatedPartitionReport;
import com.github.imcf.mcp.kafka.prompts.BasePromptHandler;

import io.quarkiverse.mcp.server.Prompt;
import io.quarkiverse.mcp.server.PromptMessage;
import jakarta.inject.Inject;

public class UnderReplicatedPartitionsPrompt extends BasePromptHandler {

    @Inject
    KafkaClientManager kafkaClientManager;

    @Prompt(name = "kafka-under-replicated-partitions",
            description = "Identifies and analyzes partitions with insufficient replication, providing detailed reporting and troubleshooting recommendations.")
    PromptMessage underReplicatedPartitions() throws Exception {
        UnderReplicatedPartitionReport data = kafkaClientManager.getUnderReplicatedPartitions();

        StringBuilder sb = new StringBuilder();
        sb.append("# Under-Replicated Partitions Report\n\n");
        sb.append("**Time**: ").append(data.timestamp()).append("\n\n");

        int count = data.underReplicatedPartitionCount();

        if (count == 0) {
            sb.append("OK **No under-replicated partitions found.** All partitions are fully replicated.\n");
            return markdown(sb.toString());
        }

        sb.append("WARNING **Found ").append(count).append(" under-replicated partitions**\n\n");

        // Table
        List<String> headers = List.of("Topic", "Partition", "Leader", "Replica Count", "ISR Count", "Missing Replicas");
        List<List<String>> rows = new ArrayList<>();

        for (UnderReplicatedPartitionReport.Detail detail : data.details()) {
            rows.add(List.of(
                    String.valueOf(detail.topic()),
                    String.valueOf(detail.partition()),
                    String.valueOf(detail.leader()),
                    String.valueOf(detail.replicaCount()),
                    String.valueOf(detail.isrCount()),
                    String.valueOf(detail.missingReplicas())
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
        List<String> recommendations = data.recommendations();
        for (int i = 0; i < recommendations.size(); i++) {
            sb.append(i + 1).append(". **").append(recommendations.get(i)).append("**\n");
        }

        return markdown(sb.toString());
    }
}
