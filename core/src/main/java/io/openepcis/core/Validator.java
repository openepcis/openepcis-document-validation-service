package io.openepcis.core;

import io.openepcis.core.exception.SchemaValidationException;
import io.openepcis.core.formatter.ValidationError;

import java.io.InputStream;
import java.util.List;

public interface Validator {

  List<ValidationError> validateAgainstCaptureSchema(final InputStream epcisInputData)
      throws SchemaValidationException;

  List<ValidationError> validateAgainstQuerySchema(final InputStream epcisInputData)
      throws SchemaValidationException;



  default InputStream getResourceAsStream(final String name) {
    InputStream inputStream = getClass().getResourceAsStream(name);
    if (inputStream == null) {
      return Thread.currentThread().getContextClassLoader().getResourceAsStream(name);
    }
    return inputStream;
  }
}
