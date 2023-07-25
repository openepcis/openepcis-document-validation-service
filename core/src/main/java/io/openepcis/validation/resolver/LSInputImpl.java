package io.openepcis.validation.resolver;

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
