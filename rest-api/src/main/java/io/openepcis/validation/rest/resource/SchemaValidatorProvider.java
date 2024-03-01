/*
 * Copyright 2022-2023 benelog GmbH & Co. KG
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
package io.openepcis.validation.rest.resource;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import io.openepcis.validation.SchemaValidator;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.smallrye.common.annotation.Blocking;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;


@RegisterForReflection
public class SchemaValidatorProvider {
    @Produces
    @RequestScoped
    public SchemaValidator schemaValidator(final JsonSchemaFactory factory) {
        return new SchemaValidator(mapper, factory);
    }

    private final ObjectMapper mapper;

    public SchemaValidatorProvider(final ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Produces
    public JsonSchemaFactory jsonSchemaFactory() {
        return JsonSchemaFactory.builder(JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7))
                .objectMapper(mapper)
                .build();
    }

}
