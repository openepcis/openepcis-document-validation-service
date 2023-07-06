package io.openepcis.core.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import io.openepcis.core.Validation;
import io.openepcis.core.exception.SchemaValidationException;
import io.openepcis.core.formatter.JsonErrorHandler;
import io.openepcis.core.formatter.ValidationError;
import lombok.extern.slf4j.Slf4j;

import java.io.InputStream;
import java.util.List;
import java.util.Set;

@Slf4j
public class JsonSchemaValidator implements Validation {

  private final ObjectMapper mapper = new ObjectMapper();
  private final JsonSchemaFactory validatorFactory =
      JsonSchemaFactory.builder(JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7))
          .objectMapper(mapper)
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
    final InputStream inputStream = JsonSchemaValidator.class.getResourceAsStream(path);
    return validatorFactory.getSchema(inputStream);
  }

  @Override
  public List<ValidationError> validateAgainstCaptureSchema(final InputStream epcisInputData)
      throws SchemaValidationException {
    try {
      final Set<ValidationMessage> captureErrors =
          captureJsonSchema.validate(mapper.readValue(epcisInputData, JsonNode.class));

      // If there are any validation errors then throw the errors
      if (!captureErrors.isEmpty()) {
        jsonErrorHandler.jsonValidationFormat(captureErrors);
      }

      return this.jsonErrorHandler.getExceptions();
    } catch (Exception exception) {
      throw new SchemaValidationException(
          "Exception occurred during validation against EPCIS JSON Capture schema : "
              + exception.getMessage());
    }
  }

  @Override
  public List<ValidationError> validateAgainstQuerySchema(final InputStream document)
      throws SchemaValidationException {
    try {
      final Set<ValidationMessage> queryErrors =
          queryJsonSchema.validate(mapper.readValue(document, JsonNode.class));

      // If there are any validation errors then throw the errors
      if (!queryErrors.isEmpty()) {
        jsonErrorHandler.jsonValidationFormat(queryErrors);
      }

      return this.jsonErrorHandler.getExceptions();
    } catch (Exception exception) {
      throw new SchemaValidationException(
          "Exception occurred during validation against EPCIS JSON Query schema : "
              + exception.getMessage());
    }
  }
}
