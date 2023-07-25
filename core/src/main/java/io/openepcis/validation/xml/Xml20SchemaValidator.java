package io.openepcis.validation.xml;

import io.openepcis.validation.formatter.XmlErrorHandler;
import io.openepcis.validation.resolver.ClasspathResourceResolver;

import javax.xml.XMLConstants;
import javax.xml.validation.SchemaFactory;

public class Xml20SchemaValidator extends AbstractXmlSchemaValidator {
    public Xml20SchemaValidator() {
        SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        schemaFactory.setResourceResolver(new ClasspathResourceResolver("xsd/2_0_0"));
        this.schemaFactory = schemaFactory;
        this.captureXmlSchema = loadSchema("/xsd/2_0_0/EPCglobal-epcis-2_0.xsd");
        this.queryXmlSchema = loadSchema("/xsd/2_0_0/EPCglobal-epcis-query-2_0.xsd");
        this.xsdErrorHandler = new XmlErrorHandler();
    }
}
