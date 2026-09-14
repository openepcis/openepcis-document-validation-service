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
package io.openepcis.validation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchemaFactory;
import io.openepcis.constants.EPCIS;
import io.openepcis.constants.EPCISDocumentType;
import io.openepcis.constants.EPCISVersion;
import io.openepcis.validation.config.ValidationDefaults;
import io.openepcis.validation.exception.UnsupportedEPCISVersionException;
import io.openepcis.validation.json.JsonSchemaValidator;
import io.openepcis.validation.model.ValidationError;
import io.openepcis.validation.xml.Xml12SchemaValidator;
import io.openepcis.validation.xml.Xml20SchemaValidator;
import io.smallrye.mutiny.Multi;
import org.apache.commons.lang3.StringUtils;

import java.io.InputStream;
import java.util.Locale;
import java.util.Optional;

public class SchemaValidator {

    private final JsonSchemaValidator jsonSchemaValidator;
    private final Xml20SchemaValidator xml20SchemaValidator;
    private final Xml12SchemaValidator xml12SchemaValidator;

    public SchemaValidator(final ObjectMapper mapper, final JsonSchemaFactory factory) {
        this(mapper, factory, ValidationDefaults.MAX_REPORTED_ERRORS);
    }

    public SchemaValidator(final ObjectMapper mapper, final JsonSchemaFactory factory, final int maxReportedErrors) {
        this.jsonSchemaValidator = new JsonSchemaValidator(mapper, factory, maxReportedErrors);
        // same limit for XML and JSON, so the two front ends behave alike
        this.xml20SchemaValidator = new Xml20SchemaValidator(maxReportedErrors);
        this.xml12SchemaValidator = new Xml12SchemaValidator(maxReportedErrors);
    }

    /**
     * Method that accepts the EPCIS XML or JSON document/event and checks if it adheres to respective schema or not.
     *
     * @param epcisInputData      input EPCIS document/event that needs to be validated against schema.
     * @param mediaType           format type of the input document: application/xml, application/json, pplication/json+ld, etc.
     * @param epcisDocumentSchema type of EPCIS Document schema against which the document needs to be validated against: Capture or Query schema.
     */
    public Multi<ValidationError> validate(final InputStream epcisInputData,
                                           final String mediaType,
                                           final EPCISDocumentType epcisDocumentSchema,
                                           final EPCISVersion epcisVersion) {

        final Optional<Validator> validator = findValidator(mediaType, epcisVersion);

        if (validator.isEmpty()) {
            // if there is no validator then throw the exception
            return Multi.createFrom().failure(() -> unsupported(mediaType, epcisVersion));
        }

        if (EPCISDocumentType.CAPTURE.equals(epcisDocumentSchema)) {
            return validator.get().validateAgainstCaptureSchema(epcisInputData);
        }

        if (EPCISDocumentType.QUERY.equals(epcisDocumentSchema)) {
            return validator.get().validateAgainstQuerySchema(epcisInputData);
        }


        return Multi.createFrom().failure(
                () -> new UnsupportedOperationException("Unsupported schema type : " + epcisDocumentSchema));
    }

    private Optional<Validator> findValidator(final String mediaType, final EPCISVersion epcisVersion) {
        final String format = StringUtils.isBlank(mediaType) ? "" : mediaType.toLowerCase(Locale.ROOT);

        // For JSON input return supported validator
        if (format.contains(EPCIS.JSON_FORMAT)) {
            // JSON is valid only for 2.x there is no JSON in 1.2
            return EPCISVersionSupport.VERSION_2_0_FAMILY.contains(epcisVersion)
                    ? Optional.of(jsonSchemaValidator)
                    : Optional.empty();
        }

        // For XML input return supported validator
        if (format.contains(EPCIS.XML_FORMAT)) {
            if (EPCISVersionSupport.VERSION_2_0_FAMILY.contains(epcisVersion))
                return Optional.of(xml20SchemaValidator);

            return EPCISVersion.VERSION_1_2_0 == epcisVersion
                    ? Optional.of(xml12SchemaValidator)
                    : Optional.empty();
        }

        return Optional.empty();
    }

    private RuntimeException unsupported(final String mediaType, final EPCISVersion epcisVersion) {
        // For any unknown format
        if (EPCISVersionSupport.supportedFor(mediaType).isEmpty()) {
            return new UnsupportedOperationException("Unsupported media type : " + mediaType);
        }

        // for a known format with version we cannot validate (version problem)
        return new UnsupportedEPCISVersionException(epcisVersion.getVersion(), mediaType, EPCISVersionSupport.listFor(mediaType));
    }
}
