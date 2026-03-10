package com.github.imcf.mcp.kafka.tools.kafka;

import static org.junit.jupiter.api.Assertions.*;

import com.github.imcf.mcp.kafka.KafkaTestResource;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

@QuarkusTest
@QuarkusTestResource(KafkaTestResource.class)
class ProduceConsumeTest {

    @Inject
    CreateTopicsHandler createTopicsHandler;

    @Inject
    ProduceMessageHandler produceMessageHandler;

    @Inject
    ConsumeMessagesHandler consumeMessagesHandler;

    @Test
    void testProduceAndConsume() {
        createTopicsHandler.createTopics("produce-consume-test", 1, (short) 1);

        var produceResult = produceMessageHandler.produceMessage(
            "produce-consume-test", "hello world", "key1", null, null);
        assertFalse(produceResult.isError());
        assertTrue(produceResult.content().getFirst().asText().text().contains("Message produced"));

        var consumeResult = consumeMessagesHandler.consumeMessages(
            "produce-consume-test", 10, 10000, true);
        assertFalse(consumeResult.isError());
        String messages = consumeResult.content().getFirst().asText().text();
        assertTrue(messages.contains("hello world"));
        assertTrue(messages.contains("key1"));
    }

    @Test
    void testProduceWithHeaders() {
        createTopicsHandler.createTopics("headers-test", 1, (short) 1);

        var produceResult = produceMessageHandler.produceMessage(
            "headers-test", "with headers", null, null, "{\"content-type\":\"text/plain\"}");
        assertFalse(produceResult.isError());

        var consumeResult = consumeMessagesHandler.consumeMessages(
            "headers-test", 10, 10000, true);
        assertFalse(consumeResult.isError());
        String messages = consumeResult.content().getFirst().asText().text();
        assertTrue(messages.contains("content-type"));
    }
}
