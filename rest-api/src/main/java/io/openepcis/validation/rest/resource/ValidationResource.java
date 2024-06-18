/*
 * Copyright 2022-2024 benelog GmbH & Co. KG
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 */
package io.openepcis.validation.rest.resource;

import io.openepcis.constants.EPCIS;
import io.openepcis.constants.EPCISDocumentType;
import io.openepcis.constants.EPCISVersion;
import io.openepcis.model.epcis.EPCISDocument;
import io.openepcis.model.rest.ProblemResponseBody;
import io.openepcis.validation.SchemaValidator;
import io.openepcis.validation.model.ValidationError;
import io.openepcis.validation.model.xml.ValidationResult;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.context.ManagedExecutor;
import org.eclipse.microprofile.openapi.annotations.enums.ParameterIn;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.ExampleObject;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.resteasy.reactive.RestHeader;
import org.jboss.resteasy.reactive.RestResponse;

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

    @Inject
    HttpHeaders httpHeaders;

    @APIResponses(
            value = {
                    @APIResponse(
                            responseCode = "200",
                            description = "Successfully validated one or more EPCIS events",
                            content = @Content(example = "")),
                    @APIResponse(
                            responseCode = "400",
                            description = "An error occurred while validating EPCIS events",
                            content = {
                            @Content(
                                    mediaType = MediaType.APPLICATION_JSON,
                                    schema = @Schema(type = SchemaType.ARRAY, implementation = ValidationError.class),
                                    example = ResponseBodyExamples.RESPONSE_400_VALIDATION_ERRORS),
                            @Content(
                                    mediaType = MediaType.APPLICATION_XML,
                                    example = ResponseBodyExamples.RESPONSE_400_VALIDATION_ERRORS_XML,
                                    schema = @Schema(implementation = ValidationResult.class)),
                            @Content(
                                    mediaType = "application/problem+json",
                                    schema = @Schema(type = SchemaType.ARRAY, implementation = ValidationError.class),
                                    example = ResponseBodyExamples.RESPONSE_400_VALIDATION_ERRORS)
                            }),
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
                                    "The client sent data in a format that is not supported by the server",
                            content =
                            @Content(
                                    schema = @Schema(implementation = ProblemResponseBody.class),
                                    example = ResponseBodyExamples.RESPONSE_415_UNSUPPORTED_MEDIA_TYPE))
            })
    @POST
    @Path("events/validate")
    @Produces({
            MediaType.APPLICATION_JSON,
            MediaType.APPLICATION_XML,
            "application/problem+json"
    })
    @RequestBody(
            description =
                    "Validate EPCIS 2.0 JSON/JSON-LD/XML or EPCIS 1.2 XML document.",
            content = {
                    @Content(
                            mediaType = MediaType.APPLICATION_JSON,
                            schema = @Schema(implementation = EPCISDocument.class),
                            examples = {
                                    @ExampleObject(
                                            name = "EPCIS 2.0 JSON document",
                                            ref = "jsonDocument",
                                            description = "Example EPCIS 2.0 document in JSON format.")
                            }),
                    @Content(
                            mediaType = MediaType.APPLICATION_XML,
                            schema = @Schema(implementation = EPCISDocument.class),
                            examples = {
                                    @ExampleObject(
                                            name = "EPCIS 1.2 XML document",
                                            ref = "xml1.2Document",
                                            description = "Example EPCIS 1.2 document"),
                                    @ExampleObject(
                                            name = "EPCIS 2.0 XML document",
                                            ref = "xmlDocument",
                                            description = "Example EPCIS 2.0 document in XML format.")
                            }),
                    @Content(
                            mediaType = "application/ld+json",
                            schema = @Schema(implementation = EPCISDocument.class),
                            examples = {
                                    @ExampleObject(
                                            name = "EPCIS 2.0 JSON document",
                                            ref = "jsonDocument",
                                            description = "Example EPCIS 2.0 document in JSON format.")
                            })
            })
    @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, "application/ld+json"})
    public Uni<RestResponse<?>> validateEvents(
            @Parameter(
                    name = "GS1-EPCIS-Version",
                    description = "The EPCIS version",
                    examples = {
                            @ExampleObject(name = "EPCIS 2.0", value = "2.0.0"),
                            @ExampleObject(name = "EPCIS 1.2", value = "1.2.0")
                    },
                    in = ParameterIn.HEADER)
            @RestHeader(value = "GS1-EPCIS-Version")
            @DefaultValue("2.0.0")
            String epcisVersion,
            @QueryParam("epcisDocumentSchema")
            @DefaultValue(EPCIS.CAPTURE)
            @Schema(type=SchemaType.STRING, enumeration = { EPCIS.CAPTURE, EPCIS.QUERY })
            String epcisDocumentSchema,
            @Valid InputStream body) {

        final String contentType = httpHeaders.getHeaderString(HttpHeaders.CONTENT_TYPE);
        final String accept = httpHeaders.getHeaderString(HttpHeaders.ACCEPT);
        return schemaValidator.validate(body, contentType, EPCISDocumentType.fromString(epcisDocumentSchema), EPCISVersion.fromString(epcisVersion).get())
                .runSubscriptionOn(managedExecutor)
                .collect().asList()
                .chain(validationErrors -> {
                    if (validationErrors.isEmpty()) {
                        return Uni.createFrom().item(RestResponse.ok());
                    }
                    if (MediaType.APPLICATION_XML.equals(accept)) {
                        return Uni.createFrom().item(RestResponse.status(Response.Status.BAD_REQUEST, new ValidationResult(validationErrors)));
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
