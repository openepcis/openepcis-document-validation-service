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
package io.openepcis.validation.test;

import io.openepcis.constants.EPCISVersion;
import io.openepcis.validation.EPCISVersionSupport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Optional;

import static io.openepcis.constants.EPCISVersion.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for EPCIS releases this service accepts, and for the version negotiation logic that selects a schema based on the request's
 */
class EPCISVersionSupportTest {

    private static final String JSON = "application/json";
    private static final String JSON_LD = "application/ld+json";
    private static final String XML = "application/xml";

    @ParameterizedTest(name = "a blank header [{0}] falls back to the default")
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void blankHeaderFallsBackToDefault(final String header) {
        // an absent header is normal, so it defaults rather than failing the request
        assertEquals(Optional.of(EPCISVersionSupport.DEFAULT_VERSION), EPCISVersionSupport.parse(header));
    }

    @Test
    void defaultVersionIs201() {
        // 2.0.1 is the current GS1 release; 2.0.0 stays accepted, it is only no longer the default
        assertEquals(VERSION_2_0_1, EPCISVersionSupport.DEFAULT_VERSION);
    }

    @Test
    void defaultVersionIsValidForEveryFormatWeAccept() {
        // a default that no format supports would turn every request without a version header into a 415
        assertTrue(EPCISVersionSupport.supportedFor(JSON).contains(EPCISVersionSupport.DEFAULT_VERSION));
        assertTrue(EPCISVersionSupport.supportedFor(JSON_LD).contains(EPCISVersionSupport.DEFAULT_VERSION));
        assertTrue(EPCISVersionSupport.supportedFor(XML).contains(EPCISVersionSupport.DEFAULT_VERSION));
    }

    @ParameterizedTest(name = "header [{0}] resolves to {1}")
    @CsvSource({
            "2.0.1, VERSION_2_0_1",
            "2.0.0, VERSION_2_0_0",
            "2.0,   VERSION_2_0_0",
            "1.2.0, VERSION_1_2_0",
            "1.2,   VERSION_1_2_0",
            "2.1.0, VERSION_2_1_0"
    })
    void parseResolvesKnownVersions(final String header, final EPCISVersion expected) {
        // both the full version and the short schemaVersion spelling are accepted by the shared enum
        assertEquals(Optional.of(expected), EPCISVersionSupport.parse(header));
    }

    @ParameterizedTest(name = "header [{0}] is refused")
    @ValueSource(strings = {"9.9.9", "2.0.2", "3.0.0", "2", "banana"})
    void parseRefusesUnknownVersions(final String header) {
        // a typo must not silently fall back to 2.0.0, because the version selects the schema
        assertTrue(EPCISVersionSupport.parse(header).isEmpty(), "an unrecognised version must be refused, not defaulted: " + header);
    }

    // --------------------------------------------------------- supportedFor

    @ParameterizedTest(name = "JSON media type [{0}] excludes every 1.x version")
    @ValueSource(strings = {JSON, JSON_LD, "application/json;charset=UTF-8"})
    void jsonExcludesTheOnePointTwoLine(final String mediaType) {
        // EPCIS 1.2 predates the JSON/JSON-LD binding, so there is no 1.2 JSON schema to check against
        assertFalse(EPCISVersionSupport.supportedFor(mediaType).contains(VERSION_1_2_0));
        assertFalse(EPCISVersionSupport.supportedFor(mediaType).contains(VERSION_1_1_0));
    }

    @ParameterizedTest(name = "JSON media type [{0}] accepts the 2.0 line")
    @ValueSource(strings = {JSON, JSON_LD, "application/json;charset=UTF-8"})
    void jsonAcceptsTheTwoPointZeroFamily(final String mediaType) {
        assertTrue(EPCISVersionSupport.supportedFor(mediaType).contains(VERSION_2_0_0));
        assertTrue(EPCISVersionSupport.supportedFor(mediaType).contains(VERSION_2_0_1));
    }

    @Test
    void xmlAcceptsOnePointTwoAndTheTwoPointZeroFamily() {
        // core bundles both xsd/1_2_0 and xsd/2_0_0, which is why XML takes all three
        assertTrue(EPCISVersionSupport.supportedFor(XML).contains(VERSION_1_2_0));
        assertTrue(EPCISVersionSupport.supportedFor(XML).contains(VERSION_2_0_0));
        assertTrue(EPCISVersionSupport.supportedFor(XML).contains(VERSION_2_0_1));
    }

    @Test
    void nothingAcceptsVersion210Yet() {
        // GS1 has published no 2.1.0 schemas; delete this once xsd/2_1_0 is bundled
        assertFalse(EPCISVersionSupport.supportedFor(JSON).contains(VERSION_2_1_0));
        assertFalse(EPCISVersionSupport.supportedFor(XML).contains(VERSION_2_1_0));
    }

    @ParameterizedTest(name = "unknown media type [{0}] supports nothing")
    @NullAndEmptySource
    @ValueSource(strings = {"text/plain", "application/pdf"})
    void unknownMediaTypeSupportsNoVersion(final String mediaType) {
        // an empty set is what tells SchemaValidator to report a media type problem, not a version one
        assertTrue(EPCISVersionSupport.supportedFor(mediaType).isEmpty());
    }

    // --------------------------------------------------------------- listFor

    @Test
    void listForNamesOnlyVersionsThatFormatAccepts() {
        // this string goes straight into the 415 message, so it must not promise 1.2 to a JSON caller
        final String json = EPCISVersionSupport.listFor(JSON);
        assertTrue(json.contains("2.0.0"));
        assertTrue(json.contains("2.0.1"));
        assertFalse(json.contains("1.2.0"));

        final String xml = EPCISVersionSupport.listFor(XML);
        assertTrue(xml.contains("1.2.0"));
        assertTrue(xml.contains("2.0.1"));
    }

    @Test
    void listForUnknownMediaTypeIsEmpty() {
        assertEquals("", EPCISVersionSupport.listFor("text/plain"));
    }
}
