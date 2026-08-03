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

import io.openepcis.validation.message.MessageTemplates;
import io.openepcis.validation.model.ValidationError;
import lombok.Getter;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXParseException;

import java.util.ArrayList;
import java.util.List;

/**
 * custom error handler while validating xml against xsd
 */
public class XmlErrorHandler implements ErrorHandler {
    @Getter
    private final List<ValidationError> exceptions;

    public XmlErrorHandler() {
        this.exceptions = new ArrayList<>();
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
        final String raw = e.getMessage() == null ? "" : e.getMessage(); // never NPE on a null message
        final String line = Integer.toString(e.getLineNumber());
        final String column = Integer.toString(e.getColumnNumber());

        // Xerces reports one mistake twice, for example "not facet-valid" then "value of element is
        // not valid". Same line and column is the same mistake, so keep the first, more specific one.
        for (final ValidationError existing : this.exceptions) {
            if (line.equals(existing.getLine()) && column.equals(existing.getLocation())) {
                return;
            }
        }

        final ValidationError errorFormat = new ValidationError();
        errorFormat.setType(typeOf(raw)); // a real type, instead of always reporting "required"
        errorFormat.setLine(line); // unchanged, the front end uses this for XML highlighting
        errorFormat.setLocation(column); // unchanged
        errorFormat.setMessage(errorFormat.filterErrorMessage(raw));
        this.exceptions.add(errorFormat);
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

}
