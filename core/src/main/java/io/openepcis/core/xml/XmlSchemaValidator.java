package io.openepcis.core.xml;

import io.openepcis.core.Validator;
import io.openepcis.core.exception.SchemaValidationException;
import io.openepcis.core.formatter.ValidationError;
import io.openepcis.core.formatter.XmlErrorHandler;
import io.openepcis.core.resolver.ClasspathResourceResolver;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
public class XmlSchemaValidator implements Validator {

  private final SchemaFactory schemaFactory =
      SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
  private final Schema captureXmlSchema;
  private final Schema queryXmlSchema;
  private final EventModifier eventModifier = new EventModifier();

  private final XmlErrorHandler xsdErrorHandler;

  public XmlSchemaValidator() {
    // Add the resolver to resolve the import of other XSD files
    schemaFactory.setResourceResolver(new ClasspathResourceResolver());
    this.captureXmlSchema = loadSchema("/xsd/EPCglobal-epcis-2_0.xsd");
    this.queryXmlSchema = loadSchema("/xsd/EPCglobal-epcis-query-2_0.xsd");
    this.xsdErrorHandler = new XmlErrorHandler();
  }

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

  public Schema loadSchema(final String name) {
    Schema schema;
    try {
      schema = schemaFactory.newSchema(new StreamSource(getResourceAsStream(name)));
    } catch (Exception e) {
      throw new SchemaValidationException(
          "Exception occurred during the loading of the XSD document to schema : "
              + e.getMessage()
              + e);
    }
    return schema;
  }

}
