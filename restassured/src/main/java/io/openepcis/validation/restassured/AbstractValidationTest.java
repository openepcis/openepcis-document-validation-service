package io.openepcis.validation.restassured;

import io.openepcis.constants.EPCIS;
import io.openepcis.resources.util.Commons;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import jakarta.ws.rs.core.MediaType;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public abstract class AbstractValidationTest {


    protected abstract String url();

    // Invalid input content type
    @Test
    public void invalidContentTypeTest() {
        Stream.of(ContentType.JSON, ContentType.XML).forEach(accept -> {
            final Response response =
                    RestAssured.given()
                            .contentType(ContentType.TEXT)
                            .accept(accept)
                            .body(Commons.getInputStream("2.0/EPCIS/XML/Capture/Documents/AggregationEvent.xml"))
                            .when()
                            .post(url());

            assertEquals(415, response.getStatusCode());

            // test for strings in JSON only
            if (ContentType.JSON.equals(accept)) {
                assertEquals("NotSupportedException", response.jsonPath().get("type"));
                assertEquals(
                        "The content-type header value did not match the value in @Consumes",
                        response.jsonPath().get("detail"));
            }
        });
    }

    @Test
    public void validJSONCaptureDocumentTest() {
        final Response response =
                RestAssured.given()
                        .contentType(ContentType.JSON)
                        .accept(ContentType.JSON)
                        .queryParam("epcisDocumentSchema", EPCIS.CAPTURE)
                        .body(Commons.getInputStream("2.0/EPCIS/JSON/Capture/Documents/AggregationEvent.json"))
                        .when()
                        .post(url());
        assertEquals(200, response.getStatusCode());
    }

    @Test
    public void validXMLCaptureDocumentTest() throws Exception {
        final Response response =
                RestAssured.given()
                        .contentType(MediaType.APPLICATION_XML)
                        .accept(ContentType.JSON)
                        .queryParam("epcisDocumentSchema", EPCIS.CAPTURE)
                        .body(
                                IOUtils.toString(
                                        Commons.getInputStream("2.0/EPCIS/XML/Capture/Documents/AssociationEvent.xml"),
                                        StandardCharsets.UTF_8))
                        .when()
                        .post(url());
        assertEquals(200, response.getStatusCode());
    }
}
