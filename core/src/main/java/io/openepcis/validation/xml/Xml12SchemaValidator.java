package io.openepcis.validation.xml;

import io.openepcis.validation.formatter.XmlErrorHandler;
import io.openepcis.validation.resolver.ClasspathResourceResolver;

import javax.xml.XMLConstants;
import javax.xml.validation.SchemaFactory;

public class Xml12SchemaValidator extends AbstractXmlSchemaValidator {
    public Xml12SchemaValidator() {
        SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        schemaFactory.setResourceResolver(new ClasspathResourceResolver("xsd/1_2_0"));
        this.schemaFactory = schemaFactory;
        this.captureXmlSchema = loadSchema("/xsd/1_2_0/EPCglobal-epcis-1_2.xsd");
        this.queryXmlSchema = loadSchema("/xsd/1_2_0/EPCglobal-epcis-query-1_2.xsd");
        this.xsdErrorHandler = new XmlErrorHandler();
    }
}
