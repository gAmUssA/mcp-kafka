package com.github.imcf.mcp.kafka.tools.kafka;

import java.util.Set;
import java.util.concurrent.TimeUnit;

import com.github.imcf.mcp.kafka.client.KafkaClientManager;
import com.github.imcf.mcp.kafka.tools.BaseToolHandler;

import io.quarkiverse.mcp.server.Tool;
import io.quarkiverse.mcp.server.ToolResponse;
import jakarta.inject.Inject;

public class ListTopicsHandler extends BaseToolHandler {

    private static final int ADMIN_TIMEOUT_SECONDS = 30;

    @Inject
    KafkaClientManager kafkaClientManager;

    @Tool(name = "list-topics", description = "List all topics in the Kafka cluster.")
    ToolResponse listTopics() {
        try {
            Set<String> topics = kafkaClientManager.getAdminClient()
                .listTopics()
                .names()
                .get(ADMIN_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            return success(String.join(",", topics));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return error("Operation interrupted");
        } catch (Exception e) {
            return error(e);
        }
    }
}
