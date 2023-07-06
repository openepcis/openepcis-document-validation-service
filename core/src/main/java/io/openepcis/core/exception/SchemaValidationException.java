package io.openepcis.core.exception;

public class SchemaValidationException extends RuntimeException {

  public SchemaValidationException(String msg) {
    super(msg);
  }

  public SchemaValidationException(String msg, Throwable cause) {
    super(msg, cause);
  }
}
