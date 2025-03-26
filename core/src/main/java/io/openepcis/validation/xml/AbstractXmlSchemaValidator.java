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
package io.openepcis.validation.xml;

import io.openepcis.validation.Validator;
import io.openepcis.validation.exception.SchemaValidationException;
import io.openepcis.validation.model.ValidationError;
import io.smallrye.mutiny.Multi;
import java.io.IOException;
import java.io.InputStream;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import org.xml.sax.SAXException;

public abstract class AbstractXmlSchemaValidator implements Validator {

  protected Schema captureXmlSchema;
  protected Schema queryXmlSchema;
  protected EventModifier eventModifier = new EventModifier();
  protected XmlErrorHandler xsdErrorHandler;
  protected SchemaFactory schemaFactory;

  @Override
  public Multi<ValidationError> validateAgainstCaptureSchema(final InputStream captureInput) {
    return Multi.createFrom()
        .emitter(
            multiEmitter -> {
              try {
                final InputStream inputEvent = eventModifier.modifyEvent(captureInput);
                final javax.xml.validation.Validator validator = captureXmlSchema.newValidator();
                validator.setErrorHandler(this.xsdErrorHandler);
                validator.validate(new StreamSource(inputEvent));
                this.xsdErrorHandler.getExceptions().forEach(multiEmitter::emit);
                multiEmitter.complete();
              } catch (Exception e) {
                multiEmitter.fail(
                    new SchemaValidationException(
                        "Exception occurred during the validation of Capture XML document/event against XSD : "
                            + e.getMessage(),
                        e));
              }
            });
  }

  @Override
  public Multi<ValidationError> validateAgainstQuerySchema(InputStream queryInput)
      throws SchemaValidationException {
    return Multi.createFrom()
        .emitter(
            multiEmitter -> {
              final javax.xml.validation.Validator validator = queryXmlSchema.newValidator();
              validator.setErrorHandler(this.xsdErrorHandler);
              try {
                validator.validate(new StreamSource(queryInput));
                this.xsdErrorHandler.getExceptions().forEach(multiEmitter::emit);
                multiEmitter.complete();
              } catch (SAXException | IOException e) {
                multiEmitter.fail(
                    new SchemaValidationException(
                        "Exception occurred during the validation of Query XML document/event against XSD : "
                            + e.getMessage()
                            + e));
              }
            });
  }

  protected Schema loadSchema(final String name) {
    Schema schema;
    try {
      schema = schemaFactory.newSchema(new StreamSource(getClass().getResourceAsStream(name)));
    } catch (Exception e) {
      throw new SchemaValidationException(
          "Exception occurred during the loading of the XSD document to schema : "
              + e.getMessage()
              + e);
    }
    return schema;
  }
}
