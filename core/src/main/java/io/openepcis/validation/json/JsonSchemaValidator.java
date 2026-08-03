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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.ValidationMessage;
import io.openepcis.validation.Validator;
import io.openepcis.validation.exception.SchemaValidationException;
import io.openepcis.validation.model.ValidationError;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.smallrye.mutiny.Multi;
import lombok.extern.slf4j.Slf4j;

import java.io.InputStream;
import java.util.Set;

@Slf4j
@RegisterForReflection(targets = JsonSchemaFactory.class)
public class JsonSchemaValidator implements Validator {

    private final ObjectMapper mapper;
    private final JsonSchemaFactory validatorFactory;
    private final JsonSchema captureJsonSchema;
    private final JsonSchema queryJsonSchema;
    private final JsonErrorHandler jsonErrorHandler;

    public JsonSchemaValidator(final ObjectMapper objectMapper, final JsonSchemaFactory validatorFactory) {
        this.mapper = objectMapper;
        this.validatorFactory = validatorFactory;
        this.captureJsonSchema = loadSchema("/json-schema/modular-EPCIS-JSON-schema.json");
        this.queryJsonSchema = loadSchema("/json-schema/modified-query-schema.json");
        this.jsonErrorHandler = new JsonErrorHandler();
    }

    private JsonSchema loadSchema(final String path) {
        return validatorFactory.getSchema(getResourceAsStream(path));
    }

    @Override
    public Multi<ValidationError> validateAgainstCaptureSchema(final InputStream epcisInputData)
            throws SchemaValidationException {
        return Multi.createFrom()
                .emitter(
                        multiEmitter -> {
                            try {
                                final JsonNode document = mapper.readValue(epcisInputData, JsonNode.class);
                                final Set<ValidationMessage> captureErrors = captureJsonSchema.validate(document);

                                // schema violations stay emitted items, a failure is reserved for broken input
                                if (!captureErrors.isEmpty()) {
                                    jsonErrorHandler.jsonValidationFormat(document, captureErrors);
                                }

                                this.jsonErrorHandler.getExceptions().stream().forEach(multiEmitter::emit);
                                multiEmitter.complete();
                            } catch (Exception exception) {
                                multiEmitter.fail(
                                        new SchemaValidationException("Exception occurred during validation against EPCIS JSON Capture schema : " + exception.getMessage()));
                            }
                        });
    }

    @Override
    public Multi<ValidationError> validateAgainstQuerySchema(final InputStream document)
            throws SchemaValidationException {
        return Multi.createFrom()
                .emitter(
                        multiEmitter -> {
                            try {
                                // keep the tree instead of discarding it, the formatter reads event types from it
                                final JsonNode queryDocument = mapper.readValue(document, JsonNode.class);
                                final Set<ValidationMessage> queryErrors = queryJsonSchema.validate(queryDocument);

                                // schema violations stay emitted items, a failure is reserved for broken input
                                if (!queryErrors.isEmpty()) {
                                    jsonErrorHandler.jsonValidationFormat(queryDocument, queryErrors);
                                }

                                this.jsonErrorHandler.getExceptions().stream().forEach(multiEmitter::emit);
                                multiEmitter.complete();
                            } catch (Exception exception) {
                                multiEmitter.fail(
                                        new SchemaValidationException("Exception occurred during validation against EPCIS JSON Query schema : " + exception.getMessage()));
                            }
                        });
    }
}
