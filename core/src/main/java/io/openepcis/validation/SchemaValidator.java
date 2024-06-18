/*
 * Copyright 2022-2024 benelog GmbH & Co. KG
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
import io.openepcis.constants.EPCISDocumentType;
import io.openepcis.constants.EPCISVersion;
import io.openepcis.validation.model.ValidationError;
import io.openepcis.validation.json.JsonSchemaValidator;
import io.openepcis.validation.xml.Xml12SchemaValidator;
import io.openepcis.validation.xml.Xml20SchemaValidator;
import io.smallrye.mutiny.Multi;

import java.io.InputStream;

public class SchemaValidator {
    private final JsonSchemaValidator jsonSchemaValidator;
    private final Xml20SchemaValidator xml20SchemaValidator;
    private final Xml12SchemaValidator xml12SchemaValidator;
    public SchemaValidator(final ObjectMapper mapper, final JsonSchemaFactory factory) {
        this.jsonSchemaValidator = new JsonSchemaValidator(mapper, factory);
        this.xml20SchemaValidator = new Xml20SchemaValidator();
        this.xml12SchemaValidator = new Xml12SchemaValidator();
    }

    /**
     * Method that accepts the EPCIS XML or JSON document/event and checks if it adheres to respective
     * schema or not.
     *
     * @param epcisInputData input EPCIS document/event that needs to be validated against schema.
     * @param mediaType      format type of the input document: application/xml, application/json,
     *                       application/json+ld, etc.
     * @param epcisDocumentSchema     type of EPCIS Document schema against which the document needs to be validated against:
     *                       Capture or Query schema.
     */
    public Multi<ValidationError> validate(
            final InputStream epcisInputData,
            final String mediaType,
            final EPCISDocumentType epcisDocumentSchema,
            final EPCISVersion epcisVersion) {

            Validator validator = getValidator(mediaType, epcisVersion);

            if (epcisDocumentSchema.equals(EPCISDocumentType.CAPTURE)) {
                return validator.validateAgainstCaptureSchema(epcisInputData);
            }

            if (epcisDocumentSchema.equals(EPCISDocumentType.QUERY)) {
                return validator.validateAgainstQuerySchema(epcisInputData);
            }

            throw new UnsupportedOperationException(
                    "Unsupported schema type : " + epcisDocumentSchema);

        }

    private Validator getValidator(final String mediaType, EPCISVersion epcisVersion) {

        if (mediaType.toLowerCase().contains("json")) {
            return jsonSchemaValidator;
        }

        if (mediaType.toLowerCase().contains("xml")) {

            if (EPCISVersion.VERSION_2_0_0.getSchemaVersion().equals(epcisVersion.getSchemaVersion())) {
                return xml20SchemaValidator;
            }

            if (EPCISVersion.VERSION_1_2_0.getSchemaVersion().equals(epcisVersion.getSchemaVersion())) {
                return xml12SchemaValidator;
            }
        }

        throw new UnsupportedOperationException("Unsupported media type : " + mediaType);
    }

}
