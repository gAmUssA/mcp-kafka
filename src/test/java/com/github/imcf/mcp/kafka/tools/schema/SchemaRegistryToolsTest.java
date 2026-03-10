package com.github.imcf.mcp.kafka.tools.schema;

import static org.junit.jupiter.api.Assertions.*;

import com.github.imcf.mcp.kafka.SchemaRegistryTestResource;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

@QuarkusTest
@QuarkusTestResource(SchemaRegistryTestResource.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class SchemaRegistryToolsTest {

    private static final String AVRO_SCHEMA = """
            {"type":"record","name":"User","fields":[{"name":"name","type":"string"},{"name":"age","type":"int"}]}""";

    @Inject
    RegisterSchemaHandler registerSchemaHandler;

    @Inject
    ListSchemasHandler listSchemasHandler;

    @Inject
    GetSchemaHandler getSchemaHandler;

    @Inject
    DeleteSchemaHandler deleteSchemaHandler;

    @Test
    @Order(1)
    void testRegisterSchema() {
        var result = registerSchemaHandler.registerSchema(
                "test-value", AVRO_SCHEMA, "AVRO", null);
        assertFalse(result.isError());
        String text = result.content().getFirst().asText().text();
        assertTrue(text.contains("Schema registered with ID"));
        assertTrue(text.contains("test-value"));
    }

    @Test
    @Order(2)
    void testListSchemas() {
        var result = listSchemasHandler.listSchemas(null, false);
        assertFalse(result.isError());
        String text = result.content().getFirst().asText().text();
        assertTrue(text.contains("test-value"));
        assertTrue(text.contains("User"));
    }

    @Test
    @Order(3)
    void testListSchemasWithPrefix() {
        // Register another schema
        registerSchemaHandler.registerSchema(
                "other-subject-value", AVRO_SCHEMA, "AVRO", null);

        var result = listSchemasHandler.listSchemas("test-", false);
        assertFalse(result.isError());
        String text = result.content().getFirst().asText().text();
        assertTrue(text.contains("test-value"));
        assertFalse(text.contains("other-subject"));
    }

    @Test
    @Order(4)
    void testGetSchema() {
        var result = getSchemaHandler.getSchema("test-value", null);
        assertFalse(result.isError());
        String text = result.content().getFirst().asText().text();
        assertTrue(text.contains("User"));
        assertTrue(text.contains("name"));
    }

    @Test
    @Order(5)
    void testGetSchemaSpecificVersion() {
        var result = getSchemaHandler.getSchema("test-value", "1");
        assertFalse(result.isError());
        String text = result.content().getFirst().asText().text();
        assertTrue(text.contains("User"));
    }

    @Test
    @Order(6)
    void testDeleteSchema() {
        registerSchemaHandler.registerSchema(
                "delete-me-value", AVRO_SCHEMA, "AVRO", null);

        var result = deleteSchemaHandler.deleteSchema("delete-me-value", null, false);
        assertFalse(result.isError());
        String text = result.content().getFirst().asText().text();
        assertTrue(text.contains("Deleted subject"));
        assertTrue(text.contains("delete-me-value"));
    }
}
