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
package io.openepcis.validation.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.networknt.schema.ValidationMessage;
import io.openepcis.validation.message.JsonErrorFormatter;
import io.openepcis.validation.model.ValidationError;
import lombok.Getter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Class to format the validation errors occur during the validation of JSON Schema Capture/Query document/event.
 */
public class JsonErrorHandler {

    @Getter
    private final List<ValidationError> exceptions;

    public JsonErrorHandler() {
        this.exceptions = new ArrayList<>();
    }

    private static final JsonErrorFormatter FORMATTER = new JsonErrorFormatter();

    /**
     * Groups the raw messages so one mistake becomes one error, then words them for people.
     */
    public void jsonValidationFormat(final JsonNode document, final Collection<ValidationMessage> messages) {
        exceptions.addAll(FORMATTER.format(document, messages));
    }

    @Deprecated
    public void jsonValidationFormat(final Collection<ValidationMessage> messages) {
        jsonValidationFormat(null, messages);
    }
}
