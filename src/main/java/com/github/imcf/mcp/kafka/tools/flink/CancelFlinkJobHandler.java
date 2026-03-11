package com.github.imcf.mcp.kafka.tools.flink;

import io.quarkiverse.mcp.server.Tool;
import io.quarkiverse.mcp.server.ToolArg;
import io.quarkiverse.mcp.server.ToolResponse;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class CancelFlinkJobHandler extends BaseFlinkToolHandler {

    @Tool(name = "cancel-flink-job", description = "Cancel a running Flink SQL operation")
    public ToolResponse cancelFlinkJob(
            @ToolArg(description = "The operation handle to cancel") String operationHandle) {

        ToolResponse guard = requireFlinkGateway();
        if (guard != null) return guard;

        if (isBlank(operationHandle)) {
            return error("operationHandle is required");
        }

        try {
            String status = flinkClient.cancelOperation(operationHandle);
            return success("Operation " + operationHandle + " canceled. Status: " + status);
        } catch (Exception e) {
            return error(e);
        }
    }
}
