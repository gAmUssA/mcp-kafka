package com.github.imcf.mcp.kafka.client;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

import org.jboss.logging.Logger;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.imcf.mcp.kafka.config.SchemaRegistryConfig;

import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * HTTP client for Confluent Schema Registry REST API.
 * Handles schema registration, retrieval, deletion, and compatibility configuration.
 */
@ApplicationScoped
public class SchemaRegistryClient {

    private static final Logger LOG = Logger.getLogger(SchemaRegistryClient.class);
    private static final String CONTENT_TYPE = "application/vnd.schemaregistry.v1+json";

    @Inject
    SchemaRegistryConfig config;

    @Inject
    ObjectMapper objectMapper;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @PreDestroy
    void shutdown() {
        // HttpClient doesn't have an explicit close, but we can help GC by nullifying
        // For Java 21+, HttpClient implements AutoCloseable, but for compatibility we log shutdown
        LOG.debug("SchemaRegistryClient shutting down");
    }

    public boolean isConfigured() {
        return config.url().isPresent() && !config.url().get().isBlank();
    }

    private String baseUrl() {
        return config.url().orElseThrow(() -> new IllegalStateException("Schema Registry URL not configured"));
    }

    private HttpRequest.Builder requestBuilder(String path) {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl() + path))
                .header("Accept", CONTENT_TYPE);

        config.auth().ifPresent(auth -> {
            if (auth.username().isPresent() && auth.password().isPresent()) {
                String credentials = auth.username().get() + ":" + auth.password().get();
                String encoded = Base64.getEncoder().encodeToString(
                        credentials.getBytes(StandardCharsets.UTF_8));
                builder.header("Authorization", "Basic " + encoded);
            }
        });

        return builder;
    }

    private HttpResponse<String> execute(HttpRequest request) throws Exception {
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 400) {
            String body = response.body();
            JsonNode error = objectMapper.readTree(body);
            String message = error.has("message") ? error.get("message").asText() : body;
            throw new SchemaRegistryException(response.statusCode(), message);
        }
        return response;
    }

    // --- Subjects ---

    public List<String> getSubjects(boolean deleted) throws Exception {
        String path = "/subjects" + (deleted ? "?deleted=true" : "");
        HttpResponse<String> response = execute(requestBuilder(path).GET().build());
        return objectMapper.readValue(response.body(), new TypeReference<>() {});
    }

    public List<Integer> getVersions(String subject, boolean deleted) throws Exception {
        String path = "/subjects/" + encode(subject) + "/versions" + (deleted ? "?deleted=true" : "");
        HttpResponse<String> response = execute(requestBuilder(path).GET().build());
        return objectMapper.readValue(response.body(), new TypeReference<>() {});
    }

    // --- Schemas ---

    public JsonNode getSchemaBySubjectVersion(String subject, String version) throws Exception {
        String ver = version != null ? version : "latest";
        String path = "/subjects/" + encode(subject) + "/versions/" + ver;
        HttpResponse<String> response = execute(requestBuilder(path).GET().build());
        return objectMapper.readTree(response.body());
    }

    public JsonNode getSchemaById(int schemaId) throws Exception {
        String path = "/schemas/ids/" + schemaId;
        HttpResponse<String> response = execute(requestBuilder(path).GET().build());
        return objectMapper.readTree(response.body());
    }

    public int registerSchema(String subject, String schema, String schemaType, String references) throws Exception {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("schema", schema);
        if (schemaType != null && !schemaType.isBlank()) {
            body.put("schemaType", schemaType);
        }
        if (references != null && !references.isBlank()) {
            body.set("references", objectMapper.readTree(references));
        }

        String path = "/subjects/" + encode(subject) + "/versions";
        HttpRequest request = requestBuilder(path)
                .header("Content-Type", CONTENT_TYPE)
                .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                .build();

        HttpResponse<String> response = execute(request);
        JsonNode result = objectMapper.readTree(response.body());
        return result.get("id").asInt();
    }

    // --- Delete ---

    public List<Integer> deleteSubject(String subject, boolean permanent) throws Exception {
        String path = "/subjects/" + encode(subject) + (permanent ? "?permanent=true" : "");
        HttpRequest request = requestBuilder(path).DELETE().build();
        HttpResponse<String> response = execute(request);
        return objectMapper.readValue(response.body(), new TypeReference<>() {});
    }

    public int deleteSchemaVersion(String subject, String version, boolean permanent) throws Exception {
        String path = "/subjects/" + encode(subject) + "/versions/" + version
                + (permanent ? "?permanent=true" : "");
        HttpRequest request = requestBuilder(path).DELETE().build();
        HttpResponse<String> response = execute(request);
        return objectMapper.readValue(response.body(), Integer.class);
    }

    // --- Compatibility ---

    public String getCompatibility(String subject) throws Exception {
        String path = subject != null
                ? "/config/" + encode(subject)
                : "/config";
        HttpResponse<String> response = execute(requestBuilder(path).GET().build());
        JsonNode result = objectMapper.readTree(response.body());
        return result.has("compatibilityLevel")
                ? result.get("compatibilityLevel").asText()
                : result.get("compatibility").asText();
    }

    public String setCompatibility(String subject, String level) throws Exception {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("compatibility", level);

        String path = subject != null
                ? "/config/" + encode(subject)
                : "/config";
        HttpRequest request = requestBuilder(path)
                .header("Content-Type", CONTENT_TYPE)
                .PUT(HttpRequest.BodyPublishers.ofString(body.toString()))
                .build();

        HttpResponse<String> response = execute(request);
        JsonNode result = objectMapper.readTree(response.body());
        return result.get("compatibility").asText();
    }

    /**
     * URL-encodes a value for use in Schema Registry REST API paths.
     */
    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    public static class SchemaRegistryException extends RuntimeException {
        private final int statusCode;

        public SchemaRegistryException(int statusCode, String message) {
            super(message);
            this.statusCode = statusCode;
        }

        public int getStatusCode() {
            return statusCode;
        }
    }
}
