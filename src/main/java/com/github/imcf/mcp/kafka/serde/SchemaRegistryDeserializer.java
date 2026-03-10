package com.github.imcf.mcp.kafka.serde;

import java.io.ByteArrayInputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import org.apache.avro.Schema;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.DatumReader;
import org.apache.avro.io.DecoderFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.imcf.mcp.kafka.client.SchemaRegistryClient;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class SchemaRegistryDeserializer {

    private static final byte MAGIC_BYTE = 0x00;

    @Inject
    SchemaRegistryClient schemaRegistryClient;

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

        ByteBuffer buffer = ByteBuffer.wrap(data);
        buffer.get(); // skip magic byte
        int schemaId = buffer.getInt();

        byte[] payload = new byte[buffer.remaining()];
        buffer.get(payload);

        JsonNode schemaNode = schemaRegistryClient.getSchemaById(schemaId);
        String schemaText = schemaNode.get("schema").asText();
        String schemaType = schemaNode.has("schemaType") ? schemaNode.get("schemaType").asText() : "AVRO";

        return switch (schemaType.toUpperCase()) {
            case "AVRO" -> deserializeAvro(payload, schemaText);
            case "JSON" -> new String(payload, StandardCharsets.UTF_8);
            case "PROTOBUF" -> deserializeProtobuf(payload, schemaText);
            default -> new String(payload, StandardCharsets.UTF_8);
        };
    }

    private String deserializeAvro(byte[] payload, String schemaText) throws Exception {
        Schema schema = new Schema.Parser().parse(schemaText);
        DatumReader<GenericRecord> reader = new GenericDatumReader<>(schema);
        GenericRecord record = reader.read(null,
                DecoderFactory.get().binaryDecoder(new ByteArrayInputStream(payload), null));
        return record.toString();
    }

    private String deserializeProtobuf(byte[] payload, String schemaText) throws Exception {
        // Skip the message index varint (first byte for simple cases)
        byte[] messageBytes = new byte[payload.length - 1];
        System.arraycopy(payload, 1, messageBytes, 0, messageBytes.length);

        // For Protobuf, return the raw bytes as a hex string since dynamic
        // deserialization requires the full descriptor which is complex
        // A production implementation would use the schema to build a descriptor
        // and deserialize to JSON via JsonFormat
        return new String(payload, StandardCharsets.UTF_8);
    }
}
