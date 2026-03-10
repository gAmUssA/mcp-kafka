package com.github.imcf.mcp.kafka.tools.cluster;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.kafka.common.Node;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.imcf.mcp.kafka.client.KafkaClientManager;
import com.github.imcf.mcp.kafka.tools.BaseToolHandler;

import io.quarkiverse.mcp.server.Tool;
import io.quarkiverse.mcp.server.ToolResponse;
import jakarta.inject.Inject;

public class DescribeClusterHandler extends BaseToolHandler {

    @Inject
    KafkaClientManager kafkaClientManager;

    @Inject
    ObjectMapper objectMapper;

    @Tool(name = "describe-cluster", description = "Get Kafka cluster metadata.")
    ToolResponse describeCluster() {
        try {
            var result = kafkaClientManager.getAdminClient().describeCluster();

            String clusterId = result.clusterId().get(30, TimeUnit.SECONDS);
            Node controller = result.controller().get(30, TimeUnit.SECONDS);
            Collection<Node> nodes = result.nodes().get(30, TimeUnit.SECONDS);

            Map<String, Object> clusterInfo = new HashMap<>();
            clusterInfo.put("clusterId", clusterId);
            clusterInfo.put("controller", Map.of(
                "id", controller.id(),
                "host", controller.host(),
                "port", controller.port()
            ));

            List<Map<String, Object>> brokers = nodes.stream()
                .map(node -> {
                    Map<String, Object> broker = new HashMap<>();
                    broker.put("id", node.id());
                    broker.put("host", node.host());
                    broker.put("port", node.port());
                    broker.put("rack", node.rack());
                    return broker;
                })
                .toList();
            clusterInfo.put("brokers", brokers);

            return success(objectMapper.writeValueAsString(clusterInfo));
        } catch (Exception e) {
            return error(e.getMessage());
        }
    }
}
