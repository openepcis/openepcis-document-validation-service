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
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class JsonDocumentTest {

    // paths of the shared documents in openepcis-test-resources
    private static final String QUERY = "2.0/EPCIS/JSON/Query/";
    private static final String QUERY_INVALID = "2.0/EPCIS/JSON/Query/Invalid/";
    private static final String INVALID = "2.0/EPCIS/JSON/Capture/Invalid/";

    private final ObjectMapper mapper = new ObjectMapper();
    private final SchemaValidator validator =
            new SchemaValidator(mapper, JsonSchemaFactory.builder(JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7))
                    .jsonNodeReader(JsonNodeReader.builder().jsonMapper(mapper).build()).build());

    @Test
    void validateCaptureDocumentTest() {
        final InputStream inputStream = Commons.getInputStream(INVALID + "Combination_of_events_with_missing_mandatory_fields.json");
        final Multi<ValidationError> jsonCaptureErrors = validator.validate(inputStream, "application/json", EPCISDocumentType.CAPTURE, EPCISVersion.VERSION_2_0_0);
        assertFalse(jsonCaptureErrors.subscribe().asStream().toList().isEmpty());
    }

    @Test
    void validateCaptureDocumentWithCertificationTest() {
        final InputStream inputStream = Commons.getInputStream(INVALID + "ObjectEvent_with_invalid_certificationInfo.json");
        final Multi<ValidationError> jsonCaptureErrors = validator.validate(inputStream, "application/json", EPCISDocumentType.CAPTURE, EPCISVersion.VERSION_2_0_0);
        assertFalse(jsonCaptureErrors.subscribe().asStream().toList().isEmpty());
    }

    @Test
    void validateQueryDocumentTest() {
        final InputStream inputStream = Commons.getInputStream(QUERY + "queryDocument.json");
        final Multi<ValidationError> jsonQueryErrors = validator.validate(inputStream, "application/json", EPCISDocumentType.QUERY, EPCISVersion.VERSION_2_0_0);
        final List<ValidationError> errors = jsonQueryErrors.subscribe().asStream().toList();
        assertEquals(0, errors.size());
    }

    @Test
    void validateQueryDocumentWithInValidEventTypeTest() {
        final InputStream inputStream = Commons.getInputStream(QUERY_INVALID + "QueryDocument_with_invalid_eventType.json");
        final Multi<ValidationError> jsonQueryErrors = validator.validate(inputStream, "application/json", EPCISDocumentType.QUERY, EPCISVersion.VERSION_2_0_0);
        final List<ValidationError> errors = jsonQueryErrors.subscribe().asStream().toList();
        assertFalse(errors.isEmpty());
        assertEquals(1, errors.size());
    }
}
