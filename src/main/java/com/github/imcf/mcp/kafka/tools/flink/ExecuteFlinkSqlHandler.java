package com.github.imcf.mcp.kafka.tools.flink;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.imcf.mcp.kafka.client.flink.FlinkSqlGatewayClient.StatementResult;

import io.quarkiverse.mcp.server.Tool;
import io.quarkiverse.mcp.server.ToolArg;
import io.quarkiverse.mcp.server.ToolResponse;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class ExecuteFlinkSqlHandler extends BaseFlinkToolHandler {

    @Inject
    ObjectMapper objectMapper;

    @Tool(name = "execute-flink-sql", description = "Execute a Flink SQL statement and return results")
    public ToolResponse executeFlinkSql(
            @ToolArg(description = "The Flink SQL statement to execute") String statement,
            @ToolArg(description = "Maximum number of result rows to return (default: 100)", required = false) Integer maxRows) {

        ToolResponse guard = requireFlinkGateway();
        if (guard != null) return guard;

        if (isBlank(statement)) {
            return error("statement is required");
        }

        try {
            int limit = (maxRows != null && maxRows > 0) ? maxRows : 100;
            StatementResult result = flinkClient.executeStatement(statement, limit);

            ObjectNode response = objectMapper.createObjectNode();
            response.put("statement", statement);
            response.put("rowCount", result.getRows().size());
            response.put("truncated", result.isTruncated());
            response.put("operationHandle", result.getOperationHandle());

            if (result.getColumns() != null && !result.getColumns().isEmpty()) {
                ArrayNode cols = response.putArray("columns");
                for (var col : result.getColumns()) {
                    ObjectNode c = cols.addObject();
                    c.put("name", col.getName());
                    if (col.getLogicalType() != null) {
                        c.put("type", col.getLogicalType().getType());
                        c.put("nullable", col.getLogicalType().isNullable());
                    }
                }
            }

            ArrayNode dataArray = response.putArray("data");
            for (var row : result.getRows()) {
                dataArray.add(objectMapper.valueToTree(row));
            }

            return success(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(response));
        } catch (Exception e) {
            return error(e);
        }
    }
}
