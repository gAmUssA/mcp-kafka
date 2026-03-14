package com.github.imcf.mcp.kafka.client;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.ConsumerGroupListing;
import org.apache.kafka.clients.admin.ListConsumerGroupOffsetsResult;
import org.apache.kafka.clients.admin.ListOffsetsResult;
import org.apache.kafka.clients.admin.OffsetSpec;
import org.apache.kafka.clients.admin.TopicDescription;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.common.Node;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.TopicPartitionInfo;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.ByteArraySerializer;

import com.github.imcf.mcp.kafka.config.KafkaConfig;
import com.github.imcf.mcp.kafka.model.ClusterOverview;
import com.github.imcf.mcp.kafka.model.ConsumerLagReport;
import com.github.imcf.mcp.kafka.model.UnderReplicatedPartitionReport;

import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Manages Kafka client instances (AdminClient, Producer, Consumer) with lazy initialization.
 * Provides thread-safe access to shared clients and proper cleanup on shutdown.
 */
@ApplicationScoped
public class KafkaClientManager {

    private static final String CONSUMER_GROUP_PREFIX = "mcp-kafka-oss-";

    @Inject
    KafkaConfig kafkaConfig;

    private volatile AdminClient adminClient;
    private volatile KafkaProducer<byte[], byte[]> producer;

    public AdminClient getAdminClient() {
        if (adminClient == null) {
            synchronized (this) {
                if (adminClient == null) {
                    adminClient = AdminClient.create(kafkaConfig.toProperties());
                }
            }
        }
        return adminClient;
    }

    public KafkaProducer<byte[], byte[]> getProducer() {
        if (producer == null) {
            synchronized (this) {
                if (producer == null) {
                    Properties props = kafkaConfig.toProperties();
                    props.put("key.serializer", ByteArraySerializer.class.getName());
                    props.put("value.serializer", ByteArraySerializer.class.getName());
                    producer = new KafkaProducer<>(props);
                }
            }
        }
        return producer;
    }

    /**
     * Creates a new consumer instance with a unique group ID.
     * Caller is responsible for closing the consumer.
     */
    public KafkaConsumer<byte[], byte[]> createConsumer() {
        Properties props = kafkaConfig.toProperties();
        props.put("key.deserializer", ByteArrayDeserializer.class.getName());
        props.put("value.deserializer", ByteArrayDeserializer.class.getName());
        props.put("group.id", CONSUMER_GROUP_PREFIX + UUID.randomUUID());
        props.put("auto.offset.reset", "earliest");
        props.put("enable.auto.commit", "false");
        return new KafkaConsumer<>(props);
    }

    private int adminTimeout() {
        return kafkaConfig.adminTimeoutSeconds();
    }

    /**
     * Returns a cluster overview with broker count, controller ID, topic/partition counts,
     * under-replicated partitions, offline partitions, and health status.
     */
    public ClusterOverview getClusterOverview() throws Exception {
        var admin = getAdminClient();
        var clusterResult = admin.describeCluster();

        Collection<Node> nodes = clusterResult.nodes().get(adminTimeout(), TimeUnit.SECONDS);
        Node controller = clusterResult.controller().get(adminTimeout(), TimeUnit.SECONDS);

        Set<String> topicNames = admin.listTopics().names().get(adminTimeout(), TimeUnit.SECONDS);
        Map<String, TopicDescription> topics = admin.describeTopics(topicNames)
                .allTopicNames().get(adminTimeout(), TimeUnit.SECONDS);

        int totalPartitions = 0;
        int underReplicated = 0;
        int offline = 0;
        for (TopicDescription td : topics.values()) {
            for (TopicPartitionInfo pi : td.partitions()) {
                totalPartitions++;
                if (pi.isr().size() < pi.replicas().size()) {
                    underReplicated++;
                }
                if (pi.leader() == null) {
                    offline++;
                }
            }
        }

        List<Integer> offlineBrokerIds = new ArrayList<>();
        // Note: AdminClient.describeCluster().nodes() only returns active nodes.
        // Determining truly offline nodes requires checking against bootstrap servers or using more advanced metadata.
        
        String healthStatus = (underReplicated == 0 && offline == 0)
                ? "healthy" : "unhealthy";

        return new ClusterOverview(
                Instant.now().toString(),
                nodes.size(),
                controller.id(),
                topicNames.size(),
                totalPartitions,
                underReplicated,
                offline,
                offlineBrokerIds,
                healthStatus
        );
    }

    /**
     * Returns under-replicated partition details: partitions where ISR count < replication factor.
     */
    public UnderReplicatedPartitionReport getUnderReplicatedPartitions() throws Exception {
        var admin = getAdminClient();
        Set<String> topicNames = admin.listTopics().names().get(adminTimeout(), TimeUnit.SECONDS);
        Map<String, TopicDescription> topics = admin.describeTopics(topicNames)
                .allTopicNames().get(adminTimeout(), TimeUnit.SECONDS);

        List<UnderReplicatedPartitionReport.Detail> details = new ArrayList<>();
        for (TopicDescription td : topics.values()) {
            for (TopicPartitionInfo pi : td.partitions()) {
                if (pi.isr().size() < pi.replicas().size()) {
                    List<Integer> replicaIds = pi.replicas().stream().map(Node::id).toList();
                    List<Integer> isrIds = pi.isr().stream().map(Node::id).toList();
                    List<Integer> missingReplicas = replicaIds.stream()
                            .filter(id -> !isrIds.contains(id)).toList();

                    details.add(new UnderReplicatedPartitionReport.Detail(
                            td.name(),
                            pi.partition(),
                            pi.leader() != null ? pi.leader().id() : -1,
                            pi.replicas().size(),
                            pi.isr().size(),
                            replicaIds,
                            isrIds,
                            missingReplicas
                    ));
                }
            }
        }

        return new UnderReplicatedPartitionReport(
                Instant.now().toString(),
                details.size(),
                details,
                List.of(
                        "Check broker health for any offline or struggling brokers",
                        "Verify network connectivity between brokers",
                        "Monitor disk space on broker nodes",
                        "Review broker logs for detailed error messages",
                        "Consider increasing replication timeouts if network is slow"
                )
        );
    }

    /**
     * Returns consumer group lag analysis with group summaries and high-lag details.
     */
    public ConsumerLagReport getConsumerGroupLag(long threshold) throws Exception {
        var admin = getAdminClient();
        Collection<ConsumerGroupListing> groups = admin.listConsumerGroups()
                .all().get(adminTimeout(), TimeUnit.SECONDS);

        List<ConsumerLagReport.GroupSummary> groupSummaries = new ArrayList<>();
        List<ConsumerLagReport.HighLagDetail> highLagDetails = new ArrayList<>();

        for (ConsumerGroupListing group : groups) {
            String groupId = group.groupId();
            // Skip internal MCP consumer groups
            if (groupId.startsWith(CONSUMER_GROUP_PREFIX)) {
                continue;
            }

            try {
                ListConsumerGroupOffsetsResult offsetsResult = admin.listConsumerGroupOffsets(groupId);
                Map<TopicPartition, OffsetAndMetadata> offsets = offsetsResult.partitionsToOffsetAndMetadata()
                        .get(adminTimeout(), TimeUnit.SECONDS);

                if (offsets.isEmpty()) {
                    continue;
                }

                // Get end offsets for the partitions
                Map<TopicPartition, OffsetSpec> endOffsetSpecs = offsets.keySet().stream()
                        .collect(Collectors.toMap(tp -> tp, tp -> OffsetSpec.latest()));
                Map<TopicPartition, ListOffsetsResult.ListOffsetsResultInfo> endOffsets =
                        admin.listOffsets(endOffsetSpecs).all().get(adminTimeout(), TimeUnit.SECONDS);

                long totalLag = 0;
                Set<String> topics = offsets.keySet().stream()
                        .map(TopicPartition::topic).collect(Collectors.toSet());

                for (Map.Entry<TopicPartition, OffsetAndMetadata> entry : offsets.entrySet()) {
                    TopicPartition tp = entry.getKey();
                    long currentOffset = entry.getValue().offset();
                    long logEndOffset = endOffsets.containsKey(tp) ? endOffsets.get(tp).offset() : currentOffset;
                    long lag = Math.max(0, logEndOffset - currentOffset);
                    totalLag += lag;

                    if (lag >= threshold) {
                        highLagDetails.add(new ConsumerLagReport.HighLagDetail(
                                groupId,
                                tp.topic(),
                                tp.partition(),
                                currentOffset,
                                logEndOffset,
                                lag
                        ));
                    }
                }

                var groupDescription = admin.describeConsumerGroups(List.of(groupId))
                        .all().get(adminTimeout(), TimeUnit.SECONDS).get(groupId);

                groupSummaries.add(new ConsumerLagReport.GroupSummary(
                        groupId,
                        groupDescription.state().toString(),
                        groupDescription.members().size(),
                        topics.size(),
                        totalLag,
                        totalLag >= threshold
                ));
            } catch (Exception e) {
                // Log the error and continue with other groups
                System.err.println("[ERROR] Failed to fetch lag for consumer group " + groupId + ": " + e.getMessage());
            }
        }

        return new ConsumerLagReport(
                Instant.now().toString(),
                threshold,
                groupSummaries.size(),
                groupSummaries,
                highLagDetails,
                List.of(
                        "Check consumer instances for errors or slowdowns",
                        "Consider scaling up consumer groups with high lag",
                        "Review consumer configuration settings",
                        "Examine processing bottlenecks in consumer application logic"
                )
        );
    }

    @PreDestroy
    void shutdown() {
        if (adminClient != null) {
            adminClient.close();
        }
        if (producer != null) {
            producer.flush();
            producer.close();
        }
    }
}
