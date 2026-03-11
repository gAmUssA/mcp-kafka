package com.github.imcf.mcp.kafka.config;

import java.util.ArrayList;
import java.util.List;

import org.jboss.logging.Logger;

import io.quarkus.runtime.Startup;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class StartupLogger {

    private static final Logger LOG = Logger.getLogger(StartupLogger.class);

    @Inject
    KafkaConfig kafkaConfig;

    @Inject
    SchemaRegistryConfig schemaRegistryConfig;

    @Inject
    FlinkSqlGatewayConfig flinkSqlGatewayConfig;

    @Startup
    void logConnectionDetails() {
        LOG.info("==========================================================");
        LOG.info("  MCP Kafka Server - Connection Details");
        LOG.info("==========================================================");

        LOG.infof("  Kafka bootstrap servers : %s", kafkaConfig.bootstrapServers());
        LOG.infof("  Kafka security protocol : %s", kafkaConfig.securityProtocol());
        kafkaConfig.saslMechanism().ifPresent(m ->
                LOG.infof("  Kafka SASL mechanism    : %s", m));
        kafkaConfig.saslUsername().ifPresent(u ->
                LOG.infof("  Kafka SASL username     : %s", u));

        if (schemaRegistryConfig.url().isPresent() && !schemaRegistryConfig.url().get().isBlank()) {
            LOG.infof("  Schema Registry URL     : %s", schemaRegistryConfig.url().get());
            schemaRegistryConfig.auth().ifPresent(auth ->
                    auth.username().ifPresent(u ->
                            LOG.infof("  Schema Registry user    : %s", u)));
        } else {
            LOG.info("  Schema Registry         : not configured");
        }

        if (flinkSqlGatewayConfig.url().isPresent() && !flinkSqlGatewayConfig.url().get().isBlank()) {
            LOG.infof("  Flink SQL Gateway URL   : %s", flinkSqlGatewayConfig.url().get());
        } else {
            LOG.info("  Flink SQL Gateway       : not configured");
        }

        List<String> features = new ArrayList<>();
        features.add("Kafka tools");
        if (schemaRegistryConfig.url().isPresent() && !schemaRegistryConfig.url().get().isBlank()) {
            features.add("Schema Registry tools");
        }
        if (flinkSqlGatewayConfig.url().isPresent() && !flinkSqlGatewayConfig.url().get().isBlank()) {
            features.add("Flink SQL Gateway tools");
        }
        LOG.infof("  Enabled features        : %s", String.join(", ", features));
        LOG.info("==========================================================");
    }
}
