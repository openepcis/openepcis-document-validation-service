package io.openepcis.core.formatter;

import com.networknt.schema.ValidationMessage;

import java.util.Collection;

/**
 * Class to format the validation errors occur during the validation of JSON Schema Capture/Query
 * document/event.
 */
public class JsonErrorHandler {

  @lombok.Getter private final java.util.List<ValidationError> exceptions;

  public JsonErrorHandler() {
    this.exceptions = new java.util.ArrayList<>();
  }

  public void jsonValidationFormat(final Collection<ValidationMessage> messages) {
    for (final ValidationMessage errorMessage : messages) {
      final ValidationError errorFormat = new ValidationError();
      errorFormat.setType(errorMessage.getType());
      errorFormat.setLine(errorMessage.getSchemaPath());
      errorFormat.setColumn(errorMessage.getPath());
      errorFormat.setMessage(errorMessage.getMessage());
      exceptions.add(errorFormat);
    }
  }
}
