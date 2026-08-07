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
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.regex.Pattern;

@JsonInclude(JsonInclude.Include.NON_NULL)
@XmlRootElement(name = "ValidationError")
@XmlType(name = "ValidationError", propOrder = {"type", "line", "eventIndex", "eventType", "column", "location", "schemaPath", "message", "field", "value", "suggestion", "allowedValues"})
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

    public ValidationError() {
    }

    public String getType() {
        return this.type;
    }

    public Integer getLine() {
        return this.line;
    }

    public Integer getColumn() {
        return this.column;
    }

    public String getLocation() {
        return this.location;
    }

    public String getSchemaPath() {
        return this.schemaPath;
    }

    public String getMessage() {
        return this.message;
    }

    public String getField() {
        return this.field;
    }

    public String getValue() {
        return this.value;
    }

    public Integer getEventIndex() {
        return this.eventIndex;
    }

    public String getEventType() {
        return this.eventType;
    }

    public String getSuggestion() {
        return this.suggestion;
    }

    public List<String> getAllowedValues() {
        return this.allowedValues;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setLine(Integer line) {
        this.line = line;
    }

    public void setColumn(Integer column) {
        this.column = column;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public void setSchemaPath(String schemaPath) {
        this.schemaPath = schemaPath;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setField(String field) {
        this.field = field;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public void setEventIndex(Integer eventIndex) {
        this.eventIndex = eventIndex;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public void setSuggestion(String suggestion) {
        this.suggestion = suggestion;
    }

    public void setAllowedValues(List<String> allowedValues) {
        this.allowedValues = allowedValues;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof ValidationError)) return false;
        ValidationError other = (ValidationError) o;
        if (!other.canEqual((Object) this)) return false;
        Object this$line = this.getLine();
        Object other$line = other.getLine();
        if (this$line == null ? other$line != null : !this$line.equals(other$line)) return false;
        Object this$column = this.getColumn();
        Object other$column = other.getColumn();
        if (this$column == null ? other$column != null : !this$column.equals(other$column)) return false;
        Object this$eventIndex = this.getEventIndex();
        Object other$eventIndex = other.getEventIndex();
        if (this$eventIndex == null ? other$eventIndex != null : !this$eventIndex.equals(other$eventIndex)) return false;
        Object this$type = this.getType();
        Object other$type = other.getType();
        if (this$type == null ? other$type != null : !this$type.equals(other$type)) return false;
        Object this$location = this.getLocation();
        Object other$location = other.getLocation();
        if (this$location == null ? other$location != null : !this$location.equals(other$location)) return false;
        Object this$schemaPath = this.getSchemaPath();
        Object other$schemaPath = other.getSchemaPath();
        if (this$schemaPath == null ? other$schemaPath != null : !this$schemaPath.equals(other$schemaPath)) return false;
        Object this$message = this.getMessage();
        Object other$message = other.getMessage();
        if (this$message == null ? other$message != null : !this$message.equals(other$message)) return false;
        Object this$field = this.getField();
        Object other$field = other.getField();
        if (this$field == null ? other$field != null : !this$field.equals(other$field)) return false;
        Object this$value = this.getValue();
        Object other$value = other.getValue();
        if (this$value == null ? other$value != null : !this$value.equals(other$value)) return false;
        Object this$eventType = this.getEventType();
        Object other$eventType = other.getEventType();
        if (this$eventType == null ? other$eventType != null : !this$eventType.equals(other$eventType)) return false;
        Object this$suggestion = this.getSuggestion();
        Object other$suggestion = other.getSuggestion();
        if (this$suggestion == null ? other$suggestion != null : !this$suggestion.equals(other$suggestion)) return false;
        Object this$allowedValues = this.getAllowedValues();
        Object other$allowedValues = other.getAllowedValues();
        if (this$allowedValues == null ? other$allowedValues != null : !this$allowedValues.equals(other$allowedValues)) return false;
        return true;
    }

    protected boolean canEqual(Object other) {
        return other instanceof ValidationError;
    }

    @Override
    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        Object $line = this.getLine();
        result = result * PRIME + ($line == null ? 43 : $line.hashCode());
        Object $column = this.getColumn();
        result = result * PRIME + ($column == null ? 43 : $column.hashCode());
        Object $eventIndex = this.getEventIndex();
        result = result * PRIME + ($eventIndex == null ? 43 : $eventIndex.hashCode());
        Object $type = this.getType();
        result = result * PRIME + ($type == null ? 43 : $type.hashCode());
        Object $location = this.getLocation();
        result = result * PRIME + ($location == null ? 43 : $location.hashCode());
        Object $schemaPath = this.getSchemaPath();
        result = result * PRIME + ($schemaPath == null ? 43 : $schemaPath.hashCode());
        Object $message = this.getMessage();
        result = result * PRIME + ($message == null ? 43 : $message.hashCode());
        Object $field = this.getField();
        result = result * PRIME + ($field == null ? 43 : $field.hashCode());
        Object $value = this.getValue();
        result = result * PRIME + ($value == null ? 43 : $value.hashCode());
        Object $eventType = this.getEventType();
        result = result * PRIME + ($eventType == null ? 43 : $eventType.hashCode());
        Object $suggestion = this.getSuggestion();
        result = result * PRIME + ($suggestion == null ? 43 : $suggestion.hashCode());
        Object $allowedValues = this.getAllowedValues();
        result = result * PRIME + ($allowedValues == null ? 43 : $allowedValues.hashCode());
        return result;
    }

    @Override
    public String toString() {
        return "ValidationError(type=" + this.getType() + ", line=" + this.getLine() + ", column=" + this.getColumn() + ", location=" + this.getLocation() + ", schemaPath=" + this.getSchemaPath() + ", message=" + this.getMessage() + ", field=" + this.getField() + ", value=" + this.getValue() + ", eventIndex=" + this.getEventIndex() + ", eventType=" + this.getEventType() + ", suggestion=" + this.getSuggestion() + ", allowedValues=" + this.getAllowedValues() + ")";
    }
}
