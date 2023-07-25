package io.openepcis.validation.rest.resource;

import io.openepcis.model.rest.ProblemResponseBody;
import io.openepcis.validation.SchemaType;
import io.openepcis.validation.SchemaValidator;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.context.ManagedExecutor;
import org.eclipse.microprofile.openapi.annotations.enums.ParameterIn;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.resteasy.reactive.RestHeader;
import org.jboss.resteasy.reactive.RestResponse;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;

import java.io.InputStream;

@Path("/api")
@Tag(
        name = "Event Validation",
        description = "Validate event for EPCIS XML or JSON/JSON-LD document or event list.")
@Slf4j
public class ValidationResource {

    @Inject
    SchemaValidator schemaValidator;

    @Inject
    ManagedExecutor managedExecutor;

    @APIResponses(
            value = {
                    @APIResponse(
                            responseCode = "200",
                            description = "Successfully validated one or more EPCIS events"),
                    @APIResponse(
                            responseCode = "400",
                            description = "An error occurred while validating EPCIS events",
                            content =
                            @Content(
                                    schema = @Schema(implementation = ProblemResponseBody.class),
                                    example = ResponseBodyExamples.RESPONSE_400_QUERY_ISSUES)),
                    @APIResponse(
                            responseCode = "401",
                            description = "Authorization information is missing or invalid.",
                            content =
                            @Content(
                                    schema = @Schema(implementation = ProblemResponseBody.class),
                                    example = ResponseBodyExamples.RESPONSE_401_UNAUTHORIZED_REQUEST)),
                    @APIResponse(
                            responseCode = "403",
                            description = "Client is unauthorized to access this resource.",
                            content =
                            @Content(
                                    schema = @Schema(implementation = ProblemResponseBody.class),
                                    example = ResponseBodyExamples.RESPONSE_403_CLIENT_UNAUTHORIZED)),
                    @APIResponse(
                            responseCode = "406",
                            description = "The server cannot return the response as requested.",
                            content =
                            @Content(
                                    schema = @Schema(implementation = ProblemResponseBody.class),
                                    example = ResponseBodyExamples.RESPONSE_406_NOT_ACCEPTABLE)),
                    @APIResponse(
                            responseCode = "500",
                            description = "An error occurred on the backend.",
                            content =
                            @Content(
                                    schema = @Schema(implementation = ProblemResponseBody.class),
                                    example = ResponseBodyExamples.RESPONSE_500_IMPLEMENTATION_EXCEPTION)),
                    @APIResponse(
                            responseCode = "415",
                            description =
                                    "The client sent data in a format that is not supported " + "by the server.",
                            content =
                            @Content(
                                    schema = @Schema(implementation = ProblemResponseBody.class),
                                    example = ResponseBodyExamples.RESPONSE_415_UNSUPPORTED_MEDIA_TYPE)),
                    @APIResponse(
                            responseCode = "413",
                            description =
                                    "The `POST` request is too large. It exceeds the limits "
                                            + "set in `GS1-EPCIS-Capture-Limit` and/or `GS1-EPCIS-Capture-File-Size-Limit`.",
                            content =
                            @Content(
                                    schema = @Schema(implementation = ProblemResponseBody.class),
                                    example = ResponseBodyExamples.RESPONSE_413_CAPTURE_PAYLOAD_TOO_LARGE))
            })
    @POST
    @Path("events/validate")
    @Produces({
            MediaType.APPLICATION_JSON,
            MediaType.APPLICATION_XML,
            "application/problem+json",
            "application/ld+json"
    })
    @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, "application/ld+json"})
    public Uni<RestResponse<?>> validateEvents(
            @Parameter(
                    name = "GS1-EPCIS-Version",
                    description = "The EPCIS version",
                    in = ParameterIn.HEADER)
            @RestHeader(value = "GS1-EPCIS-Version")
            @DefaultValue("2.0")
            String epcisVersion,
            @QueryParam("schemaType")
            String schemaType,
            @HeaderParam("Content-Type") final String contentType,
            @Valid InputStream body) {

        return schemaValidator.validate(body, contentType, SchemaType.fromString(schemaType), epcisVersion)
                .runSubscriptionOn(managedExecutor)
                .collect().asList()
                .chain(validationErrors -> {
                    if (validationErrors.isEmpty()) {
                        return Uni.createFrom().item(RestResponse.ok());
                    }
                    return Uni.createFrom().item(RestResponse.status(Response.Status.BAD_REQUEST, validationErrors));
                }).onFailure().recoverWithItem(this::createErrorResponse);
    }

    private RestResponse<ProblemResponseBody> createErrorResponse(Throwable throwable) {
        final ProblemResponseBody responseBody = new ProblemResponseBody();
        responseBody.setType(throwable.getClass().getSimpleName());
        responseBody.title(Response.Status.UNSUPPORTED_MEDIA_TYPE.getReasonPhrase());
        responseBody.setStatus(Response.Status.UNSUPPORTED_MEDIA_TYPE.getStatusCode());
        responseBody.setDetail(throwable.getMessage());
        return RestResponse.status(Response.Status.UNSUPPORTED_MEDIA_TYPE, responseBody);
    }


}
