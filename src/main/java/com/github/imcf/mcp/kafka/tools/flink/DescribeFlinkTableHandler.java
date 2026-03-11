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
public class DescribeFlinkTableHandler extends BaseFlinkToolHandler {

    @Inject
    ObjectMapper objectMapper;

    @Tool(name = "describe-flink-table", description = "Describe the schema of a Flink table")
    public ToolResponse describeFlinkTable(
            @ToolArg(description = "Table name (can be fully qualified: catalog.database.table)") String tableName) {

        ToolResponse guard = requireFlinkGateway();
        if (guard != null) return guard;

        if (isBlank(tableName)) {
            return error("tableName is required");
        }

        try {
            StatementResult result = flinkClient.executeStatement("DESCRIBE " + tableName);

            ObjectNode response = objectMapper.createObjectNode();
            response.put("table", tableName);

            ArrayNode columnsArray = response.putArray("columns");
            for (var row : result.getRows()) {
                columnsArray.add(objectMapper.valueToTree(row));
            }

            return success(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(response));
        } catch (Exception e) {
            return error(e);
        }
    }
}
