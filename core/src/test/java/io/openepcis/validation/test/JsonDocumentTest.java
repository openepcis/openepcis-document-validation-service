package io.openepcis.validation.test;

import io.openepcis.constants.EPCISDocumentType;
import io.openepcis.constants.EPCISVersion;
import io.openepcis.validation.SchemaValidator;
import io.openepcis.validation.model.ValidationError;
import io.smallrye.mutiny.Multi;
import org.junit.jupiter.api.Test;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


public class JsonDocumentTest {

    private SchemaValidator validator = new SchemaValidator();;

    @Test
    public void validateCaptureDocumentTest() {
        final InputStream inputStream = getClass().getResourceAsStream("/json/CaptureDocument.json");
        final Multi<ValidationError> jsonCaptureErrors =
                validator.validate(inputStream, "application/json", EPCISDocumentType.CAPTURE, EPCISVersion.VERSION_2_0_0);
        assertTrue(jsonCaptureErrors.subscribe().asStream().toList().size() > 0);
    }

    @Test
    public void validateQueryDocumentWithEventTypeTest() {
        final InputStream inputStream = getClass().getResourceAsStream("/json/QueryDocumentWithEventType.json");
        final Multi<ValidationError> jsonQueryErrors =
                validator.validate(inputStream, "application/json", EPCISDocumentType.QUERY, EPCISVersion.VERSION_2_0_0);
        assertEquals(0, jsonQueryErrors.subscribe().asStream().toList().size());
    }

    @Test
    public void validateQueryDocumentWithEventTimeTest() {
        final InputStream inputStream = getClass().getResourceAsStream("/json/queryDocumentWithEventTime.json");
        final Multi<ValidationError> jsonQueryErrors =
                validator.validate(inputStream, "application/json", EPCISDocumentType.QUERY, EPCISVersion.VERSION_2_0_0);
        assertEquals(0, jsonQueryErrors.subscribe().asStream().toList().size());
    }

    @Test
    public void validateQueryDocumentWithActionTest() {
        final InputStream inputStream = getClass().getResourceAsStream("/json/queryDocumentWithAction.json");
        final Multi<ValidationError> jsonQueryErrors =
                validator.validate(inputStream, "application/json", EPCISDocumentType.QUERY, EPCISVersion.VERSION_2_0_0);
        assertEquals(0, jsonQueryErrors.subscribe().asStream().toList().size());
    }

    @Test
    public void validateQueryDocumentWithInValidEventTypeTest() {
        final InputStream inputStream = getClass().getResourceAsStream("/json/queryDocumentWithInvalidEventType.json");
        final Multi<ValidationError> jsonQueryErrors =
                validator.validate(inputStream, "application/json", EPCISDocumentType.QUERY, EPCISVersion.VERSION_2_0_0);
        assertTrue(jsonQueryErrors.subscribe().asStream().toList().size() > 0);
    }

}
