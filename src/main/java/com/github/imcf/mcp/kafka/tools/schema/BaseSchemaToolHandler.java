package com.github.imcf.mcp.kafka.tools.schema;

import com.github.imcf.mcp.kafka.client.SchemaRegistryClient;
import com.github.imcf.mcp.kafka.tools.BaseToolHandler;

import io.quarkiverse.mcp.server.ToolResponse;
import jakarta.inject.Inject;

/**
 * Base class for Schema Registry tool handlers.
 * Provides common functionality including SR client injection and configuration validation.
 */
public abstract class BaseSchemaToolHandler extends BaseToolHandler {

    @Inject
    protected SchemaRegistryClient schemaRegistryClient;

    /**
     * Checks if Schema Registry is configured and returns an error response if not.
     * @return error ToolResponse if not configured, null if configured
     */
    protected ToolResponse requireSchemaRegistry() {
        if (!schemaRegistryClient.isConfigured()) {
            return error("Schema Registry is not configured. Set schema-registry.url property.");
        }
        return null;
    }
}
