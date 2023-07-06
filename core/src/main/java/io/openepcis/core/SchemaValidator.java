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

    // If input is JSON/JSON-LD and is Capture document then validate against Capture schema.
    if (mediaType.toLowerCase().contains("json") && schemaType.equals(SchemaType.CAPTURE_SCHEMA)) {
      return jsonSchemaValidator.validateAgainstCaptureSchema(epcisInputData);
    } else if (mediaType.toLowerCase().contains("json")
        && schemaType.equals(SchemaType.QUERY_SCHEMA)) {
      // If input is JSON/JSON-LD and is Query document then validate against Query schema.
      return jsonSchemaValidator.validateAgainstQuerySchema(epcisInputData);
    } else if (mediaType.toLowerCase().contains("xml")
        && schemaType.equals(SchemaType.CAPTURE_SCHEMA)) {
      // If input is XML and is Capture document then validate against Capture schema.
      return xmlSchemaValidator.validateAgainstCaptureSchema(epcisInputData);
    } else if (mediaType.toLowerCase().contains("xml")
        && schemaType.equals(SchemaType.QUERY_SCHEMA)) {
      // If input is XML and is Query document then validate against Query schema.
      return xmlSchemaValidator.validateAgainstQuerySchema(epcisInputData);
    } else {
      // If none of the request matches then throw the non-support exception
      throw new UnsupportedOperationException(
          "Requested validation is not supported currently, Please check provided MediaType/schemaType and try again!!!");
    }
  }
}
