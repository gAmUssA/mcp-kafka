package com.github.imcf.mcp.kafka.client.flink;

import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jboss.logging.Logger;

import com.github.imcf.mcp.kafka.config.FlinkSqlGatewayConfig;

import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.rest.client.RestClientBuilder;

@ApplicationScoped
public class FlinkSqlGatewayClient {

    private static final Logger LOG = Logger.getLogger(FlinkSqlGatewayClient.class);
    private static final long POLL_INTERVAL_MS = 200;

    @Inject
    FlinkSqlGatewayConfig config;

    private volatile FlinkSqlGatewayApi api;
    private volatile String sessionHandle;

    public boolean isConfigured() {
        return config.url().isPresent() && !config.url().get().isBlank();
    }

    private FlinkSqlGatewayApi getApi() {
        if (api == null) {
            synchronized (this) {
                if (api == null) {
                    String url = config.url().orElseThrow(
                            () -> new IllegalStateException("Flink SQL Gateway URL not configured"));
                    api = RestClientBuilder.newBuilder()
                            .baseUri(URI.create(url))
                            .build(FlinkSqlGatewayApi.class);
                    LOG.infof("Flink SQL Gateway client initialized: %s", url);
                }
            }
        }
        return api;
    }

    private String getOrCreateSession() {
        if (sessionHandle == null) {
            synchronized (this) {
                if (sessionHandle == null) {
                    Map<String, String> props = new LinkedHashMap<>();
                    config.defaultCatalog().ifPresent(c -> props.put("default-catalog", c));
                    config.defaultDatabase().ifPresent(d -> props.put("default-database", d));

                    OpenSessionRequest request = new OpenSessionRequest(props.isEmpty() ? null : props);
                    OpenSessionResponse response = getApi().openSession(request);
                    sessionHandle = response.getSessionHandle();
                    LOG.infof("Flink SQL Gateway session opened: %s", sessionHandle);
                }
            }
        }
        return sessionHandle;
    }

    private void invalidateSession() {
        synchronized (this) {
            sessionHandle = null;
        }
    }

    /**
     * Execute a SQL statement and return formatted results.
     * Handles session auto-recovery on session-not-found errors.
     */
    public StatementResult executeStatement(String sql, int maxRows) throws Exception {
        try {
            return doExecuteStatement(sql, maxRows);
        } catch (jakarta.ws.rs.WebApplicationException e) {
            if (e.getResponse().getStatus() == 404) {
                LOG.warn("Session not found, recreating session and retrying");
                invalidateSession();
                return doExecuteStatement(sql, maxRows);
            }
            throw e;
        }
    }

    /**
     * Execute a SQL statement using configured defaults for maxRows.
     */
    public StatementResult executeStatement(String sql) throws Exception {
        return executeStatement(sql, config.maxRows());
    }

    private StatementResult doExecuteStatement(String sql, int maxRows) throws Exception {
        String session = getOrCreateSession();
        boolean isStreamingDml = isStreamingStatement(sql);

        ExecuteStatementRequest request = new ExecuteStatementRequest(sql);
        ExecuteStatementResponse response = getApi().executeStatement(session, request);
        String opHandle = response.getOperationHandle();
        LOG.debugf("Statement submitted, operation handle: %s", opHandle);

        waitForCompletion(session, opHandle, isStreamingDml);

        if (isStreamingDml) {
            // For INSERT INTO, the job is running — return the operation handle without fetching results
            Map<String, Object> info = new LinkedHashMap<>();
            info.put("result", "Job submitted successfully");
            info.put("operationHandle", opHandle);
            return new StatementResult(null, List.of(info), false, opHandle);
        }

        return fetchAllResults(session, opHandle, maxRows);
    }

    /**
     * Check if the SQL is a streaming DML statement (INSERT INTO) that will
     * stay in RUNNING state rather than reaching FINISHED.
     */
    private boolean isStreamingStatement(String sql) {
        String trimmed = sql.trim().toUpperCase();
        return trimmed.startsWith("INSERT ");
    }

    private void waitForCompletion(String session, String operationHandle, boolean acceptRunning) throws Exception {
        long deadline = System.currentTimeMillis() + config.statementTimeout();

        while (System.currentTimeMillis() < deadline) {
            OperationStatusResponse status = getApi().getOperationStatus(session, operationHandle);
            String s = status.getStatus();

            if ("FINISHED".equals(s)) {
                return;
            }
            if (acceptRunning && "RUNNING".equals(s)) {
                LOG.infof("Streaming job is running, operation handle: %s", operationHandle);
                return;
            }
            if ("ERROR".equals(s)) {
                String errorMsg = status.getErrorMessage();
                throw new RuntimeException("Flink SQL statement failed: " +
                        (errorMsg != null ? errorMsg : s));
            }
            if ("CANCELED".equals(s)) {
                throw new RuntimeException("Flink SQL statement was canceled");
            }

            Thread.sleep(POLL_INTERVAL_MS);
        }

        // Timeout — cancel the operation
        try {
            getApi().cancelOperation(session, operationHandle);
        } catch (Exception e) {
            LOG.warnf("Failed to cancel timed-out operation: %s", e.getMessage());
        }
        throw new RuntimeException("Flink SQL statement timed out after " + config.statementTimeout() + "ms");
    }

    private StatementResult fetchAllResults(String session, String operationHandle, int maxRows) {
        List<Map<String, Object>> rows = new ArrayList<>();
        List<FetchResultsResponse.ColumnInfo> columns = null;
        boolean truncated = false;
        long token = 0;

        while (true) {
            FetchResultsResponse response = getApi().fetchResults(session, operationHandle, token, "JSON");

            if (response.getResults() != null) {
                if (columns == null && response.getResults().getColumns() != null) {
                    columns = response.getResults().getColumns();
                }

                if (response.getResults().getData() != null && columns != null) {
                    for (FetchResultsResponse.RowData row : response.getResults().getData()) {
                        if (rows.size() >= maxRows) {
                            truncated = true;
                            break;
                        }
                        Map<String, Object> rowMap = new LinkedHashMap<>();
                        List<Object> fields = row.getFields();
                        for (int i = 0; i < columns.size() && i < fields.size(); i++) {
                            rowMap.put(columns.get(i).getName(), fields.get(i));
                        }
                        rows.add(rowMap);
                    }
                }
            }

            if (truncated) {
                break;
            }

            String resultType = response.getResultType();
            if ("EOS".equals(resultType) || "END_OF_STREAM".equals(resultType)) {
                break;
            }
            if (response.getNextResultUri() == null) {
                break;
            }

            token++;
        }

        // Close the operation to free resources
        try {
            getApi().closeOperation(session, operationHandle);
        } catch (Exception e) {
            LOG.debugf("Failed to close operation %s: %s", operationHandle, e.getMessage());
        }

        return new StatementResult(columns, rows, truncated, operationHandle);
    }

    /**
     * Get the status of an operation.
     */
    public String getOperationStatus(String operationHandle) {
        String session = getOrCreateSession();
        OperationStatusResponse response = getApi().getOperationStatus(session, operationHandle);
        return response.getStatus();
    }

    /**
     * Cancel a running operation.
     */
    public String cancelOperation(String operationHandle) {
        String session = getOrCreateSession();
        OperationStatusResponse response = getApi().cancelOperation(session, operationHandle);
        return response.getStatus();
    }

    @PreDestroy
    void cleanup() {
        if (sessionHandle != null) {
            try {
                getApi().closeSession(sessionHandle);
                LOG.infof("Flink SQL Gateway session closed: %s", sessionHandle);
            } catch (Exception e) {
                LOG.debugf("Failed to close Flink session on shutdown: %s", e.getMessage());
            }
        }
    }

    /**
     * Result of a statement execution with formatted rows.
     */
    public static class StatementResult {
        private final List<FetchResultsResponse.ColumnInfo> columns;
        private final List<Map<String, Object>> rows;
        private final boolean truncated;
        private final String operationHandle;

        public StatementResult(List<FetchResultsResponse.ColumnInfo> columns,
                               List<Map<String, Object>> rows, boolean truncated, String operationHandle) {
            this.columns = columns;
            this.rows = rows;
            this.truncated = truncated;
            this.operationHandle = operationHandle;
        }

        public List<FetchResultsResponse.ColumnInfo> getColumns() {
            return columns;
        }

        public List<Map<String, Object>> getRows() {
            return rows;
        }

        public boolean isTruncated() {
            return truncated;
        }

        public String getOperationHandle() {
            return operationHandle;
        }
    }
}
