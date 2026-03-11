package com.github.imcf.mcp.kafka.client.flink;

import java.util.Map;

public class ExecuteStatementRequest {

    private String statement;
    private Map<String, String> executionConfig;
    private Long executionTimeout;

    public ExecuteStatementRequest() {
    }

    public ExecuteStatementRequest(String statement) {
        this.statement = statement;
    }

    public String getStatement() {
        return statement;
    }

    public void setStatement(String statement) {
        this.statement = statement;
    }

    public Map<String, String> getExecutionConfig() {
        return executionConfig;
    }

    public void setExecutionConfig(Map<String, String> executionConfig) {
        this.executionConfig = executionConfig;
    }

    public Long getExecutionTimeout() {
        return executionTimeout;
    }

    public void setExecutionTimeout(Long executionTimeout) {
        this.executionTimeout = executionTimeout;
    }
}
