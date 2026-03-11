package com.github.imcf.mcp.kafka.client.flink;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

@Path("/v1")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface FlinkSqlGatewayApi {

    // --- Sessions ---

    @POST
    @Path("/sessions")
    OpenSessionResponse openSession(OpenSessionRequest request);

    @DELETE
    @Path("/sessions/{sessionHandle}")
    void closeSession(@PathParam("sessionHandle") String sessionHandle);

    // --- Statements ---

    @POST
    @Path("/sessions/{sessionHandle}/statements")
    ExecuteStatementResponse executeStatement(
            @PathParam("sessionHandle") String sessionHandle,
            ExecuteStatementRequest request);

    // --- Operations ---

    @GET
    @Path("/sessions/{sessionHandle}/operations/{operationHandle}/status")
    OperationStatusResponse getOperationStatus(
            @PathParam("sessionHandle") String sessionHandle,
            @PathParam("operationHandle") String operationHandle);

    @POST
    @Path("/sessions/{sessionHandle}/operations/{operationHandle}/cancel")
    OperationStatusResponse cancelOperation(
            @PathParam("sessionHandle") String sessionHandle,
            @PathParam("operationHandle") String operationHandle);

    @POST
    @Path("/sessions/{sessionHandle}/operations/{operationHandle}/close")
    void closeOperation(
            @PathParam("sessionHandle") String sessionHandle,
            @PathParam("operationHandle") String operationHandle);

    // --- Results ---

    @GET
    @Path("/sessions/{sessionHandle}/operations/{operationHandle}/result/{token}")
    FetchResultsResponse fetchResults(
            @PathParam("sessionHandle") String sessionHandle,
            @PathParam("operationHandle") String operationHandle,
            @PathParam("token") long token,
            @QueryParam("rowFormat") String rowFormat);
}
