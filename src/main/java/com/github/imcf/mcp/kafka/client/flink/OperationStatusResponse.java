package com.github.imcf.mcp.kafka.client.flink;

import java.util.Map;

public class OperationStatusResponse {

    private String status;
    private Map<String, Object> error;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Map<String, Object> getError() {
        return error;
    }

    public void setError(Map<String, Object> error) {
        this.error = error;
    }

    /**
     * Extract a human-readable error message from the error map, if present.
     */
    public String getErrorMessage() {
        if (error == null) return null;
        Object message = error.get("message");
        if (message != null) return message.toString();
        Object className = error.get("class");
        if (className != null) return className.toString();
        return error.toString();
    }
}
