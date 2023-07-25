package io.openepcis.validation;

public enum SchemaType {
  CAPTURE_SCHEMA("captureSchema"),
  QUERY_SCHEMA("querySchema");

  private final String schema;

  public String getSchema() {
    return this.schema;
  }

  public static SchemaType fromString(String s) {
    for (SchemaType t : SchemaType.values()) {
      if (t.getSchema().equals(s)) {
        return t;
      }
    }
    return CAPTURE_SCHEMA;
  }

  SchemaType(final String schema) {
    this.schema = schema;
  }
}
