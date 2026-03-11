package com.github.imcf.mcp.kafka.serde;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.imcf.mcp.kafka.config.SchemaRegistryConfig;

import org.apache.avro.Schema;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.io.JsonDecoder;

import java.util.HashMap;
import java.util.Map;

import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig;
import io.confluent.kafka.serializers.KafkaAvroSerializer;
import io.confluent.kafka.serializers.json.KafkaJsonSchemaSerializer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class SchemaRegistrySerializer {

    @Inject
    SchemaRegistryConfig config;

    @Inject
    ObjectMapper objectMapper;

    private volatile KafkaAvroSerializer avroSerializer;
    private volatile KafkaJsonSchemaSerializer<JsonNode> jsonSchemaSerializer;

    private Map<String, Object> serializerConfig() {
        Map<String, Object> props = new HashMap<>();
        props.put(AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG,
                config.url().orElseThrow());
        props.put(AbstractKafkaSchemaSerDeConfig.AUTO_REGISTER_SCHEMAS, true);
        config.auth().ifPresent(auth -> {
            auth.username().ifPresent(u -> auth.password().ifPresent(p -> {
                props.put("basic.auth.credentials.source", "USER_INFO");
                props.put("basic.auth.user.info", u + ":" + p);
            }));
        });
        return props;
    }

    private KafkaAvroSerializer getAvroSerializer() {
        if (avroSerializer == null) {
            synchronized (this) {
                if (avroSerializer == null) {
                    avroSerializer = new KafkaAvroSerializer();
                    avroSerializer.configure(serializerConfig(), false);
                }
            }
        }
        return avroSerializer;
    }

    private KafkaJsonSchemaSerializer<JsonNode> getJsonSchemaSerializer() {
        if (jsonSchemaSerializer == null) {
            synchronized (this) {
                if (jsonSchemaSerializer == null) {
                    jsonSchemaSerializer = new KafkaJsonSchemaSerializer<>();
                    jsonSchemaSerializer.configure(serializerConfig(), false);
                }
            }
        }
        return jsonSchemaSerializer;
    }

    /**
     * Serialize a JSON value using Confluent wire format via the appropriate Confluent serializer.
     */
    public byte[] serialize(String subject, String jsonValue, String schemaType, String schemaText) throws Exception {
        // Subject naming: the Confluent serializers derive the subject from the topic name
        // We pass the topic name (extracted from subject convention: "<topic>-value" → "<topic>")
        String topic = subject.endsWith("-value") ? subject.substring(0, subject.length() - 6) : subject;

        return switch (schemaType.toUpperCase()) {
            case "AVRO" -> serializeAvro(topic, jsonValue, schemaText);
            case "JSON" -> serializeJsonSchema(topic, jsonValue);
            default -> throw new IllegalArgumentException("Unsupported schema type: " + schemaType);
        };
    }

    private byte[] serializeAvro(String topic, String jsonValue, String schemaText) throws Exception {
        Schema schema = new Schema.Parser().parse(schemaText);
        GenericDatumReader<GenericRecord> reader = new GenericDatumReader<>(schema);
        JsonDecoder jsonDecoder = DecoderFactory.get().jsonDecoder(schema, jsonValue);
        GenericRecord record = reader.read(null, jsonDecoder);

        return getAvroSerializer().serialize(topic, record);
    }

    private byte[] serializeJsonSchema(String topic, String jsonValue) throws Exception {
        JsonNode node = objectMapper.readTree(jsonValue);
        return getJsonSchemaSerializer().serialize(topic, node);
    }
}
