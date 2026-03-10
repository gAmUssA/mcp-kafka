package com.github.imcf.mcp.kafka.tools;

import org.jboss.logging.Logger;

import io.quarkiverse.mcp.server.ToolResponse;

public abstract class BaseToolHandler {

    protected final Logger log = Logger.getLogger(getClass());

    protected ToolResponse success(String message) {
        log.infof("Tool [%s] completed successfully", getClass().getSimpleName());
        log.debugf("Tool [%s] response: %s", getClass().getSimpleName(), message);
        return ToolResponse.success(message);
    }

    protected ToolResponse error(String message) {
        log.warnf("Tool [%s] failed: %s", getClass().getSimpleName(), message);
        return ToolResponse.error("Error: " + message);
    }
}
