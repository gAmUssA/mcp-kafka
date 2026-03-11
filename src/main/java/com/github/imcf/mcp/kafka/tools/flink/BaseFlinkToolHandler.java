package com.github.imcf.mcp.kafka.tools.flink;

import com.github.imcf.mcp.kafka.client.flink.FlinkSqlGatewayClient;
import com.github.imcf.mcp.kafka.tools.BaseToolHandler;

import io.quarkiverse.mcp.server.ToolResponse;
import jakarta.inject.Inject;

public abstract class BaseFlinkToolHandler extends BaseToolHandler {

    @Inject
    protected FlinkSqlGatewayClient flinkClient;

    protected ToolResponse requireFlinkGateway() {
        if (!flinkClient.isConfigured()) {
            return error("Flink SQL Gateway is not configured. Set flink-sql-gateway.url property.");
        }
        return null;
    }
}
