package com.github.imcf.mcp.kafka.config;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Optional;
import java.util.Properties;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@ConfigMapping(prefix = "kafka")
public interface KafkaConfig {

    @WithDefault("localhost:9092")
    String bootstrapServers();

    @WithDefault("PLAINTEXT")
    String securityProtocol();

    Optional<String> saslMechanism();

    Optional<String> saslUsername();

    Optional<String> saslPassword();

    Optional<String> sslTruststoreLocation();

    Optional<String> sslTruststorePassword();

    Optional<String> sslKeystoreLocation();

    Optional<String> sslKeystorePassword();

    Optional<String> propertiesFile();

    default Properties toProperties() {
        Properties props = new Properties();
        props.put("bootstrap.servers", bootstrapServers());
        props.put("security.protocol", securityProtocol());

        saslMechanism().ifPresent(v -> props.put("sasl.mechanism", v));
        saslUsername().ifPresent(username ->
            saslPassword().ifPresent(password -> {
                String jaasConfig = String.format(
                    "org.apache.kafka.common.security.plain.PlainLoginModule required username=\"%s\" password=\"%s\";",
                    username, password);
                if ("SCRAM-SHA-256".equals(saslMechanism().orElse("")) || "SCRAM-SHA-512".equals(saslMechanism().orElse(""))) {
                    jaasConfig = String.format(
                        "org.apache.kafka.common.security.scram.ScramLoginModule required username=\"%s\" password=\"%s\";",
                        username, password);
                }
                props.put("sasl.jaas.config", jaasConfig);
            })
        );

        sslTruststoreLocation().ifPresent(v -> props.put("ssl.truststore.location", v));
        sslTruststorePassword().ifPresent(v -> props.put("ssl.truststore.password", v));
        sslKeystoreLocation().ifPresent(v -> props.put("ssl.keystore.location", v));
        sslKeystorePassword().ifPresent(v -> props.put("ssl.keystore.password", v));

        propertiesFile().ifPresent(file -> {
            try (FileInputStream fis = new FileInputStream(file)) {
                Properties fileProps = new Properties();
                fileProps.load(fis);
                props.putAll(fileProps);
            } catch (IOException e) {
                throw new RuntimeException("Failed to load Kafka properties file: " + file, e);
            }
        });

        return props;
    }
}
