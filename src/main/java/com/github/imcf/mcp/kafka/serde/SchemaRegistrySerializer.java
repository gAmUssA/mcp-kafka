package com.github.imcf.mcp.kafka.serde;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import org.apache.avro.Schema;
import org.apache.avro.generic.GenericDatumWriter;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.io.JsonDecoder;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.io.DecoderFactory;

import com.github.imcf.mcp.kafka.client.SchemaRegistryClient;
import com.google.protobuf.DescriptorProtos;
import com.google.protobuf.Descriptors;
import com.google.protobuf.DynamicMessage;
import com.google.protobuf.util.JsonFormat;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class SchemaRegistrySerializer {

    private static final byte MAGIC_BYTE = 0x00;

    @Inject
    SchemaRegistryClient schemaRegistryClient;

    /**
     * Serialize a JSON value using Confluent wire format.
     * Format: [magic byte 0x00][4-byte schema ID (big-endian)][serialized payload]
     */
    public byte[] serialize(String subject, String jsonValue, String schemaType, String schemaText) throws Exception {
        int schemaId = schemaRegistryClient.registerSchema(subject, schemaText, schemaType, null);

        byte[] payload;
        switch (schemaType.toUpperCase()) {
            case "AVRO":
                payload = serializeAvro(jsonValue, schemaText);
                break;
            case "JSON":
                payload = jsonValue.getBytes(StandardCharsets.UTF_8);
                break;
            case "PROTOBUF":
                payload = serializeProtobuf(jsonValue, schemaText);
                break;
            default:
                throw new IllegalArgumentException("Unsupported schema type: " + schemaType);
        }

        return wrapWithWireFormat(schemaId, payload);
    }

    private byte[] serializeAvro(String jsonValue, String schemaText) throws Exception {
        Schema schema = new Schema.Parser().parse(schemaText);
        GenericDatumReader<GenericRecord> reader = new GenericDatumReader<>(schema);
        JsonDecoder jsonDecoder = DecoderFactory.get().jsonDecoder(schema, jsonValue);
        GenericRecord record = reader.read(null, jsonDecoder);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        DatumWriter<GenericRecord> writer = new GenericDatumWriter<>(schema);
        var encoder = EncoderFactory.get().binaryEncoder(out, null);
        writer.write(record, encoder);
        encoder.flush();
        return out.toByteArray();
    }

    private byte[] serializeProtobuf(String jsonValue, String schemaText) throws Exception {
        DescriptorProtos.FileDescriptorProto fileProto = parseProtoSchema(schemaText);
        Descriptors.FileDescriptor fileDescriptor = Descriptors.FileDescriptor.buildFrom(
                fileProto, new Descriptors.FileDescriptor[]{});

        Descriptors.Descriptor messageDescriptor = fileDescriptor.getMessageTypes().get(0);
        DynamicMessage.Builder builder = DynamicMessage.newBuilder(messageDescriptor);
        JsonFormat.parser().merge(jsonValue, builder);

        // Protobuf wire format includes message index (varint 0 for first message type)
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(0); // message index varint for first message type
        builder.build().writeTo(out);
        return out.toByteArray();
    }

    private DescriptorProtos.FileDescriptorProto parseProtoSchema(String schemaText) {
        // Simple .proto parsing - extract message name and fields
        // For production use, a full proto parser would be needed
        // This handles the common case of a single message definition
        DescriptorProtos.FileDescriptorProto.Builder fileBuilder =
                DescriptorProtos.FileDescriptorProto.newBuilder();
        fileBuilder.setSyntax("proto3");

        DescriptorProtos.DescriptorProto.Builder msgBuilder =
                DescriptorProtos.DescriptorProto.newBuilder();

        String[] lines = schemaText.split("\n");
        int fieldNumber = 1;
        for (String line : lines) {
            line = line.trim();
            if (line.startsWith("message ")) {
                String msgName = line.replace("message ", "").replace("{", "").trim();
                msgBuilder.setName(msgName);
            } else if (line.contains("=") && !line.startsWith("//") && !line.startsWith("syntax")
                    && !line.startsWith("package")) {
                // Parse field: type name = number;
                String[] parts = line.replace(";", "").trim().split("\\s+");
                if (parts.length >= 3) {
                    String type = parts[0];
                    String name = parts[1];
                    DescriptorProtos.FieldDescriptorProto.Builder fieldBuilder =
                            DescriptorProtos.FieldDescriptorProto.newBuilder()
                                    .setName(name)
                                    .setNumber(Integer.parseInt(parts[3]))
                                    .setType(mapProtoType(type));
                    msgBuilder.addField(fieldBuilder);
                }
            }
        }

        fileBuilder.addMessageType(msgBuilder);
        return fileBuilder.build();
    }

    private DescriptorProtos.FieldDescriptorProto.Type mapProtoType(String type) {
        return switch (type) {
            case "string" -> DescriptorProtos.FieldDescriptorProto.Type.TYPE_STRING;
            case "int32" -> DescriptorProtos.FieldDescriptorProto.Type.TYPE_INT32;
            case "int64" -> DescriptorProtos.FieldDescriptorProto.Type.TYPE_INT64;
            case "float" -> DescriptorProtos.FieldDescriptorProto.Type.TYPE_FLOAT;
            case "double" -> DescriptorProtos.FieldDescriptorProto.Type.TYPE_DOUBLE;
            case "bool" -> DescriptorProtos.FieldDescriptorProto.Type.TYPE_BOOL;
            case "bytes" -> DescriptorProtos.FieldDescriptorProto.Type.TYPE_BYTES;
            default -> DescriptorProtos.FieldDescriptorProto.Type.TYPE_STRING;
        };
    }

    static byte[] wrapWithWireFormat(int schemaId, byte[] payload) {
        ByteBuffer buffer = ByteBuffer.allocate(1 + 4 + payload.length);
        buffer.put(MAGIC_BYTE);
        buffer.putInt(schemaId);
        buffer.put(payload);
        return buffer.array();
    }
}
