package io.openepcis.validation.servlet;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.openepcis.constants.EPCISDocumentType;
import io.openepcis.constants.EPCISVersion;
import io.openepcis.model.rest.ProblemResponseBody;
import io.openepcis.validation.SchemaValidator;
import io.openepcis.validation.model.ValidationError;
import io.openepcis.validation.model.xml.ValidationResult;
import jakarta.inject.Inject;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.NotSupportedException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.microprofile.context.ManagedExecutor;

import java.io.IOException;
import java.io.StringWriter;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@WebServlet(name = "ValidationServlet", urlPatterns = "/api/events/validate")
public class ValidationServlet extends HttpServlet {
    private static final List<String> PRODUCES = Arrays.asList(
            MediaType.WILDCARD,
            MediaType.APPLICATION_JSON,
            MediaType.APPLICATION_XML,
            "application/problem+json");

    private static final List<String> CONSUMES = Arrays.asList(
            MediaType.APPLICATION_JSON,
            MediaType.APPLICATION_XML,
            "application/ld+json");

    @Inject
    SchemaValidator schemaValidator;

    @Inject
    ManagedExecutor managedExecutor;

    @Inject
    ObjectMapper objectMapper;

    @Inject
    Marshaller marshaller;

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        final String accept = req.getHeader(HttpHeaders.ACCEPT);
        if (!StringUtils.isEmpty(accept) && !PRODUCES.stream().anyMatch(s -> accept.contains(s))) {
            writeException(new NotSupportedException("The accept header value did not match the value in @Produces"), accept, resp);
            return;
        }

        final String contentType = req.getContentType();
        if (!StringUtils.isEmpty(contentType) && !CONSUMES.stream().anyMatch(s ->contentType.contains(s))) {
            writeException(new NotSupportedException("The content-type header value did not match the value in @Consumes"), accept, resp);
            return;
        }
        final String epcisVersion = Optional.ofNullable(req.getHeader("GS1-EPCIS-Version")).orElse("2.0.0");

        final String epcisDocumentSchema = req.getParameter("epcisDocumentSchema");
        try {
            final List<ValidationError> validationErrors = schemaValidator.validate(req.getInputStream(), contentType, EPCISDocumentType.fromString(epcisDocumentSchema), EPCISVersion.fromString(epcisVersion).get())
                    .collect().asList().await().atMost(Duration.of(10, ChronoUnit.SECONDS));
            if (validationErrors.isEmpty()) {
                resp.setHeader(HttpHeaders.CONTENT_TYPE, accept);
                resp.setStatus(HttpServletResponse.SC_OK);
                return;
            }
            resp.setHeader(HttpHeaders.CONTENT_TYPE, accept);
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            if (MediaType.APPLICATION_XML.equals(accept)) {
                final ValidationResult validationResult = new ValidationResult(validationErrors);
                try {
                    marshaller.marshal(validationResult, resp.getOutputStream());
                } catch (JAXBException e) {
                    throw new RuntimeException(e);
                }
            } else {
                objectMapper.writeValue(resp.getOutputStream(), validationErrors);
            }
        } catch (Exception e) {
            writeException(new InternalServerErrorException(), accept, resp);
        }
    }

    private void writeException(WebApplicationException exception, String mediaType, HttpServletResponse resp) throws IOException {
        resp.setStatus(exception.getResponse().getStatus());
        if (!StringUtils.isEmpty(mediaType) && mediaType.contains("json")) {
            resp.setHeader(HttpHeaders.CONTENT_TYPE, "application/problem+json");
            objectMapper.writeValue(resp.getOutputStream(), ProblemResponseBody.fromException(exception));
        } else {
            final StringWriter stringWriter = new StringWriter();
            try {
                resp.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML);
                marshaller.marshal(ProblemResponseBody.fromException(exception), resp.getOutputStream());
            } catch (JAXBException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
