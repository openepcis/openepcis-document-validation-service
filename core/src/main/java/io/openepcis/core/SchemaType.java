package io.openepcis.core;

public enum SchemaType {
  CAPTURE_SCHEMA("captureSchema"),
  QUERY_SCHEMA("querySchema");

  private final String schema;

  public String getSchema() {
    return this.schema;
  }

  SchemaType(final String schema) {
    this.schema = schema;
  }
}
