package io.openepcis.core.resolver;

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
