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
package io.openepcis.validation.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import io.openepcis.constants.EPCISDocumentType;
import io.openepcis.constants.EPCISVersion;
import io.openepcis.validation.SchemaValidator;
import io.openepcis.validation.model.ValidationError;
import io.smallrye.mutiny.Multi;
import java.io.InputStream;
import org.junit.jupiter.api.Test;

public class XmlDocumentTest {
  private ObjectMapper mapper = new ObjectMapper();
  private SchemaValidator validator =
      new SchemaValidator(
          mapper,
          JsonSchemaFactory.builder(JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7))
              .jsonMapper(mapper)
              .build());

  @Test
  public void captureDocumentWithBareEventTest() {
    final InputStream inputStream =
        getClass().getResourceAsStream("/xml/captureDocument-independent-object-event.xml");
    final Multi<ValidationError> xsdCaptureErrors =
        validator.validate(
            inputStream, "application/xml", EPCISDocumentType.CAPTURE, EPCISVersion.VERSION_2_0_0);
    var errors = xsdCaptureErrors.subscribe().asStream().toList();
    assertEquals(1, errors.size());
  }

  @Test
  public void captureDocumentWithComment() {
    final InputStream inputStream =
        getClass().getResourceAsStream("/xml/captureDocumentWithComment.xml");
    final Multi<ValidationError> xsdCaptureErrors =
        validator.validate(
            inputStream, "application/xml", EPCISDocumentType.CAPTURE, EPCISVersion.VERSION_1_2_0);
    var errors = xsdCaptureErrors.subscribe().asStream().toList();
    assertEquals(0, errors.size());
  }

  @Test
  public void captureDocumentCompleteEPCISDocumentTest() {
    final InputStream inputStream =
        getClass().getResourceAsStream("/xml/captureDocument-complete-epcis-20-document.xml");
    final Multi<ValidationError> xsdCaptureErrors =
        validator.validate(
            inputStream, "application/xml", EPCISDocumentType.CAPTURE, EPCISVersion.VERSION_2_0_0);
    assertEquals(0, xsdCaptureErrors.subscribe().asStream().toList().size());
  }

  @Test
  public void queryDocumentWithEventTimeTest() {
    final InputStream inputStream = getClass().getResourceAsStream("/xml/queryDocument.xml");
    final Multi<ValidationError> xsdQueryErrors =
        validator.validate(
            inputStream, "application/xml", EPCISDocumentType.QUERY, EPCISVersion.VERSION_2_0_0);
    assertEquals(0, xsdQueryErrors.subscribe().asStream().toList().size());
  }

  @Test
  public void queryDocumentWithEventTypeTest() {
    final InputStream inputStream =
        getClass().getResourceAsStream("/xml/queryDocumentWithEventType.xml");
    final Multi<ValidationError> xsdQueryErrors =
        validator.validate(
            inputStream, "application/xml", EPCISDocumentType.QUERY, EPCISVersion.VERSION_2_0_0);
    assertEquals(0, xsdQueryErrors.subscribe().asStream().toList().size());
  }

  @Test
  public void queryDocumentWithReadPointTest() {
    final InputStream inputStream =
        getClass().getResourceAsStream("/xml/queryDocumentWithReadPoint.xml");
    final Multi<ValidationError> xsdQueryErrors =
        validator.validate(
            inputStream, "application/xml", EPCISDocumentType.QUERY, EPCISVersion.VERSION_2_0_0);
    assertEquals(0, xsdQueryErrors.subscribe().asStream().toList().size());
  }

  @Test
  public void queryDocumentWithMissingValueTest() {
    final InputStream inputStream =
        getClass().getResourceAsStream("/xml/queryDocumentWithMissingValue.xml");
    final Multi<ValidationError> xsdQueryErrors =
        validator.validate(
            inputStream, "application/xml", EPCISDocumentType.QUERY, EPCISVersion.VERSION_2_0_0);
    assertEquals(1, xsdQueryErrors.subscribe().asStream().toList().size());
  }
}
