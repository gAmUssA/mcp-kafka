package com.github.imcf.mcp.kafka.tools.kafka;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.header.Header;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.imcf.mcp.kafka.client.KafkaClientManager;
import com.github.imcf.mcp.kafka.serde.SchemaRegistryDeserializer;
import com.github.imcf.mcp.kafka.tools.BaseToolHandler;

import io.quarkiverse.mcp.server.Tool;
import io.quarkiverse.mcp.server.ToolArg;
import io.quarkiverse.mcp.server.ToolResponse;
import jakarta.inject.Inject;

public class ConsumeMessagesHandler extends BaseToolHandler {

    @Inject
    KafkaClientManager kafkaClientManager;

    @Inject
    ObjectMapper objectMapper;

    @Inject
    SchemaRegistryDeserializer schemaRegistryDeserializer;

    @Tool(name = "consume-messages", description = "Consume messages from a Kafka topic.")
    ToolResponse consumeMessages(
            @ToolArg(description = "Topic name") String topic,
            @ToolArg(description = "Maximum number of messages to consume", defaultValue = "10") int maxMessages,
            @ToolArg(description = "Timeout in milliseconds", defaultValue = "5000") long timeoutMs,
            @ToolArg(description = "Start from the beginning of the topic", defaultValue = "true") boolean fromBeginning,
            @ToolArg(description = "Use Schema Registry for deserialization", defaultValue = "false") boolean useSchemaRegistry) {
        try (KafkaConsumer<byte[], byte[]> consumer = kafkaClientManager.createConsumer()) {
            List<TopicPartition> partitions = consumer.partitionsFor(topic).stream()
                .map(pi -> new TopicPartition(pi.topic(), pi.partition()))
                .toList();
            consumer.assign(partitions);

            if (fromBeginning) {
                consumer.seekToBeginning(partitions);
            } else {
                consumer.seekToEnd(partitions);
            }

            List<Map<String, Object>> messages = new ArrayList<>();
            long deadline = System.currentTimeMillis() + timeoutMs;

            while (messages.size() < maxMessages && System.currentTimeMillis() < deadline) {
                long remaining = deadline - System.currentTimeMillis();
                if (remaining <= 0) break;

                ConsumerRecords<byte[], byte[]> records = consumer.poll(
                    Duration.ofMillis(Math.min(remaining, 1000)));

                for (ConsumerRecord<byte[], byte[]> record : records) {
                    if (messages.size() >= maxMessages) break;

                    Map<String, Object> msg = new HashMap<>();
                    msg.put("topic", record.topic());
                    msg.put("partition", record.partition());
                    msg.put("offset", record.offset());
                    msg.put("timestamp", record.timestamp());
                    msg.put("key", record.key() != null ? new String(record.key(), StandardCharsets.UTF_8) : null);

                    if (record.value() != null) {
                        if (useSchemaRegistry && schemaRegistryDeserializer.isWireFormat(record.value())) {
                            msg.put("value", schemaRegistryDeserializer.deserialize(record.value()));
                        } else {
                            msg.put("value", new String(record.value(), StandardCharsets.UTF_8));
                        }
                    } else {
                        msg.put("value", null);
                    }

                    Map<String, String> headers = new HashMap<>();
                    for (Header header : record.headers()) {
                        headers.put(header.key(), new String(header.value(), StandardCharsets.UTF_8));
                    }
                    if (!headers.isEmpty()) {
                        msg.put("headers", headers);
                    }

                    messages.add(msg);
                }
            }

            return success(objectMapper.writeValueAsString(messages));
        } catch (Exception e) {
            return error(e.getMessage());
        }
    }
}
