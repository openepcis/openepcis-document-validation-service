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
package io.openepcis.validation.xml;

import io.openepcis.validation.model.ValidationError;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXParseException;

/** custom error handler while validating xml against xsd */
public class XmlErrorHandler implements ErrorHandler {
  @lombok.Getter private final java.util.List<ValidationError> exceptions;

  public XmlErrorHandler() {
    this.exceptions = new java.util.ArrayList<>();
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

  public void handleError(SAXParseException e) {
    // This will allow the validation to continue even if there are errors.
    final ValidationError errorFormat = new ValidationError();
    errorFormat.setType("required");
    errorFormat.setLine(Integer.toString(e.getLineNumber()));
    errorFormat.setColumn(Integer.toString(e.getColumnNumber()));
    errorFormat.setMessage(e.getMessage());
    this.exceptions.add(errorFormat);
  }
}
