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

import io.openepcis.validation.exception.SchemaValidationException;
import io.openepcis.validation.model.ValidationError;
import io.smallrye.mutiny.Multi;

import java.io.InputStream;

public interface Validator {

  Multi<ValidationError> validateAgainstCaptureSchema(final InputStream epcisInputData)
      throws SchemaValidationException;

  Multi<ValidationError> validateAgainstQuerySchema(final InputStream epcisInputData)
      throws SchemaValidationException;



  default InputStream getResourceAsStream(final String name) {
    InputStream inputStream = getClass().getResourceAsStream(name);
    if (inputStream == null) {
      return Thread.currentThread().getContextClassLoader().getResourceAsStream(name);
    }
    return inputStream;
  }
}
