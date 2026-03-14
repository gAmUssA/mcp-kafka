package com.github.imcf.mcp.kafka.resources.kafka;

import com.github.imcf.mcp.kafka.client.KafkaClientManager;
import com.github.imcf.mcp.kafka.resources.BaseResourceHandler;

import io.quarkiverse.mcp.server.RequestUri;
import io.quarkiverse.mcp.server.Resource;
import io.quarkiverse.mcp.server.TextResourceContents;
import jakarta.inject.Inject;

public class UnderReplicatedPartitionsResource extends BaseResourceHandler {

    @Inject
    KafkaClientManager kafkaClientManager;

    @Resource(uri = "kafka-mcp://under-replicated-partitions", name = "kafka-under-replicated-partitions",
              description = "Analysis of partitions with insufficient replication, including affected topics, missing replicas, and troubleshooting recommendations.")
    TextResourceContents underReplicatedPartitions(RequestUri uri) throws Exception {
        return jsonResource(uri.value(), kafkaClientManager.getUnderReplicatedPartitions());
    }
}
