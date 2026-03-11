package com.github.imcf.mcp.kafka.serde;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.apache.avro.generic.GenericRecord;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.imcf.mcp.kafka.config.SchemaRegistryConfig;

import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class SchemaRegistryDeserializer {

    private static final byte MAGIC_BYTE = 0x00;

    @Inject
    SchemaRegistryConfig config;

    @Inject
    ObjectMapper objectMapper;

    private volatile KafkaAvroDeserializer avroDeserializer;

    private Map<String, Object> deserializerConfig() {
        Map<String, Object> props = new HashMap<>();
        props.put(AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG,
                config.url().orElseThrow());
        config.auth().ifPresent(auth -> {
            auth.username().ifPresent(u -> auth.password().ifPresent(p -> {
                props.put("basic.auth.credentials.source", "USER_INFO");
                props.put("basic.auth.user.info", u + ":" + p);
            }));
        });
        return props;
    }

    private KafkaAvroDeserializer getAvroDeserializer() {
        if (avroDeserializer == null) {
            synchronized (this) {
                if (avroDeserializer == null) {
                    avroDeserializer = new KafkaAvroDeserializer();
                    avroDeserializer.configure(deserializerConfig(), false);
                }
            }
        }
        return avroDeserializer;
    }

    /**
     * Check if the byte array uses Confluent wire format (starts with magic byte 0x00).
     */
    public boolean isWireFormat(byte[] data) {
        return data != null && data.length >= 5 && data[0] == MAGIC_BYTE;
    }

    /**
     * Deserialize a Confluent wire format message to a JSON string.
     */
    public String deserialize(byte[] data) throws Exception {
        if (!isWireFormat(data)) {
            return new String(data, StandardCharsets.UTF_8);
        }

        // Use Confluent's KafkaAvroDeserializer which handles wire format + schema lookup
        Object result = getAvroDeserializer().deserialize(null, data);

        if (result instanceof GenericRecord) {
            return result.toString();
        }
        return String.valueOf(result);
    }
}
