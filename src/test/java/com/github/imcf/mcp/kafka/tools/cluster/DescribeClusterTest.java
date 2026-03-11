package com.github.imcf.mcp.kafka.tools.cluster;

import static org.junit.jupiter.api.Assertions.*;

import com.github.imcf.mcp.kafka.KafkaTestResource;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

@QuarkusTest
@QuarkusTestResource(value = KafkaTestResource.class, restrictToAnnotatedClass = true)
class DescribeClusterTest {

    @Inject
    DescribeClusterHandler describeClusterHandler;

    @Test
    void testDescribeCluster() {
        var result = describeClusterHandler.describeCluster();
        assertFalse(result.isError());
        String clusterInfo = result.content().getFirst().asText().text();
        assertTrue(clusterInfo.contains("clusterId"));
        assertTrue(clusterInfo.contains("controller"));
        assertTrue(clusterInfo.contains("brokers"));
    }
}
