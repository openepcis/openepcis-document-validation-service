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
package io.openepcis.validation.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.regex.Pattern;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@XmlRootElement(name = "ValidationError")
@XmlType(
        name = "ValidationError",
        propOrder = {
                "type", "line", "eventIndex", "eventType", "column", "location",
                "schemaPath", "message", "field", "value", "suggestion", "allowedValues"
        })
public class ValidationError {

    @XmlElement
    private String type;

    // XML only: line number in submitted document
    @XmlElement
    private Integer line;

    // XML only: column number of the error
    @XmlElement
    private Integer column;

    // JSON only: problem in document like $.epcisBody.eventList[5].bizStep
    @XmlElement
    private String location;

    // Which schema rules got rejected, for diagnostics only not for the display on UI
    @XmlElement
    private String schemaPath;

    @XmlElement
    private String message;

    // Indicate which field failed
    @XmlElement
    private String field;

    // User provided values
    @XmlElement
    private String value;

    // zero based event index
    @XmlElement
    private Integer eventIndex;

    // eventType for quick reference to users
    @XmlElement
    private String eventType;

    // closest standard value, or null for suggesting to user
    @XmlElement
    private String suggestion;

    // full list as data, UI can collapse it
    @XmlElementWrapper(name = "allowedValues")
    @XmlElement(name = "allowedValue")
    private List<String> allowedValues;

    // Strips only a leading XSD rule code such as "cvc-complex-type.2.4.a: ".
    private static final Pattern XSD_ERROR_CODE = Pattern.compile("^(cvc|src|sch)-[A-Za-z0-9_.\\-]+:\\s*");

    public String filterErrorMessage(final String message) {
        if (StringUtils.isBlank(message)) {
            return message;
        }

        return XSD_ERROR_CODE.matcher(message).replaceFirst("");
    }
}
