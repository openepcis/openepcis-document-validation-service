package io.openepcis.validation.restassured;

import io.openepcis.model.rest.ProblemResponseBody;
import jakarta.ws.rs.WebApplicationException;
import org.jboss.resteasy.reactive.RestResponse;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;

public class ExceptionMapper {

    // TODO: move common ExceptionMappers to new openepcis-xml-rest-exception

    @ServerExceptionMapper
    public final RestResponse<ProblemResponseBody> mapException(
            final WebApplicationException exception) {
        return RestResponse.status(exception.getResponse().getStatusInfo(), ProblemResponseBody.fromException(exception));
    }

}
