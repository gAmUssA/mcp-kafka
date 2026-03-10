package com.github.imcf.mcp.kafka.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class ToolFilter {

    @Inject
    McpServerConfig config;

    private volatile Set<String> allowSet;
    private volatile Set<String> blockSet;

    public boolean isAllowed(String toolName) {
        Set<String> allow = getAllowSet();
        Set<String> block = getBlockSet();

        if (!allow.isEmpty() && !allow.contains(toolName)) {
            return false;
        }
        return !block.contains(toolName);
    }

    private Set<String> getAllowSet() {
        if (allowSet == null) {
            allowSet = loadSet(config.toolsAllow().orElse(null), config.toolsAllowFile().orElse(null));
        }
        return allowSet;
    }

    private Set<String> getBlockSet() {
        if (blockSet == null) {
            blockSet = loadSet(config.toolsBlock().orElse(null), config.toolsBlockFile().orElse(null));
        }
        return blockSet;
    }

    private static Set<String> loadSet(String csvValue, String filePath) {
        Set<String> result = new java.util.HashSet<>();
        if (csvValue != null && !csvValue.isBlank()) {
            result.addAll(Arrays.stream(csvValue.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet()));
        }
        if (filePath != null && !filePath.isBlank()) {
            try {
                Files.readAllLines(Path.of(filePath)).stream()
                    .map(String::trim)
                    .filter(s -> !s.isEmpty() && !s.startsWith("#"))
                    .forEach(result::add);
            } catch (IOException e) {
                throw new RuntimeException("Failed to read tool filter file: " + filePath, e);
            }
        }
        return result.isEmpty() ? Collections.emptySet() : Collections.unmodifiableSet(result);
    }
}
