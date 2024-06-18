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
package io.openepcis.validation.json;

import com.networknt.schema.ValidationMessage;
import io.openepcis.validation.model.ValidationError;

import java.util.Collection;

/**
 * Class to format the validation errors occur during the validation of JSON Schema Capture/Query
 * document/event.
 */
public class JsonErrorHandler {

  @lombok.Getter private final java.util.List<ValidationError> exceptions;

  public JsonErrorHandler() {
    this.exceptions = new java.util.ArrayList<>();
  }

  public void jsonValidationFormat(final Collection<ValidationMessage> messages) {
    for (final ValidationMessage errorMessage : messages) {
      final ValidationError errorFormat = new ValidationError();
      errorFormat.setType(errorMessage.getType());
      errorFormat.setLine(errorMessage.getSchemaLocation().toString());
      errorFormat.setColumn(errorMessage.getInstanceLocation().toString());
      errorFormat.setMessage(errorMessage.getMessage());
      exceptions.add(errorFormat);
    }
  }
}
