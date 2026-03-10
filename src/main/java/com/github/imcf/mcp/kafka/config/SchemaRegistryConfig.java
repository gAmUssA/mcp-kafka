package com.github.imcf.mcp.kafka.config;

import java.util.Optional;

import io.smallrye.config.ConfigMapping;

@ConfigMapping(prefix = "schema-registry")
public interface SchemaRegistryConfig {

    Optional<String> url();

    Optional<Auth> auth();

    interface Auth {
        Optional<String> username();
        Optional<String> password();
    }
}
