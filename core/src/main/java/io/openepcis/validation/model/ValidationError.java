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
package io.openepcis.validation.model;

import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;
import lombok.Data;

@Data
@XmlRootElement(name = "ValidationError")
@XmlType(
    name = "ValidationError",
    propOrder = {"type", "line", "location", "message"})
public class ValidationError {

  @XmlAttribute private String type;

  @XmlElement private String line;

  @XmlElement private String location;

  @XmlElement private String message;

  public String filterErrorMessage(String message) {
    String[] messageParts = message.split(":", 2);
    return messageParts.length > 1 ? messageParts[1].trim() : message;
  }
}
