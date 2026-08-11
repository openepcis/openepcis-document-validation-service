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
import io.smallrye.mutiny.Multi;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.InputStream;
import java.util.stream.Stream;

import static io.openepcis.constants.EPCISDocumentType.CAPTURE;
import static io.openepcis.constants.EPCISDocumentType.QUERY;
import static io.openepcis.constants.EPCISVersion.VERSION_1_2_0;
import static io.openepcis.constants.EPCISVersion.VERSION_2_0_0;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class XmlDocumentTest {
    private final ObjectMapper mapper = new ObjectMapper();
    private final SchemaValidator validator =
            new SchemaValidator(
                    mapper,
                    JsonSchemaFactory.builder(JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7))
                            .jsonNodeReader(JsonNodeReader.builder().jsonMapper(mapper).build())
                            .build());
    private static final String INVALID_PATH = "2.0/EPCIS/XML/Capture/Invalid";
    private static final String QUERY_PATH = "2.0/EPCIS/XML/Query";
    private static final String CAPTURE_PATH = "2.0/EPCIS/XML/Capture/Documents";
    private static final String CAPTURE_PATH_EVENT = "2.0/EPCIS/XML/Capture/Events";
    private static final String CAPTURE_1_2_PATH = "1.2/EPCIS/XML/Capture/Documents";

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("validationTestCases")
    void validateDocuments(String testName, String resource, EPCISDocumentType documentType, EPCISVersion version, int expectedErrors) {
        final InputStream inputStream = Commons.getInputStream(resource);
        // a fixture that is not on the classpath comes back as null and only surfaces deep inside
        // EventModifier as an NPE, so name the missing file here instead
        assertNotNull(inputStream, "missing shared test resource " + resource);
        final Multi<ValidationError> validationErrorMulti = validator.validate(inputStream, "application/xml", documentType, version);
        var errors = validationErrorMulti.subscribe().asStream().toList();
        assertEquals(expectedErrors, errors.size());
    }

    static Stream<Arguments> validationTestCases() {
        return Stream.of(
                Arguments.of("Capture document with bare event", INVALID_PATH + "/ObjectEvent_with_invalid_action.xml", CAPTURE, VERSION_2_0_0, 1),
                Arguments.of("Capture document with comment", CAPTURE_1_2_PATH + "/EPCISDocument_with_comment.xml", CAPTURE, VERSION_1_2_0, 0),
                Arguments.of("Complete EPCIS capture document", CAPTURE_PATH + "/Combination_of_different_event.xml", CAPTURE, VERSION_2_0_0, 0),
                Arguments.of("Simple Query document", QUERY_PATH + "/queryDocument.xml", QUERY, VERSION_2_0_0, 0),
                Arguments.of("Query document with multiple events", QUERY_PATH + "/QueryDocumentWithMultipleEvents.xml", QUERY, VERSION_2_0_0, 0),
                Arguments.of("Query document with missing value", QUERY_PATH + "/Invalid/QueryDocument_with_missing_value.xml", QUERY, VERSION_2_0_0, 1),
                Arguments.of("Query document sent to the capture path", QUERY_PATH + "/queryDocument.xml", CAPTURE, VERSION_2_0_0, 1),
                Arguments.of("EPCIS bare event with no error", CAPTURE_PATH_EVENT + "/TransactionEvent.xml", CAPTURE, VERSION_2_0_0, 0),

                // XML Queries
                Arguments.of("Literal query with GE_ and LT_ params", QUERY_PATH + "/Queries/SimpleEventQuery.xml", QUERY, VERSION_2_0_0, 0),
                Arguments.of("Literal query without queryName", QUERY_PATH + "/Queries/Invalid/Query_without_queryName.xml", QUERY, VERSION_2_0_0, 1)
        );
    }
}
