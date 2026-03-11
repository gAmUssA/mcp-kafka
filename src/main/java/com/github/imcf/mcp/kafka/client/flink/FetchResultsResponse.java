package com.github.imcf.mcp.kafka.client.flink;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class FetchResultsResponse {

    private ResultSet results;
    private String resultType;
    private String nextResultUri;

    public ResultSet getResults() {
        return results;
    }

    public void setResults(ResultSet results) {
        this.results = results;
    }

    public String getResultType() {
        return resultType;
    }

    public void setResultType(String resultType) {
        this.resultType = resultType;
    }

    public String getNextResultUri() {
        return nextResultUri;
    }

    public void setNextResultUri(String nextResultUri) {
        this.nextResultUri = nextResultUri;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ResultSet {

        private List<ColumnInfo> columns;
        private List<RowData> data;

        public List<ColumnInfo> getColumns() {
            return columns;
        }

        public void setColumns(List<ColumnInfo> columns) {
            this.columns = columns;
        }

        public List<RowData> getData() {
            return data;
        }

        public void setData(List<RowData> data) {
            this.data = data;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ColumnInfo {

        private String name;
        private LogicalType logicalType;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public LogicalType getLogicalType() {
            return logicalType;
        }

        public void setLogicalType(LogicalType logicalType) {
            this.logicalType = logicalType;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class LogicalType {

        private String type;
        private boolean nullable;

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public boolean isNullable() {
            return nullable;
        }

        public void setNullable(boolean nullable) {
            this.nullable = nullable;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class RowData {

        private String kind;
        private List<Object> fields;

        public String getKind() {
            return kind;
        }

        public void setKind(String kind) {
            this.kind = kind;
        }

        public List<Object> getFields() {
            return fields;
        }

        public void setFields(List<Object> fields) {
            this.fields = fields;
        }
    }
}
