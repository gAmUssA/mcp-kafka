package com.github.imcf.mcp.kafka.config;

import java.util.Optional;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@ConfigMapping(prefix = "flink-sql-gateway")
public interface FlinkSqlGatewayConfig {

    Optional<String> url();

    Optional<String> defaultCatalog();

    Optional<String> defaultDatabase();

    Optional<Long> sessionIdleTimeout();

    @WithDefault("30000")
    long statementTimeout();

    @WithDefault("100")
    int maxRows();
}
