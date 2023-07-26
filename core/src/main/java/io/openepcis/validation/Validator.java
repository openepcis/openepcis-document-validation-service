package io.openepcis.validation;

import io.openepcis.validation.exception.SchemaValidationException;
import io.openepcis.validation.model.ValidationError;
import io.smallrye.mutiny.Multi;

import java.io.InputStream;

public interface Validator {

  Multi<ValidationError> validateAgainstCaptureSchema(final InputStream epcisInputData)
      throws SchemaValidationException;

  Multi<ValidationError> validateAgainstQuerySchema(final InputStream epcisInputData)
      throws SchemaValidationException;



  default InputStream getResourceAsStream(final String name) {
    InputStream inputStream = getClass().getResourceAsStream(name);
    if (inputStream == null) {
      return Thread.currentThread().getContextClassLoader().getResourceAsStream(name);
    }
    return inputStream;
  }
}
