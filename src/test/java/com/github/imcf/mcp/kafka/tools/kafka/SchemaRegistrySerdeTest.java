package com.github.imcf.mcp.kafka.tools.kafka;

import static org.junit.jupiter.api.Assertions.*;

import com.github.imcf.mcp.kafka.SchemaRegistryTestResource;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

@QuarkusTest
@QuarkusTestResource(SchemaRegistryTestResource.class)
class SchemaRegistrySerdeTest {

    private static final String AVRO_SCHEMA = """
            {"type":"record","name":"User","fields":[{"name":"name","type":"string"},{"name":"age","type":"int"}]}""";

    @Inject
    CreateTopicsHandler createTopicsHandler;

    @Inject
    ProduceMessageHandler produceMessageHandler;

    @Inject
    ConsumeMessagesHandler consumeMessagesHandler;

    @Test
    void testAvroProduceConsumeRoundTrip() {
        String topic = "avro-serde-test";
        createTopicsHandler.createTopics(topic, 1, (short) 1);

        var produceResult = produceMessageHandler.produceMessage(
                topic,
                "{\"name\":\"Alice\",\"age\":30}",
                "key1",
                null, null,
                true, "AVRO", AVRO_SCHEMA, null);
        assertFalse(produceResult.isError());
        assertTrue(produceResult.content().getFirst().asText().text().contains("Message produced"));

        var consumeResult = consumeMessagesHandler.consumeMessages(
                topic, 10, 10000, true, true);
        assertFalse(consumeResult.isError());
        String messages = consumeResult.content().getFirst().asText().text();
        assertTrue(messages.contains("Alice"));
        assertTrue(messages.contains("30"));
    }

    @Test
    void testProduceWithoutSRConsumeNormally() {
        String topic = "raw-serde-test";
        createTopicsHandler.createTopics(topic, 1, (short) 1);

        var produceResult = produceMessageHandler.produceMessage(
                topic, "plain text", "key1", null, null,
                false, null, null, null);
        assertFalse(produceResult.isError());

        var consumeResult = consumeMessagesHandler.consumeMessages(
                topic, 10, 10000, true, false);
        assertFalse(consumeResult.isError());
        String messages = consumeResult.content().getFirst().asText().text();
        assertTrue(messages.contains("plain text"));
    }
}
