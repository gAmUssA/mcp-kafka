package com.github.imcf.mcp.kafka.resources.kafka;

import com.github.imcf.mcp.kafka.client.KafkaClientManager;
import com.github.imcf.mcp.kafka.resources.BaseResourceHandler;

import io.quarkiverse.mcp.server.RequestUri;
import io.quarkiverse.mcp.server.Resource;
import io.quarkiverse.mcp.server.TextResourceContents;
import jakarta.inject.Inject;

public class ClusterOverviewResource extends BaseResourceHandler {

  @Inject
  KafkaClientManager kafkaClientManager;

  @Resource(uri = "kafka-mcp://overview", name = "kafka-cluster-overview",
      description = "Comprehensive summary of Kafka cluster health including broker counts, controller status, topic/partition metrics, and replication health.")
  TextResourceContents overview(RequestUri uri) throws Exception {
    return jsonResource(uri.value(), kafkaClientManager.getClusterOverview());
  }
}
