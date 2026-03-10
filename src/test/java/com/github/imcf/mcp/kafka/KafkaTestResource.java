package com.github.imcf.mcp.kafka;

import java.util.Map;

import org.testcontainers.kafka.KafkaContainer;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

public class KafkaTestResource implements QuarkusTestResourceLifecycleManager {

    private KafkaContainer kafka;

    @Override
    public Map<String, String> start() {
        kafka = new KafkaContainer("apache/kafka-native:latest");
        kafka.start();
        return Map.of("kafka.bootstrap-servers", kafka.getBootstrapServers());
    }

    @Override
    public void stop() {
        if (kafka != null) {
            kafka.stop();
        }
    }
}
