package com.github.imcf.mcp.kafka.resources.kafka;

import java.util.HashMap;
import java.util.Map;

import com.github.imcf.mcp.kafka.client.KafkaClientManager;
import com.github.imcf.mcp.kafka.resources.BaseResourceHandler;

import io.quarkiverse.mcp.server.RequestUri;
import io.quarkiverse.mcp.server.Resource;
import io.quarkiverse.mcp.server.TextResourceContents;
import jakarta.inject.Inject;

public class HealthCheckResource extends BaseResourceHandler {

    @Inject
    KafkaClientManager kafkaClientManager;

    @Resource(uri = "kafka-mcp://health-check", name = "kafka-health-check",
              description = "Detailed health assessment of the Kafka cluster covering broker availability, controller status, partition health, and consumer group performance.")
    TextResourceContents healthCheck(RequestUri uri) throws Exception {
        var overview = kafkaClientManager.getClusterOverview();
        var underReplicated = kafkaClientManager.getUnderReplicatedPartitions();
        var consumerLag = kafkaClientManager.getConsumerGroupLag(1000);

        Map<String, Object> brokerStatus = new HashMap<>();
        brokerStatus.put("total_brokers", overview.get("broker_count"));
        brokerStatus.put("offline_brokers", ((java.util.List<?>) overview.get("offline_broker_ids")).size());
        brokerStatus.put("offline_broker_ids", overview.get("offline_broker_ids"));
        brokerStatus.put("status", ((java.util.List<?>) overview.get("offline_broker_ids")).isEmpty() ? "healthy" : "unhealthy");

        Map<String, Object> controllerStatus = new HashMap<>();
        controllerStatus.put("controller_id", overview.get("controller_id"));
        controllerStatus.put("status", "healthy");

        int underReplicatedCount = (int) underReplicated.get("under_replicated_partition_count");
        int offlinePartitions = (int) overview.get("offline_partitions");
        Map<String, Object> partitionStatus = new HashMap<>();
        partitionStatus.put("total_partitions", overview.get("partition_count"));
        partitionStatus.put("under_replicated_partitions", underReplicatedCount);
        partitionStatus.put("offline_partitions", offlinePartitions);
        partitionStatus.put("status", (underReplicatedCount == 0 && offlinePartitions == 0) ? "healthy" : "warning");

        @SuppressWarnings("unchecked")
        var lagDetails = (java.util.List<?>) consumerLag.get("high_lag_details");
        Map<String, Object> consumerStatus = new HashMap<>();
        consumerStatus.put("total_groups", consumerLag.get("group_count"));
        consumerStatus.put("groups_with_high_lag", lagDetails.size());
        consumerStatus.put("status", lagDetails.isEmpty() ? "healthy" : "warning");

        boolean allHealthy = "healthy".equals(brokerStatus.get("status"))
                && "healthy".equals(partitionStatus.get("status"))
                && "healthy".equals(consumerStatus.get("status"));

        Map<String, Object> result = new HashMap<>();
        result.put("timestamp", overview.get("timestamp"));
        result.put("broker_status", brokerStatus);
        result.put("controller_status", controllerStatus);
        result.put("partition_status", partitionStatus);
        result.put("consumer_status", consumerStatus);
        result.put("overall_status", allHealthy ? "healthy" : "unhealthy");

        return jsonResource(uri.value(), result);
    }
}
