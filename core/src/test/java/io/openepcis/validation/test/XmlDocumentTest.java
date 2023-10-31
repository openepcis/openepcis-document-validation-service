package io.openepcis.validation.test;

import io.openepcis.constants.EPCISDocumentType;
import io.openepcis.constants.EPCISVersion;
import io.openepcis.validation.SchemaValidator;
import io.openepcis.validation.model.ValidationError;
import io.smallrye.mutiny.Multi;
import org.junit.Before;
import org.junit.Test;

import java.io.InputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;

public class XmlDocumentTest {
    private SchemaValidator validator;
    private InputStream inputStream;

    @Before
    public void before() {
        validator = new SchemaValidator();
    }

    @Test
    public void captureDocumentWithBareEventTest() {
        inputStream = getClass().getResourceAsStream("/xml/captureDocument-independent-object-event.xml");
        final Multi<ValidationError> xsdCaptureErrors =
                validator.validate(inputStream, "application/xml", EPCISDocumentType.CAPTURE, EPCISVersion.VERSION_2_0_0);
        var errors = xsdCaptureErrors.subscribe().asStream().toList();
        assertEquals(1, errors.size());
    }


    @Test
    public void captureDocumentWithComment() {
        inputStream = getClass().getResourceAsStream("/xml/captureDocumentWithComment.xml");
        final Multi<ValidationError> xsdCaptureErrors =
                validator.validate(inputStream, "application/xml", EPCISDocumentType.CAPTURE, EPCISVersion.VERSION_1_2_0);
        var errors = xsdCaptureErrors.subscribe().asStream().toList();
        assertEquals(0, errors.size());
    }

    @Test
    public void captureDocumentCompleteEPCISDocumentTest() {
        inputStream = getClass().getResourceAsStream("/xml/captureDocument-complete-epcis-20-document.xml");
        final Multi<ValidationError> xsdCaptureErrors =
                validator.validate(inputStream, "application/xml", EPCISDocumentType.CAPTURE, EPCISVersion.VERSION_2_0_0);
        assertEquals(0, xsdCaptureErrors.subscribe().asStream().toList().size());
    }

    @Test
    public void queryDocumentWithEventTimeTest() {
        inputStream = getClass().getResourceAsStream("/xml/queryDocument.xml");
        final Multi<ValidationError> xsdQueryErrors =
                validator.validate(inputStream, "application/xml", EPCISDocumentType.QUERY,EPCISVersion.VERSION_2_0_0);
        assertEquals(0, xsdQueryErrors.subscribe().asStream().toList().size());
    }

    @Test
    public void queryDocumentWithEventTypeTest() {
        inputStream = getClass().getResourceAsStream("/xml/queryDocumentWithEventType.xml");
        final Multi<ValidationError> xsdQueryErrors =
                validator.validate(inputStream, "application/xml", EPCISDocumentType.QUERY, EPCISVersion.VERSION_2_0_0);
        assertEquals(0, xsdQueryErrors.subscribe().asStream().toList().size());
    }

    @Test
    public void queryDocumentWithReadPointTest() {
        inputStream = getClass().getResourceAsStream("/xml/queryDocumentWithReadPoint.xml");
        final Multi<ValidationError> xsdQueryErrors =
                validator.validate(inputStream, "application/xml", EPCISDocumentType.QUERY, EPCISVersion.VERSION_2_0_0);
        assertEquals(0, xsdQueryErrors.subscribe().asStream().toList().size());
    }

    @Test
    public void queryDocumentWithMissingValueTest() {
        inputStream = getClass().getResourceAsStream("/xml/queryDocumentWithMissingValue.xml");
        final Multi<ValidationError> xsdQueryErrors =
                validator.validate(inputStream, "application/xml", EPCISDocumentType.QUERY, EPCISVersion.VERSION_2_0_0);
        assertEquals(1, xsdQueryErrors.subscribe().asStream().toList().size());
    }

    public static void main(String[] args) {
        final String xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <!-- Section 2.2.1.1 Direct Purchase Scenario Seller Exchange1 -->
                <!--Use Case Example: Seller (MFG) processes the order for 3 units and provides Buyer (W) with DSCSA transaction data.
                     This example shows the EPCIS message sent by Manufacturer (MFG) to WholesalerDistributor (W)\s
                      Example shipment: 1 pallet containing 3 units of GTIN A which in this example represent 3 cases of GTIN A with each case containing 4 LSUs \s
                -->
                <epcis:EPCISDocument xmlns:cbvmda="urn:epcglobal:cbv:mda" xmlns:sbdh="http://www.unece.org/cefact/namespaces/StandardBusinessDocumentHeader" xmlns:gs1ushc="http://epcis.gs1us.org/hc/ns" xmlns:epcis="urn:epcglobal:epcis:xsd:1" schemaVersion="1.2" xsi:schemaLocation="urn:epcglobal:epcis:xsd:1 EPCglobal-epcis-1_2.xsd" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" creationDate="2023-04-01T08:45:16Z">
                  <EPCISHeader>
                    <sbdh:StandardBusinessDocumentHeader>                
                """;
        //
        final String docTagPattern = "<(?!\\?|\\!--).*:EPCISDocument(.*?)>";
        final Pattern pattern = Pattern.compile(docTagPattern);
        final Matcher matcher = pattern.matcher(xml);
        System.out.println(matcher.find());
        System.out.println(matcher.group(1));
    }
}
