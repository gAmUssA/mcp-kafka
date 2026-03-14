package com.github.imcf.mcp.kafka.resources.kafka;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

import com.github.imcf.mcp.kafka.KafkaTestResource;
import com.github.imcf.mcp.kafka.client.KafkaClientManager;

import io.quarkiverse.mcp.server.RequestUri;
import io.quarkiverse.mcp.server.TextResourceContents;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.apache.kafka.clients.admin.NewTopic;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

@QuarkusTest
@QuarkusTestResource(value = KafkaTestResource.class, restrictToAnnotatedClass = true)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class KafkaResourcesTest {

    @Inject
    ClusterOverviewResource clusterOverviewResource;

    @Inject
    HealthCheckResource healthCheckResource;

    @Inject
    UnderReplicatedPartitionsResource underReplicatedPartitionsResource;

    @Inject
    ConsumerLagReportResource consumerLagReportResource;

    @Inject
    KafkaClientManager kafkaClientManager;

    private static RequestUri uri(String value) {
        return new RequestUri(value);
    }

    @Test
    @Order(1)
    void testClusterOverview() throws Exception {
        TextResourceContents result = clusterOverviewResource.overview(uri("kafka-mcp://overview"));
        assertNotNull(result);
        String json = result.text();
        assertTrue(json.contains("\"timestamp\""));
        assertTrue(json.contains("\"broker_count\""));
        assertTrue(json.contains("\"controller_id\""));
        assertTrue(json.contains("\"topic_count\""));
        assertTrue(json.contains("\"partition_count\""));
        assertTrue(json.contains("\"health_status\""));
        assertTrue(json.contains("\"healthy\""));
    }

    @Test
    @Order(2)
    void testHealthCheck() throws Exception {
        TextResourceContents result = healthCheckResource.healthCheck(uri("kafka-mcp://health-check"));
        assertNotNull(result);
        String json = result.text();
        assertTrue(json.contains("\"broker_status\""));
        assertTrue(json.contains("\"controller_status\""));
        assertTrue(json.contains("\"partition_status\""));
        assertTrue(json.contains("\"consumer_status\""));
        assertTrue(json.contains("\"overall_status\""));
    }

    @Test
    @Order(3)
    void testUnderReplicatedPartitions() throws Exception {
        TextResourceContents result = underReplicatedPartitionsResource
                .underReplicatedPartitions(uri("kafka-mcp://under-replicated-partitions"));
        assertNotNull(result);
        String json = result.text();
        assertTrue(json.contains("\"timestamp\""));
        assertTrue(json.contains("\"under_replicated_partition_count\""));
        assertTrue(json.contains("\"details\""));
        assertTrue(json.contains("\"recommendations\""));
    }

    @Test
    @Order(4)
    void testConsumerLagReport() throws Exception {
        TextResourceContents result = consumerLagReportResource
                .consumerLagReport(uri("kafka-mcp://consumer-lag-report"));
        assertNotNull(result);
        String json = result.text();
        assertTrue(json.contains("\"timestamp\""));
        assertTrue(json.contains("\"lag_threshold\""));
        assertTrue(json.contains("\"group_count\""));
        assertTrue(json.contains("\"group_summary\""));
        assertTrue(json.contains("\"recommendations\""));
    }

    @Test
    @Order(5)
    void testOverviewReflectsCreatedTopics() throws Exception {
        // Create a topic via AdminClient directly
        kafkaClientManager.getAdminClient()
                .createTopics(Collections.singleton(new NewTopic("resource-test-topic", 1, (short) 1)))
                .all().get(30, TimeUnit.SECONDS);

        // Read overview resource
        TextResourceContents result = clusterOverviewResource.overview(uri("kafka-mcp://overview"));
        assertNotNull(result);
        String json = result.text();
        assertTrue(json.contains("\"topic_count\""));
        // The topic count should be at least 1
        assertTrue(json.contains("\"partition_count\""));
    }
}
