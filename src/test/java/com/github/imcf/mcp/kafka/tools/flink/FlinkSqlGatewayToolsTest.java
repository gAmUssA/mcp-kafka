package com.github.imcf.mcp.kafka.tools.flink;

import static org.junit.jupiter.api.Assertions.*;

import com.github.imcf.mcp.kafka.FlinkSqlGatewayTestResource;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

@QuarkusTest
@QuarkusTestResource(value = FlinkSqlGatewayTestResource.class, restrictToAnnotatedClass = true)
class FlinkSqlGatewayToolsTest {

    @Inject
    ExecuteFlinkSqlHandler executeFlinkSqlHandler;

    @Inject
    ListFlinkCatalogsHandler listFlinkCatalogsHandler;

    @Inject
    ListFlinkDatabasesHandler listFlinkDatabasesHandler;

    @Inject
    ListFlinkTablesHandler listFlinkTablesHandler;

    @Inject
    DescribeFlinkTableHandler describeFlinkTableHandler;

    @Test
    void testExecuteFlinkSql() {
        var result = executeFlinkSqlHandler.executeFlinkSql("SELECT 1 AS num", null);
        assertFalse(result.isError());
        String text = result.content().getFirst().asText().text();
        assertTrue(text.contains("\"num\""));
        assertTrue(text.contains("1"));
    }

    @Test
    void testExecuteFlinkSqlInvalidStatement() {
        var result = executeFlinkSqlHandler.executeFlinkSql("INVALID SQL STATEMENT", null);
        assertTrue(result.isError());
    }

    @Test
    void testListCatalogs() {
        var result = listFlinkCatalogsHandler.listFlinkCatalogs();
        assertFalse(result.isError());
        String text = result.content().getFirst().asText().text();
        assertTrue(text.contains("default_catalog"));
    }

    @Test
    void testListDatabases() {
        var result = listFlinkDatabasesHandler.listFlinkDatabases(null);
        assertFalse(result.isError());
        String text = result.content().getFirst().asText().text();
        assertTrue(text.contains("default_database"));
    }

    @Test
    void testListTables() {
        // Create a table first, then list
        executeFlinkSqlHandler.executeFlinkSql(
                "CREATE TABLE test_table (id INT, name STRING) WITH ('connector' = 'blackhole')", null);

        var result = listFlinkTablesHandler.listFlinkTables(null, null);
        assertFalse(result.isError());
        String text = result.content().getFirst().asText().text();
        assertTrue(text.contains("test_table"));
    }

    @Test
    void testDescribeTable() {
        // Ensure table exists
        executeFlinkSqlHandler.executeFlinkSql(
                "CREATE TABLE IF NOT EXISTS describe_test (id INT, name STRING) WITH ('connector' = 'blackhole')", null);

        var result = describeFlinkTableHandler.describeFlinkTable("describe_test");
        assertFalse(result.isError());
        String text = result.content().getFirst().asText().text();
        assertTrue(text.contains("describe_test"));
    }

    @Test
    void testDescribeNonExistentTable() {
        var result = describeFlinkTableHandler.describeFlinkTable("nonexistent_table_xyz");
        assertTrue(result.isError());
    }
}
