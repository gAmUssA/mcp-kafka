package com.github.imcf.mcp.kafka.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.imcf.mcp.kafka.config.SchemaRegistryConfig;

import org.jboss.logging.Logger;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.confluent.kafka.schemaregistry.client.CachedSchemaRegistryClient;
import io.confluent.kafka.schemaregistry.client.rest.RestService;
import io.confluent.kafka.schemaregistry.client.rest.entities.Schema;
import io.confluent.kafka.schemaregistry.client.rest.entities.SchemaReference;
import io.confluent.kafka.schemaregistry.client.rest.entities.SchemaString;
import io.confluent.kafka.schemaregistry.client.rest.entities.requests.ConfigUpdateRequest;
import io.confluent.kafka.schemaregistry.client.rest.entities.requests.RegisterSchemaRequest;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class SchemaRegistryClient {

    private static final Logger LOG = Logger.getLogger(SchemaRegistryClient.class);
    private static final int SCHEMA_CACHE_SIZE = 1000;

    @Inject
    SchemaRegistryConfig config;

    @Inject
    ObjectMapper objectMapper;

    private volatile CachedSchemaRegistryClient delegate;
    private volatile RestService restService;

    public boolean isConfigured() {
        return config.url().isPresent() && !config.url().get().isBlank();
    }

    private CachedSchemaRegistryClient getClient() {
        if (delegate == null) {
            synchronized (this) {
                if (delegate == null) {
                    String url = config.url().orElseThrow(
                            () -> new IllegalStateException("Schema Registry URL not configured"));

                    Map<String, Object> configs = new HashMap<>();
                    config.auth().ifPresent(auth -> {
                        auth.username().ifPresent(u -> {
                            auth.password().ifPresent(p -> {
                                configs.put("basic.auth.credentials.source", "USER_INFO");
                                configs.put("basic.auth.user.info", u + ":" + p);
                            });
                        });
                    });

                    restService = new RestService(url);
                    delegate = new CachedSchemaRegistryClient(restService, SCHEMA_CACHE_SIZE, configs);
                    LOG.infof("Schema Registry client initialized: %s", url);
                }
            }
        }
        return delegate;
    }

    private RestService getRestService() {
        getClient(); // ensure initialized
        return restService;
    }

    // --- Subjects ---

    public List<String> getSubjects(boolean deleted) throws Exception {
        return getRestService().getAllSubjects(deleted);
    }

    // --- Schemas ---

    public JsonNode getSchemaBySubjectVersion(String subject, String version) throws Exception {
        String ver = (version != null && !version.isBlank()) ? version : "latest";

        Schema schema;
        if ("latest".equals(ver)) {
            schema = getRestService().getLatestVersion(subject);
        } else {
            schema = getRestService().getVersion(subject, Integer.parseInt(ver));
        }

        ObjectNode result = objectMapper.createObjectNode();
        result.put("subject", schema.getSubject());
        result.put("version", schema.getVersion());
        result.put("id", schema.getId());
        result.put("schemaType", schema.getSchemaType() != null ? schema.getSchemaType() : "AVRO");
        result.put("schema", schema.getSchema());
        return result;
    }

    public JsonNode getSchemaById(int schemaId) throws Exception {
        SchemaString schemaString = getRestService().getId(schemaId);

        ObjectNode result = objectMapper.createObjectNode();
        result.put("schema", schemaString.getSchemaString());
        result.put("schemaType", schemaString.getSchemaType() != null ? schemaString.getSchemaType() : "AVRO");
        return result;
    }

    public int registerSchema(String subject, String schema, String schemaType, String references) throws Exception {
        RegisterSchemaRequest request = new RegisterSchemaRequest();
        request.setSchema(schema);
        if (schemaType != null && !schemaType.isBlank()) {
            request.setSchemaType(schemaType);
        }
        if (references != null && !references.isBlank()) {
            List<SchemaReference> refs = objectMapper.readValue(references,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, SchemaReference.class));
            request.setReferences(refs);
        }

        return getRestService().registerSchema(request, subject, false).getId();
    }

    // --- Delete ---

    public List<Integer> deleteSubject(String subject, boolean permanent) throws Exception {
        return getRestService().deleteSubject(Collections.emptyMap(), subject, permanent);
    }

    public int deleteSchemaVersion(String subject, String version, boolean permanent) throws Exception {
        return getRestService().deleteSchemaVersion(Collections.emptyMap(), subject, version, permanent);
    }

    // --- Compatibility ---

    public String getCompatibility(String subject) throws Exception {
        if (subject != null) {
            return getRestService().getConfig(subject).getCompatibilityLevel();
        }
        return getRestService().getConfig(null).getCompatibilityLevel();
    }

    public String setCompatibility(String subject, String level) throws Exception {
        ConfigUpdateRequest request = new ConfigUpdateRequest();
        request.setCompatibilityLevel(level);
        ConfigUpdateRequest response = getRestService().updateCompatibility(level, subject);
        return response.getCompatibilityLevel();
    }
}
