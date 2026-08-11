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

import io.openepcis.validation.config.ValidationDefaults;
import io.openepcis.validation.message.MessageTemplates;
import io.openepcis.validation.message.ValueSuggester;
import io.openepcis.validation.model.ValidationError;
import org.apache.commons.lang3.StringUtils;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXParseException;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * custom error handler while validating xml against xsd
 */
public class XmlErrorHandler implements ErrorHandler {
    private final List<ValidationError> exceptions;
    // Max number of validation errors that can be added
    private final int maxReportedErrors;
    // Pattern to extract the values from Xerces validation errors
    private static final Pattern ENUM_VALUES = Pattern.compile("enumeration \'\\[([^\\]]*)]\'");
    private static final Pattern FACET_VALUE = Pattern.compile("Value \'([^\']*)\' is not");
    private static final Pattern DATATYPE_VALUE = Pattern.compile("\'([^\']*)\' is not a valid value for");
    private static final Pattern VALUE_OF_ELEMENT = Pattern.compile("The value \'([^\']*)\' of element \'([^\']*)\' is not valid");
    private static final Pattern ELEMENT_NAME = Pattern.compile("element \'\\{?[^\']*?[:\"]?([A-Za-z_][A-Za-z0-9_.\\-]*)}?\'");

    public XmlErrorHandler() {
        this(ValidationDefaults.MAX_REPORTED_ERRORS);
    }

    public XmlErrorHandler(final int maxReportedErrors) {
        this.exceptions = new ArrayList<>();
        // passed in from SchemaValidator, so core needs no configuration framework of its own
        this.maxReportedErrors = maxReportedErrors > 0 ? maxReportedErrors : ValidationDefaults.MAX_REPORTED_ERRORS;
    }

    @Override
    public void warning(final SAXParseException exception) {
        handleError(exception);
    }

    @Override
    public void error(final SAXParseException exception) {
        handleError(exception);
    }

    @Override
    public void fatalError(final SAXParseException exception) {
        handleError(exception);
    }

    public void handleError(final SAXParseException e) {
        if (this.exceptions.size() >= maxReportedErrors) {
            return; // the marker is already the last entry, nothing further is recorded
        }
        if (this.exceptions.size() == maxReportedErrors - 1) {
            this.exceptions.add(limitExceeded(maxReportedErrors - 1)); // same wording as the JSON path
            return;
        }
        final String raw = e.getMessage() == null ? "" : e.getMessage(); // never NPE on a null message
        final Integer line = e.getLineNumber() > 0 ? e.getLineNumber() : null;
        final Integer column = e.getColumnNumber() > 0 ? e.getColumnNumber() : null;
        // Xerces reports one mistake twice, for example "not facet-valid" then "value of element is not valid".
        // Same line and column is the same mistake, so keep the first, more specific one.
        for (final ValidationError existing : this.exceptions) {
            if (Objects.equals(line, existing.getLine()) && Objects.equals(column, existing.getColumn())) {
                // duplicate contains the values and messages so extract the values to include in errors
                enrich(existing, raw);
                return;
            }
        }
        final ValidationError errorFormat = new ValidationError();
        errorFormat.setType(typeOf(raw)); // a real type, instead of always reporting "required"
        errorFormat.setLine(line); // unchanged, the front end uses this for XML highlighting
        errorFormat.setColumn(column); // unchanged
        errorFormat.setSchemaPath(schemaRuleOf(raw)); //the cvc code, for diagnostics
        errorFormat.setMessage(errorFormat.filterErrorMessage(raw));
        enrich(errorFormat, raw); // extract the values and field names from the raw message
        this.exceptions.add(errorFormat);
    }

    //The leading W3C rule code, for example cvc-enumeration-valid, kept for diagnostics.
    private String schemaRuleOf(final String raw) {
        final int colon = raw.indexOf(':');
        return colon > 0 ? raw.substring(0, colon) : null;
    }

    /**
     * Maps the leading W3C XSD rule code to a response type via the catalogue. Longest match wins.
     */
    private String typeOf(final String raw) {
        final int colon = raw.indexOf(':');
        if (colon > 0) {
            final String code = raw.substring(0, colon); // e.g. cvc-complex-type.2.4.a
            for (int cut = code.length(); cut > 0; cut = code.lastIndexOf('.', cut - 1)) {
                final String mapped = MessageTemplates.xsdType(code.substring(0, cut));
                if (mapped != null) {
                    return mapped; // prefers cvc-complex-type.2.4 over cvc-complex-type
                }
            }
        }
        return "invalid"; // an honest default, better than claiming a field was required
    }

    // The last entry in errors when limit is exceeded, so a capped list is never mistaken for a complete one.
    private ValidationError limitExceeded(final int limit) {
        final ValidationError error = new ValidationError();
        error.setType("limitExceeded");
        error.setMessage(MessageTemplates.hasKeyword("limit.exceeded") ? MessageTemplates.render(MessageTemplates.keyword("limit.exceeded"), Map.of("limit", String.valueOf(limit))) : "Validation stopped after " + limit + " problems. This document has more.");
        return error;
    }

    // Get the values matching the error model to populate in errors from the Xerces validation errors
    private void enrich(final ValidationError error, final String raw) {
        if (StringUtils.isBlank(error.getValue())) {
            // User provided value which is invalid
            error.setValue(firstGroup(FACET_VALUE, raw));
        }
        if (StringUtils.isBlank(error.getValue())) {
            // 'not-a-date' is not a valid value for
            error.setValue(firstGroup(DATATYPE_VALUE, raw));
        }
        final Matcher valueOfElement = VALUE_OF_ELEMENT.matcher(raw);
        if (valueOfElement.find()) {
            if (StringUtils.isBlank(error.getValue())) {
                error.setValue(valueOfElement.group(1));
            }
            if (StringUtils.isBlank(error.getField())) {
                error.setField(valueOfElement.group(2)); // the element name, only this phrasing has it
            }
        }
        if (StringUtils.isBlank(error.getField())) {
            error.setField(firstGroup(ELEMENT_NAME, raw));
        }
        if (error.getAllowedValues() == null) {
            final String list = firstGroup(ENUM_VALUES, raw);
            if (list != null) {
                final List<String> allowed = Arrays.stream(list.split(",")).map(String::trim).filter(value -> !value.isEmpty()).toList();
                if (!allowed.isEmpty()) {
                    error.setAllowedValues(allowed);
                }
            }
        }
        // only worth suggesting once both halves are known, and never overwrite an existing one
        if (error.getSuggestion() == null && error.getValue() != null && error.getAllowedValues() != null) {
            error.setSuggestion(ValueSuggester.closest(error.getValue(), error.getAllowedValues()));
        }
    }

    private String firstGroup(final Pattern pattern, final String raw) {
        final Matcher matcher = pattern.matcher(raw);
        return matcher.find() ? matcher.group(1) : null;
    }

    public List<ValidationError> getExceptions() {
        return this.exceptions;
    }
}
