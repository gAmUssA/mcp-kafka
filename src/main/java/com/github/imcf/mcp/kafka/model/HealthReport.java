package com.github.imcf.mcp.kafka.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record HealthReport(
    String timestamp,
    @JsonProperty("broker_status") BrokerStatus brokerStatus,
    @JsonProperty("controller_status") ControllerStatus controllerStatus,
    @JsonProperty("partition_status") PartitionStatus partitionStatus,
    @JsonProperty("consumer_status") ConsumerStatus consumerStatus,
    @JsonProperty("overall_status") String overallStatus
) {
    public record BrokerStatus(
        @JsonProperty("total_brokers") int totalBrokers,
        @JsonProperty("offline_brokers_count") int offlineBrokersCount,
        @JsonProperty("offline_broker_ids") List<Integer> offlineBrokerIds,
        String status
    ) {}

    public record ControllerStatus(
        @JsonProperty("controller_id") int controllerId,
        String status
    ) {}

    public record PartitionStatus(
        @JsonProperty("total_partitions") int totalPartitions,
        @JsonProperty("under_replicated_partitions") int underReplicatedPartitions,
        @JsonProperty("offline_partitions") int offlinePartitions,
        String status
    ) {}

    public record ConsumerStatus(
        @JsonProperty("total_groups") int totalGroups,
        @JsonProperty("groups_with_high_lag") int groupsWithHighLag,
        String status
    ) {}
}
