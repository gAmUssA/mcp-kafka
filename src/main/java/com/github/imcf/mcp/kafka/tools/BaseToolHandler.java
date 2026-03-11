package com.github.imcf.mcp.kafka.tools;

import org.jboss.logging.Logger;

import io.quarkiverse.mcp.server.ToolResponse;

/**
 * Base class for MCP tool handlers providing common logging and response utilities.
 */
public abstract class BaseToolHandler {

    protected final Logger log = Logger.getLogger(getClass());

    protected ToolResponse success(String message) {
        log.infof("Tool [%s] completed successfully", getClass().getSimpleName());
        log.debugf("Tool [%s] response: %s", getClass().getSimpleName(), message);
        return ToolResponse.success(message);
    }

    protected ToolResponse error(String message) {
        log.warnf("Tool [%s] failed: %s", getClass().getSimpleName(), message);
        return ToolResponse.error("Error: " + (message != null ? message : "Unknown error"));
    }

    /**
     * Creates an error response and logs the full exception stack trace for debugging.
     */
    protected ToolResponse error(Exception e) {
        String message = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
        log.warnf(e, "Tool [%s] failed: %s", getClass().getSimpleName(), message);
        return ToolResponse.error("Error: " + message);
    }

    /**
     * Checks if a string is null or blank.
     */
    protected static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    /**
     * Checks if a string is not null and not blank.
     */
    protected static boolean isNotBlank(String value) {
        return value != null && !value.isBlank();
    }
}
