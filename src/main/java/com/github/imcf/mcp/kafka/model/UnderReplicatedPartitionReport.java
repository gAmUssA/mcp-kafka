package com.github.imcf.mcp.kafka.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.List;

public record UnderReplicatedPartitionReport(
    String timestamp,
    @JsonProperty("under_replicated_partition_count") int underReplicatedPartitionCount,
    List<Detail> details,
    List<String> recommendations
) {
    public record Detail(
        String topic,
        int partition,
        int leader,
        @JsonProperty("replica_count") int replicaCount,
        @JsonProperty("isr_count") int isrCount,
        List<Integer> replicas,
        List<Integer> isr,
        @JsonProperty("missing_replicas") List<Integer> missingReplicas
    ) {}

    public static UnderReplicatedPartitionReport empty() {
        return new UnderReplicatedPartitionReport(Instant.now().toString(), 0, List.of(), List.of());
    }
}
