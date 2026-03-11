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
@QuarkusTestResource(value = SchemaRegistryTestResource.class, restrictToAnnotatedClass = true)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class SchemaCompatibilityTest {

    private static final String AVRO_SCHEMA = """
            {"type":"record","name":"CompatTest","fields":[{"name":"id","type":"int"}]}""";

    @Inject
    GetSchemaCompatibilityHandler getSchemaCompatibilityHandler;

    @Inject
    SetSchemaCompatibilityHandler setSchemaCompatibilityHandler;

    @Inject
    RegisterSchemaHandler registerSchemaHandler;

    @Test
    @Order(1)
    void testGetGlobalCompatibility() {
        var result = getSchemaCompatibilityHandler.getSchemaCompatibility(null);
        assertFalse(result.isError());
        String text = result.content().getFirst().asText().text();
        assertTrue(text.contains("Global compatibility"));
    }

    @Test
    @Order(2)
    void testSetGlobalCompatibility() {
        var result = setSchemaCompatibilityHandler.setSchemaCompatibility(null, "FULL");
        assertFalse(result.isError());
        String text = result.content().getFirst().asText().text();
        assertTrue(text.contains("Global compatibility set to FULL"));
    }

    @Test
    @Order(3)
    void testSetSubjectCompatibility() {
        registerSchemaHandler.registerSchema("compat-test-value", AVRO_SCHEMA, "AVRO", null);

        var setResult = setSchemaCompatibilityHandler.setSchemaCompatibility(
                "compat-test-value", "BACKWARD");
        assertFalse(setResult.isError());
        assertTrue(setResult.content().getFirst().asText().text().contains("BACKWARD"));

        var getResult = getSchemaCompatibilityHandler.getSchemaCompatibility("compat-test-value");
        assertFalse(getResult.isError());
        assertTrue(getResult.content().getFirst().asText().text().contains("BACKWARD"));
    }
}
