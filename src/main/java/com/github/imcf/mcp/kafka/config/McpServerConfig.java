package com.github.imcf.mcp.kafka.config;

import java.util.Optional;

import io.smallrye.config.ConfigMapping;

@ConfigMapping(prefix = "mcp")
public interface McpServerConfig {

    Optional<String> toolsAllow();

    Optional<String> toolsBlock();

    Optional<String> toolsAllowFile();

    Optional<String> toolsBlockFile();
}
