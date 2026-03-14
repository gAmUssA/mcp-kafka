package com.github.imcf.mcp.kafka.prompts.kafka;

import static org.junit.jupiter.api.Assertions.*;

import com.github.imcf.mcp.kafka.KafkaTestResource;

import io.quarkiverse.mcp.server.PromptMessage;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

@QuarkusTest
@QuarkusTestResource(value = KafkaTestResource.class, restrictToAnnotatedClass = true)
class KafkaPromptsTest {

    @Inject
    ClusterOverviewPrompt clusterOverviewPrompt;

    @Inject
    HealthCheckPrompt healthCheckPrompt;

    @Inject
    UnderReplicatedPartitionsPrompt underReplicatedPartitionsPrompt;

    @Inject
    ConsumerLagReportPrompt consumerLagReportPrompt;

    @Test
    void testClusterOverviewPrompt() throws Exception {
        PromptMessage result = clusterOverviewPrompt.clusterOverview();
        assertNotNull(result);
        String text = result.content().asText().text();
        assertTrue(text.contains("# Kafka Cluster Overview"));
        assertTrue(text.contains("Broker Count"));
        assertTrue(text.contains("Total Topics"));
        assertTrue(text.contains("Overall Status"));
    }

    @Test
    void testHealthCheckPrompt() throws Exception {
        PromptMessage result = healthCheckPrompt.healthCheck();
        assertNotNull(result);
        String text = result.content().asText().text();
        assertTrue(text.contains("# Kafka Cluster Health Check Report"));
        assertTrue(text.contains("## Broker Status"));
        assertTrue(text.contains("## Controller Status"));
        assertTrue(text.contains("## Partition Health"));
        assertTrue(text.contains("## Overall Health Assessment"));
    }

    @Test
    void testUnderReplicatedPartitionsPrompt() throws Exception {
        PromptMessage result = underReplicatedPartitionsPrompt.underReplicatedPartitions();
        assertNotNull(result);
        String text = result.content().asText().text();
        assertTrue(text.contains("# Under-Replicated Partitions Report"));
    }

    @Test
    void testConsumerLagReportDefaultThreshold() throws Exception {
        PromptMessage result = consumerLagReportPrompt.consumerLagReport("1000");
        assertNotNull(result);
        String text = result.content().asText().text();
        assertTrue(text.contains("# Kafka Consumer Lag Report"));
        assertTrue(text.contains("1000 messages"));
    }

    @Test
    void testConsumerLagReportCustomThreshold() throws Exception {
        PromptMessage result = consumerLagReportPrompt.consumerLagReport("5");
        assertNotNull(result);
        String text = result.content().asText().text();
        assertTrue(text.contains("# Kafka Consumer Lag Report"));
        assertTrue(text.contains("5 messages"));
    }
}
