package com.github.imcf.mcp.kafka.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.jboss.logging.Logger;

import io.quarkiverse.mcp.server.ToolManager;
import io.quarkus.runtime.Startup;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class ToolRegistrationFilter {

    private static final Logger LOG = Logger.getLogger(ToolRegistrationFilter.class);

    private static final Set<String> KAFKA_TOOLS = Set.of(
        "list-topics", "create-topics", "delete-topics",
        "produce-message", "consume-messages",
        "get-topic-config", "alter-topic-config",
        "search-topics-by-name",
        "describe-cluster", "list-consumer-groups", "describe-consumer-group"
    );

    @Inject
    ToolManager toolManager;

    @Inject
    ToolFilter toolFilter;

    @Inject
    KafkaConfig kafkaConfig;

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
            }
        }

        if (!removed.isEmpty()) {
            LOG.infof("Disabled tools: %s", removed);
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
