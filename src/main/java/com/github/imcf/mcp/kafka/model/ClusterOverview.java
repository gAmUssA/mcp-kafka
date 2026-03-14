package com.github.imcf.mcp.kafka.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.List;

public record ClusterOverview(
    String timestamp,
    @JsonProperty("broker_count") int brokerCount,
    @JsonProperty("controller_id") int controllerId,
    @JsonProperty("topic_count") int topicCount,
    @JsonProperty("partition_count") int partitionCount,
    @JsonProperty("under_replicated_partitions") int underReplicatedPartitions,
    @JsonProperty("offline_partitions") int offlinePartitions,
    @JsonProperty("offline_broker_ids") List<Integer> offlineBrokerIds,
    @JsonProperty("health_status") String healthStatus
) {
    public static ClusterOverview empty() {
        return new ClusterOverview(Instant.now().toString(), 0, -1, 0, 0, 0, 0, List.of(), "unknown");
    }
}
