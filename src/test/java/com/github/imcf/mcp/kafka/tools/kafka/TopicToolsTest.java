package com.github.imcf.mcp.kafka.tools.kafka;

import static org.junit.jupiter.api.Assertions.*;

import com.github.imcf.mcp.kafka.KafkaTestResource;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

@QuarkusTest
@QuarkusTestResource(value = KafkaTestResource.class, restrictToAnnotatedClass = true)
class TopicToolsTest {

    @Inject
    ListTopicsHandler listTopicsHandler;

    @Inject
    CreateTopicsHandler createTopicsHandler;

    @Inject
    DeleteTopicsHandler deleteTopicsHandler;

    @Test
    void testCreateAndListTopics() {
        var createResult = createTopicsHandler.createTopics("test-topic-1,test-topic-2", 1, (short) 1);
        assertFalse(createResult.isError());
        assertTrue(createResult.content().getFirst().asText().text().contains("Created topics"));

        var listResult = listTopicsHandler.listTopics();
        assertFalse(listResult.isError());
        String topics = listResult.content().getFirst().asText().text();
        assertTrue(topics.contains("test-topic-1"));
        assertTrue(topics.contains("test-topic-2"));
    }

    @Test
    void testDeleteTopics() {
        createTopicsHandler.createTopics("topic-to-delete", 1, (short) 1);

        var deleteResult = deleteTopicsHandler.deleteTopics("topic-to-delete");
        assertFalse(deleteResult.isError());
        assertTrue(deleteResult.content().getFirst().asText().text().contains("Deleted topics"));
    }
}
