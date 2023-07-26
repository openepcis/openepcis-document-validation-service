package io.openepcis.validation.rest.resource.test;

import io.openepcis.model.rest.ProblemResponseBody;
import jakarta.ws.rs.WebApplicationException;
import org.jboss.resteasy.reactive.RestResponse;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;

public class ExceptionMapper {

    // TODO: move common ExceptionMappers to new openepcis-xml-rest-exception

    @ServerExceptionMapper
    public final RestResponse<ProblemResponseBody> mapException(
            final WebApplicationException exception) {
        final ProblemResponseBody responseBody = new ProblemResponseBody();
        responseBody.setType(exception.getClass().getSimpleName());
        responseBody.setTitle(exception.getResponse().getStatusInfo().getReasonPhrase());
        responseBody.setStatus(exception.getResponse().getStatus());
        responseBody.setDetail(exception.getMessage());
        return RestResponse.status(exception.getResponse().getStatusInfo(), responseBody);
    }

}
