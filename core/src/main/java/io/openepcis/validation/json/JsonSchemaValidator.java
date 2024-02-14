/*
 * Copyright 2022-2023 benelog GmbH & Co. KG
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
import com.networknt.schema.*;
import io.openepcis.validation.Validator;
import io.openepcis.validation.exception.SchemaValidationException;
import io.openepcis.validation.model.ValidationError;
import io.smallrye.mutiny.Multi;
import lombok.extern.slf4j.Slf4j;

import java.io.InputStream;
import java.util.Set;

@Slf4j
public class JsonSchemaValidator implements Validator {

  private final ObjectMapper mapper = new ObjectMapper();
  private final JsonSchemaFactory validatorFactory =
      JsonSchemaFactory.builder(JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7))
          .jsonMapper(mapper)
          .build();
  private final JsonSchema captureJsonSchema;
  private final JsonSchema queryJsonSchema;
  private final JsonErrorHandler jsonErrorHandler;

  public JsonSchemaValidator() {
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
    return Multi.createFrom().emitter(multiEmitter -> {
      try {
        final Set<ValidationMessage> captureErrors =
                captureJsonSchema.validate(mapper.readValue(epcisInputData, JsonNode.class));

        // If there are any validation errors then throw the errors
        if (!captureErrors.isEmpty()) {
          jsonErrorHandler.jsonValidationFormat(captureErrors);
        }

        this.jsonErrorHandler.getExceptions().stream().forEach(multiEmitter::emit);
        multiEmitter.complete();
      } catch (Exception exception) {
        multiEmitter.fail(
        new SchemaValidationException(
                "Exception occurred during validation against EPCIS JSON Capture schema : "
                        + exception.getMessage()));
      }
    });
  }

  @Override
  public Multi<ValidationError> validateAgainstQuerySchema(final InputStream document)
      throws SchemaValidationException {
    return Multi.createFrom().emitter(multiEmitter -> {
      try {
        final Set<ValidationMessage> queryErrors =
                queryJsonSchema.validate(mapper.readValue(document, JsonNode.class));

        // If there are any validation errors then throw the errors
        if (!queryErrors.isEmpty()) {
          jsonErrorHandler.jsonValidationFormat(queryErrors);
        }

        this.jsonErrorHandler.getExceptions().stream().forEach(multiEmitter::emit);
        multiEmitter.complete();
      } catch (Exception exception) {
        multiEmitter.fail(new SchemaValidationException(
                "Exception occurred during validation against EPCIS JSON Query schema : "
                        + exception.getMessage()));
      }

    });
  }
}
