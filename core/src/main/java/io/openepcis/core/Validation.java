package io.openepcis.core;

import io.openepcis.core.exception.SchemaValidationException;
import io.openepcis.core.formatter.ValidationError;

import java.io.InputStream;
import java.util.List;

public interface Validation {

  List<ValidationError> validateAgainstCaptureSchema(final InputStream epcisInputData)
      throws SchemaValidationException;

  List<ValidationError> validateAgainstQuerySchema(final InputStream epcisInputData)
      throws SchemaValidationException;
}
