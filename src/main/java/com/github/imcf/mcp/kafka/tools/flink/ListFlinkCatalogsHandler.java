package com.github.imcf.mcp.kafka.tools.flink;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.imcf.mcp.kafka.client.flink.FlinkSqlGatewayClient.StatementResult;

import io.quarkiverse.mcp.server.Tool;
import io.quarkiverse.mcp.server.ToolResponse;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;
import java.util.stream.Collectors;

@ApplicationScoped
public class ListFlinkCatalogsHandler extends BaseFlinkToolHandler {

    @Inject
    ObjectMapper objectMapper;

    @Tool(name = "list-flink-catalogs", description = "List all available Flink catalogs")
    public ToolResponse listFlinkCatalogs() {

        ToolResponse guard = requireFlinkGateway();
        if (guard != null) return guard;

        try {
            StatementResult result = flinkClient.executeStatement("SHOW CATALOGS");

            List<String> catalogs = result.getRows().stream()
                    .flatMap(row -> row.values().stream())
                    .map(String::valueOf)
                    .collect(Collectors.toList());

            return success(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(catalogs));
        } catch (Exception e) {
            return error(e);
        }
    }
}
