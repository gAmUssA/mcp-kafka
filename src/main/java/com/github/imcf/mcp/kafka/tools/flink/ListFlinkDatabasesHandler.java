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
public class ListFlinkDatabasesHandler extends BaseFlinkToolHandler {

    @Inject
    ObjectMapper objectMapper;

    @Tool(name = "list-flink-databases", description = "List databases in a Flink catalog")
    public ToolResponse listFlinkDatabases(
            @ToolArg(description = "Catalog name (uses current catalog if omitted)", required = false) String catalog) {

        ToolResponse guard = requireFlinkGateway();
        if (guard != null) return guard;

        try {
            if (isNotBlank(catalog)) {
                flinkClient.executeStatement("USE CATALOG " + catalog);
            }

            StatementResult result = flinkClient.executeStatement("SHOW DATABASES");

            List<String> databases = result.getRows().stream()
                    .flatMap(row -> row.values().stream())
                    .map(String::valueOf)
                    .collect(Collectors.toList());

            return success(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(databases));
        } catch (Exception e) {
            return error(e);
        }
    }
}
