package io.openepcis.validation.rest.resource.test;

import io.openepcis.resources.util.ResourceFinder;
import io.restassured.path.xml.XmlPath;
import io.restassured.response.Response;

import java.io.InputStream;

public class Commons {
  public static final String XML_PATH = "epcis:EPCISDocument.EPCISBody.EventList";

  // Read the provided file path as InputStream and return the values
  public static InputStream getInputStream(final String filePath) {
    return ResourceFinder.class.getClassLoader().getResourceAsStream(filePath);
  }

  // Get the XML path from provided response body
  public static XmlPath getXmlPath(final Response response) {
    // Convert the response body to string
    final String responseBody = response.getBody().asString();

    // Create an XmlPath object from the response body
    return new XmlPath(responseBody);
  }
}
