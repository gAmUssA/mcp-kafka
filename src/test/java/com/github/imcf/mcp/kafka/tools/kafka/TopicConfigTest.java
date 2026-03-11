package com.github.imcf.mcp.kafka.tools.kafka;

import static org.junit.jupiter.api.Assertions.*;

import com.github.imcf.mcp.kafka.KafkaTestResource;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

@QuarkusTest
@QuarkusTestResource(value = KafkaTestResource.class, restrictToAnnotatedClass = true)
class TopicConfigTest {

    @Inject
    CreateTopicsHandler createTopicsHandler;

    @Inject
    GetTopicConfigHandler getTopicConfigHandler;

    @Inject
    AlterTopicConfigHandler alterTopicConfigHandler;

    @Test
    void testGetTopicConfig() {
        createTopicsHandler.createTopics("config-test-topic", 1, (short) 1);

        var result = getTopicConfigHandler.getTopicConfig("config-test-topic");
        assertFalse(result.isError());
        String config = result.content().getFirst().asText().text();
        assertTrue(config.contains("retention.ms"));
    }

    @Test
    void testAlterTopicConfig() {
        createTopicsHandler.createTopics("alter-config-topic", 1, (short) 1);

        var alterResult = alterTopicConfigHandler.alterTopicConfig(
            "alter-config-topic",
            "[{\"name\":\"retention.ms\",\"value\":\"86400000\"}]",
            false);
        assertFalse(alterResult.isError());
        assertTrue(alterResult.content().getFirst().asText().text().contains("Applied"));

        var getResult = getTopicConfigHandler.getTopicConfig("alter-config-topic");
        String config = getResult.content().getFirst().asText().text();
        assertTrue(config.contains("86400000"));
    }

    @Test
    void testValidateOnly() {
        createTopicsHandler.createTopics("validate-config-topic", 1, (short) 1);

        var result = alterTopicConfigHandler.alterTopicConfig(
            "validate-config-topic",
            "[{\"name\":\"retention.ms\",\"value\":\"86400000\"}]",
            true);
        assertFalse(result.isError());
        assertTrue(result.content().getFirst().asText().text().contains("Validated"));
    }
}
