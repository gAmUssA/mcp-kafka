package com.github.imcf.mcp.kafka.tools.flink;

import io.quarkiverse.mcp.server.Tool;
import io.quarkiverse.mcp.server.ToolArg;
import io.quarkiverse.mcp.server.ToolResponse;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class GetFlinkJobStatusHandler extends BaseFlinkToolHandler {

    @Tool(name = "get-flink-job-status", description = "Get the status of a Flink SQL operation")
    public ToolResponse getFlinkJobStatus(
            @ToolArg(description = "The operation handle from a previous execute-flink-sql call") String operationHandle) {

        ToolResponse guard = requireFlinkGateway();
        if (guard != null) return guard;

        if (isBlank(operationHandle)) {
            return error("operationHandle is required");
        }

        try {
            String status = flinkClient.getOperationStatus(operationHandle);
            return success("Operation " + operationHandle + " status: " + status);
        } catch (Exception e) {
            return error(e);
        }
    }
}
