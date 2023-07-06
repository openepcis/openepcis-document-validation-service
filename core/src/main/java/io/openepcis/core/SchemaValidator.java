package io.openepcis.core;

import io.openepcis.core.formatter.ValidationError;
import io.openepcis.core.json.JsonSchemaValidator;
import io.openepcis.core.xml.XmlSchemaValidator;

import java.io.InputStream;
import java.util.List;

public class SchemaValidator {
  private final JsonSchemaValidator jsonSchemaValidator;
  private final XmlSchemaValidator xmlSchemaValidator;

  public SchemaValidator() {
    this.jsonSchemaValidator = new JsonSchemaValidator();
    this.xmlSchemaValidator = new XmlSchemaValidator();
  }

  /**
   * Method that accepts the EPCIS XML or JSON document/event and checks if it adheres to respective
   * schema or not.
   *
   * @param epcisInputData input EPCIS document/event that needs to be validated against schema.
   * @param mediaType format type of the input document: application/xml, application/json,
   *     application/json+ld, etc.
   * @param schemaType type of schema against which the document needs to be validated against:
   *     Capture or Query schema.
   */
  public List<ValidationError> validate(
      final InputStream epcisInputData, final String mediaType, final SchemaType schemaType) {

    Validator validator = getValidator(mediaType);

    if(schemaType.equals(SchemaType.CAPTURE_SCHEMA)) {
      return validator.validateAgainstCaptureSchema(epcisInputData);
    }

    if(schemaType.equals(SchemaType.QUERY_SCHEMA)) {
      return validator.validateAgainstQuerySchema(epcisInputData);
    }

    throw new UnsupportedOperationException(
            "Unsupported schema type : " + schemaType);
  }

  private Validator getValidator(final String mediaType) {

    if (mediaType.toLowerCase().contains("json")) {
      return jsonSchemaValidator;
    }

    if (mediaType.toLowerCase().contains("xml")) {
      return xmlSchemaValidator;
    }

    throw new UnsupportedOperationException("Unsupported media type : " + mediaType);
  }
}
