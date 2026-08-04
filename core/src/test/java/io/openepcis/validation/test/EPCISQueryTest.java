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
import io.openepcis.constants.EPCISVersion;
import io.openepcis.resources.util.Commons;
import io.openepcis.validation.SchemaValidator;
import io.openepcis.validation.model.ValidationError;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;

import static io.openepcis.constants.EPCISDocumentType.QUERY;
import static org.junit.jupiter.api.Assertions.*;

class EPCISQueryTest {

    private static final String QUERIES = "2.0/EPCIS/JSON/Query/Queries/";
    private static final String QUERIES_INVALID = QUERIES + "Invalid/";

    private final ObjectMapper mapper = new ObjectMapper();
    private final SchemaValidator validator =
            new SchemaValidator(mapper, JsonSchemaFactory.builder(JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7))
                    .jsonNodeReader(JsonNodeReader.builder().jsonMapper(mapper).build()).build());

    @Test
    @DisplayName("valid: a literal query with GE_ and LT_ filters reports no errors")
    void validLiteralQuery() {
        assertEquals(0, validate(QUERIES + "SimpleEventQuery.json").size());
    }


    @Test
    @DisplayName("invalid: a literal query with a bad action and a bad date reports both")
    void literalQueryReportsEveryBadValue() {
        final List<ValidationError> errors = validate(QUERIES_INVALID + "Query_with_invalid_values.json");

        assertEquals(2, errors.size()); // neither problem hides the other
        assertTrue(errors.stream().anyMatch(e -> "EQ_action".equals(e.getField())));
        assertTrue(errors.stream().anyMatch(e -> "GE_eventTime".equals(e.getField())));
    }

    @Test
    @DisplayName("valid: a bare event with its own @context reports no errors")
    void validBareEvent() {
        assertEquals(0, validate("2.0/EPCIS/JSON/Capture/Events/ObjectEvent.json").size());
    }

    @Test
    @DisplayName("valid: a query document is validated as a document, not as a query")
    void queryDocumentIsValidatedAsADocument() {
        assertEquals(0, validate("2.0/EPCIS/JSON/Query/queryDocument.json").size());
    }

    @Test
    @DisplayName("invalid: an event inside a query document reports its full nested path")
    void queryDocumentEventKeepsItsNestedPath() {
        final ValidationError error = validate("2.0/EPCIS/JSON/Query/Invalid/QueryDocument_with_invalid_action.json").getFirst();

        // the path the front end maps to an editor line must keep its exact shape
        assertTrue(error.getLocation().startsWith("$.epcisBody.queryResults.resultsBody.eventList["));
    }

    @Test
    @DisplayName("invalid: an unknown query parameter is named in the message")
    void unknownQueryParameterIsNamed() {
        final ValidationError error = validate(QUERIES_INVALID + "Query_with_unknown_parameter.json").getFirst();
        assertEquals("GE_eventDate", error.getField());
    }

    // Validates one fixture and returns the errors.
    private List<ValidationError> validate(final String resourcePath) {
        final InputStream document = Commons.getInputStream(resourcePath);
        assertNotNull(document, "missing shared test resource " + resourcePath);
        return validator
                .validate(document, "application/json", QUERY, EPCISVersion.VERSION_2_0_0)
                .subscribe()
                .asStream()
                .toList();
    }
}
