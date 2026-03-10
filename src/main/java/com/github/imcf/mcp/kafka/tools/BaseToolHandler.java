package com.github.imcf.mcp.kafka.tools;

import io.quarkiverse.mcp.server.ToolResponse;

public abstract class BaseToolHandler {

    protected ToolResponse success(String message) {
        return ToolResponse.success(message);
    }

    protected ToolResponse error(String message) {
        return ToolResponse.error("Error: " + message);
    }
}
