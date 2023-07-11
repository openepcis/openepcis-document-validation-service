package io.openepcis.core;

import io.openepcis.constants.EPCISVersion;
import io.openepcis.core.formatter.ValidationError;
import io.openepcis.core.json.JsonSchemaValidator;
import io.openepcis.core.xml.Xml12SchemaValidator;
import io.openepcis.core.xml.Xml20SchemaValidator;

import java.io.InputStream;
import java.util.List;

public class SchemaValidator {
    private final JsonSchemaValidator jsonSchemaValidator;
    private final Xml20SchemaValidator xml20SchemaValidator;
    private final Xml12SchemaValidator xml12SchemaValidator;
    public SchemaValidator() {
        this.jsonSchemaValidator = new JsonSchemaValidator();
        this.xml20SchemaValidator = new Xml20SchemaValidator();
        this.xml12SchemaValidator = new Xml12SchemaValidator();
    }

    /**
     * Method that accepts the EPCIS XML or JSON document/event and checks if it adheres to respective
     * schema or not.
     *
     * @param epcisInputData input EPCIS document/event that needs to be validated against schema.
     * @param mediaType      format type of the input document: application/xml, application/json,
     *                       application/json+ld, etc.
     * @param schemaType     type of schema against which the document needs to be validated against:
     *                       Capture or Query schema.
     */
    public List<ValidationError> validate(
            final InputStream epcisInputData,
            final String mediaType,
            final SchemaType schemaType,
            final String version) {

        try {
            Validator validator = getValidator(mediaType, version);

            if (schemaType.equals(SchemaType.CAPTURE_SCHEMA)) {
                return validator.validateAgainstCaptureSchema(epcisInputData);
            }

            if (schemaType.equals(SchemaType.QUERY_SCHEMA)) {
                return validator.validateAgainstQuerySchema(epcisInputData);
            }

            throw new UnsupportedOperationException(
                    "Unsupported schema type : " + schemaType);

        } catch (Exception e) {
            throw new RuntimeException("ERROR while processing a inputStream", e);
        }

    }
    private Validator getValidator(final String mediaType, String version) {

        if (mediaType.toLowerCase().contains("json")) {
            return jsonSchemaValidator;
        }

        if (mediaType.toLowerCase().contains("xml")) {

            if (EPCISVersion.VERSION_2_0_0.getSchemaVersion().equals(version)) {
                return xml20SchemaValidator;
            }

            if (EPCISVersion.VERSION_1_2_0.getSchemaVersion().equals(version)) {
                return xml12SchemaValidator;
            }
        }

        throw new UnsupportedOperationException("Unsupported media type : " + mediaType);
    }

}
