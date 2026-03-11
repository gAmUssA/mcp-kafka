package com.github.imcf.mcp.kafka.tools.kafka;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.header.internals.RecordHeader;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.imcf.mcp.kafka.client.KafkaClientManager;
import com.github.imcf.mcp.kafka.serde.SchemaRegistrySerializer;
import com.github.imcf.mcp.kafka.tools.BaseToolHandler;

import io.quarkiverse.mcp.server.Tool;
import io.quarkiverse.mcp.server.ToolArg;
import io.quarkiverse.mcp.server.ToolResponse;
import jakarta.inject.Inject;

public class ProduceMessageHandler extends BaseToolHandler {

    private static final int PRODUCE_TIMEOUT_SECONDS = 30;
    private static final String DEFAULT_SCHEMA_TYPE = "AVRO";

    @Inject
    KafkaClientManager kafkaClientManager;

    @Inject
    SchemaRegistrySerializer schemaRegistrySerializer;

    @Inject
    ObjectMapper objectMapper;

    @Tool(name = "produce-message", description = "Produce a message to a Kafka topic.")
    ToolResponse produceMessage(
            @ToolArg(description = "Topic name") String topic,
            @ToolArg(description = "Message value") String value,
            @ToolArg(description = "Message key") String key,
            @ToolArg(description = "Partition number") Integer partition,
            @ToolArg(description = "Headers as JSON object (e.g. {\"key\":\"value\"})") String headers,
            @ToolArg(description = "Use Schema Registry for serialization", defaultValue = "false") boolean useSchemaRegistry,
            @ToolArg(description = "Schema type: AVRO, JSON, or PROTOBUF") String schemaType,
            @ToolArg(description = "Schema definition for SR serialization") String schema,
            @ToolArg(description = "Subject name for SR (defaults to <topic>-value)") String subject) {

        if (isBlank(topic)) {
            return error("Topic name is required");
        }
        if (partition != null && partition < 0) {
            return error("Partition number must be non-negative");
        }

        try {
            byte[] keyBytes = key != null ? key.getBytes(StandardCharsets.UTF_8) : null;
            byte[] valueBytes;

            if (useSchemaRegistry && value != null) {
                String subjectName = isNotBlank(subject) ? subject : topic + "-value";
                String type = isNotBlank(schemaType) ? schemaType : DEFAULT_SCHEMA_TYPE;
                valueBytes = schemaRegistrySerializer.serialize(subjectName, value, type, schema);
            } else {
                valueBytes = value != null ? value.getBytes(StandardCharsets.UTF_8) : null;
            }

            ProducerRecord<byte[], byte[]> record = new ProducerRecord<>(
                topic, partition, keyBytes, valueBytes);

            if (isNotBlank(headers)) {
                var parsed = objectMapper.readTree(headers);
                parsed.fields().forEachRemaining(entry ->
                    record.headers().add(new RecordHeader(
                        entry.getKey(),
                        entry.getValue().asText().getBytes(StandardCharsets.UTF_8))));
            }

            RecordMetadata metadata = kafkaClientManager.getProducer()
                .send(record)
                .get(PRODUCE_TIMEOUT_SECONDS, TimeUnit.SECONDS);

            return success(String.format(
                "Message produced to topic '%s', partition %d, offset %d",
                metadata.topic(), metadata.partition(), metadata.offset()));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return error("Operation interrupted");
        } catch (Exception e) {
            return error(e);
        }
    }
}
