package com.github.imcf.mcp.kafka.resources.kafka;

import com.github.imcf.mcp.kafka.client.KafkaClientManager;
import com.github.imcf.mcp.kafka.resources.BaseResourceHandler;

import io.quarkiverse.mcp.server.RequestUri;
import io.quarkiverse.mcp.server.Resource;
import io.quarkiverse.mcp.server.TextResourceContents;
import jakarta.inject.Inject;

public class ConsumerLagReportResource extends BaseResourceHandler {

    @Inject
    KafkaClientManager kafkaClientManager;

    @Resource(uri = "kafka-mcp://consumer-lag-report", name = "kafka-consumer-lag-report",
              description = "Consumer group lag analysis with group summaries, partition lag metrics, and performance recommendations.")
    TextResourceContents consumerLagReport(RequestUri uri) throws Exception {
        return jsonResource(uri.value(), kafkaClientManager.getConsumerGroupLag(1000));
    }
}
