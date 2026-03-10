package com.github.imcf.mcp.kafka.tools.kafka;

import static org.junit.jupiter.api.Assertions.*;

import com.github.imcf.mcp.kafka.KafkaTestResource;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

@QuarkusTest
@QuarkusTestResource(KafkaTestResource.class)
class SearchTopicsTest {

    @Inject
    CreateTopicsHandler createTopicsHandler;

    @Inject
    SearchTopicsByNameHandler searchTopicsByNameHandler;

    @Test
    void testSearchBySubstring() {
        createTopicsHandler.createTopics("my-app-orders,my-app-users,other-topic", 1, (short) 1);

        var result = searchTopicsByNameHandler.searchTopicsByName("my-app");
        assertFalse(result.isError());
        String topics = result.content().getFirst().asText().text();
        assertTrue(topics.contains("my-app-orders"));
        assertTrue(topics.contains("my-app-users"));
        assertFalse(topics.contains("other-topic"));
    }

    @Test
    void testSearchNoMatches() {
        var result = searchTopicsByNameHandler.searchTopicsByName("nonexistent-xyz");
        assertFalse(result.isError());
        String topics = result.content().getFirst().asText().text();
        assertTrue(topics.isEmpty());
    }
}
