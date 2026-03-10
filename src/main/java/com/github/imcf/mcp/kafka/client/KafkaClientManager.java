package com.github.imcf.mcp.kafka.client;

import java.util.Properties;
import java.util.UUID;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.ByteArraySerializer;

import com.github.imcf.mcp.kafka.config.KafkaConfig;

import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class KafkaClientManager {

    @Inject
    KafkaConfig kafkaConfig;

    private volatile AdminClient adminClient;
    private volatile KafkaProducer<byte[], byte[]> producer;

    public AdminClient getAdminClient() {
        if (adminClient == null) {
            synchronized (this) {
                if (adminClient == null) {
                    adminClient = AdminClient.create(kafkaConfig.toProperties());
                }
            }
        }
        return adminClient;
    }

    public KafkaProducer<byte[], byte[]> getProducer() {
        if (producer == null) {
            synchronized (this) {
                if (producer == null) {
                    Properties props = kafkaConfig.toProperties();
                    props.put("key.serializer", ByteArraySerializer.class.getName());
                    props.put("value.serializer", ByteArraySerializer.class.getName());
                    producer = new KafkaProducer<>(props);
                }
            }
        }
        return producer;
    }

    public KafkaConsumer<byte[], byte[]> createConsumer() {
        Properties props = kafkaConfig.toProperties();
        props.put("key.deserializer", ByteArrayDeserializer.class.getName());
        props.put("value.deserializer", ByteArrayDeserializer.class.getName());
        props.put("group.id", "mcp-kafka-oss-" + UUID.randomUUID());
        props.put("auto.offset.reset", "earliest");
        props.put("enable.auto.commit", "false");
        return new KafkaConsumer<>(props);
    }

    @PreDestroy
    void shutdown() {
        if (adminClient != null) {
            adminClient.close();
        }
        if (producer != null) {
            producer.close();
        }
    }
}
