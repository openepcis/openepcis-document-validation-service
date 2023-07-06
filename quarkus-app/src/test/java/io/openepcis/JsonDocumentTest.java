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

public class JsonDocumentTest {

    private SchemaValidator validator;
    private InputStream inputStream;

    @Before
    public void before() {
        validator = new SchemaValidator();
    }

    @Test
    public void validateCaptureDocumentTest() {
        inputStream = getClass().getResourceAsStream("/json/CaptureDocument.json");
        final List<ValidationError> jsonCaptureErrors =
                validator.validate(inputStream, "application/json", SchemaType.CAPTURE_SCHEMA);
        assertTrue(jsonCaptureErrors.size() > 0);
    }

    @Test
    public void validateQueryDocumentWithEventTypeTest() {
        inputStream = getClass().getResourceAsStream("/json/QueryDocumentWithEventType.json");
        final List<ValidationError> jsonQueryErrors =
                validator.validate(inputStream, "application/json", SchemaType.QUERY_SCHEMA);
        assertEquals(0, jsonQueryErrors.size());
    }

    @Test
    public void validateQueryDocumentWithEventTimeTest() {
        inputStream = getClass().getResourceAsStream("/json/queryDocumentWithEventTime.json");
        final List<ValidationError> jsonQueryErrors =
                validator.validate(inputStream, "application/json", SchemaType.QUERY_SCHEMA);
        assertEquals(0, jsonQueryErrors.size());
    }

    @Test
    public void validateQueryDocumentWithActionTest() {
        inputStream = getClass().getResourceAsStream("/json/queryDocumentWithAction.json");
        final List<ValidationError> jsonQueryErrors =
                validator.validate(inputStream, "application/json", SchemaType.QUERY_SCHEMA);
        assertEquals(0, jsonQueryErrors.size());
    }

    @Test
    public void validateQueryDocumentWithInValidEventTypeTest() {
        inputStream = getClass().getResourceAsStream("/json/queryDocumentWithInvalidEventType.json");
        final List<ValidationError> jsonQueryErrors =
                validator.validate(inputStream, "application/json", SchemaType.QUERY_SCHEMA);
        assertTrue(jsonQueryErrors.size() > 0);
    }

}
