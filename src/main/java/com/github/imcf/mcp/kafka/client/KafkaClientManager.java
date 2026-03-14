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

    private static final int ADMIN_TIMEOUT_SECONDS = 30;

    /**
     * Returns a cluster overview map with broker count, controller ID, topic/partition counts,
     * under-replicated partitions, offline partitions, and health status.
     */
    public Map<String, Object> getClusterOverview() throws Exception {
        var admin = getAdminClient();
        var clusterResult = admin.describeCluster();

        Collection<Node> nodes = clusterResult.nodes().get(ADMIN_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        Node controller = clusterResult.controller().get(ADMIN_TIMEOUT_SECONDS, TimeUnit.SECONDS);

        Set<String> topicNames = admin.listTopics().names().get(ADMIN_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        Map<String, TopicDescription> topics = admin.describeTopics(topicNames)
                .allTopicNames().get(ADMIN_TIMEOUT_SECONDS, TimeUnit.SECONDS);

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
        String healthStatus = (underReplicated == 0 && offline == 0 && !offlineBrokerIds.isEmpty() == false)
                ? "healthy" : "unhealthy";
        if (underReplicated == 0 && offline == 0) {
            healthStatus = "healthy";
        }

        Map<String, Object> overview = new HashMap<>();
        overview.put("timestamp", Instant.now().toString());
        overview.put("broker_count", nodes.size());
        overview.put("controller_id", controller.id());
        overview.put("topic_count", topicNames.size());
        overview.put("partition_count", totalPartitions);
        overview.put("under_replicated_partitions", underReplicated);
        overview.put("offline_partitions", offline);
        overview.put("offline_broker_ids", offlineBrokerIds);
        overview.put("health_status", healthStatus);
        return overview;
    }

    /**
     * Returns under-replicated partition details: partitions where ISR count < replication factor.
     */
    public Map<String, Object> getUnderReplicatedPartitions() throws Exception {
        var admin = getAdminClient();
        Set<String> topicNames = admin.listTopics().names().get(ADMIN_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        Map<String, TopicDescription> topics = admin.describeTopics(topicNames)
                .allTopicNames().get(ADMIN_TIMEOUT_SECONDS, TimeUnit.SECONDS);

        List<Map<String, Object>> details = new ArrayList<>();
        for (TopicDescription td : topics.values()) {
            for (TopicPartitionInfo pi : td.partitions()) {
                if (pi.isr().size() < pi.replicas().size()) {
                    List<Integer> replicaIds = pi.replicas().stream().map(Node::id).toList();
                    List<Integer> isrIds = pi.isr().stream().map(Node::id).toList();
                    List<Integer> missingReplicas = replicaIds.stream()
                            .filter(id -> !isrIds.contains(id)).toList();

                    Map<String, Object> detail = new HashMap<>();
                    detail.put("topic", td.name());
                    detail.put("partition", pi.partition());
                    detail.put("leader", pi.leader() != null ? pi.leader().id() : -1);
                    detail.put("replica_count", pi.replicas().size());
                    detail.put("isr_count", pi.isr().size());
                    detail.put("replicas", replicaIds);
                    detail.put("isr", isrIds);
                    detail.put("missing_replicas", missingReplicas);
                    details.add(detail);
                }
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("timestamp", Instant.now().toString());
        result.put("under_replicated_partition_count", details.size());
        result.put("details", details);
        result.put("recommendations", List.of(
                "Check broker health for any offline or struggling brokers",
                "Verify network connectivity between brokers",
                "Monitor disk space on broker nodes",
                "Review broker logs for detailed error messages",
                "Consider increasing replication timeouts if network is slow"
        ));
        return result;
    }

    /**
     * Returns consumer group lag analysis with group summaries and high-lag details.
     */
    public Map<String, Object> getConsumerGroupLag(long threshold) throws Exception {
        var admin = getAdminClient();
        Collection<ConsumerGroupListing> groups = admin.listConsumerGroups()
                .all().get(ADMIN_TIMEOUT_SECONDS, TimeUnit.SECONDS);

        List<Map<String, Object>> groupSummaries = new ArrayList<>();
        List<Map<String, Object>> highLagDetails = new ArrayList<>();

        for (ConsumerGroupListing group : groups) {
            String groupId = group.groupId();
            // Skip internal MCP consumer groups
            if (groupId.startsWith(CONSUMER_GROUP_PREFIX)) {
                continue;
            }

            try {
                ListConsumerGroupOffsetsResult offsetsResult = admin.listConsumerGroupOffsets(groupId);
                Map<TopicPartition, OffsetAndMetadata> offsets = offsetsResult.partitionsToOffsetAndMetadata()
                        .get(ADMIN_TIMEOUT_SECONDS, TimeUnit.SECONDS);

                if (offsets.isEmpty()) {
                    continue;
                }

                // Get end offsets for the partitions
                Map<TopicPartition, OffsetSpec> endOffsetSpecs = offsets.keySet().stream()
                        .collect(Collectors.toMap(tp -> tp, tp -> OffsetSpec.latest()));
                Map<TopicPartition, ListOffsetsResult.ListOffsetsResultInfo> endOffsets =
                        admin.listOffsets(endOffsetSpecs).all().get(ADMIN_TIMEOUT_SECONDS, TimeUnit.SECONDS);

                long totalLag = 0;
                Set<String> topics = offsets.keySet().stream()
                        .map(TopicPartition::topic).collect(Collectors.toSet());

                for (Map.Entry<TopicPartition, OffsetAndMetadata> entry : offsets.entrySet()) {
                    TopicPartition tp = entry.getKey();
                    long currentOffset = entry.getValue().offset();
                    long logEndOffset = endOffsets.containsKey(tp) ? endOffsets.get(tp).offset() : currentOffset;
                    long lag = Math.max(0, logEndOffset - currentOffset);
                    totalLag += lag;

                    if (lag > threshold) {
                        Map<String, Object> lagDetail = new HashMap<>();
                        lagDetail.put("group_id", groupId);
                        lagDetail.put("topic", tp.topic());
                        lagDetail.put("partition", tp.partition());
                        lagDetail.put("current_offset", currentOffset);
                        lagDetail.put("log_end_offset", logEndOffset);
                        lagDetail.put("lag", lag);
                        highLagDetails.add(lagDetail);
                    }
                }

                var groupDescription = admin.describeConsumerGroups(List.of(groupId))
                        .all().get(ADMIN_TIMEOUT_SECONDS, TimeUnit.SECONDS).get(groupId);

                Map<String, Object> summary = new HashMap<>();
                summary.put("group_id", groupId);
                summary.put("state", groupDescription.state().toString());
                summary.put("member_count", groupDescription.members().size());
                summary.put("topic_count", topics.size());
                summary.put("total_lag", totalLag);
                summary.put("has_high_lag", totalLag > threshold);
                groupSummaries.add(summary);
            } catch (Exception e) {
                // Skip groups that can't be queried
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("timestamp", Instant.now().toString());
        result.put("lag_threshold", threshold);
        result.put("group_count", groupSummaries.size());
        result.put("group_summary", groupSummaries);
        result.put("high_lag_details", highLagDetails);
        result.put("recommendations", List.of(
                "Check consumer instances for errors or slowdowns",
                "Consider scaling up consumer groups with high lag",
                "Review consumer configuration settings",
                "Examine processing bottlenecks in consumer application logic"
        ));
        return result;
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
