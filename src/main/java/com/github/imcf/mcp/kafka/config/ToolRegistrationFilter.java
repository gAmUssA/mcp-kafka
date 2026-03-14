package com.github.imcf.mcp.kafka.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.jboss.logging.Logger;

import com.github.imcf.mcp.kafka.client.SchemaRegistryClient;
import com.github.imcf.mcp.kafka.client.flink.FlinkSqlGatewayClient;

import io.quarkiverse.mcp.server.PromptManager;
import io.quarkiverse.mcp.server.ResourceManager;
import io.quarkiverse.mcp.server.ToolManager;
import io.quarkus.runtime.Startup;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class ToolRegistrationFilter {

    private static final Logger LOG = Logger.getLogger(ToolRegistrationFilter.class);

    private static final Set<String> KAFKA_RESOURCES = Set.of(
        "kafka-cluster-overview", "kafka-health-check",
        "kafka-under-replicated-partitions", "kafka-consumer-lag-report"
    );

    private static final Set<String> KAFKA_PROMPTS = Set.of(
        "kafka-cluster-overview", "kafka-health-check",
        "kafka-under-replicated-partitions", "kafka-consumer-lag-report"
    );

    private static final Set<String> KAFKA_TOOLS = Set.of(
        "list-topics", "create-topics", "delete-topics",
        "produce-message", "consume-messages",
        "get-topic-config", "alter-topic-config",
        "search-topics-by-name",
        "describe-cluster", "list-consumer-groups", "describe-consumer-group"
    );

    private static final Set<String> SR_TOOLS = Set.of(
        "list-schemas", "register-schema", "get-schema",
        "delete-schema", "get-schema-compatibility", "set-schema-compatibility"
    );

    private static final Set<String> FLINK_TOOLS = Set.of(
        "execute-flink-sql", "list-flink-catalogs", "list-flink-databases",
        "list-flink-tables", "describe-flink-table",
        "get-flink-job-status", "cancel-flink-job"
    );

    @Inject
    ToolManager toolManager;

    @Inject
    ResourceManager resourceManager;

    @Inject
    PromptManager promptManager;

    @Inject
    ToolFilter toolFilter;

    @Inject
    KafkaConfig kafkaConfig;

    @Inject
    SchemaRegistryClient schemaRegistryClient;

    @Inject
    FlinkSqlGatewayClient flinkSqlGatewayClient;

    @Startup
    void filterTools() {
        List<String> removed = new ArrayList<>();

        for (var info : toolManager) {
            String name = info.name();

            if (!toolFilter.isAllowed(name)) {
                toolManager.removeTool(name);
                removed.add(name + " (filtered)");
                continue;
            }

            if (KAFKA_TOOLS.contains(name) && isBlank(kafkaConfig.bootstrapServers())) {
                toolManager.removeTool(name);
                removed.add(name + " (kafka not configured)");
                continue;
            }

            if (SR_TOOLS.contains(name) && !schemaRegistryClient.isConfigured()) {
                toolManager.removeTool(name);
                removed.add(name + " (schema registry not configured)");
                continue;
            }

            if (FLINK_TOOLS.contains(name) && !flinkSqlGatewayClient.isConfigured()) {
                toolManager.removeTool(name);
                removed.add(name + " (flink sql gateway not configured)");
            }
        }

        if (!removed.isEmpty()) {
            LOG.infof("Disabled tools: %s", removed);
        }

        // Filter resources when Kafka is not configured
        if (isBlank(kafkaConfig.bootstrapServers())) {
            List<String> removedResources = new ArrayList<>();
            for (var info : resourceManager) {
                if (KAFKA_RESOURCES.contains(info.name())) {
                    resourceManager.removeResource(info.name());
                    removedResources.add(info.name());
                }
            }
            if (!removedResources.isEmpty()) {
                LOG.infof("Disabled resources (kafka not configured): %s", removedResources);
            }

            List<String> removedPrompts = new ArrayList<>();
            for (var info : promptManager) {
                if (KAFKA_PROMPTS.contains(info.name())) {
                    promptManager.removePrompt(info.name());
                    removedPrompts.add(info.name());
                }
            }
            if (!removedPrompts.isEmpty()) {
                LOG.infof("Disabled prompts (kafka not configured): %s", removedPrompts);
            }
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
