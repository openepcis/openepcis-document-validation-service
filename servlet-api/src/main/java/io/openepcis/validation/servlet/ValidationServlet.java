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
package io.openepcis.validation.servlet;

import io.openepcis.constants.EPCISDocumentType;
import io.openepcis.constants.EPCISVersion;
import io.openepcis.model.rest.servlet.ServletSupport;
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
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;

import java.io.IOException;
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
    ServletSupport servletSupport;

    @Inject
    Marshaller marshaller;

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        final Optional<String> accept = servletSupport.accept(PRODUCES, req, resp);
        if (accept.isEmpty()) {
            return;
        }
        final Optional<String> contentType =servletSupport.contentType(CONSUMES, accept.get(), req, resp);
        if (contentType.isEmpty()) {
            return;
        }
        final String epcisVersion = Optional.ofNullable(req.getHeader("GS1-EPCIS-Version")).orElse("2.0.0");

        final String epcisDocumentSchema = req.getParameter("epcisDocumentSchema");
        try {
            final List<ValidationError> validationErrors = schemaValidator.validate(req.getInputStream(), contentType.get(), EPCISDocumentType.fromString(epcisDocumentSchema), EPCISVersion.fromString(epcisVersion).get())
                    .collect().asList().await().atMost(Duration.of(10, ChronoUnit.SECONDS));
            if (validationErrors.isEmpty()) {
                resp.setHeader(HttpHeaders.CONTENT_TYPE, accept.get());
                resp.setStatus(HttpServletResponse.SC_OK);
                return;
            }
            resp.setHeader(HttpHeaders.CONTENT_TYPE, accept.get());
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            if (MediaType.APPLICATION_XML.equals(accept.get())) {
                final ValidationResult validationResult = new ValidationResult(validationErrors);
                try {
                    marshaller.marshal(validationResult, resp.getOutputStream());
                } catch (JAXBException e) {
                    throw new RuntimeException(e);
                }
            } else {
                servletSupport.writeJson(resp, validationErrors);
            }
        } catch (Exception e) {
            servletSupport.writeException(new InternalServerErrorException(), accept.get(), resp);
        }
    }

}
