package io.openepcis.core.xml;

import io.openepcis.core.Validator;
import io.openepcis.core.exception.SchemaValidationException;
import io.openepcis.core.formatter.ValidationError;
import io.openepcis.core.formatter.XmlErrorHandler;
import org.xml.sax.SAXException;

import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public abstract class AbstractXmlSchemaValidator implements Validator {

    protected Schema captureXmlSchema;
    protected Schema queryXmlSchema;
    protected EventModifier eventModifier = new EventModifier();
    protected XmlErrorHandler xsdErrorHandler;
    protected SchemaFactory schemaFactory;

    @Override
    public List<ValidationError> validateAgainstCaptureSchema(final InputStream captureInput) {
        try {
            final InputStream inputEvent = eventModifier.modifyEvent(captureInput);
            final javax.xml.validation.Validator validator = captureXmlSchema.newValidator();
            validator.setErrorHandler(this.xsdErrorHandler);
            validator.validate(new StreamSource(inputEvent));
        } catch (Exception e) {
            throw new SchemaValidationException(
                    "Exception occurred during the validation of Capture XML document/event against XSD : "
                            + e.getMessage()
                            + e);
        }

        return this.xsdErrorHandler.getExceptions();
    }

    @Override
    public List<ValidationError> validateAgainstQuerySchema(InputStream queryInput)
            throws SchemaValidationException {

        final javax.xml.validation.Validator validator = queryXmlSchema.newValidator();
        validator.setErrorHandler(this.xsdErrorHandler);

        try {
            validator.validate(new StreamSource(queryInput));
        } catch (SAXException | IOException e) {
            throw new SchemaValidationException(
                    "Exception occurred during the validation of Query XML document/event against XSD : "
                            + e.getMessage()
                            + e);
        }

        return this.xsdErrorHandler.getExceptions();

    }

    protected Schema loadSchema(final String name) {
        Schema schema;
        try {
            schema = schemaFactory.newSchema(new StreamSource(getClass().getResourceAsStream(name)));
        } catch (Exception e) {
            throw new SchemaValidationException(
                    "Exception occurred during the loading of the XSD document to schema : "
                            + e.getMessage()
                            + e);
        }
        return schema;
    }

}
