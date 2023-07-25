package io.openepcis.validation.test;

import io.openepcis.constants.EPCISVersion;
import io.openepcis.validation.SchemaType;
import io.openepcis.validation.SchemaValidator;
import io.openepcis.validation.formatter.ValidationError;
import io.smallrye.mutiny.Multi;
import org.junit.Before;
import org.junit.Test;

import java.io.InputStream;

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
                validator.validate(inputStream, "application/xml", SchemaType.CAPTURE_SCHEMA, EPCISVersion.VERSION_2_0_0.getSchemaVersion());
        var errors = xsdCaptureErrors.subscribe().asStream().toList();
        assertEquals(1, errors.size());
    }

    @Test
    public void captureDocumentCompleteEPCISDocumentTest() {
        inputStream = getClass().getResourceAsStream("/xml/captureDocument-complete-epcis-20-document.xml");
        final Multi<ValidationError> xsdCaptureErrors =
                validator.validate(inputStream, "application/xml", SchemaType.CAPTURE_SCHEMA, EPCISVersion.VERSION_2_0_0.getSchemaVersion());
        assertEquals(0, xsdCaptureErrors.subscribe().asStream().toList().size());
    }

    @Test
    public void queryDocumentWithEventTimeTest() {
        inputStream = getClass().getResourceAsStream("/xml/queryDocument.xml");
        final Multi<ValidationError> xsdQueryErrors =
                validator.validate(inputStream, "application/xml", SchemaType.QUERY_SCHEMA,EPCISVersion.VERSION_2_0_0.getSchemaVersion());
        assertEquals(0, xsdQueryErrors.subscribe().asStream().toList().size());
    }

    @Test
    public void queryDocumentWithEventTypeTest() {
        inputStream = getClass().getResourceAsStream("/xml/queryDocumentWithEventType.xml");
        final Multi<ValidationError> xsdQueryErrors =
                validator.validate(inputStream, "application/xml", SchemaType.QUERY_SCHEMA, EPCISVersion.VERSION_2_0_0.getSchemaVersion());
        assertEquals(0, xsdQueryErrors.subscribe().asStream().toList().size());
    }

    @Test
    public void queryDocumentWithReadPointTest() {
        inputStream = getClass().getResourceAsStream("/xml/queryDocumentWithReadPoint.xml");
        final Multi<ValidationError> xsdQueryErrors =
                validator.validate(inputStream, "application/xml", SchemaType.QUERY_SCHEMA, EPCISVersion.VERSION_2_0_0.getSchemaVersion());
        assertEquals(0, xsdQueryErrors.subscribe().asStream().toList().size());
    }

    @Test
    public void queryDocumentWithMissingValueTest() {
        inputStream = getClass().getResourceAsStream("/xml/queryDocumentWithMissingValue.xml");
        final Multi<ValidationError> xsdQueryErrors =
                validator.validate(inputStream, "application/xml", SchemaType.QUERY_SCHEMA, EPCISVersion.VERSION_2_0_0.getSchemaVersion());
        assertEquals(1, xsdQueryErrors.subscribe().asStream().toList().size());
    }
}
