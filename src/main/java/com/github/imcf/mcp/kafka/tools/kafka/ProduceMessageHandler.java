package com.github.imcf.mcp.kafka.tools.kafka;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.header.internals.RecordHeader;

import com.github.imcf.mcp.kafka.client.KafkaClientManager;
import com.github.imcf.mcp.kafka.tools.BaseToolHandler;

import io.quarkiverse.mcp.server.Tool;
import io.quarkiverse.mcp.server.ToolArg;
import io.quarkiverse.mcp.server.ToolResponse;
import jakarta.inject.Inject;

public class ProduceMessageHandler extends BaseToolHandler {

    @Inject
    KafkaClientManager kafkaClientManager;

    @Tool(name = "produce-message", description = "Produce a message to a Kafka topic.")
    ToolResponse produceMessage(
            @ToolArg(description = "Topic name") String topic,
            @ToolArg(description = "Message value") String value,
            @ToolArg(description = "Message key") String key,
            @ToolArg(description = "Partition number") Integer partition,
            @ToolArg(description = "Headers as JSON object (e.g. {\"key\":\"value\"})") String headers) {
        try {
            byte[] keyBytes = key != null ? key.getBytes(StandardCharsets.UTF_8) : null;
            byte[] valueBytes = value != null ? value.getBytes(StandardCharsets.UTF_8) : null;

            ProducerRecord<byte[], byte[]> record = new ProducerRecord<>(
                topic, partition, keyBytes, valueBytes);

            if (headers != null && !headers.isBlank()) {
                var parsed = com.fasterxml.jackson.databind.json.JsonMapper.builder().build()
                    .readTree(headers);
                parsed.fields().forEachRemaining(entry ->
                    record.headers().add(new RecordHeader(
                        entry.getKey(),
                        entry.getValue().asText().getBytes(StandardCharsets.UTF_8))));
            }

            RecordMetadata metadata = kafkaClientManager.getProducer()
                .send(record)
                .get(30, TimeUnit.SECONDS);

            return success(String.format(
                "Message produced to topic '%s', partition %d, offset %d",
                metadata.topic(), metadata.partition(), metadata.offset()));
        } catch (Exception e) {
            return error(e.getMessage());
        }
    }
}
