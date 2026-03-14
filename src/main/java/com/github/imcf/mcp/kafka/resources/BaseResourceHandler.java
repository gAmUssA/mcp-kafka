package com.github.imcf.mcp.kafka.resources;

import org.jboss.logging.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkiverse.mcp.server.TextResourceContents;
import jakarta.inject.Inject;

/**
 * Base class for MCP resource handlers providing common JSON serialization and error handling.
 */
public abstract class BaseResourceHandler {

    protected final Logger log = Logger.getLogger(getClass());

    @Inject
    ObjectMapper objectMapper;

    protected TextResourceContents jsonResource(String uri, Object data) throws Exception {
        String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(data);
        log.infof("Resource [%s] served successfully", getClass().getSimpleName());
        return new TextResourceContents(uri, json, "application/json");
    }
}
