package com.github.imcf.mcp.kafka;

import java.util.Map;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.kafka.KafkaContainer;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

public class SchemaRegistryTestResource implements QuarkusTestResourceLifecycleManager {

    private Network network;
    private KafkaContainer kafka;
    private GenericContainer<?> schemaRegistry;

    @Override
    public Map<String, String> start() {
        network = Network.newNetwork();

        kafka = new KafkaContainer("apache/kafka:latest")
                .withNetwork(network)
                .withNetworkAliases("kafka")
                .withListener("kafka:19092");
        kafka.start();

        schemaRegistry = new GenericContainer<>("confluentinc/cp-schema-registry:7.9.0")
                .withNetwork(network)
                .withExposedPorts(8081)
                .withEnv("SCHEMA_REGISTRY_HOST_NAME", "schema-registry")
                .withEnv("SCHEMA_REGISTRY_KAFKASTORE_BOOTSTRAP_SERVERS", "kafka:19092")
                .withEnv("SCHEMA_REGISTRY_LISTENERS", "http://0.0.0.0:8081")
                .waitingFor(Wait.forHttp("/subjects").forStatusCode(200))
                .dependsOn(kafka);
        schemaRegistry.start();

        String srUrl = "http://" + schemaRegistry.getHost() + ":" + schemaRegistry.getMappedPort(8081);

        return Map.of(
                "kafka.bootstrap-servers", kafka.getBootstrapServers(),
                "schema-registry.url", srUrl);
    }

    @Override
    public void stop() {
        if (schemaRegistry != null) {
            schemaRegistry.stop();
        }
        if (kafka != null) {
            kafka.stop();
        }
        if (network != null) {
            network.close();
        }
    }
}
