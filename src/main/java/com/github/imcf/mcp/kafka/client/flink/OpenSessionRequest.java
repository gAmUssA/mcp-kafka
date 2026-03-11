package com.github.imcf.mcp.kafka.client.flink;

import java.util.Map;

public class OpenSessionRequest {

    private Map<String, String> properties;

    public OpenSessionRequest() {
    }

    public OpenSessionRequest(Map<String, String> properties) {
        this.properties = properties;
    }

    public Map<String, String> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, String> properties) {
        this.properties = properties;
    }
}
