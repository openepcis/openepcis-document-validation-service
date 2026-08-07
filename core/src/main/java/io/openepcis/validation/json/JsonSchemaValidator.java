/*
 * Copyright 2022-2026 benelog GmbH & Co. KG
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
package io.openepcis.validation.json;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.ValidationMessage;
import io.openepcis.constants.EPCIS;
import io.openepcis.reactive.publisher.ObjectNodePublisher;
import io.openepcis.validation.Validator;
import io.openepcis.validation.config.ValidationDefaults;
import io.openepcis.validation.exception.SchemaValidationException;
import io.openepcis.validation.message.JsonErrorFormatter;
import io.openepcis.validation.message.MessageTemplates;
import io.openepcis.validation.model.ValidationError;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.smallrye.mutiny.Multi;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@RegisterForReflection(targets = JsonSchemaFactory.class)
public class JsonSchemaValidator implements Validator {

    // Errors below here belong to the body, which is validated event by event instead.
    private static final String BODY_PREFIX = "$." + EPCIS.EPCIS_BODY_IN_CAMEL_CASE;
    private static final String EVENT_SCHEMA_REF = "{\"$ref\":\"resource:/json-schema/epcis-json-schema.json#/definitions/EPCIS-Document-Event\"}";
    private static final int PRESCAN_LIMIT = 65536;
    private static final JsonErrorFormatter FORMATTER = new JsonErrorFormatter();

    private final ObjectMapper mapper;
    private final JsonFactory prescanFactory;
    private final JsonSchema documentSchema;       // switches on type: EPCISDocument or EPCISQueryDocument
    private final JsonSchema queryParameterSchema; // GE_/LT_/EQ_ style query, not a document
    private final JsonSchema eventSchema;
    private final int maxReportedErrors;

    // Kept so existing callers and tests compile without passing configuration.
    public JsonSchemaValidator(final ObjectMapper objectMapper, final JsonSchemaFactory validatorFactory) {
        this(objectMapper, validatorFactory, ValidationDefaults.MAX_REPORTED_ERRORS);
    }

    public JsonSchemaValidator(final ObjectMapper objectMapper,
                               final JsonSchemaFactory validatorFactory,
                               final int maxReportedErrors) {
        this.mapper = objectMapper;
        this.maxReportedErrors = maxReportedErrors > 0 ? maxReportedErrors : ValidationDefaults.MAX_REPORTED_ERRORS;

        // the pre-scan must not close the body, the publisher still has to read it
        this.prescanFactory = new JsonFactory().disable(JsonParser.Feature.AUTO_CLOSE_SOURCE);

        // every schema is a bundled classpath resource, so validation never reaches the network
        this.documentSchema = loadSchema(validatorFactory, "/json-schema/modular-EPCIS-JSON-schema.json");
        this.queryParameterSchema = loadSchema(validatorFactory, "/json-schema/modified-query-schema.json");
        this.eventSchema = validatorFactory.getSchema(EVENT_SCHEMA_REF);
    }

    private JsonSchema loadSchema(final JsonSchemaFactory validationFactory, final String path) {
        return validationFactory.getSchema(getResourceAsStream(path));
    }

    @Override
    public Multi<ValidationError> validateAgainstCaptureSchema(final InputStream epcisInputData) throws SchemaValidationException {
        return validate(epcisInputData, false, "Capture");
    }

    @Override
    public Multi<ValidationError> validateAgainstQuerySchema(final InputStream epcisInputData) throws SchemaValidationException {
        return validate(epcisInputData, true, "Query");
    }

    private Multi<ValidationError> validate(final InputStream epcisInputData,
                                            final boolean queryRequested,
                                            final String label) {
        // lazy read so nothing read until subscription
        return Multi.createFrom().deferred(() -> {
            try {
                final BufferedInputStream body = new BufferedInputStream(epcisInputData);
                final String type = prescanType(body);

                // there is no type EPCISDocument/EPCISQueryDocument: a literal query when one was asked for, else a document missing its type
                if (type == null) {
                    return single(body, queryRequested ? queryParameterSchema : documentSchema);
                }

                // if type is EPCISDocument/EPCISQueryDocument then use ObjectNodePublisher to stream and validate event-by-event from eventsList
                if (EPCIS.EPCIS_DOCUMENT.equals(type) || EPCIS.EPCIS_QUERY_DOCUMENT.equals(type)) {
                    return streamDocument(body, type);
                }

                // If none match then treat is as bareEvent
                return single(body, documentSchema);
            } catch (Exception exception) {
                return Multi.createFrom().failure(new SchemaValidationException("Exception occurred during validation against EPCIS JSON " + label + " schema : " + exception.getMessage(), exception));
            }
        });
    }

    // Reads the initial token to find the top level type then revert so the further stream reads from start
    private String prescanType(final BufferedInputStream body) throws IOException {
        body.mark(PRESCAN_LIMIT);
        try (JsonParser parser = prescanFactory.createParser(body)) {
            if (parser.nextToken() != JsonToken.START_OBJECT) {
                return null;
            }
            int depth = 1;

            while (depth > 0) {
                final JsonToken token = parser.nextToken();
                if (token == null) {
                    return null;
                }

                if (token == JsonToken.FIELD_NAME) {
                    if (depth == 1 && EPCIS.TYPE.equals(parser.currentName())) {
                        parser.nextToken();
                        // only the top level type counts
                        return parser.getValueAsString();
                    }
                } else if (token == JsonToken.START_OBJECT || token == JsonToken.START_ARRAY) {
                    depth++;
                } else if (token == JsonToken.END_OBJECT || token == JsonToken.END_ARRAY) {
                    depth--;
                }
            }
            return null;
        } catch (Exception exception) {
            return null;
        } finally {
            // hand the untouched stream on
            body.reset();
        }
    }

    //  A single object: a literal query, or one bare event. Never an event list.
    private Multi<ValidationError> single(final InputStream body, final JsonSchema schema) throws IOException {
        final JsonNode node = mapper.readTree(body);
        final List<ValidationError> errors = cap(FORMATTER.format(node, schema.validate(node)));
        return Multi.createFrom().iterable(errors);
    }

    // Stream EPCISDocument/EPCISQueryDocument to validate event-by-event from eventsList
    private Multi<ValidationError> streamDocument(final InputStream body, final String documentType) throws IOException {
        // Path to read get the eventsList
        final String eventListPath = EPCIS.EPCIS_QUERY_DOCUMENT.equals(documentType) ? EPCIS.QUERY_EVENT_LIST_PATH : EPCIS.CAPTURE_EVENT_LIST_PATH;
        final AtomicInteger nextEvent = new AtomicInteger();
        final AtomicLong nodes = new AtomicLong();
        final AtomicLong kept = new AtomicLong();
        final int reportable = Math.max(1, maxReportedErrors - 1);

        return Multi.createFrom()
                .publisher(ObjectNodePublisher.<ObjectNode>fromInputStream(body))
                .onItem()
                .transformToMultiAndConcatenate(node -> {
                    nodes.incrementAndGet();
                    final String nodeType = node.path(EPCIS.TYPE).asText("");
                    //Validate header or event depending on type separately
                    return Multi.createFrom().iterable(
                            EPCIS.EPCIS_DOCUMENT.equals(nodeType) || EPCIS.EPCIS_QUERY_DOCUMENT.equals(nodeType)
                                    ? validateHeader(node)
                                    : validateEvent(node, eventListPath, nextEvent.getAndIncrement()));
                })
                // nothing parsed at all must never report like a clean document
                .onCompletion().continueWith(() -> nodes.get() == 0 ? List.of(unreadable()) : List.<ValidationError>of())
                // one more than we show, which is what proves more exist without counting
                .select().first(maxReportedErrors)
                // that extra one is dropped, it only served as proof that there were more than the limit, so report a single limitExceeded error instead
                .select().where(error -> kept.incrementAndGet() <= reportable)
                .onCompletion().continueWith(() -> kept.get() > reportable ? List.of(limitExceeded(reportable)) : List.<ValidationError>of());
    }

    // Validates the header/envelope against GS1 schema by adding empty eventsList so it validates
    private List<ValidationError> validateHeader(final ObjectNode header) {
        header.putObject(EPCIS.EPCIS_BODY_IN_CAMEL_CASE);
        final List<ValidationError> envelope = new ArrayList<>();
        for (final ValidationError error : FORMATTER.format(null, documentSchema.validate(header))) {
            final String at = error.getLocation();
            if (at == null || !at.startsWith(BODY_PREFIX)) {
                envelope.add(error);
            }
        }
        return envelope;
    }


    private List<ValidationError> validateEvent(final ObjectNode event,
                                                final String eventListPath,
                                                final int index) {
        final Set<ValidationMessage> messages = eventSchema.validate(event);
        // formatEvent turns $.eventTime back into $.epcisBody.eventList[3].eventTime
        final String eventType = event.path(EPCIS.TYPE).asText(null);
        return messages.isEmpty() ? List.of() : FORMATTER.formatEvent(eventListPath, index, eventType, messages);
    }

    // Applies the limit to a single object result, where there is no stream to cut short.
    private List<ValidationError> cap(final List<ValidationError> all) {
        final int reportable = Math.max(1, maxReportedErrors - 1);
        if (all.size() <= reportable) {
            return all;
        }
        final List<ValidationError> capped = new ArrayList<>(all.subList(0, reportable));
        capped.add(limitExceeded(reportable));
        return capped;
    }

    // The parser produced nothing so was unable to read the documents report it instead of reporting a clean document.
    private ValidationError unreadable() {
        final ValidationError error = new ValidationError();
        error.setType("required");
        error.setLocation("$");
        error.setField("@context");
        error.setMessage(MessageTemplates.hasKeyword("document.unreadable")
                ? MessageTemplates.keyword("document.unreadable")
                : "The document could not be read. Check that @context is present and appears before epcisBody.");
        return error;
    }

    // The last entry in errors when limit is exceeded, so a capped list is never mistaken for a complete one.
    private ValidationError limitExceeded(final int limit) {
        final ValidationError error = new ValidationError();
        error.setType("limitExceeded");
        error.setMessage(MessageTemplates.hasKeyword("limit.exceeded")
                ? MessageTemplates.render(MessageTemplates.keyword("limit.exceeded"), Map.of("limit", String.valueOf(limit)))
                : "Validation stopped after " + limit + " problems. This document has more.");
        return error;

    }
}
