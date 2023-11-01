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
package io.openepcis.validation.xml.resolver;

import org.w3c.dom.ls.LSInput;
import org.w3c.dom.ls.LSResourceResolver;

import java.io.InputStream;
import java.io.InputStreamReader;

public class ClasspathResourceResolver implements LSResourceResolver {

  private final String base_path;

  public ClasspathResourceResolver(String basePath) {
    base_path = basePath;
  }

  @Override
  public LSInput resolveResource(
      final String type,
      final String namespaceURI,
      final String publicId,
      final String systemId,
      final String baseURI) {
    final LSInputImpl input = new LSInputImpl();
    final String path = base_path+"/" + systemId;
    final InputStream stream = getClass().getClassLoader().getResourceAsStream(path);

    input.setPublicId(publicId);
    input.setSystemId(systemId);
    input.setBaseURI(baseURI);

    if (stream != null) {
      input.setCharacterStream(new InputStreamReader(stream));
    }

    return input;
  }
}
