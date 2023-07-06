package io.openepcis.core.formatter;

import lombok.Data;

@Data
public class ValidationError {
  private String type;
  private String line;
  private String column;
  private String message;
}
