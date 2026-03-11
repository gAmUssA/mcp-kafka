package com.github.imcf.mcp.kafka;

import java.time.Duration;
import java.util.Map;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.kafka.KafkaContainer;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

public class FlinkSqlGatewayTestResource implements QuarkusTestResourceLifecycleManager {

    private static final String KAFKA_CONNECTOR_URL =
            "https://repo1.maven.org/maven2/org/apache/flink/flink-sql-connector-kafka/4.0.1-2.0/flink-sql-connector-kafka-4.0.1-2.0.jar";
    private static final String AVRO_CONFLUENT_URL =
            "https://repo1.maven.org/maven2/org/apache/flink/flink-sql-avro-confluent-registry/2.2.0/flink-sql-avro-confluent-registry-2.2.0.jar";

    private Network network;
    private KafkaContainer kafka;
    private GenericContainer<?> flink;

    @Override
    public Map<String, String> start() {
        network = Network.newNetwork();

        kafka = new KafkaContainer("apache/kafka:latest")
                .withNetwork(network)
                .withNetworkAliases("kafka")
                .withListener("kafka:19092");
        kafka.start();

        flink = new GenericContainer<>("flink:2.2.0-java21")
                .withNetwork(network)
                .withExposedPorts(8083)
                .withCreateContainerCmdModifier(cmd ->
                        cmd.withEntrypoint("bash", "-c",
                                "wget -q -P /opt/flink/lib/ " + KAFKA_CONNECTOR_URL + " " + AVRO_CONFLUENT_URL
                                        + " && /opt/flink/bin/sql-gateway.sh start-foreground"
                                        + " -Dsql-gateway.endpoint.rest.address=0.0.0.0"
                                        + " -Dsql-gateway.endpoint.rest.bind-address=0.0.0.0"
                                        + " -Dsql-gateway.session.idle-timeout=60000"
                                        + " -Dexecution.target=local"))
                .waitingFor(Wait.forHttp("/v1/info").forPort(8083).forStatusCode(200)
                        .withStartupTimeout(Duration.ofMinutes(2)))
                .dependsOn(kafka);
        flink.start();

        String flinkUrl = "http://" + flink.getHost() + ":" + flink.getMappedPort(8083);

        return Map.of(
                "kafka.bootstrap-servers", kafka.getBootstrapServers(),
                "flink-sql-gateway.url", flinkUrl);
    }

    @Override
    public void stop() {
        if (flink != null) {
            flink.stop();
        }
        if (kafka != null) {
            kafka.stop();
        }
        if (network != null) {
            network.close();
        }
    }
}
