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
package io.openepcis.validation.xml;

import io.openepcis.validation.Validator;
import io.openepcis.validation.config.ValidationDefaults;
import io.openepcis.validation.exception.SchemaValidationException;
import io.openepcis.validation.model.ValidationError;
import io.smallrye.mutiny.Multi;
import org.xml.sax.SAXException;

import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;

public abstract class AbstractXmlSchemaValidator implements Validator {

    // Xerces property that selects the language of its validation messages
    private static final String XERCES_LOCALE_PROPERTY = "http://apache.org/xml/properties/locale";

    protected int maxReportedErrors = ValidationDefaults.MAX_REPORTED_ERRORS;
    protected Schema captureXmlSchema;
    protected Schema queryXmlSchema;
    protected EventModifier eventModifier = new EventModifier();
    protected SchemaFactory schemaFactory;

    @Override
    public Multi<ValidationError> validateAgainstCaptureSchema(final InputStream captureInput) {
        return Multi.createFrom()
                .emitter(
                        multiEmitter -> {
                            try {
                                final InputStream inputEvent = eventModifier.modifyEvent(captureInput);
                                final javax.xml.validation.Validator validator = newValidator(captureXmlSchema);
                                // Fresh handler to avoid reused validation polluting with old errors
                                final XmlErrorHandler errorHandler = new XmlErrorHandler(maxReportedErrors);

                                validator.setErrorHandler(errorHandler);
                                validator.validate(new StreamSource(inputEvent));
                                errorHandler.getExceptions().forEach(multiEmitter::emit);
                                multiEmitter.complete();
                            } catch (Exception e) {
                                multiEmitter.fail(new SchemaValidationException("Failed to validate the Capture XML document against the XSD : " + e.getMessage(), e));
                            }
                        });
    }

    @Override
    public Multi<ValidationError> validateAgainstQuerySchema(InputStream queryInput)
            throws SchemaValidationException {
        return Multi.createFrom()
                .emitter(
                        multiEmitter -> {
                            final javax.xml.validation.Validator validator = newValidator(queryXmlSchema);
                            // Fresh handler to avoid reused validation polluting with old errors
                            final XmlErrorHandler errorHandler = new XmlErrorHandler(maxReportedErrors);
                            validator.setErrorHandler(errorHandler);

                            try {
                                validator.validate(new StreamSource(queryInput));
                                errorHandler.getExceptions().forEach(multiEmitter::emit);
                                multiEmitter.complete();
                            } catch (SAXException | IOException e) {
                                multiEmitter.fail(new SchemaValidationException("Failed to validate the Query XML document against the XSD : " + e.getMessage(), e));
                            }
                        });
    }

    /**
     * Creates a validator whose messages are always in English.
     * The JDK's Xerces localises its messages to the default JVM locale, and {@link XmlErrorHandler} mines the
     * English wording for field, value and allowed values, so a German or French server would otherwise report
     * errors without those details.
     * Locale.ROOT is deliberate: Xerces ships English only as the base bundle, and a ResourceBundle lookup for
     * Locale.ENGLISH falls back to the JVM default locale before the base bundle, so it would still be German.
     */
    protected javax.xml.validation.Validator newValidator(final Schema schema) {
        final javax.xml.validation.Validator validator = schema.newValidator();
        try {
            validator.setProperty(XERCES_LOCALE_PROPERTY, Locale.ROOT);
        } catch (SAXException e) {
            // another schema implementation without this property, keep its default messages
        }
        return validator;
    }

    protected Schema loadSchema(final String name) {
        Schema schema;
        try {
            schema = schemaFactory.newSchema(new StreamSource(getClass().getResourceAsStream(name)));
        } catch (Exception e) {
            throw new SchemaValidationException("Failed to load XSD schema : " + e.getMessage(), e);
        }
        return schema;
    }
}
