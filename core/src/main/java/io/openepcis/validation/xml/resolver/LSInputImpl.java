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
package io.openepcis.validation.xml.resolver;

import lombok.Getter;
import lombok.Setter;
import org.w3c.dom.ls.LSInput;

import java.io.InputStream;
import java.io.Reader;

@Getter
@Setter
public class LSInputImpl implements LSInput {
  private Reader characterStream;
  private InputStream byteStream;
  private String stringData;
  private String systemId;
  private String publicId;
  private String baseURI;
  private String encoding;
  private boolean certifiedText;

  @Override
  public boolean getCertifiedText() {
    return certifiedText;
  }
}
