/*
 * Copyright 2022-2026 benelog GmbH & Co. KG
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 */
package io.openepcis.validation.test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.serialization.JsonNodeReader;
import io.openepcis.constants.EPCISDocumentType;
import io.openepcis.constants.EPCISVersion;
import io.openepcis.resources.util.Commons;
import io.openepcis.validation.SchemaValidator;
import io.openepcis.validation.config.ValidationDefaults;
import io.openepcis.validation.model.ValidationError;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Validates the quality of JSON validation messages provided to the users
 * Validates: that one mistake produces exactly one error, and the message names the field, the value and the event in plain language.
 */
class JsonValidationMessageTest {

    // path for valid and invalid documents from the test-resources
    private static final String VALID = "2.0/EPCIS/JSON/Capture/Documents/";
    private static final String INVALID = "2.0/EPCIS/JSON/Capture/Invalid/";

    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * Validates one fixture and returns the errors.
     */
    private List<ValidationError> validate(final String resourcePath) {
        final InputStream document = Commons.getInputStream(resourcePath);
        assertNotNull(document, "missing shared test resource " + resourcePath);
        return validate(document);
    }

    private List<ValidationError> validate(final InputStream document) {
        final SchemaValidator validator = new SchemaValidator(
                mapper,
                JsonSchemaFactory.builder(JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7))
                        .jsonNodeReader(JsonNodeReader.builder().jsonMapper(mapper).build())
                        .build());

        return validator
                .validate(document, "application/json", EPCISDocumentType.CAPTURE, EPCISVersion.VERSION_2_0_0)
                .subscribe()
                .asStream()
                .toList();
    }

    // ---------------------------------------------------------------------------
    //  Valid documents must stay silent
    // ---------------------------------------------------------------------------

    @Test
    @DisplayName(VALID + "ObjectEvent.json")
    void validObjectEvent() {
        // Valid document carries two events and a standard bizStep
        assertEquals(0, validate(VALID + "ObjectEvent.json").size());
    }

    @Test
    @DisplayName(VALID + "ObjectEvent_with_custom_bizStep_uri.json")
    void validCustomBizStepUri() {
        // Valid document with custom bizStep URI
        assertEquals(0, validate(VALID + "ObjectEvent_with_custom_bizStep_uri.json").size());
    }

    @Test
    @DisplayName(VALID + "Combination_of_different_event.json")
    void validMultipleEvents() {
        assertEquals(0, validate(VALID + "Combination_of_different_event.json").size());
    }

    // ---------------------------------------------------------------------------
    //  One issue or wrong format in event should produce exactly one error instead of multiple chained errors.
    //  Ex: one wrong bizStep used to produce two errors
    // ---------------------------------------------------------------------------

    @Test
    @DisplayName(INVALID + "ObjectEvent_with_non_standard_bizStep.json")
    void wrongBizStepGivesSingleFriendlyError() {
        final List<ValidationError> errors = validate(INVALID + "ObjectEvent_with_non_standard_bizStep.json");

        // the schema fails both anyOf branches, the response must still show one mistake
        assertEquals(1, errors.size());
        final ValidationError error = errors.getFirst();

        // line is a line number or nothing at all: JSON line numbers belong to the client, which owns
        // the text it renders. The schema rule that rejected the value travels in schemaPath.
        assertNull(error.getLine());
        assertNull(error.getColumn());
        assertNotNull(error.getSchemaPath());
        assertTrue(error.getSchemaPath().contains("/definitions/bizStep"));

        assertEquals(
                "\"transforming\" is not a standard EPCIS business step for bizStep in event 1 "
                        + "(ObjectEvent). Did you mean \"transporting\"? Use one of the 41 standard steps "
                        + "(accepting, arriving, assembling, collecting, commissioning, consigning and 35 more), "
                        + "or, for a step of your own, a full web address such as "
                        + "https://example.com/bizStep/transforming.",
                error.getMessage());

        assertEquals("bizStep", error.getField());
        assertEquals("transforming", error.getValue());
        assertEquals("transporting", error.getSuggestion());
        assertEquals(Integer.valueOf(0), error.getEventIndex());
        assertEquals("ObjectEvent", error.getEventType());
        assertEquals(41, error.getAllowedValues().size()); // full list travels as data for the UI

        // the path the front end maps to an editor line must keep its exact shape
        assertEquals("$.epcisBody.eventList[0].bizStep", error.getLocation());
    }

    @Test
    @DisplayName(INVALID + "ObjectEvent_with_non_standard_bizStep.json")
    void messageStaysShortEvenForLargeVocabularies() {
        final ValidationError error = validate(INVALID + "ObjectEvent_with_non_standard_bizStep.json").getFirst();

        // the old message was over 600 characters of raw enum dump
        assertTrue(error.getMessage().length() < 400, "message too long: " + error.getMessage().length());
        assertTrue(error.getMessage().contains("and 35 more"));
    }

    @Test
    @DisplayName("invalid: a wrong disposition suggests the closest standard disposition")
    void wrongDispositionSuggestsClosest() {
        final List<ValidationError> errors = validate(INVALID + "ObjectEvent_with_non_standard_disposition.json");
        assertEquals(1, errors.size());
        assertEquals("in_transit", errors.getFirst().getSuggestion());
        assertTrue(errors.getFirst().getMessage().contains("Did you mean \"in_transit\"?"));
    }

    // ---------------------------------------------------------------------------
    //  Grouping: several raw messages for one mistake
    // ---------------------------------------------------------------------------

    @Test
    @DisplayName("invalid: a wrong action gives ONE error, and offers no custom URI")
    void wrongActionGivesSingleError() {
        // the raw engine reports four messages here, one enum plus three branch patterns
        final List<ValidationError> errors = validate(INVALID + "ObjectEvent_with_invalid_action.json");
        assertEquals(1, errors.size());

        final ValidationError error = errors.getFirst();
        assertEquals("action", error.getField());
        assertEquals("CREATE", error.getValue());
        assertTrue(error.getMessage().contains("OBSERVE, ADD, DELETE"));

        // action has no custom URI branch, so suggesting a web address would be wrong advice
        assertFalse(error.getMessage().contains("web address"), "action must not be offered a custom URI: " + error.getMessage());
    }

    @Test
    @DisplayName("invalid: an unknown field wins over the misleading branch errors")
    void unknownFieldReportedInsteadOfBranchNoise() {
        // the raw engine reports five messages, three of them wrongly claiming epcList is missing
        final List<ValidationError> errors = validate(INVALID + "ObjectEvent_with_unknown_field_in_quantityList.json");

        assertEquals(1, errors.size());
        assertEquals("weight", errors.getFirst().getField());
        assertTrue(errors.getFirst().getMessage().contains("ex:weight")); // suggest how to fix it
    }

    @Test
    @DisplayName("invalid: an event missing every identifier list gets one 'at least one of' message")
    void missingIdentifierListsGivesOneChoice() {
        final List<ValidationError> errors = validate(INVALID + "ObjectEvent_without_identifier_list.json");

        assertEquals(1, errors.size());
        assertTrue(errors.getFirst().getMessage().contains("at least one of"));
        assertTrue(errors.getFirst().getMessage().contains("epcList"));
    }

    @Test
    @DisplayName("invalid: several genuinely missing fields are ALL named, none is dropped")
    void everyMissingRequiredFieldIsNamed() {
        // regression guard: grouping must never hide a second missing field at the same position
        final List<ValidationError> errors = validate(INVALID + "ObjectEvent_without_action_and_eventTime.json");
        final String combined = String.join(" | ", errors.stream().map(ValidationError::getMessage).toList());

        assertTrue(combined.contains("action"), "action not reported: " + combined);
        assertTrue(combined.contains("eventTime"), "eventTime not reported: " + combined);
    }

    // ---------------------------------------------------------------------------
    //  Other common mistakes
    // ---------------------------------------------------------------------------

    @Test
    @DisplayName("invalid: a bad eventTime explains the expected format with an example")
    void badEventTimeShowsExample() {
        final List<ValidationError> errors = validate(INVALID + "ObjectEvent_with_invalid_eventTime_format.json");

        assertEquals(1, errors.size());
        assertEquals("eventTime", errors.getFirst().getField());
        assertTrue(errors.getFirst().getMessage().contains("2026-07-22T09:20:00+02:00"));
        assertTrue(errors.getFirst().getMessage().contains("22-07-2026"));
    }

    @Test
    @DisplayName("invalid: a text quantity says a number is expected")
    void quantityMustBeNumber() {
        final List<ValidationError> errors = validate(INVALID + "ObjectEvent_with_quantity_as_text.json");

        assertEquals(1, errors.size());
        assertEquals("quantity", errors.getFirst().getField());
        assertTrue(errors.getFirst().getMessage().contains("number"));
    }

    @Test
    @DisplayName("invalid: an ilmd field without a namespace is told to add one")
    void ilmdFieldNeedsNamespace() {
        final List<ValidationError> errors = validate(INVALID + "ObjectEvent_with_ilmd_field_without_namespace.json");

        assertEquals(1, errors.size());
        assertEquals("materialType", errors.getFirst().getField());
        assertTrue(errors.getFirst().getMessage().contains("ex:materialType"));
    }

    @Test
    @DisplayName("invalid: a missing @context is reported against the document, not an event")
    void missingContextIsReportedAtDocumentLevel() {
        final List<ValidationError> errors = validate(INVALID + "EPCISDocument_without_context.json");
        assertEquals(1, errors.size());
        assertEquals("@context", errors.getFirst().getField());
        assertNull(errors.getFirst().getEventIndex());
        assertTrue(errors.getFirst().getMessage().contains("The document could not be read"));
    }

    @Test
    @DisplayName("invalid: the event number in the message is one based and names the event type")
    void eventNumberIsHumanReadable() {
        final List<ValidationError> errors = validate(INVALID + "ObjectEvent_with_non_standard_bizStep_in_second_event.json");

        assertEquals(1, errors.size());
        assertEquals(Integer.valueOf(1), errors.getFirst().getEventIndex());
        assertTrue(errors.getFirst().getMessage().contains("event 2 (ObjectEvent)"));
    }

    // ---------------------------------------------------------------------------
    //  Contract guards: the front end depends on these
    // ---------------------------------------------------------------------------

    @Test
    @DisplayName("contract: every error carries a message, a type and a JSON path location")
    void everyErrorIsUsableByTheFrontEnd() {
        final List<ValidationError> errors = validate(INVALID + "Combination_of_multiple_invalid_fields.json");
        assertTrue(errors.size() > 1);

        for (final ValidationError error : errors) {
            assertNotNull(error.getMessage(), "message missing");
            assertFalse(error.getMessage().isBlank(), "message blank");
            assertNotNull(error.getType(), "type missing");
            assertNotNull(error.getLocation(), "location missing");

            // the front end strips the leading $ and maps the rest to an editor line
            assertTrue(error.getLocation().startsWith("$"), "unexpected location: " + error.getLocation());
        }
    }

    @Test
    @DisplayName("contract: errors come back in document order")
    void errorsAreInDocumentOrder() {
        final List<ValidationError> errors = validate(INVALID + "Combination_of_non_standard_bizStep_and_disposition.json");

        assertEquals(2, errors.size());
        assertEquals(Integer.valueOf(0), errors.get(0).getEventIndex());
        assertEquals(Integer.valueOf(1), errors.get(1).getEventIndex());
    }

    //  Test to print every message for manual review.
    @Test
    @DisplayName("review: print the messages for a document with many different mistakes")
    void printMessagesForReview() {
        final List<ValidationError> errors = validate(INVALID + "Combination_of_multiple_invalid_fields.json");

        System.out.println("---- JSON validation messages for review ----");
        errors.forEach(error -> System.out.printf("%-22s %-46s %s%n", error.getType(), error.getLocation(), error.getMessage()));
        assertTrue(errors.size() >= 5);
    }

    // Test if the validation message limit is working and capping correctly
    @Test
    @DisplayName("invalid: a document with more problems than the limit is capped and informs about the limit")
    void errorListIsCappedTest() throws Exception {
        final int events = ValidationDefaults.MAX_REPORTED_ERRORS + 500;
        // Generate dummy event with more errors
        final StringBuilder document = new StringBuilder()
                .append("{\"@context\":[\"https://ref.gs1.org/standards/epcis/epcis-context.jsonld\"],")
                .append("\"type\":\"EPCISDocument\",\"schemaVersion\":\"2.0\",")
                .append("\"creationDate\":\"2026-07-22T10:30:00Z\",\"epcisBody\":{\"eventList\":[");

        // Generate eventList with number of events exceeding the MAX_REPORTED_ERRORS limit
        for (int i = 0; i < events / 2; i++) {
            if (i > 0) {
                document.append(',');
            }
            // 2 clear mistake per event: bizStep is not a standard value and eventTime in wrong format
            document.append("{\"type\":\"ObjectEvent\",\"eventTime\":\"2026-07-T06:45:00+02:00\",")
                    .append("\"eventTimeZoneOffset\":\"+02:00\",\"action\":\"ADD\",")
                    .append("\"bizStep\":\"transforming\",")
                    .append("\"epcList\":[\"https://id.gs1.org/01/09521141012345/21/2017\"]}");
        }
        document.append("]}}");
        final List<ValidationError> errors = validate(new ByteArrayInputStream(document.toString().getBytes(StandardCharsets.UTF_8)));

        // ValidationDefaults.MAX_REPORTED_ERRORS - 1 real problems plus one entry saying the list is incomplete
        assertEquals(ValidationDefaults.MAX_REPORTED_ERRORS, errors.size());
        assertEquals("limitExceeded", errors.getLast().getType());
        assertTrue(errors.getLast().getMessage().contains(String.valueOf(ValidationDefaults.MAX_REPORTED_ERRORS - 1)));

        // the reported ones are the first events, in order
        assertEquals("$.epcisBody.eventList[0].bizStep", errors.getFirst().getLocation());
        assertEquals("$.epcisBody.eventList[0].eventTime", errors.get(1).getLocation());
    }
}
