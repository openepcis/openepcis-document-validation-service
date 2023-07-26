package io.openepcis.validation.test;

import io.openepcis.constants.EPCISDocumentType;
import io.openepcis.constants.EPCISVersion;
import io.openepcis.validation.SchemaValidator;
import io.openepcis.validation.model.ValidationError;
import io.smallrye.mutiny.Multi;
import org.junit.Before;
import org.junit.Test;

import java.io.InputStream;

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
        final Multi<ValidationError> jsonCaptureErrors =
                validator.validate(inputStream, "application/json", EPCISDocumentType.CAPTURE, EPCISVersion.VERSION_2_0_0.getSchemaVersion());
        assertTrue(jsonCaptureErrors.subscribe().asStream().toList().size() > 0);
    }

    @Test
    public void validateQueryDocumentWithEventTypeTest() {
        inputStream = getClass().getResourceAsStream("/json/QueryDocumentWithEventType.json");
        final Multi<ValidationError> jsonQueryErrors =
                validator.validate(inputStream, "application/json", EPCISDocumentType.QUERY, EPCISVersion.VERSION_2_0_0.getSchemaVersion());
        assertEquals(0, jsonQueryErrors.subscribe().asStream().toList().size());
    }

    @Test
    public void validateQueryDocumentWithEventTimeTest() {
        inputStream = getClass().getResourceAsStream("/json/queryDocumentWithEventTime.json");
        final Multi<ValidationError> jsonQueryErrors =
                validator.validate(inputStream, "application/json", EPCISDocumentType.QUERY, EPCISVersion.VERSION_2_0_0.getSchemaVersion());
        assertEquals(0, jsonQueryErrors.subscribe().asStream().toList().size());
    }

    @Test
    public void validateQueryDocumentWithActionTest() {
        inputStream = getClass().getResourceAsStream("/json/queryDocumentWithAction.json");
        final Multi<ValidationError> jsonQueryErrors =
                validator.validate(inputStream, "application/json", EPCISDocumentType.QUERY, EPCISVersion.VERSION_2_0_0.getSchemaVersion());
        assertEquals(0, jsonQueryErrors.subscribe().asStream().toList().size());
    }

    @Test
    public void validateQueryDocumentWithInValidEventTypeTest() {
        inputStream = getClass().getResourceAsStream("/json/queryDocumentWithInvalidEventType.json");
        final Multi<ValidationError> jsonQueryErrors =
                validator.validate(inputStream, "application/json", EPCISDocumentType.QUERY, EPCISVersion.VERSION_2_0_0.getSchemaVersion());
        assertTrue(jsonQueryErrors.subscribe().asStream().toList().size() > 0);
    }

}
