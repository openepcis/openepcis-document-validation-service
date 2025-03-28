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
package io.openepcis.validation.rest.resource.test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import io.openepcis.validation.SchemaValidator;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.inject.Produces;

public class CDITestProvider {

  @Produces
  @RequestScoped
  public SchemaValidator schemaValidator() {
    final ObjectMapper mapper = new ObjectMapper();
    return new SchemaValidator(
        mapper,
        JsonSchemaFactory.builder(JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7))
            .jsonMapper(mapper)
            .build());
  }
}
