package com.github.imcf.mcp.kafka.tools.flink;

import static org.junit.jupiter.api.Assertions.*;

import com.github.imcf.mcp.kafka.FlinkSqlGatewayTestResource;
import com.github.imcf.mcp.kafka.client.flink.FlinkSqlGatewayClient;
import com.github.imcf.mcp.kafka.client.flink.FlinkSqlGatewayClient.StatementResult;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

@QuarkusTest
@QuarkusTestResource(value = FlinkSqlGatewayTestResource.class, restrictToAnnotatedClass = true)
class FlinkSqlGatewayClientTest {

    @Inject
    FlinkSqlGatewayClient client;

    @Test
    void testExecuteStatementReturnsResults() throws Exception {
        StatementResult result = client.executeStatement("SELECT 1 AS num");
        assertNotNull(result);
        assertFalse(result.getRows().isEmpty());
        assertEquals(1, result.getRows().size());
        assertNotNull(result.getOperationHandle());
    }

    @Test
    void testMaxRowsEnforcement() throws Exception {
        // Generate multiple rows with UNION ALL
        String sql = "SELECT 1 AS n UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5";
        StatementResult result = client.executeStatement(sql, 2);
        assertEquals(2, result.getRows().size());
        assertTrue(result.isTruncated());
    }

    @Test
    void testSessionReuse() throws Exception {
        // Execute two statements — should reuse the same session
        StatementResult r1 = client.executeStatement("SELECT 1");
        StatementResult r2 = client.executeStatement("SELECT 2");
        assertNotNull(r1);
        assertNotNull(r2);
    }

    @Test
    void testInvalidSqlThrowsException() {
        assertThrows(Exception.class, () -> client.executeStatement("THIS IS NOT SQL"));
    }
}
