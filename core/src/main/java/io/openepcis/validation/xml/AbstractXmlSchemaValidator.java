package io.openepcis.validation.xml;

import io.openepcis.validation.Validator;
import io.openepcis.validation.exception.SchemaValidationException;
import io.openepcis.validation.formatter.ValidationError;
import io.openepcis.validation.formatter.XmlErrorHandler;
import io.smallrye.mutiny.Multi;
import org.xml.sax.SAXException;

import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.IOException;
import java.io.InputStream;

public abstract class AbstractXmlSchemaValidator implements Validator {

    protected Schema captureXmlSchema;
    protected Schema queryXmlSchema;
    protected EventModifier eventModifier = new EventModifier();
    protected XmlErrorHandler xsdErrorHandler;
    protected SchemaFactory schemaFactory;

    @Override
    public Multi<ValidationError> validateAgainstCaptureSchema(final InputStream captureInput) {
        return Multi.createFrom().emitter(multiEmitter -> {
            try {
                final InputStream inputEvent = eventModifier.modifyEvent(captureInput);
                final javax.xml.validation.Validator validator = captureXmlSchema.newValidator();
                validator.setErrorHandler(this.xsdErrorHandler);
                validator.validate(new StreamSource(inputEvent));
                this.xsdErrorHandler.getExceptions().forEach(multiEmitter::emit);
                multiEmitter.complete();
            } catch (Exception e) {
                multiEmitter.fail(new SchemaValidationException(
                        "Exception occurred during the validation of Capture XML document/event against XSD : "
                                + e.getMessage()
                                + e));
            }
        });
    }

    @Override
    public Multi<ValidationError> validateAgainstQuerySchema(InputStream queryInput)
            throws SchemaValidationException {
        return Multi.createFrom().emitter(multiEmitter -> {
            final javax.xml.validation.Validator validator = queryXmlSchema.newValidator();
            validator.setErrorHandler(this.xsdErrorHandler);
            try {
                validator.validate(new StreamSource(queryInput));
                this.xsdErrorHandler.getExceptions().forEach(multiEmitter::emit);
                multiEmitter.complete();
            } catch (SAXException | IOException e) {
                multiEmitter.fail(
                new SchemaValidationException(
                        "Exception occurred during the validation of Query XML document/event against XSD : "
                                + e.getMessage()
                                + e));
            }
        });

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
