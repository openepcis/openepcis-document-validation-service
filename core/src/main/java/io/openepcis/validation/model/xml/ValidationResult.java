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
package io.openepcis.validation.model.xml;

import io.openepcis.validation.model.ValidationError;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

import java.util.List;

@XmlRootElement(name = "ValidationResult")
@XmlType(name = "ValidationResult")
public class ValidationResult {
  @XmlElementWrapper(name = "errors")
  @XmlElement(name = "ValidationError")
  private List<ValidationError> errors;

  public ValidationResult() {
  }

  public ValidationResult(List<ValidationError> errors) {
    this.errors = errors;
  }

  public List<ValidationError> getErrors() {
    return this.errors;
  }

  public void setErrors(List<ValidationError> errors) {
    this.errors = errors;
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) return true;
    if (!(o instanceof ValidationResult)) return false;
    ValidationResult other = (ValidationResult) o;
    if (!other.canEqual((Object) this)) return false;
    Object this$errors = this.getErrors();
    Object other$errors = other.getErrors();
    if (this$errors == null ? other$errors != null : !this$errors.equals(other$errors)) return false;
    return true;
  }

  protected boolean canEqual(Object other) {
    return other instanceof ValidationResult;
  }

  @Override
  public int hashCode() {
    int PRIME = 59;
    int result = 1;
    Object $errors = this.getErrors();
    result = result * PRIME + ($errors == null ? 43 : $errors.hashCode());
    return result;
  }

  @Override
  public String toString() {
    return "ValidationResult(errors=" + this.getErrors() + ")";
  }
}
