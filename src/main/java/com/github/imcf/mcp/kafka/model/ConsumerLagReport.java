package com.github.imcf.mcp.kafka.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.List;

public record ConsumerLagReport(
    String timestamp,
    @JsonProperty("lag_threshold") long lagThreshold,
    @JsonProperty("group_count") int groupCount,
    @JsonProperty("group_summary") List<GroupSummary> groupSummary,
    @JsonProperty("high_lag_details") List<HighLagDetail> highLagDetails,
    List<String> recommendations
) {
    public record GroupSummary(
        @JsonProperty("group_id") String groupId,
        String state,
        @JsonProperty("member_count") int memberCount,
        @JsonProperty("topic_count") int topicCount,
        @JsonProperty("total_lag") long totalLag,
        @JsonProperty("has_high_lag") boolean hasHighLag
    ) {}

    public record HighLagDetail(
        @JsonProperty("group_id") String groupId,
        String topic,
        int partition,
        @JsonProperty("current_offset") long currentOffset,
        @JsonProperty("log_end_offset") long logEndOffset,
        long lag
    ) {}

    public static ConsumerLagReport empty() {
        return new ConsumerLagReport(Instant.now().toString(), 0, 0, List.of(), List.of(), List.of());
    }
}
