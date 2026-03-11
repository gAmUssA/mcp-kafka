package com.github.imcf.mcp.kafka.tools.flink;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.imcf.mcp.kafka.client.flink.FlinkSqlGatewayClient.StatementResult;

import io.quarkiverse.mcp.server.Tool;
import io.quarkiverse.mcp.server.ToolArg;
import io.quarkiverse.mcp.server.ToolResponse;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;
import java.util.stream.Collectors;

@ApplicationScoped
public class ListFlinkTablesHandler extends BaseFlinkToolHandler {

    @Inject
    ObjectMapper objectMapper;

    @Tool(name = "list-flink-tables", description = "List tables in a Flink database")
    public ToolResponse listFlinkTables(
            @ToolArg(description = "Catalog name (uses current catalog if omitted)", required = false) String catalog,
            @ToolArg(description = "Database name (uses current database if omitted)", required = false) String database) {

        ToolResponse guard = requireFlinkGateway();
        if (guard != null) return guard;

        try {
            if (isNotBlank(catalog)) {
                flinkClient.executeStatement("USE CATALOG " + catalog);
            }
            if (isNotBlank(database)) {
                flinkClient.executeStatement("USE " + database);
            }

            StatementResult result = flinkClient.executeStatement("SHOW TABLES");

            List<String> tables = result.getRows().stream()
                    .flatMap(row -> row.values().stream())
                    .map(String::valueOf)
                    .collect(Collectors.toList());

            return success(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(tables));
        } catch (Exception e) {
            return error(e);
        }
    }
}
