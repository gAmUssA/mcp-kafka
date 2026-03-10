package com.github.imcf.mcp.kafka.config;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ToolFilterTest {

    @Test
    void testNoFiltersAllowsAll() {
        ToolFilter filter = createFilter(null, null, null, null);
        assertTrue(filter.isAllowed("list-topics"));
        assertTrue(filter.isAllowed("delete-topics"));
    }

    @Test
    void testAllowListFilters() {
        ToolFilter filter = createFilter("list-topics,produce-message", null, null, null);
        assertTrue(filter.isAllowed("list-topics"));
        assertTrue(filter.isAllowed("produce-message"));
        assertFalse(filter.isAllowed("delete-topics"));
    }

    @Test
    void testBlockListFilters() {
        ToolFilter filter = createFilter(null, "delete-topics", null, null);
        assertTrue(filter.isAllowed("list-topics"));
        assertFalse(filter.isAllowed("delete-topics"));
    }

    @Test
    void testAllowAndBlockCombined() {
        ToolFilter filter = createFilter("list-topics,delete-topics", "delete-topics", null, null);
        assertTrue(filter.isAllowed("list-topics"));
        assertFalse(filter.isAllowed("delete-topics"));
        assertFalse(filter.isAllowed("produce-message"));
    }

    @Test
    void testFileBasedFilter(@TempDir Path tempDir) throws IOException {
        Path allowFile = tempDir.resolve("allow.txt");
        Files.writeString(allowFile, "list-topics\n# comment\nproduce-message\n");

        ToolFilter filter = createFilter(null, null, allowFile.toString(), null);
        assertTrue(filter.isAllowed("list-topics"));
        assertTrue(filter.isAllowed("produce-message"));
        assertFalse(filter.isAllowed("delete-topics"));
    }

    private ToolFilter createFilter(String allow, String block, String allowFile, String blockFile) {
        ToolFilter filter = new ToolFilter();
        try {
            var configField = ToolFilter.class.getDeclaredField("config");
            configField.setAccessible(true);
            configField.set(filter, new McpServerConfig() {
                @Override public Optional<String> toolsAllow() { return Optional.ofNullable(allow); }
                @Override public Optional<String> toolsBlock() { return Optional.ofNullable(block); }
                @Override public Optional<String> toolsAllowFile() { return Optional.ofNullable(allowFile); }
                @Override public Optional<String> toolsBlockFile() { return Optional.ofNullable(blockFile); }
            });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return filter;
    }
}
