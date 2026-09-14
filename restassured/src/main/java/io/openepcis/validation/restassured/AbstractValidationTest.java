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
        Stream.of(ContentType.JSON, ContentType.XML)
                .forEach(
                        accept -> {
                            final Response response =
                                    RestAssured.given()
                                            .contentType(ContentType.TEXT)
                                            .accept(accept)
                                            .body(
                                                    Commons.getInputStream(
                                                            "2.0/EPCIS/XML/Capture/Documents/AggregationEvent.xml"))
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

    @Test
    public void xmlAcceptsVersion201Test() {
        // 2.0.1 must behave exactly like 2.0.0: GS1 changed no schema rule between them
        final Response response =
                RestAssured.given()
                        .contentType(MediaType.APPLICATION_XML)
                        .accept(ContentType.JSON)
                        .header("GS1-EPCIS-Version", "2.0.1")
                        .queryParam("epcisDocumentSchema", EPCIS.CAPTURE)
                        .body(Commons.getInputStream("2.0/EPCIS/XML/Capture/Documents/AssociationEvent.xml"))
                        .when()
                        .post(url());
        assertEquals(200, response.getStatusCode());
    }

    @Test
    public void xmlStillAcceptsVersion200Test() {
        // the old value stays valid, so the new default does not break existing callers
        final Response response =
                RestAssured.given()
                        .contentType(MediaType.APPLICATION_XML)
                        .accept(ContentType.JSON)
                        .header("GS1-EPCIS-Version", "2.0.0")
                        .queryParam("epcisDocumentSchema", EPCIS.CAPTURE)
                        .body(Commons.getInputStream("2.0/EPCIS/XML/Capture/Documents/AssociationEvent.xml"))
                        .when()
                        .post(url());
        assertEquals(200, response.getStatusCode());
    }

    @Test
    public void xmlAcceptsVersion12Test() {
        // 1.2 keeps working; only XML has a 1.2 XSD set bundled
        final Response response =
                RestAssured.given()
                        .contentType(MediaType.APPLICATION_XML)
                        .accept(ContentType.JSON)
                        .header("GS1-EPCIS-Version", "1.2.0")
                        .queryParam("epcisDocumentSchema", EPCIS.CAPTURE)
                        .body(Commons.getInputStream("1.2/EPCIS/XML/Capture/Documents/AggregationEvent.xml"))
                        .when()
                        .post(url());
        assertEquals(200, response.getStatusCode());
    }

    @Test
    public void jsonAcceptsVersion201Test() {
        final Response response =
                RestAssured.given()
                        .contentType(ContentType.JSON)
                        .accept(ContentType.JSON)
                        .header("GS1-EPCIS-Version", "2.0.1")
                        .queryParam("epcisDocumentSchema", EPCIS.CAPTURE)
                        .body(Commons.getInputStream("2.0/EPCIS/JSON/Capture/Documents/AggregationEvent.json"))
                        .when()
                        .post(url());
        assertEquals(200, response.getStatusCode());
    }

    @Test
    public void missingVersionHeaderDefaultsTo201Test() {
        // no header at all still validates, using the default
        final Response response =
                RestAssured.given()
                        .contentType(MediaType.APPLICATION_XML)
                        .accept(ContentType.JSON)
                        .queryParam("epcisDocumentSchema", EPCIS.CAPTURE)
                        .body(Commons.getInputStream("2.0/EPCIS/XML/Capture/Documents/AssociationEvent.xml"))
                        .when()
                        .post(url());
        assertEquals(200, response.getStatusCode());
    }

    @Test
    public void jsonRejectsVersion12Test() {
        // EPCIS 1.2 has no JSON binding, so this must be refused, never checked against the 2.0 schema
        final Response response =
                RestAssured.given()
                        .contentType(ContentType.JSON)
                        .accept(ContentType.JSON)
                        .header("GS1-EPCIS-Version", "1.2.0")
                        .queryParam("epcisDocumentSchema", EPCIS.CAPTURE)
                        .body(Commons.getInputStream("2.0/EPCIS/JSON/Capture/Documents/AggregationEvent.json"))
                        .when()
                        .post(url());
        assertEquals(415, response.getStatusCode());
    }

    @Test
    public void unknownVersionIsRejectedTest() {
        // a typo must not fall back to 2.0.0 silently; the header really does pick the schema
        final Response response =
                RestAssured.given()
                        .contentType(MediaType.APPLICATION_XML)
                        .accept(ContentType.JSON)
                        .header("GS1-EPCIS-Version", "9.9.9")
                        .queryParam("epcisDocumentSchema", EPCIS.CAPTURE)
                        .body(Commons.getInputStream("2.0/EPCIS/XML/Capture/Documents/AssociationEvent.xml"))
                        .when()
                        .post(url());
        assertEquals(415, response.getStatusCode());
    }

    @Test
    public void version210IsRejectedUntilItShipsTest() {
        // 2.1.0 already exists in the shared enum; delete this test when the 2.1 schemas are bundled
        final Response response =
                RestAssured.given()
                        .contentType(MediaType.APPLICATION_XML)
                        .accept(ContentType.JSON)
                        .header("GS1-EPCIS-Version", "2.1.0")
                        .queryParam("epcisDocumentSchema", EPCIS.CAPTURE)
                        .body(Commons.getInputStream("2.0/EPCIS/XML/Capture/Documents/AssociationEvent.xml"))
                        .when()
                        .post(url());
        assertEquals(415, response.getStatusCode());
    }

}
