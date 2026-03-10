package com.github.imcf.mcp.kafka.tools.kafka;

import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import com.github.imcf.mcp.kafka.client.KafkaClientManager;
import com.github.imcf.mcp.kafka.tools.BaseToolHandler;

import io.quarkiverse.mcp.server.Tool;
import io.quarkiverse.mcp.server.ToolArg;
import io.quarkiverse.mcp.server.ToolResponse;
import jakarta.inject.Inject;

public class SearchTopicsByNameHandler extends BaseToolHandler {

    @Inject
    KafkaClientManager kafkaClientManager;

    @Tool(name = "search-topics-by-name", description = "Search topics by name using regex or substring matching.")
    ToolResponse searchTopicsByName(
            @ToolArg(description = "Search term (substring or regex pattern)") String searchTerm) {
        try {
            Set<String> allTopics = kafkaClientManager.getAdminClient()
                    .listTopics()
                    .names()
                    .get(30, TimeUnit.SECONDS);

            Pattern pattern;
            try {
                pattern = Pattern.compile(searchTerm, Pattern.CASE_INSENSITIVE);
            } catch (Exception e) {
                // If not a valid regex, use as literal substring
                pattern = Pattern.compile(Pattern.quote(searchTerm), Pattern.CASE_INSENSITIVE);
            }

            Pattern finalPattern = pattern;
            List<String> matched = allTopics.stream()
                    .filter(topic -> finalPattern.matcher(topic).find())
                    .sorted()
                    .toList();

            return success(String.join(",", matched));
        } catch (Exception e) {
            return error(e.getMessage());
        }
    }
}
