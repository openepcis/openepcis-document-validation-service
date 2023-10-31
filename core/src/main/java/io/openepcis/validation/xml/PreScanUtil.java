package io.openepcis.validation.xml;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PreScanUtil {

  private static final String FIRST_TAG_REGEX = "<(?!\\?|\\!--)([\\S\\s]*?)>";
  private static final Pattern FIRST_TAG_PATTERN = Pattern.compile(FIRST_TAG_REGEX);

  public static final String scanFirstTag(final BufferedInputStream input) throws IOException {
    input.mark(4096);
    try  {
      final StringBuilder sb = new StringBuilder();
      final byte[] buffer = new byte[64];
      int len = -1;
      int bytesReceived = 0;
      Matcher matcher = FIRST_TAG_PATTERN.matcher(sb.toString());
      while (!matcher.find(0) && bytesReceived < 2048 && (len = input.read(buffer)) != -1) {
        sb.append(new String(buffer, 0, len, StandardCharsets.UTF_8));
        bytesReceived += len;
        matcher = FIRST_TAG_PATTERN.matcher(sb.toString());
      }
      if (matcher.find(0)) {
        return matcher.group(0);
      }
      return "";
    } finally {
      input.reset();
    }
  }

}
