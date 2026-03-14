package com.github.imcf.mcp.kafka.prompts;

import java.util.List;
import java.util.Map;

import org.jboss.logging.Logger;

import io.quarkiverse.mcp.server.PromptMessage;
import io.quarkiverse.mcp.server.TextContent;

/**
 * Base class for MCP prompt handlers providing common markdown formatting utilities.
 */
public abstract class BasePromptHandler {

    protected final Logger log = Logger.getLogger(getClass());

    protected PromptMessage markdown(String content) {
        log.infof("Prompt [%s] completed successfully", getClass().getSimpleName());
        return PromptMessage.withUserRole(new TextContent(content));
    }

    protected String formatTable(List<String> headers, List<List<String>> rows) {
        StringBuilder sb = new StringBuilder();
        sb.append("| ").append(String.join(" | ", headers)).append(" |\n");
        sb.append("|").append(":---|".repeat(headers.size())).append("\n");
        for (List<String> row : rows) {
            sb.append("| ").append(String.join(" | ", row)).append(" |\n");
        }
        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    protected static <T> T get(Map<String, Object> map, String key) {
        return (T) map.get(key);
    }
}
