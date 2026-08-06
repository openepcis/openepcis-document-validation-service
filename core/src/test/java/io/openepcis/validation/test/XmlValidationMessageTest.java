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
import io.openepcis.validation.model.ValidationError;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Checks the XML documents of validation reporting.
 * Xerces reports one mistake twice, once as a facet failure and once as an element failure, so the response used to contain duplicates.
 */
class XmlValidationMessageTest {

    // path for valid and invalid documents from the test-resources
    private static final String VALID = "2.0/EPCIS/XML/Capture/Documents/";
    private static final String INVALID = "2.0/EPCIS/XML/Capture/Invalid/";

    private static final String VALID_12 = "1.2/EPCIS/XML/Capture/Documents/";
    private static final String QUERY = "2.0/EPCIS/XML/Query/";
    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * Validates one fixture and returns the errors.
     */
    private List<ValidationError> validate(final String resourcePath, final EPCISDocumentType schema, final EPCISVersion version) {
        final InputStream document = Commons.getInputStream(resourcePath);
        assertNotNull(document, "missing test resource " + resourcePath);

        final SchemaValidator validator = new SchemaValidator(
                mapper,
                JsonSchemaFactory.builder(JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7))
                        .jsonNodeReader(JsonNodeReader.builder().jsonMapper(mapper).build()).build());

        return validator
                .validate(document, "application/xml", schema, version)
                .subscribe()
                .asStream()
                .toList();
    }

    // Util method for the validation of EPCIS 2.0 documents and return errors
    private List<ValidationError> validateCapture(final String fixture) {
        return validate(fixture, EPCISDocumentType.CAPTURE, EPCISVersion.VERSION_2_0_0);
    }

    // ---------------------------------------------------------------------------
    //  Valid documents must stay silent
    // ---------------------------------------------------------------------------

    @Test
    @DisplayName("valid: a complete EPCIS 2.0 XML capture document reports no errors")
    void validCompleteDocument() {
        assertEquals(0, validateCapture(VALID + "Combination_of_different_event.xml").size());
    }

    @Test
    @DisplayName("valid: a correctly ordered ObjectEvent reports no errors")
    void validObjectEvent() {
        assertEquals(0, validateCapture(VALID + "ObjectEvent.xml").size());
    }

    @Test
    @DisplayName("valid: an EPCIS 1.2 document with comments reports no errors")
    void validVersion12Document() {
        assertEquals(0,
                validate(VALID_12 + "EPCISDocument_with_comment.xml", EPCISDocumentType.CAPTURE, EPCISVersion.VERSION_1_2_0).size());
    }

    @Test
    @DisplayName("valid: a query document reports no errors")
    void validQueryDocument() {
        assertEquals(0,
                validate(QUERY + "queryDocument.xml", EPCISDocumentType.QUERY, EPCISVersion.VERSION_2_0_0).size());
    }

    // ---------------------------------------------------------------------------
    //  Duplicates: Xerces reports one mistake twice
    // ---------------------------------------------------------------------------

    @Test
    @DisplayName("invalid: a wrong action gives ONE error, not the two Xerces reports")
    void wrongActionIsNotDuplicated() {
        // Xerces emits "not facet-valid" and "value of element is not valid" at the same position
        final List<ValidationError> errors = validateCapture(INVALID + "ObjectEvent_with_invalid_action.xml");
        assertEquals(1, errors.size());

        final ValidationError error = errors.getFirst();
        assertEquals("enum", error.getType());
        assertNotEquals("required", error.getType());
        assertTrue(error.getMessage().contains("CREATE")); // echoes what user sent
        assertTrue(error.getMessage().contains("ADD")); // and lists what is allowed
    }

    @Test
    @DisplayName("invalid: a bad eventTime gives ONE error typed as a pattern failure")
    void badEventTimeIsNotDuplicated() {
        final List<ValidationError> errors = validateCapture(INVALID + "ObjectEvent_with_invalid_eventTime_format.xml");
        assertEquals(1, errors.size());
        assertEquals("pattern", errors.getFirst().getType());
        assertTrue(errors.getFirst().getMessage().contains("22-07-2026"));
    }

    // ---------------------------------------------------------------------------
    //  The error type must describe what actually went wrong
    // ---------------------------------------------------------------------------

    @Test
    @DisplayName("invalid: elements in the wrong order are typed as an unexpected element")
    void wrongElementOrderIsTyped() {
        final List<ValidationError> errors = validateCapture(INVALID + "ObjectEvent_with_wrong_element_order.xml");
        assertEquals(1, errors.size());
        assertEquals("unexpectedElement", errors.getFirst().getType());
        assertTrue(errors.getFirst().getMessage().contains("action"));
    }

    @Test
    @DisplayName("invalid: an element EPCIS does not know is typed as an unexpected element")
    void unknownElementIsTyped() {
        final List<ValidationError> errors = validateCapture(INVALID + "ObjectEvent_with_unknown_element.xml");
        assertEquals(1, errors.size());
        assertEquals("unexpectedElement", errors.getFirst().getType());
        assertTrue(errors.getFirst().getMessage().contains("madeUpField"));
    }

    @Test
    @DisplayName("invalid: a missing mandatory element is reported")
    void missingMandatoryElementIsReported() {
        final List<ValidationError> errors = validateCapture(INVALID + "ObjectEvent_without_eventTime.xml");
        assertEquals(1, errors.size());
        assertTrue(errors.getFirst().getMessage().contains("eventTime")); // names what is expected
    }

    // ---------------------------------------------------------------------------
    //  Contract guards: the front end depends on these
    // ---------------------------------------------------------------------------

    @Test
    @DisplayName("contract: the XSD rule code prefix is stripped from the message")
    void ruleCodePrefixIsRemoved() {
        final List<ValidationError> errors = validateCapture(INVALID + "ObjectEvent_with_unknown_element.xml");
        // "cvc-complex-type.2.4.a: " is noise for a reader and must not survive into the response
        assertFalse(errors.getFirst().getMessage().startsWith("cvc-"), "rule code leaked: " + errors.getFirst().getMessage());
    }

    @Test
    @DisplayName("contract: line and column are present, because the XML editor highlights by line")
    void lineAndColumnArePresent() {
        final ValidationError error = validateCapture(INVALID + "ObjectEvent_with_unknown_element.xml").getFirst();
        // for XML the front end reads line, and location carries the column
        System.out.println(error);
        assertNotNull(error.getLine());
        assertNotNull(error.getColumn());
        assertTrue(error.getLine() > 0);
        assertTrue(error.getColumn() > 0);
    }

    @Test
    @DisplayName("Check if proper field name and value are included in error")
    void checkFieldAndValueTest() {
        // Confirm field name and value is included in error
        final ValidationError error = validateCapture(INVALID + "ObjectEvent_with_invalid_eventTime_format.xml").getFirst();
        assertNotNull(error.getField());
        assertNotNull(error.getValue());
        assertNotNull(error.getLine());
        assertNotNull(error.getColumn());
    }

    @Test
    @DisplayName("Check if proper field , value and suggestions are included in error")
    void checkFieldValueAndSuggestionTest() {
        // Confirm field name and value is included in error
        final ValidationError error = validateCapture(INVALID + "ObjectEvent_with_invalid_action.xml").getFirst();
        assertNotNull(error.getField());
        assertNotNull(error.getValue());
        assertNotNull(error.getAllowedValues());
        assertFalse(error.getAllowedValues().isEmpty());
        assertNotNull(error.getLine());
        assertNotNull(error.getColumn());
    }

    @Test
    @DisplayName("contract: a bare ObjectEvent is wrapped and validated, keeping existing behaviour")
    void bareEventStillValidates() {
        // the wrapping path in EventModifier must keep working, this fixture reports exactly one error
        assertEquals(1, validateCapture(INVALID + "ObjectEvent_bare_event_without_action.xml").size());
    }

    // ---------------------------------------------------------------------------
    //  The reported fields must mean the same thing for XML as they do for JSON
    // ---------------------------------------------------------------------------

    @Test
    @DisplayName("invalid: an XML enum failure carries the field, the value and the accepted values")
    void xmlEnumErrorCarriesFieldValueAndAllowedValues() {
        final ValidationError error = validateCapture(INVALID + "ObjectEvent_with_invalid_action.xml").getFirst();

        assertEquals("enum", error.getType());
        // the element name only appears in the duplicate message Xerces emits, so this proves the
        // duplicate is mined for detail instead of being discarded
        assertEquals("action", error.getField());
        assertEquals("CREATE", error.getValue());
        assertEquals(List.of("ADD", "OBSERVE", "DELETE"), error.getAllowedValues());
    }

    @Test
    @DisplayName("invalid: a near miss XML value suggests the closest accepted value")
    void xmlNearMissSuggestsClosestValue() {
        final ValidationError error = validateCapture(INVALID + "ObjectEvent_with_near_miss_action.xml").getFirst();

        assertEquals("OBSERV", error.getValue());
        assertEquals("OBSERVE", error.getSuggestion()); // one edit away, so worth offering
    }

    @Test
    @DisplayName("invalid: a distant XML value offers no suggestion rather than a guess")
    void xmlDistantValueOffersNoSuggestion() {
        final ValidationError error = validateCapture(INVALID + "ObjectEvent_with_invalid_action.xml").getFirst();

        // CREATE is four edits from DELETE, past the threshold, so suggesting anything would be noise
        assertNull(error.getSuggestion());
    }

    @Test
    @DisplayName("invalid: line and column are numbers and the schema rule travels in schemaPath")
    void xmlErrorReportsRealLineAndColumn() {
        final ValidationError error = validateCapture(INVALID + "ObjectEvent_with_invalid_action.xml").getFirst();

        // line used to hold a schema URI for JSON and a line number for XML; it is now always a number
        assertNotNull(error.getLine());
        assertTrue(error.getLine() > 0, "line must be a real line number");
        assertNotNull(error.getColumn());
        // the instance path is a JSON concept, so XML leaves it empty rather than smuggling a column in
        assertNull(error.getLocation());
        assertEquals("cvc-enumeration-valid", error.getSchemaPath());
    }

    /**
     * Prints the XML messages so the current wording can be reviewed without starting the service.
     */
    @Test
    @DisplayName("review: print the XML messages a user would see")
    void printMessagesForReview() {
        final String[] fixtures = {
                INVALID + "ObjectEvent_with_invalid_action.xml",
                INVALID + "ObjectEvent_with_invalid_eventTime_format.xml",
                INVALID + "ObjectEvent_with_unknown_element.xml",
                INVALID + "ObjectEvent_with_wrong_element_order.xml",
                INVALID + "ObjectEvent_without_eventTime.xml"
        };

        System.out.println("---- XML validation messages for review ----");
        for (final String fixture : fixtures) {
            for (final ValidationError error : validateCapture(fixture)) {
                System.out.printf("%-45s type=%-18s line=%s col=%s%n    %s%n", fixture, error.getType(), error.getLine(), error.getLocation(), error.getMessage());
            }
        }
    }
}
