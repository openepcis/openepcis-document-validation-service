package io.openepcis;

import io.openepcis.core.SchemaType;
import io.openepcis.core.SchemaValidator;
import io.openepcis.core.formatter.ValidationError;
import org.junit.Before;
import org.junit.Test;

import java.io.InputStream;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class XmlDocumentTest {
    private SchemaValidator validator;
    private InputStream inputStream;

    @Before
    public void before() {
        validator = new SchemaValidator();
    }

    @Test
    public void captureDocumentTest() {
        inputStream = getClass().getResourceAsStream("/xml/captureDocument.xml");
        final List<ValidationError> xsdCaptureErrors =
                validator.validate(inputStream, "application/xml", SchemaType.CAPTURE_SCHEMA);
        assertTrue(xsdCaptureErrors.size() > 0);
    }

    @Test
    public void queryDocumentWithEventTimeTest() {
        inputStream = getClass().getResourceAsStream("/xml/queryDocument.xml");
        final List<ValidationError> xsdQueryErrors =
                validator.validate(inputStream, "application/xml", SchemaType.QUERY_SCHEMA);
        assertEquals(0, xsdQueryErrors.size());
    }

    @Test
    public void queryDocumentWithEventTypeTest() {
        inputStream = getClass().getResourceAsStream("/xml/queryDocumentWithEventType.xml");
        final List<ValidationError> xsdQueryErrors =
                validator.validate(inputStream, "application/xml", SchemaType.QUERY_SCHEMA);
        assertEquals(0, xsdQueryErrors.size());
    }

    @Test
    public void queryDocumentWithReadPointTest() {
        inputStream = getClass().getResourceAsStream("/xml/queryDocumentWithReadPoint.xml");
        final List<ValidationError> xsdQueryErrors =
                validator.validate(inputStream, "application/xml", SchemaType.QUERY_SCHEMA);
        assertEquals(0, xsdQueryErrors.size());
    }

    @Test
    public void queryDocumentWithMissingValueTest() {
        inputStream = getClass().getResourceAsStream("/xml/queryDocumentWithMissingValue.xml");
        final List<ValidationError> xsdQueryErrors =
                validator.validate(inputStream, "application/xml", SchemaType.QUERY_SCHEMA);
        assertEquals(1, xsdQueryErrors.size());
    }
}