package com.github.imcf.mcp.kafka.resources.kafka;

import java.util.HashMap;
import java.util.Map;

import com.github.imcf.mcp.kafka.client.KafkaClientManager;
import com.github.imcf.mcp.kafka.config.KafkaConfig;
import com.github.imcf.mcp.kafka.model.ClusterOverview;
import com.github.imcf.mcp.kafka.model.ConsumerLagReport;
import com.github.imcf.mcp.kafka.model.HealthReport;
import com.github.imcf.mcp.kafka.model.UnderReplicatedPartitionReport;
import com.github.imcf.mcp.kafka.resources.BaseResourceHandler;

import io.quarkiverse.mcp.server.RequestUri;
import io.quarkiverse.mcp.server.Resource;
import io.quarkiverse.mcp.server.TextResourceContents;
import jakarta.inject.Inject;

public class HealthCheckResource extends BaseResourceHandler {

    @Inject
    KafkaClientManager kafkaClientManager;

    @Inject
    KafkaConfig kafkaConfig;

    @Resource(uri = "kafka-mcp://health-check", name = "kafka-health-check",
              description = "Detailed health assessment of the Kafka cluster covering broker availability, controller status, partition health, and consumer group performance.")
    TextResourceContents healthCheck(RequestUri uri) throws Exception {
        ClusterOverview overview = kafkaClientManager.getClusterOverview();
        UnderReplicatedPartitionReport underReplicated = kafkaClientManager.getUnderReplicatedPartitions();
        ConsumerLagReport consumerLag = kafkaClientManager.getConsumerGroupLag(kafkaConfig.defaultLagThreshold());

        HealthReport.BrokerStatus brokerStatus = new HealthReport.BrokerStatus(
                overview.brokerCount(),
                overview.offlineBrokerIds().size(),
                overview.offlineBrokerIds(),
                overview.offlineBrokerIds().isEmpty() ? "healthy" : "unhealthy"
        );

        HealthReport.ControllerStatus controllerStatus = new HealthReport.ControllerStatus(
                overview.controllerId(),
                "healthy"
        );

        HealthReport.PartitionStatus partitionStatus = new HealthReport.PartitionStatus(
                overview.partitionCount(),
                underReplicated.underReplicatedPartitionCount(),
                overview.offlinePartitions(),
                (underReplicated.underReplicatedPartitionCount() == 0 && overview.offlinePartitions() == 0) ? "healthy" : "warning"
        );

        HealthReport.ConsumerStatus consumerStatus = new HealthReport.ConsumerStatus(
                consumerLag.groupCount(),
                consumerLag.highLagDetails().size(),
                consumerLag.highLagDetails().isEmpty() ? "healthy" : "warning"
        );

        boolean allHealthy = "healthy".equals(brokerStatus.status())
                && "healthy".equals(partitionStatus.status())
                && "healthy".equals(consumerStatus.status());

        HealthReport report = new HealthReport(
                overview.timestamp(),
                brokerStatus,
                controllerStatus,
                partitionStatus,
                consumerStatus,
                allHealthy ? "healthy" : "unhealthy"
        );

        return jsonResource(uri.value(), report);
    }
}
